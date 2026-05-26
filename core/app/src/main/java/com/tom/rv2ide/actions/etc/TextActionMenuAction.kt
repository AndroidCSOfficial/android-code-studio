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

package com.tom.rv2ide.actions.etc

import android.content.Context
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.ActionItem
import com.tom.rv2ide.actions.ActionMenu
import com.tom.rv2ide.actions.EditorActivityAction
import com.tom.rv2ide.R
import com.tom.rv2ide.resources.R as ResR
import com.tom.rv2ide.utils.TextTransformers
import io.github.rosemoe.sora.widget.CodeEditor
import org.slf4j.LoggerFactory
import com.tom.rv2ide.editor.ui.IDEEditor

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
*/

class TextActionMenuAction(context: Context, override val order: Int) :
    EditorActivityAction(), ActionMenu {

  override val children: MutableSet<ActionItem> = mutableSetOf()
  override val id: String = "ide.editor.text.actions"

  init {
    label = context.getString(R.string.edit)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_text_actions)

    addAction(ConvertToUppercaseAction(context, 0))
    addAction(ConvertToLowercaseAction(context, 1))
    addAction(DeleteLineAction(context, 2))
    addAction(DuplicateLineAction(context, 3))
    addAction(CopyLineAction(context, 4))
    addAction(CommentLineAction(context, 5))
    addAction(MoveLineUpAction(context, 6))
    addAction(MoveLineDownAction(context, 7))
    addAction(InsertLineAboveAction(context, 8))
    addAction(InsertLineBelowAction(context, 9))
    addAction(SelectLineAction(context, 10))
    addAction(TrimTrailingWhitespaceAction(context, 11))
    addAction(IndentLineAction(context, 12))
    addAction(UnindentLineAction(context, 13))
    addAction(JoinLinesAction(context, 14))
    addAction(ClearAllAction(context, 15))
  }

  override fun prepare(data: ActionData) {
    super<EditorActivityAction>.prepare(data)
    
    if (!visible) {
      return
    }
    
    // Hide if no editor is available
    val editor = data.get(IDEEditor::class.java)
    if (editor == null) {
      visible = false
      enabled = false
      return
    }
    
    // Hide if editor is not editable
    if (!editor.isEditable) {
      visible = false
      enabled = false
      return
    }
    
    // Show the action
    visible = true
    enabled = true
    
    super<ActionMenu>.prepare(data)
  }
}

class ConvertToUppercaseAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.uppercase"
  override var requiresUIThread = true
  
  companion object {
    private val log = LoggerFactory.getLogger(ConvertToUppercaseAction::class.java)
  }
  
  init {
    label = context.getString(ResR.string.action_convert_to_uppercase)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_uppercase)
  }
  
  override fun prepare(data: ActionData) {
    super.prepare(data)
    val editor = data.get(IDEEditor::class.java)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    val transformer = TextTransformers(editor)
    transformer.lineToUppercase()
    return true
  }

}

class ConvertToLowercaseAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.lowercase"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_convert_to_lowercase)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_lowercase)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).lineToLowercase()
    return true
  }
}

class DeleteLineAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.deleteline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_delete_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_delete)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).deleteLine()
    return true
  }
}

class DuplicateLineAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.duplicateline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_duplicate_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_duplicate)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).duplicateLine()
    return true
  }
}

class CopyLineAction(private val context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.copyline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_copy_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_copy)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).copyLine()
    Toast.makeText(context, ResR.string.msg_copied_to_clipboard, Toast.LENGTH_SHORT).show()
    return true
  }
}

class CommentLineAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.commentline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_comment_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_comment)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).commentLine()
    return true
  }
}

class MoveLineUpAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.movelineup"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_move_line_up)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_move_line_up)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).moveLineUp()
    return true
  }
}

class MoveLineDownAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.movelinedown"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_move_line_down)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_move_line_down)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).moveLineDown()
    return true
  }
}

class InsertLineAboveAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.insertlineabove"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_insert_line_above)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_insert_line_above)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).insertLineAbove()
    return true
  }
}

class InsertLineBelowAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.insertlinebelow"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_insert_line_below)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_insert_line_below)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).insertLineBelow()
    return true
  }
}

class SelectLineAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.selectline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_select_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_select_line)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).selectLine()
    return true
  }
}

class TrimTrailingWhitespaceAction(private val context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.trimtrailingwhitespace"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_trim_trailing_whitespace)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_trim_trailing_whitespace)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).trimTrailingWhitespace()
    Toast.makeText(context, ResR.string.msg_trailing_whitespace_removed, Toast.LENGTH_SHORT).show()
    return true
  }
}

class IndentLineAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.indentline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_indent_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_indent_line)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).indentLine()
    return true
  }
}

class UnindentLineAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.unindentline"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_unindent_line)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_unindent_line)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).unindentLine()
    return true
  }
}

class JoinLinesAction(context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.joinlines"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_join_lines)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_join_lines)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    TextTransformers(editor).joinLines()
    return true
  }
}

class ClearAllAction(private val context: Context, override val order: Int) : EditorActivityAction() {
  override val id = "ide.editor.text.clearall"
  override var requiresUIThread = true
  
  init {
    label = context.getString(ResR.string.action_clear_all)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_clear_all)
  }
  
  override suspend fun execAction(data: ActionData): Boolean {
    val editor = data.get(IDEEditor::class.java) as? CodeEditor
    if (editor == null) {
      return false
    }
    val activity = data.requireActivity()
    
    MaterialAlertDialogBuilder(activity)
      .setTitle(ResR.string.title_clear_all)
      .setMessage(ResR.string.msg_clear_all_confirm)
      .setPositiveButton(ResR.string.btn_clear) { _, _ ->
        TextTransformers(editor).clearAll()
        Toast.makeText(context, ResR.string.msg_editor_cleared, Toast.LENGTH_SHORT).show()
      }
      .setNegativeButton(android.R.string.cancel, null)
      .show()
    
    return true
  }
}