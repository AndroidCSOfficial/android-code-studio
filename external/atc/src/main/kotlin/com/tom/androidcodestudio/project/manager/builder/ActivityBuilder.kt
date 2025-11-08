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

/** Enum representing supported programming language types. */
enum class LanguageType(val extension: String, val dirName: String) {
  JAVA("java", "java"),
  KOTLIN("kt", "kotlin"),
}

/**
 * Data class representing an Activity configuration.
 *
 * @property moduleName The module name where the activity will be created (e.g., "app")
 * @property languageType The programming language type (Java or Kotlin)
 * @property packageId The package identifier (e.g., "com.example.myapp")
 * @property activityName The name of the activity class (e.g., "MainActivity")
 * @property content The activity source code content
 */
data class ActivityConfig(
    val moduleName: String,
    val languageType: LanguageType,
    val packageId: String,
    val activityName: String,
    val content: String,
)

/** Interface for writing Activity files. */
interface MainActivityBuilder {

  /**
   * Generates the activity file content.
   *
   * @param content The raw activity content
   * @return The content as a String
   */
  fun generate(content: String): String

  /**
   * Writes the activity file to the appropriate location.
   *
   * @param projectRoot The project root directory
   * @param config The activity configuration
   * @return The created File object
   */
  fun writeToFile(projectRoot: File, config: ActivityConfig): File

  /**
   * Creates a file anywhere with custom content and extension.
   *
   * @param directory The target directory
   * @param fileName The file name (without extension)
   * @param extension The file extension (e.g., "java", "kt", "xml", "json")
   * @param content The file content
   * @return The created File object
   */
  fun createFile(directory: File, fileName: String, extension: String, content: String): File
}

/** Default implementation of MainActivityBuilder for creating Activity files. */
class ActivityWriter : MainActivityBuilder {

  override fun generate(content: String): String {
    return content.trimIndent()
  }

  override fun writeToFile(projectRoot: File, config: ActivityConfig): File {
    // Build the path: moduleName/src/main/languageType/packageId
    val sourcePath =
        buildSourcePath(
            moduleName = config.moduleName,
            languageType = config.languageType,
            packageId = config.packageId,
        )

    // Create the full directory path
    val targetDir = File(projectRoot, sourcePath)
    if (!targetDir.exists()) {
      targetDir.mkdirs()
    }

    // Generate content
    val generatedContent = generate(config.content)

    // Create the activity file
    val fileName = "${config.activityName}.${config.languageType.extension}"
    val file = File(targetDir, fileName)
    file.writeText(generatedContent)

    return file
  }

  override fun createFile(
      directory: File,
      fileName: String,
      extension: String,
      content: String,
  ): File {
    // Ensure directory exists
    if (!directory.exists()) {
      directory.mkdirs()
    }

    // Generate content
    val generatedContent = generate(content)

    // Create the file with the specified extension
    val fullFileName =
        if (extension.isNotEmpty()) {
          "$fileName.$extension"
        } else {
          fileName
        }

    val file = File(directory, fullFileName)
    file.writeText(generatedContent)

    return file
  }

  /**
   * Builds the source path from module name, language type, and package ID.
   *
   * @param moduleName The module name (e.g., "app")
   * @param languageType The language type (Java or Kotlin)
   * @param packageId The package ID (e.g., "com.example.myapp")
   * @return The complete source path
   */
  private fun buildSourcePath(
      moduleName: String,
      languageType: LanguageType,
      packageId: String,
  ): String {
    val packagePath = packageIdToPath(packageId)
    return "$moduleName/src/main/${languageType.dirName}/$packagePath"
  }

  /**
   * Converts package ID to file system path.
   *
   * @param packageId The package ID (e.g., "com.example.myapp")
   * @return The path representation (e.g., "com/example/myapp")
   */
  private fun packageIdToPath(packageId: String): String {
    return packageId.replace('.', File.separatorChar)
  }
}

/** Builder class for creating ActivityConfig instances with a fluent API. */
class ActivityConfigBuilder {
  private var moduleName: String = ""
  private var languageType: LanguageType = LanguageType.KOTLIN
  private var packageId: String = ""
  private var activityName: String = ""
  private var content: String = ""

  fun moduleName(moduleName: String) = apply { this.moduleName = moduleName }

  fun languageType(languageType: LanguageType) = apply { this.languageType = languageType }

  fun packageId(packageId: String) = apply { this.packageId = packageId }

  fun activityName(activityName: String) = apply { this.activityName = activityName }

  fun content(content: String) = apply { this.content = content }

  fun build(): ActivityConfig {
    require(moduleName.isNotBlank()) { "Module name cannot be blank" }
    require(packageId.isNotBlank()) { "Package ID cannot be blank" }
    require(activityName.isNotBlank()) { "Activity name cannot be blank" }
    require(content.isNotBlank()) { "Activity content cannot be blank" }
    return ActivityConfig(moduleName, languageType, packageId, activityName, content)
  }
}

/** DSL function for creating ActivityConfig instances. */
fun activityConfig(block: ActivityConfigBuilder.() -> Unit): ActivityConfig {
  return ActivityConfigBuilder().apply(block).build()
}

/** Helper object with utility functions for package and path management. */
object PackageHelper {

  /**
   * Converts package ID to file system path.
   *
   * @param packageId The package ID (e.g., "com.example.myapp")
   * @return The path representation (e.g., "com/example/myapp")
   */
  fun packageIdToPath(packageId: String): String {
    return packageId.replace('.', File.separatorChar)
  }

  /**
   * Converts file system path to package ID.
   *
   * @param path The path (e.g., "com/example/myapp")
   * @return The package ID representation (e.g., "com.example.myapp")
   */
  fun pathToPackageId(path: String): String {
    return path.replace(File.separatorChar, '.')
  }

  /**
   * Validates if a package ID is correctly formatted.
   *
   * @param packageId The package ID to validate
   * @return True if valid, false otherwise
   */
  fun isValidPackageId(packageId: String): Boolean {
    if (packageId.isBlank()) return false

    val parts = packageId.split('.')
    if (parts.size < 2) return false

    return parts.all { part ->
      part.isNotEmpty() && part[0].isJavaIdentifierStart() && part.all { it.isJavaIdentifierPart() }
    }
  }

  /**
   * Builds the complete source path for a given configuration.
   *
   * @param moduleName The module name (e.g., "app")
   * @param languageType The language type (Java or Kotlin)
   * @param packageId The package ID (e.g., "com.example.myapp")
   * @return The complete source path
   */
  fun buildSourcePath(moduleName: String, languageType: LanguageType, packageId: String): String {
    val packagePath = packageIdToPath(packageId)
    return "$moduleName/src/main/${languageType.dirName}/$packagePath"
  }
}
