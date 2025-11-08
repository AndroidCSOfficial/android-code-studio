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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 *
 * Renders live streaming AI responses with proper code boxes
 */
class LiveResponseRenderer(
    private val context: Context,
    private val containerLayout: LinearLayout,
) {

  companion object {
    private val log = LoggerFactory.getLogger(LiveResponseRenderer::class.java)
    private const val CODE_BLOCK_MARKER = "```"
  }

  private val responseBuffer = StringBuilder()
  private var currentTextView: MaterialTextView? = null
  private var currentCodeBox: CodeBoxView? = null
  private var isInCodeBlock = false
  private var codeBlockLanguage = ""

  /** Append streaming chunk and render live */
  fun appendChunk(chunk: String) {
    responseBuffer.append(chunk)
    parseAndRenderLive()
  }

  /** Parse buffer and render content live as it streams */
  private fun parseAndRenderLive() {
    ThreadUtils.runOnUiThread {
      try {
        val content = responseBuffer.toString()
        val lines = content.lines()

        // Clear only if starting fresh
        if (containerLayout.childCount == 0) {
          ensureTextView()
        }

        var i = 0
        var currentText = StringBuilder()

        while (i < lines.size) {
          val line = lines[i]

          when {
            // Start of code block
            line.trim().startsWith(CODE_BLOCK_MARKER) && !isInCodeBlock -> {
              // Flush any pending text
              if (currentText.isNotEmpty()) {
                appendToTextView(currentText.toString())
                currentText.clear()
              }

              isInCodeBlock = true
              codeBlockLanguage = line.trim().substring(3).trim()

              // Create new code box
              currentCodeBox = createCodeBox(codeBlockLanguage)
              currentTextView = null
            }

            // End of code block
            line.trim().startsWith(CODE_BLOCK_MARKER) && isInCodeBlock -> {
              isInCodeBlock = false
              currentCodeBox?.finalize()
              currentCodeBox = null
              codeBlockLanguage = ""
            }

            // Inside code block
            isInCodeBlock -> {
              currentCodeBox?.appendLine(line)
            }

            // Regular text
            else -> {
              currentText.append(line).append("\n")
            }
          }

          i++
        }

        // Flush remaining text
        if (currentText.isNotEmpty() && !isInCodeBlock) {
          appendToTextView(currentText.toString())
        }

        // Update current code box if still streaming into it
        currentCodeBox?.render()
      } catch (e: Exception) {
        log.error("Error rendering live response", e)
      }
    }
  }

  /** Ensure we have a text view for regular content */
  private fun ensureTextView() {
    if (currentTextView == null) {
      currentTextView =
          MaterialTextView(context).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
          }
      containerLayout.addView(currentTextView)
    }
  }

  /** Append text to current text view */
  private fun appendToTextView(text: String) {
    ensureTextView()
    currentTextView?.text = text.trim()
  }

  /** Create a new code box view */
  private fun createCodeBox(language: String): CodeBoxView {
    val codeBox = CodeBoxView(context, language)
    containerLayout.addView(codeBox.getRootView())
    currentTextView = null
    return codeBox
  }

  /** Clear all content */
  fun clear() {
    ThreadUtils.runOnUiThread {
      responseBuffer.clear()
      containerLayout.removeAllViews()
      currentTextView = null
      currentCodeBox = null
      isInCodeBlock = false
      codeBlockLanguage = ""
    }
  }

  /** Finalize rendering */
  fun finalize() {
    ThreadUtils.runOnUiThread {
      currentCodeBox?.finalize()
      log.info("Live response finalized")
    }
  }

  /** Get complete response text */
  fun getResponseText(): String = responseBuffer.toString()

  /** Inner class for code box with syntax highlighting and copy button */
  private inner class CodeBoxView(private val context: Context, private val language: String) {
    private val codeContent = StringBuilder()
    private val rootCard: MaterialCardView
    private val headerLayout: LinearLayout
    private val languageLabel: MaterialTextView
    private val copyButton: MaterialButton
    private val codeTextView: MaterialTextView

    init {
      // Create card container
      rootCard =
          MaterialCardView(context).apply {
            layoutParams =
                LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    )
                    .apply { setMargins(0, 8, 0, 8) }
            cardElevation = 4f
            radius = 8f
            setCardBackgroundColor(Color.parseColor("#1E1E1E"))
          }

      val mainLayout =
          LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
          }

      // Header with language and copy button
      headerLayout =
          LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            setPadding(12, 8, 12, 8)
            setBackgroundColor(Color.parseColor("#2D2D2D"))
            gravity = Gravity.CENTER_VERTICAL
          }

      languageLabel =
          MaterialTextView(context).apply {
            text = language.ifEmpty { "code" }
            textSize = 12f
            setTextColor(Color.parseColor("#9CDCFE"))
            typeface = Typeface.create("monospace", Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
          }

      copyButton =
          MaterialButton(context).apply {
            text = "Copy"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#007ACC"))
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
            setPadding(16, 4, 16, 4)
            setOnClickListener { copyCodeToClipboard() }
          }

      headerLayout.addView(languageLabel)
      headerLayout.addView(copyButton)

      // Code content
      codeTextView =
          MaterialTextView(context).apply {
            textSize = 13f
            typeface = Typeface.create("monospace", Typeface.NORMAL)
            setPadding(16, 16, 16, 16)
            setTextColor(Color.WHITE)
            setHorizontallyScrolling(true)
            layoutParams =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                )
          }

      mainLayout.addView(headerLayout)
      mainLayout.addView(codeTextView)
      rootCard.addView(mainLayout)
    }

    fun appendLine(line: String) {
      codeContent.append(line).append("\n")
    }

    fun render() {
      val highlighted = applySyntaxHighlighting(codeContent.toString(), language)
      codeTextView.text = highlighted
    }

    fun finalize() {
      render()
    }

    fun getRootView(): View = rootCard

    private fun copyCodeToClipboard() {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("code", codeContent.toString())
      clipboard.setPrimaryClip(clip)
      Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    /** Apply basic syntax highlighting based on language */
    private fun applySyntaxHighlighting(code: String, lang: String): SpannableStringBuilder {
      val spannable = SpannableStringBuilder(code)

      when (lang.lowercase()) {
        "kotlin",
        "kt" -> highlightKotlin(spannable)
        "java" -> highlightJava(spannable)
        "xml" -> highlightXml(spannable)
        "json" -> highlightJson(spannable)
        else -> {} // Plain text
      }

      return spannable
    }

    private fun highlightKotlin(spannable: SpannableStringBuilder) {
      val keywords =
          listOf(
              "package",
              "import",
              "class",
              "interface",
              "object",
              "fun",
              "val",
              "var",
              "if",
              "else",
              "when",
              "for",
              "while",
              "return",
              "try",
              "catch",
              "finally",
              "public",
              "private",
              "protected",
              "internal",
              "override",
              "open",
              "abstract",
              "companion",
              "data",
              "sealed",
              "enum",
              "annotation",
              "suspend",
              "inline",
          )

      highlightKeywords(spannable, keywords, Color.parseColor("#569CD6"))
      highlightStrings(spannable, Color.parseColor("#CE9178"))
      highlightComments(spannable, Color.parseColor("#6A9955"))
    }

    private fun highlightJava(spannable: SpannableStringBuilder) {
      val keywords =
          listOf(
              "package",
              "import",
              "class",
              "interface",
              "public",
              "private",
              "protected",
              "static",
              "final",
              "void",
              "return",
              "if",
              "else",
              "for",
              "while",
              "try",
              "catch",
              "finally",
              "new",
              "this",
              "super",
              "extends",
              "implements",
          )

      highlightKeywords(spannable, keywords, Color.parseColor("#569CD6"))
      highlightStrings(spannable, Color.parseColor("#CE9178"))
      highlightComments(spannable, Color.parseColor("#6A9955"))
    }

    private fun highlightXml(spannable: SpannableStringBuilder) {
      // Highlight tags
      val tagPattern = Regex("</?[a-zA-Z][^>]*>")
      tagPattern.findAll(spannable).forEach { match ->
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#569CD6")),
            match.range.first,
            match.range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
    }

    private fun highlightJson(spannable: SpannableStringBuilder) {
      // Highlight keys
      val keyPattern = Regex("\"[^\"]+\"\\s*:")
      keyPattern.findAll(spannable).forEach { match ->
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#9CDCFE")),
            match.range.first,
            match.range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }

      highlightStrings(spannable, Color.parseColor("#CE9178"))
    }

    private fun highlightKeywords(
        spannable: SpannableStringBuilder,
        keywords: List<String>,
        color: Int,
    ) {
      val text = spannable.toString()
      keywords.forEach { keyword ->
        val pattern = Regex("\\b$keyword\\b")
        pattern.findAll(text).forEach { match ->
          spannable.setSpan(
              ForegroundColorSpan(color),
              match.range.first,
              match.range.last + 1,
              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
          spannable.setSpan(
              StyleSpan(Typeface.BOLD),
              match.range.first,
              match.range.last + 1,
              Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
          )
        }
      }
    }

    private fun highlightStrings(spannable: SpannableStringBuilder, color: Int) {
      val stringPattern = Regex("\"([^\"\\\\]|\\\\.)*\"")
      stringPattern.findAll(spannable).forEach { match ->
        spannable.setSpan(
            ForegroundColorSpan(color),
            match.range.first,
            match.range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
    }

    private fun highlightComments(spannable: SpannableStringBuilder, color: Int) {
      // Single line comments
      val singleLinePattern = Regex("//.*$", RegexOption.MULTILINE)
      singleLinePattern.findAll(spannable).forEach { match ->
        spannable.setSpan(
            ForegroundColorSpan(color),
            match.range.first,
            match.range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }

      // Multi-line comments
      val multiLinePattern = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
      multiLinePattern.findAll(spannable).forEach { match ->
        spannable.setSpan(
            ForegroundColorSpan(color),
            match.range.first,
            match.range.last + 1,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
      }
    }
  }
}
