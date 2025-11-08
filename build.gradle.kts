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

@file:Suppress("UnstableApiUsage")

import com.tom.rv2ide.build.config.BuildConfig
import com.tom.rv2ide.plugins.AndroidIDEPlugin
import com.tom.rv2ide.plugins.conf.configureAndroidModule
import com.tom.rv2ide.plugins.conf.configureJavaModule
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("build-logic.root-project")
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.android) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.protobuf) apply false
  alias(libs.plugins.benchmark) apply false
  id("org.jetbrains.kotlin.plugin.serialization") version "1.9.10" apply false
  
}

buildscript {
  dependencies {
    classpath(libs.kotlin.gradle.plugin)
    classpath(libs.nav.safe.args.gradle.plugin)
  }
}

project.group = BuildConfig.packageName

subprojects {
  if (project != rootProject) {
    var group = project.parent!!.group
    if (project.parent != rootProject) {
      group = "${group}.${project.parent!!.name}"
    }
    project.group = group
  }

  afterEvaluate {
    apply { plugin(AndroidIDEPlugin::class.java) }
  }

  project.version = rootProject.version

  plugins.withId("com.android.application") {
    configureAndroidModule(libs.androidx.libDesugaring)
  }
  plugins.withId("com.android.library") {
    configureAndroidModule(libs.androidx.libDesugaring)
  }
  plugins.withId("java-library") { configureJavaModule() }

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(BuildConfig.javaVersion.toString()))
    freeCompilerArgs.addAll("-Xstring-concat=inline")
  }
}
}

tasks.register<Delete>("clean") { delete(rootProject.layout.buildDirectory) }
