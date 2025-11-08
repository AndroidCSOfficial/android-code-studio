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
package com.tom.rv2ide.fragments.sidebar.utils

import android.content.Context
import android.widget.LinearLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ThreadUtils
import com.tom.rv2ide.projects.internal.ProjectManagerImpl
import com.tom.rv2ide.utils.ProjectHelper
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
enum class ResponseMode {
  CHAT, // Display in UI
  FILE_MODIFICATION, // Write to files
}

class AIResponseHandler(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val responseContainer: LinearLayout,
    private val onError: (String) -> Unit,
    private val onSuccess: (String) -> Unit,
) {

  companion object {
    private val log = LoggerFactory.getLogger(AIResponseHandler::class.java)
  }

  private val projectManager: ProjectManagerImpl
    get() = ProjectManagerImpl.getInstance()

  private var currentMode: ResponseMode = ResponseMode.CHAT
  private val liveRenderer: LiveResponseRenderer = LiveResponseRenderer(context, responseContainer)

  fun setMode(mode: ResponseMode) {
    currentMode = mode
    log.debug("Response mode set to: $mode")
  }

  fun getMode(): ResponseMode = currentMode

  /** Process streaming chunk (for live response) */
  fun processStreamingChunk(chunk: String) {
    if (currentMode == ResponseMode.CHAT) {
      liveRenderer.appendChunk(chunk)
    }
  }

  /** Finalize streaming response */
  fun finalizeStreamingResponse() {
    if (currentMode == ResponseMode.CHAT) {
      liveRenderer.finalize()
    }
  }

  /** Main entry point for processing AI responses */
  fun processResponse(
      response: String,
      originalPrompt: String,
      currentFileInserter: ((String) -> Unit)? = null,
  ) {
    lifecycleOwner.lifecycleScope.launch {
      try {
        log.debug("Processing AI response in ${currentMode} mode")

        when (currentMode) {
          ResponseMode.CHAT -> {
            displayChatResponse(response)
          }
          ResponseMode.FILE_MODIFICATION -> {
            processFileModifications(response, currentFileInserter)
          }
        }
      } catch (e: Exception) {
        log.error("Error processing AI response", e)
        onError("Error processing response: ${e.message}")
      }
    }
  }

  /** Display response as chat in the UI */
  private fun displayChatResponse(response: String) {
    ThreadUtils.runOnUiThread {
      try {
        // Use live renderer for better code block display
        liveRenderer.clear()
        liveRenderer.appendChunk(response)
        liveRenderer.finalize()

        log.info("Displayed chat response in UI")
      } catch (e: Exception) {
        log.error("Error displaying chat response", e)
        onError("Error displaying response: ${e.message}")
      }
    }
  }

  /** Process file modifications from AI response */
  private fun processFileModifications(response: String, currentFileInserter: ((String) -> Unit)?) {
    val fileModifications = parseFileModifications(response)

    if (fileModifications.isEmpty()) {
      log.warn("No file modifications found, treating as single file content")
      val cleanedResponse = cleanMarkdownBlocks(response)
      currentFileInserter?.invoke(cleanedResponse)
          ?: run { onError("No editor available to insert code") }
      return
    }

    var modificationCount = 0
    var failureCount = 0

    fileModifications.forEach { (filePath, content) ->
      try {
        val cleanedContent = cleanMarkdownBlocks(content)
        log.debug("Writing to file: $filePath (${cleanedContent.length} bytes)")

        if (writeToFile(filePath, cleanedContent)) {
          modificationCount++
          log.info("Successfully wrote to file: $filePath")
        } else {
          failureCount++
          log.error("Failed to write to file: $filePath")
        }
      } catch (e: Exception) {
        failureCount++
        log.error("Failed to write to file: $filePath", e)
        onError("Failed to write to file $filePath: ${e.message}")
      }
    }

    ThreadUtils.runOnUiThread {
      if (modificationCount > 0) {
        onSuccess(
            "Successfully modified $modificationCount file(s)" +
                if (failureCount > 0) ", $failureCount failed" else ""
        )
      } else {
        onError("Failed to modify any files. Check logs for details.")
      }
    }
  }

  /** Parse file modifications from AI response */
  private fun parseFileModifications(response: String): Map<String, String> {
    val modifications = mutableMapOf<String, String>()
    val cleanedResponse = cleanResponseFromExplanations(response)
    val lines = cleanedResponse.lines()

    var currentFile: String? = null
    val currentContent = StringBuilder()
    var inFileContent = false

    for (line in lines) {
      val trimmedLine = line.trim()

      when {
        trimmedLine.matches(Regex("^====\\s*FILE:\\s*(.+?)\\s*====\$")) -> {
          currentFile?.let { file -> modifications[file] = currentContent.toString().trimEnd() }

          currentFile =
              trimmedLine
                  .replace(Regex("^====\\s*FILE:\\s*"), "")
                  .replace(Regex("\\s*====\$"), "")
                  .trim()
          currentContent.clear()
          inFileContent = true
        }

        trimmedLine.matches(Regex("^====\\s*END_FILE\\s*====\$")) -> {
          inFileContent = false
        }

        inFileContent && currentFile != null -> {
          if (
              !trimmedLine.startsWith("```") &&
                  !trimmedLine.startsWith("---") &&
                  !trimmedLine.matches(Regex("^\\*\\*.*\\*\\*\$"))
          ) {
            currentContent.append(line).append("\n")
          }
        }
      }
    }

    currentFile?.let { file -> modifications[file] = currentContent.toString().trimEnd() }

    return modifications
  }

  private fun cleanResponseFromExplanations(response: String): String {
    val lines = response.lines()
    val cleanedLines = mutableListOf<String>()
    var skipRestOfResponse = false

    for (line in lines) {
      if (skipRestOfResponse) break

      val trimmedLine = line.trim()

      when {
        trimmedLine.matches(Regex("^====\\s*FILE:.*====\$")) -> {
          cleanedLines.add(line)
        }
        trimmedLine.matches(Regex("^====\\s*END_FILE\\s*====\$")) -> {
          cleanedLines.add(line)
          val remainingLines = lines.drop(lines.indexOf(line) + 1)
          val hasMoreFiles =
              remainingLines.any { it.trim().matches(Regex("^====\\s*FILE:.*====\$")) }
          if (!hasMoreFiles) {
            skipRestOfResponse = true
          }
        }
        else -> {
          if (!isExplanatoryContent(trimmedLine)) {
            cleanedLines.add(line)
          }
        }
      }
    }

    return cleanedLines.joinToString("\n")
  }

  private fun isExplanatoryContent(line: String): Boolean {
    val explanatoryPhrases =
        listOf(
            "the original code had",
            "the corrected code",
            "this fixes",
            "the issue was",
            "i removed",
            "i fixed",
            "the problem",
            "duplicate",
            "causing the compilation errors",
        )
    val lowerLine = line.lowercase()
    return explanatoryPhrases.any { phrase -> lowerLine.contains(phrase) }
  }

  private fun cleanMarkdownBlocks(content: String): String {
    var cleaned = content
    cleaned = cleaned.replace(Regex("\\\\```\\w*\\n?"), "")
    cleaned = cleaned.replace("\\\\```", "")
    cleaned = cleaned.replace(Regex("```[\\w-]*\\n?"), "")
    cleaned = cleaned.replace("```", "")
    cleaned = cleaned.replace(Regex("^\\*\\*.*\\*\\*\$", RegexOption.MULTILINE), "")
    cleaned = cleaned.replace(Regex("^#+ .*\$", RegexOption.MULTILINE), "")
    cleaned = cleaned.replace(Regex("^---+\$", RegexOption.MULTILINE), "")

    return cleaned
        .lines()
        .dropWhile { it.trim().isEmpty() }
        .dropLastWhile { it.trim().isEmpty() }
        .joinToString("\n")
        .trim()
  }

  private fun writeToFile(aiProvidedPath: String, content: String): Boolean {
    return try {
      if (content.trim().isEmpty()) {
        log.error("Content is empty for file: $aiProvidedPath")
        return false
      }

      val success = ProjectHelper.writeFileToProject(aiProvidedPath, content)

      if (success) {
        try {
          val resolvedFile = ProjectHelper.resolveProjectFilePath(aiProvidedPath)
          if (resolvedFile?.exists() == true) {
            projectManager.notifyFileCreated(resolvedFile)
          }
        } catch (e: Exception) {
          log.warn("Failed to notify project manager: $aiProvidedPath", e)
        }
      }

      success
    } catch (e: Exception) {
      log.error("Error writing to file: $aiProvidedPath", e)
      false
    }
  }

  fun clearResponse() {
    liveRenderer.clear()
  }
}
