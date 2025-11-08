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

package com.tom.rv2ide.actions.sidebar

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tom.rv2ide.R
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.fragments.sidebar.SubModuleFragment
import kotlin.reflect.KClass

/**
 * Sidebar action for creating new sub-modules in the project. Provides a modern Material 3
 * interface for module creation with Kotlin/Java selection.
 *
 * @author Tom
 */
class SubModuleSidebarAction(context: Context, override val order: Int) : AbstractSidebarAction() {

  companion object {
    const val ID = "ide.editor.sidebar.subModule"
  }

  override val id: String = ID
  override val fragmentClass: KClass<out Fragment> = SubModuleFragment::class

  init {
    label = context.getString(R.string.sub_module_maker_title)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_add_module)
    iconRes = R.drawable.ic_add_module
  }

  override suspend fun execAction(data: ActionData): Any {
    // The fragment will handle the UI interaction
    return true
  }
}
