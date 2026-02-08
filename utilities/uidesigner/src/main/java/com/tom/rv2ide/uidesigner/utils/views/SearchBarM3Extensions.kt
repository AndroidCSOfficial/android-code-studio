package com.tom.rv2ide.uidesigner.utils.views

import android.content.Context
import android.view.ResourceProvider
import com.google.android.material.search.SearchBar
import com.tom.rv2ide.projects.IWorkspace
import java.io.File
import org.slf4j.LoggerFactory

fun SearchBar.applyM3Preview(
  attributeName: String,
  attributeValue: String,
  context: Context,
  workspace: IWorkspace?,
  layoutFile: File?,
): Boolean {
  val normalizedAttrName = attributeName.lowercase()

  return when (normalizedAttrName) {
    "hint" -> {
      hint = attributeValue
      true
    }

    "placeholdertext" -> {
      setPlaceholderText(attributeValue)
      true
    }

    "searchicon" -> {
      val iconRes = context.resources.getIdentifier(
        attributeValue,
        "drawable",
        "android"
      )
      if (iconRes != 0) setNavigationIcon(iconRes)
      true
    }

    "searchicontint" -> {
      val color = M3Utils.parseColor(attributeValue, context)
      if (color != null) setNavigationIconTint(color)
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

    "backgroundcolor" -> {
      val color = M3Utils.parseColor(attributeValue, context)
      if (color != null) setBackgroundColor(color)
      true
    }

    else -> false
  }
}
