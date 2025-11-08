/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.lsp.kotlin

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.tom.rv2ide.lsp.models.CompletionItem
import com.tom.rv2ide.lsp.models.CompletionItemKind
import com.tom.rv2ide.lsp.models.MatchLevel
import com.tom.rv2ide.lsp.models.TextEdit
import com.tom.rv2ide.models.Position
import com.tom.rv2ide.models.Range
import java.util.concurrent.Executors
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinCompletionConverter {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinCompletionConverter::class.java)
  }

  private val snippetTransformer = SnippetTransformer()
  private val importResolver = KotlinImportResolver()

  private val cpuDispatcher =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
          .asCoroutineDispatcher()

  private val completionCache =
      java.util.concurrent.ConcurrentHashMap<String, List<CompletionItem>>()

  private fun getCacheKey(fileContent: String, position: Int): String {
    return "${fileContent.hashCode()}_$position"
  }

  private var javaCompilerBridge: KotlinJavaCompilerBridge? = null

  fun setJavaCompilerBridge(bridge: KotlinJavaCompilerBridge) {
    this.javaCompilerBridge = bridge
  }

  /** Enhanced conversion that adds classpath-based completions */
  suspend fun convertWithClasspathEnhancement(
      itemsArray: JsonArray,
      fileContent: String,
      prefix: String,
  ): List<CompletionItem> =
      withContext(cpuDispatcher) {
        KslLogs.debug("Converting {} items with classpath enhancement", itemsArray.size())

        // Convert LSP server items
        val lspItems = convertFast(itemsArray, fileContent)

        // Add classpath-based completions if prefix is valid
        val classpathItems =
            if (prefix.length >= 2 && javaCompilerBridge != null) {
              getClasspathCompletions(prefix, fileContent)
            } else {
              emptyList()
            }

        // Merge and deduplicate
        val allItems = (lspItems + classpathItems).distinctBy { "${it.ideLabel}:${it.detail}" }

        KslLogs.debug("Total items after classpath enhancement: {}", allItems.size)
        allItems
      }

  /** Get completion items from classpath (Java compiler) */
  private fun getClasspathCompletions(prefix: String, fileContent: String): List<CompletionItem> {
    val bridge = javaCompilerBridge ?: return emptyList()

    return try {
      val classes = bridge.findClassesByPrefix(prefix)

      classes.map { classInfo ->
        val needsImport =
            importResolver.needsImportForClass(
                classInfo.simpleName,
                classInfo.fullyQualifiedName,
                fileContent,
            )

        val additionalEdits =
            if (needsImport) {
              val (line, importText) =
                  importResolver.generateImportEdit(classInfo.fullyQualifiedName, fileContent)
              listOf(
                  com.tom.rv2ide.lsp.models.TextEdit(
                      range =
                          com.tom.rv2ide.models.Range(
                              start = com.tom.rv2ide.models.Position(line, 0),
                              end = com.tom.rv2ide.models.Position(line, 0),
                          ),
                      newText = importText,
                  )
              )
            } else {
              null
            }

        CompletionItem(
            ideLabel = classInfo.simpleName,
            detail = classInfo.fullyQualifiedName,
            insertText = classInfo.simpleName,
            insertTextFormat = null,
            sortText = classInfo.simpleName,
            command = null,
            completionKind = com.tom.rv2ide.lsp.models.CompletionItemKind.CLASS,
            matchLevel = com.tom.rv2ide.lsp.models.MatchLevel.CASE_SENSITIVE_PREFIX,
            additionalTextEdits = additionalEdits,
            data = null,
        )
      }
    } catch (e: Exception) {
      KslLogs.error("Failed to get classpath completions", e)
      emptyList()
    }
  }

  suspend fun convertWithCache(
      itemsArray: JsonArray,
      fileContent: String,
      position: Int,
  ): List<CompletionItem> {
    val cacheKey = getCacheKey(fileContent, position)

    return completionCache.getOrPut(cacheKey) { convert(itemsArray, fileContent) }
  }

  suspend fun convert(itemsArray: JsonArray, fileContent: String): List<CompletionItem> =
      withContext(cpuDispatcher) {
        KslLogs.debug("Received {} completion items", itemsArray.size())

        // Process items in parallel
        val filteredItems =
            itemsArray
                .map { element ->
                  async {
                    try {
                      val item = element.asJsonObject
                      val converted = convertItem(item, fileContent)

                      if (
                          converted.ideLabel.isBlank() ||
                              converted.ideLabel == "K" ||
                              converted.ideLabel == "Keyword"
                      ) {
                        null
                      } else {
                        KslLogs.trace(
                            "Converted completion item: label='{}', detail='{}', kind={}",
                            converted.ideLabel,
                            converted.detail,
                            converted.completionKind,
                        )
                        converted
                      }
                    } catch (e: Exception) {
                      KslLogs.warn("Failed to convert completion item: {}", e.message)
                      null
                    }
                  }
                }
                .awaitAll()
                .filterNotNull()

        KslLogs.debug("Filtered to {} useful completion items", filteredItems.size)
        filteredItems
      }

  fun cleanup() {
    cpuDispatcher.close()
  }

  suspend fun convertFast(itemsArray: JsonArray, fileContent: String): List<CompletionItem> =
      withContext(Dispatchers.Default) {
        KslLogs.debug("Fast converting {} items", itemsArray.size())

        val results = mutableListOf<CompletionItem>()

        for (element in itemsArray) {
          try {
            val item = element.asJsonObject
            val converted = convertItemFast(item, fileContent)

            if (
                converted.ideLabel.isNotBlank() &&
                    converted.ideLabel != "K" &&
                    converted.ideLabel != "Keyword"
            ) {
              results.add(converted)
            }
          } catch (e: Exception) {
            // Skip problematic items silently
          }

          // Yield to prevent blocking
          if (results.size % 20 == 0) {
            yield()
          }
        }

        KslLogs.debug("Converted {} items", results.size)
        results
      }

  private fun convertItemFast(item: JsonObject, fileContent: String): CompletionItem {
    val label = item.get("label")?.asString ?: ""
    val detail = item.get("detail")?.asString ?: ""
    var insertText = item.get("insertText")?.asString
    val sortText = item.get("sortText")?.asString
    val kind = item.get("kind")?.asInt ?: 1
    val insertTextFormat = item.get("insertTextFormat")?.asInt

    val isSnippet = insertTextFormat == 2

    // Handle snippets - transform parameter names FIRST, then cleanup
    if (isSnippet && insertText != null) {
      KslLogs.debug("Original snippet: {}", insertText)

      // Extract parameter names from the detail/label
      val parameterNames = extractParameterNamesFromDetail(detail, label)

      if (parameterNames.isNotEmpty() && insertText.contains("\${")) {
        // Transform p0, p1, p2 to actual parameter names
        insertText = snippetTransformer.transformSnippet(insertText, parameterNames)
        KslLogs.debug("After transformation: {}", insertText)
      }

      // Now cleanup the snippet syntax
      if (insertText.contains("\${") || insertText.contains("$")) {
        val (cleanedText, cursorOffset) = processSnippetWithCursor(insertText)
        insertText = cleanedText
        KslLogs.debug("After cleanup: {}, cursor at: {}", cleanedText, cursorOffset)
      }
    }

    // Rest of the code remains the same...
    val additionalTextEdits = item.getAsJsonArray("additionalTextEdits")
    val importEdit = extractImportFromAdditionalEdits(additionalTextEdits)

    val additionalEdits = mutableListOf<TextEdit>()

    if (importEdit != null) {
      val (line, importText) = parseImportStatement(importEdit, fileContent)
      additionalEdits.add(
          TextEdit(
              range = Range(start = Position(line, 0), end = Position(line, 0)),
              newText = importText,
          )
      )
    } else {
      val tempItem =
          CompletionItem(
              ideLabel = label,
              detail = detail,
              insertText = insertText,
              insertTextFormat = null,
              sortText = sortText,
              command = null,
              completionKind = mapCompletionKind(kind),
              matchLevel = MatchLevel.NO_MATCH,
              additionalTextEdits = null,
              data = null,
          )

      val fqn = importResolver.needsImport(tempItem, fileContent)
      if (fqn != null) {
        val (line, importText) = importResolver.generateImportEdit(fqn, fileContent)
        additionalEdits.add(
            TextEdit(
                range = Range(start = Position(line, 0), end = Position(line, 0)),
                newText = importText,
            )
        )
      }
    }

    return CompletionItem(
        ideLabel = label,
        detail = detail,
        insertText = insertText,
        insertTextFormat = null,
        sortText = sortText,
        command = null,
        completionKind = mapCompletionKind(kind),
        matchLevel = MatchLevel.NO_MATCH,
        additionalTextEdits = if (additionalEdits.isNotEmpty()) additionalEdits else null,
        data = null,
    )
  }

  private fun convertItem(item: JsonObject, fileContent: String): CompletionItem {
    val label = item.get("label")?.asString ?: ""
    val detail = item.get("detail")?.asString ?: ""
    var insertText = item.get("insertText")?.asString
    val sortText = item.get("sortText")?.asString
    val kind = item.get("kind")?.asInt ?: 1
    val insertTextFormat = item.get("insertTextFormat")?.asInt

    val isSnippet = insertTextFormat == 2

    // Transform snippet if needed with enhanced cleanup
    if (isSnippet && insertText != null && insertText.contains("\${")) {
      KslLogs.debug("Original snippet: {}", insertText)

      val parameterNames = extractParameterNamesFromDetail(detail, label)

      if (parameterNames.isNotEmpty()) {
        insertText = snippetTransformer.transformSnippet(insertText, parameterNames)
        KslLogs.debug("Transformed snippet: {}", insertText)
      }

      // Apply enhanced cleanup with cursor positioning
      val (cleanedText, cursorOffset) = processSnippetWithCursor(insertText)
      insertText = cleanedText

      if (cursorOffset != null) {
        KslLogs.debug("Final snippet with cursor at {}: {}", cursorOffset, cleanedText)
      } else {
        KslLogs.debug("Final cleaned snippet: {}", cleanedText)
      }
    }

    // Extract import info from LSP server's additionalTextEdits
    val additionalTextEdits = item.getAsJsonArray("additionalTextEdits")
    val serverImportEdit = extractImportFromAdditionalEdits(additionalTextEdits)

    // Generate additional edits if needed
    val additionalEdits = mutableListOf<TextEdit>()

    if (serverImportEdit != null) {
      // Use server-provided import
      val (line, importText) = parseImportStatement(serverImportEdit, fileContent)
      additionalEdits.add(
          TextEdit(
              range = Range(start = Position(line, 0), end = Position(line, 0)),
              newText = importText,
          )
      )
      KslLogs.debug("Using server import: {}", importText.trim())
    } else {
      // Check if we need to generate import ourselves
      val tempItem =
          CompletionItem(
              ideLabel = label,
              detail = detail,
              insertText = insertText,
              insertTextFormat = null,
              sortText = sortText,
              command = null,
              completionKind = mapCompletionKind(kind),
              matchLevel = MatchLevel.NO_MATCH,
              additionalTextEdits = null,
              data = null,
          )

      val fqn = importResolver.needsImport(tempItem, fileContent)
      if (fqn != null) {
        val (line, importText) = importResolver.generateImportEdit(fqn, fileContent)
        additionalEdits.add(
            TextEdit(
                range = Range(start = Position(line, 0), end = Position(line, 0)),
                newText = importText,
            )
        )
        KslLogs.debug("Generated import: {}", importText.trim())
      }
    }

    return CompletionItem(
        ideLabel = label,
        detail = detail,
        insertText = insertText,
        insertTextFormat = null,
        sortText = sortText,
        command = null,
        completionKind = mapCompletionKind(kind),
        matchLevel = MatchLevel.NO_MATCH,
        additionalTextEdits = if (additionalEdits.isNotEmpty()) additionalEdits else null,
        data = null,
    )
  }

  /** Extracts import statement from LSP server's additionalTextEdits */
  private fun extractImportFromAdditionalEdits(edits: JsonArray?): String? {
    if (edits == null || edits.size() == 0) return null

    try {
      for (edit in edits) {
        val editObj = edit.asJsonObject
        val newText = editObj.get("newText")?.asString ?: continue

        // Check if this is an import statement
        val trimmed = newText.trim()
        if (trimmed.startsWith("import ")) {
          return trimmed
        }
      }
    } catch (e: Exception) {
      log.error("Failed to extract import from additionalTextEdits", e)
    }

    return null
  }

  /** Parses import statement and determines insertion position */
  private fun parseImportStatement(
      importStatement: String,
      fileContent: String,
  ): Pair<Int, String> {
    val cleanImport = importStatement.trim()
    val lines = fileContent.split("\n")

    var insertLine = 0
    var lastImportIndex = -1
    var packageIndex = -1

    for ((index, line) in lines.withIndex()) {
      val trimmed = line.trim()
      when {
        trimmed.startsWith("package ") -> packageIndex = index
        trimmed.startsWith("import ") -> lastImportIndex = index
        trimmed.isNotEmpty() &&
            !trimmed.startsWith("//") &&
            !trimmed.startsWith("/*") &&
            lastImportIndex >= 0 -> break
      }
    }

    insertLine =
        when {
          lastImportIndex >= 0 -> lastImportIndex + 1
          packageIndex >= 0 -> packageIndex + 2
          else -> 0
        }

    return Pair(insertLine, "$cleanImport\n")
  }

  private fun extractParameterNamesFromDetail(detail: String, label: String): List<String> {
    var signature = detail

    if (!detail.contains("(") && label.contains("(")) {
      signature = label
    }

    return snippetTransformer.extractParameterNames(signature)
  }

  /**
   * Enhanced snippet cleanup that properly handles placeholders and cursor positioning Removes
   * parameter placeholders like p0, p1, p2 and positions cursor correctly
   */
  private fun cleanupSnippet(snippet: String): String {
    var result = snippet

    // Step 1: Handle ${n:placeholder} format - extract the placeholder text
    result =
        result.replace("""\$\{(\d+):([^}]+)\}""".toRegex()) { matchResult ->
          val tabstop = matchResult.groupValues[1].toIntOrNull() ?: 0
          val placeholder = matchResult.groupValues[2]

          // For parameter placeholders like "p0", "p1", just remove them
          // For meaningful placeholders, keep them
          if (placeholder.matches("""p\d+""".toRegex())) {
            "" // Remove generic parameter placeholders
          } else {
            placeholder // Keep meaningful placeholders
          }
        }

    // Step 2: Handle ${n} format - just remove them
    result = result.replace("""\$\{\d+\}""".toRegex(), "")

    // Step 3: Handle $n format (shorthand) - remove them
    result = result.replace("""\$\d+""".toRegex(), "")

    // Step 4: Unescape dollar signs
    result = result.replace("\\$", "$")

    // Step 5: Position cursor between parentheses if empty
    // e.g., "makeText()" should become "makeText(¶)" where ¶ is cursor position
    if (result.contains("()")) {
      result = result.replace("()", "(¶)")
    }

    return result
  }

  /**
   * Process the snippet to extract cursor position information Returns a pair of (cleanedText,
   * cursorOffset)
   */
  fun processSnippetWithCursor(snippet: String): Pair<String, Int?> {
    val cleaned = cleanupSnippet(snippet)

    // Find cursor marker
    val cursorIndex = cleaned.indexOf("¶")

    if (cursorIndex == -1) {
      // No cursor marker - try to position cursor intelligently
      // If there are parentheses, position cursor inside them
      val parenIndex = cleaned.indexOf("(")
      if (parenIndex != -1) {
        return Pair(cleaned, parenIndex + 1)
      }
      return Pair(cleaned, null)
    }

    // Remove the marker and return position
    val finalText = cleaned.replace("¶", "")
    return Pair(finalText, cursorIndex)
  }

  private fun mapCompletionKind(kind: Int): CompletionItemKind {
    return when (kind) {
      1 -> CompletionItemKind.NONE
      2 -> CompletionItemKind.METHOD
      3 -> CompletionItemKind.FUNCTION
      4 -> CompletionItemKind.CONSTRUCTOR
      5 -> CompletionItemKind.FIELD
      6 -> CompletionItemKind.VARIABLE
      7 -> CompletionItemKind.CLASS
      8 -> CompletionItemKind.INTERFACE
      9 -> CompletionItemKind.MODULE
      10 -> CompletionItemKind.PROPERTY
      12 -> CompletionItemKind.VALUE
      13 -> CompletionItemKind.ENUM
      14 -> CompletionItemKind.KEYWORD
      15 -> CompletionItemKind.SNIPPET
      20 -> CompletionItemKind.ENUM_MEMBER
      25 -> CompletionItemKind.TYPE_PARAMETER
      else -> CompletionItemKind.NONE
    }
  }
}
