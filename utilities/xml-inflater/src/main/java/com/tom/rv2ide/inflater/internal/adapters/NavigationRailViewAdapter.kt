package com.tom.rv2ide.inflater.internal.adapters

import android.content.Context
import android.view.View
import com.google.android.material.navigationrail.NavigationRailView
import com.tom.rv2ide.inflater.IAttributeHandler
import com.tom.rv2ide.inflater.IViewAdapter
import com.tom.rv2ide.inflater.annotations.IncludeInDesigner
import com.tom.rv2ide.inflater.annotations.ViewAdapter
import com.tom.rv2ide.inflater.internal.LayoutInflaterImpl

@ViewAdapter(NavigationRailView::class)
@IncludeInDesigner(group = "LAYOUTS")
open class NavigationRailViewAdapter<T : NavigationRailView>(
  context: Context,
  attrs: Map<String, String>?,
  layoutInflater: LayoutInflaterImpl,
) : FrameLayoutAdapter<T>(context, attrs, layoutInflater) {

  override fun createAttrHandlers(
      view: T,
      parent: IViewAdapter<*>?,
  ): Map<String, IAttributeHandler> {
    val handlers = super.createAttrHandlers(view, parent).toMutableMap()

    handlers["backgroundColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.setBackgroundColor(color)
      true
    }

    handlers["itemTextColor"] = { value ->
      val csl = parseColorStateList(context, value)
      if (csl != null) view.itemTextColor = csl
      true
    }

    handlers["itemIconTint"] = { value ->
      val csl = parseColorStateList(context, value)
      if (csl != null) view.itemIconTintList = csl
      true
    }

    handlers["itemTextAppearance"] = { value ->
      val styleRes = context.resources.getIdentifier(value, "style", context.packageName)
      if (styleRes != 0) {
        try {
          // Apply text appearance through style
        } catch (e: Exception) {
          // Fallback approach
        }
      }
      true
    }

    handlers["elevation"] = { value ->
      val elev = parseDimensionF(context, value)
      if (elev >= 0) view.elevation = elev
      true
    }

    handlers["labelVisibilityMode"] = { value ->
      when (value.lowercase()) {
        "labeled" -> view.labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_LABELED
        "selected" -> view.labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_SELECTED
        "unlabeled" -> view.labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_UNLABELED
        else -> view.labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_SELECTED
      }
      true
    }

    handlers["headerLayout"] = { value ->
      val layoutRes = context.resources.getIdentifier(value, "layout", context.packageName)
      if (layoutRes != 0) {
        view.headerView = layoutInflater.inflate(layoutRes, view, false)
      }
      true
    }

    handlers["menuResource"] = { value ->
      val menuRes = context.resources.getIdentifier(value, "menu", context.packageName)
      if (menuRes != 0) {
        try {
          // InflateMenu here if available
        } catch (e: Exception) {
          // Menu inflation fallback
        }
      }
      true
    }

    handlers["itemPadding"] = { value ->
      val padding = parseDimension(context, value)
      if (padding >= 0) {
        view.itemPaddingTop = padding
        view.itemPaddingBottom = padding
      }
      true
    }

    return handlers
  }
}
