/*
 * Enhanced Vector Drawable Renderer for AndroidIDE UI Designer
 * This version uses Android's native VectorDrawable capabilities
 * and provides better fallback mechanisms for complex vector drawables
 */

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import java.io.File
import java.io.FileInputStream
import java.io.StringReader
import kotlin.math.min
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Renderer for vector drawables that tries multiple approaches
 * 1. Native Android resource loading (if possible)
 * 2. VectorDrawableCompat with XML string manipulation
 * 3. Custom path rendering as fallback
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class VectorDrawableRenderer {

  companion object {
    private val log = LoggerFactory.getLogger(VectorDrawableRenderer::class.java)
  }

  /** Apply vector drawable preview to any view (background or ImageView src) */
  fun applyVectorPreview(
      view: View,
      vectorValue: String,
      context: Context,
      layoutFile: File? = null,
      isBackground: Boolean = true,
  ): Boolean {
    val value = vectorValue.trim()

    try {
      when {
        value.isEmpty() -> return false
        value.startsWith("@drawable/") -> {
          return loadVectorDrawable(view, value, context, layoutFile, isBackground)
        }
        else -> {
          // Try to parse as resource name without prefix
          return loadVectorDrawable(view, "@drawable/$value", context, layoutFile, isBackground)
        }
      }
    } catch (e: Exception) {
      log.debug("Could not preview vector '{}': {}", value, e.message)
      return false
    }
  }

  private fun loadVectorDrawable(
      view: View,
      resourceName: String,
      context: Context,
      layoutFile: File?,
      isBackground: Boolean,
  ): Boolean {
    val drawableName = resourceName.substringAfter("@drawable/")

    // Method 1: Try to load from Android resources (if it exists in the system)
    try {
      val resourceId =
          context.resources.getIdentifier(drawableName, "drawable", context.packageName)
      if (resourceId != 0) {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable != null) {
          applyDrawableToView(view, drawable, isBackground)
          log.debug("Successfully loaded vector drawable from resources: {}", drawableName)
          return true
        }
      }
    } catch (e: Exception) {
      log.debug("Could not load from resources: {}", e.message)
    }

    // Method 2: Try to find and load the vector file directly
    val projectRoot = findProjectRoot(layoutFile) ?: return false
    val vectorFile = findVectorFile(projectRoot, drawableName)

    if (vectorFile != null && isVectorDrawable(vectorFile)) {
      log.debug("Found vector drawable file: {}", vectorFile.absolutePath)

      // Method 2a: Try VectorDrawableCompat with manipulated XML
      if (tryVectorDrawableCompat(view, vectorFile, context, isBackground)) {
        return true
      }

      // Method 2b: Try custom path rendering as fallback
      if (tryCustomPathRendering(view, vectorFile, context, isBackground)) {
        return true
      }
    }

    return false
  }

  /** Try to use VectorDrawableCompat by manipulating the XML content */
  private fun tryVectorDrawableCompat(
      view: View,
      vectorFile: File,
      context: Context,
      isBackground: Boolean,
  ): Boolean {
    try {
      val xmlContent = vectorFile.readText()

      // Create a modified XML content that VectorDrawableCompat can handle
      val modifiedXml = preprocessVectorXml(xmlContent)

      // Try to create VectorDrawableCompat from the XML
      val factory = XmlPullParserFactory.newInstance()
      val parser = factory.newPullParser()
      parser.setInput(StringReader(modifiedXml))

      val drawable = VectorDrawableCompat.createFromXml(context.resources, parser)
      if (drawable != null) {
        applyDrawableToView(view, drawable, isBackground)
        log.debug("Successfully created VectorDrawableCompat from: {}", vectorFile.name)
        return true
      }
    } catch (e: Exception) {
      log.debug("VectorDrawableCompat failed for {}: {}", vectorFile.name, e.message)
    }

    return false
  }

  /** Preprocess vector XML to make it more compatible with VectorDrawableCompat */
  private fun preprocessVectorXml(xmlContent: String): String {
    var modified = xmlContent

    // Ensure proper XML declaration
    if (!modified.contains("<?xml")) {
      modified =
          """<?xml version="1.0" encoding="utf-8"?>
$modified"""
    }

    // Fix common issues with vector XML
    modified = modified.replace("android:fillType=\"evenOdd\"", "android:fillType=\"nonZero\"")

    // Ensure all paths have proper fillColor if missing
    if (modified.contains("<path") && !modified.contains("android:fillColor")) {
      modified = modified.replace("<path ", "<path android:fillColor=\"#FF000000\" ")
    }

    return modified
  }

  /** Try custom path rendering (improved version) */
  private fun tryCustomPathRendering(
      view: View,
      vectorFile: File,
      context: Context,
      isBackground: Boolean,
  ): Boolean {
    try {
      val vectorData = parseVectorData(vectorFile)
      if (vectorData == null) {
        log.debug("Failed to parse vector data from: {}", vectorFile.absolutePath)
        return false
      }

      val drawable = createVectorDrawable(vectorData, context)
      if (drawable == null) {
        log.debug("Failed to create drawable from vector data")
        return false
      }

      applyDrawableToView(view, drawable, isBackground)
      log.debug("Successfully applied custom rendered vector drawable: {}", vectorFile.name)
      return true
    } catch (e: Exception) {
      log.debug("Custom path rendering failed for {}: {}", vectorFile.absolutePath, e.message)
      return false
    }
  }

  /** Apply drawable to view based on whether it's background or ImageView src */
  private fun applyDrawableToView(view: View, drawable: Drawable, isBackground: Boolean) {
    if (isBackground) {
      view.background = drawable
    } else if (view is ImageView) {
      view.setImageDrawable(drawable)
    }
  }

  /** Find vector file in project drawable folders */
  private fun findVectorFile(projectRoot: File, drawableName: String): File? {
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

    for (folder in drawableFolders) {
      val resourceDir = File(projectRoot, folder)
      if (!resourceDir.exists()) continue

      val vectorFile = File(resourceDir, "$drawableName.xml")
      if (vectorFile.exists() && vectorFile.canRead()) {
        return vectorFile
      }
    }

    return null
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
      log.debug("Error checking if file is vector drawable: {}", e.message)
    }
    return false
  }

  /** Enhanced vector data parsing with better error handling */
  private fun parseVectorData(vectorFile: File): VectorData? {
    try {
      val factory = XmlPullParserFactory.newInstance()
      val parser = factory.newPullParser()
      parser.setInput(FileInputStream(vectorFile), "UTF-8")

      var vectorData: VectorData? = null
      val paths = mutableListOf<PathData>()
      val groups = mutableListOf<GroupData>()
      var currentGroup: GroupData? = null

      var eventType = parser.eventType
      while (eventType != XmlPullParser.END_DOCUMENT) {
        when (eventType) {
          XmlPullParser.START_TAG -> {
            when (parser.name) {
              "vector" -> {
                vectorData =
                    VectorData(
                        width = parseSize(parser.getAttributeValue(null, "android:width")),
                        height = parseSize(parser.getAttributeValue(null, "android:height")),
                        viewportWidth =
                            parser.getAttributeValue(null, "android:viewportWidth")?.toFloatOrNull()
                                ?: 24f,
                        viewportHeight =
                            parser
                                .getAttributeValue(null, "android:viewportHeight")
                                ?.toFloatOrNull() ?: 24f,
                        tint = parser.getAttributeValue(null, "android:tint"),
                    )
              }
              "group" -> {
                currentGroup =
                    GroupData(
                        name = parser.getAttributeValue(null, "android:name"),
                        rotation =
                            parser.getAttributeValue(null, "android:rotation")?.toFloatOrNull()
                                ?: 0f,
                        pivotX =
                            parser.getAttributeValue(null, "android:pivotX")?.toFloatOrNull() ?: 0f,
                        pivotY =
                            parser.getAttributeValue(null, "android:pivotY")?.toFloatOrNull() ?: 0f,
                        scaleX =
                            parser.getAttributeValue(null, "android:scaleX")?.toFloatOrNull() ?: 1f,
                        scaleY =
                            parser.getAttributeValue(null, "android:scaleY")?.toFloatOrNull() ?: 1f,
                        translateX =
                            parser.getAttributeValue(null, "android:translateX")?.toFloatOrNull()
                                ?: 0f,
                        translateY =
                            parser.getAttributeValue(null, "android:translateY")?.toFloatOrNull()
                                ?: 0f,
                    )
              }
              "path" -> {
                val pathData =
                    PathData(
                        pathString = parser.getAttributeValue(null, "android:pathData") ?: "",
                        fillColor =
                            parser.getAttributeValue(null, "android:fillColor") ?: "#FF000000",
                        strokeColor = parser.getAttributeValue(null, "android:strokeColor"),
                        strokeWidth =
                            parser.getAttributeValue(null, "android:strokeWidth")?.toFloatOrNull()
                                ?: 0f,
                        fillAlpha =
                            parser.getAttributeValue(null, "android:fillAlpha")?.toFloatOrNull()
                                ?: 1f,
                        strokeAlpha =
                            parser.getAttributeValue(null, "android:strokeAlpha")?.toFloatOrNull()
                                ?: 1f,
                    )

                if (currentGroup != null) {
                  currentGroup.paths.add(pathData)
                } else {
                  paths.add(pathData)
                }
              }
            }
          }
          XmlPullParser.END_TAG -> {
            when (parser.name) {
              "group" -> {
                if (currentGroup != null) {
                  groups.add(currentGroup)
                  currentGroup = null
                }
              }
            }
          }
        }
        eventType = parser.next()
      }

      return vectorData?.copy(paths = paths, groups = groups)
    } catch (e: Exception) {
      log.debug("Error parsing vector data: {}", e.message)
      return null
    }
  }

  /** Enhanced drawable creation with group support */
  private fun createVectorDrawable(vectorData: VectorData, context: Context): Drawable? {
    try {
      val density = context.resources.displayMetrics.density
      val bitmapWidth = (vectorData.width * density).toInt().coerceAtLeast(1)
      val bitmapHeight = (vectorData.height * density).toInt().coerceAtLeast(1)

      val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
      val canvas = Canvas(bitmap)

      // Calculate scale factors
      val scaleX = bitmapWidth / vectorData.viewportWidth
      val scaleY = bitmapHeight / vectorData.viewportHeight
      val scale = min(scaleX, scaleY)

      canvas.save()
      canvas.scale(scale, scale)

      // Center the vector in the canvas
      val offsetX = (bitmapWidth / scale - vectorData.viewportWidth) / 2f
      val offsetY = (bitmapHeight / scale - vectorData.viewportHeight) / 2f
      canvas.translate(offsetX, offsetY)

      // Draw root-level paths
      for (pathData in vectorData.paths) {
        if (pathData.pathString.isNotEmpty()) {
          drawPath(canvas, pathData)
        }
      }

      // Draw grouped paths with transformations
      for (group in vectorData.groups) {
        canvas.save()

        // Apply group transformations
        if (group.translateX != 0f || group.translateY != 0f) {
          canvas.translate(group.translateX, group.translateY)
        }
        if (group.rotation != 0f) {
          canvas.rotate(group.rotation, group.pivotX, group.pivotY)
        }
        if (group.scaleX != 1f || group.scaleY != 1f) {
          canvas.scale(group.scaleX, group.scaleY, group.pivotX, group.pivotY)
        }

        // Draw paths in the group
        for (pathData in group.paths) {
          if (pathData.pathString.isNotEmpty()) {
            drawPath(canvas, pathData)
          }
        }

        canvas.restore()
      }

      canvas.restore()

      return android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
    } catch (e: Exception) {
      log.debug("Error creating vector drawable: {}", e.message)
      return null
    }
  }

  /** Enhanced path drawing with better paint handling */
  private fun drawPath(canvas: Canvas, pathData: PathData) {
    try {
      val path = PathParser.createPathFromPathData(pathData.pathString)
      if (path == null) {
        log.debug("Failed to parse path string: {}", pathData.pathString)
        return
      }

      // Create paint for fill
      if (pathData.fillColor.isNotEmpty() && pathData.fillColor != "none") {
        val fillPaint =
            Paint().apply {
              style = Paint.Style.FILL
              color = parseColor(pathData.fillColor)
              alpha = (pathData.fillAlpha * 255).toInt()
              isAntiAlias = true
            }
        canvas.drawPath(path, fillPaint)
      }

      // Create paint for stroke
      if (
          pathData.strokeColor?.isNotEmpty() == true &&
              pathData.strokeColor != "none" &&
              pathData.strokeWidth > 0f
      ) {
        val strokePaint =
            Paint().apply {
              style = Paint.Style.STROKE
              color = parseColor(pathData.strokeColor)
              strokeWidth = pathData.strokeWidth
              alpha = (pathData.strokeAlpha * 255).toInt()
              isAntiAlias = true
            }
        canvas.drawPath(path, strokePaint)
      }
    } catch (e: Exception) {
      log.debug("Error drawing path: {}", e.message)
    }
  }

  /** Parse size attribute (e.g., "24dp" -> 24f) */
  private fun parseSize(sizeStr: String?): Float {
    if (sizeStr == null) return 24f
    val numericPart = sizeStr.replace(Regex("[^0-9.]"), "")
    return numericPart.toFloatOrNull() ?: 24f
  }

  /** Parse color string with better support for various formats */
  private fun parseColor(colorString: String): Int {
    return try {
      when {
        colorString.startsWith("#") -> Color.parseColor(colorString)
        colorString == "@android:color/white" -> Color.WHITE
        colorString == "@android:color/black" -> Color.BLACK
        colorString == "@android:color/transparent" -> Color.TRANSPARENT
        colorString.startsWith("@android:color/") -> Color.GRAY
        else -> Color.parseColor("#FF000000")
      }
    } catch (e: Exception) {
      log.debug("Could not parse color: {}, using black", colorString)
      Color.BLACK
    }
  }

  /** Find project root (reused from existing logic) */
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
        log.debug("Found project root: {}", current.absolutePath)
        return current
      }

      current = current.parentFile
    }

    current = layoutFile.parentFile
    for (i in 0..10) {
      if (current == null) break

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
}

/** Enhanced data classes for vector parsing with group support */
data class VectorData(
    val width: Float,
    val height: Float,
    val viewportWidth: Float,
    val viewportHeight: Float,
    val tint: String? = null,
    val paths: List<PathData> = emptyList(),
    val groups: List<GroupData> = emptyList(),
)

data class PathData(
    val pathString: String,
    val fillColor: String,
    val strokeColor: String? = null,
    val strokeWidth: Float = 0f,
    val fillAlpha: Float = 1f,
    val strokeAlpha: Float = 1f,
)

data class GroupData(
    val name: String? = null,
    val rotation: Float = 0f,
    val pivotX: Float = 0f,
    val pivotY: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translateX: Float = 0f,
    val translateY: Float = 0f,
    val paths: MutableList<PathData> = mutableListOf(),
)

/** Path parser using Android's native PathParser if available */
object PathParser {
  fun createPathFromPathData(pathData: String): Path? {
    return try {
      // Try to use Android's native PathParser (API 21+)
      val pathParserClass = Class.forName("androidx.core.graphics.PathParser")
      val method = pathParserClass.getMethod("createPathFromPathData", String::class.java)
      method.invoke(null, pathData) as? Path
    } catch (e: Exception) {
      // Fallback to manual parsing if native parser not available
      parsePathStringManually(pathData)
    }
  }

  private fun parsePathStringManually(pathString: String): Path? {
    // This is your existing parsing logic as fallback
    try {
      val path = Path()
      val commands =
          pathString.replace(",", " ").split(Regex("(?=[MLHVCSQTAZmlhvcsqtaz])")).filter {
            it.isNotBlank()
          }

      var currentX = 0f
      var currentY = 0f

      for (command in commands) {
        val parts = command.trim().split(Regex("\\s+"))
        if (parts.isEmpty()) continue

        val cmd = parts[0][0]
        val coords = parts.drop(1).mapNotNull { it.toFloatOrNull() }

        when (cmd.uppercaseChar()) {
          'M' -> { // MoveTo
            if (coords.size >= 2) {
              if (cmd.isUpperCase()) {
                currentX = coords[0]
                currentY = coords[1]
                path.moveTo(currentX, currentY)
              } else {
                currentX += coords[0]
                currentY += coords[1]
                path.moveTo(currentX, currentY)
              }
            }
          }
          'L' -> { // LineTo
            if (coords.size >= 2) {
              if (cmd.isUpperCase()) {
                currentX = coords[0]
                currentY = coords[1]
                path.lineTo(currentX, currentY)
              } else {
                currentX += coords[0]
                currentY += coords[1]
                path.lineTo(currentX, currentY)
              }
            }
          }
          'H' -> { // Horizontal LineTo
            if (coords.size >= 1) {
              if (cmd.isUpperCase()) {
                currentX = coords[0]
              } else {
                currentX += coords[0]
              }
              path.lineTo(currentX, currentY)
            }
          }
          'V' -> { // Vertical LineTo
            if (coords.size >= 1) {
              if (cmd.isUpperCase()) {
                currentY = coords[0]
              } else {
                currentY += coords[0]
              }
              path.lineTo(currentX, currentY)
            }
          }
          'Z' -> { // ClosePath
            path.close()
          }
          'C' -> { // CubicTo
            if (coords.size >= 6) {
              if (cmd.isUpperCase()) {
                path.cubicTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5])
                currentX = coords[4]
                currentY = coords[5]
              } else {
                path.rCubicTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5])
                currentX += coords[4]
                currentY += coords[5]
              }
            }
          }
          'Q' -> { // QuadTo
            if (coords.size >= 4) {
              if (cmd.isUpperCase()) {
                path.quadTo(coords[0], coords[1], coords[2], coords[3])
                currentX = coords[2]
                currentY = coords[3]
              } else {
                path.rQuadTo(coords[0], coords[1], coords[2], coords[3])
                currentX += coords[2]
                currentY += coords[3]
              }
            }
          }
        }
      }

      return path
    } catch (e: Exception) {
      return null
    }
  }
}
