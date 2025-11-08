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

import com.tom.rv2ide.projects.android.AndroidModule
import com.tom.rv2ide.projects.internal.ProjectManagerImpl
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Helper class for AI Agent to interact with project structure and files.
 *
 * @author Tom
 */
object ProjectHelper {

  private val log = LoggerFactory.getLogger(ProjectHelper::class.java)

  /**
   * Write a file to the correct location in the Android project structure This method handles ANY
   * Android project file type dynamically
   */
  fun writeFileToProject(
      relativePath: String,
      content: String,
      modulePath: String? = null,
  ): Boolean {
    return try {
      val targetFile = resolveProjectFilePath(relativePath, modulePath)
      if (targetFile == null) {
        log.error("Could not resolve file path: $relativePath")
        return false
      }

      // Ensure parent directories exist
      targetFile.parentFile?.mkdirs()

      // Write the content
      targetFile.writeText(content)

      log.info("Successfully wrote file: ${targetFile.absolutePath}")
      true
    } catch (e: Exception) {
      log.error("Failed to write file: $relativePath", e)
      false
    }
  }

  /**
   * Resolve a relative Android project path to an absolute File This method dynamically handles any
   * Android project structure
   */
  fun resolveProjectFilePath(relativePath: String, modulePath: String? = null): File? {
    val targetModule = getTargetModule(modulePath)
    if (targetModule == null) {
      log.error("Could not find target module: $modulePath")
      return null
    }

    // Normalize the path - remove leading "app/" if present since we'll use the actual module
    // structure
    val normalizedPath = relativePath.removePrefix("app/")

    return when {
      // Handle any res/ files (drawable, layout, values, menu, xml, anim, etc.)
      normalizedPath.startsWith("src/main/res/") -> {
        val resDir = getResourceDirectory(targetModule)
        if (resDir != null) {
          val resourcePath = normalizedPath.removePrefix("src/main/res/")
          File(resDir, resourcePath)
        } else {
          log.error("Could not find resource directory for module")
          null
        }
      }

      // Handle any Java/Kotlin source files
      normalizedPath.startsWith("src/main/java/") -> {
        val sourceDir = getSourceDirectory(targetModule)
        if (sourceDir != null) {
          val sourcePath = normalizedPath.removePrefix("src/main/java/")
          File(sourceDir, sourcePath)
        } else {
          log.error("Could not find source directory for module")
          null
        }
      }

      // Handle AndroidManifest.xml
      normalizedPath == "src/main/AndroidManifest.xml" -> {
        targetModule.mainSourceSet?.sourceProvider?.manifestFile
      }

      // Handle assets files
      normalizedPath.startsWith("src/main/assets/") -> {
        val assetsDir = getAssetsDirectory(targetModule)
        if (assetsDir != null) {
          val assetPath = normalizedPath.removePrefix("src/main/assets/")
          File(assetsDir, assetPath)
        } else {
          // Create assets directory if it doesn't exist
          val moduleRoot = targetModule.projectDir
          val assetsDir = File(moduleRoot, "src/main/assets")
          val assetPath = normalizedPath.removePrefix("src/main/assets/")
          File(assetsDir, assetPath)
        }
      }

      // Handle any other src/main/ files (like jni, aidl, etc.)
      normalizedPath.startsWith("src/main/") -> {
        val moduleRoot = targetModule.projectDir
        File(moduleRoot, normalizedPath)
      }

      // Handle build.gradle files and other root-level files
      normalizedPath == "build.gradle" || normalizedPath == "build.gradle.kts" -> {
        File(targetModule.projectDir, normalizedPath)
      }

      // Handle proguard files and other config files
      normalizedPath.endsWith(".pro") || normalizedPath.endsWith(".cfg") -> {
        File(targetModule.projectDir, normalizedPath)
      }

      // Generic fallback: relative to module root
      else -> {
        log.info("Using generic path resolution for: $relativePath")
        File(targetModule.projectDir, normalizedPath)
      }
    }
  }

  /** Get the main resource directory for a module */
  private fun getResourceDirectory(module: AndroidModule): File? {
    return module.mainSourceSet?.sourceProvider?.resDirectories?.firstOrNull()
  }

  /** Get the main source directory for a module */
  private fun getSourceDirectory(module: AndroidModule): File? {
    return module.mainSourceSet?.sourceProvider?.javaDirectories?.firstOrNull()
  }

  /** Get the assets directory for a module */
  private fun getAssetsDirectory(module: AndroidModule): File? {
    return module.mainSourceSet?.sourceProvider?.assetsDirectories?.firstOrNull()
  }

  /** Check if a file path represents a valid Android project file */
  fun isValidAndroidProjectPath(relativePath: String): Boolean {
    val normalizedPath = relativePath.removePrefix("app/")

    return when {
      normalizedPath.startsWith("src/main/res/") -> true
      normalizedPath.startsWith("src/main/java/") -> true
      normalizedPath.startsWith("src/main/assets/") -> true
      normalizedPath == "src/main/AndroidManifest.xml" -> true
      normalizedPath == "build.gradle" -> true
      normalizedPath == "build.gradle.kts" -> true
      normalizedPath.startsWith("src/main/") -> true // Any other src/main files
      else -> false
    }
  }

  /** Get the expected file type based on the path */
  fun getFileType(relativePath: String): String {
    val normalizedPath = relativePath.removePrefix("app/")

    return when {
      normalizedPath.contains("/res/drawable/") -> "Drawable Resource"
      normalizedPath.contains("/res/layout/") -> "Layout Resource"
      normalizedPath.contains("/res/values/") -> "Values Resource"
      normalizedPath.contains("/res/menu/") -> "Menu Resource"
      normalizedPath.contains("/res/xml/") -> "XML Resource"
      normalizedPath.contains("/res/anim/") -> "Animation Resource"
      normalizedPath.contains("/res/color/") -> "Color Resource"
      normalizedPath.contains("/res/mipmap/") -> "Mipmap Resource"
      normalizedPath.contains("/java/") && normalizedPath.endsWith(".kt") -> "Kotlin Source"
      normalizedPath.contains("/java/") && normalizedPath.endsWith(".java") -> "Java Source"
      normalizedPath.contains("/assets/") -> "Asset File"
      normalizedPath == "src/main/AndroidManifest.xml" -> "Android Manifest"
      normalizedPath.startsWith("build.gradle") -> "Gradle Build File"
      else -> "Project File"
    }
  }

  /** Find strings.xml file in the project */
  fun findStringsXml(modulePath: String? = null): File? {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return null

    val androidModules = workspace.getSubProjects().filterIsInstance<AndroidModule>()

    // If specific module is requested, find it
    val targetModule =
        if (modulePath != null) {
          androidModules.find { it.path == modulePath }
        } else {
          // Find main app module or first available module
          androidModules.find { it.path == ":app" } ?: androidModules.firstOrNull()
        }

    return targetModule?.let { module ->
      module.mainSourceSet?.sourceProvider?.resDirectories?.firstNotNullOfOrNull { resDir ->
        val valuesDir = File(resDir, "values")
        val stringsFile = File(valuesDir, "strings.xml")
        if (stringsFile.exists()) stringsFile else null
      }
    }
  }

  /** Find colors.xml file in the project */
  fun findColorsXml(modulePath: String? = null): File? {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return null

    val androidModules = workspace.getSubProjects().filterIsInstance<AndroidModule>()

    val targetModule =
        if (modulePath != null) {
          androidModules.find { it.path == modulePath }
        } else {
          androidModules.find { it.path == ":app" } ?: androidModules.firstOrNull()
        }

    return targetModule?.let { module ->
      module.mainSourceSet?.sourceProvider?.resDirectories?.firstNotNullOfOrNull { resDir ->
        val valuesDir = File(resDir, "values")
        val colorsFile = File(valuesDir, "colors.xml")
        if (colorsFile.exists()) colorsFile else null
      }
    }
  }

  /** Find any resource file by name */
  fun findResourceFile(fileName: String, modulePath: String? = null): File? {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return null

    val androidModules = workspace.getSubProjects().filterIsInstance<AndroidModule>()

    val targetModule =
        if (modulePath != null) {
          androidModules.find { it.path == modulePath }
        } else {
          androidModules.find { it.path == ":app" } ?: androidModules.firstOrNull()
        }

    return targetModule?.let { module ->
      module.mainSourceSet?.sourceProvider?.resDirectories?.firstNotNullOfOrNull { resDir ->
        findFileRecursively(resDir, fileName)
      }
    }
  }

  /** Find layout file by name */
  fun findLayoutFile(layoutName: String, modulePath: String? = null): File? {
    val fileName = if (layoutName.endsWith(".xml")) layoutName else "$layoutName.xml"
    return findResourceFile("layout/$fileName", modulePath)
  }

  /** Get all source directories for a module */
  fun getSourceDirectories(modulePath: String? = null): List<File> {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return emptyList()

    val androidModules = workspace.getSubProjects().filterIsInstance<AndroidModule>()

    val targetModule =
        if (modulePath != null) {
          androidModules.find { it.path == modulePath }
        } else {
          androidModules.find { it.path == ":app" } ?: androidModules.firstOrNull()
        }

    return targetModule?.mainSourceSet?.sourceProvider?.javaDirectories?.toList() ?: emptyList()
  }

  /** Get package name for a module */
  fun getPackageName(modulePath: String? = null): String? {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return null

    val androidModules = workspace.getSubProjects().filterIsInstance<AndroidModule>()

    val targetModule =
        if (modulePath != null) {
          androidModules.find { it.path == modulePath }
        } else {
          androidModules.find { it.path == ":app" } ?: androidModules.firstOrNull()
        }

    return try {
      targetModule?.getSelectedVariant()?.mainArtifact?.applicationId
          ?: targetModule?.variants?.firstOrNull()?.mainArtifact?.applicationId
    } catch (e: Exception) {
      log.error("Failed to get package name for module: $modulePath", e)
      null
    }
  }

  /** Create or update strings.xml with new string resource */
  fun addStringResource(name: String, value: String, modulePath: String? = null): Boolean {
    return try {
      val stringsFile = findStringsXml(modulePath)
      if (stringsFile == null) {
        log.error("strings.xml not found for module: $modulePath")
        return false
      }

      val content =
          if (stringsFile.exists()) {
            stringsFile.readText()
          } else {
            // Create basic strings.xml structure
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
</resources>"""
          }

      // Check if string already exists
      if (content.contains("name=\"$name\"")) {
        log.warn("String resource '$name' already exists")
        return true // Consider it successful if it already exists
      }

      // Add the new string resource
      val newStringEntry = "    <string name=\"$name\">$value</string>"
      val updatedContent =
          if (content.contains("</resources>")) {
            content.replace("</resources>", "$newStringEntry\n</resources>")
          } else {
            content + "\n$newStringEntry\n</resources>"
          }

      // Ensure parent directory exists
      stringsFile.parentFile?.mkdirs()

      // Write the updated content
      stringsFile.writeText(updatedContent)

      log.info("Successfully added string resource '$name' to ${stringsFile.absolutePath}")
      true
    } catch (e: Exception) {
      log.error("Failed to add string resource '$name'", e)
      false
    }
  }

  /** Create or update colors.xml with new color resource */
  fun addColorResource(name: String, value: String, modulePath: String? = null): Boolean {
    return try {
      var colorsFile = findColorsXml(modulePath)

      // If colors.xml doesn't exist, create it
      if (colorsFile == null) {
        val targetModule = getTargetModule(modulePath)
        if (targetModule != null) {
          val resDir = targetModule.mainSourceSet?.sourceProvider?.resDirectories?.firstOrNull()
          if (resDir != null) {
            val valuesDir = File(resDir, "values")
            valuesDir.mkdirs()
            colorsFile = File(valuesDir, "colors.xml")
          }
        }
      }

      if (colorsFile == null) {
        log.error("Cannot create colors.xml for module: $modulePath")
        return false
      }

      val content =
          if (colorsFile.exists()) {
            colorsFile.readText()
          } else {
            // Create basic colors.xml structure
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
</resources>"""
          }

      // Check if color already exists
      if (content.contains("name=\"$name\"")) {
        log.warn("Color resource '$name' already exists")
        return true
      }

      // Add the new color resource
      val newColorEntry = "    <color name=\"$name\">$value</color>"
      val updatedContent =
          if (content.contains("</resources>")) {
            content.replace("</resources>", "$newColorEntry\n</resources>")
          } else {
            content + "\n$newColorEntry\n</resources>"
          }

      // Write the updated content
      colorsFile.writeText(updatedContent)

      log.info("Successfully added color resource '$name' to ${colorsFile.absolutePath}")
      true
    } catch (e: Exception) {
      log.error("Failed to add color resource '$name'", e)
      false
    }
  }

  /** Get the AndroidManifest.xml file for a module */
  fun getManifestFile(modulePath: String? = null): File? {
    val targetModule = getTargetModule(modulePath)
    return targetModule?.mainSourceSet?.sourceProvider?.manifestFile
  }

  /** Add permission to AndroidManifest.xml */
  fun addPermission(permission: String, modulePath: String? = null): Boolean {
    return try {
      val manifestFile = getManifestFile(modulePath)
      if (manifestFile == null || !manifestFile.exists()) {
        log.error("AndroidManifest.xml not found for module: $modulePath")
        return false
      }

      val content = manifestFile.readText()

      // Check if permission already exists
      if (content.contains("android.permission.$permission") || content.contains(permission)) {
        log.warn("Permission '$permission' already exists")
        return true
      }

      // Add the permission
      val permissionEntry =
          "    <uses-permission android:name=\"android.permission.$permission\" />"

      // Find the right place to insert (after <manifest> tag but before <application>)
      val updatedContent =
          if (content.contains("<application")) {
            content.replace("<application", "$permissionEntry\n\n    <application")
          } else {
            // Fallback: add after manifest declaration
            val lines = content.lines().toMutableList()
            val manifestIndex = lines.indexOfFirst { it.contains("<manifest") }
            if (manifestIndex != -1 && manifestIndex + 1 < lines.size) {
              lines.add(manifestIndex + 1, permissionEntry)
              lines.joinToString("\n")
            } else {
              content
            }
          }

      manifestFile.writeText(updatedContent)
      log.info("Successfully added permission '$permission' to AndroidManifest.xml")
      true
    } catch (e: Exception) {
      log.error("Failed to add permission '$permission'", e)
      false
    }
  }

  private fun getTargetModule(modulePath: String?): AndroidModule? {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return null

    val androidModules = workspace.getSubProjects().filterIsInstance<AndroidModule>()

    return if (modulePath != null) {
      androidModules.find { it.path == modulePath }
    } else {
      androidModules.find { it.path == ":app" } ?: androidModules.firstOrNull()
    }
  }

  private fun findFileRecursively(directory: File, fileName: String): File? {
    if (!directory.exists() || !directory.isDirectory) return null

    // Check if the file exists directly in this directory
    val directFile = File(directory, fileName)
    if (directFile.exists()) return directFile

    // If fileName contains path separators, handle it specially
    if (fileName.contains("/")) {
      val targetFile = File(directory, fileName)
      if (targetFile.exists()) return targetFile
    }

    // Search in subdirectories
    directory.listFiles()?.forEach { child ->
      if (child.isDirectory) {
        val result = findFileRecursively(child, fileName)
        if (result != null) return result
      }
    }

    return null
  }

  /** Get all Android modules in the project */
  fun getAndroidModules(): List<AndroidModule> {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return emptyList()

    return workspace.getSubProjects().filterIsInstance<AndroidModule>()
  }

  /** Find the module that contains a specific file */
  fun findModuleForFile(file: File): AndroidModule? {
    val projectManager = ProjectManagerImpl.getInstance()
    val workspace = projectManager.getWorkspace() ?: return null

    val foundModule = workspace.findModuleForFile(file, false)
    return if (foundModule is AndroidModule) foundModule else null
  }

  /** Get project root directory */
  fun getProjectRoot(): File {
    return ProjectManagerImpl.getInstance().projectDir
  }
}
