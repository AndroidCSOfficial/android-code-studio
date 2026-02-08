package com.tom.rv2ide.inflater.internal.adapters

import android.content.Context
import android.view.View
import com.google.android.material.divider.MaterialDivider
import com.tom.rv2ide.inflater.IAttributeHandler
import com.tom.rv2ide.inflater.IViewAdapter
import com.tom.rv2ide.inflater.annotations.IncludeInDesigner
import com.tom.rv2ide.inflater.annotations.ViewAdapter
import com.tom.rv2ide.inflater.internal.LayoutInflaterImpl

@ViewAdapter(MaterialDivider::class)
@IncludeInDesigner(group = "WIDGETS")
open class MaterialDividerAdapter<T : MaterialDivider>(
  context: Context,
  attrs: Map<String, String>?,
  layoutInflater: LayoutInflaterImpl,
) : ViewAdapter<T>(context, attrs, layoutInflater) {

  override fun createUiWidgets(): T {
    val view = super.createUiWidgets()
    return view
  }

  override fun createAttrHandlers(
      view: T,
      parent: IViewAdapter<*>?,
  ): Map<String, IAttributeHandler> {
    val handlers = super.createAttrHandlers(view, parent).toMutableMap()

    handlers["android:layout_height"] = { value ->
      val height = parseDimension(context, value)
      if (height >= 0) {
        val lp = view.layoutParams ?: android.view.ViewGroup.LayoutParams(
          android.view.ViewGroup.LayoutParams.MATCH_PARENT,
          height
        )
        lp.height = height
        view.layoutParams = lp
      }
      true
    }

    handlers["dividerColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.dividerColor = color
      true
    }

    handlers["dividerInsetStart"] = { value ->
      val inset = parseDimension(context, value)
      if (inset >= 0) view.dividerInsetStart = inset
      true
    }

    handlers["dividerInsetEnd"] = { value ->
      val inset = parseDimension(context, value)
      if (inset >= 0) view.dividerInsetEnd = inset
      true
    }

    handlers["thickness"] = { value ->
      val thick = parseDimension(context, value)
      if (thick > 0) {
        val lp = view.layoutParams ?: android.view.ViewGroup.LayoutParams(
          android.view.ViewGroup.LayoutParams.MATCH_PARENT,
          thick
        )
        lp.height = thick
        view.layoutParams = lp
      }
      true
    }

    handlers["backgroundColor"] = { value ->
      val color = parseColor(context, value)
      if (color != null) view.dividerColor = color
      true
    }

    return handlers
  }
}
