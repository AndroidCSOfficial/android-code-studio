package com.tom.rv2ide.uidesigner.utils.views

import android.content.Context
import com.google.android.material.divider.MaterialDivider
import com.tom.rv2ide.projects.IWorkspace
import java.io.File

fun MaterialDivider.applyM3Preview(
  attributeName: String,
  attributeValue: String,
  context: Context,
  workspace: IWorkspace?,
  layoutFile: File?,
): Boolean {
  val normalizedAttrName = attributeName.lowercase()

  return when (normalizedAttrName) {
    "dividercolor" -> {
      val color = M3Utils.parseColor(attributeValue, context)
      if (color != null) dividerColor = color
      true
    }

    "dividerinsetstart" -> {
      val inset = M3Utils.parseDimension(attributeValue, context)
      if (inset >= 0) dividerInsetStart = inset
      true
    }

    "dividerinsetend" -> {
      val inset = M3Utils.parseDimension(attributeValue, context)
      if (inset >= 0) dividerInsetEnd = inset
      true
    }

    "thickness" -> {
      val thickness = M3Utils.parseDimension(attributeValue, context)
      if (thickness > 0) {
        val lp = layoutParams
        lp?.height = thickness
        layoutParams = lp
      }
      true
    }

    "android:layout_height" -> {
      val height = M3Utils.parseDimension(attributeValue, context)
      if (height > 0) {
        val lp = layoutParams ?: android.view.ViewGroup.LayoutParams(
          android.view.ViewGroup.LayoutParams.MATCH_PARENT,
          height
        )
        lp.height = height
        layoutParams = lp
      }
      true
    }

    else -> false
  }
}
