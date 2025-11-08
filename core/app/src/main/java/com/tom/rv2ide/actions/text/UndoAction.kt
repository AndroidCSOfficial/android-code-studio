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

import android.content.Context
import androidx.core.content.ContextCompat
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.EditorRelatedAction
import com.tom.rv2ide.actions.editor.ExtractAction
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
class UndoAction(context: Context, override val order: Int) : EditorRelatedAction() {

  companion object {
    private val log = LoggerFactory.getLogger(UndoAction::class.java)
  }

  override val id: String = "ide.editor.code.text.undo"

  init {
    label = context.getString(R.string.undo)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_undo)
  }

  override fun prepare(data: ActionData) {
    super.prepare(data)

    if (!visible) {
      return
    }

    val editor = data.getEditor()!!
    enabled = editor.canUndo()
  }

  override suspend fun execAction(data: ActionData): Any {
    val editor = data.getEditor()
    return if (editor != null) {
      // Check if the last action was a string extraction
      val lastExtraction = ExtractAction.getLastExtraction()

      // Perform the undo
      editor.undo()
      data.getActivity()?.invalidateOptionsMenu()

      // If there was a recent string extraction, remove it from strings.xml
      // and set it as pending for redo
      if (lastExtraction != null) {
        log.debug("Processing undo for extracted string: ${lastExtraction.stringName}")

        CoroutineScope(Dispatchers.IO).launch {
          try {
            val removed = removeLastExtractedString(lastExtraction)
            if (removed) {
              // Set this extraction as pending for potential redo
              RedoAction.setPendingExtraction(lastExtraction)
              // Clear it from ExtractAction since it's been undone
              ExtractAction.clearLastExtraction()
              log.debug("Successfully undone string extraction: ${lastExtraction.stringName}")
            } else {
              log.error("Failed to remove extracted string: ${lastExtraction.stringName}")
            }
          } catch (e: Exception) {
            log.error("Error removing extracted string from XML", e)
          }
        }
      }

      true
    } else {
      false
    }
  }

  private suspend fun removeLastExtractedString(
      extractionInfo: ExtractAction.ExtractionInfo
  ): Boolean {
    return withContext(Dispatchers.IO) {
      try {
        if (!extractionInfo.stringsFile.exists()) {
          log.error("Strings file does not exist: ${extractionInfo.stringsFile.absolutePath}")
          return@withContext false
        }

        val content = extractionInfo.stringsFile.readText()

        // find the exact line containing our string
        val lines = content.lines().toMutableList()
        var foundIndex = -1

        for ((index, line) in lines.withIndex()) {
          if (
              line.contains("""name="${extractionInfo.stringName}"""") &&
                  line.trim().startsWith("<string")
          ) {
            foundIndex = index
            break
          }
        }

        if (foundIndex != -1) {
          // Remove the specific line
          lines.removeAt(foundIndex)

          // Clean up empty lines for better formatting
          if (foundIndex < lines.size && lines[foundIndex].trim().isEmpty()) {
            lines.removeAt(foundIndex)
          }
          if (
              foundIndex > 0 &&
                  foundIndex <= lines.size &&
                  lines[foundIndex - 1].trim().isEmpty() &&
                  lines.size > foundIndex &&
                  !lines[foundIndex].trim().isEmpty()
          ) {
            // Only remove empty line before if there's content after
            lines.removeAt(foundIndex - 1)
          }

          val updatedContent = lines.joinToString("\n")
          extractionInfo.stringsFile.writeText(updatedContent)
          log.debug("Successfully removed string '${extractionInfo.stringName}' from strings.xml")
          return@withContext true
        } else {
          log.debug("String '${extractionInfo.stringName}' not found in strings.xml")
          return@withContext false
        }
      } catch (e: Exception) {
        log.error("Error removing string from XML file", e)
        return@withContext false
      }
    }
  }
}
