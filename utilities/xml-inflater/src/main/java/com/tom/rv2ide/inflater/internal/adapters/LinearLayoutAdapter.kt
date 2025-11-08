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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.LAYOUTS
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.INamespace
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.IViewGroup
import com.tom.rv2ide.inflater.LayoutStrategy
import com.tom.rv2ide.inflater.internal.LayoutFile
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.inflater.utils.newAttribute
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [LinearLayout].
 *
 * @author Akash Yadav
 */
@ViewAdapter(LinearLayout::class)
@IncludeInDesigner(group = LAYOUTS)
open class LinearLayoutAdapter<T : LinearLayout> : ViewGroupAdapter<T>() {

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    create("baselineAligned") { view.isBaselineAligned = parseBoolean(value) }
    create("baselineAlignedChildIndex") {
      view.baselineAlignedChildIndex = parseInteger(value, view.childCount)
    }
    create("gravity") { view.gravity = parseGravity(value) }
    create("measureWithLargestChild") {
      view.isMeasureWithLargestChildEnabled = parseBoolean(value)
    }
    create("orientation") { view.orientation = parseOrientation(value) }
    create("weightSum") { view.weightSum = parseFloat(value) }

    // Add background attribute handler
    create("background") {
      val background = parseBackground(value, view.context)
      if (background != null) {
        view.background = background
      }
    }
  }

  override fun createUiWidgets(): List<UiWidget> {
    val horizontal =
        LinearLayoutWidget(
            title = string.widget_linear_layout_horz,
            icon = drawable.ic_widget_linear_layout_horz,
            isVertical = false,
        )
    val vertical =
        LinearLayoutWidget(
            title = string.widget_linear_layout_vert,
            icon = drawable.ic_widget_linear_layout_vert,
            isVertical = true,
        )
    return listOf(horizontal, vertical)
  }

  override fun getLayoutStrategy(group: IViewGroup): LayoutStrategy {
    val orientation = group.findAttribute("orientation", INamespace.ANDROID.uri)
    return if (orientation?.value == "vertical") LayoutStrategy.VERTICAL
    else LayoutStrategy.HORIZONTAL
  }

  protected open fun parseOrientation(value: String): Int {
    return when (value) {
      "vertical" -> LinearLayout.VERTICAL
      "horizontal" -> LinearLayout.HORIZONTAL
      else -> LinearLayout.HORIZONTAL
    }
  }

  /**
   * Parses background attribute value and returns appropriate Drawable. Supports:
   * - Color values: #RRGGBB, #AARRGGBB, color names
   * - Drawable resources: @drawable/name
   * - Color resources: @color/name
   */
  protected open fun parseBackground(value: String, context: Context): Drawable? {
    return try {
      when {
        // Handle color values (hex colors)
        value.startsWith("#") -> {
          val color = Color.parseColor(value)
          ColorDrawable(color)
        }

        // Handle drawable resources (@drawable/name)
        value.startsWith("@drawable/") -> {
          val resourceName = value.substring(10) // Remove "@drawable/"
          val resourceId =
              context.resources.getIdentifier(resourceName, "drawable", context.packageName)
          if (resourceId != 0) {
            ContextCompat.getDrawable(context, resourceId)
          } else {
            null
          }
        }

        // Handle color resources (@color/name)
        value.startsWith("@color/") -> {
          val resourceName = value.substring(7) // Remove "@color/"
          val resourceId =
              context.resources.getIdentifier(resourceName, "color", context.packageName)
          if (resourceId != 0) {
            val color = ContextCompat.getColor(context, resourceId)
            ColorDrawable(color)
          } else {
            null
          }
        }

        // Handle Android system resources (@android:drawable/name, @android:color/name)
        value.startsWith("@android:drawable/") -> {
          val resourceName = value.substring(18) // Remove "@android:drawable/"
          val resourceId = android.R.drawable::class.java.getField(resourceName).getInt(null)
          ContextCompat.getDrawable(context, resourceId)
        }

        value.startsWith("@android:color/") -> {
          val resourceName = value.substring(15) // Remove "@android:color/"
          val resourceId = android.R.color::class.java.getField(resourceName).getInt(null)
          val color = ContextCompat.getColor(context, resourceId)
          ColorDrawable(color)
        }

        // Handle named colors (red, blue, green, etc.)
        else -> {
          try {
            val color = Color.parseColor(value)
            ColorDrawable(color)
          } catch (e: IllegalArgumentException) {
            // If it's not a valid color name, return null
            null
          }
        }
      }
    } catch (e: Exception) {
      // Log the error if needed, but don't crash
      // Log.w("LinearLayoutAdapter", "Failed to parse background: $value", e)
      null
    }
  }

  private class LinearLayoutWidget(
      @StringRes title: Int,
      @DrawableRes icon: Int,
      private val isVertical: Boolean,
  ) : UiWidget(LinearLayout::class.java, title, icon) {

    companion object {

      private const val HORIZONTAL = "horizontal"
      private const val VERTICAL = "vertical"
    }

    override fun createView(context: Context, parent: ViewGroup, layoutFile: LayoutFile): IView {
      return super.createView(context, parent, layoutFile).apply {
        addAttribute(
            newAttribute(
                this,
                name = "orientation",
                value = if (isVertical) VERTICAL else HORIZONTAL,
            )
        )
      }
    }
  }
}
