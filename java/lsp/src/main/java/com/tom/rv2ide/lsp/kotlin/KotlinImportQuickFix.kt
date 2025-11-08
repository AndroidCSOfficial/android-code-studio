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

import com.tom.rv2ide.models.Range
import java.nio.file.Path

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

/** Handles quick fix actions for import diagnostics */
class KotlinImportQuickFix(
    private val documentManager: KotlinDocumentManager,
    private val analyzer: KotlinImportAnalyzer,
) {

  /** Applies import quick fix when user clicks on orange warning */
  fun applyImportFix(file: Path, range: Range): Boolean {
    val metadata = analyzer.getMetadata(file, range) ?: return false

    // If multiple options, use first one for now
    // TODO: Show selection dialog for multiple imports
    val importToAdd = metadata.possibleImports.firstOrNull() ?: return false

    return addImport(file, importToAdd)
  }

  /** Gets available import options for a diagnostic */
  fun getImportOptions(file: Path, range: Range): List<String> {
    return analyzer.getMetadata(file, range)?.possibleImports ?: emptyList()
  }

  private fun addImport(file: Path, fullyQualifiedName: String): Boolean {
    try {
      val content = file.toFile().readText()

      // Check if already imported
      if (content.contains("import $fullyQualifiedName")) {
        KslLogs.debug("Import already exists: {}", fullyQualifiedName)
        return true
      }

      val lines = content.split("\n").toMutableList()
      val insertPosition = findImportInsertPosition(lines)

      // Add import statement
      lines.add(insertPosition, "import $fullyQualifiedName")

      val newContent = lines.joinToString("\n")
      val uri = file.toUri().toString()
      val version = documentManager.getDocumentVersion(uri) + 1

      documentManager.setDocumentVersion(uri, version)
      documentManager.notifyDocumentChange(file, newContent, version)

      KslLogs.info("Auto-imported: {}", fullyQualifiedName)
      return true
    } catch (e: Exception) {
      KslLogs.error("Failed to add import", e)
      return false
    }
  }

  private fun findImportInsertPosition(lines: List<String>): Int {
    var lastImportIndex = -1
    var packageIndex = -1

    for ((index, line) in lines.withIndex()) {
      val trimmed = line.trim()

      when {
        trimmed.startsWith("package ") -> packageIndex = index
        trimmed.startsWith("import ") -> lastImportIndex = index
        trimmed.isNotEmpty() &&
            !trimmed.startsWith("//") &&
            !trimmed.startsWith("/*") &&
            lastImportIndex >= 0 -> break
      }
    }

    return when {
      lastImportIndex >= 0 -> lastImportIndex + 1
      packageIndex >= 0 -> packageIndex + 2
      else -> 0
    }
  }
}
