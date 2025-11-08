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

package com.tom.rv2ide.uidesigner.fragments

import com.tom.rv2ide.inflater.events.IInflateEventsListener
import com.tom.rv2ide.inflater.events.IInflationEvent
import com.tom.rv2ide.inflater.events.InflationFinishEvent
import com.tom.rv2ide.inflater.events.InflationStartEvent
import com.tom.rv2ide.inflater.events.OnInflateViewEvent
import com.tom.rv2ide.inflater.internal.ViewImpl

/**
 * Handles layout inflation events in [DesignerWorkspaceFragment].
 *
 * @author Akash Yadav
 */
class WorkspaceLayoutInflationHandler : IInflateEventsListener {

  private var fragment: DesignerWorkspaceFragment? = null

  internal fun init(fragment: DesignerWorkspaceFragment) {
    this.fragment = fragment
  }

  internal fun release() {
    this.fragment = null
  }

  override fun onEvent(event: IInflationEvent<*>) {
    val frag = this.fragment ?: return
    if (event is InflationStartEvent) {
      frag.isInflating = true
      frag.undoManager.disable()
    }
    if (event is InflationFinishEvent) {
      frag.isInflating = false
      frag.undoManager.enable()
      frag.updateHierarchy()
    }
    if (event is OnInflateViewEvent) {
      frag.setupView(event.data)
    }
    if (event is InflationFinishEvent && event.data.isNotEmpty()) {
      val file = (event.data[0] as ViewImpl).file
      frag.workspaceView.file = file
      frag.placeholder.file = file
    }
  }
}
