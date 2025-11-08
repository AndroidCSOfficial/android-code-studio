/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.actions.text

import android.app.Activity
import android.content.Context
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.blankj.utilcode.util.KeyboardUtils
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.EditorRelatedAction
import com.tom.rv2ide.actions.editor.ExtractAction
import com.tom.rv2ide.actions.markInvisible
import com.tom.rv2ide.resources.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/**
 * @author Akash Yadav
 * @modification Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null ++ strings extraction
 */
class RedoAction(context: Context, override val order: Int) : EditorRelatedAction() {

  companion object {
    private val log = LoggerFactory.getLogger(RedoAction::class.java)
    private var pendingExtraction: ExtractAction.ExtractionInfo? = null

    // Static method to handle pending extractions
    fun setPendingExtraction(extraction: ExtractAction.ExtractionInfo?) {
      pendingExtraction = extraction
    }

    fun clearPendingExtraction() {
      pendingExtraction = null
    }
  }

  init {
    label = context.getString(R.string.redo)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_redo)
  }

  override val id: String = "ide.editor.code.text.redo"

  override fun prepare(data: ActionData) {
    super.prepare(data)

    if (!visible) {
      return
    }

    val editor =
        data.getEditor()
            ?: run {
              markInvisible()
              return
            }

    enabled = editor.canRedo()
  }

  override suspend fun execAction(data: ActionData): Boolean {
    val editor =
        data.getEditor()
            ?: run {
              markInvisible()
              return false
            }

    // First, perform the redo operation
    editor.redo()
    data.getActivity()?.invalidateOptionsMenu()

    // Check if there's a pending extraction that needs to be restored
    val extractionToRestore = pendingExtraction
    if (extractionToRestore != null) {
      log.debug("Restoring extraction: ${extractionToRestore.stringName}")

      CoroutineScope(Dispatchers.IO).launch {
        try {
          val success = reAddExtractedString(extractionToRestore)
          if (success) {
            // Restore the extraction info to ExtractAction
            ExtractAction.setLastExtraction(extractionToRestore)
            // Clear the pending extraction since it's been restored
            clearPendingExtraction()
            log.debug("Successfully restored extracted string: ${extractionToRestore.stringName}")
          } else {
            log.error("Failed to restore extracted string: ${extractionToRestore.stringName}")
          }
        } catch (e: Exception) {
          log.error("Error restoring extracted string to XML", e)
        }
      }
    }

    return true
  }

  override fun getShowAsActionFlags(data: ActionData): Int {
    return if (KeyboardUtils.isSoftInputVisible(data.get(Context::class.java) as Activity)) {
      MenuItem.SHOW_AS_ACTION_IF_ROOM
    } else {
      MenuItem.SHOW_AS_ACTION_NEVER
    }
  }

  private suspend fun reAddExtractedString(extractionInfo: ExtractAction.ExtractionInfo): Boolean {
    return withContext(Dispatchers.IO) {
      try {
        if (!extractionInfo.stringsFile.exists()) {
          log.error("Strings file does not exist: ${extractionInfo.stringsFile.absolutePath}")
          return@withContext false
        }

        val content = extractionInfo.stringsFile.readText()

        // Check if the string is already there (avoid duplicates)
        if (content.contains("""name="${extractionInfo.stringName}"""")) {
          log.debug("String '${extractionInfo.stringName}' already exists in strings.xml")
          return@withContext true // Consider this a success since it's already there
        }

        // Extract the original content from the original text (removing quotes if present)
        val originalText = extractionInfo.originalText.trim()
        val cleanContent =
            when {
              originalText.startsWith("\"") &&
                  originalText.endsWith("\"") &&
                  originalText.length > 1 -> originalText.substring(1, originalText.length - 1)
              originalText.startsWith("'") &&
                  originalText.endsWith("'") &&
                  originalText.length > 1 -> originalText.substring(1, originalText.length - 1)
              else -> originalText
            }

        // Escape the content for XML
        val escapedContent =
            cleanContent
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'")

        val newStringEntry =
            """    <string name="${extractionInfo.stringName}">$escapedContent</string>"""

        // Handle XML insertion (same logic as in ExtractAction)
        val updatedContent =
            if (content.contains("</resources>")) {
              // Insert before the closing </resources> tag
              val resourcesEnd = content.lastIndexOf("</resources>")
              val beforeEnd = content.substring(0, resourcesEnd)
              val afterEnd = content.substring(resourcesEnd)

              // Check if we need to add a newline before the new entry
              val needsNewline = !beforeEnd.endsWith("\n") && !beforeEnd.endsWith("\r\n")
              val prefix = if (needsNewline) "\n" else ""

              beforeEnd + prefix + newStringEntry + "\n" + afterEnd
            } else {
              // No </resources> tag found, create proper structure
              """<?xml version="1.0" encoding="utf-8"?>
<resources>
$newStringEntry
</resources>"""
            }

        extractionInfo.stringsFile.writeText(updatedContent)
        log.debug("Successfully re-added string '${extractionInfo.stringName}' to strings.xml")
        return@withContext true
      } catch (e: Exception) {
        log.error("Error re-adding string to XML file", e)
        return@withContext false
      }
    }
  }
}
