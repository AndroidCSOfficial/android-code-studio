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

package com.tom.rv2ide.inflater.internal.adapters

import com.google.android.material.slider.Slider
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Material Slider adapter with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(Slider::class)
@IncludeInDesigner(group = GOOGLE)
open class SliderAdapter<T : Slider> : ViewAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(Slider::class.java, string.widget_slider, drawable.ic_widget_slider)
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 Slider specific attributes
    create("android:value") {
      val value = value.toFloatOrNull() ?: 0f
      if (value >= view.valueFrom && value <= view.valueTo) {
        view.value = value
      }
    }

    create("android:valueFrom") {
      val valueFrom = value.toFloatOrNull() ?: 0f
      view.valueFrom = valueFrom
    }

    create("android:valueTo") {
      val valueTo = value.toFloatOrNull() ?: 100f
      view.valueTo = valueTo
    }

    create("android:stepSize") {
      val stepSize = value.toFloatOrNull() ?: 1f
      if (stepSize > 0) view.stepSize = stepSize
    }

    create("android:trackHeight") {
      val height = parseDimensionF(context, value)
      if (height > 0) view.trackHeight = height.toInt()
    }

    create("app:trackColorInactive") {
      val color = parseColor(context, value)
      view.setTrackInactiveColor(color)
    }

    create("app:trackColorActive") {
      val color = parseColor(context, value)
      view.setTrackActiveColor(color)
    }

    create("app:thumbColor") {
      val color = parseColor(context, value)
      view.setThumbColor(color)
    }

    create("app:thumbStrokeColor") {
      val color = parseColor(context, value)
      view.setThumbStrokeColor(color)
    }

    create("app:tickColor") {
      val color = parseColor(context, value)
      view.setTickColor(color)
    }

    create("app:haloRadius") {
      val radius = parseDimensionF(context, value)
      if (radius > 0) view.haloRadius = radius.toInt()
    }

    create("app:labelBehavior") {
      when (value.lowercase()) {
        "withinbounds" -> view.setLabelFormatter { "${it.toInt()}" }
        "floating" -> view.setLabelFormatter { "${it.toInt()}" }
        "gone" -> {
          // Hide label
        }
      }
    }
  }
}
