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

import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [Chip] with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(Chip::class)
@IncludeInDesigner(group = GOOGLE)
open class ChipAdapter<T : Chip> : CompoundButtonAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(UiWidget(Chip::class.java, string.widget_chip, drawable.ic_widget_chip))
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 Chip specific attributes
    create("chipIcon") { view.chipIcon = parseDrawable(context, value) }

    create("chipIconTint") {
      val color = parseColor(context, value)
      view.chipIconTintList = android.content.res.ColorStateList.valueOf(color)
    }

    create("closeIcon") { view.closeIcon = parseDrawable(context, value) }

    create("closeIconTint") {
      val color = parseColor(context, value)
      view.closeIconTintList = android.content.res.ColorStateList.valueOf(color)
    }

    create("checkedIcon") { view.checkedIcon = parseDrawable(context, value) }

    create("checkedIconTint") {
      val color = parseColor(context, value)
      view.checkedIconTintList = android.content.res.ColorStateList.valueOf(color)
    }

    create("chipBackgroundColor") {
      val color = parseColor(context, value)
      view.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(color))
    }

    create("chipStrokeColor") {
      val color = parseColor(context, value)
      view.setChipStrokeColor(android.content.res.ColorStateList.valueOf(color))
    }

    create("chipStrokeWidth") {
      val width = parseDimensionF(context, value)
      if (width >= 0) view.chipStrokeWidth = width
    }

    create("chipCornerRadius") {
      val radius = parseDimensionF(context, value)
      if (radius >= 0) view.chipCornerRadius = radius
    }

    create("rippleColor") {
      val color = parseColor(context, value)
      view.rippleColor = color
    }

    create("textColor") {
      val color = parseColor(context, value)
      view.setTextColor(color)
    }

    create("elevation") {
      val elevation = parseDimensionF(context, value)
      if (elevation >= 0) view.elevation = elevation
    }

    create("motionEasing") {
      // Motion easing typically handled through styles
    }
  }
}

/**
 * Attribute adapter for [ChipGroup] with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(ChipGroup::class)
@IncludeInDesigner(group = GOOGLE)
open class ChipGroupAdapter<T : ChipGroup> : ViewGroupAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(UiWidget(ChipGroup::class.java, string.widget_chip_group, drawable.ic_widget_chip))
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 ChipGroup specific attributes
    create("singleSelection") {
      val single = parseBoolean(value)
      view.isSingleSelection = single
    }

    create("selectionRequired") {
      val required = parseBoolean(value)
      view.isSelectionRequired = required
    }

    create("checkedChip") {
      val id = value.toIntOrNull()
      if (id != null && id > 0) {
        view.check(id)
      }
    }

    create("chipSpacing") {
      val spacing = parseDimensionF(context, value)
      if (spacing >= 0) view.chipSpacing = spacing.toInt()
    }

    create("chipSpacingHorizontal") {
      val spacing = parseDimensionF(context, value)
      if (spacing >= 0) view.chipSpacingHorizontal = spacing.toInt()
    }

    create("chipSpacingVertical") {
      val spacing = parseDimensionF(context, value)
      if (spacing >= 0) view.chipSpacingVertical = spacing.toInt()
    }
  }
}
