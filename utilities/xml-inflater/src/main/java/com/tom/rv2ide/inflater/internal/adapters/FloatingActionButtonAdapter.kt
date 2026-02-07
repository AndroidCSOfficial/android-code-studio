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

import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [FloatingActionButton] with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(FloatingActionButton::class)
@IncludeInDesigner(group = GOOGLE)
open class FloatingActionButtonAdapter<T : FloatingActionButton> : ImageButtonAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(
            FloatingActionButton::class.java,
            string.widget_fab,
            drawable.ic_widget_floating_action_button,
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 FAB specific attributes
    create("size") {
      when (value.lowercase()) {
        "auto" -> view.size = FloatingActionButton.SIZE_AUTO
        "mini" -> view.size = FloatingActionButton.SIZE_MINI
        "normal" -> view.size = FloatingActionButton.SIZE_NORMAL
      }
    }

    create("fabsize") {
      when (value.lowercase()) {
        "auto" -> view.size = FloatingActionButton.SIZE_AUTO
        "mini" -> view.size = FloatingActionButton.SIZE_MINI
        "normal" -> view.size = FloatingActionButton.SIZE_NORMAL
      }
    }

    create("fabCustomSize") {
      val size = parseDimensionF(context, value)
      if (size > 0) view.customSize = size.toInt()
    }

    create("elevation") {
      val elevation = parseDimensionF(context, value)
      if (elevation >= 0) view.elevation = elevation
    }

    create("hoveredFocusedTranslationZ") {
      val translationZ = parseDimensionF(context, value)
      if (translationZ >= 0) view.hoveredFocusedTranslationZ = translationZ
    }

    create("pressedTranslationZ") {
      val translationZ = parseDimensionF(context, value)
      if (translationZ >= 0) view.pressedTranslationZ = translationZ
    }

    create("fabBackgroundColor") {
      val color = parseColor(context, value)
      view.setBackgroundColor(color)
    }

    create("backgroundTint") {
      val color = parseColor(context, value)
      view.backgroundTintList = createColorStateList(color)
    }

    create("rippleColor") {
      val color = parseColor(context, value)
      view.rippleColor = color
    }

    create("borderWidth") {
      val width = parseDimensionF(context, value)
      if (width >= 0) view.borderWidth = width.toInt()
    }

    create("shapeAppearance") {
      // Shape appearance is typically handled through styles
      // Store for reference
    }
  }

  private fun createColorStateList(color: Int) =
      android.content.res.ColorStateList.valueOf(color)
}
