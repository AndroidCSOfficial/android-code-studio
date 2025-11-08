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

import com.google.gson.JsonObject
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinDocumentManager(private val processManager: KotlinServerProcessManager) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinDocumentManager::class.java)
  }

  private val openedDocuments = ConcurrentHashMap.newKeySet<String>()
  private val documentVersions = ConcurrentHashMap<String, Int>()

  fun ensureDocumentOpen(file: Path, content: String? = null) {
    val uri = file.toUri().toString()
    if (openedDocuments.add(uri)) {
      KslLogs.info("Opening document: {}", uri)
      val text =
          content
              ?: try {
                file.toFile().readText()
              } catch (e: Exception) {
                KslLogs.error("Failed to read file: {}", file, e)
                return
              }

      val version = getDocumentVersion(uri) + 1
      setDocumentVersion(uri, version)

      val params =
          JsonObject().apply {
            add(
                "textDocument",
                JsonObject().apply {
                  addProperty("uri", uri)
                  addProperty("languageId", "kotlin")
                  addProperty("version", version)
                  addProperty("text", text)
                },
            )
          }
      KslLogs.info("Sending didOpen notification for: {}", uri)
      processManager.sendNotification("textDocument/didOpen", params)
      // Some servers require an explicit open-before-lint; trigger immediate lint
      // Use a handler to delay didSave slightly to ensure didOpen is processed first
      android.os
          .Handler(android.os.Looper.getMainLooper())
          .postDelayed(
              {
                KslLogs.info("Sending didSave notification for: {}", uri)
                notifyDocumentSave(file)
                // Send another didSave after a longer delay to ensure server processes it
                android.os
                    .Handler(android.os.Looper.getMainLooper())
                    .postDelayed(
                        {
                          KslLogs.info("Sending second didSave notification for: {}", uri)
                          notifyDocumentSave(file)
                        },
                        200,
                    )
              },
              50,
          )
    } else {
      // If already open, make sure server has some recent version, avoid stale state
      if (getDocumentVersion(uri) <= 0) {
        setDocumentVersion(uri, 1)
      }
    }
  }

  fun notifyDocumentChange(file: Path, newText: String, version: Int) {
    val uri = file.toUri().toString()

    if (!openedDocuments.contains(uri)) {
      KslLogs.warn("Document not opened, opening it first: {}", uri)
      ensureDocumentOpen(file, newText)
      // Trigger immediate lint after open to avoid "not open" diagnostic ignores
      notifyDocumentSave(file)
      return
    }

    KslLogs.debug("Notifying document change: {} (version: {})", uri, version)

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

    processManager.sendNotification("textDocument/didChange", params)
  }

  fun notifyDocumentSave(file: Path) {
    // val uri = file.toUri().toString()
    // KslLogs.debug("Sending didSave notification for: {}", uri)
    // val params = JsonObject().apply {
    // add("textDocument", JsonObject().apply {
    // addProperty("uri", uri)
    // })
    // }
    // processManager.sendNotification("textDocument/didSave", params)
  }

  fun closeDocument(file: Path) {
    val uri = file.toUri().toString()
    if (openedDocuments.remove(uri)) {
      val params =
          JsonObject().apply { add("textDocument", JsonObject().apply { addProperty("uri", uri) }) }
      processManager.sendNotification("textDocument/didClose", params)
    }
  }

  fun isDocumentOpen(uri: String): Boolean = openedDocuments.contains(uri)

  fun getDocumentVersion(uri: String): Int = documentVersions.getOrDefault(uri, 0)

  fun setDocumentVersion(uri: String, version: Int) {
    documentVersions[uri] = version
  }

  fun clear() {
    openedDocuments.clear()
    documentVersions.clear()
  }
}
