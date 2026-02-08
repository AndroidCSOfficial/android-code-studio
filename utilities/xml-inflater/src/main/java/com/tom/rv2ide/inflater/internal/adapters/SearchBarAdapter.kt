package com.tom.rv2ide.inflater.internal.adapters

import android.content.Context
import android.view.View
import com.google.android.material.search.SearchBar
import com.tom.rv2ide.inflater.IAttributeHandler
import com.tom.rv2ide.inflater.IViewAdapter
import com.tom.rv2ide.inflater.annotations.IncludeInDesigner
import com.tom.rv2ide.inflater.annotations.ViewAdapter
import com.tom.rv2ide.inflater.internal.LayoutInflaterImpl

@ViewAdapter(SearchBar::class)
@IncludeInDesigner(group = "WIDGETS")
open class SearchBarAdapter<T : SearchBar>(
  context: Context,
  attrs: Map<String, String>?,
  layoutInflater: LayoutInflaterImpl,
) : FrameLayoutAdapter<T>(context, attrs, layoutInflater) {

  override fun createUiWidgets(): T {
    val view = super.createUiWidgets()
    view.setPlaceholderText(android.R.string.search_go)
    return view
  }

  override fun createAttrHandlers(
      view: T,
      parent: IViewAdapter<*>?,
  ): Map<String, IAttributeHandler> {
    val handlers = super.createAttrHandlers(view, parent).toMutableMap()

    handlers["hint"] = { value ->
      view.hint = value
      true
    }

    handlers["placeholderText"] = { value ->
      val hintRes = context.resources.getIdentifier(
        "search_bar_${value.lowercase()}",
        "string",
        "android"
      )
      if (hintRes != 0) {
        view.setPlaceholderText(hintRes)
      } else {
        view.setPlaceholderText(value)
      }
      true
    }

    handlers["searchIcon"] = { value ->
      val res = context.resources.getIdentifier(value, "drawable", context.packageName)
      if (res != 0) view.setNavigationIcon(res)
      true
    }

    handlers["searchIconTint"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.setNavigationIconTint(color)
      true
    }

    handlers["elevation"] = { value ->
      val elev = parseDimensionF(context, value)
      if (elev >= 0) view.elevation = elev
      true
    }

    handlers["backgroundColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.setBackgroundColor(color)
      true
    }

    return handlers
  }
}
