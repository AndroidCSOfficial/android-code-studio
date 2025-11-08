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

package com.tom.rv2ide.inflater.internal.adapters

import com.google.android.material.textfield.TextInputEditText
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.WIDGETS
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [EditText].
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
@ViewAdapter(TextInputEditText::class)
@IncludeInDesigner(group = WIDGETS)
open class TextInputEditTextAdapter<T : TextInputEditText> : TextViewAdapter<T>() {
  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(TextInputEditText::class.java, string.widget_edittext, drawable.ic_widget_edittext)
    )
  }
}
