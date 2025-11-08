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

package com.tom.rv2ide.services.ai.Instructions.io

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

/** File validation helper class */
class FileValidation {
  companion object {
    /** Validate that a file name matches expected content type */
    fun validateFileType(fileName: String, content: String): ValidationResult {
      return when {
        fileName.endsWith(".kt") -> validateKotlinFile(content)
        fileName.endsWith(".java") -> validateJavaFile(content)
        fileName == "build.gradle" -> validateGroovyBuildFile(content)
        fileName == "build.gradle.kts" -> validateKotlinBuildFile(content)
        fileName.endsWith(".xml") -> validateXmlFile(fileName, content)
        else -> ValidationResult.valid()
      }
    }

    private fun validateKotlinFile(content: String): ValidationResult {
      if (
          !content.contains(Regex("(class|fun|val|var|object|interface)\\s")) &&
              !content.contains("package ")
      ) {
        return ValidationResult.error("Content doesn't appear to be valid Kotlin code")
      }
      return ValidationResult.valid()
    }

    private fun validateJavaFile(content: String): ValidationResult {
      if (
          !content.contains(Regex("(class|public|private|protected)\\s")) &&
              !content.contains("package ")
      ) {
        return ValidationResult.error("Content doesn't appear to be valid Java code")
      }
      return ValidationResult.valid()
    }

    private fun validateGroovyBuildFile(content: String): ValidationResult {
      if (
          !content.contains("android") &&
              !content.contains("dependencies") &&
              !content.contains("apply plugin")
      ) {
        return ValidationResult.error("Content doesn't appear to be a Gradle build script")
      }
      if (content.contains("plugins {") || content.contains("implementation(")) {
        return ValidationResult.error(
            "Content appears to be Kotlin DSL but file is .gradle (should be .gradle.kts)"
        )
      }
      return ValidationResult.valid()
    }

    private fun validateKotlinBuildFile(content: String): ValidationResult {
      if (
          !content.contains("android {") &&
              !content.contains("dependencies {") &&
              !content.contains("plugins {")
      ) {
        return ValidationResult.error("Content doesn't appear to be a Kotlin DSL build script")
      }
      if (content.contains("apply plugin:") || content.contains("implementation '")) {
        return ValidationResult.error(
            "Content appears to be Groovy DSL but file is .gradle.kts (should be .gradle)"
        )
      }
      return ValidationResult.valid()
    }

    private fun validateXmlFile(fileName: String, content: String): ValidationResult {
      if (!content.trimStart().startsWith("<")) {
        return ValidationResult.error("XML file must start with < character")
      }

      when {
        fileName == "AndroidManifest.xml" -> {
          if (!content.contains("<manifest")) {
            return ValidationResult.error("AndroidManifest.xml must contain <manifest> element")
          }
        }
        fileName.startsWith("activity_") || fileName.startsWith("fragment_") -> {
          if (
              !content.contains(Regex("<(LinearLayout|RelativeLayout|ConstraintLayout|androidx)"))
          ) {
            return ValidationResult.error("Layout file should contain Android layout elements")
          }
        }
      }

      return ValidationResult.valid()
    }
  }

  data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null) {
    companion object {
      fun valid() = ValidationResult(true)

      fun error(message: String) = ValidationResult(false, message)
    }
  }
}
