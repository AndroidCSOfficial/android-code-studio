/*
 * FIXED MaterialButton Preview Support for AndroidIDE UI Designer
 * This version addresses common issues and provides better debugging
 */

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.File
import org.slf4j.LoggerFactory

/** FIXED MaterialButton renderer with better error handling and debugging */
class MaterialButtonRenderer {

  companion object {
    private val log = LoggerFactory.getLogger(MaterialButtonRenderer::class.java)
  }

  /** Apply MaterialButton-specific attribute previews - FIXED VERSION */
  fun applyMaterialButtonPreview(
      button: MaterialButton,
      attributeName: String,
      attributeValue: String,
      context: Context,
      layoutFile: File? = null,
  ): Boolean {
    val value = attributeValue.trim()

    // log.*
    // log.*
    // log.*

    if (value.isEmpty()) {
      // log.*
      return false
    }

    try {
      val normalizedAttrName = attributeName.lowercase().replace("app:", "").replace("android:", "")
      // log.*

      val result =
          when (normalizedAttrName) {
            "icon" -> {
              // log.*
              applyIconPreview(button, value, context, layoutFile)
            }
            "iconsize" -> {
              // log.*
              applyIconSizePreview(button, value, context)
            }
            "icontint" -> {
              // log.*
              applyIconTintPreview(button, value, context)
            }
            "icongravity" -> {
              // log.*
              applyIconGravityPreview(button, value)
            }
            "iconpadding" -> {
              // log.*
              applyIconPaddingPreview(button, value, context)
            }
            "cornerradius" -> {
              // log.*
              applyCornerRadiusPreview(button, value, context)
            }
            "strokewidth" -> {
              // log.*
              applyStrokeWidthPreview(button, value, context)
            }
            "strokecolor" -> {
              // log.*
              applyStrokeColorPreview(button, value, context)
            }
            "backgroundtint" -> {
              // log.*
              applyBackgroundTintPreview(button, value, context)
            }
            "ripplecolor" -> {
              // log.*
              applyRippleColorPreview(button, value, context)
            }
            else -> {
              // log.*
              false
            }
          }

      // log.*
      return result
    } catch (e: Exception) {
      // log.*
      return false
    }
  }

  /** FIXED: Apply icon preview with better error handling */
  private fun applyIconPreview(
      button: MaterialButton,
      iconValue: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    // log.*

    return try {
      when {
        iconValue.isEmpty() -> {
          // log.*
          button.icon = null
          true
        }
        iconValue.startsWith("@drawable/") -> {
          // log.*
          loadDrawableIcon(button, iconValue, context, layoutFile)
        }
        iconValue.startsWith("@mipmap/") -> {
          // log.*
          loadMipmapIcon(button, iconValue, context, layoutFile)
        }
        iconValue.startsWith("@android:drawable/") -> {
          // log.*
          loadAndroidDrawableIcon(button, iconValue, context)
        }
        else -> {
          // log.*
          loadDrawableIcon(button, "@drawable/$iconValue", context, layoutFile)
        }
      }
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun loadDrawableIcon(
      button: MaterialButton,
      resourceName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val drawableName = resourceName.substringAfter("@drawable/")
    // log.*

    try {
      // Try standard resource loading first
      var resourceId =
          context.resources.getIdentifier(drawableName, "drawable", context.packageName)
      // log.*

      if (resourceId == 0) {
        resourceId =
            context.applicationContext.resources.getIdentifier(
                drawableName,
                "drawable",
                context.applicationContext.packageName,
            )
        // log.*
      }

      if (resourceId != 0) {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        // log.*
        if (drawable != null) {
          button.icon = drawable
          // log.*
          return true
        }
      }

      // Try loading from project files if resource loading failed
      // log.*
      return loadIconFromProjectFiles(button, drawableName, context, layoutFile)
    } catch (e: Exception) {
      // log.*
      return false
    }
  }

  private fun loadMipmapIcon(
      button: MaterialButton,
      resourceName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val mipmapName = resourceName.substringAfter("@mipmap/")
    // log.*

    try {
      var resourceId = context.resources.getIdentifier(mipmapName, "mipmap", context.packageName)
      if (resourceId == 0) {
        resourceId =
            context.applicationContext.resources.getIdentifier(
                mipmapName,
                "mipmap",
                context.applicationContext.packageName,
            )
      }

      if (resourceId != 0) {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable != null) {
          button.icon = drawable
          // log.*
          return true
        }
      }

      return loadMipmapFromProjectFiles(button, mipmapName, context, layoutFile)
    } catch (e: Exception) {
      // log.*
      return false
    }
  }

  private fun loadAndroidDrawableIcon(
      button: MaterialButton,
      resourceName: String,
      context: Context,
  ): Boolean {
    return try {
      val drawableName = resourceName.substringAfter("@android:drawable/")
      // log.*

      val resourceId = context.resources.getIdentifier(drawableName, "drawable", "android")
      // log.*

      if (resourceId != 0) {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        // log.*
        if (drawable != null) {
          button.icon = drawable
          // log.*
          return true
        }
      }

      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun loadIconFromProjectFiles(
      button: MaterialButton,
      iconName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val projectRoot = findProjectRoot(layoutFile)
    if (projectRoot == null) {
      // log.*
      return false
    }

    // log.*

    val drawableFolders =
        listOf(
            "src/main/res/drawable",
            "src/main/res/drawable-hdpi",
            "src/main/res/drawable-mdpi",
            "src/main/res/drawable-xhdpi",
            "src/main/res/drawable-xxhdpi",
            "src/main/res/drawable-xxxhdpi",
            "app/src/main/res/drawable",
            "app/src/main/res/drawable-hdpi",
            "app/src/main/res/drawable-mdpi",
            "app/src/main/res/drawable-xhdpi",
            "app/src/main/res/drawable-xxhdpi",
            "app/src/main/res/drawable-xxxhdpi",
        )

    return loadIconFromFolders(button, iconName, projectRoot, drawableFolders, context, "drawable")
  }

  private fun loadMipmapFromProjectFiles(
      button: MaterialButton,
      iconName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val projectRoot = findProjectRoot(layoutFile) ?: return false

    val mipmapFolders =
        listOf(
            "src/main/res/mipmap",
            "src/main/res/mipmap-hdpi",
            "src/main/res/mipmap-mdpi",
            "src/main/res/mipmap-xhdpi",
            "src/main/res/mipmap-xxhdpi",
            "src/main/res/mipmap-xxxhdpi",
            "app/src/main/res/mipmap",
            "app/src/main/res/mipmap-hdpi",
            "app/src/main/res/mipmap-mdpi",
            "app/src/main/res/mipmap-xhdpi",
            "app/src/main/res/mipmap-xxhdpi",
            "app/src/main/res/mipmap-xxxhdpi",
        )

    return loadIconFromFolders(button, iconName, projectRoot, mipmapFolders, context, "mipmap")
  }

  private fun loadIconFromFolders(
      button: MaterialButton,
      iconName: String,
      projectRoot: File,
      folders: List<String>,
      context: Context,
      type: String,
  ): Boolean {
    val extensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".xml")

    // log.*

    for (folder in folders) {
      val resourceDir = File(projectRoot, folder)
      // log.*

      if (!resourceDir.exists()) {
        // log.*
        continue
      }

      for (ext in extensions) {
        val iconFile = File(resourceDir, "$iconName$ext")
        // log.*

        if (iconFile.exists() && iconFile.canRead()) {
          // log.*

          try {
            if (ext == ".xml") {
              // log.*
              if (loadXmlDrawableIcon(button, iconFile, context)) {
                return true
              }
            } else {
              // log.*
              val bitmap = BitmapFactory.decodeFile(iconFile.absolutePath)
              if (bitmap != null) {
                val drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                button.icon = drawable
                // log.*
                return true
              }
            }
          } catch (e: Exception) {
            // log.*
          }
        }
      }
    }

    // log.*
    return false
  }

  private fun loadXmlDrawableIcon(
      button: MaterialButton,
      xmlFile: File,
      context: Context,
  ): Boolean {
    return try {
      // log.*

      // Try to create drawable from XML
      val inputStream = xmlFile.inputStream()
      val drawable = Drawable.createFromStream(inputStream, xmlFile.name)
      inputStream.close()

      if (drawable != null) {
        button.icon = drawable
        // log.*
        return true
      }

      // Fallback: Create a colored placeholder
      val placeholderDrawable = ColorDrawable(Color.parseColor("#757575"))
      button.icon = placeholderDrawable
      // log.*
      return true
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  /** FIXED: Apply background tint with better error handling */
  private fun applyBackgroundTintPreview(
      button: MaterialButton,
      tintValue: String,
      context: Context,
  ): Boolean {
    // log.*

    return try {
      val color = parseColorValue(tintValue, context)
      // log.*

      if (color != null) {
        val colorStateList = ColorStateList.valueOf(color)
        button.backgroundTintList = colorStateList
        // log.*
        return true
      }

      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  /** FIXED: Apply corner radius with better error handling */
  private fun applyCornerRadiusPreview(
      button: MaterialButton,
      radiusValue: String,
      context: Context,
  ): Boolean {
    // log.*

    return try {
      val radius = parseDimensionValue(radiusValue, context)
      // log.*

      if (radius >= 0) {
        button.cornerRadius = radius
        // log.*
        return true
      }

      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  // Other attribute methods with similar fixes...
  private fun applyIconSizePreview(
      button: MaterialButton,
      sizeValue: String,
      context: Context,
  ): Boolean {
    return try {
      val size = parseDimensionValue(sizeValue, context)
      if (size > 0) {
        button.iconSize = size
        // log.*
        return true
      }
      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun applyIconTintPreview(
      button: MaterialButton,
      tintValue: String,
      context: Context,
  ): Boolean {
    return try {
      val color = parseColorValue(tintValue, context)
      if (color != null) {
        button.iconTint = ColorStateList.valueOf(color)
        // log.*
        return true
      }
      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun applyIconGravityPreview(button: MaterialButton, gravityValue: String): Boolean {
    return try {
      val gravity =
          when (gravityValue.lowercase()) {
            "start" -> MaterialButton.ICON_GRAVITY_START
            "end" -> MaterialButton.ICON_GRAVITY_END
            "top" -> MaterialButton.ICON_GRAVITY_TOP
            "textstart" -> MaterialButton.ICON_GRAVITY_TEXT_START
            "textend" -> MaterialButton.ICON_GRAVITY_TEXT_END
            "texttop" -> MaterialButton.ICON_GRAVITY_TEXT_TOP
            else -> return false
          }

      button.iconGravity = gravity
      // log.*
      true
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun applyIconPaddingPreview(
      button: MaterialButton,
      paddingValue: String,
      context: Context,
  ): Boolean {
    return try {
      val padding = parseDimensionValue(paddingValue, context)
      if (padding >= 0) {
        button.iconPadding = padding
        // log.*
        return true
      }
      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun applyStrokeWidthPreview(
      button: MaterialButton,
      widthValue: String,
      context: Context,
  ): Boolean {
    return try {
      val width = parseDimensionValue(widthValue, context)
      if (width >= 0) {
        button.strokeWidth = width
        // log.*
        return true
      }
      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun applyStrokeColorPreview(
      button: MaterialButton,
      colorValue: String,
      context: Context,
  ): Boolean {
    return try {
      val color = parseColorValue(colorValue, context)
      if (color != null) {
        button.strokeColor = ColorStateList.valueOf(color)
        // log.*
        return true
      }
      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  private fun applyRippleColorPreview(
      button: MaterialButton,
      colorValue: String,
      context: Context,
  ): Boolean {
    return try {
      val color = parseColorValue(colorValue, context)
      if (color != null) {
        button.rippleColor = ColorStateList.valueOf(color)
        // log.*
        return true
      }
      false
    } catch (e: Exception) {
      // log.*
      false
    }
  }

  /** FIXED: Parse dimension value with better error handling and malformed input support */
  private fun parseDimensionValue(value: String, context: Context): Int {
    val trimmed = value.trim()
    // log.*

    return try {
      when {
        trimmed.endsWith("dp") -> {
          val dpValue = trimmed.removeSuffix("dp").toFloat()
          val result = (dpValue * context.resources.displayMetrics.density).toInt()
          // log.*
          result
        }
        trimmed.endsWith("d") -> {
          // Handle malformed "40d" as "40dp"
          val dpValue = trimmed.removeSuffix("d").toFloat()
          val result = (dpValue * context.resources.displayMetrics.density).toInt()
          // log.*
          result
        }
        trimmed.endsWith("sp") -> {
          val spValue = trimmed.removeSuffix("sp").toFloat()
          val result = (spValue * context.resources.displayMetrics.scaledDensity).toInt()
          // log.*
          result
        }
        trimmed.endsWith("px") -> {
          val result = trimmed.removeSuffix("px").toInt()
          // log.*
          result
        }
        trimmed.matches(Regex("\\d+(\\.\\d+)?")) -> {
          // Plain number, assume dp
          val dpValue = trimmed.toFloat()
          val result = (dpValue * context.resources.displayMetrics.density).toInt()
          // log.*
          result
        }
        else -> {
          // log.*
          0
        }
      }
    } catch (e: Exception) {
      // log.*
      0
    }
  }

  /** FIXED: Parse color value with better error handling */
  private fun parseColorValue(value: String, context: Context): Int? {
    val trimmed = value.trim()
    // log.*

    return try {
      when {
        trimmed.startsWith("#") -> {
          val color = Color.parseColor(trimmed)
          // log.*
          color
        }
        trimmed.startsWith("@color/") -> {
          val colorName = trimmed.substringAfter("@color/")
          // log.*

          var resourceId = context.resources.getIdentifier(colorName, "color", context.packageName)
          if (resourceId == 0) {
            resourceId =
                context.applicationContext.resources.getIdentifier(
                    colorName,
                    "color",
                    context.applicationContext.packageName,
                )
          }

          if (resourceId != 0) {
            val color = ContextCompat.getColor(context, resourceId)
            // log.*
            color
          } else {
            // log.*
            null
          }
        }
        trimmed.startsWith("@android:color/") -> {
          val colorName = trimmed.substringAfter("@android:color/")
          val resourceId = context.resources.getIdentifier(colorName, "color", "android")
          if (resourceId != 0) {
            val color = ContextCompat.getColor(context, resourceId)
            // log.*
            color
          } else {
            null
          }
        }
        else -> {
          // Try to parse as direct color value
          val color = Color.parseColor(trimmed)
          // log.*
          color
        }
      }
    } catch (e: Exception) {
      // log.*
      null
    }
  }

  /** Find project root - same logic as other renderers */
  private fun findProjectRoot(layoutFile: File?): File? {
    if (layoutFile == null) return null

    var current = layoutFile.parentFile

    for (i in 0..10) {
      if (current == null) break

      val hasGradleBuild =
          File(current, "build.gradle").exists() || File(current, "build.gradle.kts").exists()
      val hasAppModule = File(current, "app").exists()
      val hasSrcMain = File(current, "src/main").exists()
      val hasSettings =
          File(current, "settings.gradle").exists() || File(current, "settings.gradle.kts").exists()

      if (hasSrcMain && !hasAppModule && !hasSettings) {
        current = current.parentFile
        continue
      }

      if (hasGradleBuild || hasAppModule || hasSettings) {
        // log.*
        return current
      }

      current = current.parentFile
    }

    // Fallback logic
    current = layoutFile.parentFile
    for (i in 0..10) {
      if (current == null) break

      if (current.name == "layout" && current.parentFile?.name == "res") {
        val resDir = current.parentFile
        val mainDir = resDir?.parentFile
        val srcDir = mainDir?.parentFile
        val moduleRoot = srcDir?.parentFile

        if (moduleRoot != null) {
          // log.*
          return moduleRoot
        }
      }

      current = current.parentFile
    }

    // log.*
    return null
  }
}
