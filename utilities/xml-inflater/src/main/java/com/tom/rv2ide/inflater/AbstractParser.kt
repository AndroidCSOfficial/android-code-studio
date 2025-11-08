/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.inflater

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.tom.rv2ide.inflater.internal.utils.defaultGravity
import com.tom.rv2ide.inflater.internal.utils.unknownDrawable

/**
 * Abstract class which provides access to the internal parsing utility methods to its subclasses.
 * Extended with Material Design token and theme attribute support.
 *
 * @author Akash Yadav
 * @modification Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
abstract class AbstractParser {

  companion object {
    // Material Design 3 color tokens mapping
    private val materialTokens =
        mapOf(
            // Primary colors
            "md.sys.color.primary" to "?attr/colorPrimary",
            "md.sys.color.on-primary" to "?attr/colorOnPrimary",
            "md.sys.color.primary-container" to "?attr/colorPrimaryContainer",
            "md.sys.color.on-primary-container" to "?attr/colorOnPrimaryContainer",

            // Secondary colors
            "md.sys.color.secondary" to "?attr/colorSecondary",
            "md.sys.color.on-secondary" to "?attr/colorOnSecondary",
            "md.sys.color.secondary-container" to "?attr/colorSecondaryContainer",
            "md.sys.color.on-secondary-container" to "?attr/colorOnSecondaryContainer",

            // Tertiary colors
            "md.sys.color.tertiary" to "?attr/colorTertiary",
            "md.sys.color.on-tertiary" to "?attr/colorOnTertiary",
            "md.sys.color.tertiary-container" to "?attr/colorTertiaryContainer",
            "md.sys.color.on-tertiary-container" to "?attr/colorOnTertiaryContainer",

            // Error colors
            "md.sys.color.error" to "?attr/colorError",
            "md.sys.color.on-error" to "?attr/colorOnError",
            "md.sys.color.error-container" to "?attr/colorErrorContainer",
            "md.sys.color.on-error-container" to "?attr/colorOnErrorContainer",

            // Surface colors
            "md.sys.color.surface" to "?attr/colorSurface",
            "md.sys.color.on-surface" to "?attr/colorOnSurface",
            "md.sys.color.surface-variant" to "?attr/colorSurfaceVariant",
            "md.sys.color.on-surface-variant" to "?attr/colorOnSurfaceVariant",
            "md.sys.color.surface-container" to "?attr/colorSurfaceContainer",
            "md.sys.color.surface-container-high" to "?attr/colorSurfaceContainerHigh",
            "md.sys.color.surface-container-highest" to "?attr/colorSurfaceContainerHighest",

            // Background colors
            "md.sys.color.background" to "?attr/android:colorBackground",
            "md.sys.color.on-background" to "?attr/colorOnBackground",

            // Outline colors
            "md.sys.color.outline" to "?attr/colorOutline",
            "md.sys.color.outline-variant" to "?attr/colorOutlineVariant",
        )

    // Material Design typography tokens
    private val typographyTokens =
        mapOf(
            "md.sys.typescale.display-large" to "?attr/textAppearanceDisplayLarge",
            "md.sys.typescale.display-medium" to "?attr/textAppearanceDisplayMedium",
            "md.sys.typescale.display-small" to "?attr/textAppearanceDisplaySmall",
            "md.sys.typescale.headline-large" to "?attr/textAppearanceHeadlineLarge",
            "md.sys.typescale.headline-medium" to "?attr/textAppearanceHeadlineMedium",
            "md.sys.typescale.headline-small" to "?attr/textAppearanceHeadlineSmall",
            "md.sys.typescale.title-large" to "?attr/textAppearanceTitleLarge",
            "md.sys.typescale.title-medium" to "?attr/textAppearanceTitleMedium",
            "md.sys.typescale.title-small" to "?attr/textAppearanceTitleSmall",
            "md.sys.typescale.body-large" to "?attr/textAppearanceBodyLarge",
            "md.sys.typescale.body-medium" to "?attr/textAppearanceBodyMedium",
            "md.sys.typescale.body-small" to "?attr/textAppearanceBodySmall",
            "md.sys.typescale.label-large" to "?attr/textAppearanceLabelLarge",
            "md.sys.typescale.label-medium" to "?attr/textAppearanceLabelMedium",
            "md.sys.typescale.label-small" to "?attr/textAppearanceLabelSmall",
        )

    // Material Design shape tokens
    private val shapeTokens =
        mapOf(
            "md.sys.shape.corner.none" to "0dp",
            "md.sys.shape.corner.extra-small" to "4dp",
            "md.sys.shape.corner.small" to "8dp",
            "md.sys.shape.corner.medium" to "12dp",
            "md.sys.shape.corner.large" to "16dp",
            "md.sys.shape.corner.extra-large" to "28dp",
            "md.sys.shape.corner.full" to "50%",
        )

    // Material Design elevation tokens
    private val elevationTokens =
        mapOf(
            "md.sys.elevation.level0" to "0dp",
            "md.sys.elevation.level1" to "1dp",
            "md.sys.elevation.level2" to "3dp",
            "md.sys.elevation.level3" to "6dp",
            "md.sys.elevation.level4" to "8dp",
            "md.sys.elevation.level5" to "12dp",
        )
  }

  /**
   * Parses the given string value representing an ID resource.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseId(resName: String, value: String, def: Int = 0): Int {
    return com.tom.rv2ide.inflater.internal.utils.parseId(resName, value, def)
  }

  /**
   * Parses the given string value representing a float to its actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseFloat(value: String, def: Float = 0f): Float {
    return com.tom.rv2ide.inflater.internal.utils.parseFloat(value = value, def = def)
  }

  /**
   * Parses the given string value representing a long to its actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseLong(value: String, def: Long = 0L): Long {
    return com.tom.rv2ide.inflater.internal.utils.parseLong(value = value, def = def)
  }

  /**
   * Parses the given string value representing an integer or reference to an integer resource to
   * its actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseInteger(value: String, def: Int = 0): Int {
    return com.tom.rv2ide.inflater.internal.utils.parseInteger(value = value, def = def)
  }

  /**
   * Parses the given string value representing a reference to an integer array resource to its
   * actual value. Returns an empty array if the resource reference cannot be resolved.
   *
   * @param value The string value. Usually value from attributes.
   */
  protected open fun parseIntegerArray(value: String): IntArray {
    return com.tom.rv2ide.inflater.internal.utils.parseIntegerArray(value) ?: intArrayOf()
  }

  /**
   * Parses the given string value representing a boolean or reference to an boolean resource to its
   * actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseBoolean(value: String, def: Boolean = false): Boolean {
    return com.tom.rv2ide.inflater.internal.utils.parseBoolean(value = value, def = def)
  }

  /**
   * Parses the given string value representing a string or reference to a string resource to its
   * actual value. Returns [value] itself if it cannot be parsed.
   *
   * @param value The string value. Usually value from attributes.
   */
  protected open fun parseString(value: String): String {
    return com.tom.rv2ide.inflater.internal.utils.parseString(value)
  }

  /**
   * Parses the given string value representing a reference to a string array resource to its actual
   * value. Returns an empty array if the resource reference cannot be resolved.
   *
   * @param value The string value. Usually value from attributes.
   */
  protected open fun parseStringArray(value: String): Array<String> {
    return com.tom.rv2ide.inflater.internal.utils.parseStringArray(value) ?: emptyArray()
  }

  /**
   * Parses the given string value representing a color or reference to a drawable resource to an
   * actual drawable which can be rendered.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseDrawable(
      context: Context,
      value: String,
      def: Drawable = unknownDrawable(),
  ): Drawable {
    // Check for Material Design token first
    val resolvedToken = resolveMaterialToken(context, value)
    if (resolvedToken != null) {
      return parseDrawable(context, resolvedToken, def)
    }

    // Handle Material theme attributes
    when {
      value.startsWith("?attr/") -> {
        return resolveMaterialThemeDrawable(context, value, def)
      }
      value.startsWith("@drawable/") && isMaterialIcon(value) -> {
        return resolveMaterialIconDrawable(context, value, def)
      }
      else ->
          return com.tom.rv2ide.inflater.internal.utils.parseDrawable(
              context = context,
              value = value,
              def = def,
          )
    }
  }

  /**
   * Parses the gravity flags which can be single flag value like `center` or a multiple combined
   * flag values like `start|top`.
   *
   * @param value The gravity string.
   * @param def The default gravity flag value.
   */
  @JvmOverloads
  protected open fun parseGravity(value: String, def: Int = defaultGravity()): Int {
    return com.tom.rv2ide.inflater.internal.utils.parseGravity(value = value, def = def)
  }

  /**
   * Parses the given string value representing a dimension value or reference to a dimension
   * resource to its actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseDimension(context: Context, value: String, def: Int = 0): Int {
    return parseDimensionF(context = context, value = value, def = def.toFloat()).toInt()
  }

  /**
   * Parses the given string value representing a dimension value or reference to a dimension
   * resource to its actual value as a float point number.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseDimensionF(context: Context, value: String, def: Float = 0f): Float {
    // Check for Material Design token first
    val resolvedToken = resolveMaterialToken(context, value)
    if (resolvedToken != null) {
      return parseDimensionF(context, resolvedToken, def)
    }

    return com.tom.rv2ide.inflater.internal.utils.parseDimension(
        context = context,
        value = value,
        def = def,
    )
  }

  /**
   * Parses the given string value representing a color code or reference to a color resource to its
   * actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseColor(
      context: Context,
      value: String,
      def: Int = Color.TRANSPARENT,
  ): Int {
    // Check if it's a Material Design token first
    val resolvedToken = resolveMaterialToken(context, value)
    if (resolvedToken != null) {
      return parseColor(context, resolvedToken, def)
    }

    // Handle Material theme attributes
    when {
      value.startsWith("?attr/color") -> {
        return resolveMaterialThemeColor(context, value, def)
      }
      value.startsWith("@color/") -> {
        // Try to resolve as Material color resource
        return resolveMaterialColorResource(context, value, def)
      }
      else ->
          return com.tom.rv2ide.inflater.internal.utils.parseColor(
              context = context,
              value = value,
              def = def,
          )
    }
  }

  /**
   * Parses the given string value representing a color code or reference to a color state list
   * resource to its actual value.
   *
   * @param value The string value. Usually value from attributes.
   * @param def The default value.
   */
  @JvmOverloads
  protected open fun parseColorStateList(
      context: Context,
      value: String,
      def: ColorStateList = ColorStateList.valueOf(Color.TRANSPARENT),
  ): ColorStateList {
    // Check for Material Design token
    val resolvedToken = resolveMaterialToken(context, value)
    if (resolvedToken != null) {
      return parseColorStateList(context, resolvedToken, def)
    }

    // Handle Material theme attributes
    when {
      value.startsWith("?attr/color") -> {
        val color = resolveMaterialThemeColor(context, value, Color.TRANSPARENT)
        return ColorStateList.valueOf(color)
      }
      value.startsWith("@color/") -> {
        val color = resolveMaterialColorResource(context, value, Color.TRANSPARENT)
        return ColorStateList.valueOf(color)
      }
      else ->
          return com.tom.rv2ide.inflater.internal.utils.parseColorStateList(
              context = context,
              value = value,
              def = def,
          )
    }
  }

  /**
   * Parses the given string value representing a date to miliseconds.
   *
   * @param value The string value. Usually value from attributes.
   * @param format The date format for [java.text.SimpleDateFormat].
   * @param def The default value.
   */
  @JvmOverloads
  fun parseDate(value: String, format: String = "MM/dd/yyyy", def: Long = 0L): Long {
    return com.tom.rv2ide.inflater.internal.utils.parseDate(
        value = value,
        format = format,
        def = def,
    )
  }

  // New Material Design helper methods

  /** Resolves Material Design tokens to theme attributes or actual values. */
  protected open fun resolveMaterialToken(context: Context, token: String): String? {
    return when {
      materialTokens.containsKey(token) -> materialTokens[token]
      typographyTokens.containsKey(token) -> typographyTokens[token]
      shapeTokens.containsKey(token) -> shapeTokens[token]
      elevationTokens.containsKey(token) -> elevationTokens[token]
      else -> null
    }
  }

  /** Parse Material Design component styles. */
  protected fun parseMaterialStyle(context: Context, value: String): Int? {
    return when {
      value.startsWith("@style/Widget.Material3.") -> {
        resolveMaterialStyleResource(context, value)
      }
      value.startsWith("@style/Widget.MaterialComponents.") -> {
        resolveMaterialStyleResource(context, value)
      }
      value.startsWith("?attr/materialButton") -> {
        resolveMaterialThemeStyle(context, value)
      }
      else -> null
    }
  }

  /** Parse Material Design text appearances. */
  protected fun parseMaterialTextAppearance(context: Context, value: String): Int? {
    // Check for typography token first
    val resolvedToken = resolveMaterialToken(context, value)
    if (resolvedToken != null) {
      return parseMaterialTextAppearance(context, resolvedToken)
    }

    return when {
      value.startsWith("?attr/textAppearance") -> {
        resolveMaterialThemeStyle(context, value)
      }
      value.startsWith("@style/TextAppearance.Material3.") -> {
        resolveMaterialStyleResource(context, value)
      }
      else -> null
    }
  }

  /** Parse Material Design elevation values. */
  protected fun parseElevation(context: Context, value: String, def: Float = 0f): Float {
    // Check for Material Design elevation token
    val resolvedToken = resolveMaterialToken(context, value)
    if (resolvedToken != null) {
      return parseDimensionF(context, resolvedToken, def)
    }

    return parseDimensionF(context, value, def)
  }

  /** Parse Material Design shape appearance. */
  protected fun parseShapeAppearance(context: Context, value: String): Int? {
    return when {
      value.startsWith("?attr/shapeAppearance") -> {
        resolveMaterialThemeStyle(context, value)
      }
      value.startsWith("@style/ShapeAppearance.Material3.") -> {
        resolveMaterialStyleResource(context, value)
      }
      else -> null
    }
  }

  // Private helper methods

  private fun resolveMaterialThemeColor(context: Context, attrRef: String, def: Int): Int {
    return try {
      val attrName = attrRef.removePrefix("?attr/")
      val attrId = getAttributeId(context, attrName)
      if (attrId != 0) {
        resolveThemeAttribute(context, attrRef, def)
      } else {
        def
      }
    } catch (e: Exception) {
      def
    }
  }

  private fun resolveMaterialColorResource(context: Context, colorRef: String, def: Int): Int {
    return try {
      val colorName = colorRef.removePrefix("@color/")
      val colorId = getColorResourceId(context, colorName)
      if (colorId != 0) {
        ContextCompat.getColor(context, colorId)
      } else {
        def
      }
    } catch (e: Exception) {
      def
    }
  }

  private fun resolveMaterialStyleResource(context: Context, styleRef: String): Int? {
    return try {
      val styleName = styleRef.removePrefix("@style/")
      getStyleResourceId(context, styleName)
    } catch (e: Exception) {
      null
    }
  }

  private fun resolveMaterialThemeStyle(context: Context, attrRef: String): Int? {
    return try {
      val attrName = attrRef.removePrefix("?attr/")
      val attrId = getAttributeId(context, attrName)
      if (attrId != 0) {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(attrId, typedValue, true)) {
          typedValue.resourceId
        } else null
      } else null
    } catch (e: Exception) {
      null
    }
  }

  private fun resolveMaterialThemeDrawable(
      context: Context,
      attrRef: String,
      def: Drawable,
  ): Drawable {
    return try {
      val attrName = attrRef.removePrefix("?attr/")
      val attrId = getAttributeId(context, attrName)
      if (attrId != 0) {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(attrId, typedValue, true)) {
          ContextCompat.getDrawable(context, typedValue.resourceId) ?: def
        } else def
      } else def
    } catch (e: Exception) {
      def
    }
  }

  private fun resolveMaterialIconDrawable(
      context: Context,
      drawableRef: String,
      def: Drawable,
  ): Drawable {
    return try {
      val drawableName = drawableRef.removePrefix("@drawable/")
      val drawableId = getDrawableResourceId(context, drawableName)
      if (drawableId != 0) {
        ContextCompat.getDrawable(context, drawableId) ?: def
      } else def
    } catch (e: Exception) {
      def
    }
  }

  private fun isMaterialIcon(drawableRef: String): Boolean {
    val drawableName = drawableRef.removePrefix("@drawable/")
    return drawableName.startsWith("ic_") ||
        drawableName.startsWith("material_") ||
        drawableName.contains("_24dp")
  }

  private fun resolveThemeAttribute(context: Context, attrRef: String, def: Int): Int {
    return try {
      val attrName = attrRef.removePrefix("?attr/").removePrefix("?android:attr/")
      val attrId = getAttributeId(context, attrName)
      if (attrId != 0) {
        val typedValue = TypedValue()
        if (context.theme.resolveAttribute(attrId, typedValue, true)) {
          when (typedValue.type) {
            TypedValue.TYPE_INT_COLOR_ARGB8,
            TypedValue.TYPE_INT_COLOR_RGB8,
            TypedValue.TYPE_INT_COLOR_ARGB4,
            TypedValue.TYPE_INT_COLOR_RGB4 -> typedValue.data
            else -> def
          }
        } else def
      } else def
    } catch (e: Exception) {
      def
    }
  }

  // Resource ID resolution helpers (these need to be implemented based on your resource system)
  private fun getAttributeId(context: Context, attrName: String): Int {
    return context.resources.getIdentifier(attrName, "attr", context.packageName)
  }

  private fun getColorResourceId(context: Context, colorName: String): Int {
    return context.resources.getIdentifier(colorName, "color", context.packageName)
  }

  private fun getStyleResourceId(context: Context, styleName: String): Int {
    return context.resources.getIdentifier(styleName, "style", context.packageName)
  }

  private fun getDrawableResourceId(context: Context, drawableName: String): Int {
    return context.resources.getIdentifier(drawableName, "drawable", context.packageName)
  }
}
