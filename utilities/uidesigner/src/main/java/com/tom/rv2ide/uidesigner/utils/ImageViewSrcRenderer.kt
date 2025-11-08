/*
 * Enhanced ImageView Src Preview Support for AndroidIDE UI Designer
 * This loads actual drawable resources for ImageView src attributes
 */

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileInputStream
import org.slf4j.LoggerFactory
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

/**
 * Helper class to apply ImageView src previews in the UI Designer This loads actual image files and
 * XML vectors from Android resource folders
 */
class ImageViewSrcRenderer {

  companion object {
    private val log = LoggerFactory.getLogger(ImageViewSrcRenderer::class.java)
  }

  /** Apply src preview to an ImageView in the designer */
  fun applySrcPreview(
      imageView: ImageView,
      srcValue: String,
      context: Context,
      layoutFile: File? = null,
  ): Boolean {
    val value = srcValue.trim()

    try {
      when {
        value.isEmpty() -> return false
        value.startsWith("@drawable/") -> {
          return loadDrawableResource(imageView, value, context, layoutFile)
        }
        value.startsWith("@mipmap/") -> {
          return loadMipmapResource(imageView, value, context, layoutFile)
        }
        value.startsWith("@android:drawable/") -> {
          return loadAndroidDrawable(imageView, value, context)
        }
        else -> {
          // Try to parse as resource name without prefix
          return loadDrawableResource(imageView, "@drawable/$value", context, layoutFile)
        }
      }
    } catch (e: Exception) {
      log.debug("Could not preview src '{}': {}", value, e.message)
      return false
    }
  }

  private fun loadDrawableResource(
      imageView: ImageView,
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
        imageView.setImageDrawable(drawable)
        return true
      }
    }

    // Try loading from res/drawable folders directly
    return loadFromResDrawable(imageView, drawableName, context, layoutFile)
  }

  private fun loadMipmapResource(
      imageView: ImageView,
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
        imageView.setImageDrawable(drawable)
        return true
      }
    }

    // Try loading from res/mipmap folders directly
    return loadFromResMipmap(imageView, mipmapName, context, layoutFile)
  }

  private fun loadFromResDrawable(
      imageView: ImageView,
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

    return loadImageFromFolders(imageView, drawableName, projectRoot, drawableFolders, context)
  }

  private fun loadFromResMipmap(
      imageView: ImageView,
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

    return loadImageFromFolders(imageView, mipmapName, projectRoot, mipmapFolders, context)
  }

  private fun loadImageFromFolders(
      imageView: ImageView,
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
              log.debug("Attempting to load XML drawable for ImageView: {}", imageFile.absolutePath)
              if (loadXmlDrawable(imageView, imageFile, context)) {
                return true
              }
              continue
            }

            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            if (bitmap != null) {
              imageView.setImageBitmap(bitmap)
              log.debug("Loaded image for ImageView: {}", imageFile.absolutePath)
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

  /** Load XML drawable files (vectors, shapes, gradients, etc.) for ImageView */
  private fun loadXmlDrawable(imageView: ImageView, xmlFile: File, context: Context): Boolean {
    try {
      // First, try using VectorDrawableCompat for vector drawables
      if (isVectorDrawable(xmlFile)) {
        return loadVectorDrawable(imageView, xmlFile, context)
      }

      // Try parsing as shape drawable
      if (isShapeDrawable(xmlFile)) {
        return loadShapeDrawable(imageView, xmlFile, context)
      }

      // Try parsing as layer-list or other drawable types
      return loadGenericXmlDrawable(imageView, xmlFile, context)
    } catch (e: Exception) {
      log.debug("Error loading XML drawable for ImageView {}: {}", xmlFile.absolutePath, e.message)
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

  /** Load vector drawable for ImageView */
  private fun loadVectorDrawable(imageView: ImageView, xmlFile: File, context: Context): Boolean {
    return try {
      // Try to create a drawable from the XML stream
      val inputStream = FileInputStream(xmlFile)
      val drawable = Drawable.createFromStream(inputStream, xmlFile.name)

      if (drawable != null) {
        imageView.setImageDrawable(drawable)
        log.debug("SUCCESS: Loaded vector drawable for ImageView: {}", xmlFile.name)
        return true
      }

      // Fallback: Create a placeholder to show that the XML was recognized
      val placeholderDrawable =
          ColorDrawable(Color.parseColor("#2196F3")) // Blue placeholder for vectors
      imageView.setImageDrawable(placeholderDrawable)
      log.debug("FALLBACK: Created vector placeholder for ImageView: {}", xmlFile.name)
      return true
    } catch (e: Exception) {
      log.debug("Vector loading failed for ImageView: {}", e.message)
      return false
    }
  }

  /** Load shape drawable for ImageView by parsing XML */
  private fun loadShapeDrawable(imageView: ImageView, xmlFile: File, context: Context): Boolean {
    try {
      // For ImageView, we can reuse the BackgroundPreviewRenderer's shape parsing logic
      // but apply it to setImageDrawable instead of background
      val backgroundRenderer = BackgroundPreviewRenderer()

      // Create a temporary view to parse the shape, then extract the drawable
      val tempView = android.view.View(context)
      val xmlContent = xmlFile.readText()

      // Try to extract basic shape properties for a simple shape drawable
      val drawable = parseSimpleShapeForImageView(xmlFile)
      if (drawable != null) {
        imageView.setImageDrawable(drawable)
        log.debug("Successfully loaded shape drawable for ImageView: {}", xmlFile.absolutePath)
        return true
      }
    } catch (e: Exception) {
      log.debug(
          "Failed to load shape drawable for ImageView {}: {}",
          xmlFile.absolutePath,
          e.message,
      )
    }
    return false
  }

  /** Parse a simple shape drawable specifically for ImageView */
  private fun parseSimpleShapeForImageView(xmlFile: File): Drawable? {
    try {
      val content = xmlFile.readText()

      // Create a simple colored shape as preview
      val colorRegex = """android:color="(#[0-9A-Fa-f]{6,8})"""".toRegex()
      val colorMatch = colorRegex.find(content)

      if (colorMatch != null) {
        val color = Color.parseColor(colorMatch.groupValues[1])
        return ColorDrawable(color)
      }

      // Default shape color if no color found
      return ColorDrawable(Color.parseColor("#9E9E9E"))
    } catch (e: Exception) {
      log.debug("Error parsing simple shape: {}", e.message)
      return null
    }
  }

  /** Try to load generic XML drawable for ImageView */
  private fun loadGenericXmlDrawable(
      imageView: ImageView,
      xmlFile: File,
      context: Context,
  ): Boolean {
    try {
      // Create a placeholder drawable to indicate XML was found
      val placeholder =
          ColorDrawable(Color.parseColor("#FF9800")) // Orange placeholder for other XMLs
      imageView.setImageDrawable(placeholder)
      log.debug("Loaded generic XML placeholder for ImageView: {}", xmlFile.absolutePath)
      return true
    } catch (e: Exception) {
      log.debug(
          "Failed to load generic XML drawable for ImageView {}: {}",
          xmlFile.absolutePath,
          e.message,
      )
      return false
    }
  }

  /**
   * Find project root based on the layout file path (reused from BackgroundPreviewRenderer logic)
   */
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

  private fun loadAndroidDrawable(
      imageView: ImageView,
      resourceName: String,
      context: Context,
  ): Boolean {
    return try {
      val drawableName = resourceName.substringAfter("@android:drawable/")
      val resourceId = context.resources.getIdentifier(drawableName, "drawable", "android")

      if (resourceId != 0) {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable != null) {
          imageView.setImageDrawable(drawable)
          return true
        }
      }

      false
    } catch (e: Exception) {
      false
    }
  }
}

/** Enhanced ImageView src completion */
object ImageViewSrcCompletions {

  val COMMON_IMAGE_SOURCES =
      listOf(
          // Common Android drawables
          "@android:drawable/ic_menu_add",
          "@android:drawable/ic_menu_delete",
          "@android:drawable/ic_menu_edit",
          "@android:drawable/ic_menu_info_details",
          "@android:drawable/ic_menu_search",
          "@android:drawable/ic_menu_share",
          "@android:drawable/ic_dialog_alert",
          "@android:drawable/ic_dialog_info",
          "@android:drawable/star_on",
          "@android:drawable/star_off",
          "@android:drawable/btn_star",

          // Resource references
          "@drawable/",
          "@mipmap/",
      )

  fun getSrcSuggestions(currentText: String): List<String> {
    return when {
      currentText.startsWith("@drawable/") -> listOf("@drawable/")
      currentText.startsWith("@mipmap/") -> listOf("@mipmap/")
      currentText.startsWith("@android:drawable/") ->
          COMMON_IMAGE_SOURCES.filter {
            it.startsWith("@android:drawable/") &&
                it.contains(currentText.substringAfter("@android:drawable/"), ignoreCase = true)
          }
      else -> COMMON_IMAGE_SOURCES.filter { it.contains(currentText, ignoreCase = true) }.take(10)
    }
  }
}
