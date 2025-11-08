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
import com.google.android.material.badge.BadgeDrawable
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("BadgeDrawableM3Extensions")

/**
 * BadgeDrawable M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun BadgeDrawable.applyM3Preview(
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
      "backgroundcolor" -> applyBackgroundColorM3(value, context)
      "badgetextcolor" -> applyBadgeTextColorM3(value, context)
      "number",
      "badgenumber" -> applyNumberM3(value)
      "maxcharactercount",
      "maxcount" -> applyMaxCharacterCountM3(value)
      "badgegravity" -> applyBadgeGravityM3(value)
      "horizontaloffset" -> applyHorizontalOffsetM3(value, context)
      "verticaloffset" -> applyVerticalOffsetM3(value, context)
      "visible",
      "isvisible" -> applyVisibilityM3(value)
      "textsize" -> applyTextSizeM3(value, context)
      "alpha" -> applyAlphaM3(value)
      "badgeradius" -> applyBadgeRadiusM3(value, context)
      else -> {
        log.debug("Unsupported BadgeDrawable attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply BadgeDrawable M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun BadgeDrawable.applyBackgroundColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      backgroundColor = color
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyBadgeTextColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      badgeTextColor = color
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyNumberM3(numberValue: String): Boolean {
  return try {
    val number = numberValue.toIntOrNull()
    if (number != null) {
      if (number > 0) {
        this.number = number
      } else {
        clearNumber()
      }
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyMaxCharacterCountM3(countValue: String): Boolean {
  return try {
    val count = countValue.toIntOrNull()
    if (count != null && count > 0) {
      maxCharacterCount = count
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyBadgeGravityM3(gravityValue: String): Boolean {
  return try {
    val gravity =
        when (gravityValue.lowercase()) {
          "top_end" -> BadgeDrawable.TOP_END
          "top_start" -> BadgeDrawable.TOP_START
          "bottom_end" -> BadgeDrawable.BOTTOM_END
          "bottom_start" -> BadgeDrawable.BOTTOM_START
          else -> return false
        }
    badgeGravity = gravity
    true
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyHorizontalOffsetM3(offsetValue: String, context: Context): Boolean {
  return try {
    val offset = M3Utils.parseDimensionM3(offsetValue, context)
    horizontalOffset = offset
    true
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyVerticalOffsetM3(offsetValue: String, context: Context): Boolean {
  return try {
    val offset = M3Utils.parseDimensionM3(offsetValue, context)
    verticalOffset = offset
    true
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyVisibilityM3(visibleValue: String): Boolean {
  return try {
    isVisible = visibleValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyTextSizeM3(sizeValue: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(sizeValue, context)
    if (size > 0) {
      // Note: BadgeDrawable doesn't have direct text size setter
      // This would require accessing internal text appearance
      log.warn("BadgeDrawable text size modification not directly supported")
      false
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyAlphaM3(alphaValue: String): Boolean {
  return try {
    val alphaFloat = alphaValue.toFloatOrNull()
    if (alphaFloat != null && alphaFloat in 0f..1f) {
      alpha = (alphaFloat * 255).toInt()
      true
    } else {
      val alphaInt = alphaValue.toIntOrNull()
      if (alphaInt != null && alphaInt in 0..255) {
        alpha = alphaInt
        true
      } else false
    }
  } catch (e: Exception) {
    false
  }
}

private fun BadgeDrawable.applyBadgeRadiusM3(radiusValue: String, context: Context): Boolean {
  return try {
    val radius = M3Utils.parseDimensionM3(radiusValue, context)
    if (radius >= 0) {
      // Note: BadgeDrawable doesn't expose radius setter directly
      // This is controlled by the badge style
      log.warn("BadgeDrawable radius modification not directly supported")
      false
    } else false
  } catch (e: Exception) {
    false
  }
}
