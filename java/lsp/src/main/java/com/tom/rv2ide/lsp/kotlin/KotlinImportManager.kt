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

package com.tom.rv2ide.lsp.kotlin

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * Manages automatic imports for Kotlin completions When a completion item requires an import, this
 * manager will automatically add it
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class KotlinImportManager(
    private val processManager: KotlinServerProcessManager,
    private val documentManager: KotlinDocumentManager,
) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinImportManager::class.java)

    // Cache of class name to fully qualified name mappings
    private val importCache = ConcurrentHashMap<String, String>()
  }

  /** Checks if a completion item needs an import and adds it if necessary */
  suspend fun ensureImport(file: Path, className: String, detail: String?): String? {
    if (isKeywordOrPrimitive(className)) {
      return null
    }

    val fullyQualifiedName =
        extractFullyQualifiedName(className, detail) ?: importCache[className] ?: return null

    if (hasImport(file, fullyQualifiedName)) {
      KslLogs.debug("Import already exists: {}", fullyQualifiedName)
      return null
    }

    return addImport(file, fullyQualifiedName)
  }

  /** Adds an import statement to the file */
  private fun addImport(file: Path, fullyQualifiedName: String): String {
    try {
      val content = file.toFile().readText()
      val lines = content.lines().toMutableList()

      val importPosition = findImportInsertPosition(lines)
      val importStatement = "import $fullyQualifiedName"

      lines.add(importPosition, importStatement)

      val newContent = lines.joinToString("\n")
      val uri = file.toUri().toString()
      val version = documentManager.getDocumentVersion(uri) + 1
      documentManager.setDocumentVersion(uri, version)
      documentManager.notifyDocumentChange(file, newContent, version)

      KslLogs.info("Added import: {}", fullyQualifiedName)

      val simpleName = fullyQualifiedName.substringAfterLast('.')
      importCache[simpleName] = fullyQualifiedName

      return importStatement
    } catch (e: Exception) {
      KslLogs.error("Failed to add import", e)
      return ""
    }
  }

  /** Finds the appropriate position to insert a new import */
  private fun findImportInsertPosition(lines: List<String>): Int {
    var lastImportIndex = -1
    var packageIndex = -1

    for ((index, line) in lines.withIndex()) {
      val trimmed = line.trim()

      if (trimmed.startsWith("package ")) {
        packageIndex = index
      } else if (trimmed.startsWith("import ")) {
        lastImportIndex = index
      } else if (
          trimmed.isNotEmpty() &&
              !trimmed.startsWith("//") &&
              !trimmed.startsWith("/*") &&
              lastImportIndex >= 0
      ) {
        break
      }
    }

    return when {
      lastImportIndex >= 0 -> lastImportIndex + 1
      packageIndex >= 0 -> packageIndex + 2
      else -> 0
    }
  }

  /** Checks if the file already has the import */
  private fun hasImport(file: Path, fullyQualifiedName: String): Boolean {
    try {
      val content = file.toFile().readText()
      return content.contains("import $fullyQualifiedName")
    } catch (e: Exception) {
      return false
    }
  }

  /** Extracts fully qualified name from completion detail */
  private fun extractFullyQualifiedName(className: String, detail: String?): String? {
    if (detail.isNullOrBlank()) return null

    // Pattern 1: "defined in package.Class"
    val definedInPattern = """defined in ([a-zA-Z0-9_.]+)""".toRegex()
    definedInPattern.find(detail)?.let {
      val packageOrFqn = it.groupValues[1]
      return if (packageOrFqn.endsWith(".$className")) {
        packageOrFqn
      } else {
        "$packageOrFqn.$className"
      }
    }

    // Pattern 2: Just package name in detail
    val packagePattern = """^([a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*)*)$""".toRegex()
    if (packagePattern.matches(detail)) {
      return "$detail.$className"
    }

    // Pattern 3: Full qualified name in parentheses
    val fqnPattern = """([a-zA-Z0-9_.]+\.$className)""".toRegex()
    fqnPattern.find(detail)?.let {
      return it.groupValues[1]
    }

    return null
  }

  /** Checks if the name is a keyword or primitive type */
  private fun isKeywordOrPrimitive(name: String): Boolean {
    val keywords =
        setOf(
            "package",
            "import",
            "class",
            "interface",
            "fun",
            "val",
            "var",
            "if",
            "else",
            "when",
            "for",
            "while",
            "do",
            "return",
            "break",
            "continue",
            "object",
            "companion",
            "this",
            "super",
            "is",
            "in",
            "as",
            "try",
            "catch",
            "finally",
            "throw",
            "null",
            "true",
            "false",
        )

    val primitives =
        setOf(
            "Int",
            "Long",
            "Short",
            "Byte",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "String",
            "Unit",
            "Nothing",
            "Any",
        )

    return name in keywords || name in primitives
  }

  fun clearCache() {
    importCache.clear()
  }
}
