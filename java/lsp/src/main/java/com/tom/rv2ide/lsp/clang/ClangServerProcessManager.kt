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

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tom.rv2ide.lsp.models.DiagnosticResult
import com.tom.rv2ide.utils.Environment
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ClangServerProcessManager(private val context: Context) {

  companion object {
    private val log = LoggerFactory.getLogger(ClangServerProcessManager::class.java)
  }

  private val gson = Gson()
  private var process: Process? = null
  private var writer: OutputStreamWriter? = null
  private var reader: BufferedReader? = null
  private val nextId = AtomicInteger(1)
  private val pendingRequests = ConcurrentHashMap<Int, (JsonObject?) -> Unit>()
  private val requestTimestamps = ConcurrentHashMap<Int, Long>()
  private var diagnosticsCallback: ((DiagnosticResult) -> Unit)? = null
  private val notificationHandler = ClangNotificationHandler()

  fun setDiagnosticsCallback(callback: (DiagnosticResult) -> Unit) {
    diagnosticsCallback = callback
    notificationHandler.setDiagnosticsCallback(callback)
  }

  fun startServer(compileCommandsPath: String?) {
    if (process?.isAlive == true) {
      ClangLogs.debug("Server already running")
      return
    }

    ClangLogs.info("Starting Clang Language Server...")

    val clangdExec = File(Environment.PREFIX, "bin/clangd")

    if (!clangdExec.exists()) {
      ClangLogs.error("Clangd not found at: {}", clangdExec.absolutePath)
      return
    }

    val command =
        mutableListOf(
            clangdExec.absolutePath,
            "--background-index",
            "--clang-tidy",
            "--completion-style=detailed",
            "--header-insertion=iwyu",
            "--pch-storage=memory",
        )

    if (compileCommandsPath != null) {
      command.add("--compile-commands-dir=$compileCommandsPath")
    }

    val processBuilder = ProcessBuilder(command).apply { redirectErrorStream(false) }

    try {
      process = processBuilder.start()
      val rawInput = process!!.inputStream
      val rawOutput = process!!.outputStream

      writer = OutputStreamWriter(process!!.outputStream, StandardCharsets.UTF_8)
      reader = BufferedReader(InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8))

      startReaderThread()
      startErrorReaderThread()

      ClangLogs.info("Clangd started successfully")
    } catch (e: Exception) {
      ClangLogs.error("Failed to start clangd", e)
    }
  }

  fun sendRequest(method: String, params: JsonObject, callback: (JsonObject?) -> Unit) {
    val id = nextId.getAndIncrement()
    pendingRequests[id] = callback
    requestTimestamps[id] = System.currentTimeMillis()

    val payload =
        JsonObject().apply {
          addProperty("jsonrpc", "2.0")
          addProperty("id", id)
          addProperty("method", method)
          add("params", params)
        }

    ClangLogs.info(">>> Sending request ID {} ({})", id, method)
    ClangLogs.debug("Request payload: {}", gson.toJson(payload).take(500))
    sendMessage(payload)
  }

  fun sendNotification(method: String, params: JsonObject) {
    val payload =
        JsonObject().apply {
          addProperty("jsonrpc", "2.0")
          addProperty("method", method)
          add("params", params)
        }

    ClangLogs.debug(">>> Sending notification: {}", method)
    sendMessage(payload)
  }

  private fun sendMessage(payload: JsonObject) {
    val data = gson.toJson(payload)
    val w =
        writer
            ?: run {
              ClangLogs.error("Cannot send message: writer is null")
              return
            }

    synchronized(w) {
      try {
        val contentBytes = data.toByteArray(StandardCharsets.UTF_8)
        w.write("Content-Length: ${contentBytes.size}\r\n\r\n")
        w.write(data)
        w.flush()
        ClangLogs.debug("Message sent successfully ({} bytes)", contentBytes.size)
      } catch (e: Exception) {
        ClangLogs.error("Failed to send message", e)
      }
    }
  }

  private fun startReaderThread() {
    val r = reader ?: return
    Thread(
            {
              ClangLogs.error("🔥🔥🔥 READER THREAD STARTED 🔥🔥🔥")

              try {
                val buffer = CharArray(8192)
                var leftover = ""

                while (true) {
                  val bytesRead = r.read(buffer)
                  if (bytesRead == -1) {
                    ClangLogs.error("🔥 EOF reached, server closed")
                    return@Thread
                  }

                  val chunk = leftover + String(buffer, 0, bytesRead)
                  leftover = processChunk(chunk)
                }
              } catch (e: Exception) {
                ClangLogs.error("🔥🔥🔥 READER THREAD DIED 🔥🔥🔥", e)
                e.printStackTrace()
              }
            },
            "clangd-stdio-reader",
        )
        .start()
  }

  private fun processChunk(chunk: String): String {
    var remaining = chunk
    var processedAny = false

    while (remaining.isNotEmpty()) {
      // Look for Content-Length header
      val contentLengthMatch =
          Regex("Content-Length:\\s*(\\d+)", RegexOption.IGNORE_CASE).find(remaining)

      if (contentLengthMatch == null) {
        // No header found, this might be a malformed message without headers
        ClangLogs.warn("🔥 No Content-Length header found, looking for JSON directly")
        val jsonStart = remaining.indexOf('{')
        if (jsonStart >= 0) {
          val potentialJson = remaining.substring(jsonStart)
          try {
            // Try to parse as JSON
            gson.fromJson(potentialJson, JsonObject::class.java)
            ClangLogs.debug("🔥 Found standalone JSON, processing")
            handleMessage(potentialJson)
            remaining = ""
            processedAny = true
            break
          } catch (e: Exception) {
            // Not valid JSON, might be partial message
            ClangLogs.debug("🔥 Not valid JSON, keeping as leftover")
            return remaining
          }
        } else {
          // No JSON found, discard this chunk
          ClangLogs.warn("🔥 No JSON found in chunk, discarding")
          return ""
        }
      }

      val contentLength = contentLengthMatch.groupValues[1].toInt()
      val headerEnd = contentLengthMatch.range.last + 1

      // Find the double CRLF that ends headers
      val headerSection = remaining.substring(0, headerEnd)
      val bodyStartIndex = remaining.indexOf("\r\n\r\n", headerEnd)

      if (bodyStartIndex == -1) {
        // Headers not complete yet
        ClangLogs.debug("🔥 Headers not complete, waiting for more data")
        return remaining
      }

      val actualBodyStart = bodyStartIndex + 4 // Skip \r\n\r\n
      val totalMessageLength = actualBodyStart + contentLength

      if (remaining.length < totalMessageLength) {
        // Body not complete yet
        ClangLogs.debug("🔥 Body not complete (have ${remaining.length}, need $totalMessageLength)")
        return remaining
      }

      // Extract and process the complete message
      val jsonBody = remaining.substring(actualBodyStart, totalMessageLength)
      ClangLogs.debug("🔥 Processing message with $contentLength bytes")
      handleMessage(jsonBody)

      // Move to next message
      remaining = remaining.substring(totalMessageLength)
      processedAny = true
    }

    // If we didn't process anything but have data, it might be malformed
    if (!processedAny && chunk.isNotEmpty()) {
      ClangLogs.warn("🔥 Could not process chunk, attempting recovery")
      return attemptRecovery(chunk)
    }

    return remaining
  }

  private fun attemptRecovery(chunk: String): String {
    // Look for JSON objects in the chunk and try to extract them
    var remaining = chunk
    var jsonStart = remaining.indexOf('{')

    while (jsonStart >= 0) {
      var jsonEnd = -1
      var braceCount = 0
      var inString = false
      var escapeNext = false

      // Find the matching closing brace
      for (i in jsonStart until remaining.length) {
        val c = remaining[i]

        when {
          escapeNext -> escapeNext = false
          c == '\\' -> escapeNext = true
          c == '"' && !escapeNext -> inString = !inString
          !inString -> {
            when (c) {
              '{' -> braceCount++
              '}' -> {
                braceCount--
                if (braceCount == 0) {
                  jsonEnd = i + 1
                  break
                }
              }
            }
          }
        }
      }

      if (jsonEnd > jsonStart) {
        val potentialJson = remaining.substring(jsonStart, jsonEnd)
        try {
          gson.fromJson(potentialJson, JsonObject::class.java)
          ClangLogs.warn("🔥 Recovered JSON from malformed stream")
          handleMessage(potentialJson)
          remaining = remaining.substring(jsonEnd)
          jsonStart = remaining.indexOf('{')
        } catch (e: Exception) {
          // Not valid JSON, move to next potential start
          jsonStart = remaining.indexOf('{', jsonStart + 1)
        }
      } else {
        // No complete JSON found
        break
      }
    }

    return remaining
  }

  private fun startErrorReaderThread() {
    val errorReader =
        BufferedReader(InputStreamReader(process!!.errorStream, StandardCharsets.UTF_8))
    Thread(
            {
              try {
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                  ClangLogs.warn("Clangd stderr: {}", line)
                }
              } catch (e: Exception) {
                ClangLogs.error("Error in error reader thread", e)
              }
            },
            "clangd-error-reader",
        )
        .start()
  }

  private fun handleMessage(json: String) {
    try {

      ClangLogs.error("🔥🔥🔥 handleMessage called with {} chars", json.length)
      ClangLogs.error("🔥🔥🔥 Message start: {}", json.take(100))

      val obj =
          try {
            gson.fromJson(json, JsonObject::class.java)
          } catch (e: com.google.gson.JsonSyntaxException) {
            ClangLogs.error("JSON syntax error, skipping message")
            ClangLogs.error("Full message: {}", json)
            return
          } catch (e: Exception) {
            ClangLogs.error("Unexpected JSON parse error", e)
            ClangLogs.error("Message preview: {}", json.take(200))
            return
          }

      ClangLogs.error(
          "🔥 Parsed successfully, has ID: {}, has method: {}",
          obj.has("id"),
          obj.has("method"),
      )

      if (obj.has("id")) {
        val id = obj.get("id").asInt
        ClangLogs.error("*** MESSAGE HAS ID: {} ***", id)
        ClangLogs.error("*** Checking for callback in pendingRequests ***")
        ClangLogs.error("*** PendingRequests keys: {} ***", pendingRequests.keys())

        val callback = pendingRequests.remove(id)
        val timestamp = requestTimestamps.remove(id)

        val elapsed =
            if (timestamp != null) {
              System.currentTimeMillis() - timestamp
            } else {
              -1L
            }

        ClangLogs.info("<<< Response for ID {} ({}ms)", id, elapsed)

        if (callback == null) {
          ClangLogs.error("*** NO CALLBACK FOUND FOR ID {} ***", id)
          ClangLogs.error("*** Available callbacks: {} ***", pendingRequests.keys())
          return
        }

        ClangLogs.error("*** CALLBACK FOUND! Invoking... ***")

        if (obj.has("error")) {
          val error = obj.getAsJsonObject("error")
          ClangLogs.error(
              "LSP Error: code={}, message={}",
              error.get("code")?.asInt,
              error.get("message")?.asString,
          )
          callback.invoke(null)
        } else if (obj.has("result")) {
          val result = obj.get("result")

          try {
            when {
              result.isJsonNull -> {
                ClangLogs.debug("Null result for request {}", id)
                callback.invoke(null)
              }
              result.isJsonArray -> {
                ClangLogs.debug("Array result with {} items", result.asJsonArray.size())
                val wrapped = JsonObject().apply { add("items", result.asJsonArray) }
                callback.invoke(wrapped)
              }
              result.isJsonObject -> {
                ClangLogs.debug("Object result")
                callback.invoke(result.asJsonObject)
              }
              result.isJsonPrimitive -> {
                ClangLogs.debug("Primitive result")
                val wrapped = JsonObject().apply { add("result", result) }
                callback.invoke(wrapped)
              }
              else -> {
                ClangLogs.warn("Unknown result type")
                callback.invoke(null)
              }
            }
          } catch (e: Exception) {
            ClangLogs.error("Error processing result for ID {}", id, e)
            callback.invoke(null)
          }
        } else {
          ClangLogs.warn("Response has neither result nor error")
          callback.invoke(null)
        }
      } else if (obj.has("method")) {
        val method = obj.get("method")?.asString
        ClangLogs.debug("<<< Notification: {}", method)
        try {
          notificationHandler.handle(obj)
        } catch (e: Exception) {
          ClangLogs.error("Error handling notification: {}", method, e)
        }
      } else {
        ClangLogs.warn("Message has neither id nor method")
        ClangLogs.warn("Message: {}", json.take(200))
      }
    } catch (e: Exception) {
      ClangLogs.error("FATAL error in handleMessage", e)
      ClangLogs.error("Message length: {}", json.length)
      ClangLogs.error("Message preview: {}", json.take(200))
    }
  }

  fun shutdown() {
    ClangLogs.info("Shutting down clangd process...")
    try {
      sendRequest("shutdown", JsonObject()) {}
      Thread.sleep(100)
      sendNotification("exit", JsonObject())
      Thread.sleep(100)
    } catch (e: Exception) {
      ClangLogs.error("Error during shutdown", e)
    }

    try {
      writer?.close()
    } catch (e: Exception) {}
    try {
      reader?.close()
    } catch (e: Exception) {}
    try {
      process?.destroy()
      ClangLogs.info("Process destroyed")
    } catch (e: Exception) {}

    process = null
    pendingRequests.clear()
    requestTimestamps.clear()
    ClangLogs.info("Clangd shutdown complete")
  }
}
