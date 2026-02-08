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
import com.google.android.material.bottomappbar.BottomAppBar
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("BottomAppBarM3Extensions")

/**
 * Material BottomAppBar M3 preview extension
 *
 * @author Enhancement for M3 compatibility
 */
fun BottomAppBar.applyM3Preview(
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
      "elevation" -> applyElevationM3(value, context)
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      "fbalignmentmode" -> applyFabAlignmentModeM3(value)
      "fabcradlemargin" -> applyFabCradleMarginM3(value, context)
      "fabcradleroundedcornerradius" -> applyFabCradleRoundedCornerRadiusM3(value, context)
      "hideOnScroll" -> applyHideOnScrollM3(value)
      "navigationicon" -> applyNavigationIconM3(value, context, workspace, layoutFile)
      else -> {
        log.debug("Unsupported BottomAppBar attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply BottomAppBar M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun BottomAppBar.applyElevationM3(elevationValue: String, context: Context): Boolean {
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

private fun BottomAppBar.applyBackgroundColorM3(colorValue: String, context: Context): Boolean {
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

private fun BottomAppBar.applyFabAlignmentModeM3(modeValue: String): Boolean {
  return try {
    when (modeValue.lowercase()) {
      "center" -> {
        fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
        true
      }
      "end" -> {
        fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun BottomAppBar.applyFabCradleMarginM3(marginValue: String, context: Context): Boolean {
  return try {
    val margin = M3Utils.parseDimensionM3(marginValue, context)
    if (margin >= 0) {
      fabCradleMargin = margin.toFloat()
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomAppBar.applyFabCradleRoundedCornerRadiusM3(
    radiusValue: String,
    context: Context,
): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(radiusValue, context)
    if (radius >= 0) {
      fabCradleRoundedCornerRadius = radius.toFloat()
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BottomAppBar.applyHideOnScrollM3(hideValue: String): Boolean {
  return try {
    val hideOnScroll = hideValue.lowercase() == "true"
    this.hideOnScroll = hideOnScroll
    true
  } catch (e: Exception) {
    false
  }
}

private fun BottomAppBar.applyNavigationIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.isEmpty() -> {
        navigationIcon = null
        true
      }
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) { drawable ->
          navigationIcon = drawable
        }
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) { drawable ->
          navigationIcon = drawable
        }
      }
      else -> false
    }
  } catch (e: Exception) {
    log.error("Failed to apply BottomAppBar navigation icon: $iconValue", e)
    false
  }
}
