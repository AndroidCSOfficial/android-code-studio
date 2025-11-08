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

package com.tom.rv2ide.utils

import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.managers.PreferenceManager

/**
 * Utility class for managing Kotlin Lsp features.
 *
 * This class provides methods to control and query the state of various LSP features such as hover
 * information and diagnostics through the application's preferences.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
object LspUtils {

  private val preferenceManager: PreferenceManager
    get() = BaseApplication.getBaseInstance().prefManager

  /**
   * Checks if hover functionality is enabled.
   *
   * @return Boolean indicating hover enabled status, or null if not set
   */
  fun isHoverEnabled(): Boolean? {
    return preferenceManager.getBoolean("acs_lsp_hover_enabled", false)
  }

  /**
   * Checks if diagnostics functionality is enabled.
   *
   * @return Boolean indicating diagnostics enabled status, or null if not set
   */
  fun isDiagnosticsEnabled(): Boolean? {
    return preferenceManager.getBoolean("acs_lsp_diagnostics_enabled", false)
  }

  /**
   * Enables or disables diagnostics functionality.
   *
   * @param enabled Boolean flag to set diagnostics enabled status
   */
  fun setDiagnosticsEnabled(enabled: Boolean) {
    preferenceManager.putBoolean("acs_lsp_diagnostics_enabled", enabled)
  }

  /**
   * Enables or disables hover functionality.
   *
   * @param enabled Boolean flag to set hover enabled status
   */
  fun setHoverEnabled(enabled: Boolean) {
    preferenceManager.putBoolean("acs_lsp_hover_enabled", enabled)
  }
}
