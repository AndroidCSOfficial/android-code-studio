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
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SearchViewM3Extensions")

/**
 * Material SearchView M3 preview extension
 *
 * @author Enhancement for M3 compatibility
 */
fun SearchView.applyM3Preview(
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
      "hinticon" -> applyHintIconM3(value, context, workspace, layoutFile)
      "headerlayout" -> {
        log.debug("SearchView header layout: $value")
        true
      }
      "inputtype" -> applyInputTypeM3(value)
      "textcolor" -> applyTextColorM3(value, context)
      "hintcolor" -> applyHintColorM3(value, context)
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      else -> {
        log.debug("Unsupported SearchView attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply SearchView M3 attribute: $normalizedAttrName", e)
    false
  }
}

/**
 * Material SearchBar M3 preview extension
 */
fun SearchBar.applyM3Preview(
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
      "hinticon" -> applyHintIconM3(value, context, workspace, layoutFile)
      "navigationicon" -> applyNavigationIconM3(value, context, workspace, layoutFile)
      "menuitems" -> {
        log.debug("SearchBar menu items: $value")
        true
      }
      "textcolor" -> applyTextColorM3(value, context)
      "hintcolor" -> applyHintColorM3(value, context)
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      else -> {
        log.debug("Unsupported SearchBar attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply SearchBar M3 attribute: $normalizedAttrName", e)
    false
  }
}

// SearchView specific implementations
private fun SearchView.applyHintM3(hintValue: String): Boolean {
  return try {
    hint = hintValue
    true
  } catch (e: Exception) {
    false
  }
}

private fun SearchView.applyHintIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.isEmpty() -> true
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) {}
        true
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) {}
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    log.error("Failed to apply SearchView hint icon: $iconValue", e)
    false
  }
}

private fun SearchView.applyInputTypeM3(inputTypeValue: String): Boolean {
  return try {
    when (inputTypeValue.lowercase()) {
      "text" -> {
        true
      }
      "textsearch" -> {
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun SearchView.applyTextColorM3(colorValue: String, context: Context): Boolean {
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

private fun SearchView.applyHintColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setHintTextColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SearchView.applyBackgroundColorM3(colorValue: String, context: Context): Boolean {
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

// SearchBar specific implementations
private fun SearchBar.applyHintM3(hintValue: String): Boolean {
  return try {
    hint = hintValue
    true
  } catch (e: Exception) {
    false
  }
}

private fun SearchBar.applyHintIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.isEmpty() -> true
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) {}
        true
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) {}
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    log.error("Failed to apply SearchBar hint icon: $iconValue", e)
    false
  }
}

private fun SearchBar.applyNavigationIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.isEmpty() -> true
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) { drawable ->
          setNavigationIcon(drawable)
        }
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) { drawable ->
          setNavigationIcon(drawable)
        }
      }
      else -> false
    }
  } catch (e: Exception) {
    log.error("Failed to apply SearchBar navigation icon: $iconValue", e)
    false
  }
}

private fun SearchBar.applyTextColorM3(colorValue: String, context: Context): Boolean {
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

private fun SearchBar.applyHintColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setHintTextColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SearchBar.applyBackgroundColorM3(colorValue: String, context: Context): Boolean {
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

private fun SearchBar.applyElevationM3(elevationValue: String, context: Context): Boolean {
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
