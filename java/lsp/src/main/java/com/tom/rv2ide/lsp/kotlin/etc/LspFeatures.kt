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
 *  along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.lsp.kotlin.etc

import com.google.gson.JsonObject
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.lsp.kotlin.KotlinServerProcessManager
import com.tom.rv2ide.managers.PreferenceManager

/**
 * Utility class for managing Kotlin Lsp features.
 *
 * This class provides methods to control and query the state of various LSP features such as hover
 * information and diagnostics through the application's preferences.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
object LspFeatures {

  private const val KOTLIN_HOVER_KEY = "acs_kotlin_lsp_hover_enabled"
  private const val KOTLIN_DIAGNOSTICS_KEY = "acs_kotlin_lsp_diagnostics_enabled"
  private const val KOTLIN_CODE_FORMAT_STYLE_KEY = "acs_kotlin_code_format_style"

  private val preferenceManager: PreferenceManager
    get() = BaseApplication.getBaseInstance().prefManager

  private var processManager: KotlinServerProcessManager? = null

  fun setProcessManager(manager: KotlinServerProcessManager) {
    processManager = manager
  }

  /**
   * Checks if hover functionality is enabled.
   *
   * @return Boolean indicating hover enabled status, or null if not set
   */
  fun isHoverEnabled(): Boolean? {
    return preferenceManager.getBoolean(KOTLIN_HOVER_KEY, false)
  }

  /**
   * Checks if diagnostics functionality is enabled.
   *
   * @return Boolean indicating diagnostics enabled status, or null if not set
   */
  fun isDiagnosticsEnabled(): Boolean? {
    return preferenceManager.getBoolean(KOTLIN_DIAGNOSTICS_KEY, false)
  }

  /** @return String of of the style */
  fun getCodeFormatStyle(): String? {
    return preferenceManager.getString(KOTLIN_CODE_FORMAT_STYLE_KEY, "google" /* default */)
  }

  /**
   * Enables or disables diagnostics functionality.
   *
   * @param enabled Boolean flag to set diagnostics enabled status
   */
  fun setDiagnosticsEnabled(enabled: Boolean) {
    preferenceManager.putBoolean(KOTLIN_DIAGNOSTICS_KEY, enabled)
  }

  /**
   * Enables or disables hover functionality.
   *
   * @param enabled Boolean flag to set hover enabled status
   */
  fun setHoverEnabled(enabled: Boolean) {
    preferenceManager.putBoolean(KOTLIN_HOVER_KEY, enabled)
  }

  /** @param style String to set code format style */
  fun setCodeFormatStyle(style: String) {
    preferenceManager.putString(KOTLIN_CODE_FORMAT_STYLE_KEY, style)

    // Notify server of the change
    processManager?.let { manager ->
      val configParams =
          JsonObject().apply {
            add(
                "settings",
                JsonObject().apply {
                  add(
                      "kotlin",
                      JsonObject().apply {
                        add(
                            "formatting",
                            JsonObject().apply {
                              addProperty("formatter", "ktfmt")
                              add(
                                  "ktfmt",
                                  JsonObject().apply {
                                    addProperty("style", style)
                                    addProperty("indent", 4)
                                    addProperty("maxWidth", 100)
                                    addProperty("removeUnusedImports", true)
                                  },
                              )
                            },
                        )
                      },
                  )
                },
            )
          }

      manager.sendNotification("workspace/didChangeConfiguration", configParams)
    }
  }
}
