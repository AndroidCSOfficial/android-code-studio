/*
 * Advanced Drawable Parser for AndroidIDE UI Designer
 * Three simple methods - no complex parsing needed!
 */

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import org.slf4j.LoggerFactory

/** Simple drawable application - let Android handle the parsing */
class AdvancedDrawableParser {

  companion object {
    private val log = LoggerFactory.getLogger(BackgroundPreviewRenderer::class.java)
  }

  /** Main method - try all three approaches */
  fun applyIconToView(view: View, iconPath: String, context: Context): Boolean {
    // Try all three methods in order
    return tryResourceIdMethod(view, iconPath, context) ||
        tryAppCompatMethod(view, iconPath, context) ||
        tryDirectResourceMethod(view, iconPath, context)
  }

  /** Method 1: Use resource identifier (works with all drawable types including vectors) */
  private fun tryResourceIdMethod(view: View, iconPath: String, context: Context): Boolean {
    return try {
      val resourceName = cleanResourceName(iconPath)
      val resourceId =
          context.resources.getIdentifier(resourceName, "drawable", context.packageName)

      if (resourceId != 0) {
        when (view) {
          is ImageView -> {
            view.setImageResource(resourceId)
            log.debug("Method 1 SUCCESS (ImageView): $resourceName")
          }
          else -> {
            view.setBackgroundResource(resourceId)
            log.debug("Method 1 SUCCESS (Background): $resourceName")
          }
        }
        true
      } else {
        log.debug("Method 1 FAILED: Resource not found: $resourceName")
        false
      }
    } catch (e: Exception) {
      log.debug("Method 1 ERROR: ${e.message}")
      false
    }
  }

  /** Method 2: Use AppCompatResources (better vector support) */
  private fun tryAppCompatMethod(view: View, iconPath: String, context: Context): Boolean {
    return try {
      val resourceName = cleanResourceName(iconPath)
      val resourceId =
          context.resources.getIdentifier(resourceName, "drawable", context.packageName)

      if (resourceId != 0) {
        val drawable = AppCompatResources.getDrawable(context, resourceId)
        if (drawable != null) {
          when (view) {
            is ImageView -> {
              view.setImageDrawable(drawable)
              log.debug("Method 2 SUCCESS (ImageView): $resourceName")
            }
            else -> {
              view.background = drawable
              log.debug("Method 2 SUCCESS (Background): $resourceName")
            }
          }
          true
        } else {
          log.debug("Method 2 FAILED: Could not create drawable: $resourceName")
          false
        }
      } else {
        log.debug("Method 2 FAILED: Resource not found: $resourceName")
        false
      }
    } catch (e: Exception) {
      log.debug("Method 2 ERROR: ${e.message}")
      false
    }
  }

  /** Method 3: Direct resource loading with common resource IDs */
  private fun tryDirectResourceMethod(view: View, iconPath: String, context: Context): Boolean {
    return try {
      val resourceName = cleanResourceName(iconPath)

      // Try common Android system resources first
      val systemResourceId = getSystemResourceId(resourceName, context)
      if (systemResourceId != 0) {
        when (view) {
          is ImageView -> {
            view.setImageResource(systemResourceId)
            log.debug("Method 3 SUCCESS (System ImageView): $resourceName")
          }
          else -> {
            view.setBackgroundResource(systemResourceId)
            log.debug("Method 3 SUCCESS (System Background): $resourceName")
          }
        }
        return true
      }

      // Try with different variations of the name
      val variations =
          listOf(
              resourceName,
              "ic_$resourceName",
              "${resourceName}_24",
              "ic_${resourceName}_24dp",
              resourceName.replace("_", ""),
          )

      for (variation in variations) {
        val resourceId = context.resources.getIdentifier(variation, "drawable", context.packageName)
        if (resourceId != 0) {
          when (view) {
            is ImageView -> {
              view.setImageResource(resourceId)
              log.debug("Method 3 SUCCESS (Variation ImageView): $variation")
            }
            else -> {
              view.setBackgroundResource(resourceId)
              log.debug("Method 3 SUCCESS (Variation Background): $variation")
            }
          }
          return true
        }
      }

      log.debug("Method 3 FAILED: All variations failed for: $resourceName")
      false
    } catch (e: Exception) {
      log.debug("Method 3 ERROR: ${e.message}")
      false
    }
  }

  /** Clean the resource name from various formats */
  private fun cleanResourceName(iconPath: String): String {
    return iconPath
        .removePrefix("@drawable/")
        .removePrefix("@android:drawable/")
        .removeSuffix(".xml")
        .removeSuffix(".png")
        .removeSuffix(".jpg")
        .removeSuffix(".jpeg")
        .trim()
  }

  /** Try to get system resource ID for common icons */
  private fun getSystemResourceId(resourceName: String, context: Context): Int {
    return try {
      // Try android system resources
      context.resources.getIdentifier(resourceName, "drawable", "android")
    } catch (e: Exception) {
      0
    }
  }

  /**
   * Keep this method for backward compatibility with BackgroundPreviewExtensions.kt But now it just
   * delegates to the simple methods above
   */
  fun tryParseVectorWithPaths(
      xmlFile: java.io.File,
      context: Context,
  ): android.graphics.drawable.Drawable? {
    log.debug("Legacy method called - using resource approach instead")

    // Convert file path to resource name and try to load it
    val resourceName = xmlFile.nameWithoutExtension
    val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)

    return if (resourceId != 0) {
      try {
        AppCompatResources.getDrawable(context, resourceId)
      } catch (e: Exception) {
        log.debug("Legacy method failed: ${e.message}")
        null
      }
    } else {
      log.debug("Legacy method - resource not found: $resourceName")
      null
    }
  }
}
