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

package com.tom.rv2ide.artificial.dialogs

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.R
import com.tom.rv2ide.artificial.permissions.AIPermissionManager

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
*/

class AIPermissionDialog(private val context: Context) {

    private val permissionManager = AIPermissionManager(context)

    fun showFileWriteConfirmation(
        fileName: String,
        onConfirm: () -> Unit,
        onDeny: () -> Unit
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.ai_file_write_title))
            .setMessage(context.getString(R.string.ai_file_write_message, fileName))
            .setPositiveButton(context.getString(R.string.action_allow)) { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.action_deny)) { dialog, _ ->
                onDeny()
                dialog.dismiss()
            }
            .setNeutralButton(context.getString(R.string.action_always_allow)) { dialog, _ ->
                permissionManager.setRequireConfirmation(false)
                onConfirm()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    fun showPermissionSettings(onSettingsChanged: () -> Unit) {
        val view = LayoutInflater.from(context).inflate(
            R.layout.dialog_ai_permissions, 
            null
        )

        val enableWriteCheckbox: CheckBox = view.findViewById(R.id.enableWriteCheckbox)
        val requireConfirmationCheckbox: CheckBox = view.findViewById(R.id.requireConfirmationCheckbox)
        val autoBackupCheckbox: CheckBox = view.findViewById(R.id.autoBackupCheckbox)

        // Load current settings
        enableWriteCheckbox.isChecked = permissionManager.isFileWriteEnabled()
        requireConfirmationCheckbox.isChecked = permissionManager.requiresConfirmation()
        autoBackupCheckbox.isChecked = permissionManager.isAutoBackupEnabled()

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.ai_permissions_title))
            .setView(view)
            .setPositiveButton(context.getString(R.string.action_save)) { dialog, _ ->
                permissionManager.setFileWriteEnabled(enableWriteCheckbox.isChecked)
                permissionManager.setRequireConfirmation(requireConfirmationCheckbox.isChecked)
                permissionManager.setAutoBackup(autoBackupCheckbox.isChecked)
                onSettingsChanged()
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.action_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
