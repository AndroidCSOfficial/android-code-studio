/*
 * String extraction helper for AndroidIDE
 * Detects when user finishes typing string literals and offers extraction
 */

package com.tom.rv2ide.utils

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.tom.rv2ide.ui.CodeEditorView
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

class StringExtractionHelper(private val context: Context, private val scope: CoroutineScope) {

  companion object {
    private val log = LoggerFactory.getLogger(StringExtractionHelper::class.java)

    // Regex patterns for different file types
    private val JAVA_KOTLIN_STRING_PATTERN =
        Pattern.compile(""""([^"\\]*(\\.[^"\\]*)*)"""", Pattern.MULTILINE)

    // XML string pattern - matches quoted strings in XML attributes and text content
    private val XML_STRING_PATTERN =
        Pattern.compile(""""([^"\\]*(\\.[^"\\]*)*)"|'([^'\\]*(\\.[^'\\]*)*)'""", Pattern.MULTILINE)

    private const val MIN_STRING_LENGTH = 4
    private const val MAX_STRING_LENGTH = 100
  }

  // Track cursor positions and string locations
  private val editorLastCursorPos =
      ConcurrentHashMap<CodeEditorView, Pair<Int, Int>>() // line, column
  private val editorPendingStrings = ConcurrentHashMap<CodeEditorView, MutableSet<String>>()
  private var currentPopup: PopupWindow? = null

  /** Check for completed string literals (when cursor moves away from them) */
  fun checkForNewStringLiterals(editors: List<CodeEditorView>) {
    log.debug(
        "StringExtractionHelper.checkForNewStringLiterals called with ${editors.size} editors"
    )

    try {
      for (editor in editors) {
        val file = editor.file
        log.debug("Processing editor for file: ${file?.absolutePath ?: "null"}")

        if (file == null || !isValidFileType(file)) {
          log.debug("Skipping file - invalid or null")
          continue
        }

        val ideEditor = editor.editor ?: continue
        val currentContent = ideEditor.text?.toString() ?: ""

        if (currentContent.isEmpty()) {
          log.debug("Skipping file - empty content")
          continue
        }

        // Get current cursor position
        val cursor = ideEditor.cursor
        val currentCursorPos = Pair(cursor.leftLine, cursor.leftColumn)
        val lastCursorPos = editorLastCursorPos[editor]

        log.debug("Current cursor: ${currentCursorPos}, Last cursor: ${lastCursorPos}")

        // Check if cursor moved (indicating user finished typing something)
        if (lastCursorPos != null && lastCursorPos != currentCursorPos) {
          log.debug("Cursor moved, checking for completed strings")

          // Find string at the previous cursor position
          val completedString =
              findStringNearPosition(
                  currentContent,
                  lastCursorPos.first,
                  lastCursorPos.second,
                  file,
              )

          if (completedString != null) {
            log.debug("Found completed string: '$completedString'")

            val pendingStrings = editorPendingStrings.getOrPut(editor) { mutableSetOf() }

            if (
                !pendingStrings.contains(completedString) &&
                    isExtractableString(completedString, file)
            ) {
              log.debug("String is new and extractable, showing popup")
              pendingStrings.add(completedString)
              showExtractionPopup(completedString, editor, file)
              editorLastCursorPos[editor] = currentCursorPos
              return // Only process one at a time
            }
          }
        }

        // Update cursor position
        editorLastCursorPos[editor] = currentCursorPos
      }

      log.debug("No new extractable strings found")
    } catch (e: Exception) {
      log.error("Error checking for string literals", e)
    }
  }

  /** Find string literal near a specific position in the text */
  private fun findStringNearPosition(content: String, line: Int, column: Int, file: File): String? {
    try {
      val lines = content.split('\n')
      if (line >= lines.size) return null

      val currentLine = lines[line]
      log.debug("Checking line $line: '$currentLine'")

      // Choose pattern based on file type
      val pattern =
          if (file.extension.lowercase() == "xml") {
            XML_STRING_PATTERN
          } else {
            JAVA_KOTLIN_STRING_PATTERN
          }

      // Look for strings in the current line and nearby lines
      for (lineOffset in -1..1) {
        val checkLine = line + lineOffset
        if (checkLine < 0 || checkLine >= lines.size) continue

        val lineContent = lines[checkLine]
        val matcher = pattern.matcher(lineContent)

        while (matcher.find()) {
          // For XML pattern, we need to check both group 1 (double quotes) and group 3 (single
          // quotes)
          val stringContent =
              if (file.extension.lowercase() == "xml") {
                matcher.group(1) ?: matcher.group(3) ?: continue
              } else {
                matcher.group(1) ?: continue
              }

          val startPos = matcher.start()
          val endPos = matcher.end()

          log.debug(
              "Found string '$stringContent' at positions $startPos-$endPos in line $checkLine"
          )

          // Check if this string is near the cursor position
          if (lineOffset == 0) {
            // Same line - check if cursor was inside or just after the string
            if (column >= startPos && column <= endPos + 2) { // +2 for some tolerance
              log.debug("String '$stringContent' is near cursor position")
              return stringContent
            }
          } else {
            // Adjacent line - return first extractable string found
            if (isExtractableString(stringContent, file)) {
              log.debug("Found extractable string '$stringContent' in adjacent line")
              return stringContent
            }
          }
        }
      }
    } catch (e: Exception) {
      log.error("Error finding string near position", e)
    }

    return null
  }

  private fun isValidFileType(file: File): Boolean {
    val extension = file.extension.lowercase()
    val name = file.name.lowercase()
    val path = file.absolutePath.lowercase()

    val isValid =
        when (extension) {
          "kt" -> !name.endsWith(".gradle.kts")
          "java" -> !name.contains("gradle")
          "xml" -> {
            // Only include XML files that are likely to contain user-facing strings
            path.contains("/res/layout/") ||
                path.contains("/res/menu/") ||
                path.contains("/res/xml/") ||
                path.contains("\\res\\layout\\") ||
                path.contains("\\res\\menu\\") ||
                path.contains("\\res\\xml\\") ||
                // Also include if it's in res folder but not values (to avoid strings.xml itself)
                (path.contains("/res/") && !path.contains("/values/")) ||
                (path.contains("\\res\\") && !path.contains("\\values\\"))
          }
          else -> false
        }

    log.debug("File ${file.name}: extension=$extension, path=$path, isValid=$isValid")
    return isValid
  }

  private fun isExtractableString(content: String, file: File): Boolean {
    val lengthOk = content.length >= MIN_STRING_LENGTH && content.length <= MAX_STRING_LENGTH
    val notEmpty = content.trim().isNotEmpty()
    val hasLetter = content.any { it.isLetter() }

    // Common exclusions for all file types
    val notUrl =
        !content.lowercase().startsWith("http") &&
            !content.lowercase().startsWith("ftp://") &&
            !content.lowercase().startsWith("file://")
    val notNumber =
        !content.matches(Regex("^\\d+(\\.\\d+)?[a-zA-Z%]*$")) // Numbers with optional units/percent
    val notFilePath = !content.contains("/") && !content.contains("\\") && !content.contains(":")
    val notPackageName =
        !content.matches(Regex("^[a-z]+\\.[a-z]+(\\.[a-z]+)+$")) // com.example.package format
    val notHexColor = !content.matches(Regex("^#[0-9a-fA-F]{3,8}$"))
    val notMimeType = !content.matches(Regex("^[a-z]+/[a-z+]+$"))

    // XML-specific exclusions
    val xmlSpecificChecks =
        if (file.extension.lowercase() == "xml") {
          val notAndroidAttribute =
              !content.matches(Regex("^@[a-zA-Z]+/[a-zA-Z_]+$")) // @drawable/icon, @color/primary
          val notDimension = !content.matches(Regex("^\\d+(dp|sp|px|dip|pt|in|mm)$")) // 16dp, 14sp
          val notResourceReference = !content.startsWith("@") // @string/, @drawable/, etc.
          val notXmlNamespace =
              !content.matches(Regex("^[a-z]+://[a-zA-Z0-9./]+$")) // xmlns declarations
          val notViewId =
              !content.matches(
                  Regex("^@\\+?id/[a-zA-Z_][a-zA-Z0-9_]*$")
              ) // @+id/button1, @id/text_view

          notAndroidAttribute &&
              notDimension &&
              notResourceReference &&
              notXmlNamespace &&
              notViewId
        } else {
          true
        }

    // Programming keywords (less restrictive for XML)
    val keywordPattern =
        if (file.extension.lowercase() == "xml") {
          // Only exclude XML-specific keywords for XML files
          Regex(
              "^(true|false|null||background|version|wrap_content|match_parent|fill_parent|vertical|horizontal|center|left|right|top|bottom|start|end)$"
          )
        } else {
          // Full programming keywords for code files
          Regex(
              "^(null|true|false|void|int|string|boolean|class|function|var|val|let|const|if|else|for|while|do|switch|case|break|continue|return|try|catch|finally|throw|new|this|super|extends|implements|public|private|protected|static|final|abstract|interface)$"
          )
        }
    val notKeyword = !content.lowercase().matches(keywordPattern)

    // JSON key check (more lenient for XML)
    val notJsonKey =
        if (file.extension.lowercase() == "xml") {
          true // Don't apply JSON key restriction to XML
        } else {
          !content.matches(Regex("^[a-z_]+$")) || content.contains(" ")
        }

    val isExtractable =
        lengthOk &&
            notEmpty &&
            hasLetter &&
            notUrl &&
            notNumber &&
            notFilePath &&
            notPackageName &&
            notJsonKey &&
            notHexColor &&
            notMimeType &&
            notKeyword &&
            xmlSpecificChecks

    log.debug(
        "String '$content' in ${file.extension} extractable: length=$lengthOk, notEmpty=$notEmpty, hasLetter=$hasLetter, notUrl=$notUrl, notNum=$notNumber, notPath=$notFilePath, notPkg=$notPackageName, notJsonKey=$notJsonKey, notHex=$notHexColor, notMime=$notMimeType, notKeyword=$notKeyword, xmlChecks=$xmlSpecificChecks -> $isExtractable"
    )
    return isExtractable
  }

  private fun showExtractionPopup(
      stringContent: String,
      editor: CodeEditorView,
      currentFile: File,
  ) {
    log.debug("Showing extraction popup for: '$stringContent'")

    scope.launch(Dispatchers.Main) {
      try {
        val ideEditor = editor.editor ?: return@launch

        // Dismiss existing popup before showing new one
        currentPopup?.dismiss()

        // Create popup content with checkbox
        val popupView =
            createPopupView(stringContent, currentFile) { action, translatable ->
              when (action) {
                "extract" -> {
                  currentPopup?.dismiss()
                  extractStringToResource(stringContent, editor, currentFile, translatable)
                }
                "skip" -> {
                  currentPopup?.dismiss()
                }
              }
            }

        // Create popup that persists until user action
        currentPopup =
            PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    false,
                )
                .apply {
                  elevation = 16f
                  isTouchable = true
                  isOutsideTouchable = false // Don't dismiss on outside touch
                  isFocusable = false
                  inputMethodMode = PopupWindow.INPUT_METHOD_NOT_NEEDED

                  // Multiple fallback approaches to show the popup
                  var shown = false

                  // Try 1: Activity content view
                  try {
                    val activity = context as? android.app.Activity
                    val contentView = activity?.findViewById<View>(android.R.id.content)
                    if (contentView != null) {
                      showAtLocation(contentView, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 50)
                      shown = true
                      log.debug("Popup shown using content view")
                    }
                  } catch (e: Exception) {
                    log.debug("Content view approach failed: ${e.message}")
                  }

                  // Try 2: Editor root view
                  if (!shown) {
                    try {
                      showAtLocation(
                          ideEditor.rootView,
                          Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                          0,
                          50,
                      )
                      shown = true
                      log.debug("Popup shown using editor root view")
                    } catch (e: Exception) {
                      log.debug("Editor root view approach failed: ${e.message}")
                    }
                  }

                  // Try 3: Direct editor view
                  if (!shown) {
                    try {
                      showAtLocation(ideEditor, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 50)
                      shown = true
                      log.debug("Popup shown using direct editor")
                    } catch (e: Exception) {
                      log.debug("Direct editor approach failed: ${e.message}")
                    }
                  }

                  if (!shown) {
                    log.error("All popup display methods failed!")
                  }
                }
      } catch (e: Exception) {
        log.error("Error showing extraction popup", e)
      }
    }
  }

  private fun createPopupView(
      stringContent: String,
      currentFile: File,
      onAction: (String, Boolean) -> Unit,
  ): View {
    // Get theme colors
    val typedValue = TypedValue()
    val theme = context.theme

    // Get surface color (background)
    theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
    val surfaceColor = ContextCompat.getColor(context, typedValue.resourceId)

    // Get primary text color
    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    val primaryTextColor = ContextCompat.getColor(context, typedValue.resourceId)

    // Get secondary text color
    theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
    val secondaryTextColor = ContextCompat.getColor(context, typedValue.resourceId)

    // Get accent/primary color for buttons
    theme.resolveAttribute(
        com.google.android.material.R.attr.colorPrimaryContainer,
        typedValue,
        true,
    )
    val accentColor = ContextCompat.getColor(context, typedValue.resourceId)

    val container =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(32, 24, 32, 24) // More padding for better appearance

          // Create highly rounded background
          val background =
              GradientDrawable().apply {
                setColor(surfaceColor)
                cornerRadius = 30f // Much more rounded
                // Add subtle shadow effect with stroke
                setStroke(1, secondaryTextColor and 0x30FFFFFF) // Semi-transparent border
              }
          setBackground(background)
        }

    // Title
    val titleText =
        TextView(context).apply {
          val fileType =
              when (currentFile.extension.lowercase()) {
                "xml" -> "XML Layout"
                "kt" -> "Kotlin"
                "java" -> "Java"
                else -> "File"
              }
          text = "Extract String from $fileType?"
          textSize = 16f // Slightly larger
          setTextColor(primaryTextColor)
          setPadding(0, 0, 0, 16)
          // Make title bold
          setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
    container.addView(titleText)

    // String preview
    val stringPreview =
        TextView(context).apply {
          text = "\"${stringContent.take(50)}${if (stringContent.length > 50) "..." else ""}\""
          textSize = 13f
          setTextColor(secondaryTextColor)
          setPadding(8, 8, 8, 8)

          // Add subtle background for the string preview
          val previewBackground =
              GradientDrawable().apply {
                setColor(secondaryTextColor and 0x10FFFFFF) // Very light background
                cornerRadius = 20f
              }
          setBackground(previewBackground)
        }
    container.addView(stringPreview)

    // Show what the replacement will look like
    val replacementPreview =
        TextView(context).apply {
          val replacement =
              when (currentFile.extension.lowercase()) {
                "xml" -> "✨ XML detected"
                "kt" -> "⚡️ KOTLIN detected"
                "java" -> "☕ JAVA detected"
                else -> "R.string.example_name"
              }
          text = "$replacement"
          textSize = 12f
          setTextColor(accentColor)
          setPadding(8, 8, 8, 12)
        }
    container.addView(replacementPreview)

    // Translatable checkbox
    val translatableCheckbox =
        CheckBox(context).apply {
          text = "Translatable"
          isChecked = true // Default to true
          setTextColor(primaryTextColor)
          setPadding(8, 4, 8, 16)
          textSize = 14f

          // Style the checkbox
          try {
            // Try to set the button tint to match theme
            val colorStateList = android.content.res.ColorStateList.valueOf(accentColor)
            buttonTintList = colorStateList
          } catch (e: Exception) {
            log.debug("Could not set checkbox tint: ${e.message}")
          }
        }
    container.addView(translatableCheckbox)

    // Buttons container
    val buttonContainer =
        LinearLayout(context).apply {
          orientation = LinearLayout.HORIZONTAL
          gravity = Gravity.END // Align buttons to the right
        }

    // Skip button (secondary action, shown first but less prominent)
    val skipButton =
        Button(context).apply {
          text = "Skip"
          textSize = 14f
          setPadding(32, 16, 32, 16)
          setTextColor(secondaryTextColor)

          val buttonBackground =
              GradientDrawable().apply {
                setColor(android.graphics.Color.TRANSPARENT)
                cornerRadius = 60f // Rounded button
                setStroke(2, secondaryTextColor and 0x50FFFFFF) // Semi-transparent border
              }
          setBackground(buttonBackground)

          setOnClickListener {
            onAction("skip", false)
          } // Translatable value doesn't matter for skip

          // Add margin
          val params =
              LinearLayout.LayoutParams(
                  ViewGroup.LayoutParams.WRAP_CONTENT,
                  ViewGroup.LayoutParams.WRAP_CONTENT,
              )
          params.setMargins(0, 0, 16, 0) // Margin to the right
          layoutParams = params
        }

    // Extract button (primary action)
    val extractButton =
        Button(context).apply {
          text = "Extract"
          textSize = 14f
          setPadding(32, 16, 32, 16)
          setTextColor(android.graphics.Color.WHITE) // White text on colored background

          val buttonBackground =
              GradientDrawable().apply {
                setColor(accentColor) // Use theme accent color
                cornerRadius = 60f // Rounded button
              }
          setBackground(buttonBackground)

          setOnClickListener { onAction("extract", translatableCheckbox.isChecked) }
        }

    buttonContainer.addView(skipButton)
    buttonContainer.addView(extractButton)
    container.addView(buttonContainer)

    return container
  }

  private fun extractStringToResource(
      stringContent: String,
      editor: CodeEditorView,
      currentFile: File,
      translatable: Boolean = true,
  ) {
    scope.launch {
      try {
        val stringName = sanitizeStringName(stringContent)
        val stringsXmlFile = findStringsXmlFile(currentFile)

        if (stringsXmlFile == null) {
          withContext(Dispatchers.Main) {
            showToast("Could not find or create strings.xml file in project")
          }
          return@launch
        }

        // Add to strings.xml with translatable parameter
        val finalStringName =
            addStringToXml(stringsXmlFile, stringName, stringContent, translatable)

        if (finalStringName != null) {
          // Replace in current file
          withContext(Dispatchers.Main) {
            replaceStringInEditor(stringContent, finalStringName, editor)
          }
        } else {
          withContext(Dispatchers.Main) { showToast("Failed to add string to strings.xml") }
        }
      } catch (e: Exception) {
        log.error("Error extracting string to resource", e)
        withContext(Dispatchers.Main) { showToast("Error: ${e.message}") }
      }
    }
  }

  private fun sanitizeStringName(content: String): String {
    return content
        .lowercase()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .trim()
        .replace(Regex("\\s+"), "_")
        .take(30)
        .let { if (it.isEmpty()) "extracted_string" else it }
  }

  private fun findStringsXmlFile(currentFile: File): File? {
    var dir = currentFile.parentFile

    // Navigate up to find project root
    while (
        dir != null &&
            !File(dir, "build.gradle").exists() &&
            !File(dir, "build.gradle.kts").exists() &&
            !File(dir, "settings.gradle").exists()
    ) {
      dir = dir.parentFile
    }

    if (dir == null) return null

    val appDir = File(dir, "app")
    val baseDir = if (appDir.exists()) appDir else dir

    val stringsFile = File(baseDir, "src/main/res/values/strings.xml")

    stringsFile.parentFile?.mkdirs()

    if (!stringsFile.exists()) {
      stringsFile.writeText(
          """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">My App</string>
</resources>"""
      )
    }

    return stringsFile
  }

  private suspend fun addStringToXml(
      stringsFile: File,
      stringName: String,
      stringContent: String,
      translatable: Boolean = true,
  ): String? {
    return withContext(Dispatchers.IO) {
      try {
        val content = stringsFile.readText()

        var finalName = stringName
        var counter = 1
        while (content.contains("""name="$finalName"""")) {
          finalName = "${stringName}_$counter"
          counter++
        }

        val escapedContent =
            stringContent
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'")

        val translatableAttr =
            if (translatable) "translatable=\"true\"" else "translatable=\"false\""
        val newStringEntry =
            """    <string name="$finalName" $translatableAttr>$escapedContent</string>"""

        val updatedContent =
            if (content.contains("</resources>")) {
              content.replace("</resources>", "$newStringEntry\n</resources>")
            } else {
              content + "\n$newStringEntry\n</resources>"
            }

        stringsFile.writeText(updatedContent)
        log.debug("Added string to XML: $finalName = $stringContent (translatable=$translatable)")
        finalName
      } catch (e: Exception) {
        log.error("Error adding string to XML", e)
        null
      }
    }
  }

  private fun replaceStringInEditor(
      originalString: String,
      stringName: String,
      editor: CodeEditorView,
  ) {
    val ideEditor = editor.editor ?: return
    val editorText = ideEditor.text
    val fullText = editorText.toString()

    val file = editor.file ?: return
    val replacement =
        when (file.extension.lowercase()) {
          "xml" -> "\"@string/$stringName\"" // XML files need quotes around the reference
          "kt" -> "getString(R.string.$stringName)"
          "java" -> "getString(R.string.$stringName)"
          else -> "R.string.$stringName"
        }

    // For XML files, we need to handle both single and double quoted strings
    val quotedStrings =
        if (file.extension.lowercase() == "xml") {
          listOf("\"$originalString\"", "'$originalString'")
        } else {
          listOf("\"$originalString\"")
        }

    var replaced = false
    for (quotedString in quotedStrings) {
      val lastIndex = fullText.lastIndexOf(quotedString)

      if (lastIndex >= 0) {
        try {
          val beforeText = fullText.substring(0, lastIndex)
          val lines = beforeText.split('\n')
          val startLine = lines.size - 1
          val startColumn = lines.last().length

          val afterText = fullText.substring(0, lastIndex + quotedString.length)
          val endLines = afterText.split('\n')
          val endLine = endLines.size - 1
          val endColumn = endLines.last().length

          editorText.replace(startLine, startColumn, endLine, endColumn, replacement)

          replaced = true
          val fileType =
              when (file.extension.lowercase()) {
                "xml" -> "XML"
                else -> file.extension.uppercase()
              }
          showToast("String extracted to R.string.$stringName in $fileType")
          log.debug("Replaced '$quotedString' with '$replacement' in editor")
          break
        } catch (e: Exception) {
          log.error("Error replacing text in editor", e)
        }
      }
    }

    if (!replaced) {
      log.warn("Could not find string '$originalString' in editor content")
      showToast("Failed to replace text in editor")
    }
  }

  private fun showToast(message: String) {
    // Get theme colors for toast
    val typedValue = TypedValue()
    val theme = context.theme

    theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)
    val surfaceColor = ContextCompat.getColor(context, typedValue.resourceId)

    theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
    val textColor = ContextCompat.getColor(context, typedValue.resourceId)

    // Create a rounded toast popup
    val popupView =
        LinearLayout(context).apply {
          orientation = LinearLayout.VERTICAL
          setPadding(32, 20, 32, 20)

          val background =
              GradientDrawable().apply {
                setColor(surfaceColor)
                cornerRadius = 20f // Very rounded toast
                // Add shadow effect
                setStroke(1, textColor and 0x20FFFFFF)
              }
          setBackground(background)

          val textView =
              TextView(context).apply {
                text = message
                textSize = 14f
                setTextColor(textColor)
                gravity = Gravity.CENTER
              }
          addView(textView)
        }

    val popup =
        PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                false,
            )
            .apply {
              elevation = 12f

              try {
                showAtLocation(
                    (context as? android.app.Activity)?.findViewById(android.R.id.content),
                    Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
                    0,
                    100, // Show at bottom
                )
              } catch (e: Exception) {
                log.debug("Could not show toast popup: ${e.message}")
              }

              popupView.postDelayed(
                  { if (isShowing) dismiss() },
                  3500,
              ) // Slightly longer display time
            }
  }

  /** Initialize for an editor */
  fun initializeForEditor(editor: CodeEditorView) {
    val file = editor.file ?: return
    if (isValidFileType(file)) {
      try {
        val ideEditor = editor.editor ?: return
        val cursor = ideEditor.cursor
        editorLastCursorPos[editor] = Pair(cursor.leftLine, cursor.leftColumn)
        editorPendingStrings[editor] = mutableSetOf()
        log.debug("Initialized string extraction for file: ${file.absolutePath}")
      } catch (e: Exception) {
        log.error("Error initializing string extraction for editor", e)
      }
    }
  }

  /** Clean up for an editor */
  fun cleanupForEditor(editor: CodeEditorView) {
    editorLastCursorPos.remove(editor)
    editorPendingStrings.remove(editor)
    currentPopup?.dismiss()
    log.debug("Cleaned up string extraction for file: ${editor.file?.absolutePath}")
  }

  /** Clear all data */
  fun clear() {
    editorLastCursorPos.clear()
    editorPendingStrings.clear()
    currentPopup?.dismiss()
    currentPopup = null
  }
}
