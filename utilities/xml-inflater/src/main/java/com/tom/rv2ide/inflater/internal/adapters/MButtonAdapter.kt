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
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.WIDGETS
import com.tom.rv2ide.inflater.INamespace
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.internal.LayoutFile
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.inflater.utils.newAttribute
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [MaterialButton].
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
@ViewAdapter(MaterialButton::class)
@IncludeInDesigner(group = WIDGETS)
open class MButtonAdapter<T : MaterialButton> : TextViewAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(MaterialButton::class.java, string.widget_mbutton, drawable.ic_widget_button)
    )
  }

  private fun parseIconGravity(value: String): Int {
    return when (value.lowercase()) {
      "start" -> MaterialButton.ICON_GRAVITY_START
      "end" -> MaterialButton.ICON_GRAVITY_END
      "top" -> MaterialButton.ICON_GRAVITY_TOP
      "text_start" -> MaterialButton.ICON_GRAVITY_TEXT_START
      "text_end" -> MaterialButton.ICON_GRAVITY_TEXT_END
      "text_top" -> MaterialButton.ICON_GRAVITY_TEXT_TOP
      else -> MaterialButton.ICON_GRAVITY_START
    }
  }

  private class MaterialButtonWidget(@StringRes title: Int, @DrawableRes icon: Int) :
      UiWidget(MaterialButton::class.java, title, icon) {

    override fun createView(context: Context, parent: ViewGroup, layoutFile: LayoutFile): IView {
      return super.createView(context, parent, layoutFile).apply {
        // Set some default attributes for better visual appearance in designer
        addAttribute(
            newAttribute(this, namespace = null, name = "app:cardCornerRadius", value = "8dp")
        )
        addAttribute(
            newAttribute(this, namespace = null, name = "app:cardElevation", value = "4dp")
        )
        addAttribute(
            newAttribute(
                this,
                namespace = INamespace.ANDROID,
                name = "layout_margin",
                value = "8dp",
            )
        )
        addAttribute(
            newAttribute(
                this,
                namespace = INamespace.ANDROID,
                name = "layout_width",
                value = "match_parent",
            )
        )
        addAttribute(
            newAttribute(
                this,
                namespace = INamespace.ANDROID,
                name = "layout_height",
                value = "wrap_content",
            )
        )

        // Make it a proper container that can accept children
        addAttribute(
            newAttribute(
                this,
                namespace = null,
                name = "app:background",
                value = "?attr/colorOnPrimary",
            )
        )
      }
    }
  }
}
