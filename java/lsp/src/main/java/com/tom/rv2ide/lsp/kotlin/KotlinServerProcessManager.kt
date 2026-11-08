/*
 * This file is part of AndroidCodeStudio.
 *
 * AndroidCodeStudio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidCodeStudio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.lsp.kotlin

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.tom.rv2ide.lsp.models.DiagnosticResult
import com.tom.rv2ide.utils.Environment
import java.io.*
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinServerProcessManager(context: Context) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinServerProcessManager::class.java)
    // Optimized buffer sizes for better performance
    private const val BUFFER_SIZE = 32768 // 32KB buffer
    private const val MAX_CONTENT_LENGTH = 10485760 // 10MB max message size
  }

  private val context: Context = context.applicationContext

  private val gson = Gson()
  private var process: Process? = null
  private var writer: BufferedWriter? = null
  private var reader: BufferedReader? = null
  private val nextId = AtomicInteger(1)
  private val pendingRequests = ConcurrentHashMap<Int, (JsonObject?) -> Unit>()
  private var diagnosticsCallback: ((DiagnosticResult) -> Unit)? = null
  private val notificationHandler = KotlinNotificationHandler()
  
  // Thread pool for handling responses asynchronously
  private val executorService = Executors.newFixedThreadPool(2)

  fun setDiagnosticsCallback(callback: (DiagnosticResult) -> Unit) {
    diagnosticsCallback = callback
    notificationHandler.setDiagnosticsCallback(callback)
  }

  fun startServer(classpathProvider: KotlinClasspathProvider) {
    if (process?.isAlive == true) {
      KslLogs.debug("Server already running")
      return
    }

    KslLogs.info("Starting Kotlin Language Server via distribution script...")

    val serverHome = Environment.SERVERS_KOTLIN_DIR
    
    // Look for the executable script in bin directory, with a fallback
    var executableFile = File(serverHome, "bin/kotlin-language-server")
    if (!executableFile.exists()) {
        executableFile = File(serverHome, "server/bin/kotlin-language-server")
    }

    if (!executableFile.exists()) {
      KslLogs.error("Server executable not found at: {}", executableFile.absolutePath)
      return
    }

    // Ensure the script has execute permissions
    executableFile.setExecutable(true)

    val javaExec = findJavaExecutable()
    val androidClasspath = classpathProvider.getClasspath()

    val javaHome =
        if (Environment.JAVA_HOME != null) {
          Environment.JAVA_HOME.absolutePath
        } else {
          Environment.PREFIX.absolutePath
        }
    
    val klsCacheDir = "${Environment.HOME}/klsCacheDir"
    
    // Execute the bash/sh script directly using 'sh' to avoid shebang issues on Android
    val command = listOf("sh", executableFile.absolutePath)

    val processBuilder =
        ProcessBuilder(command).apply {
          redirectErrorStream(false)
          directory(serverHome)
          environment().apply {
            put("JAVA_HOME", javaHome)

            val javaBinPath = File(javaExec).parent ?: ""
            val currentPath = get("PATH") ?: ""
            put("PATH", "$javaBinPath:$currentPath")

            put("KOTLIN_LSP_DISABLE_DEPENDENCY_RESOLUTION", "true")
            put("KOTLIN_LSP_USE_PREDEFINED_CLASSPATH", "true")
            put("KOTLIN_LSP_CLASSPATH", androidClasspath)
            put("CLASSPATH", androidClasspath)

            val sdkPath = classpathProvider.getAndroidSdkPath()
            if (sdkPath.isNotEmpty()) {
              put("ANDROID_SDK_ROOT", sdkPath)
              put("ANDROID_HOME", sdkPath)
            }

            KslLogs.info("Environment: JAVA_HOME={}", javaHome)
            KslLogs.info("Environment: PATH={}", get("PATH"))
          }
        }

    try {
      process = processBuilder.start()

      // Use buffered streams with larger buffers for better performance
      writer = BufferedWriter(
          OutputStreamWriter(process!!.outputStream, StandardCharsets.UTF_8),
          BUFFER_SIZE
      )
      reader = BufferedReader(
          InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8),
          BUFFER_SIZE
      )

      startReaderThread()
      startErrorReaderThread()

      // Reduced wait time for faster startup
      Thread.sleep(1000)

      sendInitialization()

      KslLogs.info("Server started successfully with JAVA_HOME: {}", javaHome)
    } catch (e: Exception) {
      KslLogs.error("Failed to start server", e)
    }
  }

  private fun sendInitialization() {
    val initParams =
        JsonObject().apply {
          addProperty("processId", android.os.Process.myPid())
          add("rootUri", null)

          add(
              "capabilities",
              JsonObject().apply {
                add(
                    "textDocument",
                    JsonObject().apply {
                      add(
                          "completion",
                          JsonObject().apply {
                            add(
                                "completionItem",
                                JsonObject().apply {
                                  addProperty("snippetSupport", true)
                                  addProperty("commitCharactersSupport", true)
                                  add(
                                      "documentationFormat",
                                      gson.toJsonTree(listOf("markdown", "plaintext")),
                                  )
                                  addProperty("deprecatedSupport", true)
                                  addProperty("preselectSupport", true)

                                  add(
                                      "resolveSupport",
                                      gson.toJsonTree(
                                          mapOf(
                                              "properties" to
                                                  listOf(
                                                      "documentation",
                                                      "detail",
                                                      "additionalTextEdits",
                                                  )
                                          )
                                      ),
                                  )
                                },
                            )
                            addProperty("contextSupport", true)
                          },
                      )

                      add(
                          "signatureHelp",
                          JsonObject().apply {
                            add(
                                "signatureInformation",
                                JsonObject().apply {
                                  add(
                                      "documentationFormat",
                                      gson.toJsonTree(listOf("markdown", "plaintext")),
                                  )
                                  add(
                                      "parameterInformation",
                                      JsonObject().apply {
                                        addProperty("labelOffsetSupport", true)
                                      },
                                  )
                                  addProperty("activeParameterSupport", true)
                                },
                            )
                            addProperty("contextSupport", true)
                          },
                      )
                    },
                )
                add(
                    "workspace",
                    JsonObject().apply {
                      addProperty("applyEdit", true)
                      add("workspaceEdit", gson.toJsonTree(mapOf("documentChanges" to true)))
                    },
                )
              },
          )
        }

    sendRequest("initialize", initParams) { result ->
      KslLogs.info("Server initialized: {}", result != null)
      sendNotification("initialized", JsonObject())
    }
  }

  fun sendRequest(method: String, params: JsonObject, callback: (JsonObject?) -> Unit) {
    val id = nextId.getAndIncrement()
    pendingRequests[id] = callback

    val payload =
        JsonObject().apply {
          addProperty("jsonrpc", "2.0")
          addProperty("id", id)
          addProperty("method", method)
          add("params", params)
        }

    KslLogs.debug("Sending request ID {}: {}", id, method)
    sendMessage(payload)
  }

  fun sendNotification(method: String, params: JsonObject) {
    val payload =
        JsonObject().apply {
          addProperty("jsonrpc", "2.0")
          addProperty("method", method)
          add("params", params)
        }

    KslLogs.debug("Sending notification: {}", method)
    sendMessage(payload)
  }

  private fun sendMessage(payload: JsonObject) {
    val data = gson.toJson(payload)
    val w =
        writer
            ?: run {
              KslLogs.error("Cannot send message: writer is null")
              return
            }

    synchronized(w) {
      try {
        val contentBytes = data.toByteArray(StandardCharsets.UTF_8)
        w.write("Content-Length: ${contentBytes.size}\r\n\r\n")
        w.write(data)
        w.flush()
      } catch (e: Exception) {
        KslLogs.error("Failed to send message", e)
      }
    }
  }

  private fun startReaderThread() {
    val r = reader ?: return
    Thread(
            {
              try {
                while (true) {
                  var contentLength = -1

                  // Read headers
                  while (true) {
                    val line = r.readLine() ?: return@Thread
                    if (line.isEmpty()) break

                    if (line.startsWith("Content-Length:", ignoreCase = true)) {
                      contentLength = line.substringAfter(":").trim().toIntOrNull() ?: -1
                    }
                  }

                  if (contentLength <= 0 || contentLength > MAX_CONTENT_LENGTH) {
                    KslLogs.warn("Invalid content length: {}", contentLength)
                    continue
                  }

                  // Read content with optimized buffer
                  val buffer = CharArray(contentLength)
                  var totalRead = 0
                  while (totalRead < contentLength) {
                    val read = r.read(buffer, totalRead, contentLength - totalRead)
                    if (read < 0) break
                    totalRead += read
                  }

                  val json = String(buffer)
                  
                  // Handle message asynchronously for better performance
                  executorService.submit {
                    handleMessage(json)
                  }
                }
              } catch (e: Exception) {
                KslLogs.error("Error in reader thread", e)
              }
            },
            "kls-jsonrpc-reader",
        )
        .apply {
          priority = Thread.MAX_PRIORITY // High priority for reading
        }
        .start()
  }

  private fun startErrorReaderThread() {
    val errorReader =
        BufferedReader(
            InputStreamReader(process!!.errorStream, StandardCharsets.UTF_8),
            BUFFER_SIZE
        )
    Thread(
            {
              try {
                var line: String?
                while (errorReader.readLine().also { line = it } != null) {
                  KslLogs.warn("KLS stderr: {}", line)
                }
              } catch (e: Exception) {
                KslLogs.error("Error in error reader thread", e)
              }
            },
            "kls-error-reader",
        )
        .start()
  }

  private fun handleMessage(json: String) {
    try {
      val obj = gson.fromJson(json, JsonObject::class.java)

      if (obj.has("id")) {
        val id = obj.get("id").asInt
        val callback = pendingRequests.remove(id)

        if (callback == null) {
          KslLogs.warn("No callback found for request ID: {}", id)
          return
        }

        if (obj.has("error")) {
          val error = obj.getAsJsonObject("error")
          val errorMsg = error.get("message")?.asString ?: "Unknown error"
          val errorCode = error.get("code")?.asInt ?: -1
          KslLogs.error("LSP error response for request {}: [{}] {}", id, errorCode, errorMsg)
          callback.invoke(null)
        } else if (obj.has("result")) {
          val result = obj.get("result")

          when {
            result.isJsonNull -> {
              KslLogs.debug("Request {} returned null result", id)
              callback.invoke(null)
            }
            result.isJsonArray -> {
              KslLogs.debug(
                  "Request {} returned array result with {} items",
                  id,
                  result.asJsonArray.size(),
              )
              val wrappedResult = JsonObject().apply { add("result", result) }
              callback.invoke(wrappedResult)
            }
            result.isJsonObject -> {
              KslLogs.debug("Request {} returned object result", id)
              callback.invoke(result.asJsonObject)
            }
            result.isJsonPrimitive -> {
              KslLogs.debug("Request {} returned primitive result", id)
              val wrappedResult = JsonObject().apply { add("result", result) }
              callback.invoke(wrappedResult)
            }
            else -> {
              KslLogs.warn("Request {} returned unknown result type", id)
              callback.invoke(null)
            }
          }
        } else {
          KslLogs.warn("Request {} has neither result nor error", id)
          callback.invoke(null)
        }
      } else if (obj.has("method")) {
        notificationHandler.handle(obj)
      } else {
        KslLogs.warn("Message has neither id nor method: {}", json.take(200))
      }
    } catch (e: Exception) {
      KslLogs.error("Error handling message: {}", json.take(200), e)
    }
  }

  private fun findJavaExecutable(): String {
    val candidates =
        listOf(
            "/data/data/com.tom.rv2ide/files/usr/bin/java",
            System.getenv("JAVA_HOME")?.let { "$it/bin/java" },
            "java",
        )

    return candidates.filterNotNull().firstOrNull { path ->
      try {
        File(path).exists() || Runtime.getRuntime().exec(arrayOf(path, "-version")).waitFor() == 0
      } catch (e: Exception) {
        false
      }
    } ?: "java"
  }

  fun shutdown() {
    try {
      sendRequest("shutdown", JsonObject()) {}
      sendNotification("exit", JsonObject())
      
      // Give server time to gracefully shutdown
      Thread.sleep(500)
    } catch (e: Exception) {
      KslLogs.error("Error during shutdown", e)
    }

    // Shutdown executor service
    executorService.shutdown()

    try {
      writer?.close()
    } catch (e: Exception) {}
    try {
      reader?.close()
    } catch (e: Exception) {}
    try {
      process?.destroy()
    } catch (e: Exception) {}

    process = null
    pendingRequests.clear()
  }
}