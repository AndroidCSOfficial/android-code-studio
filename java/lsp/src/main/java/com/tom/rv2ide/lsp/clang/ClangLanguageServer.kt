/*
 *  This file is part of AndroidCodeStudio.
 */
package com.tom.rv2ide.lsp.clang

import android.content.Context
import com.tom.rv2ide.lsp.api.ILanguageClient
import com.tom.rv2ide.lsp.api.ILanguageServer
import com.tom.rv2ide.lsp.api.IServerSettings
import com.tom.rv2ide.lsp.models.*
import com.tom.rv2ide.models.Range
import com.tom.rv2ide.projects.IWorkspace
import java.nio.file.Path
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

class ClangLanguageServer(private val context: Context) : ILanguageServer {

  companion object {
    const val SERVER_ID = "clang"
    private val log = LoggerFactory.getLogger(ClangLanguageServer::class.java)
  }

  private val processManager = ClangServerProcessManager(context)
  private val documentManager = ClangDocumentManager(processManager)
  private val requestHandler = ClangRequestHandler(processManager, documentManager)
  private val eventHandler = ClangEventHandler(documentManager)

  private var _client: ILanguageClient? = null
  private var initialized = false
  private var workspaceSetup: ClangWorkspaceSetup? = null

  private val completionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

  init {
    System.err.println("===== ClangLanguageServer CREATED =====")
    ClangLogs.error("ClangLanguageServer CONSTRUCTOR called")
    if (!org.greenrobot.eventbus.EventBus.getDefault().isRegistered(eventHandler)) {
      org.greenrobot.eventbus.EventBus.getDefault().register(eventHandler)
    }
    processManager.setDiagnosticsCallback { diagnostics ->
      _client?.publishDiagnostics(diagnostics)
    }
    ClangLogs.error("ClangLanguageServer initialization complete")
  }

  override val serverId: String = SERVER_ID
  override val client: ILanguageClient?
    get() = _client

  override fun connectClient(client: ILanguageClient?) {
    this._client = client
    ClangLogs.info("Connected language client: {}", client?.javaClass?.simpleName)
  }

  override fun applySettings(settings: IServerSettings?) {
    ClangLogs.debug("Applied settings: {}", settings)
  }

  override fun setupWorkspace(workspace: IWorkspace) {
    ClangLogs.info("Setting up workspace: {}", workspace.getProjectDir())
    workspaceSetup = ClangWorkspaceSetup(context, workspace)
    workspaceSetup?.setup(processManager)
    initialized = true
    ClangLogs.info("Workspace setup complete, initialized={}", initialized)
  }

  // MATCH THE KOTLIN IMPLEMENTATION EXACTLY
  override fun complete(params: CompletionParams?): CompletionResult {
    System.err.println("!!!!! CLANG COMPLETE CALLED !!!!!")
    System.err.println("initialized = $initialized")
    System.err.println("params = $params")

    ClangLogs.error("********** CLANG COMPLETE METHOD CALLED **********")
    ClangLogs.error("Initialized: {}", initialized)
    ClangLogs.error("Params: {}", params)

    return if (initialized && params != null) {
      runBlocking {
        withTimeout(3000) {
          val result = async(Dispatchers.Default) { requestHandler.complete(params) }
          result.await()
        }
      }
    } else {
      ClangLogs.error("RETURNING EMPTY: initialized={}, params={}", initialized, params)
      CompletionResult(emptyList())
    }
  }

  override suspend fun findReferences(params: ReferenceParams): ReferenceResult {
    ClangLogs.info("findReferences called for: {}", params.file)
    return if (initialized) {
      requestHandler.findReferences(params)
    } else {
      ReferenceResult(emptyList())
    }
  }

  override suspend fun findDefinition(params: DefinitionParams): DefinitionResult {
    ClangLogs.info("findDefinition called for: {}", params.file)
    return if (initialized) {
      requestHandler.findDefinition(params)
    } else {
      DefinitionResult(emptyList())
    }
  }

  override suspend fun expandSelection(params: ExpandSelectionParams): Range {
    return params.selection
  }

  override suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp {
    ClangLogs.info("signatureHelp called for: {}", params.file)
    return if (initialized) {
      requestHandler.signatureHelp(params)
    } else {
      SignatureHelp(emptyList(), 0, 0)
    }
  }

  override suspend fun analyze(file: Path): DiagnosticResult {
    // Clangd provides diagnostics automatically via textDocument/publishDiagnostics
    return DiagnosticResult.NO_UPDATE
  }

  override fun formatCode(params: FormatCodeParams?): CodeFormatResult {
    return CodeFormatResult(false, mutableListOf())
  }

  override fun handleFailure(failure: LSPFailure?): Boolean {
    ClangLogs.error("LSP failure: type={}, error={}", failure?.type, failure?.error?.message)
    return false
  }

  override fun shutdown() {
    ClangLogs.error("=== SHUTDOWN CALLED ===")
    ClangLogs.error("Stack trace:")
    Thread.currentThread().stackTrace.take(10).forEach { ClangLogs.error("  at {}", it) }

    ClangLogs.info("Shutting down Clang Language Server...")
    completionScope.cancel()
    try {
      org.greenrobot.eventbus.EventBus.getDefault().unregister(eventHandler)
    } catch (e: Exception) {
      ClangLogs.warn("Error unregistering from EventBus", e)
    }

    ClangLogs.error("About to call documentManager.clear()")
    documentManager.clear()

    ClangLogs.error("About to call processManager.shutdown()")
    processManager.shutdown()

    initialized = false
    ClangLogs.info("Clang Language Server shutdown complete")
  }
}
