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

package com.tom.androidcodestudio.project.manager.builder

import java.io.File

/**
 * A builder class for creating Android project directory structures. Handles the creation of
 * standard Android project folders and package directories.
 */
class ProjectStructBuilder {

  /**
   * Builds the complete project structure for an Android module.
   *
   * @param moduleName The name of the module (e.g., "app", "library")
   * @param projectType The type of project (JAVA or KOTLIN)
   * @param packageId The package ID (e.g., "com.example.myapp")
   * @param baseDir The base directory where the project should be created
   * @param hasLayout Whether to create layout directories (true for apps, false for libraries)
   * @return Result indicating success or failure with message
   */
  fun buildProjectStructure(
      moduleName: String,
      projectType: ProjectType,
      packageId: String,
      baseDir: File,
      hasLayout: Boolean = true,
  ): BuildResult {
    return try {
      // Validate inputs
      if (moduleName.isBlank()) {
        return BuildResult(false, "Module name cannot be blank")
      }

      if (packageId.isBlank()) {
        return BuildResult(false, "Package ID cannot be blank")
      }

      if (!baseDir.exists() && !baseDir.mkdirs()) {
        return BuildResult(false, "Failed to create base directory: ${baseDir.absolutePath}")
      }

      // Create module directory
      val moduleDir = File(baseDir, moduleName)
      if (!createDirectory(moduleDir)) {
        return BuildResult(false, "Failed to create module directory: ${moduleDir.absolutePath}")
      }

      // Create main source directories
      val srcMainDir = File(moduleDir, "src/main")
      if (!createDirectory(srcMainDir)) {
        return BuildResult(false, "Failed to create src/main directory")
      }

      // Create language-specific directory (java or kotlin)
      val languageDirName =
          when (projectType) {
            ProjectType.JAVA -> "java"
            ProjectType.KOTLIN -> "kotlin"
          }
      val languageDir = File(srcMainDir, languageDirName)
      if (!createDirectory(languageDir)) {
        return BuildResult(false, "Failed to create $languageDirName directory")
      }

      // Create package directory structure
      val packageDirs = convertPackageToDirs(packageId)
      val packageDir = File(languageDir, packageDirs)
      if (!createDirectory(packageDir)) {
        return BuildResult(false, "Failed to create package directory: $packageDirs")
      }

      // Create resource directories
      val resDir = File(srcMainDir, "res")
      if (!createDirectory(resDir)) {
        return BuildResult(false, "Failed to create res directory")
      }

      // Create standard resource subdirectories
      val resourceDirs = getResourceDirectories(hasLayout)
      resourceDirs.forEach { resourceDir ->
        val dir = File(resDir, resourceDir)
        if (!createDirectory(dir)) {
          return BuildResult(false, "Failed to create resource directory: $resourceDir")
        }
      }

      // Create gradle directory (if it's the main app module)
      if (moduleName == "app") {
        val gradleDir = File(baseDir, "gradle")
        if (!createDirectory(gradleDir)) {
          return BuildResult(false, "Failed to create gradle directory")
        }

        // Create gradle wrapper directories
        val wrapperDir = File(gradleDir, "wrapper")
        if (!createDirectory(wrapperDir)) {
          return BuildResult(false, "Failed to create gradle/wrapper directory")
        }
      }

      BuildResult(true, "Project structure created successfully for module: $moduleName")
    } catch (e: Exception) {
      BuildResult(false, "Error creating project structure: ${e.message}")
    }
  }

  /**
   * Converts a package ID to directory path.
   *
   * @param packageId The package ID (e.g., "com.example.myapp")
   * @return Directory path (e.g., "com/example/myapp")
   */
  fun convertPackageToDirs(packageId: String): String {
    return packageId.replace('.', '/')
  }

  /**
   * Creates a directory and all necessary parent directories.
   *
   * @param directory The directory to create
   * @return true if successful, false otherwise
   */
  private fun createDirectory(directory: File): Boolean {
    return try {
      if (!directory.exists()) {
        directory.mkdirs()
      } else {
        true
      }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Gets the list of standard Android resource directories to create.
   *
   * @param hasLayout Whether to include layout directories
   * @return List of resource directory names
   */
  private fun getResourceDirectories(hasLayout: Boolean): List<String> {
    val directories =
        mutableListOf(
            "drawable",
            "drawable-v24",
            "mipmap-anydpi-v26",
            "mipmap-hdpi",
            "mipmap-mdpi",
            "mipmap-xhdpi",
            "mipmap-xxhdpi",
            "mipmap-xxxhdpi",
            "values",
            "values-night",
            "xml",
        )

    if (hasLayout) {
      directories.add("layout")
    }

    return directories
  }

  /**
   * Gets the complete path for the main source directory.
   *
   * @param moduleName The module name
   * @param projectType The project type
   * @param packageId The package ID
   * @return The full path to the main source directory
   */
  fun getMainSourcePath(moduleName: String, projectType: ProjectType, packageId: String): String {
    val languageDir =
        when (projectType) {
          ProjectType.JAVA -> "java"
          ProjectType.KOTLIN -> "kotlin"
        }
    val packagePath = convertPackageToDirs(packageId)
    return "$moduleName/src/main/$languageDir/$packagePath"
  }

  /**
   * Gets the complete path for the res directory.
   *
   * @param moduleName The module name
   * @return The full path to the res directory
   */
  fun getResPath(moduleName: String): String {
    return "$moduleName/src/main/res"
  }
}

/** Result class for build operations. */
data class BuildResult(val success: Boolean, val message: String)

/** Enum representing the project language type. */
enum class ProjectType {
  JAVA,
  KOTLIN,
}
