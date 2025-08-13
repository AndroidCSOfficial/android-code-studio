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

import java.util.Properties

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
  includeBuild("composite-builds/build-logic") {
    name = "build-logic"
  }

  repositories {
    maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    gradlePluginPortal()
    google()
    mavenCentral()
  }
}

dependencyResolutionManagement {
  val dependencySubstitutions = mapOf(
    "build-deps" to arrayOf(
      "appintro",
      "fuzzysearch",
      "google-java-format",
      "java-compiler",
      "javac",
      "javapoet",
      "jaxp",
      "jdk-compiler",
      "jdk-jdeps",
      "jdt",
      "layoutlib-api",
      "logback-core"
    ),

    "build-deps-common" to arrayOf(
      "desugaring-core"
    )
  )

  for ((build, modules) in dependencySubstitutions) {
    includeBuild("composite-builds/${build}") {
      this.name = build
      dependencySubstitution {
        for (module in modules) {
          substitute(module("com.itsaky.androidide.build:${module}"))
            .using(project(":${module}"))
        }
      }
    }
  }

  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}

// = App version name
gradle.rootProject {
  val sourceVersion = "2.7.1-beta"
  val revVersion = "1.0"
  var versionName = "$sourceVersion-rev-$revVersion"
  
  val versionFile = rootProject.file("version.properties")
  if (versionFile.exists()) {
    val props = Properties().apply {
      versionFile.inputStream().use { load(it) }
    }
  
    val revVersionProp = props.getProperty("version.name.rev")
    if (!revVersion.isEmpty()) {
      versionName = "$sourceVersion-rev-$revVersionProp"
    }
  }
  
  project.setProperty("version", versionName)
}

rootProject.name = "AndroidIDE"

// keep this sorted alphabetically
include(
  ":annotation:annotations",
  ":annotation:processors",
  ":annotation:processors-ksp",
  ":core:actions",
  ":core:app",
  ":core:common",
  ":core:indexing-api",
  ":core:indexing-core",
  ":core:lsp-api",
  ":core:lsp-models",
  ":core:projects",
  ":core:resources",
  ":editor:api",
  ":editor:impl",
  ":editor:lexers",
  ":editor:treesitter",
  ":event:eventbus",
  ":event:eventbus-android",
  ":event:eventbus-events",
  ":java:javac-services",
  ":java:lsp",
  ":logging:idestats",
  ":logging:logger",
  ":logging:logsender",
  ":termux:application",
  ":termux:emulator",
  ":termux:shared",
  ":termux:view",
  ":tooling:api",
  ":tooling:builder-model-impl",
  ":tooling:events",
  ":tooling:impl",
  ":tooling:model",
  ":tooling:plugin",
  ":tooling:plugin-config",
  ":utilities:build-info",
  ":utilities:flashbar",
  ":utilities:framework-stubs",
  ":utilities:lookup",
  ":utilities:preferences",
  ":utilities:shared",
  ":utilities:templates-api",
  ":utilities:templates-impl",
  ":utilities:treeview",
  ":utilities:uidesigner",
  ":utilities:xml-inflater",
  ":xml:aaptcompiler",
  ":xml:dom",
  ":xml:lsp",
  ":xml:resources-api",
  ":xml:utils",
)