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

package com.tom.rv2ide.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.tom.rv2ide.resources.R
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.*
import org.json.JSONObject

/** * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null */
class TomIDEUpdater(private val context: Context) {

  companion object {
    private const val TAG = "TomIDEUpdater"
    private const val UPDATE_JSON_URL =
        "https://raw.githubusercontent.com/AndroidCSOfficial/android-code-studio/refs/heads/dev/updater.json"
    private const val DOWNLOAD_NOTIFICATION_ID = 1001
  }

  data class ArchVariant(val versionCode: Int, val versionName: String, val apkUrl: String)

  data class UpdateInfo(
      val baseVersionCode: Int,
      val baseVersionName: String,
      val variants: Map<String, ArchVariant>,
      val changelogUrl: String,
  )

  private var downloadJob: Job? = null
  private var progressDialog: androidx.appcompat.app.AlertDialog? = null
  private var progressIndicator: LinearProgressIndicator? = null
  private var progressText: TextView? = null

  fun checkForUpdates() {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val updateInfo = fetchUpdateInfo()
        if (updateInfo != null && isUpdateAvailable(updateInfo)) {
          val changelog = fetchChangelog(updateInfo.changelogUrl)
          withContext(Dispatchers.Main) { showUpdateDialog(updateInfo, changelog) }
        } else {
          withContext(Dispatchers.Main) {
            Toast.makeText(
                    context,
                    context.getString(R.string.updater_no_updates_available),
                    Toast.LENGTH_SHORT,
                )
                .show()
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error checking for updates", e)
        withContext(Dispatchers.Main) {
          Toast.makeText(
                  context,
                  context.getString(R.string.updater_check_failed),
                  Toast.LENGTH_SHORT,
              )
              .show()
        }
      }
    }
  }

  private suspend fun fetchUpdateInfo(): UpdateInfo? {
    return withContext(Dispatchers.IO) {
      try {
        val url = URL(UPDATE_JSON_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
          val response = connection.inputStream.bufferedReader().use { it.readText() }
          parseUpdateInfo(response)
        } else {
          Log.e(TAG, context.getString(R.string.updater_http_error, responseCode))
          null
        }
      } catch (e: Exception) {
        Log.e(TAG, context.getString(R.string.updater_fetch_error), e)
        null
      }
    }
  }

  private fun parseUpdateInfo(jsonString: String): UpdateInfo? {
    return try {
      val jsonObject = JSONObject(jsonString)
      val variantsJson = jsonObject.getJSONObject("variants")
      val variants = mutableMapOf<String, ArchVariant>()

      for (key in variantsJson.keys()) {
        val variantJson = variantsJson.getJSONObject(key)
        variants[key] =
            ArchVariant(
                versionCode = variantJson.getInt("versionCode"),
                versionName = variantJson.getString("versionName"),
                apkUrl = variantJson.getString("apkUrl"),
            )
      }

      UpdateInfo(
          baseVersionCode = jsonObject.getInt("baseVersionCode"),
          baseVersionName = jsonObject.getString("baseVersionName"),
          variants = variants,
          changelogUrl = jsonObject.getString("changelog"),
      )
    } catch (e: Exception) {
      Log.e(TAG, context.getString(R.string.updater_parse_error), e)
      null
    }
  }

  private suspend fun fetchChangelog(changelogUrl: String): String {
    return withContext(Dispatchers.IO) {
      try {
        val url = URL(changelogUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
          val markdown = connection.inputStream.bufferedReader().use { it.readText() }
          parseMarkdownToPlainText(markdown)
        } else {
          context.getString(R.string.updater_changelog_failed)
        }
      } catch (e: Exception) {
        Log.e(TAG, context.getString(R.string.updater_changelog_error), e)
        context.getString(R.string.updater_changelog_failed)
      }
    }
  }

  private fun parseMarkdownToPlainText(markdown: String): String {
    var text = markdown

    // Remove code blocks
    text = text.replace(Regex("```[\\s\\S]*?```"), "")
    text = text.replace(Regex("`[^`]*`"), "")

    // Convert headers
    text = text.replace(Regex("^#{1,6}\\s*(.*)$", RegexOption.MULTILINE), "$1")

    // Convert bold and italic
    text = text.replace(Regex("\\*\\*\\*([^*]*)\\*\\*\\*"), "$1")
    text = text.replace(Regex("\\*\\*([^*]*)\\*\\*"), "$1")
    text = text.replace(Regex("\\*([^*]*)\\*"), "$1")
    text = text.replace(Regex("___([^_]*)___"), "$1")
    text = text.replace(Regex("__([^_]*)__"), "$1")
    text = text.replace(Regex("_([^_]*)_"), "$1")

    // Convert links
    text = text.replace(Regex("\\[([^\\]]*)\\]\\([^\\)]*\\)"), "$1")

    // Convert lists
    text = text.replace(Regex("^[-*+]\\s+", RegexOption.MULTILINE), "• ")
    text = text.replace(Regex("^\\d+\\.\\s+", RegexOption.MULTILINE), "• ")

    // Clean up extra whitespace
    text = text.replace(Regex("\\n\\s*\\n"), "\n\n")
    text = text.trim()

    return text
  }

  private fun isUpdateAvailable(updateInfo: UpdateInfo): Boolean {
    return try {
      val currentVersionCode =
          context.packageManager.getPackageInfo(context.packageName, 0).versionCode

      val currentArch = getDeviceArchitecture()
      val availableVariant =
          updateInfo.variants[currentArch]
              ?: updateInfo.variants["armeabi-v7a"] // Fallback for arm64 devices

      if (availableVariant != null) {
        Log.d(
            TAG,
            context.getString(
                R.string.updater_version_check_log,
                currentVersionCode,
                availableVariant.versionCode,
                currentArch,
            ),
        )
        availableVariant.versionCode > currentVersionCode
      } else {
        Log.w(TAG, context.getString(R.string.updater_no_compatible_variant, currentArch))
        false
      }
    } catch (e: PackageManager.NameNotFoundException) {
      Log.e(TAG, context.getString(R.string.updater_package_not_found), e)
      false
    }
  }

  private fun getDeviceArchitecture(): String {
    val supportedAbis =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          Build.SUPPORTED_ABIS
        } else {
          @Suppress("DEPRECATION")
          arrayOf(Build.CPU_ABI, Build.CPU_ABI2).filterNotNull().toTypedArray()
        }

    Log.d(TAG, context.getString(R.string.updater_supported_abis, supportedAbis.contentToString()))

    // Prioritize 64-bit architecture
    for (abi in supportedAbis) {
      when (abi) {
        "arm64-v8a" -> return "arm64-v8a"
        "x86_64" -> return "x86_64"
      }
    }

    // Fallback to 32-bit architectures
    for (abi in supportedAbis) {
      when (abi) {
        "armeabi-v7a",
        "armeabi" -> return "armeabi-v7a"
        "x86" -> return "x86"
      }
    }

    // Default fallback
    Log.w(TAG, context.getString(R.string.updater_unknown_arch_fallback))
    return "armeabi-v7a"
  }

  private fun getVariantForCurrentArchitecture(updateInfo: UpdateInfo): ArchVariant? {
    val currentArch = getDeviceArchitecture()
    Log.d(TAG, context.getString(R.string.updater_device_arch, currentArch))
    Log.d(
        TAG,
        context.getString(R.string.updater_available_variants, updateInfo.variants.keys.toString()),
    )

    // Try to get the exact architecture match
    var variant = updateInfo.variants[currentArch]

    // If no exact match, try fallbacks
    if (variant == null) {
      Log.d(TAG, context.getString(R.string.updater_trying_fallbacks, currentArch))
      when (currentArch) {
        "arm64-v8a" -> {
          // arm64 can run armeabi-v7a
          variant = updateInfo.variants["armeabi-v7a"]
          if (variant != null) {
            Log.d(TAG, context.getString(R.string.updater_using_arm32_fallback))
          }
        }
        "x86_64" -> {
          // x86_64 can run x86
          variant = updateInfo.variants["x86"]
          if (variant != null) {
            Log.d(TAG, context.getString(R.string.updater_using_x86_fallback))
          }
        }
      }
    }

    // If still no match, try to get any available variant
    if (variant == null && updateInfo.variants.isNotEmpty()) {
      Log.w(TAG, context.getString(R.string.updater_using_first_variant))
      variant = updateInfo.variants.values.first()
    }

    return variant
  }

  private fun showUpdateDialog(updateInfo: UpdateInfo, changelog: String) {
    val currentArch = getDeviceArchitecture()
    val availableVariant = getVariantForCurrentArchitecture(updateInfo)

    if (availableVariant == null) {
      Toast.makeText(
              context,
              context.getString(R.string.updater_no_compatible_update),
              Toast.LENGTH_LONG,
          )
          .show()
      return
    }

    val availableArchs = updateInfo.variants.keys.joinToString(", ")

    val message = buildString {
      append(
          context.getString(R.string.updater_new_version_available, availableVariant.versionName)
      )
      append("\n")
      append(context.getString(R.string.updater_current_architecture, currentArch))
      append("\n")
      append(context.getString(R.string.updater_target_version_code, availableVariant.versionCode))
      append("\n")
      append(context.getString(R.string.updater_available_variants, availableArchs))
      append("\n\n")
      append(changelog)
    }

    val dialog =
        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.updater_update_available))
            .setMessage(message)
            .setPositiveButton(context.getString(R.string.updater_download)) { _, _ ->
              downloadAndInstall(availableVariant.apkUrl)
            }
            .setNeutralButton(context.getString(R.string.updater_view_in_browser)) { _, _ ->
              openInBrowser(availableVariant.apkUrl)
            }
            .setNegativeButton(context.getString(R.string.updater_later), null)
            .setCancelable(true)
            .create()

    dialog.show()
  }

  private fun downloadAndInstall(apkUrl: String) {
    if (!hasInstallPermission()) {
      requestInstallPermission()
      return
    }

    showProgressDialog()

    downloadJob =
        CoroutineScope(Dispatchers.IO).launch {
          try {
            val apkFile = downloadApk(apkUrl)
            withContext(Dispatchers.Main) {
              hideProgressDialog()
              if (apkFile != null) {
                installApk(apkFile)
              } else {
                Toast.makeText(
                        context,
                        context.getString(R.string.updater_download_failed),
                        Toast.LENGTH_SHORT,
                    )
                    .show()
              }
            }
          } catch (e: Exception) {
            Log.e(TAG, context.getString(R.string.updater_download_error), e)
            withContext(Dispatchers.Main) {
              hideProgressDialog()
              Toast.makeText(
                      context,
                      context.getString(R.string.updater_download_failed_with_error, e.message),
                      Toast.LENGTH_SHORT,
                  )
                  .show()
            }
          }
        }
  }

  private suspend fun downloadApk(apkUrl: String): File? {
    return withContext(Dispatchers.IO) {
      try {
        val url = URL(apkUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 30000
        connection.readTimeout = 30000

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
          Log.e(TAG, context.getString(R.string.updater_http_error, responseCode))
          return@withContext null
        }

        val contentLength = connection.contentLength
        val inputStream = connection.inputStream

        // Create temp file
        val apkFile = File(context.getExternalFilesDir(null), "update.apk")
        val outputStream = FileOutputStream(apkFile)

        val buffer = ByteArray(8192)
        var totalBytes = 0
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          outputStream.write(buffer, 0, bytesRead)
          totalBytes += bytesRead

          if (contentLength > 0) {
            val progress = (totalBytes * 100) / contentLength
            val progressMB = totalBytes / (1024 * 1024)
            val totalMB = contentLength / (1024 * 1024)

            withContext(Dispatchers.Main) {
              updateProgress(
                  progress,
                  context.getString(R.string.updater_progress_format, progressMB, totalMB),
              )
            }
          }
        }

        inputStream.close()
        outputStream.close()

        apkFile
      } catch (e: Exception) {
        Log.e(TAG, context.getString(R.string.updater_download_apk_error), e)
        null
      }
    }
  }

  private fun installApk(apkFile: File) {
    try {
      val intent = Intent(Intent.ACTION_VIEW)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val apkUri =
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      } else {
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
      }

      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, context.getString(R.string.updater_install_error), e)
      Toast.makeText(
              context,
              context.getString(R.string.updater_installation_failed),
              Toast.LENGTH_SHORT,
          )
          .show()
    }
  }

  private fun hasInstallPermission(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      context.packageManager.canRequestPackageInstalls()
    } else {
      true
    }
  }

  private fun requestInstallPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
      intent.data = Uri.parse("package:${context.packageName}")

      if (context is Activity) {
        context.startActivityForResult(intent, 1234)
      } else {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
      }

      Toast.makeText(
              context,
              context.getString(R.string.updater_enable_install_permission),
              Toast.LENGTH_LONG,
          )
          .show()
    }
  }

  private fun showProgressDialog() {
    val dialogView =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(64, 32, 64, 32)
        }

    val titleText =
        TextView(context).apply {
          text = context.getString(R.string.updater_downloading_update)
          textSize = 18f
          setPadding(0, 0, 0, 24)
        }

    progressIndicator =
        LinearProgressIndicator(context).apply {
          isIndeterminate = false
          max = 100
          progress = 0
        }

    progressText =
        TextView(context).apply {
          text = context.getString(R.string.updater_preparing_download)
          setPadding(0, 16, 0, 0)
        }

    dialogView.addView(titleText)
    dialogView.addView(progressIndicator)
    dialogView.addView(progressText)

    progressDialog =
        MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setNegativeButton(context.getString(R.string.updater_cancel)) { _, _ ->
              cancelDownload()
            }
            .setCancelable(false)
            .create()

    progressDialog?.show()
  }

  private fun updateProgress(progress: Int, text: String) {
    progressIndicator?.progress = progress
    progressText?.text = text
  }

  private fun hideProgressDialog() {
    progressDialog?.dismiss()
    progressDialog = null
    progressIndicator = null
    progressText = null
  }

  private fun openInBrowser(url: String) {
    try {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } catch (e: Exception) {
      Log.e(TAG, context.getString(R.string.updater_browser_error), e)
      Toast.makeText(
              context,
              context.getString(R.string.updater_cannot_open_browser),
              Toast.LENGTH_SHORT,
          )
          .show()
    }
  }

  fun cancelDownload() {
    downloadJob?.cancel()
    hideProgressDialog()
  }
}
