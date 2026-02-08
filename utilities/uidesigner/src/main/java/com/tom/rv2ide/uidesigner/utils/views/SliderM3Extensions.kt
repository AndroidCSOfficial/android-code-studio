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
import com.google.android.material.slider.Slider
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.M3Utils
import java.io.File
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("SliderM3Extensions")

/**
 * Material Slider M3 preview extension
 *
 * @author Enhancement for M3 compatibility
 */
fun Slider.applyM3Preview(
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
      "value" -> applyValueM3(value)
      "valuefrom" -> applyValueFromM3(value)
      "valueto" -> applyValueToM3(value)
      "stepsize" -> applyStepSizeM3(value)
      "trackheight" -> applyTrackHeightM3(value, context)
      "trackcolorinactive" -> applyTrackColorInactiveM3(value, context)
      "trackcoloractive" -> applyTrackColorActiveM3(value, context)
      "thumbcolor" -> applyThumbColorM3(value, context)
      "labelcolor" -> applyLabelColorM3(value, context)
      "elevation" -> applyElevationM3(value, context)
      else -> {
        log.debug("Unsupported Slider attribute: $normalizedAttrName")
        false
      }
    }
  } catch (e: Exception) {
    log.error("Failed to apply Slider M3 attribute: $normalizedAttrName", e)
    false
  }
}

private fun Slider.applyValueM3(value: String): Boolean {
  return try {
    val sliderValue = value.toFloatOrNull() ?: 0f
    if (sliderValue >= valueFrom && sliderValue <= valueTo) {
      this.value = sliderValue
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyValueFromM3(value: String): Boolean {
  return try {
    val valueFrom = value.toFloatOrNull() ?: 0f
    this.valueFrom = valueFrom
    true
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyValueToM3(value: String): Boolean {
  return try {
    val valueTo = value.toFloatOrNull() ?: 100f
    this.valueTo = valueTo
    true
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyStepSizeM3(value: String): Boolean {
  return try {
    val stepSize = value.toFloatOrNull() ?: 1f
    if (stepSize > 0) {
      this.stepSize = stepSize
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyTrackHeightM3(heightValue: String, context: Context): Boolean {
  return try {
    val height = M3Utils.parseDimensionM3(heightValue, context)
    if (height > 0) {
      trackHeight = height
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyTrackColorInactiveM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setTrackInactiveColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyTrackColorActiveM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setTrackActiveColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyThumbColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null) {
      setThumbColor(color)
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyLabelColorM3(colorValue: String, context: Context): Boolean {
  return try {
    val color = M3Utils.parseColorM3(colorValue, context)
    if (color != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      // Label color handled through formatter
      true
    } else false
  } catch (e: Exception) {
    false
  }
}

private fun Slider.applyElevationM3(elevationValue: String, context: Context): Boolean {
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
