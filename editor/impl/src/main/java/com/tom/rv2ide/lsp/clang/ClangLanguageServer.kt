package com.tom.rv2ide.lsp.clang

import android.content.Context
import com.tom.rv2ide.lsp.api.ILanguageClient
import com.tom.rv2ide.lsp.api.ILanguageServer
import com.tom.rv2ide.lsp.api.IServerSettings
import com.tom.rv2ide.lsp.models.CodeFormatResult
import com.tom.rv2ide.lsp.models.CompletionParams
import com.tom.rv2ide.lsp.models.CompletionResult
import com.tom.rv2ide.lsp.models.DefinitionParams
import com.tom.rv2ide.lsp.models.DefinitionResult
import com.tom.rv2ide.lsp.models.DiagnosticResult
import com.tom.rv2ide.lsp.models.ExpandSelectionParams
import com.tom.rv2ide.lsp.models.FormatCodeParams
import com.tom.rv2ide.lsp.models.ReferenceParams
import com.tom.rv2ide.lsp.models.ReferenceResult
import com.tom.rv2ide.lsp.models.SignatureHelp
import com.tom.rv2ide.lsp.models.SignatureHelpParams
import com.tom.rv2ide.models.Range
import com.tom.rv2ide.projects.IWorkspace
import java.nio.file.Path

class ClangLanguageServer(private val context: Context) : ILanguageServer {

    override val serverId: String = SERVER_ID

    override var client: ILanguageClient? = null
        private set

    companion object {
        const val SERVER_ID = "ide.lsp.clang"
    }

    override fun shutdown() {
    }

    override fun connectClient(client: ILanguageClient?) {
        this.client = client
    }

    override fun applySettings(settings: IServerSettings?) {
    }

    override fun setupWorkspace(workspace: IWorkspace) {
    }

    override fun complete(params: CompletionParams?): CompletionResult {
        return CompletionResult.EMPTY
    }

    override suspend fun findReferences(params: ReferenceParams): ReferenceResult {
        return ReferenceResult(emptyList())
    }

    override suspend fun findDefinition(params: DefinitionParams): DefinitionResult {
        return DefinitionResult(emptyList())
    }

    override suspend fun expandSelection(params: ExpandSelectionParams): Range {
        return params.selection
    }

    override suspend fun signatureHelp(params: SignatureHelpParams): SignatureHelp {
        return SignatureHelp(emptyList(), -1, -1)
    }

    override suspend fun analyze(file: Path): DiagnosticResult {
        return DiagnosticResult.NO_UPDATE
    }

    override fun formatCode(params: FormatCodeParams?): CodeFormatResult {
        return CodeFormatResult(false, mutableListOf())
    }
}
