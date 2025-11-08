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


import com.tom.rv2ide.build.config.BuildConfig

plugins {
  id("com.android.library")
  id("kotlin-android")
  id("kotlin-kapt")
}

android {
  namespace = "${BuildConfig.packageName}.lsp.java"

  sourceSets {
    getByName("androidTest") {
      assets.srcDirs(rootProject.file("utilities/framework-stubs/libs"))
    }
  }
}

kapt {
  arguments {
    arg("eventBusIndex", "${BuildConfig.packageName}.events.LspJavaEventsIndex")
  }
}

dependencies {
  kapt(projects.annotation.processors)
  kapt(libs.google.auto.service)

  api(projects.core.indexingApi)

  // Include the Kotlin language server modules
  // implementation(projects.server.server)
  // implementation(projects.server.shared)

  implementation(libs.androidide.ts)
  implementation(libs.androidide.ts.java)
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.appcompat)
  implementation(libs.common.editor)
  implementation(libs.common.javaparser)
  implementation(libs.common.utilcode)

  implementation(libs.google.auto.service.annotations)
  implementation(libs.google.guava)
  implementation(libs.google.gson)
  implementation(libs.google.material)

  implementation(projects.core.actions)
  implementation(projects.core.common)
  implementation(projects.core.lspApi)
  implementation(projects.core.resources)
  implementation(projects.editor.api)
  implementation(projects.java.javacServices)

  implementation(libs.composite.javac)
  implementation(libs.composite.javapoet)
  implementation(libs.composite.jaxp)
  implementation(libs.composite.jdkJdeps)
  implementation(libs.composite.jdt)
  implementation(libs.composite.googleJavaFormat)

  implementation(libs.androidx.core.ktx)
  implementation(libs.common.kotlin)
  
  // Kotlin compiler for Kotlin LSP
  implementation(libs.kotlin.compiler.embeddable)
  implementation(libs.kotlin.scripting.compiler.embeddable)
  implementation(libs.asm)
  
  // LSP4J dependencies for kotlin-language-server integration
  implementation(libs.org.eclipse.lsp4j.lsp4j)
  implementation(libs.org.eclipse.lsp4j.jsonrpc)

}
