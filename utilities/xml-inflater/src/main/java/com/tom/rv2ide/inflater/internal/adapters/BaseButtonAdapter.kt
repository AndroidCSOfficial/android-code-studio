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
import android.widget.Button
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.IViewAdapter

/**
 * Base adapter for button-like views.
 *
 * @author Akash Yadav
 */
abstract class BaseButtonAdapter<T : Button> : IViewAdapter<T>() {

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    // Common button attributes
    create("text") { view.text = parseString(value) }
    create("textColor") { view.setTextColor(parseColor(context, value)) }
    create("textSize") {
      val size = parseDimensionF(context, value)
      if (size > 0) view.textSize = size
    }
    create("textStyle") { view.setTypeface(view.typeface, parseTextStyle(value)) }
    create("enabled") { view.isEnabled = parseBoolean(value) }
    create("clickable") { view.isClickable = parseBoolean(value) }
    create("background") { view.background = parseDrawable(context, value) }
    create("padding") {
      val padding = parseDimension(context, value)
      view.setPadding(padding, padding, padding, padding)
    }
    create("paddingLeft") {
      view.setPadding(
          parseDimension(context, value),
          view.paddingTop,
          view.paddingRight,
          view.paddingBottom,
      )
    }
    create("paddingTop") {
      view.setPadding(
          view.paddingLeft,
          parseDimension(context, value),
          view.paddingRight,
          view.paddingBottom,
      )
    }
    create("paddingRight") {
      view.setPadding(
          view.paddingLeft,
          view.paddingTop,
          parseDimension(context, value),
          view.paddingBottom,
      )
    }
    create("paddingBottom") {
      view.setPadding(
          view.paddingLeft,
          view.paddingTop,
          view.paddingRight,
          parseDimension(context, value),
      )
    }
    create("layout_width") {
      val width = parseDimension(context, value)
      view.layoutParams = view.layoutParams?.apply { this.width = width }
    }
    create("layout_height") {
      val height = parseDimension(context, value)
      view.layoutParams = view.layoutParams?.apply { this.height = height }
    }
  }

  override fun onCreateView(name: String, context: Context): View? {
    return null // Let subclasses handle view creation
  }

  override fun applyBasic(view: IView) {
    val button = view.view as T
    button.text = "Button"

    // Set basic layout params
    button.layoutParams =
        android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
        )
  }

  override fun isRequiredAttribute(attribute: com.tom.rv2ide.inflater.IAttribute): Boolean {
    if (attribute.namespace?.uri != com.tom.rv2ide.inflater.INamespace.ANDROID.uri) {
      return false
    }

    return when (attribute.name) {
      "layout_width",
      "layout_height" -> true
      else -> false
    }
  }

  protected fun parseTextStyle(value: String): Int {
    return when (value.lowercase()) {
      "bold" -> android.graphics.Typeface.BOLD
      "italic" -> android.graphics.Typeface.ITALIC
      "bold_italic" -> android.graphics.Typeface.BOLD_ITALIC
      else -> android.graphics.Typeface.NORMAL
    }
  }
}
