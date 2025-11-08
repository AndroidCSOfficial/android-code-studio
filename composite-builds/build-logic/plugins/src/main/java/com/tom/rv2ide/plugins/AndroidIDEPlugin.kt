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

package com.tom.rv2ide.plugins

import com.android.build.gradle.BaseExtension
import com.tom.rv2ide.plugins.util.isAndroidModule
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Manages plugins applied on the IDE's project modules.
 *
 * @author Akash Yadav
 */
class AndroidIDEPlugin : Plugin<Project> {

  override fun apply(target: Project) = target.run {
    if (project.path == rootProject.path) {
      throw GradleException("Cannot apply ${AndroidIDEPlugin::class.simpleName} to root project")
    }

    if (!project.buildFile.exists() || !project.buildFile.isFile) {
      return@run
    }

    if (isAndroidModule) {
      // setup signing configuration
      plugins.apply(SigningConfigPlugin::class.java)
    }
  }
}
