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

import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/** MaterialTextView adapter with Material Design 3 support. */
@ViewAdapter(MaterialTextView::class)
@IncludeInDesigner(group = GOOGLE)
open class MaterialTextViewAdapter<T : MaterialTextView> : TextViewAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(
            MaterialTextView::class.java,
            string.widget_text_view,
            drawable.ic_widget_text_view,
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 text attributes - handle both with and without namespace
    create("textAppearance") {
      try {
        val resId = tryResolveResourceId(context, value)
        if (resId != 0) {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            view.setTextAppearance(resId)
          }
        }
      } catch (e: Exception) {
        // Fallback: ignore if resource not found
      }
    }

    create("textColor") {
      val color = parseColor(context, value)
      view.setTextColor(color)
    }

    create("textSize") {
      val size = parseDimensionF(context, value)
      if (size > 0) view.textSize = size
    }

    create("textStyle") {
      val style = parseTextStyle(value)
      view.setTypeface(null, style)
    }

    create("fontFamily") {
      try {
        val typeface = android.graphics.Typeface.create(value, android.graphics.Typeface.NORMAL)
        view.typeface = typeface
      } catch (e: Exception) {
        // Ignore if font not found
      }
    }

    create("lineHeight") {
      val height = parseDimensionF(context, value)
      if (height > 0) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
          view.lineHeight = height.toInt()
        }
      }
    }

    create("lineSpacing") {
      val spacing = parseDimensionF(context, value)
      if (spacing >= 0) view.lineSpacing(spacing, 1f)
    }

    create("letterSpacing") {
      val spacing = value.toFloatOrNull() ?: 0f
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
        view.letterSpacing = spacing
      }
    }

    create("enabled") {
      val enabled = parseBoolean(value)
      view.isEnabled = enabled
    }

    create("alpha") {
      val alpha = value.toFloatOrNull() ?: 1f
      view.alpha = alpha
    }
  }

  private fun tryResolveResourceId(context: android.content.Context, resName: String): Int {
    return try {
      val parts = resName.split("/")
      if (parts.size == 2 && parts[0].startsWith("@")) {
        val type = parts[0].substring(1)
        val name = parts[1]
        context.resources.getIdentifier(name, type, context.packageName)
      } else 0
    } catch (e: Exception) {
      0
    }
  }

  override fun applyBasic(view: IView) {
    super.applyBasic(view)

    // Apply M3 default styling
    val textView = view.view as MaterialTextView
    textView.text = "Material Text"
    textView.textSize = 14f // 14sp for M3 body text
  }
}
