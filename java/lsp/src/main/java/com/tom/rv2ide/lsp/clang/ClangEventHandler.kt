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

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ClangEventHandler(private val documentManager: ClangDocumentManager) {

  private val lastChangeTime = java.util.concurrent.ConcurrentHashMap<String, Long>()
  private val changeThrottleMs = 200L

  @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.ASYNC)
  fun onContentChange(event: com.tom.rv2ide.eventbus.events.editor.DocumentChangeEvent) {
    val file = event.changedFile
    val ext = file.toString().substringAfterLast(".")
    if (ext !in setOf("c", "cpp", "cc", "cxx", "h", "hpp")) return

    val uri = file.toUri().toString()
    val currentTime = System.currentTimeMillis()
    val lastChange = lastChangeTime[uri] ?: 0L

    if (currentTime - lastChange < changeThrottleMs) {
      return
    }

    lastChangeTime[uri] = currentTime

    try {
      val content = event.newText ?: file.toFile().readText()

      if (content.isNotEmpty() && event.version > 0) {
        val currentVersion = documentManager.getDocumentVersion(uri)
        if (event.version > currentVersion) {
          documentManager.setDocumentVersion(uri, event.version)
          documentManager.notifyDocumentChange(file, content, event.version)
        }
      }
    } catch (e: Exception) {
      ClangLogs.error("Failed to handle document change", e)
    }
  }

  @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.ASYNC)
  fun onFileOpened(event: com.tom.rv2ide.eventbus.events.editor.DocumentOpenEvent) {
    val file = event.openedFile
    val ext = file.toString().substringAfterLast(".")
    if (ext !in setOf("c", "cpp", "cc", "cxx", "h", "hpp")) return

    ClangLogs.debug("Document open event for: {}", file)
    documentManager.ensureDocumentOpen(file, event.text)
  }

  @org.greenrobot.eventbus.Subscribe(threadMode = org.greenrobot.eventbus.ThreadMode.ASYNC)
  fun onFileClosed(event: com.tom.rv2ide.eventbus.events.editor.DocumentCloseEvent) {
    val file = event.closedFile
    val ext = file.toString().substringAfterLast(".")
    if (ext !in setOf("c", "cpp", "cc", "cxx", "h", "hpp")) return

    ClangLogs.debug("Document close event for: {}", file)
    documentManager.closeDocument(file)
  }
}
