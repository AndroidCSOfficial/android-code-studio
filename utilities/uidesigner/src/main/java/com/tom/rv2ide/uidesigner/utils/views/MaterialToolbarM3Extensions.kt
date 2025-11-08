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
import com.google.android.material.appbar.MaterialToolbar
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("MaterialToolbarM3Extensions")

/**
 * MaterialToolbar M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun MaterialToolbar.applyM3Preview(
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
      "title" -> applyTitleM3(value)
      "subtitle" -> applySubtitleM3(value)
      "titlecentered" -> applyTitleCenteredM3(value)
      "subtitlecentered" -> applySubtitleCenteredM3(value)
      "navigationicon" -> applyNavigationIconM3(value, context, workspace, layoutFile)
      "menu" -> applyMenuM3(value, context, workspace)
      "elevation" -> applyElevationM3(value, context)
      else -> {
        log.debug("Unsupported MaterialToolbar attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply MaterialToolbar M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun MaterialToolbar.applyTitleM3(titleValue: String): Boolean {
  return try {
    title = titleValue
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialToolbar.applySubtitleM3(subtitleValue: String): Boolean {
  return try {
    subtitle = subtitleValue
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialToolbar.applyTitleCenteredM3(centeredValue: String): Boolean {
  return try {
    isTitleCentered = centeredValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialToolbar.applySubtitleCenteredM3(centeredValue: String): Boolean {
  return try {
    isSubtitleCentered = centeredValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun MaterialToolbar.applyNavigationIconM3(
    iconValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      iconValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(iconValue, context, workspace, layoutFile) { drawable ->
          navigationIcon = drawable
        }
      }
      iconValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(iconValue, context) { drawable -> navigationIcon = drawable }
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun MaterialToolbar.applyMenuM3(
    menuValue: String,
    context: Context,
    workspace: IWorkspace?,
): Boolean {
  return try {
    if (menuValue.startsWith("@menu/")) {
      val menuName = menuValue.removePrefix("@menu/")
      val menuId = context.resources.getIdentifier(menuName, "menu", context.packageName)
      if (menuId != 0) {
        inflateMenu(menuId)
        true
      } else false
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun MaterialToolbar.applyElevationM3(elevationValue: String, context: Context): Boolean {
  return try {
    val elevation = M3Utils.parseDimensionM3(elevationValue, context)
    if (elevation >= 0) {
      this.elevation = elevation.toFloat()
      true
    } else false
  } catch (e: Exception) {
    false
  }
}
