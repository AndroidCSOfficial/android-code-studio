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
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import com.tom.rv2ide.projects.IWorkspace
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Shared M3 utility functions used across all view extensions
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
object M3Utils {
  private val log = LoggerFactory.getLogger(M3Utils::class.java)

  /** Parse color with M3 color token support */
  fun parseColorM3(value: String, context: Context): Int? {
    val trimmed = value.trim()

    return try {
      when {
        trimmed.startsWith("#") -> Color.parseColor(trimmed)
        trimmed.startsWith("@color/") -> {
          val colorName = trimmed.substringAfter("@color/")
          val resourceId = context.resources.getIdentifier(colorName, "color", context.packageName)
          if (resourceId != 0) ContextCompat.getColor(context, resourceId) else null
        }
        trimmed.startsWith("@android:color/") -> {
          val colorName = trimmed.substringAfter("@android:color/")
          val resourceId = context.resources.getIdentifier(colorName, "color", "android")
          if (resourceId != 0) ContextCompat.getColor(context, resourceId) else null
        }
        // M3 color tokens
        trimmed.equals("?attr/colorPrimary", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_PRIMARY)
        trimmed.equals("?attr/colorOnPrimary", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_ON_PRIMARY)
        trimmed.equals("?attr/colorSecondary", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_SECONDARY)
        trimmed.equals("?attr/colorOnSecondary", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_ON_SECONDARY)
        trimmed.equals("?attr/colorSurface", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_SURFACE)
        trimmed.equals("?attr/colorOnSurface", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_ON_SURFACE)
        trimmed.equals("?attr/colorSurfaceVariant", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_SURFACE_VARIANT)
        trimmed.equals("?attr/colorOnSurfaceVariant", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_ON_SURFACE_VARIANT)
        trimmed.equals("?attr/colorError", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_ERROR)
        trimmed.equals("?attr/colorOnError", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_ON_ERROR)
        trimmed.equals("?attr/colorOutline", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_OUTLINE)
        trimmed.equals("?attr/colorOutlineVariant", ignoreCase = true) ->
            Color.parseColor(MaterialDesign3Renderer.M3_OUTLINE_VARIANT)
        trimmed.equals("?attr/colorPrimaryContainer", ignoreCase = true) ->
            Color.parseColor("#EADDFF")
        trimmed.equals("?attr/colorOnPrimaryContainer", ignoreCase = true) ->
            Color.parseColor("#21005D")
        trimmed.equals("?attr/colorSecondaryContainer", ignoreCase = true) ->
            Color.parseColor("#E8DEF8")
        trimmed.equals("?attr/colorOnSecondaryContainer", ignoreCase = true) ->
            Color.parseColor("#1D192B")
        else -> Color.parseColor(trimmed)
      }
    } catch (e: Exception) {
      null
    }
  }

  /** Parse dimension with M3 support */
  fun parseDimensionM3(value: String, context: Context): Int {
    val trimmed = value.trim()

    return try {
      when {
        trimmed.endsWith("dp") -> {
          val dpValue = trimmed.removeSuffix("dp").toFloat()
          dpToPx(dpValue, context)
        }
        trimmed.endsWith("sp") -> {
          val spValue = trimmed.removeSuffix("sp").toFloat()
          spToPx(spValue, context)
        }
        trimmed.endsWith("px") -> trimmed.removeSuffix("px").toInt()
        trimmed.matches(Regex("\\d+(\\.\\d+)?")) -> {
          val dpValue = trimmed.toFloat()
          dpToPx(dpValue, context)
        }
        else -> 0
      }
    } catch (e: Exception) {
      0
    }
  }

  /** Create M3 color state list with proper state handling */
  fun createM3ColorStateList(color: Int): ColorStateList {
    val states =
        arrayOf(
            intArrayOf(android.R.attr.state_enabled),
            intArrayOf(-android.R.attr.state_enabled),
            intArrayOf(android.R.attr.state_pressed),
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(),
        )

    val colors =
        intArrayOf(
            color,
            Color.argb(0x61, Color.red(color), Color.green(color), Color.blue(color)),
            Color.argb(0x1F, Color.red(color), Color.green(color), Color.blue(color)),
            color,
            color,
        )

    return ColorStateList(states, colors)
  }

  /** Create M3 surface background with proper theming */
  fun createM3SurfaceBackground(color: Int, cornerRadius: Int): Drawable {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(color)
    gradientDrawable.cornerRadius = cornerRadius.toFloat()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val rippleColor = Color.argb(0x1F, Color.red(color), Color.green(color), Color.blue(color))
      return RippleDrawable(ColorStateList.valueOf(rippleColor), gradientDrawable, null)
    }

    return gradientDrawable
  }

  /** Load drawable with M3 support */
  fun loadDrawableM3(
      resourceName: String,
      context: Context,
      workspace: IWorkspace?,
      layoutFile: File?,
      onSuccess: (Drawable) -> Unit,
  ): Boolean {
    val drawableName = resourceName.substringAfter("@drawable/")

    return try {
      var resourceId =
          context.resources.getIdentifier(drawableName, "drawable", context.packageName)
      if (resourceId == 0) {
        resourceId =
            context.applicationContext.resources.getIdentifier(
                drawableName,
                "drawable",
                context.applicationContext.packageName,
            )
      }

      if (resourceId != 0) {
        val drawable = AppCompatResources.getDrawable(context, resourceId)
        if (drawable != null) {
          onSuccess(drawable)
          return true
        }
      }

      // Use workspace for better project root detection
      loadDrawableFromProjectFilesM3(drawableName, context, workspace, layoutFile, onSuccess)
    } catch (e: Exception) {
      log.error("Failed to load drawable: $resourceName", e)
      false
    }
  }

  fun loadMipmapM3(resourceName: String, context: Context, onSuccess: (Drawable) -> Unit): Boolean {
    val mipmapName = resourceName.substringAfter("@mipmap/")

    return try {
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
        val drawable = AppCompatResources.getDrawable(context, resourceId)
        if (drawable != null) {
          onSuccess(drawable)
          return true
        }
      }

      false
    } catch (e: Exception) {
      log.error("Failed to load mipmap: $resourceName", e)
      false
    }
  }

  fun loadAndroidDrawableM3(
      resourceName: String,
      context: Context,
      onSuccess: (Drawable) -> Unit,
  ): Boolean {
    val drawableName = resourceName.substringAfter("@android:drawable/")

    return try {
      val resourceId = context.resources.getIdentifier(drawableName, "drawable", "android")
      if (resourceId != 0) {
        val drawable = AppCompatResources.getDrawable(context, resourceId)
        if (drawable != null) {
          onSuccess(drawable)
          return true
        }
      }
      false
    } catch (e: Exception) {
      log.error("Failed to load Android drawable: $resourceName", e)
      false
    }
  }

  private fun loadDrawableFromProjectFilesM3(
      iconName: String,
      context: Context,
      workspace: IWorkspace?,
      layoutFile: File?,
      onSuccess: (Drawable) -> Unit,
  ): Boolean {
    // Try to get project root from workspace first
    val projectRoot = workspace?.getProjectDir() ?: findProjectRootFallback(layoutFile)

    if (projectRoot == null) {
      log.warn("Could not determine project root for loading drawable: $iconName")
      return false
    }

    log.debug("Loading drawable from project root: ${projectRoot.absolutePath}")

    // Try to find the module that contains the layout file
    val moduleRoot =
        if (workspace != null && layoutFile != null) {
          workspace.findModuleForFile(layoutFile)?.projectDir ?: projectRoot
        } else {
          projectRoot
        }

    val drawableFolders = buildDrawableFolderList(projectRoot, moduleRoot)
    val extensions = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".xml")

    for (folder in drawableFolders) {
      val resourceDir = File(folder)
      if (!resourceDir.exists()) continue

      for (ext in extensions) {
        val iconFile = File(resourceDir, "$iconName$ext")
        if (iconFile.exists() && iconFile.canRead()) {
          try {
            if (ext == ".xml") {
              val inputStream = iconFile.inputStream()
              val drawable = Drawable.createFromStream(inputStream, iconFile.name)
              inputStream.close()
              if (drawable != null) {
                onSuccess(drawable)
                log.debug("Successfully loaded XML drawable: ${iconFile.absolutePath}")
                return true
              }
            } else {
              val bitmap = android.graphics.BitmapFactory.decodeFile(iconFile.absolutePath)
              if (bitmap != null) {
                val drawable = android.graphics.drawable.BitmapDrawable(context.resources, bitmap)
                onSuccess(drawable)
                log.debug("Successfully loaded bitmap drawable: ${iconFile.absolutePath}")
                return true
              }
            }
          } catch (e: Exception) {
            log.warn("Failed to load drawable file: ${iconFile.absolutePath}", e)
          }
        }
      }
    }

    log.warn("Could not find drawable in project files: $iconName")
    return false
  }

  /** Build list of potential drawable folders to search */
  private fun buildDrawableFolderList(projectRoot: File, moduleRoot: File): List<String> {
    val folders = mutableListOf<String>()

    // Add module-specific paths first (higher priority)
    val modulePath = moduleRoot.absolutePath.removePrefix(projectRoot.absolutePath).trim('/')
    if (modulePath.isNotEmpty()) {
      folders.addAll(
          listOf(
                  "$modulePath/src/main/res/drawable",
                  "$modulePath/src/main/res/drawable-hdpi",
                  "$modulePath/src/main/res/drawable-mdpi",
                  "$modulePath/src/main/res/drawable-xhdpi",
                  "$modulePath/src/main/res/drawable-xxhdpi",
                  "$modulePath/src/main/res/drawable-xxxhdpi",
              )
              .map { File(projectRoot, it).absolutePath }
      )
    }

    // Add common Android project structures
    folders.addAll(
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
            .map { File(projectRoot, it).absolutePath }
    )

    return folders
  }

  /** Fallback method to find project root when workspace is not available */
  private fun findProjectRootFallback(layoutFile: File?): File? {
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
        log.debug("Found project root (fallback): ${current.absolutePath}")
        return current
      }

      current = current.parentFile
    }

    log.warn("Could not find project root using fallback method")
    return null
  }

  fun dpToPx(dp: Float, context: Context): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        )
        .toInt()
  }

  fun spToPx(sp: Float, context: Context): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics,
        )
        .toInt()
  }
}
