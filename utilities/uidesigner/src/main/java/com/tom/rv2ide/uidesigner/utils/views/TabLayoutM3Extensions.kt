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
import com.google.android.material.tabs.TabLayout
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("TabLayoutM3Extensions")

/**
 * Material TabLayout M3 preview extension
 *
 * @author Enhancement for M3 compatibility
 */
fun TabLayout.applyM3Preview(
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
      "tabmode" -> applyTabModeM3(value)
      "tabgravity" -> applyTabGravityM3(value)
      "tabindicatorcolor" -> applyTabIndicatorColorM3(value, context)
      "tabindicatorheight" -> applyTabIndicatorHeightM3(value, context)
      "tabtextcolor" -> applyTabTextColorM3(value, context)
      "tabbackgroundcolor" -> applyTabBackgroundColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      else -> {
        log.debug("Unsupported TabLayout attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply TabLayout M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun TabLayout.applyTabModeM3(modeValue: String): Boolean {
  return try {
    when (modeValue.lowercase()) {
      "fixed" -> {
        tabMode = TabLayout.MODE_FIXED
        true
      }
      "scrollable" -> {
        tabMode = TabLayout.MODE_SCROLLABLE
        true
      }
      "auto" -> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          tabMode = TabLayout.MODE_AUTO
        }
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun TabLayout.applyTabGravityM3(gravityValue: String): Boolean {
  return try {
    when (gravityValue.lowercase()) {
      "fill" -> {
        tabGravity = TabLayout.GRAVITY_FILL
        true
      }
      "center" -> {
        tabGravity = TabLayout.GRAVITY_CENTER
        true
      }
      "start" -> {
        tabGravity = TabLayout.GRAVITY_START
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun TabLayout.applyTabIndicatorColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setSelectedTabIndicatorColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun TabLayout.applyTabIndicatorHeightM3(heightValue: String, context: Context): Boolean {
  return try {
    val height = M3Utils.parseDimensionM3(heightValue, context)
    if (height > 0) {
      setSelectedTabIndicatorHeight(height)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun TabLayout.applyTabTextColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setTabTextColors(color, color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun TabLayout.applyTabBackgroundColorM3(colorValue: String, context: Context): Boolean {
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

private fun TabLayout.applyElevationM3(elevationValue: String, context: Context): Boolean {
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

private fun TabLayout.applyBackgroundColorM3(colorValue: String, context: Context): Boolean {
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
