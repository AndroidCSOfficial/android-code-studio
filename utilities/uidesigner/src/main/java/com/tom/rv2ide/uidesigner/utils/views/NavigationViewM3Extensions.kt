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
import com.google.android.material.navigation.NavigationView
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("NavigationViewM3Extensions")

/**
 * Material NavigationView M3 preview extension
 *
 * @author Enhancement for M3 compatibility
 */
fun NavigationView.applyM3Preview(
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
      "itemicontint" -> applyItemIconTintM3(value, context)
      "itemtextcolor" -> applyItemTextColorM3(value, context)
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      "itemhorizontalpadding" -> applyItemHorizontalPaddingM3(value, context)
      "itemverticalpadding" -> applyItemVerticalPaddingM3(value, context)
      else -> {
        log.debug("Unsupported NavigationView attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply NavigationView M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun NavigationView.applyItemIconTintM3(tintValue: String, context: Context): Boolean {
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

private fun NavigationView.applyItemTextColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      itemTextColor = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun NavigationView.applyBackgroundColorM3(colorValue: String, context: Context): Boolean {
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

private fun NavigationView.applyElevationM3(elevationValue: String, context: Context): Boolean {
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

private fun NavigationView.applyItemHorizontalPaddingM3(
    paddingValue: String,
    context: Context,
): Boolean {
  return try {
    val padding = M3Utils.parseDimensionM3(paddingValue, context)
    if (padding >= 0) {
      itemHorizontalPadding = padding
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun NavigationView.applyItemVerticalPaddingM3(
    paddingValue: String,
    context: Context,
): Boolean {
  return try {
    val padding = M3Utils.parseDimensionM3(paddingValue, context)
    if (padding >= 0) {
      itemVerticalPadding = padding
      true
    } else false
  } catch (e: Exception) {
    false
  }
}
