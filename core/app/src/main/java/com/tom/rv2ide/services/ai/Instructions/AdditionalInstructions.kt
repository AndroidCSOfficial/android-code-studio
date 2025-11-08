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

package com.tom.rv2ide.services.ai.Instructions

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

/**
 * System instructions with comprehensive file structure awareness and anti-confusion mechanisms.
 */

/** * TODO: Allow the user to add custom instructions and rules in the sidebar */
class SystemInstructions {
  companion object {
    private var cachedProjectContext: String? = null
    private var lastContextUpdate: Long = 0
    private const val CONTEXT_CACHE_DURATION = 5 * 60 * 1000L // 5 minutes

    private val baseInstructions =
        """
        You are an expert Android development AI assistant with CRITICAL FILE STRUCTURE AWARENESS.
        
        === ABSOLUTE CRITICAL RULES (NEVER VIOLATE) ===
        0. ❌ NEVER respond, modify, or take any action when the user says "hi" or uses any form of greeting (e.g., "hello", "hey", "good morning", etc.)
        1. NEVER create duplicate files with different extensions (e.g., NEVER create both build.gradle AND build.gradle.kts)
        2. NEVER mix file contents (e.g., NEVER put build.gradle content in MainActivity.kt)
        3. ALWAYS use EXACT file paths provided in the project structure
        4. ALWAYS match content type to file extension (.kt = Kotlin, .xml = XML, etc.)
        5. CHECK the existing project structure before creating or modifying files
        6. RESPECT the project's UI framework (Compose-only, Views-only, or Hybrid)
        
        === FILE TYPE ENFORCEMENT ===
        - .kt files: ONLY Kotlin code (classes, functions, properties)
        - .java files: ONLY Java code  
        - build.gradle: ONLY Groovy DSL build script syntax
        - build.gradle.kts: ONLY Kotlin DSL build script syntax
        - AndroidManifest.xml: ONLY Android manifest XML
        - layout/*.xml: ONLY Android XML layouts
        - values/*.xml: ONLY Android resource XML (strings, colors, styles)
        - menu/*.xml: ONLY Android menu XML
        
        === PROJECT STRUCTURE UNDERSTANDING ===
        You work with Android projects that have specific structures:
        - app/src/main/java/[package]/ - Source code (.kt/.java files)
        - app/src/main/res/ - Resources (layouts, strings, drawables, etc.)
        - app/src/main/AndroidManifest.xml - Application manifest
        - app/build.gradle(.kts) - App module build configuration
        - build.gradle(.kts) - Root project build configuration
        - settings.gradle(.kts) - Project settings
        
        === UI FRAMEWORK AWARENESS ===
        CRITICAL: Determine the project's UI framework and NEVER mix incompatible approaches:
        
        1. COMPOSE-ONLY projects:
           - Use @Composable functions
           - Use Modifier, Column, Row, Text, Button, etc.
           - NO XML layouts
           - Activities use setContent { }
           
        2. VIEW-BASED projects:
           - Use XML layouts in res/layout/
           - Use findViewById or View Binding/Data Binding
           - Activities use setContentView()
           - NO @Composable functions
           
        3. HYBRID projects:
           - Match the existing pattern in the current context
           - Don't mix unless explicitly requested
        
        === OUTPUT FORMAT RULES ===
        1. Single file modification:
           - Provide ONLY the clean code content
           - NO markdown code blocks (no ```)
           - NO explanatory text unless requested
           
        2. Multiple files:
           - Use EXACT format: ==== FILE: exact/relative/path/filename.ext ====
           - Follow with the complete file content
           - Use EXACT paths from the project structure
           
        3. File paths:
           - ALWAYS use forward slashes (/)
           - Use relative paths from project root
           - Match EXACTLY the existing file structure
           
        === ANTI-CONFUSION MECHANISMS ===
        1. Before creating ANY file, check if it already exists
        2. If build.gradle.kts exists, NEVER create build.gradle
        3. If build.gradle exists, NEVER create build.gradle.kts
        4. If MainActivity.kt exists, don't create MainActivity.java
        5. Always verify the file extension matches the content type
        6. Check the project's package structure for source files
        
        === CONTENT VALIDATION ===
        - Kotlin files must contain valid Kotlin syntax (package, imports, classes/functions)
        - Java files must contain valid Java syntax
        - XML files must be well-formed XML with proper Android attributes
        - Build files must contain valid Gradle DSL (Groovy or Kotlin)
        - Manifest files must follow Android manifest schema
        
        === ERROR PREVENTION ===
        1. If unsure about file location, ask for clarification
        2. If project structure is unclear, request the current structure
        3. Never assume file extensions - use what's provided
        4. Always maintain consistency with existing code style
        5. Preserve existing imports and package declarations
        
        === COMMON MISTAKE PREVENTION ===
        - DON'T put Gradle build script content in source files
        - DON'T put Activity code in manifest files
        - DON'T put XML layout content in Kotlin files
        - DON'T create files in wrong directories
        - DON'T mix Compose and View-based UI unless hybrid is confirmed
        
        === EXAMPLES OF CORRECT FILE PATHS ===
        - app/build.gradle.kts (if project uses Kotlin DSL)
        - app/src/main/java/com/example/app/MainActivity.kt
        - app/src/main/res/layout/activity_main.xml
        - app/src/main/res/values/strings.xml
        - app/src/main/AndroidManifest.xml
        
        === WHEN TO USE EACH FORMAT ===
        - Single file: When modifying or creating one file only
        - Multiple files: When creating/modifying 2+ files
        - Always specify complete file paths
        - Never use placeholder paths like "path/to/file"
        
        You have deep expertise in:
        - Android SDK and APIs (all versions)
        - Kotlin and Java programming
        - Jetpack Compose UI framework
        - Traditional Android Views and XML layouts
        - Gradle build system (both Groovy and Kotlin DSL)
        - Android project structure and conventions
        - Material Design principles
        - Android architecture patterns (MVVM, MVP, Clean Architecture)
        - Dependency injection (Dagger, Hilt, Koin)
        - Android Jetpack libraries
        - Testing frameworks (JUnit, Espresso, Compose Testing)
        
        REMEMBER: Your primary goal is to provide correct, working Android code that respects the project's structure and doesn't create confusion or duplicate files.
        """
            .trimIndent()

    fun getSystemInstructions(projectContext: String? = null): String {
      return if (projectContext != null) {
        buildString {
          append(baseInstructions)
          append("\n\n=== CURRENT PROJECT ANALYSIS ===\n")
          append(projectContext)
          append("\n\n=== CRITICAL REMINDERS FOR THIS PROJECT ===\n")

          // Add project-specific warnings based on context
          if (projectContext.contains("build.gradle.kts")) {
            append(
                "⚠️  This project uses KOTLIN DSL - only modify .kts files, never create .gradle files\n"
            )
          } else if (projectContext.contains("build.gradle")) {
            append(
                "⚠️  This project uses GROOVY DSL - only modify .gradle files, never create .kts files\n"
            )
          }

          if (projectContext.contains("COMPOSE_ONLY")) {
            append(
                "⚠️  This is a COMPOSE-ONLY project - NO XML layouts, use @Composable functions only\n"
            )
          } else if (projectContext.contains("VIEW_BASED_ONLY")) {
            append(
                "⚠️  This is a VIEW-BASED project - NO Compose, use XML layouts and traditional Views\n"
            )
          } else if (projectContext.contains("HYBRID")) {
            append(
                "⚠️  This is a HYBRID project - match existing patterns, don't mix unless requested\n"
            )
          }

          if (projectContext.contains("MainActivity.kt")) {
            append("⚠️  MainActivity is in Kotlin - maintain Kotlin for activities\n")
          }

          append("\nAlways refer to this project analysis before making any file changes!")
        }
      } else {
        baseInstructions
      }
    }

    fun updateProjectContext(context: String) {
      cachedProjectContext = context
      lastContextUpdate = System.currentTimeMillis()
    }

    fun getCachedProjectContext(): String? {
      return if (System.currentTimeMillis() - lastContextUpdate < CONTEXT_CACHE_DURATION) {
        cachedProjectContext
      } else {
        null
      }
    }

    fun clearCache() {
      cachedProjectContext = null
      lastContextUpdate = 0
    }

    /** Get project-specific validation rules */
    fun getValidationRules(projectContext: String?): List<String> {
      val rules = mutableListOf<String>()

      if (projectContext != null) {
        // Extract build system type
        when {
          projectContext.contains("Kotlin DSL") -> {
            rules.add("Use build.gradle.kts files only")
            rules.add("Use Kotlin DSL syntax in build files")
          }
          projectContext.contains("Groovy DSL") -> {
            rules.add("Use build.gradle files only")
            rules.add("Use Groovy DSL syntax in build files")
          }
        }

        // Extract UI framework rules
        when {
          projectContext.contains("Compose Only") -> {
            rules.add("Use @Composable functions for UI")
            rules.add("No XML layout files")
            rules.add("Use setContent in activities")
          }
          projectContext.contains("Traditional Views") -> {
            rules.add("Use XML layout files")
            rules.add("No @Composable functions")
            rules.add("Use setContentView in activities")
          }
          projectContext.contains("Hybrid") -> {
            rules.add("Match existing UI pattern in current context")
            rules.add("Don't mix UI frameworks without explicit request")
          }
        }

        // Extract language rules
        if (projectContext.contains("Kotlin")) {
          rules.add("Primary language is Kotlin")
          rules.add("Use Kotlin syntax and conventions")
        }

        if (projectContext.contains("Package:")) {
          val packageLine = projectContext.lines().find { it.startsWith("Package:") }
          packageLine?.let {
            val packageName = it.substringAfter("Package:").trim()
            rules.add("Use package name: $packageName")
          }
        }
      }

      return rules
    }

    /** Generate project-aware error prevention tips */
    fun getErrorPreventionTips(projectContext: String?): String {
      return buildString {
        append("=== ERROR PREVENTION CHECKLIST ===\n")

        if (projectContext != null) {
          append("✓ Check project uses ")
          when {
            projectContext.contains("Kotlin DSL") -> append("build.gradle.kts (not build.gradle)\n")
            projectContext.contains("Groovy DSL") -> append("build.gradle (not build.gradle.kts)\n")
            else -> append("correct build file extension\n")
          }

          append("✓ Verify UI framework is ")
          when {
            projectContext.contains("Compose Only") -> append("Jetpack Compose (no XML layouts)\n")
            projectContext.contains("Traditional Views") -> append("XML Views (no Compose)\n")
            projectContext.contains("Hybrid") -> append("Hybrid (match existing pattern)\n")
            else -> append("determined before creating UI code\n")
          }

          if (projectContext.contains("Package:")) {
            val packageLine = projectContext.lines().find { it.startsWith("Package:") }
            packageLine?.let {
              val packageName = it.substringAfter("Package:").trim()
              append("✓ Use correct package: $packageName\n")
            }
          }
        } else {
          append("✓ Request project structure before making changes\n")
          append("✓ Verify file extensions and content types match\n")
          append("✓ Check for existing files before creating new ones\n")
        }

        append("✓ Ensure file paths are correct and complete\n")
        append("✓ Match content type to file extension\n")
        append("✓ Preserve existing project patterns\n")
      }
    }
  }
}

// Backward compatibility
public var system_instructions: String
  get() = SystemInstructions.getSystemInstructions()
  set(value) {
    // Maintain backward compatibility but prefer using SystemInstructions directly
  }
