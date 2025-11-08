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

package com.tom.rv2ide.lsp.java.actions

import android.content.Context
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.hasRequiredData
import com.tom.rv2ide.actions.markInvisible
import com.tom.rv2ide.actions.newDialogBuilder
import com.tom.rv2ide.actions.requirePath
import com.tom.rv2ide.lsp.java.JavaCompilerProvider
import com.tom.rv2ide.lsp.java.compiler.CompileTask
import com.tom.rv2ide.lsp.java.visitors.FindTypeDeclarationAt
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.resources.R
import com.tom.rv2ide.utils.flashInfo
import io.github.rosemoe.sora.widget.CodeEditor
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import jdkx.lang.model.element.Modifier.STATIC
import openjdk.source.tree.ClassTree
import openjdk.source.tree.Tree.Kind.VARIABLE
import openjdk.source.tree.VariableTree
import org.slf4j.LoggerFactory

/**
 * Any action that has to work with fields in the current class can inherit this action.
 *
 * @author Akash Yadav
 */
abstract class FieldBasedAction : BaseJavaCodeAction() {

  companion object {

    private val log = LoggerFactory.getLogger(FieldBasedAction::class.java)
  }

  override fun prepare(data: ActionData) {
    super.prepare(data)

    if (
        !visible ||
            !data.hasRequiredData(
                com.tom.rv2ide.models.Range::class.java,
                CodeEditor::class.java,
            ) ||
            IProjectManager.getInstance().getWorkspace() == null
    ) {
      markInvisible()
      return
    }

    visible = true
    enabled = true
  }

  override suspend fun execAction(data: ActionData): Any {
    val range = data[com.tom.rv2ide.models.Range::class.java]!!
    val file = data.requirePath()
    val module =
        IProjectManager.getInstance().getWorkspace()?.findModuleForFile(file, false) ?: return Any()

    return JavaCompilerProvider.get(module).compile(file).get { task ->
      val triple = findFields(task, file, range)
      val type = triple.second
      val fields = triple.third
      val fieldNames = fields.map { "${it.name}: ${it.type}" } // Get the names

      log.debug("Found {} fields in class {}", fieldNames.size, type.simpleName)

      return@get fieldNames
    }
  }

  protected fun findFields(
      task: CompileTask,
      file: Path,
      range: com.tom.rv2ide.models.Range,
  ): Triple<FindTypeDeclarationAt, ClassTree, MutableList<VariableTree>> {
    // 1-based line and column index
    val startLine = range.start.line + 1
    val startColumn = range.start.column + 1
    val endLine = range.end.line + 1
    val endColumn = range.end.column + 1
    val lines = task.root().lineMap
    val start = lines.getPosition(startLine.toLong(), startColumn.toLong())
    val end = lines.getPosition(endLine.toLong(), endColumn.toLong())

    if (start == (-1).toLong() || end == (-1).toLong()) {
      throw CompletionException(
          RuntimeException("Unable to find position for the given selection range")
      )
    }

    val typeFinder = FindTypeDeclarationAt(task.task)
    var type = typeFinder.scan(task.root(file), start)
    if (type == null) {
      type = typeFinder.scan(task.root(file), end)
    }

    if (type == null) {
      throw CompletionException(
          RuntimeException("Unable to find class declaration within cursor range")
      )
    }

    val fields =
        type.members
            .filter { it.kind == VARIABLE }
            .map { it as VariableTree }
            .filter { !it.modifiers.flags.contains(STATIC) }
            .toMutableList()
    return Triple(typeFinder, type, fields)
  }

  @Suppress("UNCHECKED_CAST")
  override fun postExec(data: ActionData, result: Any) {
    if (result !is List<*>) {
      log.error("Unable to find fields in the current class")
      return
    }

    if (result.isEmpty()) {
      flashInfo(data[Context::class.java]!!.getString(R.string.msg_no_fields_found))
      return
    }

    onGetFields(result as List<String>, data)
  }

  /**
   * Called when the fields of the current class are found. As this method is called inside
   * [postExec], the current thread is the UI thread.
   */
  abstract fun onGetFields(fields: List<String>, data: ActionData)

  /**
   * Shows the field selector dialog. Returns a [CompletableFuture] which is completed when the user
   * confirms the selected fields.
   */
  protected fun showFieldSelector(
      fields: List<String>,
      data: ActionData,
      listener: OnFieldsSelectedListener?,
  ) {
    val names = fields.toTypedArray()
    val checkedNames = mutableSetOf<String>()
    val builder = newDialogBuilder(data)
    builder.setTitle(data[Context::class.java]!!.getString(R.string.msg_select_fields))
    builder.setMultiChoiceItems(names, BooleanArray(fields.size)) { _, which, checked ->
      checkedNames.apply {
        val item = names[which]
        if (checked) {
          add(item)
        } else {
          remove(item)
        }
      }
    }

    builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
      dialog.dismiss()

      if (checkedNames.isEmpty()) {
        flashInfo(data[Context::class.java]!!.getString(R.string.msg_no_fields_selected))
        return@setPositiveButton
      }

      listener?.onFieldsSelected(checkedNames)
    }
    builder.setNegativeButton(android.R.string.cancel, null)
    builder.show()
  }

  /** Listener to get callback when fields are selected by the user. */
  fun interface OnFieldsSelectedListener {

    /**
     * Called when the user is done selecting fields.
     *
     * @param fields The selected field names.
     */
    fun onFieldsSelected(fields: MutableSet<String>)
  }
}
