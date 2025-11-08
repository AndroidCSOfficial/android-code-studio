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

package com.tom.rv2ide.preferences.internal

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
@Suppress("MemberVisibilityCanBePrivate")
object LSPPreferences {

  const val ACS_KOTLIN_LSP_CURSOR_HOVER = "acs_kotlin_lsp_hover_enabled"
  const val ACS_KOTLIN_LSP_DIAGNOSTICS = "acs_kotlin_lsp_diagnostics_enabled"
  const val ACS_KOTLIN_LSP_FORMAT_STYLE = "acs_kotlin_lsp_format_style"

  var codeDiagnostics: Boolean
    get() = prefManager.getBoolean(ACS_KOTLIN_LSP_DIAGNOSTICS, true)
    set(value) {
      prefManager.putBoolean(ACS_KOTLIN_LSP_DIAGNOSTICS, value)
    }

  var cursorHover: Boolean
    get() = prefManager.getBoolean(ACS_KOTLIN_LSP_CURSOR_HOVER, true)
    set(value) {
      prefManager.putBoolean(ACS_KOTLIN_LSP_CURSOR_HOVER, value)
    }

  var codeFormatStyle: String
    get() = prefManager.getString(ACS_KOTLIN_LSP_FORMAT_STYLE, "google")
    set(value) {
      prefManager.putString(ACS_KOTLIN_LSP_FORMAT_STYLE, value)
    }
}
