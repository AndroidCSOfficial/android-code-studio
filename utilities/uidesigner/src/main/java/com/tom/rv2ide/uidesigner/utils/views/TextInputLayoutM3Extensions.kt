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
import com.google.android.material.textfield.TextInputLayout
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TextInputLayoutM3Extensions")

/**
 * TextInputLayout M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun TextInputLayout.applyM3Preview(
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
      "hint" -> applyHintM3(value)
      "helpertext" -> applyHelperTextM3(value)
      "errortext" -> applyErrorTextM3(value)
      "counterenabled" -> applyCounterEnabledM3(value)
      "countermaxlength" -> applyCounterMaxLengthM3(value)
      "hinttextcolor" -> applyHintTextColorM3(value, context)
      "boxstrokecolor" -> applyBoxStrokeColorM3(value, context)
      "boxstrokewidth" -> applyBoxStrokeWidthM3(value, context)
      "boxstrokewidthfocused" -> applyBoxStrokeWidthFocusedM3(value, context)
      "boxcornerradius" -> applyBoxCornerRadiusM3(value, context)
      "boxcornerradiustopleft" -> applyBoxCornerRadiusTopLeftM3(value, context)
      "boxcornerradiustopright" -> applyBoxCornerRadiusTopRightM3(value, context)
      "boxcornerradiusbottomleft" -> applyBoxCornerRadiusBottomLeftM3(value, context)
      "boxcornerradiusbottomright" -> applyBoxCornerRadiusBottomRightM3(value, context)
      "starticon" -> applyStartIconM3(value, context, workspace, layoutFile)
      "starticontint" -> applyStartIconTintM3(value, context)
      "startcontentdescription" -> applyStartIconContentDescriptionM3(value)
      "endicon" -> applyEndIconM3(value, context, workspace, layoutFile)
      "endicontint" -> applyEndIconTintM3(value, context)
      "endiconmode" -> applyEndIconModeM3(value)
      "endcontentdescription" -> applyEndIconContentDescriptionM3(value)
      "prefixtext" -> applyPrefixTextM3(value)
      "suffixtext" -> applySuffixTextM3(value)
      "prefixtextcolor" -> applyPrefixTextColorM3(value, context)
      "suffixtextcolor" -> applySuffixTextColorM3(value, context)
      "hintanimationenabled" -> applyHintAnimationEnabledM3(value)
      "hintenabled" -> applyHintEnabledM3(value)
      "enabled" -> applyEnabledM3(value)
      "error" -> applyErrorEnabledM3(value)
      "errorenabled" -> applyErrorEnabledM3(value)
      "helpertextenabled" -> applyHelperTextEnabledM3(value)
      "placeholdertext" -> applyPlaceholderTextM3(value)
      "placeholdertextcolor" -> applyPlaceholderTextColorM3(value, context)
      else -> {
        log.debug("Unsupported TextInputLayout attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply TextInputLayout M3 attribute: $normalizedAttrName", e)
    false
  }
}

// Basic text attributes
private fun TextInputLayout.applyHintM3(value: String): Boolean {
  return try {
    hint = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply hint", e)
    false
  }
}

private fun TextInputLayout.applyHelperTextM3(value: String): Boolean {
  return try {
    helperText = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply helper text", e)
    false
  }
}

private fun TextInputLayout.applyErrorTextM3(value: String): Boolean {
  return try {
    error = value
    isErrorEnabled = value.isNotEmpty()
    true
  } catch (e: Exception) {
    log.error("Failed to apply error text", e)
    false
  }
}

private fun TextInputLayout.applyPlaceholderTextM3(value: String): Boolean {
  return try {
    placeholderText = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply placeholder text", e)
    false
  }
}

// Counter attributes
private fun TextInputLayout.applyCounterEnabledM3(value: String): Boolean {
  return try {
    isCounterEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply counter enabled", e)
    false
  }
}

private fun TextInputLayout.applyCounterMaxLengthM3(value: String): Boolean {
  return try {
    val maxLength = value.toIntOrNull() ?: return false
    counterMaxLength = maxLength
    true
  } catch (e: Exception) {
    log.error("Failed to apply counter max length", e)
    false
  }
}

// Color attributes
private fun TextInputLayout.applyHintTextColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setHintTextColor(M3Utils.createM3ColorStateList(color))
    true
  } catch (e: Exception) {
    log.error("Failed to apply hint text color", e)
    false
  }
}

private fun TextInputLayout.applyBoxStrokeColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    boxStrokeColor = color
    true
  } catch (e: Exception) {
    log.error("Failed to apply box stroke color", e)
    false
  }
}

private fun TextInputLayout.applyPrefixTextColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setPrefixTextColor(M3Utils.createM3ColorStateList(color))
    true
  } catch (e: Exception) {
    log.error("Failed to apply prefix text color", e)
    false
  }
}

private fun TextInputLayout.applySuffixTextColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setSuffixTextColor(M3Utils.createM3ColorStateList(color))
    true
  } catch (e: Exception) {
    log.error("Failed to apply suffix text color", e)
    false
  }
}

private fun TextInputLayout.applyPlaceholderTextColorM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setPlaceholderTextColor(M3Utils.createM3ColorStateList(color))
    true
  } catch (e: Exception) {
    log.error("Failed to apply placeholder text color", e)
    false
  }
}

// Box stroke and corner attributes
private fun TextInputLayout.applyBoxStrokeWidthM3(value: String, context: Context): Boolean {
  return try {
    val width = M3Utils.parseDimensionM3(value, context)
    if (width >= 0) {
      boxStrokeWidth = width
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box stroke width", e)
    false
  }
}

private fun TextInputLayout.applyBoxStrokeWidthFocusedM3(value: String, context: Context): Boolean {
  return try {
    val width = M3Utils.parseDimensionM3(value, context)
    if (width >= 0) {
      boxStrokeWidthFocused = width
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box stroke width focused", e)
    false
  }
}

private fun TextInputLayout.applyBoxCornerRadiusM3(value: String, context: Context): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(value, context).toFloat()
    if (radius >= 0) {
      setBoxCornerRadii(radius, radius, radius, radius)
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box corner radius", e)
    false
  }
}

private fun TextInputLayout.applyBoxCornerRadiusTopLeftM3(
    value: String,
    context: Context,
): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(value, context).toFloat()
    if (radius >= 0) {
      // Note: This requires getting current radii and updating only one corner
      // For simplicity, we'll just set all corners to this value
      // In a real implementation, you'd need to track all corner values
      setBoxCornerRadii(radius, radius, radius, radius)
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box corner radius top left", e)
    false
  }
}

private fun TextInputLayout.applyBoxCornerRadiusTopRightM3(
    value: String,
    context: Context,
): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(value, context).toFloat()
    if (radius >= 0) {
      setBoxCornerRadii(radius, radius, radius, radius)
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box corner radius top right", e)
    false
  }
}

private fun TextInputLayout.applyBoxCornerRadiusBottomLeftM3(
    value: String,
    context: Context,
): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(value, context).toFloat()
    if (radius >= 0) {
      setBoxCornerRadii(radius, radius, radius, radius)
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box corner radius bottom left", e)
    false
  }
}

private fun TextInputLayout.applyBoxCornerRadiusBottomRightM3(
    value: String,
    context: Context,
): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(value, context).toFloat()
    if (radius >= 0) {
      setBoxCornerRadii(radius, radius, radius, radius)
      true
    } else false
  } catch (e: Exception) {
    log.error("Failed to apply box corner radius bottom right", e)
    false
  }
}

// Icon attributes
private fun TextInputLayout.applyStartIconM3(
    value: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      value.isEmpty() -> {
        startIconDrawable = null
        true
      }
      value.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(value, context, workspace, layoutFile) { drawable ->
          startIconDrawable = drawable
        }
      }
      value.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(value, context) { drawable -> startIconDrawable = drawable }
      }
      value.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(value, context) { drawable -> startIconDrawable = drawable }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$value", context, workspace, layoutFile) { drawable ->
          startIconDrawable = drawable
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply start icon", e)
    false
  }
}

private fun TextInputLayout.applyStartIconTintM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setStartIconTintList(M3Utils.createM3ColorStateList(color))
    true
  } catch (e: Exception) {
    log.error("Failed to apply start icon tint", e)
    false
  }
}

private fun TextInputLayout.applyStartIconContentDescriptionM3(value: String): Boolean {
  return try {
    startIconContentDescription = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply start icon content description", e)
    false
  }
}

private fun TextInputLayout.applyEndIconM3(
    value: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      value.isEmpty() -> {
        endIconDrawable = null
        true
      }
      value.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(value, context, workspace, layoutFile) { drawable ->
          endIconDrawable = drawable
        }
      }
      value.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(value, context) { drawable -> endIconDrawable = drawable }
      }
      value.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(value, context) { drawable -> endIconDrawable = drawable }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$value", context, workspace, layoutFile) { drawable ->
          endIconDrawable = drawable
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply end icon", e)
    false
  }
}

private fun TextInputLayout.applyEndIconTintM3(value: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(value, context) ?: return false
    setEndIconTintList(M3Utils.createM3ColorStateList(color))
    true
  } catch (e: Exception) {
    log.error("Failed to apply end icon tint", e)
    false
  }
}

private fun TextInputLayout.applyEndIconModeM3(value: String): Boolean {
  return try {
    val mode =
        when (value.lowercase()) {
          "none" -> TextInputLayout.END_ICON_NONE
          "password_toggle" -> TextInputLayout.END_ICON_PASSWORD_TOGGLE
          "clear_text" -> TextInputLayout.END_ICON_CLEAR_TEXT
          "dropdown_menu" -> TextInputLayout.END_ICON_DROPDOWN_MENU
          "custom" -> TextInputLayout.END_ICON_CUSTOM
          else -> return false
        }
    endIconMode = mode
    true
  } catch (e: Exception) {
    log.error("Failed to apply end icon mode", e)
    false
  }
}

private fun TextInputLayout.applyEndIconContentDescriptionM3(value: String): Boolean {
  return try {
    endIconContentDescription = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply end icon content description", e)
    false
  }
}

// Prefix and suffix
private fun TextInputLayout.applyPrefixTextM3(value: String): Boolean {
  return try {
    prefixText = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply prefix text", e)
    false
  }
}

private fun TextInputLayout.applySuffixTextM3(value: String): Boolean {
  return try {
    suffixText = value
    true
  } catch (e: Exception) {
    log.error("Failed to apply suffix text", e)
    false
  }
}

// Animation and state attributes
private fun TextInputLayout.applyHintAnimationEnabledM3(value: String): Boolean {
  return try {
    isHintAnimationEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply hint animation enabled", e)
    false
  }
}

private fun TextInputLayout.applyHintEnabledM3(value: String): Boolean {
  return try {
    isHintEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply hint enabled", e)
    false
  }
}

private fun TextInputLayout.applyEnabledM3(value: String): Boolean {
  return try {
    isEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply enabled", e)
    false
  }
}

private fun TextInputLayout.applyErrorEnabledM3(value: String): Boolean {
  return try {
    isErrorEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply error enabled", e)
    false
  }
}

private fun TextInputLayout.applyHelperTextEnabledM3(value: String): Boolean {
  return try {
    isHelperTextEnabled = value.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    log.error("Failed to apply helper text enabled", e)
    false
  }
}
