package com.tom.rv2ide.uidesigner.utils.views

import android.content.Context
import com.google.android.material.navigationrail.NavigationRailView
import com.tom.rv2ide.projects.IWorkspace
import java.io.File

fun NavigationRailView.applyM3Preview(
  attributeName: String,
  attributeValue: String,
  context: Context,
  workspace: IWorkspace?,
  layoutFile: File?,
): Boolean {
  val normalizedAttrName = attributeName.lowercase()

  return when (normalizedAttrName) {
    "backgroundcolor" -> {
      val color = M3Utils.parseColor(attributeValue, context)
      if (color != null) setBackgroundColor(color)
      true
    }

    "itemtextcolor" -> {
      val csl = M3Utils.parseColorStateList(attributeValue, context)
      if (csl != null) itemTextColor = csl
      true
    }

    "itemicontinttint" -> {
      val csl = M3Utils.parseColorStateList(attributeValue, context)
      if (csl != null) itemIconTintList = csl
      true
    }

    "elevation" -> {
      try {
        elevation = M3Utils.parseDimensionF(attributeValue, context)
      } catch (e: Exception) {
        elevation = 4f
      }
      true
    }

    "labelvisibilitymode" -> {
      when (attributeValue.lowercase()) {
        "labeled" -> labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_LABELED
        "selected" -> labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_SELECTED
        "unlabeled" -> labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_UNLABELED
        else -> labelVisibilityMode = NavigationRailView.LABEL_VISIBILITY_SELECTED
      }
      true
    }

    "iteminsetstart" -> {
      val inset = M3Utils.parseDimension(attributeValue, context)
      if (inset >= 0) {
        itemInsetStart = inset
      }
      true
    }

    "iteminsetend" -> {
      val inset = M3Utils.parseDimension(attributeValue, context)
      if (inset >= 0) {
        itemInsetEnd = inset
      }
      true
    }

    else -> false
  }
}
