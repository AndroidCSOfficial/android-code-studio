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

package com.tom.rv2ide.lsp.clang

import com.google.gson.JsonObject
import com.tom.rv2ide.lsp.models.*
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ClangRequestHandler(
    private val processManager: ClangServerProcessManager,
    private val documentManager: ClangDocumentManager,
) {

  companion object {
    private val log = LoggerFactory.getLogger(ClangRequestHandler::class.java)
    private const val COMPLETION_TIMEOUT = 10000L
    private const val DEBOUNCE_DELAY = 100L // Reduced from 150
  }

  private val completionConverter = ClangCompletionConverter()
  private val lastSyncTime = ConcurrentHashMap<String, Long>()
  private val syncThrottleMs = 200L // Reduced from 300
  private val lastCompletionRequest = AtomicLong(0)
  private var activeCompletionJob: Job? = null

  suspend fun complete(params: CompletionParams): CompletionResult = coroutineScope {
    val startTime = System.currentTimeMillis()
    ClangLogs.error("========================================")
    ClangLogs.error("🔥 COMPLETION REQUEST START")
    ClangLogs.error("========================================")
    ClangLogs.error("File: {}", params.file)
    ClangLogs.error("Position: line={}, col={}", params.position.line, params.position.column)

    if (params.position.line < 0 || params.position.column < 0) {
      ClangLogs.error("ABORT: Invalid position")
      return@coroutineScope CompletionResult(emptyList())
    }

    // DON'T cancel here - just track timestamps for debouncing
    val requestTimestamp = System.currentTimeMillis()
    lastCompletionRequest.set(requestTimestamp)

    delay(DEBOUNCE_DELAY)

    if (lastCompletionRequest.get() != requestTimestamp) {
      ClangLogs.error("ABORT: Request superseded")
      return@coroutineScope CompletionResult(emptyList())
    }

    val deferred = CompletableDeferred<CompletionResult>()

    try {
      val uri = params.file.toUri().toString()

      ClangLogs.error("Checking document state for URI: {}", uri)
      val isDocOpen = documentManager.isDocumentOpen(uri)
      ClangLogs.error("Document open status: {}", isDocOpen)

      if (!isDocOpen) {
        ClangLogs.error("Document not open, opening now...")
        withContext(Dispatchers.IO) {
          val currentContent = params.content?.toString() ?: params.file.toFile().readText()
          documentManager.ensureDocumentOpen(params.file, currentContent)
        }
        delay(1500)
      } else {
        val currentContent = params.content?.toString()
        if (currentContent != null && currentContent.isNotEmpty()) {
          val currentTime = System.currentTimeMillis()
          val lastSync = lastSyncTime[uri] ?: 0L

          if (currentTime - lastSync > syncThrottleMs) {
            ClangLogs.error("Syncing content changes...")
            withContext(Dispatchers.IO) {
              val currentVersion = documentManager.getDocumentVersion(uri)
              val newVersion = currentVersion + 1
              documentManager.notifyDocumentChange(params.file, currentContent, newVersion)
              documentManager.setDocumentVersion(uri, newVersion)
              lastSyncTime[uri] = currentTime
            }
            delay(300)
          } else {
            ClangLogs.error(
                "Skipping sync, too soon since last sync ({}ms ago)",
                currentTime - lastSync,
            )
          }
        }
      }

      val lspParams =
          JsonObject().apply {
            add("textDocument", JsonObject().apply { addProperty("uri", uri) })
            add(
                "position",
                JsonObject().apply {
                  addProperty("line", params.position.line)
                  addProperty("character", params.position.column)
                },
            )
            add("context", JsonObject().apply { addProperty("triggerKind", 1) })
          }

      ClangLogs.error("Sending completion request to clangd...")
      ClangLogs.error("Request URI: {}", uri)
      ClangLogs.error(
          "Request position: line={}, character={}",
          params.position.line,
          params.position.column,
      )

      processManager.sendRequest("textDocument/completion", lspParams) { result ->
        try {
          ClangLogs.error("=== COMPLETION CALLBACK INVOKED ===")
          ClangLogs.error(
              "Deferred isActive: {}, isCompleted: {}, isCancelled: {}",
              deferred.isActive,
              deferred.isCompleted,
              deferred.isCancelled,
          )
          ClangLogs.error("Result null? {}", result == null)

          if (deferred.isCompleted || deferred.isCancelled) {
            ClangLogs.error("!!! DEFERRED ALREADY COMPLETED/CANCELLED - IGNORING CALLBACK !!!")
            return@sendRequest
          }

          val items = mutableListOf<CompletionItem>()

          if (result != null) {
            ClangLogs.error("Result keys: {}", result.keySet())
            ClangLogs.error("Has 'items'? {}", result.has("items"))

            if (result.has("items")) {
              val itemsArray = result.getAsJsonArray("items")
              ClangLogs.error("Items array size: {}", itemsArray.size())

              val clangdItems = completionConverter.convert(itemsArray)
              items.addAll(clangdItems)
              ClangLogs.error("Converted {} clangd items", clangdItems.size)
            } else {
              // Try alternative result format
              if (result.has("isIncomplete") && result.has("items")) {
                val itemsArray = result.getAsJsonArray("items")
                ClangLogs.error("Alternative format - Items array size: {}", itemsArray.size())

                val clangdItems = completionConverter.convert(itemsArray)
                items.addAll(clangdItems)
                ClangLogs.error(
                    "Converted {} clangd items from alternative format",
                    clangdItems.size,
                )
              } else {
                ClangLogs.error(
                    "No 'items' key in result. Full result: {}",
                    result.toString().take(500),
                )
              }
            }
          } else {
            ClangLogs.error("Result is NULL - clangd returned error or no completions")
          }

          ClangLogs.error("Total completion items: {}", items.size)
          ClangLogs.error("About to call deferred.complete()...")

          val completed = deferred.complete(CompletionResult(items))
          ClangLogs.error("deferred.complete() returned: {}", completed)
        } catch (e: Exception) {
          ClangLogs.error("Error in completion callback: {}", e.message)
          e.printStackTrace()
          if (!deferred.isCompleted) {
            deferred.completeExceptionally(e)
          }
        }
      }

      val result =
          withTimeoutOrNull(COMPLETION_TIMEOUT) {
            ClangLogs.error("Waiting for deferred to complete...")
            deferred.await()
          }

      if (result == null) {
        ClangLogs.error(
            "COMPLETION TIMEOUT - deferred state: active={}, completed={}, cancelled={}",
            deferred.isActive,
            deferred.isCompleted,
            deferred.isCancelled,
        )
        // Don't return empty list on timeout, let the next request try again
        return@coroutineScope CompletionResult(emptyList())
      }

      val elapsed = System.currentTimeMillis() - startTime
      ClangLogs.error("Completion succeeded with {} items in {}ms", result.items.size, elapsed)
      return@coroutineScope result
    } catch (e: Exception) {
      ClangLogs.error("Exception in complete method: {}", e.message)
      ClangLogs.error("Exception type: {}", e.javaClass.name)
      e.printStackTrace()
      return@coroutineScope CompletionResult(emptyList())
    }
  }

  suspend fun findReferences(params: ReferenceParams): ReferenceResult =
      withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<ReferenceResult>()

        documentManager.ensureDocumentOpen(params.file)

        val lspParams =
            JsonObject().apply {
              add(
                  "textDocument",
                  JsonObject().apply { addProperty("uri", params.file.toUri().toString()) },
              )
              add(
                  "position",
                  JsonObject().apply {
                    addProperty("line", params.position.line)
                    addProperty("character", params.position.column)
                  },
              )
              add(
                  "context",
                  JsonObject().apply {
                    addProperty("includeDeclaration", params.includeDeclaration)
                  },
              )
            }

        processManager.sendRequest("textDocument/references", lspParams) { result ->
          val locations = convertToLocations(result)
          deferred.complete(ReferenceResult(locations))
        }

        withTimeoutOrNull(5000) { deferred.await() } ?: ReferenceResult(emptyList())
      }

  suspend fun findDefinition(params: DefinitionParams): DefinitionResult =
      withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<DefinitionResult>()

        documentManager.ensureDocumentOpen(params.file)

        val lspParams =
            JsonObject().apply {
              add(
                  "textDocument",
                  JsonObject().apply { addProperty("uri", params.file.toUri().toString()) },
              )
              add(
                  "position",
                  JsonObject().apply {
                    addProperty("line", params.position.line)
                    addProperty("character", params.position.column)
                  },
              )
            }

        processManager.sendRequest("textDocument/definition", lspParams) { result ->
          val locations = convertToLocations(result)
          deferred.complete(DefinitionResult(locations))
        }

        withTimeoutOrNull(5000) { deferred.await() } ?: DefinitionResult(emptyList())
      }

  suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp =
      withContext(Dispatchers.IO) {
        val deferred = CompletableDeferred<SignatureHelp>()

        documentManager.ensureDocumentOpen(params.file)

        val lspParams =
            JsonObject().apply {
              add(
                  "textDocument",
                  JsonObject().apply { addProperty("uri", params.file.toUri().toString()) },
              )
              add(
                  "position",
                  JsonObject().apply {
                    addProperty("line", params.position.line)
                    addProperty("character", params.position.column)
                  },
              )
            }

        processManager.sendRequest("textDocument/signatureHelp", lspParams) { result ->
          val help = convertToSignatureHelp(result)
          deferred.complete(help)
        }

        withTimeoutOrNull(5000) { deferred.await() } ?: SignatureHelp(emptyList(), 0, 0)
      }

  private fun convertToLocations(result: JsonObject?): List<com.tom.rv2ide.models.Location> {
    return result?.asJsonArray?.map { element ->
      val loc = element.asJsonObject
      val range = loc.getAsJsonObject("range")
      val start = range.getAsJsonObject("start")
      val end = range.getAsJsonObject("end")

      com.tom.rv2ide.models.Location(
          file = Paths.get(java.net.URI(loc.get("uri").asString)),
          range =
              com.tom.rv2ide.models.Range(
                  start =
                      com.tom.rv2ide.models.Position(
                          start.get("line").asInt,
                          start.get("character").asInt,
                      ),
                  end =
                      com.tom.rv2ide.models.Position(
                          end.get("line").asInt,
                          end.get("character").asInt,
                      ),
              ),
      )
    } ?: emptyList()
  }

  private fun convertToSignatureHelp(result: JsonObject?): SignatureHelp {
    if (result == null) return SignatureHelp(emptyList(), 0, 0)

    val signatures =
        result.getAsJsonArray("signatures")?.map { element ->
          val sig = element.asJsonObject
          val label = sig.get("label")?.asString ?: ""
          val doc = sig.get("documentation")?.asString ?: ""

          val parameters =
              sig.getAsJsonArray("parameters")?.map { paramElement ->
                val param = paramElement.asJsonObject
                ParameterInformation(
                    label = param.get("label")?.asString ?: "",
                    documentation =
                        MarkupContent(param.get("documentation")?.asString ?: "", MarkupKind.PLAIN),
                )
              } ?: emptyList()

          SignatureInformation(
              label = label,
              documentation = MarkupContent(doc, MarkupKind.PLAIN),
              parameters = parameters,
          )
        } ?: emptyList()

    return SignatureHelp(
        signatures,
        result.get("activeSignature")?.asInt ?: 0,
        result.get("activeParameter")?.asInt ?: 0,
    )
  }
}
