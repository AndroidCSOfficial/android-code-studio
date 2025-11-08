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

package com.tom.rv2ide.actions.etc

import android.content.Context
import android.view.MenuItem
import androidx.core.content.ContextCompat
import com.tom.rv2ide.R
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.EditorActivityAction
import com.tom.rv2ide.actions.markInvisible
import com.tom.rv2ide.actions.openApplicationModuleChooser
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.projects.android.androidAppProjects
import com.tom.rv2ide.utils.IntentUtils
import com.tom.rv2ide.utils.flashError
import org.slf4j.LoggerFactory

/**
 * An action to launch the already installed application on the device.
 *
 * @author Akash Yadav
 */
class LaunchAppAction(context: Context, override val order: Int) : EditorActivityAction() {

  override val id: String = "ide.editor.launchInstalledApp"
  override var requiresUIThread: Boolean = true

  init {
    label = context.getString(R.string.title_launch_app)
    icon = ContextCompat.getDrawable(context, R.drawable.ic_open_external)
  }

  companion object {
    private val log = LoggerFactory.getLogger(LaunchAppAction::class.java)
  }

  override fun prepare(data: ActionData) {
    super.prepare(data)
    data.getActivity()
        ?: run {
          markInvisible()
          return
        }

    visible = true

    enabled =
        IProjectManager.getInstance().getWorkspace()?.androidAppProjects()?.iterator()?.hasNext() ==
            true
  }

  override suspend fun execAction(data: ActionData) {
    openApplicationModuleChooser(data) { app ->
      val variant = app.getSelectedVariant()

      log.debug("Selected variant: {}", variant?.name)

      if (variant == null) {
        flashError(R.string.err_selected_variant_not_found)
        return@openApplicationModuleChooser
      }

      val applicationId = variant.mainArtifact.applicationId
      if (applicationId == null) {
        log.error("Unable to launch application. variant.mainArtifact.applicationId is null")
        flashError(R.string.err_cannot_determine_package)
        return@openApplicationModuleChooser
      }

      log.info("Launching application: {}", applicationId)

      val activity = data.requireActivity()
      IntentUtils.launchApp(activity, applicationId, logError = false)
    }
  }

  override fun getShowAsActionFlags(data: ActionData): Int {
    // prefer showing this in the overflow menu
    return MenuItem.SHOW_AS_ACTION_IF_ROOM
  }
}
