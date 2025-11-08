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
import com.google.android.material.appbar.AppBarLayout
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AppBarLayoutM3Extensions")

/**
 * AppBarLayout M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun AppBarLayout.applyM3Preview(
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
      "expanded" -> applyExpandedM3(value)
      "liftonelevation" -> applyLiftOnScrollM3(value)
      else -> {
        log.debug("Unsupported AppBarLayout attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply AppBarLayout M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun AppBarLayout.applyElevationM3(elevationValue: String, context: Context): Boolean {
  return try {
    val elevation = M3Utils.parseDimensionM3(elevationValue, context)
    if (elevation >= 0) {
      targetElevation = elevation.toFloat()
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun AppBarLayout.applyExpandedM3(expandedValue: String): Boolean {
  return try {
    val expanded = expandedValue.lowercase() in listOf("true", "1", "yes")
    setExpanded(expanded, false)
    true
  } catch (e: Exception) {
    false
  }
}

private fun AppBarLayout.applyLiftOnScrollM3(liftValue: String): Boolean {
  return try {
    isLiftOnScroll = liftValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}
