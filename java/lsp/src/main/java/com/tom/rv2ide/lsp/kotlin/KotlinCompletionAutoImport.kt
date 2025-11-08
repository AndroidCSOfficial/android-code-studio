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

import com.tom.rv2ide.lsp.models.CompletionItem
import java.nio.file.Path
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinCompletionAutoImport(private val documentManager: KotlinDocumentManager) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinCompletionAutoImport::class.java)
  }

  /**
   * Checks if completion needs import and applies it automatically Call this when user selects a
   * completion item
   */
  suspend fun handleCompletionSelected(file: Path, item: CompletionItem): Boolean {
    try {
      // Check if this completion has additionalTextEdits (imports)
      val additionalEdits = item.additionalTextEdits
      if (additionalEdits.isNullOrEmpty()) {
        return false // No import needed
      }

      // Apply all additional edits (usually imports)
      for (edit in additionalEdits) {
        val importStatement = edit.newText.trim()
        if (importStatement.startsWith("import ")) {
          addImportToFile(file, importStatement)
        }
      }

      return true
    } catch (e: Exception) {
      log.error("Failed to auto-import for completion", e)
      return false
    }
  }

  private fun addImportToFile(file: Path, importStatement: String): Boolean {
    try {
      val content = file.toFile().readText()

      // Check if already imported
      if (content.contains(importStatement)) {
        log.debug("Import already exists: {}", importStatement)
        return true
      }

      val lines = content.split("\n").toMutableList()
      val insertPosition = findImportInsertPosition(lines)

      lines.add(insertPosition, importStatement)

      val newContent = lines.joinToString("\n")
      val uri = file.toUri().toString()
      val version = documentManager.getDocumentVersion(uri) + 1

      documentManager.setDocumentVersion(uri, version)
      documentManager.notifyDocumentChange(file, newContent, version)

      log.info("Auto-imported: {}", importStatement)
      return true
    } catch (e: Exception) {
      log.error("Failed to add import", e)
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
