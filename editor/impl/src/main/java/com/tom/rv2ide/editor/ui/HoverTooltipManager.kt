package com.tom.rv2ide.editor.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.tom.rv2ide.lsp.models.DefinitionParams
import com.tom.rv2ide.models.Position
import com.tom.rv2ide.progress.ICancelChecker
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/** Manages hover tooltips showing documentation when cursor hovers over code */
class HoverTooltipManager(private val context: Context, private val editor: IDEEditor) {

  companion object {
    private val log = LoggerFactory.getLogger(HoverTooltipManager::class.java)
    private const val HOVER_DELAY = 800L
  }

  // private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
  private val tooltipContainer: ViewGroup by lazy {
    (editor.parent as? ViewGroup)
        ?: (editor.rootView as? ViewGroup)
        ?: throw IllegalStateException("Unable to find a valid ViewGroup container for tooltip.")
  }

  private val handler = Handler(Looper.getMainLooper())
  private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  private var tooltipView: View? = null
  private var currentJob: Job? = null
  private var hoverRunnable: Runnable? = null
  private var lastHoverLine = -1
  private var lastHoverColumn = -1

  /** Initialize hover support */
  fun init() {
    // Listen to cursor/selection changes
    editor.subscribeEvent(io.github.rosemoe.sora.event.SelectionChangeEvent::class.java) { event, _
      ->
      handleCursorMove()
    }
  }

  private fun handleCursorMove() {
    val cursor = editor.cursor
    val line = cursor.leftLine
    val column = cursor.leftColumn

    // If same position, don't restart
    if (line == lastHoverLine && column == lastHoverColumn) {
      return
    }

    lastHoverLine = line
    lastHoverColumn = column

    cancelHover()

    // Schedule hover request after cursor stops moving
    hoverRunnable = Runnable { requestHover(line, column) }
    handler.postDelayed(hoverRunnable!!, HOVER_DELAY)
  }

  private fun cancelHover() {
    hoverRunnable?.let { handler.removeCallbacks(it) }
    hoverRunnable = null
    currentJob?.cancel()
    currentJob = null
    dismissTooltip()
  }

  private fun requestHover(line: Int, column: Int) {
    val file = editor.file ?: return
    val languageServer = editor.languageServer ?: return

    currentJob =
        scope.launch {
          try {
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
                    position = Position(line, column),
                    cancelChecker = cancelChecker,
                )

            val hoverResult = withContext(Dispatchers.IO) { languageServer.hover(params) }

            // Filter out meaningless hover results
            val content = hoverResult.value.trim()
            if (
                content.isNotEmpty() &&
                    content != "Unit" &&
                    !content.equals("unit", ignoreCase = true) &&
                    content.length > 2
            ) {
              withContext(Dispatchers.Main) { displayTooltip(content) }
            }
          } catch (e: Exception) {
            if (e !is CancellationException) {
              log.debug("Failed to fetch hover info", e)
            }
          }
        }
  }

  private fun displayTooltip(content: String) {
    dismissTooltip()

    try {
      // Get dynamic Material 3 colors (SurfaceContainer + OnSurface)
      val surfaceColor =
          com.google.android.material.color.MaterialColors.getColor(
              context,
              com.google.android.material.R.attr.colorSurfaceContainerHigh,
              0xFFF5F5F5.toInt(),
          )

      val textColor =
          com.google.android.material.color.MaterialColors.getColor(
              context,
              com.google.android.material.R.attr.colorOnSurface,
              0xFF212121.toInt(),
          )

      val cardView =
          com.google.android.material.card.MaterialCardView(context).apply {
            radius = context.resources.displayMetrics.density * 20f
            cardElevation = context.resources.displayMetrics.density * 6f
            setCardBackgroundColor(surfaceColor)
            setContentPadding(dpToPx(15), dpToPx(15), dpToPx(15), dpToPx(15))
            strokeWidth = dpToPx(1)
            strokeColor =
                com.google.android.material.color.MaterialColors.getColor(
                    context,
                    com.google.android.material.R.attr.colorOutlineVariant,
                    0x22000000,
                )
          }

      val textView =
          TextView(context).apply {
            text = applySyntaxHighlighting(formatContent(content))
            textSize = 10f
            setTextColor(textColor)
            typeface = android.graphics.Typeface.MONOSPACE
            maxLines = 15
            ellipsize = android.text.TextUtils.TruncateAt.END
          }

      cardView.addView(textView)

      val maxWidth = editor.width * 9 / 10
      cardView.measure(
          View.MeasureSpec.makeMeasureSpec(maxWidth, View.MeasureSpec.AT_MOST),
          View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
      )

      // Get cursor position
      val cursor = editor.cursor
      val line = cursor.leftLine
      val column = cursor.leftColumn

      // Get the Y position of the cursor line (top of the line in editor coordinates)
      val lineTopInEditor = editor.getRowTop(line)

      // Get X position of the character
      val charX = editor.getCharOffsetX(line, column)

      // Convert to screen coordinates by subtracting scroll offsets
      val cursorScreenY = lineTopInEditor - editor.scrollY
      val cursorScreenX = charX - editor.scrollX

      // Tooltip dimensions
      val tooltipHeight = cardView.measuredHeight
      val tooltipWidth = cardView.measuredWidth

      // Position tooltip above cursor with spacing
      val verticalSpacing = dpToPx(8)
      val top = (cursorScreenY - tooltipHeight - verticalSpacing).coerceAtLeast(dpToPx(4))

      // Center tooltip horizontally above cursor, but keep within screen bounds
      val centerX = cursorScreenX - (tooltipWidth / 2)
      val left =
          centerX
              .coerceIn(dpToPx(4).toFloat(), (editor.width - tooltipWidth - dpToPx(4)).toFloat())
              .toInt()

      val layoutParams =
          FrameLayout.LayoutParams(
                  FrameLayout.LayoutParams.WRAP_CONTENT,
                  FrameLayout.LayoutParams.WRAP_CONTENT,
              )
              .apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = left
                topMargin = top
              }

      tooltipContainer.addView(cardView, layoutParams)
      tooltipView = cardView
    } catch (e: Exception) {
      log.error("Failed to display tooltip", e)
    }
  }

  private fun applySyntaxHighlighting(text: String): CharSequence {
    val builder = android.text.SpannableStringBuilder(text)

    val keywordColor =
        com.google.android.material.color.MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorPrimaryContainer,
            0xFF6200EE.toInt(),
        )
    val typeColor =
        com.google.android.material.color.MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorTertiaryContainer,
            0xFF018786.toInt(),
        )
    val stringColor =
        com.google.android.material.color.MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorSecondaryContainer,
            0xFF00897B.toInt(),
        )
    val commentColor =
        com.google.android.material.color.MaterialColors.getColor(
            context,
            com.google.android.material.R.attr.colorOutlineVariant,
            0xFF757575.toInt(),
        )
    val keywordPattern =
        Regex("\\b(fun|val|var|class|interface|return|if|else|for|while|when|in|is|as)\\b")
    val typePattern = Regex("\\b([A-Z][A-Za-z0-9_]*)\\b")
    val stringPattern = Regex("\".*?\"")
    val commentPattern =
        Regex("//.*?$|/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.MULTILINE))

    fun applyColor(pattern: Regex, color: Int) {
      pattern.findAll(text).forEach {
        builder.setSpan(
            android.text.style.ForegroundColorSpan(color),
            it.range.first,
            it.range.last + 1,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
    }

    applyColor(keywordPattern, keywordColor)
    applyColor(typePattern, typeColor)
    applyColor(stringPattern, stringColor)
    applyColor(commentPattern, commentColor)

    return builder
  }

  private fun isDarkTheme(): Boolean {
    val nightModeFlags =
        context.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK
    return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
  }

  private fun dismissTooltip() {
    tooltipView?.let { view ->
      try {
        tooltipContainer.removeView(view)
      } catch (e: Exception) {
        log.debug("Error removing tooltip view", e)
      }
      tooltipView = null
    }
  }

  private fun formatContent(text: String): String {
    return text.replace(Regex("```[a-z]*\\n"), "").replace("```", "").trim().take(1000)
  }

  private fun dpToPx(dp: Int): Int {
    return (dp * context.resources.displayMetrics.density).toInt()
  }

  fun destroy() {
    cancelHover()
    scope.cancel()
  }
}

// Extension functions
private const val HOVER_TOOLTIP_TAG = 0x7F0A0002

fun IDEEditor.initHoverTooltips() {
  val tooltipManager = HoverTooltipManager(context, this)
  tooltipManager.init()
  setTag(HOVER_TOOLTIP_TAG, tooltipManager)
}

fun IDEEditor.cleanupHoverTooltips() {
  val tooltipManager = getTag(HOVER_TOOLTIP_TAG) as? HoverTooltipManager
  tooltipManager?.destroy()
  setTag(HOVER_TOOLTIP_TAG, null)
}
