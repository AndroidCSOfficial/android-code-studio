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

package com.tom.rv2ide.utils

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.tom.rv2ide.R
import com.tom.rv2ide.actions.SidebarActionItem

/**
 * Data class representing a navigation item in the sidebar.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
data class SidebarNavigationItem(
    val id: String,
    val icon: Drawable?,
    val title: String,
    val subtitle: String? = null,
    val isSelected: Boolean = false,
    val action: SidebarActionItem,
)

class SidebarNavigationAdapter(
    private val onItemClick: (SidebarNavigationItem) -> Unit,
    private val onItemLongClick: ((SidebarNavigationItem) -> Boolean)? = null,
) : ListAdapter<SidebarNavigationItem, SidebarNavigationAdapter.ViewHolder>(DiffCallback()) {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val view =
        LayoutInflater.from(parent.context).inflate(R.layout.item_sidebar_navigation, parent, false)
    return ViewHolder(view)
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.bind(getItem(position))
  }

  inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val button: MaterialButton = itemView.findViewById(R.id.navigation_button)

    fun bind(item: SidebarNavigationItem) {
      button.apply {
        val paddingLeft = paddingLeft
        val paddingTop = paddingTop
        val paddingRight = paddingRight
        val paddingBottom = paddingBottom

        icon = null
        iconTint = null

        contentDescription = item.title
        isSelected = item.isSelected
        isChecked = item.isSelected

        if (item.isSelected) {
          val baseColor =
              resolveColorAttr(context, com.google.android.material.R.attr.colorPrimaryContainer)

          val alpha = (0.5f * 255).toInt() // 50% opacity
          val red = (android.graphics.Color.red(baseColor) * 0.7f).toInt()
          val green = (android.graphics.Color.green(baseColor) * 0.7f).toInt()
          val blue = (android.graphics.Color.blue(baseColor) * 0.7f).toInt()
          val finalColor = android.graphics.Color.argb(alpha, red, green, blue)

          val shape =
              android.graphics.drawable.GradientDrawable().apply {
                this.shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 40f
                setColor(finalColor)
              }
          val insetValue = 30
          background = android.graphics.drawable.InsetDrawable(shape, insetValue)
        } else {
          background = null
        }

        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

        item.icon?.let { drawable ->
          icon = drawable.constantState?.newDrawable()?.mutate() ?: drawable.mutate()
          if (item.isSelected) {
            val iconTintColor =
                resolveColorAttr(
                    context,
                    com.google.android.material.R.attr.colorOnPrimaryContainer,
                )
            iconTint = ColorStateList.valueOf(iconTintColor)
          } else {
            iconTint = ColorStateList.valueOf(android.graphics.Color.parseColor("#666666"))
          }
        }

        setOnClickListener { onItemClick(item) }

        onItemLongClick?.let { longClickHandler ->
          setOnLongClickListener { longClickHandler(item) }
        }
      }
    }
  }

  private fun resolveColorAttr(context: Context, @AttrRes attrRes: Int): Int {
    val typedValue = TypedValue()
    val theme = context.theme
    if (theme.resolveAttribute(attrRes, typedValue, true)) {
      return typedValue.data
    }
    return ContextCompat.getColor(context, android.R.color.black)
  }

  private fun resolveColorStateListAttr(context: Context, @AttrRes attrRes: Int): ColorStateList? {
    val typedValue = TypedValue()
    val theme = context.theme
    if (theme.resolveAttribute(attrRes, typedValue, true)) {
      return ContextCompat.getColorStateList(context, typedValue.resourceId)
    }
    return ContextCompat.getColorStateList(context, android.R.color.transparent)
  }

  private class DiffCallback : DiffUtil.ItemCallback<SidebarNavigationItem>() {
    override fun areItemsTheSame(
        oldItem: SidebarNavigationItem,
        newItem: SidebarNavigationItem,
    ): Boolean = oldItem.id == newItem.id

    override fun areContentsTheSame(
        oldItem: SidebarNavigationItem,
        newItem: SidebarNavigationItem,
    ): Boolean = oldItem == newItem
  }
}
