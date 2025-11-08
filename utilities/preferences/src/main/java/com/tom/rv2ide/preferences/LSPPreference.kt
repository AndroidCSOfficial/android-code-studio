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

package com.tom.rv2ide.preferences

import android.view.LayoutInflater
import android.view.View
import android.widget.ProgressBar
import androidx.preference.Preference
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.preferences.databinding.LayoutDialogTextViewBinding
import com.tom.rv2ide.preferences.observers.LSPStateObserver
import com.tom.rv2ide.resources.R.string
import com.tom.rv2ide.utils.AppRestartDialog
import com.tom.rv2ide.utils.Environment
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

abstract class LSPPreference(
    val hint: Int? = null,
    val setValue: ((Any) -> Unit)? = null,
    val getValue: (() -> Any)? = null,
    val serverId: String? = null,
    val isInstalled: (() -> Boolean)? = null,
) : DialogPreference() {

  private var downloadUrl: String? = null
  private var serverVersion: String? = null

  override fun onConfigureDialog(preference: Preference, dialog: MaterialAlertDialogBuilder) {
    super.onConfigureDialog(preference, dialog)
    val binding = LayoutDialogTextViewBinding.inflate(LayoutInflater.from(dialog.context))
    onConfigureTextView(binding.status)
    dialog.setView(binding.root)

    val installed = isInstalled?.invoke() ?: true

    if (serverId != null) {
      if (installed) {
        // Show uninstall button when installed
        dialog.setNeutralButton(string.lsp_server_uninstall) { _, _ ->
          showUninstallConfirmation(dialog.context, serverId)
        }
      } else {
        // Show download button when not installed
        dialog.setNeutralButton(string.updater_download) { _, _ ->
          downloadServer(dialog.context, serverId)
        }

        // Fetch server info
        fetchServerInfo(serverId) { url, version ->
          downloadUrl = url
          serverVersion = version
        }
      }
    }

    dialog.setPositiveButton(android.R.string.ok) { iface, _ -> iface.dismiss() }
    dialog.setNegativeButton(android.R.string.cancel) { iface, _ -> iface.dismiss() }
  }

  protected open fun onConfigureTextView(textView: MaterialTextView) {
    val value = prefValue()
    val context = textView.context

    // Get the string resource and format it with the value
    val formattedText =
        if (hint != null) {
          context.getString(hint, value)
        } else {
          value
        }

    textView.text = formattedText
  }

  override fun onPreferenceChanged(preference: Preference, newValue: Any?): Boolean {
    return true
  }

  private fun prefValue(): String {
    return getValue?.let {
      when (val value = it()) {
        is String -> value
        is Int -> value.toString()
        else -> value.toString()
      }
    } ?: ""
  }

  private fun showUninstallConfirmation(context: android.content.Context, serverId: String) {
    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(string.lsp_server_uninstall_title))
        .setMessage(context.getString(string.lsp_server_uninstall_message, serverId))
        .setPositiveButton(string.lsp_server_uninstall) { _, _ ->
          uninstallServer(context, serverId)
        }
        .setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  private fun uninstallServer(context: android.content.Context, serverId: String) {
    val progressDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(string.lsp_server_uninstalling, serverId))
            .setMessage(context.getString(string.lsp_server_please_wait))
            .setCancelable(false)
            .create()

    progressDialog.show()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val serverDir = File(Environment.HOME, "acs/servers/${serverId.lowercase()}")

        if (serverDir.exists()) {
          val deleted = serverDir.deleteRecursively()

          withContext(Dispatchers.Main) {
            progressDialog.dismiss()
            if (deleted) {
              showSuccessDialog(
                  context,
                  context.getString(string.lsp_server_uninstall_success, serverId),
              )
              LSPStateObserver.notifyServerStateChanged()
              // ask user to restart the app
              AppRestartDialog.show(context)
            } else {
              showErrorDialog(
                  context,
                  context.getString(string.lsp_server_uninstall_failed, serverId),
              )
            }
          }
        } else {
          withContext(Dispatchers.Main) {
            progressDialog.dismiss()
            showErrorDialog(context, context.getString(string.lsp_server_not_found, serverId))
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          progressDialog.dismiss()
          showErrorDialog(
              context,
              context.getString(string.lsp_server_uninstall_error, serverId, e.message ?: ""),
          )
        }
      }
    }
  }

  private fun fetchServerInfo(serverId: String, callback: (String?, String?) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val json = URL(manifestUrl()).readText()
        val jsonObject = JSONObject(json)
        val serversArray = jsonObject.getJSONArray("Servers")

        for (i in 0 until serversArray.length()) {
          val server = serversArray.getJSONObject(i)
          if (server.getString("id") == serverId) {
            val link = server.getString("link")
            val version = server.getString("version")
            if (link != "null") {
              withContext(Dispatchers.Main) { callback(link, version) }
            }
            break
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) { callback(null, null) }
      }
    }
  }

  private fun downloadServer(context: android.content.Context, serverId: String) {
    val progressLayout =
        android.widget.LinearLayout(context).apply {
          orientation = android.widget.LinearLayout.VERTICAL
          setPadding(24, 24, 24, 24)
        }

    val progressText =
        MaterialTextView(context).apply {
          text = context.getString(string.lsp_server_preparing_download)
          textSize = 16f
          setPadding(0, 0, 0, 24)
        }

    val progressBar =
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
          isIndeterminate = false
          max = 100
          progress = 0
          layoutParams =
              android.widget.LinearLayout.LayoutParams(
                  android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                  android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
              )
        }

    val progressPercentage =
        MaterialTextView(context).apply {
          text = context.getString(string.lsp_server_progress_percentage, 0)
          textSize = 14f
          setPadding(0, 16, 0, 0)
          textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

    progressLayout.addView(progressText)
    progressLayout.addView(progressBar)
    progressLayout.addView(progressPercentage)

    val progressDialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(string.lsp_server_downloading, serverId))
            .setView(progressLayout)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
              // TODO: Implement download cancellation
              dialog.dismiss()
            }
            .create()

    progressDialog.show()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        withContext(Dispatchers.Main) {
          progressText.text = context.getString(string.lsp_server_fetching_info)
        }

        val json = URL(manifestUrl()).readText()
        val jsonObject = JSONObject(json)
        val serversArray = jsonObject.getJSONArray("Servers")

        var downloadLink: String? = null
        for (i in 0 until serversArray.length()) {
          val server = serversArray.getJSONObject(i)
          if (server.getString("id") == serverId) {
            downloadLink = server.getString("link")
            if (downloadLink == "null") {
              downloadLink = null
            }
            break
          }
        }

        if (downloadLink == null) {
          withContext(Dispatchers.Main) {
            progressDialog.dismiss()
            showErrorDialog(
                context,
                context.getString(string.lsp_server_no_download_link, serverId),
            )
          }
          return@launch
        }

        val serversDir = File(Environment.HOME, "acs/servers")
        serversDir.mkdirs()

        val serverDir = File(serversDir, serverId.lowercase())
        serverDir.mkdirs()

        withContext(Dispatchers.Main) {
          progressText.text = context.getString(string.lsp_server_connecting)
        }

        val connection = URL(downloadLink).openConnection()
        connection.connect()

        val fileLength = connection.contentLength
        val inputStream = connection.getInputStream()

        val tempFile = File(serversDir, "temp_${serverId}.zip")
        val outputStream = FileOutputStream(tempFile)

        withContext(Dispatchers.Main) {
          progressText.text = context.getString(string.lsp_server_downloading_status)
        }

        val buffer = ByteArray(8192)
        var totalRead = 0L
        var len: Int

        while (inputStream.read(buffer).also { len = it } > 0) {
          outputStream.write(buffer, 0, len)
          totalRead += len

          if (fileLength > 0) {
            val progress = ((totalRead * 100) / fileLength).toInt()
            withContext(Dispatchers.Main) {
              progressBar.progress = progress
              progressPercentage.text =
                  context.getString(string.lsp_server_progress_percentage, progress)
            }
          }
        }

        outputStream.close()
        inputStream.close()

        withContext(Dispatchers.Main) {
          progressText.text = context.getString(string.lsp_server_extracting)
          progressBar.isIndeterminate = true
        }

        val zipInputStream = ZipInputStream(tempFile.inputStream())
        var zipEntry = zipInputStream.nextEntry
        var extractedCount = 0

        while (zipEntry != null) {
          val file = File(serverDir, zipEntry.name)

          if (zipEntry.isDirectory) {
            file.mkdirs()
          } else {
            file.parentFile?.mkdirs()
            val fileOutputStream = FileOutputStream(file)
            val extractBuffer = ByteArray(8192)
            var extractLen: Int
            while (zipInputStream.read(extractBuffer).also { extractLen = it } > 0) {
              fileOutputStream.write(extractBuffer, 0, extractLen)
            }
            fileOutputStream.close()

            if (file.extension.isEmpty() || file.name.endsWith(".sh")) {
              file.setExecutable(true)
            }

            extractedCount++
          }

          zipInputStream.closeEntry()
          zipEntry = zipInputStream.nextEntry
        }

        zipInputStream.close()

        tempFile.delete()

        withContext(Dispatchers.Main) {
          progressDialog.dismiss()
          showSuccessDialog(
              context,
              context.getString(string.lsp_server_install_success, serverId, extractedCount),
          )
          LSPStateObserver.notifyServerStateChanged()
          AppRestartDialog.show(context)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        withContext(Dispatchers.Main) {
          progressDialog.dismiss()
          showErrorDialog(
              context,
              context.getString(string.lsp_server_download_failed, serverId, e.message ?: ""),
          )
        }
      }
    }
  }

  private fun showErrorDialog(context: android.content.Context, message: String) {
    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(string.lsp_server_error_title))
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  private fun showSuccessDialog(context: android.content.Context, message: String) {
    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(string.lsp_server_success_title))
        .setMessage(message)
        .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  // TODO: allow the user to change repo url
  private fun manifestUrl(): String =
      "https://raw.githubusercontent.com/AndroidCSOfficial/acs-language-servers/refs/heads/main/servers-manifest.json"

  private fun showToast(context: android.content.Context, message: String) {
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
  }
}
