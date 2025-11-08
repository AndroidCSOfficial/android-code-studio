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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.card.MaterialCardView
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.INamespace
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.internal.LayoutFile
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.inflater.utils.newAttribute
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/*
 * MaterialCardView adapter with Material Design 3 support.
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

@ViewAdapter(MaterialCardView::class)
@IncludeInDesigner(group = GOOGLE)
open class MaterialCardViewAdapter<T : MaterialCardView> : ViewGroupAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        MaterialCardViewWidget(title = string.widget_mcardview, icon = drawable.ic_settings)
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 card attributes - handle both with and without namespace
    create("cardCornerRadius") {
      val radius = parseDimensionF(context, value)
      if (radius >= 0) view.radius = radius
    }

    create("cardElevation") {
      val elevation = parseDimensionF(context, value)
      if (elevation >= 0) view.cardElevation = elevation
    }

    create("cardBackgroundColor") {
      val color = parseColor(context, value)
      view.setCardBackgroundColor(color)
    }

    create("strokeColor") {
      val color = parseColor(context, value)
      view.strokeColor = color
    }

    create("strokeWidth") {
      val width = parseDimensionF(context, value)
      if (width >= 0) view.strokeWidth = width.toInt()
    }

    create("contentPadding") {
      val padding = parseDimension(context, value)
      if (padding >= 0) view.setContentPadding(padding, padding, padding, padding)
    }

    create("contentPaddingLeft") {
      val padding = parseDimension(context, value)
      if (padding >= 0) {
        view.setContentPadding(
            padding,
            view.contentPaddingTop,
            view.contentPaddingRight,
            view.contentPaddingBottom,
        )
      }
    }

    create("contentPaddingTop") {
      val padding = parseDimension(context, value)
      if (padding >= 0) {
        view.setContentPadding(
            view.contentPaddingLeft,
            padding,
            view.contentPaddingRight,
            view.contentPaddingBottom,
        )
      }
    }

    create("contentPaddingRight") {
      val padding = parseDimension(context, value)
      if (padding >= 0) {
        view.setContentPadding(
            view.contentPaddingLeft,
            view.contentPaddingTop,
            padding,
            view.contentPaddingBottom,
        )
      }
    }

    create("contentPaddingBottom") {
      val padding = parseDimension(context, value)
      if (padding >= 0) {
        view.setContentPadding(
            view.contentPaddingLeft,
            view.contentPaddingTop,
            view.contentPaddingRight,
            padding,
        )
      }
    }
  }

  override fun defaultNamespace(): INamespace? {
    return INamespace.APP
  }

  override fun canHandleNamespace(nsUri: String?): Boolean {
    return super.canHandleNamespace(nsUri) || nsUri == INamespace.APP.uri
  }

  override fun onCreateView(name: String, context: Context): View? {
    return try {
      MaterialCardView(context)
    } catch (e: Exception) {
      null
    }
  }

  override fun applyBasic(view: IView) {
    super.applyBasic(view)

    // Apply M3 default styling
    val cardView = view.view as MaterialCardView
    cardView.radius = 12f // 12dp for M3
    cardView.cardElevation = 1f // Lower elevation for M3
    cardView.strokeWidth = 0
  }

  private class MaterialCardViewWidget(@StringRes title: Int, @DrawableRes icon: Int) :
      UiWidget(MaterialCardView::class.java, title, icon) {

    override fun createView(context: Context, parent: ViewGroup, layoutFile: LayoutFile): IView {
      return super.createView(context, parent, layoutFile).apply {
        // Set some default attributes for better visual appearance in designer
        addAttribute(
            newAttribute(
                this,
                namespace = INamespace.APP,
                name = "cardCornerRadius",
                value = "12dp",
            )
        )
        addAttribute(
            newAttribute(this, namespace = INamespace.APP, name = "cardElevation", value = "1dp")
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
            newAttribute(this, namespace = INamespace.APP, name = "contentPadding", value = "16dp")
        )
      }
    }
  }
}
