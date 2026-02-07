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

import android.os.Build
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("BottomNavigationViewM3Extensions")

/**
 * Material BottomNavigationView M3 preview extension
 * Handles Material Design 3 specific attributes for bottom navigation
 *
 * @author Enhancement for M3 compatibility
 */
fun BottomNavigationView.applyM3Preview(
    attributeName: String,
    attributeValue: String,
    context: android.content.Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  val value = attributeValue.trim()
  if (value.isEmpty()) return false

  val normalizedAttrName = attributeName.lowercase().replace("app:", "").replace("android:", "")

  return try {
    when (normalizedAttrName) {
      "menu" -> applyMenuM3(value)
      "itemicontint" -> applyItemIconTintM3(value, context)
      "itemtexttint" -> applyItemTextTintM3(value, context)
      "itembackgroundcolor" -> applyItemBackgroundColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      "labelvisibilitymode" -> applyLabelVisibilityModeM3(value)
      "activeIndicatorColor" -> applyActiveIndicatorColorM3(value, context)
      "activeIndicatorWidth" -> applyActiveIndicatorWidthM3(value, context)
      "activeIndicatorHeight" -> applyActiveIndicatorHeightM3(value, context)
      "activeIndicatorMarginHorizontal" ->
          applyActiveIndicatorMarginHorizontalM3(value, context)
      "activeIndicatorMarginVertical" -> applyActiveIndicatorMarginVerticalM3(value, context)
      "shapeappearance" -> {
        log.debug("BottomNavigationView shape appearance: $value")
        true
      }
      else -> {
        log.debug("Unsupported BottomNavigationView attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply BottomNavigationView M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun BottomNavigationView.applyMenuM3(menuValue: String): Boolean {
  return try {
    log.debug("BottomNavigationView menu resource: $menuValue")
    true
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyItemIconTintM3(
    tintValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      itemIconTintList = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyItemTextTintM3(
    tintValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      itemTextColor = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyItemBackgroundColorM3(
    colorValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      itemBackgroundColor = color
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyElevationM3(
    elevationValue: String,
    context: android.content.Context,
): Boolean {
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

private fun BottomNavigationView.applyBackgroundColorM3(
    colorValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setBackgroundColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyLabelVisibilityModeM3(visibilityMode: String): Boolean {
  return try {
    when (visibilityMode.lowercase()) {
      "labeled" -> {
        labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_LABELED
        true
      }
      "selected" -> {
        labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_SELECTED
        true
      }
      "unlabeled" -> {
        labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_UNLABELED
        true
      }
      "auto" -> {
        labelVisibilityMode = BottomNavigationView.LABEL_VISIBILITY_AUTO
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyActiveIndicatorColorM3(
    colorValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      itemActiveIndicatorColor = color
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyActiveIndicatorWidthM3(
    widthValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val width = M3Utils.parseDimensionM3(widthValue, context)
    if (width > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      itemActiveIndicatorWidth = width
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyActiveIndicatorHeightM3(
    heightValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val height = M3Utils.parseDimensionM3(heightValue, context)
    if (height > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      itemActiveIndicatorHeight = height
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyActiveIndicatorMarginHorizontalM3(
    marginValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val margin = M3Utils.parseDimensionM3(marginValue, context)
    if (margin >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      itemActiveIndicatorMarginHorizontal = margin
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomNavigationView.applyActiveIndicatorMarginVerticalM3(
    marginValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val margin = M3Utils.parseDimensionM3(marginValue, context)
    if (margin >= 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      itemActiveIndicatorMarginVertical = margin
      true
    } else false
  } catch (e: Exception) {
    false
  }
}
