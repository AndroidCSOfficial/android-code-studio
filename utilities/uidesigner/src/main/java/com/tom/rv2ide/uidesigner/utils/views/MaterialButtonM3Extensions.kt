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
import android.os.Build
import com.google.android.material.button.MaterialButton
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MaterialButtonM3Extensions")

/**
 * Material Button M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun MaterialButton.applyM3Preview(
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
      "icon" -> applyIconM3(value, context, workspace, layoutFile)
      "iconsize" -> applyIconSizeM3(value, context)
      "icontint" -> applyIconTintM3(value, context)
      "icongravity" -> applyIconGravityM3(value)
      "iconpadding" -> applyIconPaddingM3(value, context)
      "backgroundtint" -> applyBackgroundTintM3(value, context)
      "cornerradius" -> applyCornerRadiusM3(value, context)
      "strokewidth" -> applyStrokeWidthM3(value, context)
      "strokecolor" -> applyStrokeColorM3(value, context)
      "ripplecolor" -> applyRippleColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      "surfacecolor" -> applySurfaceColorM3(value, context)
      "textcolor" -> applyTextColorM3(value, context)
      "textsize" -> applyTextSizeM3(value, context)
      "textstyle" -> applyTextStyleM3(value, context)
      "shapeappearance" -> applyShapeAppearanceM3(value, context)
      "checked" -> applyCheckedStateM3(value)
      "enabled" -> applyEnabledStateM3(value)
      else -> {
        log.debug("Unsupported MaterialButton attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply MaterialButton M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun MaterialButton.applyIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.isEmpty() -> {
        icon = null
        true
      }
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) { drawable ->
          icon = drawable
        }
      }
      iconValue.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(iconValue, context) { drawable -> icon = drawable }
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) { drawable -> icon = drawable }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$iconValue", context, workspace, layoutFile) { drawable ->
          icon = drawable
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply icon: $iconValue", e)
    false
  }
}

private fun MaterialButton.applyBackgroundTintM3(tintValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      backgroundTintList = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyCornerRadiusM3(radiusValue: String, context: Context): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(radiusValue, context)
    if (radius >= 0) {
      cornerRadius = radius
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyElevationM3(elevationValue: String, context: Context): Boolean {
  return try {
    val elevation = M3Utils.parseDimensionM3(elevationValue, context)
    if (elevation >= 0) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.elevation = elevation.toFloat()
      }
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applySurfaceColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      background = M3Utils.createM3SurfaceBackground(color, cornerRadius)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyTextColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setTextColor(M3Utils.createM3ColorStateList(color))
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyTextSizeM3(sizeValue: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(sizeValue, context)
    if (size > 0) {
      textSize = size.toFloat() / context.resources.displayMetrics.scaledDensity
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyTextStyleM3(styleValue: String, context: Context): Boolean {
  return try {
    val style =
        when (styleValue.lowercase()) {
          "bold" -> android.graphics.Typeface.BOLD
          "italic" -> android.graphics.Typeface.ITALIC
          "bold_italic" -> android.graphics.Typeface.BOLD_ITALIC
          else -> android.graphics.Typeface.NORMAL
        }
    typeface = android.graphics.Typeface.defaultFromStyle(style)
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyShapeAppearanceM3(shapeValue: String, context: Context): Boolean {
  return try {
    when (shapeValue.lowercase()) {
      "rounded" -> {
        cornerRadius = M3Utils.dpToPx(8f, context)
        true
      }
      "rectangular" -> {
        cornerRadius = 0
        true
      }
      "circular" -> {
        cornerRadius = M3Utils.dpToPx(28f, context)
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyCheckedStateM3(checkedValue: String): Boolean {
  return try {
    isChecked = checkedValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyEnabledStateM3(enabledValue: String): Boolean {
  return try {
    isEnabled = enabledValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyIconSizeM3(sizeValue: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(sizeValue, context)
    if (size > 0) {
      iconSize = size
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyIconTintM3(tintValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      iconTint = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyIconGravityM3(gravityValue: String): Boolean {
  return try {
    val gravity =
        when (gravityValue.lowercase()) {
          "start" -> MaterialButton.ICON_GRAVITY_START
          "end" -> MaterialButton.ICON_GRAVITY_END
          "top" -> MaterialButton.ICON_GRAVITY_TOP
          "textstart" -> MaterialButton.ICON_GRAVITY_TEXT_START
          "textend" -> MaterialButton.ICON_GRAVITY_TEXT_END
          "texttop" -> MaterialButton.ICON_GRAVITY_TEXT_TOP
          else -> return false
        }
    iconGravity = gravity
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyIconPaddingM3(paddingValue: String, context: Context): Boolean {
  return try {
    val padding = M3Utils.parseDimensionM3(paddingValue, context)
    if (padding >= 0) {
      iconPadding = padding
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyStrokeWidthM3(widthValue: String, context: Context): Boolean {
  return try {
    val width = M3Utils.parseDimensionM3(widthValue, context)
    if (width >= 0) {
      strokeWidth = width
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyStrokeColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      strokeColor = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialButton.applyRippleColorM3(colorValue: String, context: Context): Boolean {
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
