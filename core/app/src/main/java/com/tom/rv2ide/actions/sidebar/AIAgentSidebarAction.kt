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

package com.tom.rv2ide.actions.sidebar

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.tom.rv2ide.R
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.fragments.sidebar.AIAgentFragment
import com.tom.rv2ide.managers.PreferenceManager
import kotlin.reflect.KClass

/**
 * Sidebar action for AI Agent functionality.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class AIAgentSidebarAction(context: Context, override val order: Int) : AbstractSidebarAction() {

  override val id: String = ID
  override val fragmentClass: KClass<out Fragment> = AIAgentFragment::class

  private val prefManager: PreferenceManager
    get() = BaseApplication.getBaseInstance().prefManager

  init {
    label = context.getString(R.string.ai_agent_title)
    subtitle = "v0.1-preview"
    icon = ContextCompat.getDrawable(context, R.drawable.ic_ai_agent)
    iconRes = R.drawable.ic_ai_agent
  }

  companion object {
    const val ID = "ide.editor.sidebar.ai_agent"
  }

  override suspend fun execAction(data: ActionData): Any {
    // Check if AI Agent is enabled in preferences
    return prefManager.getBoolean("ai_agent_enabled", false)
  }
}
