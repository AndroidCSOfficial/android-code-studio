/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.templates.impl.basicActivity

import com.tom.rv2ide.templates.ProjectTemplate
import com.tom.rv2ide.templates.base.AndroidModuleTemplateBuilder
import com.tom.rv2ide.templates.base.modules.android.defaultAppModule
import com.tom.rv2ide.templates.base.util.AndroidModuleResManager.ResourceType.LAYOUT
import com.tom.rv2ide.templates.base.util.SourceWriter
import com.tom.rv2ide.templates.impl.R
import com.tom.rv2ide.templates.impl.base.createRecipe
import com.tom.rv2ide.templates.impl.base.emptyThemesAndColors
import com.tom.rv2ide.templates.impl.base.writeMainActivity
import com.tom.rv2ide.templates.impl.baseProjectImpl

fun basicActivityProject(): ProjectTemplate {
  return baseProjectImpl {
    templateName = R.string.template_basic
    thumb = R.drawable.basic_activity
    defaultAppModule {
      recipe = createRecipe {
        sources { writeBasicActivitySrc(this) }

        res {
          writeBasicActivityLayout()
          emptyThemesAndColors()
        }
      }
    }
  }
}

private fun AndroidModuleTemplateBuilder.writeBasicActivitySrc(writer: SourceWriter) {
  writeMainActivity(writer = writer, ktSrc = ::basicActivitySrcKt, javaSrc = ::basicActivitySrcJava)
}

internal fun AndroidModuleTemplateBuilder.writeBasicActivityLayout() {
  res.apply {
    writeXmlResource("activity_main", LAYOUT, source = ::basicActivityLayout)
    writeXmlResource("content_main", LAYOUT, source = ::basicActivityContent)
  }
}
