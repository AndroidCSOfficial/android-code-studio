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

package com.tom.rv2ide.experimental.assetstudio.m3icons

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.R

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object ColorPickerDialog {
    fun show(
        context: Context,
        initialColor: Int,
        initialDynamicColor: String?,
        onColorSelected: (Int, String?) -> Unit
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.m3icons_dialog_color_picker, null)

        val seekBarRed = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderRed)
        val seekBarGreen = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderGreen)
        val seekBarBlue = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderBlue)
        val seekBarAlpha = dialogView.findViewById<com.google.android.material.slider.Slider>(R.id.sliderAlpha)
        val colorPreview = dialogView.findViewById<android.view.View>(R.id.colorPreviewBox)
        val hexInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.hexInput)
        val dynamicColorSpinner = dialogView.findViewById<android.widget.Spinner>(R.id.dynamicColorSpinner)

        val dynamicColors = DynamicColorHelper.getDynamicColors()
        val adapter = android.widget.ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            dynamicColors.map { it.first }
        )
        dynamicColorSpinner.adapter = adapter

        var isUpdatingFromSpinner = false

        seekBarRed.value = Color.red(initialColor).toFloat()
        seekBarGreen.value = Color.green(initialColor).toFloat()
        seekBarBlue.value = Color.blue(initialColor).toFloat()
        seekBarAlpha.value = Color.alpha(initialColor).toFloat()

        var currentColor = initialColor
        var selectedDynamicColor: String? = initialDynamicColor ?: "?attr/colorControlNormal"
        colorPreview.setBackgroundColor(currentColor)

        if (initialDynamicColor != null) {
            val index = dynamicColors.indexOfFirst { it.first == initialDynamicColor }
            if (index >= 0) {
                dynamicColorSpinner.setSelection(index)
            }
            hexInput.setText(initialDynamicColor)
        } else {
            dynamicColorSpinner.setSelection(0)
            hexInput.setText("?attr/colorControlNormal")
        }

        val updateColor = {
            currentColor = Color.argb(
                seekBarAlpha.value.toInt(),
                seekBarRed.value.toInt(),
                seekBarGreen.value.toInt(),
                seekBarBlue.value.toInt()
            )
            colorPreview.setBackgroundColor(currentColor)
            if (selectedDynamicColor == null) {
                hexInput.setText(String.format("%08X", currentColor))
            }
        }

        seekBarRed.addOnChangeListener { _, _, _ ->
            if (!isUpdatingFromSpinner) {
                dynamicColorSpinner.setSelection(1)
                selectedDynamicColor = null
                updateColor()
            }
        }
        seekBarGreen.addOnChangeListener { _, _, _ ->
            if (!isUpdatingFromSpinner) {
                dynamicColorSpinner.setSelection(1)
                selectedDynamicColor = null
                updateColor()
            }
        }
        seekBarBlue.addOnChangeListener { _, _, _ ->
            if (!isUpdatingFromSpinner) {
                dynamicColorSpinner.setSelection(1)
                selectedDynamicColor = null
                updateColor()
            }
        }
        seekBarAlpha.addOnChangeListener { _, _, _ ->
            if (!isUpdatingFromSpinner) {
                dynamicColorSpinner.setSelection(1)
                selectedDynamicColor = null
                updateColor()
            }
        }

        dynamicColorSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val selected = dynamicColors[position]
                if (selected.second != null) {
                    isUpdatingFromSpinner = true
                    
                    selectedDynamicColor = selected.first
                    val typedValue = TypedValue()
                    context.theme.resolveAttribute(selected.second!!, typedValue, true)

                    currentColor = if (typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                        typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                        typedValue.data
                    } else {
                        Color.BLACK
                    }

                    seekBarAlpha.value = Color.alpha(currentColor).toFloat()
                    seekBarRed.value = Color.red(currentColor).toFloat()
                    seekBarGreen.value = Color.green(currentColor).toFloat()
                    seekBarBlue.value = Color.blue(currentColor).toFloat()

                    colorPreview.setBackgroundColor(currentColor)
                    hexInput.setText(selected.first)
                    
                    isUpdatingFromSpinner = false
                } else {
                    selectedDynamicColor = null
                    hexInput.setText(String.format("%08X", currentColor))
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        hexInput.setOnEditorActionListener { _, _, _ ->
            try {
                val hexColor = hexInput.text.toString().removePrefix("#")
                val color = when (hexColor.length) {
                    6 -> Color.parseColor("#FF$hexColor")
                    8 -> Color.parseColor("#$hexColor")
                    else -> currentColor
                }
                dynamicColorSpinner.setSelection(1)
                selectedDynamicColor = null
                seekBarAlpha.value = Color.alpha(color).toFloat()
                seekBarRed.value = Color.red(color).toFloat()
                seekBarGreen.value = Color.green(color).toFloat()
                seekBarBlue.value = Color.blue(color).toFloat()
                updateColor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            false
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.asset_studio_select_color))
            .setView(dialogView)
            .setPositiveButton(context.getString(R.string.action_ok)) { dialog, _ ->
                onColorSelected(currentColor, selectedDynamicColor)
                dialog.dismiss()
            }
            .setNegativeButton(context.getString(R.string.action_cancel), null)
            .show()
    }
}