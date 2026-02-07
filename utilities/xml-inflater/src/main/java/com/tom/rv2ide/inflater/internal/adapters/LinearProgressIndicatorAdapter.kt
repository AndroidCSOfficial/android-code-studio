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
import com.google.android.material.progressindicator.LinearProgressIndicator
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
 * MaterialAdapter for LinearProgressIndicator with Material Design 3 support.
 *
 * @author Enhancement for M3 compatibility
 */
@ViewAdapter(LinearProgressIndicator::class)
@IncludeInDesigner(group = GOOGLE)
open class LinearProgressIndicatorAdapter<T : LinearProgressIndicator> : ViewAdapter<T>() {

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        LinearProgressIndicatorWidget(
            title = string.widget_progressbar, icon = drawable.ic_widget_progress_bar
        )
    )
  }

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // Material Design 3 progress indicator attributes
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

    create("trackCornerRadius") {
      val radius = parseDimensionF(context, value)
      if (radius >= 0) view.trackCornerRadius = radius.toInt()
    }

    create("indicatorHeight") {
      val height = parseDimensionF(context, value)
      if (height > 0) view.indicatorHeight = height.toInt()
    }

    create("showAnimationBehavior") {
      when (value.lowercase()) {
        "linear" -> view.showAnimationBehavior =
            LinearProgressIndicator.SHOW_OUTWARD
        "outward" -> view.showAnimationBehavior =
            LinearProgressIndicator.SHOW_OUTWARD
        "inward" -> view.showAnimationBehavior =
            LinearProgressIndicator.SHOW_INWARD
        "none" -> view.showAnimationBehavior = LinearProgressIndicator.SHOW_NONE
      }
    }

    create("hideAnimationBehavior") {
      when (value.lowercase()) {
        "outward" -> view.hideAnimationBehavior =
            LinearProgressIndicator.HIDE_OUTWARD
        "inward" -> view.hideAnimationBehavior =
            LinearProgressIndicator.HIDE_INWARD
        "none" -> view.hideAnimationBehavior = LinearProgressIndicator.HIDE_NONE
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

    internal data class LinearProgressIndicatorWidget(
        @StringRes override val title: Int = titleRes,
        @DrawableRes override val preview: Int = iconRes,
        override val name: String = LinearProgressIndicator::class.java.simpleName,
    ) : UiWidget
  }
}
