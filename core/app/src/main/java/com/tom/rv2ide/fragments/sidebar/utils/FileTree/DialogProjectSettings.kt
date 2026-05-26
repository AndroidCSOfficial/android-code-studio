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
package com.tom.rv2ide.fragments.sidebar.utils.FileTree

import com.tom.rv2ide.R
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.projects.internal.ProjectManagerImpl
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.DialogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.tom.rv2ide.common.databinding.LayoutDialogProgressBinding

class DialogProjectSettings(private val context: Context) {

    private var dialog: androidx.appcompat.app.AlertDialog? = null

    fun show() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_project_settings, null)
        val backupProjBtn: MaterialButton = view.findViewById(R.id.backupBtn)
        
        backupProjBtn.setOnClickListener {
            val projectPath = ProjectManagerImpl.getInstance().projectDir.absolutePath.toString()
            val projectFile = File(projectPath)
            backupProject(projectFile)
        }

        dialog = MaterialAlertDialogBuilder(context)
            .setView(view)
            .setCancelable(true)
            .create()
        dialog?.show()
    }

    fun dismiss() {
        dialog?.findViewById<View>(R.id.coordinator)?.let { rootView ->
            rootView.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(250)
                .withEndAction { 
                    dialog?.dismiss()
                    dialog = null
                }
                .start()
        } ?: run {
            dialog?.dismiss()
            dialog = null
        }
    }

    private fun backupProject(project: File, onComplete: () -> Unit = {}) {
        val backupDir = File(Environment.PROJECTS_DIR, "backed_up_projects")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }

        val timestamp =
            java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
                .format(java.util.Date())
        val backupFileName = "${project.name}_backup_$timestamp.zip"
        val backupFile = File(backupDir, backupFileName)

        val builder = DialogUtils.newMaterialDialogBuilder(context)
        val binding = LayoutDialogProgressBinding.inflate(LayoutInflater.from(context))

        binding.message.visibility = View.VISIBLE
        binding.message.text = context.getString(R.string.project_backing_up)
        binding.progress.isIndeterminate = true

        builder.setTitle(context.getString(R.string.project_backup_in_progress))
        builder.setMessage(context.getString(R.string.project_backup_message, project.name))
        builder.setView(binding.root)
        builder.setCancelable(false)

        val progressDialog = builder.show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                    project.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(project).path
                            if (
                                !relativePath.startsWith("build/") &&
                                !relativePath.contains("/build/") &&
                                !relativePath.startsWith(".androidide/") &&
                                !relativePath.startsWith(".gradle/") &&
                                !relativePath.contains("/.gradle/") &&
                                !relativePath.startsWith(".idea/") &&
                                !relativePath.contains("/.idea/")
                            ) {
                                val zipEntry = ZipEntry(relativePath)
                                zipOut.putNextEntry(zipEntry)
                                file.inputStream().use { input -> input.copyTo(zipOut) }
                                zipOut.closeEntry()
                            }
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    val successBuilder = DialogUtils.newMaterialDialogBuilder(context)
                    successBuilder.setTitle(context.getString(R.string.project_backup_completed))
                    successBuilder.setMessage(
                        "Project backed up successfully!\n\nLocation:\n${backupFile.absolutePath}"
                    )
                    successBuilder.setPositiveButton(context.getString(R.string.action_ok)) { d, _ ->
                        d.dismiss()
                        onComplete()
                    }
                    successBuilder.show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    val errorBuilder = DialogUtils.newMaterialDialogBuilder(context)
                    errorBuilder.setTitle(context.getString(R.string.project_backup_failed))
                    errorBuilder.setMessage(context.getString(R.string.project_backup_failed_message, e.localizedMessage))
                    errorBuilder.setPositiveButton(context.getString(R.string.action_ok), null)
                    errorBuilder.show()
                }
            }
        }
    }
}