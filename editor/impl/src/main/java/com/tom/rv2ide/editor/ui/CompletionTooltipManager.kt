package com.tom.rv2ide.editor.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.tom.rv2ide.lsp.models.CompletionItem
import com.tom.rv2ide.lsp.models.DefinitionParams
import com.tom.rv2ide.progress.ICancelChecker
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/** Manages tooltips showing documentation for completion items */
class CompletionTooltipManager(private val context: Context, private val editor: CodeEditor) {

  companion object {
    private val log = LoggerFactory.getLogger(CompletionTooltipManager::class.java)
    private const val TOOLTIP_DELAY = 300L
  }

  private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private val handler = Handler(Looper.getMainLooper())
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private var tooltipView: View? = null
  private var currentJob: Job? = null
  private var showTooltipRunnable: Runnable? = null
  private var isShowing = false

  /** Request tooltip for a completion item */
  fun showTooltip(item: CompletionItem, anchorY: Int) {
    // Cancel any pending tooltip
    cancelPendingTooltip()

    // Check if item has detail
    val hasInfo = item.detail.isNotEmpty()

    if (hasInfo) {
      // Show detail immediately
      showTooltipImmediately(item.detail, anchorY)
      return
    }

    // Otherwise, schedule hover request
    showTooltipRunnable = Runnable { requestHoverInfo(item, anchorY) }
    handler.postDelayed(showTooltipRunnable!!, TOOLTIP_DELAY)
  }

  /** Hide the current tooltip */
  fun hideTooltip() {
    cancelPendingTooltip()
    dismissTooltip()
  }

  /** Clean up resources */
  fun destroy() {
    hideTooltip()
    scope.cancel()
  }

  private fun cancelPendingTooltip() {
    showTooltipRunnable?.let { handler.removeCallbacks(it) }
    showTooltipRunnable = null
    currentJob?.cancel()
    currentJob = null
  }

  private fun showTooltipImmediately(content: String, anchorY: Int) {
    if (content.isEmpty()) return

    dismissTooltip()
    displayTooltip(content, anchorY)
  }

  private fun requestHoverInfo(item: CompletionItem, anchorY: Int) {
    val ideEditor = editor as? IDEEditor ?: return
    val file = ideEditor.file ?: return
    val languageServer = ideEditor.languageServer ?: return

    currentJob =
        scope.launch {
          try {
            val line = editor.cursor.leftLine
            val column = editor.cursor.leftColumn

            // Create a cancel checker for the hover request
            val cancelChecker =
                object : ICancelChecker {
                  override fun isCancelled(): Boolean {
                    val job = currentJob
                    return job == null || !job.isActive
                  }

                  override fun abortIfCancelled() {
                    if (isCancelled()) {
                      throw CancellationException("Operation cancelled")
                    }
                  }

                  override fun cancel() {
                    currentJob?.cancel()
                  }
                }

            val params =
                DefinitionParams(
                    file = file.toPath(),
                    position = com.tom.rv2ide.models.Position(line, column),
                    cancelChecker = cancelChecker,
                )

            val hoverResult = withContext(Dispatchers.IO) { languageServer.hover(params) }

            if (hoverResult.value.isNotEmpty()) {
              withContext(Dispatchers.Main) { displayTooltip(hoverResult.value, anchorY) }
            }
          } catch (e: Exception) {
            if (e !is CancellationException) {
              log.debug("Failed to fetch hover info", e)
            }
          }
        }
  }

  private fun displayTooltip(content: String, anchorY: Int) {
    dismissTooltip()

    try {
      // Create a simple CardView with TextView
      val cardView =
          CardView(context).apply {
            radius = context.resources.displayMetrics.density * 8
            cardElevation = context.resources.displayMetrics.density * 8
            setCardBackgroundColor(0xFFF5F5F5.toInt())
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
          }

      val textView =
          TextView(context).apply {
            text = formatContent(content)
            textSize = 12f
            setTextColor(0xFF212121.toInt())
            typeface = android.graphics.Typeface.MONOSPACE
            maxLines = 10
            ellipsize = android.text.TextUtils.TruncateAt.END
          }

      cardView.addView(textView)

      // Measure the view
      val maxWidth = (windowManager.defaultDisplay.width * 0.8).toInt()
      cardView.measure(
          View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
      )

      val tooltipHeight = cardView.measuredHeight

      // Position tooltip above the completion window
      val params =
          WindowManager.LayoutParams(
              WindowManager.LayoutParams.WRAP_CONTENT,
              WindowManager.LayoutParams.WRAP_CONTENT,
              WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
              WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                  WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
              PixelFormat.TRANSLUCENT,
          )

      params.gravity = Gravity.TOP or Gravity.START
      params.x = dpToPx(16)

      // Calculate Y position - position above the anchor with some padding
      val padding = dpToPx(8)
      params.y = (anchorY - tooltipHeight - padding).coerceAtLeast(dpToPx(8))

      windowManager.addView(cardView, params)
      tooltipView = cardView
      isShowing = true
    } catch (e: Exception) {
      log.error("Failed to display tooltip", e)
    }
  }

  private fun dismissTooltip() {
    tooltipView?.let { view ->
      try {
        windowManager.removeView(view)
      } catch (e: Exception) {
        log.debug("Error removing tooltip view", e)
      }
      tooltipView = null
      isShowing = false
    }
  }

  /** Simple content formatter */
  private fun formatContent(text: String): String {
    return text
        .replace(Regex("```[a-z]*\\n"), "")
        .replace("```", "")
        .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
        .replace(Regex("\\*(.+?)\\*"), "$1")
        .replace(Regex("`(.+?)`"), "$1")
        .replace(Regex("^#+\\s+"), "")
        .trim()
        .take(500)
  }

  private fun dpToPx(dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
  }
}
