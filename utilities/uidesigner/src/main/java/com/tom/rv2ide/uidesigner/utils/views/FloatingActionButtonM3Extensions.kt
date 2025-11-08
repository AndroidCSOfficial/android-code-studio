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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("FloatingActionButtonM3Extensions")

/**
 * Floating Action Button M3 preview extension
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
fun FloatingActionButton.applyM3Preview(
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
      "src" -> applySrcM3(value, context, workspace, layoutFile)
      "tint" -> applyTintM3(value, context)
      "backgroundtint" -> applyBackgroundTintM3(value, context)
      "ripplecolor" -> applyRippleColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      "size" -> applySizeM3(value)
      "customsize" -> applyCustomSizeM3(value, context)
      "shapeappearance" -> applyShapeAppearanceM3(value, context)
      "maximagesize" -> applyMaxImageSizeM3(value, context)
      "showmotion" -> applyShowMotionM3(value, context)
      "hidemotion" -> applyHideMotionM3(value, context)
      "layout_anchor" -> applyLayoutAnchorM3(value)
      "layout_anchorgravity" -> applyLayoutAnchorGravityM3(value)
      "layout_gravity" -> applyLayoutGravityM3(value)
      "layout_behavior" -> applyLayoutBehaviorM3(value)
      "enabled" -> applyEnabledStateM3(value)
      "visible" -> applyVisibilityStateM3(value)
      "contentdescription" -> applyContentDescriptionM3(value)
      else -> {
        log.debug("Unsupported FloatingActionButton attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply FloatingActionButton M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun FloatingActionButton.applySrcM3(
    srcValue: String,
    context: Context,
    workspace: IWorkspace?,
    layoutFile: File?,
): Boolean {
  return try {
    when {
      srcValue.isEmpty() -> {
        setImageDrawable(null)
        true
      }
      srcValue.startsWith("@drawable/") -> {
        M3Utils.loadDrawableM3(srcValue, context, workspace, layoutFile) { drawable ->
          setImageDrawable(drawable)
        }
      }
      srcValue.startsWith("@mipmap/") -> {
        M3Utils.loadMipmapM3(srcValue, context) { drawable -> setImageDrawable(drawable) }
      }
      srcValue.startsWith("@android:drawable/") -> {
        M3Utils.loadAndroidDrawableM3(srcValue, context) { drawable -> setImageDrawable(drawable) }
      }
      else -> {
        M3Utils.loadDrawableM3("@drawable/$srcValue", context, workspace, layoutFile) { drawable ->
          setImageDrawable(drawable)
        }
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply src: $srcValue", e)
    false
  }
}

private fun FloatingActionButton.applyTintM3(tintValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(tintValue, context)
    if (color != null) {
      imageTintList = M3Utils.createM3ColorStateList(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyBackgroundTintM3(
    tintValue: String,
    context: Context,
): Boolean {
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

private fun FloatingActionButton.applyRippleColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      // For FAB, we need to use setRippleColor instead of direct property assignment
      setRippleColor(M3Utils.createM3ColorStateList(color))
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyElevationM3(
    elevationValue: String,
    context: Context,
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

private fun FloatingActionButton.applySizeM3(sizeValue: String): Boolean {
  return try {
    val size =
        when (sizeValue.lowercase()) {
          "mini",
          "auto" -> FloatingActionButton.SIZE_MINI
          "normal",
          "standard" -> FloatingActionButton.SIZE_NORMAL
          else -> return false
        }
    setSize(size)
    true
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyCustomSizeM3(sizeValue: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(sizeValue, context)
    if (size > 0) {
      setCustomSize(size)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyShapeAppearanceM3(
    shapeValue: String,
    context: Context,
): Boolean {
  return try {
    // FAB shape is typically controlled through ShapeAppearanceModel
    // For preview purposes, we'll handle basic shape types
    when (shapeValue.lowercase()) {
      "rounded" -> {
        // Use a smaller corner radius for rounded appearance
        // Actual shape would be set via ShapeAppearanceModel
        true
      }
      "rectangular" -> {
        // Rectangular FAB - would need ShapeAppearanceModel
        true
      }
      "circular" -> {
        // Default FAB behavior - circular
        true
      }
      "oval" -> {
        // Oval shape
        true
      }
      else -> false
    }
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyMaxImageSizeM3(sizeValue: String, context: Context): Boolean {
  return try {
    val size = M3Utils.parseDimensionM3(sizeValue, context)
    if (size > 0) {
      // maxImageSize is private, so we can't set it directly
      // This would typically be handled internally by the FAB
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyShowMotionM3(motionValue: String, context: Context): Boolean {
  return try {
    // Note: This would typically load a MotionSpec from resources
    // For preview purposes, we'll just return true if the value is not empty
    motionValue.isNotEmpty()
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyHideMotionM3(motionValue: String, context: Context): Boolean {
  return try {
    // Note: This would typically load a MotionSpec from resources
    // For preview purposes, we'll just return true if the value is not empty
    motionValue.isNotEmpty()
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyLayoutAnchorM3(anchorValue: String): Boolean {
  return try {
    // This would typically be handled by the layout system
    // For preview purposes, we'll just return true if the value is not empty
    anchorValue.isNotEmpty()
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyLayoutAnchorGravityM3(gravityValue: String): Boolean {
  return try {
    // This would typically be handled by the layout system
    // For preview purposes, we'll just return true if the value is not empty
    gravityValue.isNotEmpty()
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyLayoutGravityM3(gravityValue: String): Boolean {
  return try {
    // This would typically be handled by the layout system
    // For preview purposes, we'll just return true if the value is not empty
    gravityValue.isNotEmpty()
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyLayoutBehaviorM3(behaviorValue: String): Boolean {
  return try {
    // This would typically be handled by the layout system
    // For preview purposes, we'll just return true if the value is not empty
    behaviorValue.isNotEmpty()
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyEnabledStateM3(enabledValue: String): Boolean {
  return try {
    isEnabled = enabledValue.lowercase() in listOf("true", "1", "yes")
    true
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyVisibilityStateM3(visibleValue: String): Boolean {
  return try {
    val isVisible = visibleValue.lowercase() in listOf("true", "1", "yes", "visible")
    visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
    true
  } catch (e: Exception) {
    false
  }
}

private fun FloatingActionButton.applyContentDescriptionM3(descriptionValue: String): Boolean {
  return try {
    contentDescription = descriptionValue
    true
  } catch (e: Exception) {
    false
  }
}

// Extension function to access the underlying support widget for elevation properties
private fun FloatingActionButton.ensureSupportWidget():
    com.google.android.material.floatingactionbutton.FloatingActionButton {
  return this
}
