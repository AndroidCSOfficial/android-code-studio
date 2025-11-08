/*
 *  This file is part of AndroidTC.
 *
 *  AndroidTC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidTC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with AndroidTC.  If not, see <https://www.gnu.org/licenses/>.
 */

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

package com.tom.androidcodestudio.project.types

/**
 * Enum representing different types of Android projects that can be created. Each type defines the
 * project structure and configuration needed.
 */
enum class ProjectType(
    val displayName: String,
    val description: String,
    val language: ProjectLanguage,
    val requiresAndroidConfig: Boolean = true,
    val defaultMinSdk: Int = 21,
    val defaultTargetSdk: Int = 34,
    val plugins: List<String> = emptyList(),
    val features: List<String> = emptyList(),
) {
  /** Basic Android Activity project with a single main activity */
  ACTIVITY(
      displayName = "Basic Activity",
      description = "Standard Android project with a single main activity",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = true,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("com.android.application", "org.jetbrains.kotlin.android"),
      features = listOf("Single Activity", "Basic UI", "Material Design"),
  ),

  /** Java library project for creating reusable Java components */
  JAVA_LIBRARY(
      displayName = "Java Library",
      description = "Reusable Java library component",
      language = ProjectLanguage.JAVA,
      requiresAndroidConfig = false,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("java-library"),
      features = listOf("Java Library", "Reusable Components"),
  ),

  /** Kotlin library project for creating reusable Kotlin components */
  KOTLIN_LIBRARY(
      displayName = "Kotlin Library",
      description = "Reusable Kotlin library component",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = false,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("kotlin"),
      features = listOf("Kotlin Library", "Reusable Components"),
  ),

  /** JitPack compatible library for easy distribution */
  JITPACK_LIBRARY(
      displayName = "JitPack Library",
      description = "Library configured for JitPack distribution",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = false,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("kotlin"), // JitPack works with standard Kotlin plugin
      features = listOf("JitPack Ready", "Library Distribution", "Maven Publishing"),
  ),

  /** Jetpack Compose project with modern UI toolkit */
  COMPOSE(
      displayName = "Jetpack Compose",
      description = "Modern Android UI with Jetpack Compose",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = true,
      defaultMinSdk = 21, // Compose requires min API 21
      defaultTargetSdk = 34,
      plugins = listOf("com.android.application", "org.jetbrains.kotlin.android"),
      features = listOf("Jetpack Compose", "Modern UI", "Declarative Programming"),
  ),

  /** Game development project with GameActivity */
  GAME_ACTIVITY(
      displayName = "Game Activity",
      description = "Android project optimized for game development",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = true,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("com.android.application", "org.jetbrains.kotlin.android"),
      features = listOf("Game Development", "GameActivity", "Performance Optimized"),
  ),

  /** Empty project with minimal configuration */
  EMPTY_PROJECT(
      displayName = "Empty Project",
      description = "Minimal Android project with basic setup",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = true,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("com.android.application", "org.jetbrains.kotlin.android"),
      features = listOf("Minimal Setup", "Clean Slate"),
  ),

  /** Multi-module project with app and library modules */
  MULTI_MODULE(
      displayName = "Multi-Module Project",
      description = "Project with multiple modules for better architecture",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = true,
      defaultMinSdk = 21,
      defaultTargetSdk = 34,
      plugins = listOf("com.android.application", "org.jetbrains.kotlin.android"),
      features = listOf("Multi-module", "Clean Architecture", "Feature Modules"),
  ),

  /** Wear OS project for wearable devices */
  WEAR_OS(
      displayName = "Wear OS",
      description = "Project for Android wearable devices",
      language = ProjectLanguage.KOTLIN,
      requiresAndroidConfig = true,
      defaultMinSdk = 26, // Wear OS requires min API 26
      defaultTargetSdk = 34,
      plugins = listOf("com.android.application", "org.jetbrains.kotlin.android"),
      features = listOf("Wear OS", "Wearable Devices", "Round Screen Support"),
  );

  /** Checks if this project type is a library */
  fun isLibrary(): Boolean {
    return !requiresAndroidConfig
  }

  /** Gets the recommended package name suffix for this project type */
  fun getPackageSuffix(): String {
    return when (this) {
      JAVA_LIBRARY,
      KOTLIN_LIBRARY -> "library"
      JITPACK_LIBRARY -> "lib"
      COMPOSE -> "compose"
      GAME_ACTIVITY -> "game"
      WEAR_OS -> "wear"
      MULTI_MODULE -> "app"
      else -> "app"
    }
  }

  companion object {
    /** Gets all project types that support a specific language */
    fun getTypesByLanguage(language: ProjectLanguage): List<ProjectType> {
      return values().filter { it.language == language }
    }

    /** Finds a project type by its display name */
    fun fromDisplayName(displayName: String): ProjectType? {
      return values().find { it.displayName == displayName }
    }

    /** Gets all library project types */
    fun getLibraryTypes(): List<ProjectType> {
      return values().filter { it.isLibrary() }
    }

    /** Gets all application project types */
    fun getApplicationTypes(): List<ProjectType> {
      return values().filter { !it.isLibrary() }
    }
  }
}

/** Enum representing the programming language used in the project */
enum class ProjectLanguage {
  KOTLIN,
  JAVA,
}
