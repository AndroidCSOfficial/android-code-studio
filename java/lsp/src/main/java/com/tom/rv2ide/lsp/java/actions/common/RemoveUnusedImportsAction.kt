package com.tom.rv2ide.lsp.java.actions.common

import com.google.googlejavaformat.java.FormatterException
import com.google.googlejavaformat.java.RemoveUnusedImports
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.hasRequiredData
import com.tom.rv2ide.actions.markInvisible
import com.tom.rv2ide.actions.requireEditor
import com.tom.rv2ide.lsp.java.actions.BaseJavaCodeAction
import com.tom.rv2ide.resources.R.string
import io.github.rosemoe.sora.widget.CodeEditor
import org.slf4j.LoggerFactory

class RemoveUnusedImportsAction : BaseJavaCodeAction() {

  override val id: String = "ide.editor.lsp.java.removeUnusedImports"
  override var label: String = ""
  override val titleTextRes: Int = string.action_remove_unused_imports

  companion object {

    private val log = LoggerFactory.getLogger(RemoveUnusedImportsAction::class.java)
  }

  override fun prepare(data: ActionData) {
    super.prepare(data)
    if (!visible) {
      return
    }

    if (!data.hasRequiredData(CodeEditor::class.java)) {
      markInvisible()
      return
    }

    visible = true
    enabled = true
  }

  override suspend fun execAction(data: ActionData): Any {
    val watch = com.tom.rv2ide.utils.StopWatch("Remove unused imports")
    return try {
      val editor = data.requireEditor()
      val content = editor.text
      val output = RemoveUnusedImports.removeUnusedImports(content.toString())
      watch.log()
      output
    } catch (e: FormatterException) {
      log.error("Failed to remove unused imports", e)
      false
    }
  }

  override fun postExec(data: ActionData, result: Any) {
    if (result is String && result.isNotEmpty()) {
      val editor = data.requireEditor()
      editor.setText(result)
    }
  }
}
