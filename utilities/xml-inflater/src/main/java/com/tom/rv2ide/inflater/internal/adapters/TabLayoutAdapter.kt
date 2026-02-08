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

import com.google.android.material.tabs.TabLayout
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Material TabLayout adapter with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(TabLayout::class)
@IncludeInDesigner(group = GOOGLE)
open class TabLayoutAdapter<T : TabLayout> : HorizontalScrollViewAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(TabLayout::class.java, string.widget_tab_layout, drawable.ic_widget_tabs)
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 TabLayout specific attributes
    create("app:tabMode") {
      when (value.lowercase()) {
        "fixed" -> view.tabMode = TabLayout.MODE_FIXED
        "scrollable" -> view.tabMode = TabLayout.MODE_SCROLLABLE
        "auto" -> {
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            view.tabMode = TabLayout.MODE_AUTO
          }
        }
      }
    }

    create("app:tabGravity") {
      when (value.lowercase()) {
        "fill" -> view.tabGravity = TabLayout.GRAVITY_FILL
        "center" -> view.tabGravity = TabLayout.GRAVITY_CENTER
        "start" -> view.tabGravity = TabLayout.GRAVITY_START
      }
    }

    create("app:tabIndicatorColor") {
      val color = parseColor(context, value)
      view.setSelectedTabIndicatorColor(color)
    }

    create("app:tabIndicatorHeight") {
      val height = parseDimensionF(context, value)
      if (height > 0) view.setSelectedTabIndicatorHeight(height.toInt())
    }

    create("app:tabTextColor") {
      val color = parseColor(context, value)
      view.setTabTextColors(color, color)
    }

    create("app:tabSelectedTextColor") {
      val color = parseColor(context, value)
      view.setTabTextColors(view.tabTextColors?.defaultColor ?: 0xFF000000.toInt(), color)
    }

    create("app:tabBackground") {
      val drawable = parseDrawable(context, value)
      drawable?.let { view.setTabBackground(it) }
    }

    create("app:tabMinWidth") {
      val width = parseDimensionF(context, value)
      if (width > 0) view.tabMinWidth = width.toInt()
    }

    create("app:tabMaxWidth") {
      val width = parseDimensionF(context, value)
      if (width > 0) view.tabMaxWidth = width.toInt()
    }

    create("app:tabPaddingStart") {
      val padding = parseDimensionF(context, value)
      if (padding >= 0) view.tabPaddingStart = padding.toInt()
    }

    create("app:tabPaddingEnd") {
      val padding = parseDimensionF(context, value)
      if (padding >= 0) view.tabPaddingEnd = padding.toInt()
    }

    create("app:tabRippleColor") {
      val color = parseColor(context, value)
      try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
          view.setTabRippleColorResource(android.R.color.transparent)
        }
      } catch (e: Exception) {
        // Not available on this API
      }
    }

    create("app:badgeTextColor") {
      // Badge colors handled per-tab
    }
  }
}
