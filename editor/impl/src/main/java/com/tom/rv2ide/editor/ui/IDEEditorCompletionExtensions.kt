package com.tom.rv2ide.editor.ui

import com.tom.rv2ide.lsp.models.CompletionItem
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion

/** Extension functions to add tooltip support to IDE editor */
private const val TOOLTIP_MANAGER_TAG = 0x7F0A0001

/** Initialize tooltip support for completion items */
fun IDEEditor.initCompletionTooltips() {
  val tooltipManager = CompletionTooltipManager(context, this)
  setTag(TOOLTIP_MANAGER_TAG, tooltipManager)

  // Hook into the completion window
  setupCompletionItemListener(tooltipManager)
}

/** Setup listener for completion item changes */
private fun CodeEditor.setupCompletionItemListener(tooltipManager: CompletionTooltipManager) {
  // Listen to content changes - when completion is showing and user types/navigates
  subscribeEvent(io.github.rosemoe.sora.event.ContentChangeEvent::class.java) { event, unsubscribe
    ->
    val autoCompletion = getComponent(EditorAutoCompletion::class.java)
    if (autoCompletion.isShowing) {
      // Delay to let completion update
      postDelayed({ handleCompletionSelection(tooltipManager, autoCompletion) }, 50)
    } else {
      tooltipManager.hideTooltip()
    }
  }

  // Hide tooltip when editor loses focus or is released
  subscribeEvent(io.github.rosemoe.sora.event.EditorReleaseEvent::class.java) { event, unsubscribe
    ->
    tooltipManager.hideTooltip()
  }
}

/** Handle completion item selection */
private fun CodeEditor.handleCompletionSelection(
    tooltipManager: CompletionTooltipManager,
    autoCompletion: EditorAutoCompletion,
) {
  try {
    val currentSelection = getCurrentCompletionItem() ?: return
    val completionWindowY = getCompletionWindowY()

    tooltipManager.showTooltip(currentSelection, completionWindowY)
  } catch (e: Exception) {
    // Silently fail
  }
}

/** Get the currently selected completion item using reflection */
private fun CodeEditor.getCurrentCompletionItem(): CompletionItem? {
  try {
    val autoCompletion = getComponent(EditorAutoCompletion::class.java)

    val windowField = EditorAutoCompletion::class.java.getDeclaredField("mWindow")
    windowField.isAccessible = true
    val window = windowField.get(autoCompletion)

    if (window != null) {
      val listViewField = window.javaClass.getDeclaredField("mListView")
      listViewField.isAccessible = true
      val listView = listViewField.get(window) as? android.widget.ListView

      val selectedPosition = listView?.selectedItemPosition ?: return null
      val adapter = listView.adapter

      if (selectedPosition >= 0 && selectedPosition < adapter.count) {
        return adapter.getItem(selectedPosition) as? CompletionItem
      }
    }
  } catch (e: Exception) {
    // Reflection failed
  }
  return null
}

/** Get the Y position of the completion window */
private fun CodeEditor.getCompletionWindowY(): Int {
  try {
    val autoCompletion = getComponent(EditorAutoCompletion::class.java)

    val windowField = EditorAutoCompletion::class.java.getDeclaredField("mWindow")
    windowField.isAccessible = true
    val window = windowField.get(autoCompletion)

    if (window != null) {
      val locationField = window.javaClass.getDeclaredField("mY")
      locationField.isAccessible = true
      return locationField.getInt(window)
    }
  } catch (e: Exception) {
    // Reflection failed
  }

  // Fallback: use cursor position with row height
  val rowHeight = (textSizePx + (textSizePx * 0.2f)).toInt()
  return cursor.leftLine * rowHeight
}

/** Clean up tooltip resources */
fun IDEEditor.cleanupCompletionTooltips() {
  val tooltipManager = getTag(TOOLTIP_MANAGER_TAG) as? CompletionTooltipManager
  tooltipManager?.destroy()
  setTag(TOOLTIP_MANAGER_TAG, null)
}
