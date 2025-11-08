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

package com.tom.rv2ide.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tom.rv2ide.databinding.ItemColorPresetBinding

/**
 * Adapter for color preset selection in the color picker dialog.
 *
 * @author Tom
 */
class ColorPresetAdapter(
    private val colors: List<Int>,
    private val onColorSelected: (Int) -> Unit,
) : RecyclerView.Adapter<ColorPresetAdapter.ColorViewHolder>() {

  class ColorViewHolder(val binding: ItemColorPresetBinding) :
      RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
    val binding = ItemColorPresetBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ColorViewHolder(binding)
  }

  override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
    val color = colors[position]
    holder.binding.colorButton.setBackgroundColor(color)
    holder.binding.colorButton.setOnClickListener { onColorSelected(color) }
  }

  override fun getItemCount(): Int = colors.size

  companion object {
    fun getDefaultColors(): List<Int> {
      return listOf(
          Color.parseColor("#FF6200EE"), // Purple
          Color.parseColor("#FF03DAC6"), // Teal
          Color.parseColor("#FF018786"), // Dark Teal
          Color.parseColor("#FF000000"), // Black
          Color.parseColor("#FFFFFFFF"), // White
          Color.parseColor("#FFFF0000"), // Red
          Color.parseColor("#FF00FF00"), // Green
          Color.parseColor("#FF0000FF"), // Blue
          Color.parseColor("#FFFFFF00"), // Yellow
          Color.parseColor("#FFFF00FF"), // Magenta
          Color.parseColor("#FF00FFFF"), // Cyan
          Color.parseColor("#FF808080"), // Gray
          Color.parseColor("#FFFFA500"), // Orange
          Color.parseColor("#FF800080"), // Purple
          Color.parseColor("#FF008000"), // Dark Green
          Color.parseColor("#FF000080"), // Navy
      )
    }
  }
}
