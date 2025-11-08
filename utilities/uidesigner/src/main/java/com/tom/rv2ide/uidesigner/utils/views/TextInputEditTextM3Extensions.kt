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

package com.tom.rv2ide.uidesigner.utils.views

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.view.Gravity
import com.google.android.material.textfield.TextInputEditText
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TextInputEditTextM3Extensions")

/**
 * TextInputEditText M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun TextInputEditText.applyM3Preview(
    attributeName: String,
    attributeValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  val value = attributeValue.trim()
  if (value.isEmpty()) return false

  val normalizedAttrName = attributeName.lowercase().replace("app:", "").replace("android:", "")

  return try {
    when (normalizedAttrName) {
      "text" -> applyTextM3(value)
      "hint" -> applyHintM3(value)
      "textcolor" -> applyTextColorM3(value, context)
      "texthintcolor" -> applyTextHintColorM3(value, context)
      "textsize" -> applyTextSizeM3(value, context)
      "textstyle" -> applyTextStyleM3(value)
      "gravity" -> applyGravityM3(value)
      "inputtype" -> applyInputTypeM3(value)
      "maxlength" -> applyMaxLengthM3(value)
      "maxlines" -> applyMaxLinesM3(value)
      "minlines" -> applyMinLinesM3(value)
      "lines" -> applyLinesM3(value)
      "singleline" -> applySingleLineM3(value)
      "enabled" -> applyEnabledM3(value)
      "editable" -> applyEditableM3(value)
      "cursorvisible" -> applyCursorVisibleM3(value)
      "selectallonfocus" -> applySelectAllOnFocusM3(value)
      "imeactionlabel" -> applyImeActionLabelM3(value)
      "imeoptions" -> applyImeOptionsM3(value)
      "drawablestart" -> applyDrawableStartM3(value, context, workspace, layoutFile)
      "drawableend" -> applyDrawableEndM3(value, context, workspace, layoutFile)
      "drawabletop" -> applyDrawableTopM3(value, context, workspace, layoutFile)
      "drawablebottom" -> applyDrawableBottomM3(value, context, workspace, layoutFile)
      "drawabletint" -> applyDrawableTintM3(value, context)
      "drawablepadding" -> applyDrawablePaddingM3(value, context)
      else -> {
        log.debug("Unsupported TextInputEditText attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply TextInputEditText M3 attribute: $normalizedAttrName", e)
    false
  }
}

// Basic text attributes
private fun TextInputEditText.applyTextM3(value: String): Boolean {
  return try {
    setText(value)
    true
  } catch (e: Exception) {
    log.error("Failed to apply text", e)
    false
  }
}

private fun TextInputEditText.applyHintM3(value: String): Boolean {
  return try {
    hint = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply hint", e)
    false
  }
}

// Color attributes
private fun TextInputEditText.applyTextColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setTextColor(color)
    true
  } catch (e: Exception) {
    log.error("Failed to apply text color", e)
    false
  }
}

private fun TextInputEditText.applyTextHintColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setHintTextColor(color)
    true
  } catch (e: Exception) {
    log.error("Failed to apply text hint color", e)
    false
  }
}

// Text size and style
private fun TextInputEditText.applyTextSizeM3(value: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(value, context)
    if (size > 0) {
      textSize = size.toFloat() / context.resources.displayMetrics.scaledDensity
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply text size", e)
    false
  }
}

private fun TextInputEditText.applyTextStyleM3(value: String): Boolean {
  return try {
    val style =
        when (value.lowercase()) {
          "bold" -> Typeface.BOLD
          "italic" -> Typeface.ITALIC
          "bold_italic",
          "bolditalic" -> Typeface.BOLD_ITALIC
          "normal" -> Typeface.NORMAL
          else -> return false
        }
    setTypeface(typeface, style)
    true
  } catch (e: Exception) {
    log.error("Failed to apply text style", e)
    false
  }
}

// Gravity
private fun TextInputEditText.applyGravityM3(value: String): Boolean {
  return try {
    val gravityValue = parseGravity(value)
    if (gravityValue != 0) {
      gravity = gravityValue
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply gravity", e)
    false
  }
}

private fun parseGravity(value: String): Int {
  var gravity = 0
  val parts = value.split("|")

  for (part in parts) {
    gravity =
        gravity or
            when (part.trim().lowercase()) {
              "top" -> Gravity.TOP
              "bottom" -> Gravity.BOTTOM
              "left" -> Gravity.LEFT
              "right" -> Gravity.RIGHT
              "center" -> Gravity.CENTER
              "center_vertical" -> Gravity.CENTER_VERTICAL
              "center_horizontal" -> Gravity.CENTER_HORIZONTAL
              "start" -> Gravity.START
              "end" -> Gravity.END
              "fill" -> Gravity.FILL
              "fill_vertical" -> Gravity.FILL_VERTICAL
              "fill_horizontal" -> Gravity.FILL_HORIZONTAL
              "clip_vertical" -> Gravity.CLIP_VERTICAL
              "clip_horizontal" -> Gravity.CLIP_HORIZONTAL
              else -> 0
            }
  }

  return gravity
}

// Input type
private fun TextInputEditText.applyInputTypeM3(value: String): Boolean {
  return try {
    val inputTypeValue = parseInputType(value)
    if (inputTypeValue != 0) {
      inputType = inputTypeValue
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply input type", e)
    false
  }
}

private fun parseInputType(value: String): Int {
  var inputType = 0
  val parts = value.split("|")

  for (part in parts) {
    inputType =
        inputType or
            when (part.trim().lowercase()) {
              "none" -> InputType.TYPE_NULL
              "text" -> InputType.TYPE_CLASS_TEXT
              "textcapcharacters" -> InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
              "textcapwords" -> InputType.TYPE_TEXT_FLAG_CAP_WORDS
              "textcapsentences" -> InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
              "textautocorrect" -> InputType.TYPE_TEXT_FLAG_AUTO_CORRECT
              "textautocomplete" -> InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE
              "textmultiline" -> InputType.TYPE_TEXT_FLAG_MULTI_LINE
              "textimeMultiLine" -> InputType.TYPE_TEXT_FLAG_IME_MULTI_LINE
              "textnosuggestions" -> InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
              "textUri" -> InputType.TYPE_TEXT_VARIATION_URI
              "textemailaddress" -> InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
              "textemailsubject" -> InputType.TYPE_TEXT_VARIATION_EMAIL_SUBJECT
              "textshortmessage" -> InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
              "textlongmessage" -> InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE
              "textpersonname" -> InputType.TYPE_TEXT_VARIATION_PERSON_NAME
              "textpostaladdress" -> InputType.TYPE_TEXT_VARIATION_POSTAL_ADDRESS
              "textpassword" -> InputType.TYPE_TEXT_VARIATION_PASSWORD
              "textvisiblepassword" -> InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
              "textwebemailaddress" -> InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS
              "textfilter" -> InputType.TYPE_TEXT_VARIATION_FILTER
              "textphonetic" -> InputType.TYPE_TEXT_VARIATION_PHONETIC
              "textwebedittext" -> InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
              "number" -> InputType.TYPE_CLASS_NUMBER
              "numbersigned" -> InputType.TYPE_NUMBER_FLAG_SIGNED
              "numberdecimal" -> InputType.TYPE_NUMBER_FLAG_DECIMAL
              "numberpassword" -> InputType.TYPE_NUMBER_VARIATION_PASSWORD
              "phone" -> InputType.TYPE_CLASS_PHONE
              "datetime" -> InputType.TYPE_CLASS_DATETIME
              "date" -> InputType.TYPE_DATETIME_VARIATION_DATE
              "time" -> InputType.TYPE_DATETIME_VARIATION_TIME
              else -> 0
            }
  }

  return inputType
}

// Max length
private fun TextInputEditText.applyMaxLengthM3(value: String): Boolean {
  return try {
    val maxLength = value.toIntOrNull() ?: return false
    filters = arrayOf(android.text.InputFilter.LengthFilter(maxLength))
    true
  } catch (e: Exception) {
    log.error("Failed to apply max length", e)
    false
  }
}

// Lines
private fun TextInputEditText.applyMaxLinesM3(value: String): Boolean {
  return try {
    val lines = value.toIntOrNull() ?: return false
    maxLines = lines
    true
  } catch (e: Exception) {
    log.error("Failed to apply max lines", e)
    false
  }
}

private fun TextInputEditText.applyMinLinesM3(value: String): Boolean {
  return try {
    val lines = value.toIntOrNull() ?: return false
    minLines = lines
    true
  } catch (e: Exception) {
    log.error("Failed to apply min lines", e)
    false
  }
}

private fun TextInputEditText.applyLinesM3(value: String): Boolean {
  return try {
    val lines = value.toIntOrNull() ?: return false
    setLines(lines)
    true
  } catch (e: Exception) {
    log.error("Failed to apply lines", e)
    false
  }
}

private fun TextInputEditText.applySingleLineM3(value: String): Boolean {
  return try {
    val singleLine = value.lowercase() in listOf("true", "1", "yes")
    isSingleLine = singleLine
    true
  } catch (e: Exception) {
    log.error("Failed to apply single line", e)
    false
  }
}

// State attributes
private fun TextInputEditText.applyEnabledM3(value: String): Boolean {
  return try {
    isEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply enabled", e)
    false
  }
}

private fun TextInputEditText.applyEditableM3(value: String): Boolean {
  return try {
    val editable = value.lowercase() in listOf("true", "1", "yes")
    // EditText doesn't have direct editable property, use enabled or focusable
    isFocusable = editable
    isFocusableInTouchMode = editable
    true
  } catch (e: Exception) {
    log.error("Failed to apply editable", e)
    false
  }
}

private fun TextInputEditText.applyCursorVisibleM3(value: String): Boolean {
  return try {
    isCursorVisible = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply cursor visible", e)
    false
  }
}

private fun TextInputEditText.applySelectAllOnFocusM3(value: String): Boolean {
  return try {
    setSelectAllOnFocus(value.lowercase() in listOf("true", "1", "yes"))
    true
  } catch (e: Exception) {
    log.error("Failed to apply select all on focus", e)
    false
  }
}

// IME attributes
private fun TextInputEditText.applyImeActionLabelM3(value: String): Boolean {
  return try {
    setImeActionLabel(value, imeOptions)
    true
  } catch (e: Exception) {
    log.error("Failed to apply IME action label", e)
    false
  }
}

private fun TextInputEditText.applyImeOptionsM3(value: String): Boolean {
  return try {
    val imeOption = parseImeOptions(value)
    if (imeOption != 0) {
      imeOptions = imeOption
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply IME options", e)
    false
  }
}

private fun parseImeOptions(value: String): Int {
  var options = 0
  val parts = value.split("|")

  for (part in parts) {
    options =
        options or
            when (part.trim().lowercase()) {
              "normal" -> android.view.inputmethod.EditorInfo.IME_NULL
              "actionnone" -> android.view.inputmethod.EditorInfo.IME_ACTION_NONE
              "actiongo" -> android.view.inputmethod.EditorInfo.IME_ACTION_GO
              "actionsearch" -> android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
              "actionsend" -> android.view.inputmethod.EditorInfo.IME_ACTION_SEND
              "actionnext" -> android.view.inputmethod.EditorInfo.IME_ACTION_NEXT
              "actiondone" -> android.view.inputmethod.EditorInfo.IME_ACTION_DONE
              "actionprevious" -> android.view.inputmethod.EditorInfo.IME_ACTION_PREVIOUS
              "flagnoextractui" -> android.view.inputmethod.EditorInfo.IME_FLAG_NO_EXTRACT_UI
              "flagnofullscreen" -> android.view.inputmethod.EditorInfo.IME_FLAG_NO_FULLSCREEN
              "flagnavigatenext" -> android.view.inputmethod.EditorInfo.IME_FLAG_NAVIGATE_NEXT
              "flagnavigateprevious" ->
                  android.view.inputmethod.EditorInfo.IME_FLAG_NAVIGATE_PREVIOUS
              "flagforceascii" -> android.view.inputmethod.EditorInfo.IME_FLAG_FORCE_ASCII
              else -> 0
            }
  }

  return options
}

// Drawable attributes
private fun TextInputEditText.applyDrawableStartM3(
    value: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      value.isEmpty() -> {
        setCompoundDrawablesWithIntrinsicBounds(
            null,
            compoundDrawables[1],
            compoundDrawables[2],
            compoundDrawables[3],
        )
        true
      }
      value.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(value, context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              drawable,
              compoundDrawables[1],
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
      value.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              drawable,
              compoundDrawables[1],
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
      value.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              drawable,
              compoundDrawables[1],
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$value", context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              drawable,
              compoundDrawables[1],
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply drawable start", e)
    false
  }
}

private fun TextInputEditText.applyDrawableEndM3(
    value: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      value.isEmpty() -> {
        setCompoundDrawablesWithIntrinsicBounds(
            compoundDrawables[0],
            compoundDrawables[1],
            null,
            compoundDrawables[3],
        )
        true
      }
      value.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(value, context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              drawable,
              compoundDrawables[3],
          )
        }
      }
      value.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              drawable,
              compoundDrawables[3],
          )
        }
      }
      value.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              drawable,
              compoundDrawables[3],
          )
        }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$value", context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              drawable,
              compoundDrawables[3],
          )
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply drawable end", e)
    false
  }
}

private fun TextInputEditText.applyDrawableTopM3(
    value: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      value.isEmpty() -> {
        setCompoundDrawablesWithIntrinsicBounds(
            compoundDrawables[0],
            null,
            compoundDrawables[2],
            compoundDrawables[3],
        )
        true
      }
      value.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(value, context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              drawable,
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
      value.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              drawable,
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
      value.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              drawable,
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$value", context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              drawable,
              compoundDrawables[2],
              compoundDrawables[3],
          )
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply drawable top", e)
    false
  }
}

private fun TextInputEditText.applyDrawableBottomM3(
    value: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      value.isEmpty() -> {
        setCompoundDrawablesWithIntrinsicBounds(
            compoundDrawables[0],
            compoundDrawables[1],
            compoundDrawables[2],
            null,
        )
        true
      }
      value.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(value, context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              compoundDrawables[2],
              drawable,
          )
        }
      }
      value.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              compoundDrawables[2],
              drawable,
          )
        }
      }
      value.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(value, context) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              compoundDrawables[2],
              drawable,
          )
        }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$value", context, workspace, layoutFile) { drawable ->
          setCompoundDrawablesWithIntrinsicBounds(
              compoundDrawables[0],
              compoundDrawables[1],
              compoundDrawables[2],
              drawable,
          )
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply drawable bottom", e)
    false
  }
}

private fun TextInputEditText.applyDrawableTintM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    compoundDrawableTintList = M3Utils.createM3ColorStateList(color)
    true
  } catch (e: Exception) {
    log.error("Failed to apply drawable tint", e)
    false
  }
}

private fun TextInputEditText.applyDrawablePaddingM3(value: String, context: Context): Boolean {
  return try {
    val padding = M3Utils.parseDimensionM3(value, context)
    if (padding >= 0) {
      compoundDrawablePadding = padding
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply drawable padding", e)
    false
  }
}
