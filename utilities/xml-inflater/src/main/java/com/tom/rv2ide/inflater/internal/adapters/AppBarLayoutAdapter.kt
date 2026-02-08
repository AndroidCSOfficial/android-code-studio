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

import com.google.android.material.appbar.AppBarLayout
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Material AppBarLayout adapter with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(AppBarLayout::class)
@IncludeInDesigner(group = GOOGLE)
open class AppBarLayoutAdapter<T : AppBarLayout> : ViewGroupAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(
            AppBarLayout::class.java,
            string.widget_app_bar_layout,
            drawable.ic_widget_appbar,
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 AppBar specific attributes
    create("elevation") {
      val elevation = parseDimensionF(context, value)
      if (elevation >= 0) view.elevation = elevation
    }

    create("backgroundColor") {
      val color = parseColor(context, value)
      view.setBackgroundColor(color)
    }

    create("elevated") {
      val elevated = parseBoolean(value)
      if (elevated) {
        view.elevation = 4f // M3 default elevated elevation
      }
    }

    create("statusBarForeground") {
      val drawable = parseDrawable(context, value)
      drawable?.let { view.statusBarForeground = it }
    }

    create("liftOnScrollListener") {
      // Listeners are typically set in code, not XML
    }

    create("liftable") {
      try {
        val liftable = parseBoolean(value)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
          view.isLiftOnScroll = liftable
        }
      } catch (e: Exception) {
        // Not supported on this API
      }
    }
  }
}
