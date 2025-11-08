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

package com.tom.rv2ide.actions.etc

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tom.rv2ide.R
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.EditorActivityAction
import com.tom.rv2ide.activities.IDEConfigurations
import org.slf4j.LoggerFactory

/**
 * An action to launch ide configurations
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class IdeConfigurationsAction(context: Context, override val order: Int) : EditorActivityAction() {

  override val id: String = "ide.editor.ideconfigurations"
  override var requiresUIThread: Boolean = true

  init {
    label = context.getString(R.string.title_ide_configurations)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_cfg_main)
  }

  companion object {
    private val log = LoggerFactory.getLogger(IdeConfigurationsAction::class.java)
  }

  override suspend fun execAction(data: ActionData): Boolean {
    val activity = data.requireActivity()
    activity.startActivity(Intent(activity, IDEConfigurations::class.java))
    return true
  }
}
