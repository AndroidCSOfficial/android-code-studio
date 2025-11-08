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
import android.graphics.drawable.StateListDrawable
import android.os.Build
import android.util.TypedValue
import androidx.core.content.ContextCompat
import org.slf4j.LoggerFactory

/*
 * Material Design 3 Theme Manager Handles M3 color schemes, typography, and component theming
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class MaterialDesign3Theme {

  companion object {
    private val log = LoggerFactory.getLogger(MaterialDesign3Theme::class.java)

    // M3 Color Palette - Light Theme
    private const val M3_LIGHT_PRIMARY = "#6750A4"
    private const val M3_LIGHT_ON_PRIMARY = "#FFFFFF"
    private const val M3_LIGHT_PRIMARY_CONTAINER = "#EADDFF"
    private const val M3_LIGHT_ON_PRIMARY_CONTAINER = "#21005D"

    private const val M3_LIGHT_SECONDARY = "#625B71"
    private const val M3_LIGHT_ON_SECONDARY = "#FFFFFF"
    private const val M3_LIGHT_SECONDARY_CONTAINER = "#E8DEF8"
    private const val M3_LIGHT_ON_SECONDARY_CONTAINER = "#1D192B"

    private const val M3_LIGHT_TERTIARY = "#7D5260"
    private const val M3_LIGHT_ON_TERTIARY = "#FFFFFF"
    private const val M3_LIGHT_TERTIARY_CONTAINER = "#FFD8E4"
    private const val M3_LIGHT_ON_TERTIARY_CONTAINER = "#31111D"

    private const val M3_LIGHT_ERROR = "#BA1A1A"
    private const val M3_LIGHT_ON_ERROR = "#FFFFFF"
    private const val M3_LIGHT_ERROR_CONTAINER = "#FFDAD6"
    private const val M3_LIGHT_ON_ERROR_CONTAINER = "#410002"

    private const val M3_LIGHT_SURFACE = "#FFFBFE"
    private const val M3_LIGHT_ON_SURFACE = "#1C1B1F"
    private const val M3_LIGHT_SURFACE_VARIANT = "#E7E0EC"
    private const val M3_LIGHT_ON_SURFACE_VARIANT = "#49454F"
    private const val M3_LIGHT_SURFACE_TINT = "#6750A4"
    private const val M3_LIGHT_SURFACE_CONTAINER = "#F3EDF7"
    private const val M3_LIGHT_SURFACE_CONTAINER_HIGH = "#ECE6F0"
    private const val M3_LIGHT_SURFACE_CONTAINER_HIGHEST = "#E6E0E9"

    private const val M3_LIGHT_OUTLINE = "#79747E"
    private const val M3_LIGHT_OUTLINE_VARIANT = "#CAC4D0"

    private const val M3_LIGHT_INVERSE_SURFACE = "#313033"
    private const val M3_LIGHT_INVERSE_ON_SURFACE = "#F4EFF4"
    private const val M3_LIGHT_INVERSE_PRIMARY = "#D0BCFF"

    // M3 Color Palette - Dark Theme
    private const val M3_DARK_PRIMARY = "#D0BCFF"
    private const val M3_DARK_ON_PRIMARY = "#381E72"
    private const val M3_DARK_PRIMARY_CONTAINER = "#4F378B"
    private const val M3_DARK_ON_PRIMARY_CONTAINER = "#EADDFF"

    private const val M3_DARK_SECONDARY = "#CCC2DC"
    private const val M3_DARK_ON_SECONDARY = "#332D41"
    private const val M3_DARK_SECONDARY_CONTAINER = "#4A4458"
    private const val M3_DARK_ON_SECONDARY_CONTAINER = "#E8DEF8"

    private const val M3_DARK_TERTIARY = "#EFB8C8"
    private const val M3_DARK_ON_TERTIARY = "#492532"
    private const val M3_DARK_TERTIARY_CONTAINER = "#633B48"
    private const val M3_DARK_ON_TERTIARY_CONTAINER = "#FFD8E4"

    private const val M3_DARK_ERROR = "#F2B8B5"
    private const val M3_DARK_ON_ERROR = "#601410"
    private const val M3_DARK_ERROR_CONTAINER = "#8C1D18"
    private const val M3_DARK_ON_ERROR_CONTAINER = "#FFDAD6"

    private const val M3_DARK_SURFACE = "#141218"
    private const val M3_DARK_ON_SURFACE = "#E6E0E9"
    private const val M3_DARK_SURFACE_VARIANT = "#49454F"
    private const val M3_DARK_ON_SURFACE_VARIANT = "#CAC4D0"
    private const val M3_DARK_SURFACE_TINT = "#D0BCFF"
    private const val M3_DARK_SURFACE_CONTAINER = "#211F26"
    private const val M3_DARK_SURFACE_CONTAINER_HIGH = "#2B2930"
    private const val M3_DARK_SURFACE_CONTAINER_HIGHEST = "#36343B"

    private const val M3_DARK_OUTLINE = "#938F99"
    private const val M3_DARK_OUTLINE_VARIANT = "#49454F"

    private const val M3_DARK_INVERSE_SURFACE = "#E6E0E9"
    private const val M3_DARK_INVERSE_ON_SURFACE = "#313033"
    private const val M3_DARK_INVERSE_PRIMARY = "#6750A4"
  }

  private var isDarkTheme = false
  private var context: Context? = null

  /** Initialize M3 theme with context */
  fun initialize(context: Context, isDarkTheme: Boolean = false) {
    this.context = context
    this.isDarkTheme = isDarkTheme
    log.debug("M3 Theme initialized - Dark: {}", isDarkTheme)
  }

  /** Get M3 color by attribute name */
  fun getM3Color(attributeName: String): Int? {
    val normalizedName = attributeName.lowercase().replace("?attr/", "").replace("color", "")
    log.debug("Getting M3 color for attribute: {}", normalizedName)

    return when (normalizedName) {
      "primary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_PRIMARY) else Color.parseColor(M3_LIGHT_PRIMARY)
      "onprimary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_PRIMARY)
          else Color.parseColor(M3_LIGHT_ON_PRIMARY)
      "primarycontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_PRIMARY_CONTAINER)
          else Color.parseColor(M3_LIGHT_PRIMARY_CONTAINER)
      "onprimarycontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_PRIMARY_CONTAINER)
          else Color.parseColor(M3_LIGHT_ON_PRIMARY_CONTAINER)

      "secondary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SECONDARY)
          else Color.parseColor(M3_LIGHT_SECONDARY)
      "onsecondary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_SECONDARY)
          else Color.parseColor(M3_LIGHT_ON_SECONDARY)
      "secondarycontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SECONDARY_CONTAINER)
          else Color.parseColor(M3_LIGHT_SECONDARY_CONTAINER)
      "onsecondarycontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_SECONDARY_CONTAINER)
          else Color.parseColor(M3_LIGHT_ON_SECONDARY_CONTAINER)

      "tertiary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_TERTIARY)
          else Color.parseColor(M3_LIGHT_TERTIARY)
      "ontertiary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_TERTIARY)
          else Color.parseColor(M3_LIGHT_ON_TERTIARY)
      "tertiarycontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_TERTIARY_CONTAINER)
          else Color.parseColor(M3_LIGHT_TERTIARY_CONTAINER)
      "ontertiarycontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_TERTIARY_CONTAINER)
          else Color.parseColor(M3_LIGHT_ON_TERTIARY_CONTAINER)

      "error" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ERROR) else Color.parseColor(M3_LIGHT_ERROR)
      "onerror" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_ERROR)
          else Color.parseColor(M3_LIGHT_ON_ERROR)
      "errorcontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ERROR_CONTAINER)
          else Color.parseColor(M3_LIGHT_ERROR_CONTAINER)
      "onerrorcontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_ERROR_CONTAINER)
          else Color.parseColor(M3_LIGHT_ON_ERROR_CONTAINER)

      "surface" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SURFACE) else Color.parseColor(M3_LIGHT_SURFACE)
      "onsurface" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_SURFACE)
          else Color.parseColor(M3_LIGHT_ON_SURFACE)
      "surfacevariant" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SURFACE_VARIANT)
          else Color.parseColor(M3_LIGHT_SURFACE_VARIANT)
      "onsurfacevariant" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_ON_SURFACE_VARIANT)
          else Color.parseColor(M3_LIGHT_ON_SURFACE_VARIANT)
      "surfacetint" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SURFACE_TINT)
          else Color.parseColor(M3_LIGHT_SURFACE_TINT)
      "surfacecontainer" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SURFACE_CONTAINER)
          else Color.parseColor(M3_LIGHT_SURFACE_CONTAINER)
      "surfacecontainerhigh" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SURFACE_CONTAINER_HIGH)
          else Color.parseColor(M3_LIGHT_SURFACE_CONTAINER_HIGH)
      "surfacecontainerhighest" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_SURFACE_CONTAINER_HIGHEST)
          else Color.parseColor(M3_LIGHT_SURFACE_CONTAINER_HIGHEST)

      "outline" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_OUTLINE) else Color.parseColor(M3_LIGHT_OUTLINE)
      "outlinevariant" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_OUTLINE_VARIANT)
          else Color.parseColor(M3_LIGHT_OUTLINE_VARIANT)

      "inversesurface" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_INVERSE_SURFACE)
          else Color.parseColor(M3_LIGHT_INVERSE_SURFACE)
      "inverseonsurface" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_INVERSE_ON_SURFACE)
          else Color.parseColor(M3_LIGHT_INVERSE_ON_SURFACE)
      "inverseprimary" ->
          if (isDarkTheme) Color.parseColor(M3_DARK_INVERSE_PRIMARY)
          else Color.parseColor(M3_LIGHT_INVERSE_PRIMARY)

      else -> {
        log.debug("Unknown M3 color attribute: {}", normalizedName)
        null
      }
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
            intArrayOf(android.R.attr.state_selected),
            intArrayOf(android.R.attr.state_focused),
            intArrayOf(),
        )

    // Create M3-appropriate color variations for different states
    val colors =
        intArrayOf(
            color, // enabled
            Color.argb(0x61, Color.red(color), Color.green(color), Color.blue(color)), // disabled
            Color.argb(0x1F, Color.red(color), Color.green(color), Color.blue(color)), // pressed
            color, // checked
            color, // selected
            Color.argb(0x3D, Color.red(color), Color.green(color), Color.blue(color)), // focused
            color, // default
        )

    return ColorStateList(states, colors)
  }

  /** Create M3 surface background with proper theming */
  fun createM3SurfaceBackground(color: Int, cornerRadius: Int, elevation: Float = 0f): Drawable {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(color)
    gradientDrawable.cornerRadius = cornerRadius.toFloat()

    // Add elevation shadow for M3
    if (elevation > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // Create a more sophisticated shadow effect
      val shadowColor = if (isDarkTheme) Color.argb(0x40, 0, 0, 0) else Color.argb(0x20, 0, 0, 0)
      val rippleColor = Color.argb(0x1F, Color.red(color), Color.green(color), Color.blue(color))
      return RippleDrawable(ColorStateList.valueOf(rippleColor), gradientDrawable, null)
    }

    return gradientDrawable
  }

  /** Create M3 button background with proper state handling */
  fun createM3ButtonBackground(
      backgroundColor: Int,
      cornerRadius: Int,
      strokeColor: Int? = null,
      strokeWidth: Int = 0,
  ): Drawable {
    val stateListDrawable = StateListDrawable()

    // Normal state
    val normalDrawable = GradientDrawable()
    normalDrawable.setColor(backgroundColor)
    normalDrawable.cornerRadius = cornerRadius.toFloat()
    strokeColor?.let { normalDrawable.setStroke(strokeWidth, it) }
    stateListDrawable.addState(intArrayOf(), normalDrawable)

    // Pressed state
    val pressedDrawable = GradientDrawable()
    pressedDrawable.setColor(
        Color.argb(
            0x1F,
            Color.red(backgroundColor),
            Color.green(backgroundColor),
            Color.blue(backgroundColor),
        )
    )
    pressedDrawable.cornerRadius = cornerRadius.toFloat()
    strokeColor?.let { pressedDrawable.setStroke(strokeWidth, it) }
    stateListDrawable.addState(intArrayOf(android.R.attr.state_pressed), pressedDrawable)

    // Disabled state
    val disabledDrawable = GradientDrawable()
    disabledDrawable.setColor(
        Color.argb(
            0x61,
            Color.red(backgroundColor),
            Color.green(backgroundColor),
            Color.blue(backgroundColor),
        )
    )
    disabledDrawable.cornerRadius = cornerRadius.toFloat()
    strokeColor?.let { disabledDrawable.setStroke(strokeWidth, it) }
    stateListDrawable.addState(intArrayOf(-android.R.attr.state_enabled), disabledDrawable)

    return stateListDrawable
  }

  /** Create M3 card background with proper elevation */
  fun createM3CardBackground(color: Int, cornerRadius: Int, elevation: Float): Drawable {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(color)
    gradientDrawable.cornerRadius = cornerRadius.toFloat()

    // Add elevation effect
    if (elevation > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val shadowColor = if (isDarkTheme) Color.argb(0x40, 0, 0, 0) else Color.argb(0x20, 0, 0, 0)
      val rippleColor = Color.argb(0x0F, Color.red(color), Color.green(color), Color.blue(color))
      return RippleDrawable(ColorStateList.valueOf(rippleColor), gradientDrawable, null)
    }

    return gradientDrawable
  }

  /** Create M3 chip background with proper theming */
  fun createM3ChipBackground(color: Int, cornerRadius: Int): Drawable {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(color)
    gradientDrawable.cornerRadius = cornerRadius.toFloat()

    // Add ripple effect for chips
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      val rippleColor = Color.argb(0x1F, Color.red(color), Color.green(color), Color.blue(color))
      return RippleDrawable(ColorStateList.valueOf(rippleColor), gradientDrawable, null)
    }

    return gradientDrawable
  }

  /** Create M3 text field background */
  fun createM3TextFieldBackground(
      backgroundColor: Int,
      strokeColor: Int,
      strokeWidth: Int,
      cornerRadius: Int,
  ): Drawable {
    val gradientDrawable = GradientDrawable()
    gradientDrawable.setColor(backgroundColor)
    gradientDrawable.cornerRadius = cornerRadius.toFloat()
    gradientDrawable.setStroke(strokeWidth, strokeColor)

    return gradientDrawable
  }

  /** Get M3 typography scale */
  fun getM3TextSize(style: String, context: Context): Float {
    return when (style.lowercase()) {
      "displaylarge" -> 57f
      "displaymedium" -> 45f
      "displaysmall" -> 36f
      "headlinelarge" -> 32f
      "headlinemedium" -> 28f
      "headlinesmall" -> 24f
      "titlelarge" -> 22f
      "titlemedium" -> 16f
      "titlesmall" -> 14f
      "bodylarge" -> 16f
      "bodymedium" -> 14f
      "bodysmall" -> 12f
      "labelarge" -> 14f
      "labelmedium" -> 12f
      "labelsmall" -> 11f
      else -> 14f // Default to body medium
    }
  }

  /** Get M3 line height */
  fun getM3LineHeight(style: String, context: Context): Float {
    return when (style.lowercase()) {
      "displaylarge" -> 64f
      "displaymedium" -> 52f
      "displaysmall" -> 44f
      "headlinelarge" -> 40f
      "headlinemedium" -> 36f
      "headlinesmall" -> 32f
      "titlelarge" -> 28f
      "titlemedium" -> 24f
      "titlesmall" -> 20f
      "bodylarge" -> 24f
      "bodymedium" -> 20f
      "bodysmall" -> 16f
      "labelarge" -> 20f
      "labelmedium" -> 16f
      "labelsmall" -> 16f
      else -> 20f // Default to body medium
    }
  }

  /** Get M3 letter spacing */
  fun getM3LetterSpacing(style: String): Float {
    return when (style.lowercase()) {
      "displaylarge" -> -0.25f
      "displaymedium" -> 0f
      "displaysmall" -> 0f
      "headlinelarge" -> 0f
      "headlinemedium" -> 0f
      "headlinesmall" -> 0f
      "titlelarge" -> 0f
      "titlemedium" -> 0.15f
      "titlesmall" -> 0.1f
      "bodylarge" -> 0.5f
      "bodymedium" -> 0.25f
      "bodysmall" -> 0.4f
      "labelarge" -> 0.1f
      "labelmedium" -> 0.5f
      "labelsmall" -> 0.5f
      else -> 0.25f // Default to body medium
    }
  }

  /** Parse M3 color value with theme support */
  fun parseM3Color(value: String, context: Context): Int? {
    val trimmed = value.trim()
    log.debug("Parsing M3 color: '{}'", trimmed)

    return try {
      when {
        trimmed.startsWith("#") -> {
          Color.parseColor(trimmed)
        }
        trimmed.startsWith("@color/") -> {
          val colorName = trimmed.substringAfter("@color/")
          val resourceId = context.resources.getIdentifier(colorName, "color", context.packageName)
          if (resourceId != 0) {
            ContextCompat.getColor(context, resourceId)
          } else null
        }
        trimmed.startsWith("@android:color/") -> {
          val colorName = trimmed.substringAfter("@android:color/")
          val resourceId = context.resources.getIdentifier(colorName, "color", "android")
          if (resourceId != 0) {
            ContextCompat.getColor(context, resourceId)
          } else null
        }
        trimmed.startsWith("?attr/") -> {
          // Handle M3 color attributes
          getM3Color(trimmed)
        }
        else -> {
          // Try to parse as direct color value
          Color.parseColor(trimmed)
        }
      }
    } catch (e: Exception) {
      log.error("Failed to parse M3 color '{}': {}", trimmed, e.message)
      null
    }
  }

  /** Parse M3 dimension value */
  fun parseM3Dimension(value: String, context: Context): Int {
    val trimmed = value.trim()
    log.debug("Parsing M3 dimension: '{}'", trimmed)

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
        trimmed.endsWith("px") -> {
          trimmed.removeSuffix("px").toInt()
        }
        trimmed.matches(Regex("\\d+(\\.\\d+)?")) -> {
          // Plain number, assume dp
          val dpValue = trimmed.toFloat()
          dpToPx(dpValue, context)
        }
        else -> {
          log.debug("Unable to parse M3 dimension: '{}'", trimmed)
          0
        }
      }
    } catch (e: Exception) {
      log.error("Error parsing M3 dimension '{}': {}", trimmed, e.message)
      0
    }
  }

  /** Check if current theme is dark */
  fun isDarkTheme(): Boolean = isDarkTheme

  /** Toggle theme */
  fun toggleTheme() {
    isDarkTheme = !isDarkTheme
    log.debug("M3 Theme toggled - Dark: {}", isDarkTheme)
  }

  /** Set theme */
  fun setTheme(isDark: Boolean) {
    isDarkTheme = isDark
    log.debug("M3 Theme set - Dark: {}", isDarkTheme)
  }

  private fun dpToPx(dp: Float, context: Context): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics,
        )
        .toInt()
  }

  private fun spToPx(sp: Float, context: Context): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics,
        )
        .toInt()
  }
}
