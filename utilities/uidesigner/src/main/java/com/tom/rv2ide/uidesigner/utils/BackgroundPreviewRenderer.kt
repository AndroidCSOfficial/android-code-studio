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

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Helper class to apply background previews in the UI Designer This loads actual image files and
 * XML vectors from Android resource folders
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class BackgroundPreviewRenderer {

  companion object {
    private val log = LoggerFactory.getLogger(BackgroundPreviewRenderer::class.java)
  }

  /** Apply background preview to a view in the designer */
  fun applyBackgroundPreview(
      view: View,
      backgroundValue: String,
      context: Context,
      layoutFile: File? = null,
  ): Boolean {
    val value = backgroundValue.trim()

    try {
      when {
        value.isEmpty() -> return false
        value == "transparent" -> {
          view.background = ColorDrawable(Color.TRANSPARENT)
          return true
        }
        value.startsWith("#") -> {
          val color = Color.parseColor(value)
          view.background = ColorDrawable(color)
          return true
        }
        value.startsWith("@drawable/") -> {
          return loadDrawableResource(view, value, context, layoutFile)
        }
        value.startsWith("@mipmap/") -> {
          return loadMipmapResource(view, value, context, layoutFile)
        }
        value.startsWith("@color/") -> {
          return loadColorResource(view, value, context)
        }
        value.startsWith("@android:drawable/") -> {
          return loadAndroidDrawable(view, value, context)
        }
        value.startsWith("@android:color/") -> {
          return loadAndroidColor(view, value, context)
        }
        else -> {
          try {
            val color = Color.parseColor(value)
            view.background = ColorDrawable(color)
            return true
          } catch (e: Exception) {
            return false
          }
        }
      }
    } catch (e: Exception) {
      log.debug("Could not preview background '{}': {}", value, e.message)
      return false
    }
  }

  private fun loadDrawableResource(
      view: View,
      resourceName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val drawableName = resourceName.substringAfter("@drawable/")

    // Try standard resource loading first
    var resourceId = context.resources.getIdentifier(drawableName, "drawable", context.packageName)
    if (resourceId == 0) {
      resourceId =
          context.applicationContext.resources.getIdentifier(
              drawableName,
              "drawable",
              context.applicationContext.packageName,
          )
    }

    if (resourceId != 0) {
      val drawable = ContextCompat.getDrawable(context, resourceId)
      if (drawable != null) {
        view.background = drawable
        return true
      }
    }

    // Try loading from res/drawable folders directly
    return loadFromResDrawable(view, drawableName, context, layoutFile)
  }

  private fun loadMipmapResource(
      view: View,
      resourceName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val mipmapName = resourceName.substringAfter("@mipmap/")

    // Try standard resource loading first
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
        view.background = drawable
        return true
      }
    }

    // Try loading from res/mipmap folders directly
    return loadFromResMipmap(view, mipmapName, context, layoutFile)
  }

  private fun loadFromResDrawable(
      view: View,
      drawableName: String,
      context: Context,
      layoutFile: File?,
  ): Boolean {
    val projectRoot = findProjectRoot(layoutFile) ?: return false

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

    return loadImageFromFolders(view, drawableName, projectRoot, drawableFolders, context)
  }

  private fun loadFromResMipmap(
      view: View,
      mipmapName: String,
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

    return loadImageFromFolders(view, mipmapName, projectRoot, mipmapFolders, context)
  }

  private fun loadImageFromFolders(
      view: View,
      imageName: String,
      projectRoot: File,
      folders: List<String>,
      context: Context,
  ): Boolean {
    val extensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".xml")

    for (folder in folders) {
      val resourceDir = File(projectRoot, folder)
      if (!resourceDir.exists()) continue

      for (ext in extensions) {
        val imageFile = File(resourceDir, "$imageName$ext")
        if (imageFile.exists() && imageFile.canRead()) {
          try {
            // Handle XML drawables (vectors, shapes, etc.)
            if (ext == ".xml") {
              log.debug("Attempting to load XML drawable: {}", imageFile.absolutePath)
              if (loadXmlDrawable(view, imageFile, context)) {
                return true
              }
              continue
            }

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap != null) {
              view.background = BitmapDrawable(context.resources, bitmap)
              log.debug("Loaded image: {}", imageFile.absolutePath)
              return true
            }
          } catch (e: Exception) {
            log.debug("Error loading image {}: {}", imageFile.absolutePath, e.message)
          }
        }
      }
    }

    return false
  }

  /** Load XML drawable files (vectors, shapes, gradients, etc.) */
  private fun loadXmlDrawable(view: View, xmlFile: File, context: Context): Boolean {
    try {
      // First, try using VectorDrawableCompat for vector drawables
      if (isVectorDrawable(xmlFile)) {
        return loadVectorDrawable(view, xmlFile, context)
      }

      // Try parsing as shape drawable
      if (isShapeDrawable(xmlFile)) {
        return loadShapeDrawable(view, xmlFile, context)
      }

      // Try parsing as layer-list or other drawable types
      return loadGenericXmlDrawable(view, xmlFile, context)
    } catch (e: Exception) {
      log.debug("Error loading XML drawable {}: {}", xmlFile.absolutePath, e.message)
      return false
    }
  }

  /** Check if XML file is a vector drawable */
  private fun isVectorDrawable(xmlFile: File): Boolean {
    try {
      val factory = XmlPullParserFactory.newInstance()
      val parser = factory.newPullParser()
      parser.setInput(FileInputStream(xmlFile), "UTF-8")

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          return parser.name == "vector"
        }
        eventType = parser.next()
      }
    } catch (e: Exception) {
      // Ignore parsing errors
    }
    return false
  }

  /** Check if XML file is a shape drawable */
  private fun isShapeDrawable(xmlFile: File): Boolean {
    try {
      val factory = XmlPullParserFactory.newInstance()
      val parser = factory.newPullParser()
      parser.setInput(FileInputStream(xmlFile), "UTF-8")

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          return parser.name == "shape"
        }
        eventType = parser.next()
      }
    } catch (e: Exception) {
      // Ignore parsing errors
    }
    return false
  }

  /** Load vector drawable using VectorDrawableCompat */
  private fun loadVectorDrawable(view: View, xmlFile: File, context: Context): Boolean {
    return try {
      // Create a temporary resource entry
      val packageName = context.packageName
      val drawableName = xmlFile.nameWithoutExtension

      // Try to inflate using the system drawable inflater
      val inputStream = FileInputStream(xmlFile)
      val drawable = Drawable.createFromStream(inputStream, xmlFile.name)

      if (drawable != null) {
        view.background = drawable
        log.debug("SUCCESS: Loaded with createFromStream: {}", xmlFile.name)
        return true
      }

      // Fallback: Just create a colored rectangle as a preview indicator
      // This at least shows that the XML was found and recognized
      // Fallback: Extract gradient info and create proper gradient
      val gradientInfo = extractGradientFromXml(xmlFile)
      if (gradientInfo != null) {
        val gradientDrawable =
            GradientDrawable().apply {
              colors = gradientInfo.colors
              orientation = gradientInfo.orientation
              shape = GradientDrawable.RECTANGLE

              // Apply corner radius
              if (gradientInfo.cornerRadius > 0f) {
                cornerRadius = gradientInfo.cornerRadius
              } else if (
                  gradientInfo.topLeftRadius > 0f ||
                      gradientInfo.topRightRadius > 0f ||
                      gradientInfo.bottomLeftRadius > 0f ||
                      gradientInfo.bottomRightRadius > 0f
              ) {
                cornerRadii =
                    floatArrayOf(
                        gradientInfo.topLeftRadius,
                        gradientInfo.topLeftRadius, // top-left
                        gradientInfo.topRightRadius,
                        gradientInfo.topRightRadius, // top-right
                        gradientInfo.bottomRightRadius,
                        gradientInfo.bottomRightRadius, // bottom-right
                        gradientInfo.bottomLeftRadius,
                        gradientInfo.bottomLeftRadius, // bottom-left
                    )
              }
            }
        view.background = gradientDrawable
        log.debug("SUCCESS: Created gradient from XML: {}", xmlFile.name)
        return true
      }

      // Final fallback: Green placeholder
      val fallbackDrawable =
          GradientDrawable().apply {
            setColor(Color.parseColor("#4CAF50"))
            cornerRadius = 8f
          }
      view.background = fallbackDrawable
      log.debug("FALLBACK: Created placeholder for XML: {}", xmlFile.name)
      return true
    } catch (e: Exception) {
      log.debug("Vector loading completely failed: {}", e.message)
      return false
    }
  }

  data class GradientInfo(
      val colors: IntArray,
      val orientation: GradientDrawable.Orientation,
      val cornerRadius: Float = 0f,
      val topLeftRadius: Float = 0f,
      val topRightRadius: Float = 0f,
      val bottomLeftRadius: Float = 0f,
      val bottomRightRadius: Float = 0f,
  )

  private fun extractGradientFromXml(xmlFile: File): GradientInfo? {
    try {
      val content = xmlFile.readText()

      // Extract colors (existing code)
      val colorRegex = """(startColor|endColor|centerColor)="(#[0-9A-Fa-f]{6,8})"""".toRegex()
      val colorMatches = colorRegex.findAll(content).toList()

      // Extract angle (existing code)
      val angleRegex = """angle="(\d+)"""".toRegex()
      val angleMatch = angleRegex.find(content)
      val angle = angleMatch?.groupValues?.get(1)?.toFloatOrNull() ?: 0f

      // Extract corner radius values
      val radiusRegex =
          """(radius|topLeftRadius|topRightRadius|bottomLeftRadius|bottomRightRadius)="(\d+(?:\.\d+)?)(dp|px)?""""
              .toRegex()
      val radiusMatches = radiusRegex.findAll(content).toList()

      var cornerRadius = 0f
      var topLeftRadius = 0f
      var topRightRadius = 0f
      var bottomLeftRadius = 0f
      var bottomRightRadius = 0f

      radiusMatches.forEach { match ->
        val radiusType = match.groupValues[1]
        val radiusValue = match.groupValues[2].toFloatOrNull() ?: 0f

        when (radiusType) {
          "radius" -> cornerRadius = radiusValue
          "topLeftRadius" -> topLeftRadius = radiusValue
          "topRightRadius" -> topRightRadius = radiusValue
          "bottomLeftRadius" -> bottomLeftRadius = radiusValue
          "bottomRightRadius" -> bottomRightRadius = radiusValue
        }
      }

      // Build colors array (existing code)
      if (colorMatches.isNotEmpty()) {
        val colors = mutableListOf<Int>()

        colorMatches
            .find { it.groupValues[1] == "startColor" }
            ?.let { colors.add(Color.parseColor(it.groupValues[2])) }
        colorMatches
            .find { it.groupValues[1] == "centerColor" }
            ?.let { colors.add(Color.parseColor(it.groupValues[2])) }
        colorMatches
            .find { it.groupValues[1] == "endColor" }
            ?.let { colors.add(Color.parseColor(it.groupValues[2])) }

        if (colors.size >= 2) {
          return GradientInfo(
              colors = colors.toIntArray(),
              orientation = getGradientOrientation(angle),
              cornerRadius = cornerRadius,
              topLeftRadius = topLeftRadius,
              topRightRadius = topRightRadius,
              bottomLeftRadius = bottomLeftRadius,
              bottomRightRadius = bottomRightRadius,
          )
        }
      }
    } catch (e: Exception) {
      log.debug("Failed to extract gradient: {}", e.message)
    }
    return null
  }

  /** Load shape drawable by parsing XML */
  private fun loadShapeDrawable(view: View, xmlFile: File, context: Context): Boolean {
    try {
      val drawable = parseShapeDrawable(xmlFile)
      if (drawable != null) {
        view.background = drawable
        log.debug("Successfully loaded shape drawable: {}", xmlFile.absolutePath)
        return true
      }
    } catch (e: Exception) {
      log.debug("Failed to load shape drawable {}: {}", xmlFile.absolutePath, e.message)
    }
    return false
  }

  /** Parse shape drawable from XML file */
  private fun parseShapeDrawable(xmlFile: File): GradientDrawable? {
    try {
      val factory = XmlPullParserFactory.newInstance()
      val parser = factory.newPullParser()
      parser.setInput(FileInputStream(xmlFile), "UTF-8")

      val drawable = GradientDrawable()
      var currentTag = ""

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          XmlPullParser.START_TAG -> {
            currentTag = parser.name
            when (currentTag) {
              "shape" -> {
                val shape = parser.getAttributeValue(null, "android:shape")
                when (shape) {
                  "rectangle" -> drawable.shape = GradientDrawable.RECTANGLE
                  "oval" -> drawable.shape = GradientDrawable.OVAL
                  "ring" -> drawable.shape = GradientDrawable.RING
                  "line" -> drawable.shape = GradientDrawable.LINE
                  else -> drawable.shape = GradientDrawable.RECTANGLE
                }
              }
              "solid" -> {
                val color = parser.getAttributeValue(null, "android:color")
                if (color != null) {
                  drawable.setColor(parseColor(color))
                }
              }
              "gradient" -> {
                parseGradient(parser, drawable)
              }
              "corners" -> {
                val radius = parser.getAttributeValue(null, "android:radius")
                if (radius != null) {
                  val radiusValue = parseDimension(radius)
                  drawable.cornerRadius = radiusValue
                }
              }
              "stroke" -> {
                val width = parser.getAttributeValue(null, "android:width")
                val color = parser.getAttributeValue(null, "android:color")
                if (width != null && color != null) {
                  drawable.setStroke(parseDimension(width).toInt(), parseColor(color))
                }
              }
            }
          }
        }
        eventType = parser.next()
      }

      return drawable
    } catch (e: Exception) {
      log.debug("Error parsing shape drawable: {}", e.message)
      return null
    }
  }

  /** Parse gradient attributes */
  private fun parseGradient(parser: XmlPullParser, drawable: GradientDrawable) {
    try {
      val startColor = parser.getAttributeValue(null, "android:startColor")
      val endColor = parser.getAttributeValue(null, "android:endColor")
      val centerColor = parser.getAttributeValue(null, "android:centerColor")
      val angle = parser.getAttributeValue(null, "android:angle")
      val type = parser.getAttributeValue(null, "android:type")

      val colors = mutableListOf<Int>()

      if (startColor != null) {
        colors.add(parseColor(startColor))
      }
      if (centerColor != null) {
        colors.add(parseColor(centerColor))
      }
      if (endColor != null) {
        colors.add(parseColor(endColor))
      }

      if (colors.isNotEmpty()) {
        drawable.colors = colors.toIntArray()
      }

      // Set gradient type
      when (type) {
        "radial" -> drawable.gradientType = GradientDrawable.RADIAL_GRADIENT
        "sweep" -> drawable.gradientType = GradientDrawable.SWEEP_GRADIENT
        else -> drawable.gradientType = GradientDrawable.LINEAR_GRADIENT
      }

      // Set orientation based on angle
      if (angle != null) {
        val angleValue = angle.toFloatOrNull() ?: 0f
        drawable.orientation = getGradientOrientation(angleValue)
      }
    } catch (e: Exception) {
      log.debug("Error parsing gradient: {}", e.message)
    }
  }

  /** Get gradient orientation from angle */
  private fun getGradientOrientation(angle: Float): GradientDrawable.Orientation {
    return when ((angle % 360).toInt()) {
      0 -> GradientDrawable.Orientation.LEFT_RIGHT
      45 -> GradientDrawable.Orientation.BL_TR
      90 -> GradientDrawable.Orientation.BOTTOM_TOP
      135 -> GradientDrawable.Orientation.BR_TL
      180 -> GradientDrawable.Orientation.RIGHT_LEFT
      225 -> GradientDrawable.Orientation.TR_BL
      270 -> GradientDrawable.Orientation.TOP_BOTTOM
      315 -> GradientDrawable.Orientation.TL_BR
      else -> GradientDrawable.Orientation.LEFT_RIGHT
    }
  }

  /** Parse color string to int */
  private fun parseColor(colorString: String): Int {
    return try {
      if (colorString.startsWith("@color/")) {
        // For now, return a default color for resource references
        // In a full implementation, you'd resolve this from colors.xml
        Color.GRAY
      } else {
        Color.parseColor(colorString)
      }
    } catch (e: Exception) {
      Color.TRANSPARENT
    }
  }

  /** Parse dimension string to float (simplified) */
  private fun parseDimension(dimensionString: String): Float {
    return try {
      when {
        dimensionString.endsWith("dp") -> {
          dimensionString.removeSuffix("dp").toFloat()
        }
        dimensionString.endsWith("px") -> {
          dimensionString.removeSuffix("px").toFloat()
        }
        else -> {
          dimensionString.toFloat()
        }
      }
    } catch (e: Exception) {
      0f
    }
  }

  /** Try to load generic XML drawable using Android's inflation system */
  private fun loadGenericXmlDrawable(view: View, xmlFile: File, context: Context): Boolean {
    try {
      // Try to create a temporary resource and inflate it
      // This is a fallback for other drawable types like layer-list, selector, etc.

      // For now, we'll create a placeholder drawable to indicate XML was found
      // but not fully parsed - this can be enhanced based on specific needs
      val placeholder = ColorDrawable(Color.parseColor("#E0E0E0"))
      view.background = placeholder
      log.debug("Loaded placeholder for XML drawable: {}", xmlFile.absolutePath)
      return true
    } catch (e: Exception) {
      log.debug("Failed to load generic XML drawable {}: {}", xmlFile.absolutePath, e.message)
      return false
    }
  }

  /** Find project root based on the layout file path */
  private fun findProjectRoot(layoutFile: File?): File? {
    if (layoutFile == null) return null

    var current = layoutFile.parentFile

    // Navigate up from the layout file to find project root
    for (i in 0..10) {
      if (current == null) break

      // Look for typical Android project structure indicators
      val hasGradleBuild =
          File(current, "build.gradle").exists() || File(current, "build.gradle.kts").exists()
      val hasAppModule = File(current, "app").exists()
      val hasSrcMain = File(current, "src/main").exists()
      val hasSettings =
          File(current, "settings.gradle").exists() || File(current, "settings.gradle.kts").exists()

      // If we're in a module directory (has src/main), go up one more level
      if (hasSrcMain && !hasAppModule && !hasSettings) {
        current = current.parentFile
        continue
      }

      // Found project root
      if (hasGradleBuild || hasAppModule || hasSettings) {
        log.debug("Found project root: {}", current.absolutePath)
        return current
      }

      current = current.parentFile
    }

    // Fallback: try to find based on common Android project patterns
    current = layoutFile.parentFile
    for (i in 0..10) {
      if (current == null) break

      // Check if we're in res/layout and navigate to module root
      if (current.name == "layout" && current.parentFile?.name == "res") {
        val resDir = current.parentFile
        val mainDir = resDir?.parentFile
        val srcDir = mainDir?.parentFile
        val moduleRoot = srcDir?.parentFile

        if (moduleRoot != null) {
          log.debug("Found module root via res/layout path: {}", moduleRoot.absolutePath)
          return moduleRoot
        }
      }

      current = current.parentFile
    }

    log.warn("Could not find project root for layout file: {}", layoutFile.absolutePath)
    return null
  }

  private fun loadColorResource(view: View, resourceName: String, context: Context): Boolean {
    return try {
      val colorName = resourceName.substringAfter("@color/")
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
        view.background = ColorDrawable(color)
        return true
      }

      false
    } catch (e: Exception) {
      false
    }
  }

  private fun loadAndroidDrawable(view: View, resourceName: String, context: Context): Boolean {
    return try {
      val drawableName = resourceName.substringAfter("@android:drawable/")
      val resourceId = context.resources.getIdentifier(drawableName, "drawable", "android")

      if (resourceId != 0) {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable != null) {
          view.background = drawable
          return true
        }
      }

      false
    } catch (e: Exception) {
      false
    }
  }

  private fun loadAndroidColor(view: View, resourceName: String, context: Context): Boolean {
    return try {
      val colorName = resourceName.substringAfter("@android:color/")
      val resourceId = context.resources.getIdentifier(colorName, "color", "android")

      if (resourceId != 0) {
        val color = ContextCompat.getColor(context, resourceId)
        view.background = ColorDrawable(color)
        return true
      }

      false
    } catch (e: Exception) {
      false
    }
  }
}

/** Enhanced background completion */
object BackgroundCompletions {

  val COMMON_BACKGROUNDS =
      listOf(
          // Solid colors
          "#FFFFFF",
          "#000000",
          "#808080",
          "#FF0000",
          "#00FF00",
          "#0000FF",
          "#FFFF00",
          "#FF00FF",
          "#00FFFF",
          "#FFC0CB",
          "#FFA500",
          "#800080",

          // Transparent
          "transparent",
          "@android:color/transparent",

          // Common Android backgrounds
          "@android:drawable/btn_default",
          "@android:drawable/list_selector_background",
          "@android:color/white",
          "@android:color/black",
          "@android:color/darker_gray",

          // Resource references
          "@drawable/",
          "@color/",
          "@mipmap/",
      )

  fun getBackgroundSuggestions(currentText: String): List<String> {
    return when {
      currentText.startsWith("#") -> getColorSuggestions(currentText)
      currentText.startsWith("@drawable/") -> listOf("@drawable/")
      currentText.startsWith("@color/") -> listOf("@color/")
      currentText.startsWith("@mipmap/") -> listOf("@mipmap/")
      else -> COMMON_BACKGROUNDS.filter { it.contains(currentText, ignoreCase = true) }.take(10)
    }
  }

  private fun getColorSuggestions(current: String): List<String> {
    return when (current.length) {
      1 -> listOf("#FFFFFF", "#000000", "#FF0000", "#00FF00", "#0000FF")
      2,
      3,
      4,
      5,
      6 -> {
        val suggestions = mutableListOf<String>()
        if (current.length < 7) {
          suggestions.add(current + "0".repeat(7 - current.length))
          suggestions.add(current + "F".repeat(7 - current.length))
        }
        suggestions.add(current)
        suggestions
      }
      else -> listOf(current)
    }
  }
}
