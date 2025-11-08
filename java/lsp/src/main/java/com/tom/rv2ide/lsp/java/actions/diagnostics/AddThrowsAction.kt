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
package com.tom.rv2ide.lsp.java.actions.diagnostics

import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.hasRequiredData
import com.tom.rv2ide.actions.markInvisible
import com.tom.rv2ide.actions.requireFile
import com.tom.rv2ide.actions.requirePath
import com.tom.rv2ide.lsp.java.JavaCompilerProvider
import com.tom.rv2ide.lsp.java.actions.BaseJavaCodeAction
import com.tom.rv2ide.lsp.java.models.DiagnosticCode
import com.tom.rv2ide.lsp.java.rewrite.AddException
import com.tom.rv2ide.lsp.java.utils.CodeActionUtils
import com.tom.rv2ide.lsp.models.DiagnosticItem
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.resources.R
import org.slf4j.LoggerFactory

/** @author Akash Yadav */
class AddThrowsAction : BaseJavaCodeAction() {

  override val id = "ide.editor.lsp.java.diagnostics.addThrows"
  override var label: String = ""
  private val diagnosticCode = DiagnosticCode.NOT_THROWN.id

  override val titleTextRes: Int = R.string.action_add_throws

  companion object {

    private val log = LoggerFactory.getLogger(AddThrowsAction::class.java)
  }

  override fun prepare(data: ActionData) {
    super.prepare(data)

    if (!visible || !data.hasRequiredData(DiagnosticItem::class.java)) {
      markInvisible()
      return
    }

    val diagnostic = data[DiagnosticItem::class.java]!!
    if (diagnosticCode != diagnostic.code) {
      markInvisible()
      return
    }
  }

  override suspend fun execAction(data: ActionData): Any {
    val diagnostic = data[DiagnosticItem::class.java]!!
    val compiler =
        JavaCompilerProvider.get(
            IProjectManager.getInstance()
                .getWorkspace()
                ?.findModuleForFile(data.requireFile(), false) ?: return Any()
        )
    val file = data.requirePath()
    return compiler.compile(file).get { task ->
      val needsThrow = CodeActionUtils.findMethod(task, diagnostic.range)
      val exceptionName = CodeActionUtils.extractExceptionName(diagnostic.message)
      return@get AddException(
          needsThrow.className,
          needsThrow.methodName,
          needsThrow.erasedParameterTypes,
          exceptionName,
      )
    }
  }

  override fun postExec(data: ActionData, result: Any) {
    if (result !is AddException) {
      log.warn("Unable to add 'throws' expression")
      return
    }

    performCodeAction(data, result)
  }
}
