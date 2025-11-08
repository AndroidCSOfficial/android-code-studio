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

import com.google.gson.JsonObject
import com.tom.rv2ide.lsp.models.*
import java.nio.file.Paths
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinNotificationHandler {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinNotificationHandler::class.java)
  }

  private var diagnosticsCallback: ((DiagnosticResult) -> Unit)? = null

  fun setDiagnosticsCallback(callback: (DiagnosticResult) -> Unit) {
    this.diagnosticsCallback = callback
  }

  fun handle(obj: JsonObject) {
    val method = obj.get("method")?.asString ?: return
    val params = obj.getAsJsonObject("params")

    when (method) {
      "textDocument/publishDiagnostics" -> {
        KslLogs.debug("Received diagnostics notification")
        handlePublishDiagnostics(params)
      }
      "window/showMessage" -> {
        val message = params?.get("message")?.asString
        KslLogs.info("KLS window/showMessage: {}", message)
      }
      "window/logMessage" -> {
        handleLogMessage(params)
      }
      else -> {
        KslLogs.debug("Unhandled notification: {}", method)
      }
    }
  }

  private fun handlePublishDiagnostics(params: JsonObject?) {
    params ?: return
    val uri = params.get("uri")?.asString ?: return
    val diagnosticsArray = params.getAsJsonArray("diagnostics") ?: return

    KslLogs.info("Received {} diagnostics for: {}", diagnosticsArray.size(), uri)

    val diagnostics =
        diagnosticsArray.mapNotNull { element ->
          try {
            val diag = element.asJsonObject
            val range = diag.getAsJsonObject("range")
            val start = range.getAsJsonObject("start")
            val end = range.getAsJsonObject("end")

            // Extract diagnostic code (may be string or number)
            val code =
                when {
                  diag.has("code") && diag.get("code").isJsonPrimitive -> {
                    val codeElement = diag.get("code")
                    when {
                      codeElement.asJsonPrimitive.isString -> codeElement.asString
                      codeElement.asJsonPrimitive.isNumber -> codeElement.asString
                      else -> ""
                    }
                  }
                  else -> ""
                }

            DiagnosticItem(
                message = diag.get("message")?.asString ?: "",
                code = code,
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
                source = diag.get("source")?.asString ?: "kotlin",
                severity =
                    when (diag.get("severity")?.asInt) {
                      1 -> DiagnosticSeverity.ERROR
                      2 -> DiagnosticSeverity.WARNING
                      3 -> DiagnosticSeverity.INFO
                      4 -> DiagnosticSeverity.HINT
                      else -> DiagnosticSeverity.ERROR
                    },
            )
          } catch (e: Exception) {
            KslLogs.error("Failed to parse diagnostic item", e)
            null
          }
        }

    if (diagnostics.isNotEmpty()) {
      val filePath =
          try {
            java.nio.file.Paths.get(java.net.URI(uri))
          } catch (e: Exception) {
            KslLogs.error("Invalid URI: {}", uri, e)
            return
          }

      diagnosticsCallback?.invoke(DiagnosticResult(filePath, diagnostics))
    }
  }

  private fun handleLogMessage(params: JsonObject?) {
    val message = params?.get("message")?.asString
    val messageType = params?.get("type")?.asInt
    when (messageType) {
      1 -> KslLogs.error("KLS: {}", message)
      2 -> KslLogs.warn("KLS: {}", message)
      3 -> KslLogs.info("KLS: {}", message)
      4 -> KslLogs.debug("KLS: {}", message)
      else -> KslLogs.trace("KLS: {}", message)
    }
  }
}
