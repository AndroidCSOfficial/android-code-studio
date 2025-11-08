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

package com.tom.rv2ide.inflater.internal.adapters

import android.content.Context
import android.view.View
import com.google.android.material.button.MaterialButton
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/** MaterialButton adapter with Material Design 3 support. */
open class ImageButtonAdapter : BaseButtonAdapter<MaterialButton>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(MaterialButton::class.java, string.widget_mbutton, drawable.ic_widget_button)
    )
  }

  override fun createAttrHandlers(
      create: (String, AttributeHandlerScope<MaterialButton>.() -> Unit) -> Unit
  ) {
    super.createAttrHandlers(create)

    // Material Design specific attributes
    create("icon") {
      val drawable = parseDrawable(context, value)
      view.icon = drawable
    }

    create("iconSize") {
      val size = parseDimension(context, value)
      if (size > 0) view.iconSize = size
    }

    create("iconGravity") { view.iconGravity = parseIconGravity(value) }

    create("iconPadding") {
      val padding = parseDimension(context, value)
      if (padding >= 0) view.iconPadding = padding
    }

    create("cornerRadius") {
      val radius = parseDimensionF(context, value)
      if (radius >= 0) view.cornerRadius = radius.toInt()
    }

    create("strokeWidth") {
      val width = parseDimensionF(context, value)
      if (width >= 0) view.strokeWidth = width.toInt()
    }

    create("strokeColor") {
      val color = parseColor(context, value)
      view.strokeColor = android.content.res.ColorStateList.valueOf(color)
    }

    create("rippleColor") {
      val color = parseColor(context, value)
      view.rippleColor = android.content.res.ColorStateList.valueOf(color)
    }
  }

  override fun onCreateView(name: String, context: Context): View? {
    return try {
      MaterialButton(context)
    } catch (e: Exception) {
      null
    }
  }

  override fun applyBasic(view: IView) {
    super.applyBasic(view)

    // Apply Material Design 3 defaults
    val button = view.view as MaterialButton
    button.text = "Material Button"
    button.cornerRadius = 20 // M3 default corner radius
    button.strokeWidth = 0
    button.elevation = 0f
  }

  private fun parseIconGravity(value: String): Int {
    return when (value.lowercase()) {
      "start" -> MaterialButton.ICON_GRAVITY_START
      "end" -> MaterialButton.ICON_GRAVITY_END
      "text_start" -> MaterialButton.ICON_GRAVITY_TEXT_START
      "text_end" -> MaterialButton.ICON_GRAVITY_TEXT_END
      "text_top" -> MaterialButton.ICON_GRAVITY_TEXT_TOP
      else -> MaterialButton.ICON_GRAVITY_START
    }
  }
}
