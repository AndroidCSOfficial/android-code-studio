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

package com.tom.rv2ide.templates.android

import android.content.Context
import com.tom.androidcodestudio.project.manager.builder.LanguageType
import com.tom.rv2ide.templates.AtcInterface
import com.tom.rv2ide.templates.preferences.Options
import java.io.File

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

interface Template {
  val displayName: String
  val templateType: TemplateType

  /**
   * Configure global options when this template is selected. Override in specific templates to set
   * custom options.
   */
  fun configureOptions() {
    // Default: reset ALL options to defaults
    Options.resetToDefaults()
  }

  suspend fun create(
      context: Context,
      listener: AtcInterface.TemplateCreationListener?,
      options: TemplateOptions,
  )

  enum class TemplateType {
    ACTIVITY,
    FRAGMENT,
    COMPOSE,
    OTHER,
  }
}

data class TemplateOptions(
    val projectName: String,
    val packageId: String,
    val languageType: LanguageType,
    val minSdk: Int,
    val useKts: Boolean,
    val saveLocation: File,
)
