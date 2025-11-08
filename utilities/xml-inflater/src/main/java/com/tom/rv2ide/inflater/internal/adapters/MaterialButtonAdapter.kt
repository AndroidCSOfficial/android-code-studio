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
package com.tom.rv2ide.inflater.internal.adapters

import com.google.android.material.button.MaterialButton
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [Button].
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
@ViewAdapter(MaterialButton::class)
@IncludeInDesigner(group = GOOGLE)
open class MaterialButtonAdapter<T : MaterialButton> : TextViewAdapter<T>() {
  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(MaterialButton::class.java, string.widget_mbutton, drawable.ic_widget_button)
    )
  }
}
