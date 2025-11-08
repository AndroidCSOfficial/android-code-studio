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

package com.tom.rv2ide.templates.impl.withCpp

import com.tom.rv2ide.templates.ProjectTemplate
import com.tom.rv2ide.templates.base.AndroidModuleTemplateBuilder
import com.tom.rv2ide.templates.base.modules.android.defaultAppModule
import com.tom.rv2ide.templates.base.util.AndroidModuleJniManager
import com.tom.rv2ide.templates.base.util.AndroidModuleJniManager.JniFileType.CPPFILE
import com.tom.rv2ide.templates.base.util.AndroidModuleResManager.ResourceType.LAYOUT
import com.tom.rv2ide.templates.base.util.SourceWriter
import com.tom.rv2ide.templates.impl.R
import com.tom.rv2ide.templates.impl.base.createRecipe
import com.tom.rv2ide.templates.impl.base.emptyThemesAndColors
import com.tom.rv2ide.templates.impl.base.writeMainActivity
import com.tom.rv2ide.templates.impl.baseProjectImpl
import java.io.File

fun withCppProject(): ProjectTemplate = baseProjectImpl {
  templateName = R.string.template_cpp
  thumb = R.drawable.cpp_activity
  defaultAppModule {
    recipe = createRecipe {
      sources { writeEmptyActivity(this) }

      res { writeEmptyActivity() }
      writeEmptyJniActivity()
    }
  }
}

internal fun AndroidModuleTemplateBuilder.writeEmptyActivity() {
  res.apply {
    // layout/activity_main.xml
    writeXmlResource("activity_main", LAYOUT, source = ::emptyLayoutSrc)
    emptyThemesAndColors()
  }
}

internal fun AndroidModuleTemplateBuilder.writeEmptyJniActivity() {
  val jni = AndroidModuleJniManager()
  jni.apply {
    // Create C++ source file
    writeJniSource(
        "tomaslib",
        CPPFILE,
        source = { createBasicJniCppSource(data.packageName, "MainActivity", "sayHello") },
    )

    val androidMkFile = File(jniDir(), "Android.mk")
    executor.save(createAndroidMkFile("tomaslib"), androidMkFile)

    val appMkFile = File(jniDir(), "Application.mk")
    executor.save(createApplicationMkFile(), appMkFile)
  }
}

internal fun AndroidModuleTemplateBuilder.writeEmptyActivity(writer: SourceWriter) {
  writeMainActivity(writer, ::withCppSrcKt, ::withCppSrcJava)
}
