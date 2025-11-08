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
package com.tom.rv2ide.utils

import android.content.Context
import com.tom.rv2ide.actions.ActionItem.Location.EDITOR_FILE_TABS
import com.tom.rv2ide.actions.ActionItem.Location.EDITOR_FILE_TREE
import com.tom.rv2ide.actions.ActionItem.Location.EDITOR_TOOLBAR
import com.tom.rv2ide.actions.ActionsRegistry
import com.tom.rv2ide.actions.build.ProjectSyncAction
import com.tom.rv2ide.actions.build.QuickRunWithCancellationAction
import com.tom.rv2ide.actions.build.RunTasksAction
import com.tom.rv2ide.actions.editor.CopyAction
import com.tom.rv2ide.actions.editor.CutAction
import com.tom.rv2ide.actions.editor.ExpandSelectionAction
import com.tom.rv2ide.actions.editor.ExtractAction
import com.tom.rv2ide.actions.editor.LongSelectAction
import com.tom.rv2ide.actions.editor.PasteAction
import com.tom.rv2ide.actions.editor.SelectAllAction
import com.tom.rv2ide.actions.etc.DisconnectLogSendersAction
import com.tom.rv2ide.actions.etc.FindActionMenu
import com.tom.rv2ide.actions.etc.IdeConfigurationsAction
import com.tom.rv2ide.actions.etc.LaunchAppAction
import com.tom.rv2ide.actions.etc.PreviewLayoutAction
import com.tom.rv2ide.actions.etc.ReloadColorSchemesAction
import com.tom.rv2ide.actions.file.CloseAllFilesAction
import com.tom.rv2ide.actions.file.CloseFileAction
import com.tom.rv2ide.actions.file.CloseOtherFilesAction
import com.tom.rv2ide.actions.file.FormatCodeAction
import com.tom.rv2ide.actions.file.SaveFileAction
import com.tom.rv2ide.actions.filetree.CopyPathAction
import com.tom.rv2ide.actions.filetree.DeleteAction
import com.tom.rv2ide.actions.filetree.NewFileAction
import com.tom.rv2ide.actions.filetree.NewFolderAction
import com.tom.rv2ide.actions.filetree.OpenWithAction
import com.tom.rv2ide.actions.filetree.RenameAction
import com.tom.rv2ide.actions.text.RedoAction
import com.tom.rv2ide.actions.text.UndoAction

/**
 * Takes care of registering actions to the actions registry for the editor activity.
 *
 * @author Akash Yadav
 */
class EditorActivityActions {

  companion object {

    @JvmStatic
    fun register(context: Context) {
      clear()
      val registry = ActionsRegistry.getInstance()
      var order = 0

      // Toolbar actions
      registry.registerAction(UndoAction(context, order++))
      registry.registerAction(RedoAction(context, order++))
      registry.registerAction(QuickRunWithCancellationAction(context, order++))
      registry.registerAction(RunTasksAction(context, order++))
      registry.registerAction(SaveFileAction(context, order++))
      registry.registerAction(PreviewLayoutAction(context, order++))
      registry.registerAction(FindActionMenu(context, order++))
      registry.registerAction(ProjectSyncAction(context, order++))
      registry.registerAction(ReloadColorSchemesAction(context, order++))
      registry.registerAction(DisconnectLogSendersAction(context, order++))
      registry.registerAction(LaunchAppAction(context, order++))
      registry.registerAction(IdeConfigurationsAction(context, order++))

      // editor text actions
      registry.registerAction(ExtractAction(context, order++))
      registry.registerAction(ExpandSelectionAction(context, order++))
      registry.registerAction(SelectAllAction(context, order++))
      registry.registerAction(LongSelectAction(context, order++))
      registry.registerAction(CutAction(context, order++))
      registry.registerAction(ExtractAction(context, order++))
      registry.registerAction(CopyAction(context, order++))
      registry.registerAction(PasteAction(context, order++))
      registry.registerAction(FormatCodeAction(context, order++))

      // file tab actions
      registry.registerAction(CloseFileAction(context, order++))
      registry.registerAction(CloseOtherFilesAction(context, order++))
      registry.registerAction(CloseAllFilesAction(context, order++))

      // file tree actions
      registry.registerAction(CopyPathAction(context, order++))
      registry.registerAction(DeleteAction(context, order++))
      registry.registerAction(NewFileAction(context, order++))
      registry.registerAction(NewFolderAction(context, order++))
      registry.registerAction(OpenWithAction(context, order++))
      registry.registerAction(RenameAction(context, order++))
    }

    @JvmStatic
    fun clear() {
      // EDITOR_TEXT_ACTIONS should not be cleared as the language servers register actions there as
      // well
      val locations = arrayOf(EDITOR_TOOLBAR, EDITOR_FILE_TABS, EDITOR_FILE_TREE)
      val registry = ActionsRegistry.getInstance()
      locations.forEach(registry::clearActions)
    }
  }
}
