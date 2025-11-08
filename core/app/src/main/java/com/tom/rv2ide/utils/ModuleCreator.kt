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

package com.tom.rv2ide.utils

import java.io.File
import java.io.IOException

/**
 * Utility class for creating new sub-modules in Android projects. Handles module structure
 * creation, build.gradle generation, and settings.gradle.kts updates.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class ModuleCreator {

  data class CreationResult(val success: Boolean, val errorMessage: String? = null)

  data class AppModuleConfig(val compileSdk: Int, val minSdk: Int)

  /**
   * Creates a new sub-module with the specified configuration.
   *
   * @param moduleName The name of the module to create
   * @param language The programming language (Kotlin or Java)
   * @param projectRoot The root directory of the project
   * @return CreationResult indicating success or failure
   */
  fun createModule(
      moduleName: String,
      language: com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage,
      projectRoot: File,
  ): CreationResult {
    return try {
      if (moduleName.isBlank()) {
        return CreationResult(false, "Module name cannot be empty")
      }

      if (!projectRoot.exists() || !projectRoot.isDirectory) {
        return CreationResult(false, "Project root directory does not exist")
      }

      val moduleDir = File(projectRoot, moduleName)
      if (moduleDir.exists()) {
        return CreationResult(false, "Module '$moduleName' already exists")
      }

      val useKotlinDsl = detectBuildScriptDsl(projectRoot)
      val basePackageName = detectBasePackageName(projectRoot)
      val appConfig = detectAppModuleConfig(projectRoot)

      createModuleStructure(
          moduleDir,
          moduleName,
          language,
          useKotlinDsl,
          basePackageName,
          appConfig,
      )

      updateSettingsGradle(projectRoot, moduleName)
      addDependencyToAppModule(projectRoot, moduleName, useKotlinDsl)

      CreationResult(true)
    } catch (e: Exception) {
      CreationResult(false, e.message ?: "Unknown error occurred")
    }
  }

  private fun detectBuildScriptDsl(projectRoot: File): Boolean {
    val appBuildFileKts = File(projectRoot, "app/build.gradle.kts")
    val appBuildFileGroovy = File(projectRoot, "app/build.gradle")
    if (appBuildFileKts.exists()) {
      return true
    }
    if (appBuildFileGroovy.exists()) {
      return false
    }
    return true
  }

  private fun detectBasePackageName(projectRoot: File): String {
    val appBuildFileKts = File(projectRoot, "app/build.gradle.kts")
    val appBuildFileGroovy = File(projectRoot, "app/build.gradle")

    val buildFile = if (appBuildFileKts.exists()) appBuildFileKts else appBuildFileGroovy

    if (buildFile.exists()) {
      val content = buildFile.readText()
      val namespacePattern = Regex("namespace\\s*[=:]\\s*[\"']([^\"']+)[\"']")
      val applicationIdPattern = Regex("applicationId\\s*[=:]\\s*[\"']([^\"']+)[\"']")

      val namespaceMatch = namespacePattern.find(content)
      if (namespaceMatch != null) {
        return namespaceMatch.groupValues[1]
      }

      val applicationIdMatch = applicationIdPattern.find(content)
      if (applicationIdMatch != null) {
        return applicationIdMatch.groupValues[1]
      }
    }
    return "com.example"
  }

  private fun detectAppModuleConfig(projectRoot: File): AppModuleConfig {
    val appBuildFileKts = File(projectRoot, "app/build.gradle.kts")
    val appBuildFileGroovy = File(projectRoot, "app/build.gradle")

    val buildFile = if (appBuildFileKts.exists()) appBuildFileKts else appBuildFileGroovy

    var compileSdk = 34 // Default fallback
    var minSdk = 21 // Default fallback

    if (buildFile.exists()) {
      val content = buildFile.readText()
      val compileSdkPattern = Regex("compileSdk\\s*[=:]\\s*(\\d+)")
      val compileSdkMatch = compileSdkPattern.find(content)
      if (compileSdkMatch != null) {
        compileSdk = compileSdkMatch.groupValues[1].toIntOrNull() ?: 34
      }
      val minSdkPattern = Regex("minSdk\\s*[=:]\\s*(\\d+)")
      val minSdkMatch = minSdkPattern.find(content)
      if (minSdkMatch != null) {
        minSdk = minSdkMatch.groupValues[1].toIntOrNull() ?: 21
      }
    }

    return AppModuleConfig(compileSdk, minSdk)
  }

  private fun createModuleStructure(
      moduleDir: File,
      moduleName: String,
      language: com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage,
      useKotlinDsl: Boolean,
      basePackageName: String,
      appConfig: AppModuleConfig,
  ) {
    val srcMainDir = File(moduleDir, "src/main")
    val javaDir =
        File(
            srcMainDir,
            if (
                language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN
            )
                "kotlin"
            else "java",
        )
    val resourcesDir = File(srcMainDir, "resources")

    moduleDir.mkdirs()
    srcMainDir.mkdirs()
    javaDir.mkdirs()
    resourcesDir.mkdirs()

    createBuildGradle(moduleDir, moduleName, language, useKotlinDsl, basePackageName, appConfig)
    createProguardRules(moduleDir)
    createConsumerRules(moduleDir)
    createSampleSourceFile(javaDir, moduleName, language, basePackageName)
  }

  private fun createBuildGradle(
      moduleDir: File,
      moduleName: String,
      language: com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage,
      useKotlinDsl: Boolean,
      basePackageName: String,
      appConfig: AppModuleConfig,
  ) {
    val buildFile = File(moduleDir, if (useKotlinDsl) "build.gradle.kts" else "build.gradle")

    val content =
        if (useKotlinDsl) {
          generateKotlinDslBuildScript(moduleName, language, basePackageName, appConfig)
        } else {
          generateGroovyBuildScript(moduleName, language, basePackageName, appConfig)
        }

    buildFile.writeText(content)
  }

  private fun generateKotlinDslBuildScript(
      moduleName: String,
      language: com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage,
      basePackageName: String,
      appConfig: AppModuleConfig,
  ): String {
    val kotlinPlugin =
        if (language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN) {
          "id(\"kotlin-android\")"
        } else {
          "// Java module - no additional plugin needed"
        }

    val kotlinOptions =
        if (language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN) {
          """
  kotlinOptions {
    jvmTarget = "1.8"
  }"""
        } else {
          ""
        }

    return """
plugins {
  id("com.android.library")
  $kotlinPlugin
}

android {
  namespace = "$basePackageName.$moduleName"
  compileSdk = ${appConfig.compileSdk}

  defaultConfig {
    minSdk = ${appConfig.minSdk}
  }
  
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }
$kotlinOptions
}

dependencies {
  // Core Android dependencies
  implementation("androidx.annotation:annotation:1.7.0")
}
"""
        .trimIndent()
  }

  private fun generateGroovyBuildScript(
      moduleName: String,
      language: com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage,
      basePackageName: String,
      appConfig: AppModuleConfig,
  ): String {
    val kotlinPlugin =
        if (language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN) {
          "id 'kotlin-android'"
        } else {
          "// Java module - no additional plugin needed"
        }

    val kotlinOptions =
        if (language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN) {
          """
  kotlinOptions {
    jvmTarget = '1.8'
  }"""
        } else {
          ""
        }

    return """
plugins {
  id 'com.android.library'
  $kotlinPlugin
}

android {
  namespace '$basePackageName.$moduleName'
  compileSdk ${appConfig.compileSdk}

  defaultConfig {
    minSdk ${appConfig.minSdk}
  }

  buildTypes {
    release {
      minifyEnabled false
      proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
  }

  compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
  }
$kotlinOptions
}

dependencies {
  // Core Android dependencies
  implementation 'androidx.annotation:annotation:1.7.0'
}
"""
        .trimIndent()
  }

  private fun createProguardRules(moduleDir: File) {
    val proguardFile = File(moduleDir, "proguard-rules.pro")
    proguardFile.writeText(
        """
        # Add project specific ProGuard rules here.
        # You can control the set of applied configuration files using the
        # proguardFiles setting in build.gradle.
        #
        # For more details, see
        #   http://developer.android.com/guide/developing/tools/proguard.html

        # If your project uses WebView with JS, uncomment the following
        # and specify the fully qualified class name to the JavaScript interface
        # class:
        #-keepclassmembers class fqcn.of.javascript.interface.for.webview {
        #   public *;
        #}

        # Uncomment this to preserve the line number information for
        # debugging stack traces.
        #-keepattributes SourceFile,LineNumberTable

        # If you keep the line number information, uncomment this to
        # hide the original source file name.
        #-renamesourcefileattribute SourceFile
        """
            .trimIndent()
    )
  }

  private fun createConsumerRules(moduleDir: File) {
    val consumerRulesFile = File(moduleDir, "consumer-rules.pro")
    consumerRulesFile.writeText(
        """
        # Consumer ProGuard rules for this module
        # These rules will be applied to consumers of this library
        """
            .trimIndent()
    )
  }

  private fun createSampleSourceFile(
      sourceDir: File,
      moduleName: String,
      language: com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage,
      basePackageName: String,
  ) {
    val packageDir = File(sourceDir, basePackageName.replace(".", "/") + "/$moduleName")
    packageDir.mkdirs()

    val fileName =
        if (language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN) {
          "SampleClass.kt"
        } else {
          "SampleClass.java"
        }

    val sampleFile = File(packageDir, fileName)

    val content =
        if (language == com.tom.rv2ide.fragments.sidebar.SubModuleFragment.ModuleLanguage.KOTLIN) {
          """
package $basePackageName.$moduleName

/**
 * Sample class for the $moduleName module.
 */
class SampleClass {
    
    /**
     * Sample method that returns a greeting message.
     */
    fun getGreeting(): String {
        return "Hello from $moduleName module!"
    }
}
"""
              .trimIndent()
        } else {
          """
package $basePackageName.$moduleName;

/**
 * Sample class for the $moduleName module.
 */
public class SampleClass {
    
    /**
     * Sample method that returns a greeting message.
     */
    public String getGreeting() {
        return "Hello from $moduleName module!";
    }
}
"""
              .trimIndent()
        }

    sampleFile.writeText(content)
  }

  private fun updateSettingsGradle(projectRoot: File, moduleName: String) {
    val settingsFile = File(projectRoot, "settings.gradle.kts")
    if (!settingsFile.exists()) {
      throw IOException("settings.gradle.kts not found in project root")
    }

    val content = settingsFile.readText()

    // Check if module is already included
    if (content.contains(":$moduleName")) {
      return // Module already included
    }

    val includePattern = Regex("include\\s*\\(\\s*([^)]*)\\s*\\)")
    val match = includePattern.find(content)

    if (match != null) {
      val existingModules = match.groupValues[1].trim()
      val newModuleEntry = ":$moduleName"

      if (existingModules.isNotEmpty()) {
        val newContent =
            content.replace(
                match.groupValues[0],
                "include(\n  $existingModules,\n  \"$newModuleEntry\"\n)",
            )
        settingsFile.writeText(newContent)
      } else {
        val newContent = content.replace(match.groupValues[0], "include(\"$newModuleEntry\")")
        settingsFile.writeText(newContent)
      }
    } else {
      val newContent = content + "\n\ninclude(\":$moduleName\")\n"
      settingsFile.writeText(newContent)
    }
  }

  private fun addDependencyToAppModule(
      projectRoot: File,
      moduleName: String,
      useKotlinDsl: Boolean,
  ) {
    val appBuildFile =
        File(projectRoot, if (useKotlinDsl) "app/build.gradle.kts" else "app/build.gradle")
    if (!appBuildFile.exists()) {
      return // App module doesn't exist, skip
    }

    val content = appBuildFile.readText()

    // Check if dependency is already added
    if (
        content.contains("project(\":$moduleName\")") || content.contains("project(':$moduleName')")
    ) {
      return // Dependency already exists
    }

    // Find the dependencies block and add the new module dependency
    val dependenciesPattern = Regex("dependencies\\s*\\{")
    val match = dependenciesPattern.find(content)

    if (match != null) {
      val insertPosition = match.range.last + 1
      val dependencyLine =
          if (useKotlinDsl) {
            "\n    implementation(project(\":$moduleName\"))\n"
          } else {
            "\n    implementation project(':$moduleName')\n"
          }

      val newContent =
          content.substring(0, insertPosition) + dependencyLine + content.substring(insertPosition)

      appBuildFile.writeText(newContent)
    }
  }
}
