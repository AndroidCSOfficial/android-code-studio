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

package com.tom.rv2ide.lsp.kotlin.providers

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.tom.rv2ide.lsp.kotlin.KotlinServerProcessManager
import com.tom.rv2ide.lsp.kotlin.KslLogs
import com.tom.rv2ide.lsp.kotlin.etc.LspFeatures
import com.tom.rv2ide.lsp.models.CodeFormatResult
import com.tom.rv2ide.lsp.models.FormatCodeParams
import com.tom.rv2ide.lsp.models.IndexedTextEdit
import java.nio.file.Path
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinCodeFormatProvider(private val processManager: KotlinServerProcessManager) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinCodeFormatProvider::class.java)
    private const val FORMAT_TIMEOUT = 15000L
  }

  fun format(filePath: Path, params: FormatCodeParams?): CodeFormatResult {
    if (params == null) {
      KslLogs.warn("Format params is null")
      return CodeFormatResult(false, mutableListOf())
    }

    if (!(filePath.toString().endsWith(".kt") || filePath.toString().endsWith(".kts"))) {
      KslLogs.debug("Not a Kotlin file: {}", filePath)
      return CodeFormatResult(false, mutableListOf())
    }

    KslLogs.info("Starting format for: {}", filePath)

    return runBlocking {
      try {
        formatDocument(filePath, params)
      } catch (e: Exception) {
        KslLogs.error("Error formatting document", e)
        CodeFormatResult(false, mutableListOf())
      }
    }
  }

  private suspend fun formatDocument(filePath: Path, params: FormatCodeParams): CodeFormatResult {
    val deferred = CompletableDeferred<CodeFormatResult>()

    val uri = filePath.toUri().toString()

    // Read current content for offset calculation
    val content = params.content?.toString() ?: filePath.toFile().readText()

    sendFormattingConfiguration()

    val lspParams =
        JsonObject().apply {
          add("textDocument", JsonObject().apply { addProperty("uri", uri) })
          add(
              "options",
              JsonObject().apply {
                val currentIndent =
                    when (LspFeatures.getCodeFormatStyle()) {
                      "google",
                      "facebook" -> 2
                      "kotlinlang" -> 4
                      else -> 4
                    }
                addProperty("tabSize", currentIndent)
                addProperty("insertSpaces", true)
                addProperty("trimTrailingWhitespace", true)
                addProperty("insertFinalNewline", true)
                addProperty("trimFinalNewlines", true)
              },
          )
        }

    KslLogs.info("Sending format request for: {}", uri)
    KslLogs.debug("Format params: {}", lspParams.toString())

    processManager.sendRequest("textDocument/formatting", lspParams) { result ->
      try {
        KslLogs.info("Received format response")

        if (result == null) {
          KslLogs.warn("Format result is null - no changes needed or error occurred")
          deferred.complete(CodeFormatResult(false, mutableListOf()))
          return@sendRequest
        }

        KslLogs.debug("Format result: {}", result.toString())

        val edits = convertToIndexedTextEdits(result, content)
        val success = edits.isNotEmpty()

        if (success) {
          KslLogs.info("Format successful: {} edits", edits.size)
          for (i in edits.indices) {
            val edit = edits[i]
            val preview = edit.newText.take(50).split('\n').joinToString(" ")
            KslLogs.debug(
                "Edit {}: [{} - {}] length={}, preview='{}'",
                i + 1,
                edit.start,
                edit.end,
                edit.newText.length,
                preview,
            )
          }
        } else {
          KslLogs.warn("Format returned no edits")
        }

        // Create result with IndexedTextEdits
        val formatResult = CodeFormatResult(success)
        edits.forEach { formatResult.indexedTextEdits.add(it) }

        deferred.complete(formatResult)
      } catch (e: Exception) {
        KslLogs.error("Error processing format result", e)
        deferred.complete(CodeFormatResult(false, mutableListOf()))
      }
    }

    val result = withTimeoutOrNull(FORMAT_TIMEOUT) { deferred.await() }

    if (result == null) {
      KslLogs.error("Format request timed out after {}ms", FORMAT_TIMEOUT)
      return CodeFormatResult(false, mutableListOf())
    }

    return result
  }

  private fun sendFormattingConfiguration() {
    val style = LspFeatures.getCodeFormatStyle() ?: "google"
    val indentSize =
        when (style) {
          "google",
          "facebook" -> 2
          "kotlinlang" -> 4
          else -> 4
        }

    val configParams =
        JsonObject().apply {
          add(
              "settings",
              JsonObject().apply {
                add(
                    "kotlin",
                    JsonObject().apply {
                      add(
                          "formatting",
                          JsonObject().apply {
                            addProperty("formatter", "ktfmt")
                            add(
                                "ktfmt",
                                JsonObject().apply {
                                  addProperty("style", style)
                                  addProperty("indent", indentSize)
                                  addProperty("maxWidth", 100)
                                  addProperty("removeUnusedImports", true)
                                },
                            )
                          },
                      )
                    },
                )
              },
          )
        }

    processManager.sendNotification("workspace/didChangeConfiguration", configParams)
    KslLogs.debug("Sent formatting configuration: style={}, indent={}", style, indentSize)
  }

  private fun convertToIndexedTextEdits(
      result: JsonObject?,
      content: String,
  ): List<IndexedTextEdit> {
    try {
      if (result == null) {
        KslLogs.debug("Result is null")
        return emptyList()
      }

      val editsArray: JsonArray =
          when {
            result.has("result") -> {
              val resultField = result.get("result")
              when {
                resultField.isJsonArray -> {
                  KslLogs.debug("Result has 'result' field with JsonArray")
                  resultField.asJsonArray
                }
                resultField.isJsonNull -> {
                  KslLogs.debug("Result field is null")
                  return emptyList()
                }
                else -> {
                  KslLogs.warn("Result field is not an array")
                  return emptyList()
                }
              }
            }
            result.isJsonArray -> {
              KslLogs.debug("Result is directly a JsonArray")
              result.asJsonArray
            }
            else -> {
              KslLogs.warn("Result is neither object with 'result' field nor array")
              return emptyList()
            }
          }

      if (editsArray.size() == 0) {
        KslLogs.debug("Edits array is empty")
        return emptyList()
      }

      KslLogs.debug("Converting {} edits", editsArray.size())

      // Split content into lines for offset calculation
      val lines = content.split("\n")

      return editsArray.mapNotNull { element ->
        try {
          val edit = element.asJsonObject
          val range = edit.getAsJsonObject("range")
          val start = range.getAsJsonObject("start")
          val end = range.getAsJsonObject("end")
          val newText = edit.get("newText")?.asString ?: ""

          val startLine = start.get("line").asInt
          val startChar = start.get("character").asInt
          val endLine = end.get("line").asInt
          val endChar = end.get("character").asInt

          KslLogs.debug(
              "LSP edit: [{},{}] to [{},{}], text length: {}",
              startLine,
              startChar,
              endLine,
              endChar,
              newText.length,
          )

          // Convert line/column to character offsets
          val startOffset = lineColumnToOffset(lines, startLine, startChar)
          val endOffset = lineColumnToOffset(lines, endLine, endChar)

          KslLogs.debug("Converted to offsets: {} to {}", startOffset, endOffset)

          val indexedEdit = IndexedTextEdit()
          indexedEdit.newText = newText
          indexedEdit.start = startOffset
          indexedEdit.end = endOffset
          indexedEdit
        } catch (e: Exception) {
          KslLogs.error("Error converting text edit", e)
          null
        }
      }
    } catch (e: Exception) {
      KslLogs.error("Error parsing format result", e)
      return emptyList()
    }
  }

  private fun lineColumnToOffset(lines: List<String>, line: Int, column: Int): Int {
    var offset = 0

    // Add lengths of all lines before the target line
    for (i in 0 until minOf(line, lines.size)) {
      offset += lines[i].length + 1 // +1 for newline character
    }

    // Add the column offset within the target line
    if (line < lines.size) {
      offset += minOf(column, lines[line].length)
    } else {
      // If line is beyond content, just add the column
      offset += column
    }

    return offset
  }
}
