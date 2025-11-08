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
package com.tom.rv2ide.fragments.output

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import com.blankj.utilcode.util.ThreadUtils
import com.tom.rv2ide.R
import com.tom.rv2ide.preferences.internal.BuildPreferences

class BuildOutputFragment : NonEditableEditorFragment() {
  private val unsavedLines: MutableList<String?> = ArrayList()
  private val pendingOutputBuffer = StringBuilder()
  private val flushHandler = Handler(Looper.getMainLooper())
  private var flushPending = false
  private var currentLineCount = 0

  companion object {
    private const val MAX_LINES = 10000
    private const val TRIM_TO_LINES = 8000
    private const val FLUSH_DELAY_MS = 150L
    private const val BATCH_SIZE = 100
  }

  private val flushRunnable = Runnable {
    flushPending = false
    flushPendingOutput()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    emptyStateViewModel.emptyMessage.value = getString(R.string.msg_emptyview_buildoutput)
    if (unsavedLines.isNotEmpty()) {
      for (line in unsavedLines) {
        editor?.append("${line!!.trim()}\n")
      }
      unsavedLines.clear()
    }
  }

  override fun onDestroyView() {
    flushHandler.removeCallbacks(flushRunnable)
    pendingOutputBuffer.clear()
    editor?.release()
    super.onDestroyView()
  }

  fun appendOutput(output: String?) {
    // Check if build output is disabled
    if (!BuildPreferences.isBuildOutputEnabled) {
      return
    }

    if (editor == null) {
      unsavedLines.add(output)
      return
    }

    val message =
        if (output == null || output.endsWith("\n")) {
          output ?: ""
        } else {
          "${output}\n"
        }

    synchronized(pendingOutputBuffer) { pendingOutputBuffer.append(message) }

    // Schedule flush if not already pending
    if (!flushPending) {
      flushPending = true
      flushHandler.postDelayed(flushRunnable, FLUSH_DELAY_MS)
    }
  }

  private fun flushPendingOutput() {
    val textToAppend: String
    synchronized(pendingOutputBuffer) {
      if (pendingOutputBuffer.isEmpty()) {
        return
      }
      textToAppend = pendingOutputBuffer.toString()
      pendingOutputBuffer.clear()
    }

    ThreadUtils.runOnUiThread {
      editor?.let { ed ->
        val newLines = textToAppend.count { it == '\n' }
        currentLineCount += newLines

        // Append in batches to reduce UI operations
        val lines = textToAppend.lines()
        lines.chunked(BATCH_SIZE).forEach { chunk ->
          ed.append(chunk.joinToString("\n") + if (chunk.last().isNotEmpty()) "\n" else "")
        }

        // Trim if exceeds max lines
        if (currentLineCount > MAX_LINES) {
          trimExcessLines()
        }

        emptyStateViewModel.isEmpty.value = false
      }
    }
  }

  private fun trimExcessLines() {
    editor?.let { ed ->
      try {
        val text = ed.text?.toString() ?: return
        val lines = text.lines()

        if (lines.size > MAX_LINES) {
          val trimmedLines = lines.takeLast(TRIM_TO_LINES)
          val linesToRemove = lines.size - TRIM_TO_LINES
          val newText =
              "... [${linesToRemove} lines trimmed for performance] ...\n\n" +
                  trimmedLines.joinToString("\n")

          ed.setText(newText)
          currentLineCount = TRIM_TO_LINES
        }
      } catch (e: Exception) {
        // Fail silently if trimming doesn't work
      }
    }
  }
}
