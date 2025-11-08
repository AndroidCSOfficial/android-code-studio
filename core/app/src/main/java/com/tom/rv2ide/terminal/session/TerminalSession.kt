package com.tom.rv2ide.terminal.session

import android.os.Handler
import android.os.Looper
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Terminal Session implementation for Android Code Studio Handles shell process execution and
 * terminal I/O
 */
class TerminalSession
private constructor(
    private val executablePath: String,
    private val arguments: Array<String>,
    private val workingDirectory: String,
    private val environment: Array<String>,
) {

  private var process: Process? = null
  private var inputStream: OutputStream? = null
  private var outputStream: InputStream? = null
  private var errorStream: InputStream? = null
  private var client: TerminalSessionClient? = null
  private var executor: ExecutorService? = null
  private var isRunning = false

  companion object {
    fun createTerminalSession(
        executablePath: String,
        arguments: Array<String>,
        workingDirectory: String,
        environment: Array<String>,
    ): TerminalSession {
      return TerminalSession(executablePath, arguments, workingDirectory, environment)
    }
  }

  fun setTerminalSessionClient(client: TerminalSessionClient) {
    this.client = client
  }

  fun start() {
    if (isRunning) return

    try {
      val processBuilder = ProcessBuilder(executablePath, *arguments)
      processBuilder.directory(File(workingDirectory))
      processBuilder
          .environment()
          .putAll(
              environment.associate {
                val parts = it.split("=", limit = 2)
                parts[0] to parts.getOrElse(1) { "" }
              }
          )

      process = processBuilder.start()
      inputStream = process?.outputStream
      outputStream = process?.inputStream
      errorStream = process?.errorStream

      isRunning = true
      executor = Executors.newCachedThreadPool()

      // Start reading output
      executor?.execute { readOutput() }
      executor?.execute { readError() }

      // Start a thread to monitor process exit
      executor?.execute { monitorProcess() }
    } catch (e: Exception) {
      isRunning = false
      client?.onSessionFinished(this)
    }
  }

  private fun monitorProcess() {
    try {
      process?.waitFor()
      isRunning = false
      Handler(Looper.getMainLooper()).post { client?.onSessionFinished(this) }
    } catch (e: InterruptedException) {
      isRunning = false
    }
  }

  private fun readOutput() {
    outputStream?.let { stream ->
      val reader = BufferedReader(InputStreamReader(stream))
      try {
        var line: String?
        while (reader.readLine().also { line = it } != null && isRunning) {
          line?.let { text ->
            Handler(Looper.getMainLooper()).post { client?.onTextChanged(this, text + "\n") }
          }
        }
      } catch (e: IOException) {
        // Stream closed
      }
    }
  }

  private fun readError() {
    errorStream?.let { stream ->
      val reader = BufferedReader(InputStreamReader(stream))
      try {
        var line: String?
        while (reader.readLine().also { line = it } != null && isRunning) {
          line?.let { text ->
            Handler(Looper.getMainLooper()).post { client?.onTextChanged(this, "[ERROR] $text\n") }
          }
        }
      } catch (e: IOException) {
        // Stream closed
      }
    }
  }

  fun write(data: String) {
    inputStream?.let { stream ->
      try {
        stream.write(data.toByteArray())
        stream.flush()
      } catch (e: IOException) {
        // Handle write error
      }
    }
  }

  fun finish() {
    isRunning = false
    process?.destroy()
    inputStream?.close()
    outputStream?.close()
    errorStream?.close()
    executor?.shutdown()

    Handler(Looper.getMainLooper()).post { client?.onSessionFinished(this) }
  }

  fun isRunning(): Boolean = isRunning
}
