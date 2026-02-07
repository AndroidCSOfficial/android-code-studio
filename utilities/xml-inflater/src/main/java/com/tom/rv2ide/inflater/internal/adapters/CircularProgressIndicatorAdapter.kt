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

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.material.progressindicator.CircularProgressIndicator
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

/**
 * MaterialAdapter for CircularProgressIndicator with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(CircularProgressIndicator::class)
@IncludeInDesigner(group = GOOGLE)
open class CircularProgressIndicatorAdapter<T : CircularProgressIndicator> : ViewAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        CircularProgressIndicatorWidget(
            title = string.widget_progressbar, icon = drawable.ic_widget_progress_bar
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 circular progress indicator attributes
    create("progress") {
      val progress = value.toIntOrNull() ?: 0
      if (progress in 0..100) view.progress = progress
    }

    create("max") {
      val max = value.toIntOrNull() ?: 100
      if (max > 0) view.max = max
    }

    create("indeterminate") {
      val indeterminate = parseBoolean(value)
      view.isIndeterminate = indeterminate
    }

    create("indicatorColor") {
      val color = parseColor(context, value)
      view.setIndicatorColor(color)
    }

    create("trackColor") {
      val color = parseColor(context, value)
      view.trackColor = color
    }

    create("indicatorSize") {
      val size = parseDimensionF(context, value)
      if (size > 0) view.indicatorSize = size.toInt()
    }

    create("indicatorInset") {
      val inset = parseDimensionF(context, value)
      if (inset >= 0) view.indicatorInset = inset.toInt()
    }

    create("trackThickness") {
      val thickness = parseDimensionF(context, value)
      if (thickness > 0) view.trackThickness = thickness.toInt()
    }

    create("showAnimationBehavior") {
      when (value.lowercase()) {
        "outward" -> view.showAnimationBehavior =
            CircularProgressIndicator.SHOW_OUTWARD
        "inward" -> view.showAnimationBehavior =
            CircularProgressIndicator.SHOW_INWARD
        "none" -> view.showAnimationBehavior = CircularProgressIndicator.SHOW_NONE
      }
    }

    create("hideAnimationBehavior") {
      when (value.lowercase()) {
        "outward" -> view.hideAnimationBehavior =
            CircularProgressIndicator.HIDE_OUTWARD
        "inward" -> view.hideAnimationBehavior =
            CircularProgressIndicator.HIDE_INWARD
        "none" -> view.hideAnimationBehavior = CircularProgressIndicator.HIDE_NONE
      }
    }
  }

  override fun mapAttributeHandler(
      view: IView,
      attribute: INamespace?,
      name: String,
      value: String,
  ): Boolean {
    return super.mapAttributeHandler(view, attribute, name, value) ||
        addAttribute(view, attribute, name, value)
  }

  private fun addAttribute(
      view: IView,
      namespace: INamespace?,
      name: String,
      value: String,
  ): Boolean {
    view.addAttribute(newAttribute(namespace, name, value, view.layoutFile))
    return true
  }

  companion object {
    @StringRes val titleRes: Int = string.widget_progressbar

    @DrawableRes val iconRes: Int = drawable.ic_widget_progress_bar

    internal data class CircularProgressIndicatorWidget(
        @StringRes override val title: Int = titleRes,
        @DrawableRes override val preview: Int = iconRes,
        override val name: String = CircularProgressIndicator::class.java.simpleName,
    ) : UiWidget
  }
}
