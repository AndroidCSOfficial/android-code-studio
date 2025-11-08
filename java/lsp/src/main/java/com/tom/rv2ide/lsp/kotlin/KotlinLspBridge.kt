/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.lsp.kotlin

import com.tom.rv2ide.lsp.api.ILanguageClient
import com.tom.rv2ide.lsp.models.DiagnosticItem
import com.tom.rv2ide.lsp.models.DiagnosticResult
import com.tom.rv2ide.models.Range
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.LanguageClient

/** Bridge between AndroidIDE's ILanguageClient and kotlin-language-server's LanguageClient */
/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinLspBridge(private val ideClient: ILanguageClient) : LanguageClient {

  override fun telemetryEvent(p0: Any?) {
    // TODO: Handle telemetry events
  }

  override fun publishDiagnostics(p0: org.eclipse.lsp4j.PublishDiagnosticsParams?) {
    p0?.let { params ->
      val diagnostics =
          params.diagnostics.map { diagnostic ->
            com.tom.rv2ide.lsp.models.DiagnosticItem(
                message = diagnostic.message,
                code = diagnostic.code?.toString() ?: "",
                range =
                    com.tom.rv2ide.models.Range(
                        start =
                            com.tom.rv2ide.models.Position(
                                diagnostic.range.start.line,
                                diagnostic.range.start.character,
                            ),
                        end =
                            com.tom.rv2ide.models.Position(
                                diagnostic.range.end.line,
                                diagnostic.range.end.character,
                            ),
                    ),
                source = diagnostic.source ?: "kotlin-language-server",
                severity =
                    when (diagnostic.severity) {
                      org.eclipse.lsp4j.DiagnosticSeverity.Error ->
                          com.tom.rv2ide.lsp.models.DiagnosticSeverity.ERROR
                      org.eclipse.lsp4j.DiagnosticSeverity.Warning ->
                          com.tom.rv2ide.lsp.models.DiagnosticSeverity.WARNING
                      org.eclipse.lsp4j.DiagnosticSeverity.Information ->
                          com.tom.rv2ide.lsp.models.DiagnosticSeverity.INFO
                      org.eclipse.lsp4j.DiagnosticSeverity.Hint ->
                          com.tom.rv2ide.lsp.models.DiagnosticSeverity.HINT
                      else -> com.tom.rv2ide.lsp.models.DiagnosticSeverity.ERROR
                    },
            )
          }
      ideClient.publishDiagnostics(
          com.tom.rv2ide.lsp.models.DiagnosticResult(
              java.nio.file.Paths.get(params.uri),
              diagnostics,
          )
      )
    }
  }

  override fun showMessage(p0: org.eclipse.lsp4j.MessageParams?) {
    // TODO: Handle show message
  }

  override fun showMessageRequest(
      p0: org.eclipse.lsp4j.ShowMessageRequestParams?
  ): CompletableFuture<org.eclipse.lsp4j.MessageActionItem?> {
    // TODO: Handle show message request
    return CompletableFuture.completedFuture(null)
  }

  override fun logMessage(p0: org.eclipse.lsp4j.MessageParams?) {
    // TODO: Handle log message
  }

  override fun createProgress(
      p0: org.eclipse.lsp4j.WorkDoneProgressCreateParams?
  ): CompletableFuture<Void> {
    // TODO: Handle create progress
    return CompletableFuture.completedFuture(null)
  }

  override fun notifyProgress(p0: org.eclipse.lsp4j.ProgressParams?) {
    // TODO: Handle notify progress
  }

  override fun applyEdit(
      p0: org.eclipse.lsp4j.ApplyWorkspaceEditParams?
  ): CompletableFuture<org.eclipse.lsp4j.ApplyWorkspaceEditResponse> {
    // TODO: Handle apply edit
    return CompletableFuture.completedFuture(org.eclipse.lsp4j.ApplyWorkspaceEditResponse(false))
  }

  override fun registerCapability(
      p0: org.eclipse.lsp4j.RegistrationParams?
  ): CompletableFuture<Void> {
    // TODO: Handle register capability
    return CompletableFuture.completedFuture(null)
  }

  override fun unregisterCapability(
      p0: org.eclipse.lsp4j.UnregistrationParams?
  ): CompletableFuture<Void> {
    // TODO: Handle unregister capability
    return CompletableFuture.completedFuture(null)
  }
}
