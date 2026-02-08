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

import com.google.android.material.navigation.NavigationView
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Material NavigationView adapter with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(NavigationView::class)
@IncludeInDesigner(group = GOOGLE)
open class NavigationViewAdapter<T : NavigationView> : FrameLayoutAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(
            NavigationView::class.java,
            string.widget_navigation_view,
            drawable.ic_widget_navigation_drawer,
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 NavigationView specific attributes
    create("menu") {
      // Menu items are typically defined in separate menu resource files
      log.debug("NavigationView menu resource: $value")
    }

    create("headerLayout") {
      // Header is typically a separate layout file
      log.debug("NavigationView header layout: $value")
    }

    create("itemIconTint") {
      val color = parseColor(context, value)
      view.itemIconTintList = android.content.res.ColorStateList.valueOf(color)
    }

    create("itemTextColor") {
      val color = parseColor(context, value)
      view.itemTextColor = android.content.res.ColorStateList.valueOf(color)
    }

    create("itemBackground") {
      val drawable = parseDrawable(context, value)
      drawable?.let { view.itemBackground = it }
    }

    create("itemHorizontalPadding") {
      val padding = parseDimensionF(context, value)
      if (padding >= 0) view.itemHorizontalPadding = padding.toInt()
    }

    create("itemVerticalPadding") {
      val padding = parseDimensionF(context, value)
      if (padding >= 0) view.itemVerticalPadding = padding.toInt()
    }

    create("elevation") {
      val elevation = parseDimensionF(context, value)
      if (elevation >= 0) view.elevation = elevation
    }

    create("backgroundColor") {
      val color = parseColor(context, value)
      view.setBackgroundColor(color)
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(NavigationViewAdapter::class.java)
  }
}
