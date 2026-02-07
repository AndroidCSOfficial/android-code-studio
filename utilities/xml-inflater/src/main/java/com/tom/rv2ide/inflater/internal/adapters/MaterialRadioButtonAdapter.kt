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

import com.google.android.material.radiobutton.MaterialRadioButton
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [MaterialRadioButton] with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(MaterialRadioButton::class)
@IncludeInDesigner(group = GOOGLE)
open class MaterialRadioButtonAdapter<T : MaterialRadioButton> : CompoundButtonAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(
            MaterialRadioButton::class.java,
            string.widget_radiobutton,
            drawable.ic_widget_radiobutton,
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 RadioButton specific attributes
    create("useMaterialThemeColors") {
      try {
        val use = parseBoolean(value)
        // Handled through Material theme
      } catch (e: Exception) {
        // Ignore
      }
    }

    create("buttonTint") {
      val color = parseColor(context, value)
      view.buttonTintList = android.content.res.ColorStateList.valueOf(color)
    }

    create("buttonTintMode") {
      // Typically handled through styles
    }

    create("checked") {
      val checked = parseBoolean(value)
      view.isChecked = checked
    }

    create("enabled") {
      val enabled = parseBoolean(value)
      view.isEnabled = enabled
    }

    create("text") { view.text = value }

    create("textColor") {
      val color = parseColor(context, value)
      view.setTextColor(color)
    }

    create("textAppearance") {
      // Text appearance typically handled through styles
    }
  }
}
