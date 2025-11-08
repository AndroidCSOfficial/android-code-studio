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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("ChipM3Extensions")

/**
 * Chip and ChipGroup M3 preview extensions
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

// ========== Chip Extensions ==========

fun Chip.applyM3Preview(
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
      "checkable" -> applyCheckableM3(value)
      "checked" -> applyCheckedM3(value)
      "closable",
      "closeiconenable",
      "closeiconeabled" -> applyCloseIconEnabledM3(value)
      "chipicon",
      "chipstarticon" -> applyChipIconM3(value, context, workspace, layoutFile)
      "chipiconsize" -> applyChipIconSizeM3(value, context)
      "chipicontint" -> applyChipIconTintM3(value, context)
      "chipbackgroundcolor" -> applyChipBackgroundColorM3(value, context)
      "chipstrokecolor" -> applyChipStrokeColorM3(value, context)
      "chipstrokewidth" -> applyChipStrokeWidthM3(value, context)
      "ripplecolor" -> applyRippleColorM3(value, context)
      "textcolor" -> applyTextColorM3(value, context)
      else -> {
        log.debug("Unsupported Chip attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply Chip M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun Chip.applyTextM3(textValue: String): Boolean {
  return try {
    text = textValue
    true
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyCheckableM3(checkableValue: String): Boolean {
  return try {
    isCheckable = checkableValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyCheckedM3(checkedValue: String): Boolean {
  return try {
    isChecked = checkedValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyCloseIconEnabledM3(enabledValue: String): Boolean {
  return try {
    isCloseIconVisible = enabledValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyChipIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) { drawable ->
          chipIcon = drawable
        }
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) { drawable -> chipIcon = drawable }
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyChipIconSizeM3(sizeValue: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(sizeValue, context)
    if (size > 0) {
      chipIconSize = size.toFloat()
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyChipIconTintM3(tintValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      chipIconTint = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyChipBackgroundColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      chipBackgroundColor = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyChipStrokeColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      chipStrokeColor = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyChipStrokeWidthM3(widthValue: String, context: Context): Boolean {
  return try {
    val width = M3Utils.parseDimensionM3(widthValue, context)
    if (width >= 0) {
      chipStrokeWidth = width.toFloat()
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyRippleColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      rippleColor = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Chip.applyTextColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setTextColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

// ========== ChipGroup Extensions ==========

fun ChipGroup.applyM3Preview(
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
      "singleselecion",
      "singleselection" -> applySingleSelectionM3(value)
      "selectionrequired" -> applySelectionRequiredM3(value)
      "chipspacing" -> applyChipSpacingM3(value, context)
      "chipspacinghorizontal" -> applyChipSpacingHorizontalM3(value, context)
      "chipspacingvertical" -> applyChipSpacingVerticalM3(value, context)
      else -> {
        log.debug("Unsupported ChipGroup attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply ChipGroup M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun ChipGroup.applySingleSelectionM3(singleValue: String): Boolean {
  return try {
    isSingleSelection = singleValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun ChipGroup.applySelectionRequiredM3(requiredValue: String): Boolean {
  return try {
    isSelectionRequired = requiredValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun ChipGroup.applyChipSpacingM3(spacingValue: String, context: Context): Boolean {
  return try {
    val spacing = M3Utils.parseDimensionM3(spacingValue, context)
    if (spacing >= 0) {
      chipSpacingHorizontal = spacing
      chipSpacingVertical = spacing
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun ChipGroup.applyChipSpacingHorizontalM3(
    spacingValue: String,
    context: Context,
): Boolean {
  return try {
    val spacing = M3Utils.parseDimensionM3(spacingValue, context)
    if (spacing >= 0) {
      chipSpacingHorizontal = spacing
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun ChipGroup.applyChipSpacingVerticalM3(spacingValue: String, context: Context): Boolean {
  return try {
    val spacing = M3Utils.parseDimensionM3(spacingValue, context)
    if (spacing >= 0) {
      chipSpacingVertical = spacing
      true
    } else false
  } catch (e: Exception) {
    false
  }
}
