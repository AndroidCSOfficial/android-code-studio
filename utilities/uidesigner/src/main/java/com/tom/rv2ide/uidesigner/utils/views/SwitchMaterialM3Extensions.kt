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
import com.google.android.material.switchmaterial.SwitchMaterial
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SwitchMaterialM3Extensions")

/**
 * Material SwitchMaterial M3 preview extension
 * Handles Material Design 3 specific attributes for switches
 *
 * @author Enhancement for M3 compatibility
 */
fun SwitchMaterial.applyM3Preview(
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
      "thumbicon" -> applyThumbIconM3(value, context, workspace, layoutFile)
      "thumbicontint" -> applyThumbIconTintM3(value, context)
      "tracktint" -> applyTrackTintM3(value, context)
      "trackinactivebordercolor" -> applyTrackInactiveBorderColorM3(value, context)
      "thumbtint" -> applyThumbTintM3(value, context)
      "textoncolor" -> applyTextOnColorM3(value, context)
      "textoffcolor" -> applyTextOffColorM3(value, context)
      "checked" -> applyCheckedStateM3(value)
      "enabled" -> applyEnabledStateM3(value)
      "text" -> applyTextM3(value)
      "textappearance" -> {
        log.debug("SwitchMaterial text appearance: $value")
        true
      }
      else -> {
        log.debug("Unsupported SwitchMaterial attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply SwitchMaterial M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun SwitchMaterial.applyThumbIconM3(
    iconValue: String,
    context: android.content.Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.isEmpty() -> {
        thumbIconDrawable = null
        true
      }
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) { drawable ->
          thumbIconDrawable = drawable
        }
      }
      iconValue.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(iconValue, context) { drawable -> thumbIconDrawable = drawable }
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) { drawable ->
          thumbIconDrawable = drawable
        }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$iconValue", context, workspace, layoutFile) { drawable ->
          thumbIconDrawable = drawable
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply thumb icon: $iconValue", e)
    false
  }
}

private fun SwitchMaterial.applyThumbIconTintM3(
    tintValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      thumbIconTintList = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyTrackTintM3(
    tintValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      trackTintList = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyTrackInactiveBorderColorM3(
    colorValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      trackDecorationDrawable = null
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyThumbTintM3(
    tintValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      thumbTintList = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyTextOnColorM3(
    colorValue: String,
    context: android.content.Context,
): Boolean {
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

private fun SwitchMaterial.applyTextOffColorM3(
    colorValue: String,
    context: android.content.Context,
): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      // Fallback for older APIs
      setTextColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyCheckedStateM3(checkedValue: String): Boolean {
  return try {
    isChecked =
        when (checkedValue.lowercase()) {
          "true" -> true
          "false" -> false
          else -> false
        }
    true
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyEnabledStateM3(enabledValue: String): Boolean {
  return try {
    isEnabled =
        when (enabledValue.lowercase()) {
          "true" -> true
          "false" -> false
          else -> true
        }
    true
  } catch (e: Exception) {
    false
  }
}

private fun SwitchMaterial.applyTextM3(textValue: String): Boolean {
  return try {
    text = textValue
    true
  } catch (e: Exception) {
    false
  }
}
