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
package com.tom.rv2ide.handlers.system

import android.content.Context
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Toast
import androidx.lifecycle.LifecycleCoroutineScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.customizablecardview.CustomizableCardView
import com.tom.rv2ide.R
import com.tom.rv2ide.activities.IDEConfigurations
import com.tom.rv2ide.activities.IdeConfigurations.*
import com.tom.rv2ide.fragments.sidebar.utils.showErrorDialog
import com.tom.rv2ide.handlers.IConfigHandler
import com.tom.rv2ide.handlers.system.installer.IPackageInstaller
import com.tom.rv2ide.ideconfigurations.utils.IDEUtils
import com.tom.rv2ide.managers.PreferenceManager
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.GeneralFileUtils
import com.tom.rv2ide.utils.IBuildSystemUtils
import com.tom.rv2ide.utils.flashError
import com.tom.rv2ide.utils.flashProgress
import com.tom.rv2ide.utils.flashSuccess
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null */
class INdk(
    private val context: Context,
    private val prefManager: PreferenceManager,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val isDarkTheme: Boolean,
) : IConfigHandler {

  override val handlerName: String = "NDK"
  override val cardHolderId: Int = R.id.ndkCardHolder
  override val removeButtonId: Int = R.id.remove_btn
  override val reinstallButtonId: Int = R.id.reinstall_btn

  private val NDK_DIR = File(Environment.HOME, "android-sdk/ndk")
  private val ACS_DIR = File(Environment.HOME, "acs")

  private external fun processNdkInstall(version: String, arch: String, filename: String)

  override fun updateStatus() {

    val activity = context as? IDEConfigurations
    val cardHolder = activity?.findViewById<CustomizableCardView>(R.id.ndkCardHolder)

    val listedVersions = GeneralFileUtils.listDirsInDirectory(NDK_DIR)
    val availableVersions = listedVersions.map { it.name }.sorted()

    val statusText: String
    val color: Int
    val removeBtn = (context as? android.app.Activity)?.findViewById<View>(removeButtonId)

    when {
      availableVersions.isEmpty() -> {
        statusText = "Not installed"
        color = Color.RED
        removeBtn?.visibility = View.GONE
      }
      availableVersions.size == 1 -> {
        statusText = "${availableVersions.first()}"
        color = if (isDarkTheme) Color.GREEN else Color.rgb(0, 75, 0)
        removeBtn?.visibility = View.VISIBLE
      }
      availableVersions.size == 2 -> {
        statusText = "${availableVersions.joinToString(" & ")}"
        color = if (isDarkTheme) Color.GREEN else Color.rgb(0, 75, 0)
        removeBtn?.visibility = View.VISIBLE
      }
      else -> {
        statusText =
            "${availableVersions.first()}...${availableVersions.last()} (${availableVersions.size} versions)"
        color = if (isDarkTheme) Color.GREEN else Color.rgb(0, 75, 0)
        removeBtn?.visibility = View.VISIBLE
      }
    }

    val fullText = context.getString(R.string.cfg_installed_ndk_version, statusText)
    val spannableString = SpannableString(fullText)

    val startIndex = fullText.indexOf(statusText)
    if (startIndex != -1) {
      val endIndex = startIndex + statusText.length
      spannableString.setSpan(
          ForegroundColorSpan(color),
          startIndex,
          endIndex,
          SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE,
      )
    }

    cardHolder?.setCardSummary(spannableString)
  }

  override fun onRemoveClick() {
    val listedVersions = GeneralFileUtils.listDirsInDirectory(NDK_DIR)
    val availableVersions = listedVersions.map { it.name }.sorted()

    when {
      availableVersions.isEmpty() -> {
        Toast.makeText(context, "No NDK versions found to remove", Toast.LENGTH_SHORT).show()
      }
      availableVersions.size == 1 -> {
        removeNdkVersion(availableVersions.first())
      }
      else -> {
        showVersionSelectionDialog(
            title = "Select NDK Version to Remove",
            versions = availableVersions,
            onVersionSelected = { selectedVersion -> removeNdkVersion(selectedVersion) },
        )
      }
    }
  }

  override fun onReinstallClick() {
    if (!isACSProgramExists()) {
      showErrorDialog(
          ctx = context,
          title = context.getString(R.string.no_acs_title),
          message = context.getString(R.string.no_acs_summary),
          negativeBtnTitle = "OK",
      )
      return
    }

    // Get the version from the dropdown in IDEConfigurations
    val activity = context as? IDEConfigurations
    val ndkDropdown =
        activity?.findViewById<android.widget.AutoCompleteTextView>(R.id.ndks_dropdown)
    val selectedVersion = ndkDropdown?.text?.toString()?.trim()

    if (
        selectedVersion.isNullOrEmpty() ||
            selectedVersion == "Loading versions..." ||
            selectedVersion == "No versions available"
    ) {
      flashError("Please select ndk version from the dropdown first")
      return
    }

    initializeAcs("android-native-kit", selectedVersion)
  }

  override fun initialize() {
    // Initialize NDK if needed
  }

  override fun cleanup() {
    // Cleanup NDK resources if needed
  }

  private fun removeNdkVersion(version: String) {
    val utils = IDEUtils()
    MaterialAlertDialogBuilder(context)
        .setTitle("Remove NDK")
        .setMessage("Are you sure you want to remove NDK version $version?")
        .setPositiveButton("Remove") { dialog, _ ->
          if (utils.deleteNdk(context, version)) {
            if (version == getVersion()) {
              prefManager.putBoolean("ndk_installed", false)
            }
            Toast.makeText(context, "Successfully removed NDK $version", Toast.LENGTH_SHORT).show()
            updateStatus()
          } else {
            Toast.makeText(context, "Failed to remove NDK $version", Toast.LENGTH_SHORT).show()
          }
          dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .create()
        .show()
  }

  private fun showVersionSelectionDialog(
      title: String,
      versions: List<String>,
      onVersionSelected: (String) -> Unit,
  ) {
    MaterialAlertDialogBuilder(context)
        .setTitle(title)
        .setItems(versions.toTypedArray()) { dialog, which ->
          val selectedVersion = versions[which]
          onVersionSelected(selectedVersion)
          dialog.dismiss()
        }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        .create()
        .show()
  }

  private fun initializeAcs(packageId: String, version: String) {
    showErrorDialog(
        ctx = context,
        title = context.getString(R.string.acs_install_title),
        message = context.getString(R.string.acs_ask_install_message, version),
        negativeBtnTitle = context.getString(R.string.btn_proceed_str),
        onNegativeClick = {
          val cpuArch = getCpuArchitecture()
          if (!listOf("armeabi-v7a", "arm64-v8a", "x86_64").contains(cpuArch)) {
            flashError("Unsupported architecture: $cpuArch")
            return@showErrorDialog
          }

          val architecture: AcsCommandInterface.Architecture =
              when (cpuArch) {
                "armeabi-v7a" -> AcsCommandInterface.Architecture.ARM_V7A
                "arm64-v8a" -> AcsCommandInterface.Architecture.ARM64_V8A
                "x86_64" -> AcsCommandInterface.Architecture.X86_64
                else -> throw IllegalStateException("This should never happen")
              }

          flashProgress(configure = { message("Getting package information...") }) { infoFlashbar ->
            lifecycleScope.launch(Dispatchers.Main) {
              try {
                val filenameResult =
                    withContext(Dispatchers.IO) {
                      AcsCommandInterface.getPackageField(
                          manifestUrl = AcsProvider.getManifestUrl,
                          architecture = architecture,
                          packageId = packageId,
                          field = AcsCommandInterface.ManifestField.FILENAME,
                          version = version,
                      )
                    }

                infoFlashbar.dismiss()

                if (!filenameResult.success) {
                  flashError("Failed to get package filename: ${filenameResult.errorOutput}")
                  return@launch
                }

                val filename = filenameResult.output.trim()
                if (filename.isEmpty()) {
                  flashError("Empty filename received from manifest")
                  return@launch
                }

                flashProgress(configure = { message("Downloading $version...") }) { downloadFlashbar
                  ->
                  lifecycleScope.launch(Dispatchers.IO) {
                    try {
                      val downloadResult =
                          AcsProvider.acsRunner(
                              packageId = packageId,
                              artifactVersion = version,
                              arch = architecture,
                          )

                      withContext(Dispatchers.Main) {
                        downloadFlashbar.dismiss()

                        if (downloadResult.output.contains("successfully", ignoreCase = true)) {
                          val successFlash = flashSuccess("Successfully downloaded")

                          lifecycleScope.launch(Dispatchers.IO) {
                            delay(1000)
                            withContext(Dispatchers.Main) { successFlash?.dismiss() }

                            withContext(Dispatchers.Main) {
                              flashProgress(configure = { message("Extracting $version...") }) {
                                  extractFlashbar ->
                                lifecycleScope.launch(Dispatchers.IO) {
                                  try {
                                    val installer = IPackageInstaller()
                                    val archiveFile = File(ACS_DIR, filename)

                                    installer.extractXzArchive(
                                        archiveFile.absolutePath,
                                        NDK_DIR.absolutePath,
                                    )

                                    withContext(Dispatchers.Main) {
                                      extractFlashbar.dismiss()
                                      prefManager.putBoolean("ndk_installed", true)
                                      flashSuccess("Installation completed successfully!")
                                      renameDir(NDK_DIR.absolutePath, "$version")
                                      updateNdkBuild(version)
                                      updateStatus()
                                    }
                                  } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                      extractFlashbar.dismiss()
                                      flashError("Installation failed: ${e.message}")
                                      e.printStackTrace()
                                    }
                                  }
                                }
                              }
                            }
                          }
                        } else {
                          showErrorDialog(
                              ctx = context,
                              title = "Download Failed",
                              message =
                                  "Package verification failed:\n${downloadResult.output}\n${downloadResult.errorOutput}",
                              negativeBtnTitle = "OK",
                          )
                        }
                      }
                    } catch (e: Exception) {
                      withContext(Dispatchers.Main) {
                        downloadFlashbar.dismiss()
                        flashError("Download failed: ${e.message}")
                        e.printStackTrace()
                      }
                    }
                  }
                }
              } catch (e: Exception) {
                infoFlashbar.dismiss()
                flashError("Failed to get package info: ${e.message}")
                e.printStackTrace()
              }
            }
          }
        },
    )
  }

  private fun updateNdkBuild(version: String) {
    val build_script = File(NDK_DIR, "$version/ndk-build")
    if (build_script.exists()) {
      build_script.writeText(
          """
    #!/bin/sh
    DIR=${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)
    ${Environment.BIN_DIR}/bash "${'$'}DIR/build/ndk-build" "${'$'}@"
            """
              .trimIndent()
      )
      build_script.setExecutable(true)
    }
  }

  private fun renameDir(parentDir: String, newName: String = "ndk") {
    val parent = File(parentDir)

    if (!parent.exists() || !parent.isDirectory) {
      return
    }

    parent.listFiles()?.forEach { file ->
      if (file.isDirectory && file.name.startsWith("android-ndk")) {
        val newFile = File(parent, newName)

        // Handle name conflicts by adding a number suffix
        var finalNewFile = newFile
        var counter = 1
        while (finalNewFile.exists()) {
          finalNewFile = File(parent, "$newName-$counter")
          counter++
        }

        if (file.renameTo(finalNewFile)) {
          println("Renamed: ${file.name} -> ${finalNewFile.name}")
        } else {
          println("Failed to rename: ${file.name}")
        }
      }
    }
  }

  private fun isACSProgramExists(): Boolean {
    // DEPRECATED
    // val program: File = File(context.filesDir.parent, "usr/bin/acs")
    // return GeneralFileUtils.isFileExists(program.absoluteFile)
    return true // using the acs library which is always going to be available
  }

  private fun getVersion(): String {
    return (IBuildSystemUtils::getNdkVersion)() ?: "Unknown"
  }
}
