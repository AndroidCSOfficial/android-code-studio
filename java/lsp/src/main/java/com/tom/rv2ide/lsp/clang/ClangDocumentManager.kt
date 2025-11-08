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
package com.tom.rv2ide.lsp.clang

import com.google.gson.JsonObject
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ClangDocumentManager(private val processManager: ClangServerProcessManager) {

  companion object {
    private val log = LoggerFactory.getLogger(ClangDocumentManager::class.java)
  }

  private val openedDocuments = ConcurrentHashMap.newKeySet<String>()
  private val documentVersions = ConcurrentHashMap<String, Int>()

  fun ensureDocumentOpen(file: Path, content: String? = null) {
    val uri = file.toUri().toString()

    ClangLogs.error("=== ensureDocumentOpen called ===")
    ClangLogs.error("URI: {}", uri)
    ClangLogs.error("Content provided: {}", content != null)
    ClangLogs.error("In openedDocuments set: {}", openedDocuments.contains(uri))
    ClangLogs.error("Current openedDocuments: {}", openedDocuments.toList())

    // Check if already opened
    if (openedDocuments.contains(uri)) {
      ClangLogs.error("Document supposedly open, SKIPPING")
      return
    }

    ClangLogs.error("Opening document NOW")

    val text =
        content
            ?: try {
              file.toFile().readText()
            } catch (e: Exception) {
              ClangLogs.error("Failed to read file: {}", file, e)
              return
            }

    val languageId =
        when {
          file.toString().endsWith(".c") -> "c"
          file.toString().endsWith(".cpp") ||
              file.toString().endsWith(".cc") ||
              file.toString().endsWith(".cxx") -> "cpp"
          file.toString().endsWith(".h") || file.toString().endsWith(".hpp") -> "cpp"
          else -> "c"
        }

    // Mark as opened BEFORE sending notification
    openedDocuments.add(uri)
    documentVersions[uri] = 1

    ClangLogs.error("Added to openedDocuments, size now: {}", openedDocuments.size)

    val params =
        JsonObject().apply {
          add(
              "textDocument",
              JsonObject().apply {
                addProperty("uri", uri)
                addProperty("languageId", languageId)
                addProperty("version", 1)
                addProperty("text", text)
              },
          )
        }

    ClangLogs.error("Sending didOpen notification...")
    processManager.sendNotification("textDocument/didOpen", params)

    ClangLogs.error("Document opened successfully: {}", uri)
  }

  fun closeDocument(file: Path) {
    val uri = file.toUri().toString()

    ClangLogs.error("=== closeDocument called ===")
    ClangLogs.error("URI: {}", uri)
    ClangLogs.error("Was in set: {}", openedDocuments.contains(uri))

    if (openedDocuments.remove(uri)) {
      documentVersions.remove(uri)

      val params =
          JsonObject().apply { add("textDocument", JsonObject().apply { addProperty("uri", uri) }) }
      processManager.sendNotification("textDocument/didClose", params)
      ClangLogs.error("Document CLOSED: {}", uri)
    } else {
      ClangLogs.error("Document was NOT in set, nothing closed")
    }
  }

  fun clear() {
    ClangLogs.error("=== CLEAR CALLED - CLOSING ALL DOCUMENTS ===")
    ClangLogs.error("Documents to clear: {}", openedDocuments.toList())
    openedDocuments.clear()
    documentVersions.clear()
  }

  fun notifyDocumentChange(file: Path, newText: String, version: Int) {
    val uri = file.toUri().toString()

    // CRITICAL: If not in our tracking set, clangd definitely doesn't have it
    if (!openedDocuments.contains(uri)) {
      ClangLogs.warn("Document not in tracking set, opening fresh: {}", uri)
      ensureDocumentOpen(file, newText)
      return
    }

    ClangLogs.debug("Notifying document change: {} (version: {})", uri, version)

    val params =
        JsonObject().apply {
          add(
              "textDocument",
              JsonObject().apply {
                addProperty("uri", uri)
                addProperty("version", version)
              },
          )
          add(
              "contentChanges",
              com.google.gson.JsonArray().apply {
                add(JsonObject().apply { addProperty("text", newText) })
              },
          )
        }

    try {
      processManager.sendNotification("textDocument/didChange", params)
    } catch (e: Exception) {
      ClangLogs.error("Failed to send didChange, reopening document", e)
      openedDocuments.remove(uri)
      ensureDocumentOpen(file, newText)
    }
  }

  fun isDocumentOpen(uri: String): Boolean {
    val isOpen = openedDocuments.contains(uri)
    ClangLogs.debug("isDocumentOpen({}): {}", uri, isOpen)
    return isOpen
  }

  fun getDocumentVersion(uri: String): Int {
    return documentVersions.getOrDefault(uri, 0)
  }

  fun setDocumentVersion(uri: String, version: Int) {
    documentVersions[uri] = version
  }
}
