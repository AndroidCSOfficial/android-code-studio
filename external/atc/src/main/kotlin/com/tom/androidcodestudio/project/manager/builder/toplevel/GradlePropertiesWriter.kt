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

package com.tom.androidcodestudio.project.manager.builder.toplevel

import java.io.File

/**
 * Data class representing a single property entry.
 *
 * @property key The property key
 * @property value The property value
 * @property comment Optional comment for the property
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
data class GradleProperty(val key: String, val value: String, val comment: String? = null)

/** Interface for writing gradle.properties files. */
interface TLGradleProperties {

  /**
   * Generates the gradle.properties file content from a raw string.
   *
   * @param content The raw content to write
   * @return The content as a String
   */
  fun generate(content: String): String

  /**
   * Generates the gradle.properties file content from property entries.
   *
   * @param properties List of property entries
   * @param headerComment Optional header comment for the file
   * @return The generated content as a String
   */
  fun generate(properties: List<GradleProperty>, headerComment: String? = null): String

  /**
   * Writes raw content to gradle.properties file.
   *
   * @param outputDir The directory where the file will be created
   * @param content The raw content to write
   * @return The created File object
   */
  fun writeToFile(outputDir: File, content: String): File

  /**
   * Writes property entries to gradle.properties file.
   *
   * @param outputDir The directory where the file will be created
   * @param properties List of property entries
   * @param headerComment Optional header comment for the file
   * @return The created File object
   */
  fun writeToFile(
      outputDir: File,
      properties: List<GradleProperty>,
      headerComment: String? = null,
  ): File
}

/** Default implementation of TLGradleProperties for generating gradle.properties files. */
class GradlePropertiesWriter : TLGradleProperties {

  companion object {
    const val FILE_NAME = "gradle.properties"
  }

  override fun generate(content: String): String {
    return content.trimIndent()
  }

  override fun generate(properties: List<GradleProperty>, headerComment: String?): String {
    val builder = StringBuilder()

    // Add header comment if provided
    if (headerComment != null) {
      builder.appendLine("# $headerComment")
      builder.appendLine()
    }

    // Add properties
    properties.forEach { property ->
      // Add property comment if provided
      if (property.comment != null) {
        builder.appendLine("# ${property.comment}")
      }
      builder.appendLine("${property.key}=${property.value}")

      // Add blank line after property for readability
      if (property.comment != null) {
        builder.appendLine()
      }
    }

    return builder.toString()
  }

  override fun writeToFile(outputDir: File, content: String): File {
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val generatedContent = generate(content)
    val file = File(outputDir, FILE_NAME)
    file.writeText(generatedContent)

    return file
  }

  override fun writeToFile(
      outputDir: File,
      properties: List<GradleProperty>,
      headerComment: String?,
  ): File {
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val content = generate(properties, headerComment)
    val file = File(outputDir, FILE_NAME)
    file.writeText(content)

    return file
  }
}

/** Builder class for creating GradleProperty instances with a fluent API. */
class GradlePropertyBuilder {
  private var key: String = ""
  private var value: String = ""
  private var comment: String? = null

  fun key(key: String) = apply { this.key = key }

  fun value(value: String) = apply { this.value = value }

  fun comment(comment: String?) = apply { this.comment = comment }

  fun build(): GradleProperty {
    require(key.isNotBlank()) { "Property key cannot be blank" }
    require(value.isNotBlank()) { "Property value cannot be blank" }
    return GradleProperty(key, value, comment)
  }
}

/** DSL function for creating GradleProperty instances. */
fun gradleProperty(block: GradlePropertyBuilder.() -> Unit): GradleProperty {
  return GradlePropertyBuilder().apply(block).build()
}

/** Helper object with predefined common Android gradle properties. */
object GradlePropertiesPresets {

  /** Standard Android Gradle properties. */
  val STANDARD_ANDROID =
      """
          # Project-wide Gradle settings.
          # IDE (e.g. Android Studio) users:
          # Gradle settings configured through the IDE *will override*
          # any settings specified in this file.
          
          # For more details on how to configure your build environment visit
          # http://www.gradle.org/docs/current/userguide/build_environment.html
          
          # Specifies the JVM arguments used for the daemon process.
          # The setting is particularly useful for tweaking memory settings.
          org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
          
          # When configured, Gradle will run in incubating parallel mode.
          # This option should only be used with decoupled projects. More details, visit
          # http://www.gradle.org/docs/current/userguide/multi_project_builds.html#sec:decoupled_projects
          # org.gradle.parallel=true
          
          # AndroidX package structure to make it clearer which packages are bundled with the
          # Android operating system, and which are packaged with your app's APK
          # https://developer.android.com/topic/libraries/support-library/androidx-rn
          android.useAndroidX=true
          
          # Kotlin code style for this project: "official" or "obsolete":
          kotlin.code.style=official
          
          # Enables namespacing of each library's R class so that its R class includes only the
          # resources declared in the library itself and none from the library's dependencies,
          # thereby reducing the size of the R class for that library
          android.nonTransitiveRClass=true
      """
          .trimIndent()

  /** Minimal Android Gradle properties. */
  val MINIMAL_ANDROID =
      """
          org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
          android.useAndroidX=true
          android.nonTransitiveRClass=true
      """
          .trimIndent()

  /** Properties for non-transitive R classes (optimized builds). */
  val NON_TRANSITIVE_R_CLASS =
      """
          android.nonTransitiveRClass=true
          android.useAndroidX=true
      """
          .trimIndent()
}
