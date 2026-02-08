package com.tom.rv2ide.inflater.internal.adapters

import android.content.Context
import android.view.View
import com.google.android.material.search.SearchView
import com.tom.rv2ide.inflater.IAttributeHandler
import com.tom.rv2ide.inflater.IViewAdapter
import com.tom.rv2ide.inflater.annotations.IncludeInDesigner
import com.tom.rv2ide.inflater.annotations.ViewAdapter
import com.tom.rv2ide.inflater.internal.LayoutInflaterImpl

@ViewAdapter(SearchView::class)
@IncludeInDesigner(group = "WIDGETS")
open class SearchViewAdapter<T : SearchView>(
  context: Context,
  attrs: Map<String, String>?,
  layoutInflater: LayoutInflaterImpl,
) : FrameLayoutAdapter<T>(context, attrs, layoutInflater) {

  override fun createUiWidgets(): T {
    val view = super.createUiWidgets()
    view.setHint(android.R.string.search_go)
    return view
  }

  override fun createAttrHandlers(
      view: T,
      parent: IViewAdapter<*>?,
  ): Map<String, IAttributeHandler> {
    val handlers = super.createAttrHandlers(view, parent).toMutableMap()

    handlers["hint"] = { value ->
      val hintRes = context.resources.getIdentifier(value, "string", context.packageName)
      if (hintRes != 0) {
        view.setHint(hintRes)
      } else {
        view.setHint(value)
      }
      true
    }

    handlers["inputType"] = { value ->
      val inputType = when (value.lowercase()) {
        "text" -> android.text.InputType.TYPE_CLASS_TEXT
        "number" -> android.text.InputType.TYPE_CLASS_NUMBER
        "phone" -> android.text.InputType.TYPE_CLASS_PHONE
        "email" -> android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        "uri" -> android.text.InputType.TYPE_TEXT_VARIATION_URI
        else -> android.text.InputType.TYPE_CLASS_TEXT
      }
      view.editText?.inputType = inputType
      true
    }

    handlers["backgroundColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.setBackgroundColor(color)
      true
    }

    handlers["textColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.editText?.setTextColor(color)
      true
    }

    handlers["cursorColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) {
        try {
          view.editText?.setTextColor(color)
        } catch (e: Exception) {
          // Fallback si no se puede establecer
        }
      }
      true
    }

    handlers["elevation"] = { value ->
      val elev = parseDimensionF(context, value)
      if (elev >= 0) view.elevation = elev
      true
    }

    return handlers
  }
}
