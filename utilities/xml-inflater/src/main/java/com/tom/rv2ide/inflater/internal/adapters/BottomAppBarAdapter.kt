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

import com.google.android.material.bottomappbar.BottomAppBar
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Material BottomAppBar adapter with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(BottomAppBar::class)
@IncludeInDesigner(group = GOOGLE)
open class BottomAppBarAdapter<T : BottomAppBar> : ViewGroupAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(
            BottomAppBar::class.java,
            string.widget_bottom_app_bar,
            drawable.ic_widget_appbar,
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 BottomAppBar specific attributes
    create("elevation") {
      val elevation = parseDimensionF(context, value)
      if (elevation >= 0) view.elevation = elevation
    }

    create("backgroundColor") {
      val color = parseColor(context, value)
      view.setBackgroundColor(color)
    }

    create("menu") {
      // Menu items are defined in separate menu resource files
      log.debug("BottomAppBar menu resource: $value")
    }

    create("navigationIcon") {
      val drawable = parseDrawable(context, value)
      drawable?.let { view.navigationIcon = it }
    }

    create("navigationContentDescription") {
      view.navigationContentDescription = value
    }

    create("fabAlignmentMode") {
      when (value.lowercase()) {
        "center" -> view.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_CENTER
        "end" -> view.fabAlignmentMode = BottomAppBar.FAB_ALIGNMENT_MODE_END
      }
    }

    create("fabCradleMargin") {
      val margin = parseDimensionF(context, value)
      if (margin >= 0) view.fabCradleMargin = margin
    }

    create("fabCradleRoundedCornerRadius") {
      val radius = parseDimensionF(context, value)
      if (radius >= 0) view.fabCradleRoundedCornerRadius = radius
    }

    create("hideOnScroll") {
      val hideOnScroll = parseBoolean(value)
      view.hideOnScroll = hideOnScroll
    }
  }

  companion object {
    private val log = org.slf4j.LoggerFactory.getLogger(BottomAppBarAdapter::class.java)
  }
}
