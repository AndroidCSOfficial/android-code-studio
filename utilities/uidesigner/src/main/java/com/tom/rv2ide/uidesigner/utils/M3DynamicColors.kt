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
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import org.slf4j.LoggerFactory

/**
 * Material Design 3 Dynamic Colors Support (Material You)
 *
 * Manages Material You (dynamic colors) on Android 12+ with fallback to static M3 palette
 * for older API levels.
 *
 * @author Enhancement for Material Design 3
 */
object M3DynamicColors {
  private val log = LoggerFactory.getLogger(M3DynamicColors::class.java)

  /**
   * Represents a complete Material 3 color scheme with both static and dynamic support
   */
  data class M3ColorScheme(
      // Primary colors
      val primary: Int,
      val onPrimary: Int,
      val primaryContainer: Int,
      val onPrimaryContainer: Int,

      // Secondary colors
      val secondary: Int,
      val onSecondary: Int,
      val secondaryContainer: Int,
      val onSecondaryContainer: Int,

      // Tertiary colors
      val tertiary: Int,
      val onTertiary: Int,
      val tertiaryContainer: Int,
      val onTertiaryContainer: Int,

      // Error state
      val error: Int,
      val onError: Int,
      val errorContainer: Int,
      val onErrorContainer: Int,

      // Surface variants
      val surface: Int,
      val onSurface: Int,
      val surfaceVariant: Int,
      val onSurfaceVariant: Int,
      val surfaceTint: Int,
      val surfaceContainer: Int,
      val surfaceContainerHigh: Int,
      val surfaceContainerHighest: Int,
      val surfaceContainerLow: Int,
      val surfaceContainerLowest: Int,

      // Outline
      val outline: Int,
      val outlineVariant: Int,

      // Scrim
      val scrim: Int,

      // Inverse colors
      val inversePrimary: Int,
      val inverseSurface: Int,
      val inverseOnSurface: Int,

      // Background (for dark mode)
      val background: Int,
      val onBackground: Int,
  )

  /**
   * Get Material 3 dynamic color scheme for current device
   * - Android 12+: Uses system dynamic colors from wallpaper
   * - Android < 12: Returns static M3 default palette
   */
  fun getDynamicColorScheme(context: Context, isDarkTheme: Boolean): M3ColorScheme {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      // Android 12+: Load dynamic colors from system
      loadDynamicColorScheme(context, isDarkTheme)
    } else {
      // Fallback: Static M3 palette
      getStaticColorScheme(isDarkTheme)
    }
  }

  /**
   * Load dynamic colors from Android 12+ system
   */
  private fun loadDynamicColorScheme(context: Context, isDarkTheme: Boolean): M3ColorScheme {
    val prefix = if (isDarkTheme) "system" else "system"
    val colorMap = mutableMapOf<String, Int>()

    // Map of system color names to parse
    val systemColors =
        listOf(
            "accent1",
            "accent2",
            "accent3",
            "neutral1",
            "neutral2",
        )

    // Load all system accent colors (0-900 tones)
    for (accentName in systemColors) {
      for (tone in listOf(0, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900)) {
        val resourceName = "${prefix}_${accentName}_${tone}"
        try {
          val resourceId = context.resources.getIdentifier(resourceName, "color", "android")
          if (resourceId != 0) {
            colorMap[resourceName] = ContextCompat.getColor(context, resourceId)
            log.debug("Loaded dynamic color: $resourceName")
          }
        } catch (e: Exception) {
          // Color not available on this API level
        }
      }
    }

    // Map system colors to M3 tokens
    return if (colorMap.isNotEmpty()) {
      mapSystemColorsToM3(colorMap, isDarkTheme)
    } else {
      log.warn("Failed to load dynamic colors, using static palette")
      getStaticColorScheme(isDarkTheme)
    }
  }

  /**
   * Map Android 12+ system colors to Material 3 tokens
   */
  private fun mapSystemColorsToM3(
      systemColors: Map<String, Int>,
      isDarkTheme: Boolean,
  ): M3ColorScheme {
    // Default to tone 500 for primary color, tone 700 for darker variants
    val toneLight = if (isDarkTheme) 200 else 500
    val toneDark = if (isDarkTheme) 100 else 700

    // Extract primary colors from system_accent1
    val primary = systemColors["system_accent1_${if (isDarkTheme) 200 else 500}"] ?: Color.BLUE
    val onPrimary = systemColors["system_accent1_900"] ?: Color.WHITE
    val primaryContainer =
        systemColors["system_accent1_${if (isDarkTheme) 100 else 90}"] ?: Color.LTGRAY
    val onPrimaryContainer =
        systemColors["system_accent1_${if (isDarkTheme) 900 else 10}"] ?: Color.BLACK

    // Extract secondary colors from system_accent2
    val secondary = systemColors["system_accent2_${if (isDarkTheme) 200 else 500}"] ?: Color.GRAY
    val onSecondary =
        systemColors["system_accent2_${if (isDarkTheme) 900 else 0}"] ?: Color.WHITE
    val secondaryContainer =
        systemColors["system_accent2_${if (isDarkTheme) 100 else 90}"] ?: Color.LTGRAY
    val onSecondaryContainer =
        systemColors["system_accent2_${if (isDarkTheme) 900 else 10}"] ?: Color.BLACK

    // Extract tertiary colors from system_accent3
    val tertiary = systemColors["system_accent3_${if (isDarkTheme) 200 else 500}"] ?: Color.GRAY
    val onTertiary =
        systemColors["system_accent3_${if (isDarkTheme) 900 else 0}"] ?: Color.WHITE
    val tertiaryContainer =
        systemColors["system_accent3_${if (isDarkTheme) 100 else 90}"] ?: Color.LTGRAY
    val onTertiaryContainer =
        systemColors["system_accent3_${if (isDarkTheme) 900 else 10}"] ?: Color.BLACK

    // Error colors (typically red, not from accent)
    val error = Color.parseColor(if (isDarkTheme) "#F2B8B5" else "#B3261E")
    val onError = Color.parseColor(if (isDarkTheme) "#601410" else "#FFFFFF")
    val errorContainer = Color.parseColor(if (isDarkTheme) "#8C1D18" else "#F9DEDC")
    val onErrorContainer = Color.parseColor(if (isDarkTheme) "#FFDAD6" else "#410E0B")

    // Surface variants
    val surface = Color.parseColor(if (isDarkTheme) "#141218" else "#FFFBFE")
    val onSurface = Color.parseColor(if (isDarkTheme) "#E6E0E9" else "#1C1B1F")
    val surfaceVariant = systemColors["system_neutral1_700"] ?: Color.parseColor(if (isDarkTheme) "#49454F" else "#E7E0EC")
    val onSurfaceVariant =
        Color.parseColor(if (isDarkTheme) "#CAC4D0" else "#49454F")

    // Background
    val background = Color.parseColor(if (isDarkTheme) "#141218" else "#FFFBFE")
    val onBackground = Color.parseColor(if (isDarkTheme) "#E6E0E9" else "#1C1B1F")

    return M3ColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = primary,
        surfaceContainer = Color.parseColor(if (isDarkTheme) "#211F26" else "#F5F2F7"),
        surfaceContainerHigh = Color.parseColor(if (isDarkTheme) "#2B2930" else "#ECE9F0"),
        surfaceContainerHighest = Color.parseColor(if (isDarkTheme) "#36343B" else "#E7E4EA"),
        surfaceContainerLow = Color.parseColor(if (isDarkTheme) "#0F0D13" else "#F9F7FC"),
        surfaceContainerLowest = Color.parseColor(if (isDarkTheme) "#000000" else "#FFFFFF"),
        outline = Color.parseColor(if (isDarkTheme) "#79747E" else "#79747E"),
        outlineVariant = Color.parseColor(if (isDarkTheme) "#49454F" else "#CAC4D0"),
        scrim = Color.parseColor("#000000"),
        inversePrimary = Color.parseColor(if (isDarkTheme) "#D0BCFF" else "#6750A4"),
        inverseSurface = Color.parseColor(if (isDarkTheme) "#E6E0E9" else "#313033"),
        inverseOnSurface = Color.parseColor(if (isDarkTheme) "#1C1B1F" else "#F5EFF7"),
        background = background,
        onBackground = onBackground,
    )
  }

  /**
   * Get static Material 3 default color scheme
   */
  fun getStaticColorScheme(isDarkTheme: Boolean): M3ColorScheme {
    return if (isDarkTheme) {
      M3ColorScheme(
          primary = Color.parseColor("#D0BCFF"),
          onPrimary = Color.parseColor("#21005D"),
          primaryContainer = Color.parseColor("#4F378B"),
          onPrimaryContainer = Color.parseColor("#EADDFF"),
          secondary = Color.parseColor("#CBC4CF"),
          onSecondary = Color.parseColor("#332D41"),
          secondaryContainer = Color.parseColor("#4A4458"),
          onSecondaryContainer = Color.parseColor("#E8DEF8"),
          tertiary = Color.parseColor("#EFB8C8"),
          onTertiary = Color.parseColor("#492532"),
          tertiaryContainer = Color.parseColor("#633B48"),
          onTertiaryContainer = Color.parseColor("#FFD8E4"),
          error = Color.parseColor("#F2B8B5"),
          onError = Color.parseColor("#601410"),
          errorContainer = Color.parseColor("#8C1D18"),
          onErrorContainer = Color.parseColor("#FFDAD6"),
          surface = Color.parseColor("#141218"),
          onSurface = Color.parseColor("#E6E0E9"),
          surfaceVariant = Color.parseColor("#49454F"),
          onSurfaceVariant = Color.parseColor("#CAC4D0"),
          surfaceTint = Color.parseColor("#D0BCFF"),
          surfaceContainer = Color.parseColor("#211F26"),
          surfaceContainerHigh = Color.parseColor("#2B2930"),
          surfaceContainerHighest = Color.parseColor("#36343B"),
          surfaceContainerLow = Color.parseColor("#0F0D13"),
          surfaceContainerLowest = Color.parseColor("#000000"),
          outline = Color.parseColor("#79747E"),
          outlineVariant = Color.parseColor("#49454F"),
          scrim = Color.parseColor("#000000"),
          inversePrimary = Color.parseColor("#6750A4"),
          inverseSurface = Color.parseColor("#E6E0E9"),
          inverseOnSurface = Color.parseColor("#1C1B1F"),
          background = Color.parseColor("#141218"),
          onBackground = Color.parseColor("#E6E0E9"),
      )
    } else {
      M3ColorScheme(
          primary = Color.parseColor("#6750A4"),
          onPrimary = Color.parseColor("#FFFFFF"),
          primaryContainer = Color.parseColor("#EADDFF"),
          onPrimaryContainer = Color.parseColor("#21005D"),
          secondary = Color.parseColor("#625B71"),
          onSecondary = Color.parseColor("#FFFFFF"),
          secondaryContainer = Color.parseColor("#E8DEF8"),
          onSecondaryContainer = Color.parseColor("#1D192B"),
          tertiary = Color.parseColor("#7D5260"),
          onTertiary = Color.parseColor("#FFFFFF"),
          tertiaryContainer = Color.parseColor("#FFD8E4"),
          onTertiaryContainer = Color.parseColor("#31111D"),
          error = Color.parseColor("#B3261E"),
          onError = Color.parseColor("#FFFFFF"),
          errorContainer = Color.parseColor("#F9DEDC"),
          onErrorContainer = Color.parseColor("#410E0B"),
          surface = Color.parseColor("#FFFBFE"),
          onSurface = Color.parseColor("#1C1B1F"),
          surfaceVariant = Color.parseColor("#E7E0EC"),
          onSurfaceVariant = Color.parseColor("#49454F"),
          surfaceTint = Color.parseColor("#6750A4"),
          surfaceContainer = Color.parseColor("#F5F2F7"),
          surfaceContainerHigh = Color.parseColor("#ECE9F0"),
          surfaceContainerHighest = Color.parseColor("#E7E4EA"),
          surfaceContainerLow = Color.parseColor("#F9F7FC"),
          surfaceContainerLowest = Color.parseColor("#FFFFFF"),
          outline = Color.parseColor("#79747E"),
          outlineVariant = Color.parseColor("#CAC4D0"),
          scrim = Color.parseColor("#000000"),
          inversePrimary = Color.parseColor("#D0BCFF"),
          inverseSurface = Color.parseColor("#313033"),
          inverseOnSurface = Color.parseColor("#F5EFF7"),
          background = Color.parseColor("#FFFBFE"),
          onBackground = Color.parseColor("#1C1B1F"),
      )
    }
  }

  /**
   * Get a specific color from the scheme by token name
   */
  fun getColorByToken(scheme: M3ColorScheme, tokenName: String): Int? {
    return when (tokenName.lowercase()) {
      "primary" -> scheme.primary
      "onprimary" -> scheme.onPrimary
      "primarycontainer" -> scheme.primaryContainer
      "onprimarycontainer" -> scheme.onPrimaryContainer
      "secondary" -> scheme.secondary
      "onsecondary" -> scheme.onSecondary
      "secondarycontainer" -> scheme.secondaryContainer
      "onsecondarycontainer" -> scheme.onSecondaryContainer
      "tertiary" -> scheme.tertiary
      "ontertiary" -> scheme.onTertiary
      "tertiarycontainer" -> scheme.tertiaryContainer
      "ontertiarycontainer" -> scheme.onTertiaryContainer
      "error" -> scheme.error
      "onerror" -> scheme.onError
      "errorcontainer" -> scheme.errorContainer
      "onerrorcontainer" -> scheme.onErrorContainer
      "surface" -> scheme.surface
      "onsurface" -> scheme.onSurface
      "surfacevariant" -> scheme.surfaceVariant
      "onsurfacevariant" -> scheme.onSurfaceVariant
      "surfacetint" -> scheme.surfaceTint
      "surfacecontainer" -> scheme.surfaceContainer
      "surfacecontainerhigh" -> scheme.surfaceContainerHigh
      "surfacecontainerhighest" -> scheme.surfaceContainerHighest
      "surfacecontainerlow" -> scheme.surfaceContainerLow
      "surfacecontainerlowest" -> scheme.surfaceContainerLowest
      "outline" -> scheme.outline
      "outlinevariant" -> scheme.outlineVariant
      "scrim" -> scheme.scrim
      "inverseprimary" -> scheme.inversePrimary
      "inversesurface" -> scheme.inverseSurface
      "inverseonsurface" -> scheme.inverseOnSurface
      "background" -> scheme.background
      "onbackground" -> scheme.onBackground
      else -> null
    }
  }
}
