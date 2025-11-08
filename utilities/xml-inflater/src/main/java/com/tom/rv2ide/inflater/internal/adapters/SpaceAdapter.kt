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

import android.widget.Space
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.WIDGETS
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [Space].
 *
 * @author Deep Kr. Ghosh
 */
@com.tom.rv2ide.annotations.inflater.ViewAdapter(Space::class)
@IncludeInDesigner(group = WIDGETS)
open class SpaceAdapter<@Suppress("FINAL_UPPER_BOUND") T : Space> : ViewAdapter<T>() {
  override fun createUiWidgets(): List<UiWidget> {
    return listOf(UiWidget(Space::class.java, string.widget_space, drawable.ic_widget_space))
  }
}
