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

import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.tom.customizablecardview.CustomizableCardView
import com.tom.rv2ide.R
import com.tom.rv2ide.activities.IdeConfigurations.*
import com.tom.rv2ide.activities.IdeConfigurations.net.isOnline
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity
import com.tom.rv2ide.databinding.ActivityMainCrashBinding
import com.tom.rv2ide.handlers.ConfigHandlerRegistry
import com.tom.rv2ide.managers.PreferenceManager
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.FlashType.SUCCESS
import com.tom.rv2ide.utils.GeneralFileUtils
import com.tom.rv2ide.utils.flashError
import com.tom.rv2ide.utils.flashInfo
import com.tom.rv2ide.utils.flashMessage
import com.tom.rv2ide.viewmodel.MainViewModel
import com.tom.rv2ide.viewmodel.MainViewModel.Companion.SCREEN_MAIN
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class IDEConfigurations : EdgeToEdgeIDEActivity() {

  private val viewModel by viewModels<MainViewModel>()
  private var _binding: ActivityMainCrashBinding? = null
  private var isDarkTheme: Boolean = false
  private var networkMonitorJob: Job? = null

  private val binding: ActivityMainCrashBinding
    get() = checkNotNull(_binding)

  private val prefManager: PreferenceManager
    get() = BaseApplication.getBaseInstance().prefManager

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    viewModel.setScreen(SCREEN_MAIN)
    isDarkTheme =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES

    // Register all handlers dynamically
    ConfigHandlerRegistry.registerHandlers(this, prefManager, lifecycleScope, isDarkTheme)

    // Initialize all handlers and setup their UI
    ConfigHandlerRegistry.getHandlers().forEach { handler ->
      handler.initialize()
      setupHandlerUI(handler)
    }

    setUpDropdown()
    checkAcsSystem()
    monitorNetworkConnection()
  }

  private fun setupHandlerUI(handler: com.tom.rv2ide.handlers.IConfigHandler) {
    try {
      val cardHolder = findViewById<View>(handler.cardHolderId) as? CustomizableCardView
      val removeButton = findViewById<View>(handler.removeButtonId) as? MaterialButton
      val reinstallButton = findViewById<View>(handler.reinstallButtonId) as? MaterialButton

      cardHolder?.let { handler.updateStatus() }

      removeButton?.setOnClickListener {
        handler.onRemoveClick()
        cardHolder?.let { handler.updateStatus() }
      }

      reinstallButton?.setOnClickListener {
        handler.onReinstallClick()
        cardHolder?.let { handler.updateStatus() }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  private fun checkAcsSystem() {
    val status = flashInfo("Checking acs system...")
    lifecycleScope.launch(Dispatchers.IO) {
      try {
        withContext(Dispatchers.Main) {
          if (AcsCommandInterface.isAcsAvailable()) {
            flashMessage("ACS build system is ready to use", SUCCESS)
          }
        }
      } catch (e: TimeoutCancellationException) {
        withContext(Dispatchers.Main) { flashError("Request timed out") }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          status?.dismiss()
          flashError("An error occurred: ${e.message}")
        }
        e.printStackTrace()
      }
    }
  }

  private fun monitorNetworkConnection() {
    networkMonitorJob?.cancel()

    networkMonitorJob =
        lifecycleScope.launch {
          repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.IO) {
              var errorShown = false

              while (isActive) {
                val isConnected = isOnline(this@IDEConfigurations)

                if (!isConnected && !errorShown) {
                  withContext(Dispatchers.Main) { flashError(getString(R.string.no_internet)) }
                  errorShown = true
                } else if (isConnected && errorShown) {
                  errorShown = false
                }

                delay(1000)
              }
            }
          }
        }
  }

  private fun setUpDropdown() {
    val dropdown = findViewById<AutoCompleteTextView>(R.id.ndks_dropdown)
    val cmake_dropdown = findViewById<AutoCompleteTextView>(R.id.cmake_dropdown)
    dropdown.setText("Loading versions...", false)
    cmake_dropdown.setText("Loading versions...", false)

    val cpuArch = getCpuArchitecture()
    val architecture: AcsCommandInterface.Architecture =
        when (cpuArch) {
          "armeabi-v7a" -> AcsCommandInterface.Architecture.ARM_V7A
          "arm64-v8a" -> AcsCommandInterface.Architecture.ARM64_V8A
          "x86_64" -> AcsCommandInterface.Architecture.X86_64
          else -> {
            flashError("Unsupported architecture: $cpuArch")
            return
          }
        }

    lifecycleScope.launch(Dispatchers.IO) {
      try {
        val versionsResult =
            AcsCommandInterface.listVersions(
                manifestUrl = AcsProvider.getManifestUrl,
                architecture = architecture,
                packageId = "android-native-kit",
            )

        val cmakeVersionsResult =
            AcsCommandInterface.listVersions(
                manifestUrl = AcsProvider.getManifestUrl,
                architecture = architecture,
                packageId = "android-cmake",
            )

        withContext(Dispatchers.Main) {
          // Process NDK versions
          if (versionsResult.success) {
            val versions =
                versionsResult.output
                    .trim()
                    .lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                    .filterNot { it.contains("Available versions:", ignoreCase = true) }
                    .filter { it.matches(Regex("""\d+\.\d+\.\d+""")) }
                    .sortedWith(
                        Comparator { v1, v2 ->
                          val parts1 = v1.split('.').map { it.toIntOrNull() ?: 0 }
                          val parts2 = v2.split('.').map { it.toIntOrNull() ?: 0 }

                          for (i in 0 until maxOf(parts1.size, parts2.size)) {
                            val part1 = parts1.getOrElse(i) { 0 }
                            val part2 = parts2.getOrElse(i) { 0 }
                            if (part1 != part2) {
                              return@Comparator part1.compareTo(part2)
                            }
                          }
                          return@Comparator 0
                        }
                    )

            if (versions.isNotEmpty()) {
              val adapter = ArrayAdapter(this@IDEConfigurations, R.layout.dropdown_item, versions)
              dropdown.setAdapter(adapter)

              val installedVersions =
                  GeneralFileUtils.listDirsInDirectory(File(Environment.HOME, "android-sdk/ndk"))
                      .map { it.name }
                      .sorted()

              dropdown.setText(
                  if (installedVersions.isNotEmpty()) installedVersions.last() else versions.last(),
                  false,
              )

              dropdown.setOnItemClickListener { parent, view, position, id ->
                val selectedVersion = versions[position]
                ConfigHandlerRegistry.getHandlerByName("NDK")?.let { handler ->
                  findViewById<CustomizableCardView>(handler.cardHolderId)?.let {
                    handler.updateStatus()
                  }
                }
              }
            } else {
              dropdown.setText("No versions available", false)
              flashError("No NDK versions found in the repository")
            }
          } else {
            dropdown.setText("Failed to load", false)
            flashError("Failed to fetch versions: ${versionsResult.errorOutput}")
          }

          // Process CMake versions
          if (cmakeVersionsResult.success) {
            val versions =
                cmakeVersionsResult.output
                    .trim()
                    .lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim() }
                    .filterNot { it.contains("Available versions:", ignoreCase = true) }
                    .filter { it.matches(Regex("""\d+\.\d+\.\d+""")) }
                    .sortedWith(
                        Comparator { v1, v2 ->
                          val parts1 = v1.split('.').map { it.toIntOrNull() ?: 0 }
                          val parts2 = v2.split('.').map { it.toIntOrNull() ?: 0 }

                          for (i in 0 until maxOf(parts1.size, parts2.size)) {
                            val part1 = parts1.getOrElse(i) { 0 }
                            val part2 = parts2.getOrElse(i) { 0 }
                            if (part1 != part2) {
                              return@Comparator part1.compareTo(part2)
                            }
                          }
                          return@Comparator 0
                        }
                    )

            if (versions.isNotEmpty()) {
              val adapter = ArrayAdapter(this@IDEConfigurations, R.layout.dropdown_item, versions)
              cmake_dropdown.setAdapter(adapter)

              val installedVersions =
                  GeneralFileUtils.listDirsInDirectory(File(Environment.HOME, "android-sdk/cmake"))
                      .map { it.name }
                      .sorted()

              cmake_dropdown.setText(
                  if (installedVersions.isNotEmpty()) installedVersions.last() else versions.last(),
                  false,
              )

              cmake_dropdown.setOnItemClickListener { parent, view, position, id ->
                val selectedVersion = versions[position]
                ConfigHandlerRegistry.getHandlerByName("CMake")?.let { handler ->
                  findViewById<CustomizableCardView>(handler.cardHolderId)?.let {
                    handler.updateStatus()
                  }
                }
              }
            } else {
              cmake_dropdown.setText("No versions available", false)
              flashError("No CMake versions found in the repository")
            }
          } else {
            cmake_dropdown.setText("Failed to load", false)
            flashError("Failed to fetch versions: ${cmakeVersionsResult.errorOutput}")
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          dropdown.setText("Error loading", false)
          flashError("Error fetching versions: ${e.message}")
        }
        e.printStackTrace()
      }
    }
  }

  private fun changeNdkVersion(version: String) {
    prefManager.putString("bs_native_kit_version", version)
  }

  private fun changeCMakeVersion(version: String) {
    prefManager.putString("bs_cmake_version", version)
  }

  override fun bindLayout(): View {
    _binding = ActivityMainCrashBinding.inflate(layoutInflater)
    return binding.root
  }

  override fun onDestroy() {
    super.onDestroy()
    ConfigHandlerRegistry.getHandlers().forEach { it.cleanup() }
    networkMonitorJob?.cancel()
    networkMonitorJob = null
    _binding = null
  }

  // Deprecated
  // companion object {
  // init {
  // System.loadLibrary("tom_ideutils")
  // }
  // }
}
