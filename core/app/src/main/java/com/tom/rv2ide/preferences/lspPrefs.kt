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

import androidx.preference.Preference
import com.tom.rv2ide.lsp.kotlin.etc.LspFeatures
import com.tom.rv2ide.preferences.internal.LSPPreferences
import com.tom.rv2ide.preferences.internal.LSPPreferences.ACS_KOTLIN_LSP_CURSOR_HOVER
import com.tom.rv2ide.preferences.internal.LSPPreferences.ACS_KOTLIN_LSP_DIAGNOSTICS
import com.tom.rv2ide.preferences.internal.LSPPreferences.ACS_KOTLIN_LSP_FORMAT_STYLE
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string
import com.tom.rv2ide.utils.Environment
import java.io.File
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

@Parcelize
class LSPPreferencesScreen(
    override val key: String = "idepref_editor_lsp",
    override val title: Int = string.servers,
    override val children: List<IPreference> = mutableListOf(),
) : IPreferenceGroup() {

  init {
    addPreference(LSPOptions())
  }
}

@Parcelize
private class LSPOptions(
    override val key: String = "idepref_lsp_options",
    override val title: Int = string.lsp_options,
    override val summary: Int? = string.lsp_options_summary,
    override val children: List<IPreference> = mutableListOf(),
) : IPreferenceScreen() {

  init {
    addPreference(KotlinCategory())
  }
}

// New Kotlin Category
@Parcelize
private class KotlinCategory(
    override val key: String = "lsp_kotlin_category",
    override val title: Int = string.kotlin,
    override val summary: Int? = string.kotlin_lsp_category_summary,
    override val children: List<IPreference> = mutableListOf(),
) : IPreferenceScreen() {

  init {
    addPreference(KotlinLSP())
    addPreference(KotlinHover())
    addPreference(KotlinDiagnostics())
    addPreference(KotlinFormatStyle())
  }
}

@Parcelize
private class KotlinLSP(
    override val key: String = "lsp_kotlin_server",
    override val title: Int = string.lsp_options_kotlin_title,
    override val summary: Int? = string.lsp_options_kotlin_summary,
) :
    LSPPreference(
        hint = string.server_status,
        getValue = { getStatus(isKotlinServerInstalled()) },
        serverId = "Kotlin",
        isInstalled = ::isKotlinServerInstalled,
    )

@Parcelize
private class KotlinHover(
    override val key: String = ACS_KOTLIN_LSP_CURSOR_HOVER,
    override val title: Int = string.acs_lsp_hover_title,
    override val icon: Int? = drawable.ic_tooltip,
) :
    SwitchPreference(
        setValue = LSPPreferences::cursorHover::set,
        getValue = LSPPreferences::cursorHover::get,
    ) {

  @IgnoredOnParcel
  override val summary: Int?
    get() =
        if (isKotlinServerInstalled()) {
          string.acs_lsp_hover_summary
        } else {
          string.kotlin_server_required
        }

  override fun onCreatePreference(
      context: android.content.Context
  ): androidx.preference.Preference {
    val pref = super.onCreatePreference(context)
    pref.isEnabled = isKotlinServerInstalled()
    return pref
  }
}

@Parcelize
private class KotlinDiagnostics(
    override val key: String = ACS_KOTLIN_LSP_DIAGNOSTICS,
    override val title: Int = string.acs_lsp_diagnostics_title,
    override val icon: Int? = drawable.ic_diagnostics,
) :
    SwitchPreference(
        setValue = LSPPreferences::codeDiagnostics::set,
        getValue = LSPPreferences::codeDiagnostics::get,
    ) {

  @IgnoredOnParcel
  override val summary: Int?
    get() =
        if (isKotlinServerInstalled()) {
          string.acs_lsp_diagnostics_summary
        } else {
          string.kotlin_server_required
        }

  override fun onCreatePreference(
      context: android.content.Context
  ): androidx.preference.Preference {
    val pref = super.onCreatePreference(context)
    pref.isEnabled = isKotlinServerInstalled()
    return pref
  }
}

@Parcelize
private class KotlinFormatStyle(
    override val key: String = ACS_KOTLIN_LSP_FORMAT_STYLE,
    override val title: Int = string.acs_lsp_kotlin_code_style_title,
    override val icon: Int? = drawable.ic_format_code,
) : SingleChoicePreference() {

  @IgnoredOnParcel override val dialogCancellable = true

  @IgnoredOnParcel
  override val summary: Int?
    get() =
        if (isKotlinServerInstalled()) {
          string.acs_lsp_kotlin_code_style_summary
        } else {
          string.kotlin_server_required
        }

  companion object {
    private const val STYLE_KOTLINLANG = "kotlinlang"
    private const val STYLE_GOOGLE = "google"
    private const val STYLE_FACEBOOK = "facebook"
    private val STYLES = listOf(STYLE_GOOGLE, STYLE_KOTLINLANG, STYLE_FACEBOOK)
    private const val DEFAULT_STYLE = STYLE_GOOGLE
  }

  override fun getEntries(preference: Preference): Array<PreferenceChoices.Entry> {
    val currentStyle = LSPPreferences.codeFormatStyle
    return STYLES.mapIndexed { index, style ->
          PreferenceChoices.Entry(style, currentStyle == style, style)
        }
        .toTypedArray()
  }

  override fun onChoiceConfirmed(
      preference: Preference,
      entry: PreferenceChoices.Entry?,
      position: Int,
  ) {
    val selectedStyle = STYLES.getOrNull(position) ?: DEFAULT_STYLE
    LSPPreferences.codeFormatStyle = selectedStyle
    LspFeatures.setCodeFormatStyle(selectedStyle)
  }

  override fun onCreatePreference(
      context: android.content.Context
  ): androidx.preference.Preference {
    val pref = super.onCreatePreference(context)
    pref.isEnabled = isKotlinServerInstalled()
    return pref
  }
}

@Parcelize
private class CCPPCategory(
    override val key: String = "lsp_ccpp_category",
    override val title: Int = string.c_cpp,
    override val summary: Int? = string.ccpp_lsp_category_summary,
    override val children: List<IPreference> = mutableListOf(),
) : IPreferenceScreen() {

  init {
    addPreference(CCPPLSP())
  }
}

@Parcelize
private class CCPPLSP(
    override val key: String = "lsp_c_cpp_server",
    override val title: Int = string.lsp_options_c_cpp_title,
    override val summary: Int? = string.lsp_options_c_cpp_summary,
) :
    LSPPreference(
        hint = string.server_status,
        getValue = { getStatus(isCCppServerInstalled()) },
        serverId = "C_CPP",
        isInstalled = ::isCCppServerInstalled,
    )

private fun getStatus(installed: Boolean): String = if (installed) "installed" else "not installed"

private fun isKotlinServerInstalled(): Boolean {
  val serverDir = File(Environment.HOME, "acs/servers/kotlin/server")
  return serverDir.exists() && serverDir.isDirectory && serverDir.listFiles()?.isNotEmpty() == true
}

private fun isCCppServerInstalled(): Boolean {
  val serverDir = File(Environment.HOME, "acs/servers/c_cpp")
  return serverDir.exists() && serverDir.isDirectory && serverDir.listFiles()?.isNotEmpty() == true
}
