package com.itsaky.androidide.activities

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
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

class TomIDEUpdater(private val context: Context) {
    
    companion object {
        private const val TAG = "TomIDEUpdater"
        private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/Mohammed-Baqer-null/AndroidIDE-Rv2/refs/heads/dev/appupdater.json"
        private const val DOWNLOAD_NOTIFICATION_ID = 1001
    }
    
    data class UpdateInfo(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String,
        val changelogUrl: String
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
                    withContext(Dispatchers.Main) {
                        showUpdateDialog(updateInfo, changelog)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No updates available", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking for updates", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to check for updates", Toast.LENGTH_SHORT).show()
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
                    Log.e(TAG, "HTTP error: $responseCode")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching update info", e)
                null
            }
        }
    }
    
    private fun parseUpdateInfo(jsonString: String): UpdateInfo? {
        return try {
            val jsonObject = JSONObject(jsonString)
            UpdateInfo(
                versionCode = jsonObject.getInt("versionCode"),
                versionName = jsonObject.getString("versionName"),
                apkUrl = jsonObject.getString("apkUrl"),
                changelogUrl = jsonObject.getString("changelog")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing update info", e)
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
                    "Failed to load changelog"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching changelog", e)
                "Failed to load changelog"
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
            val currentVersionCode = context.packageManager
                .getPackageInfo(context.packageName, 0).versionCode
            updateInfo.versionCode > currentVersionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Package not found", e)
            false
        }
    }
    
    private fun showUpdateDialog(updateInfo: UpdateInfo, changelog: String) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Update Available")
            .setMessage("New version ${updateInfo.versionName} is available!\n\n$changelog")
            .setPositiveButton("Download") { _, _ ->
                downloadAndInstall(updateInfo.apkUrl)
            }
            .setNeutralButton("View in Browser") { _, _ ->
                openInBrowser(updateInfo.apkUrl)
            }
            .setNegativeButton("Later", null)
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
        
        downloadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                val apkFile = downloadApk(apkUrl)
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    if (apkFile != null) {
                        installApk(apkFile)
                    } else {
                        Toast.makeText(context, "Download failed", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading APK", e)
                withContext(Dispatchers.Main) {
                    hideProgressDialog()
                    Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    Log.e(TAG, "HTTP error: $responseCode")
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
                            updateProgress(progress, "$progressMB MB / $totalMB MB")
                        }
                    }
                }
                
                inputStream.close()
                outputStream.close()
                
                apkFile
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading APK", e)
                null
            }
        }
    }
    
    private fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val apkUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
            }
            
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error installing APK", e)
            Toast.makeText(context, "Installation failed", Toast.LENGTH_SHORT).show()
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
                "Please enable 'Install unknown apps' and try again",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun showProgressDialog() {
        val dialogView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 32)
        }
        
        val titleText = TextView(context).apply {
            text = "Downloading Update"
            textSize = 18f
            setPadding(0, 0, 0, 24)
        }
        
        progressIndicator = LinearProgressIndicator(context).apply {
            isIndeterminate = false
            max = 100
            progress = 0
        }
        
        progressText = TextView(context).apply {
            text = "Preparing download..."
            setPadding(0, 16, 0, 0)
        }
        
        dialogView.addView(titleText)
        dialogView.addView(progressIndicator)
        dialogView.addView(progressText)
        
        progressDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setNegativeButton("Cancel") { _, _ ->
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
            Log.e(TAG, "Error opening browser", e)
            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun cancelDownload() {
        downloadJob?.cancel()
        hideProgressDialog()
    }
}