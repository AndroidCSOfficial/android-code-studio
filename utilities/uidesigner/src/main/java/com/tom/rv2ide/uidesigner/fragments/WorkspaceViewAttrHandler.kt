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

// import com.tom.rv2ide.uidesigner.utils.applyBackgroundPreview
import com.tom.rv2ide.inflater.internal.ViewImpl
import com.tom.rv2ide.uidesigner.models.UiAttribute
import com.tom.rv2ide.uidesigner.undo.AttrAddedAction
import com.tom.rv2ide.uidesigner.undo.AttrRemovedAction
import com.tom.rv2ide.uidesigner.undo.AttrUpdatedAction
import com.tom.rv2ide.uidesigner.utils.applyAttributePreview

/**
 * Handles view attribute changes in [DesignerWorkspaceFragment].
 *
 * @author Akash Yadav
 */
internal class WorkspaceViewAttrHandler : com.tom.rv2ide.inflater.IView.AttributeChangeListener {

  private var fragment: DesignerWorkspaceFragment? = null

  internal fun init(fragment: DesignerWorkspaceFragment) {
    this.fragment = fragment
  }

  internal fun release() {
    this.fragment = null
  }

  override fun onAttributeAdded(
      view: com.tom.rv2ide.inflater.IView,
      attribute: com.tom.rv2ide.inflater.IAttribute,
  ) {
    val frag = this.fragment ?: return
    frag.undoManager.push(AttrAddedAction(view = view, attr = attribute as UiAttribute))
  }

  override fun onAttributeRemoved(
      view: com.tom.rv2ide.inflater.IView,
      attribute: com.tom.rv2ide.inflater.IAttribute,
  ) {
    val frag = this.fragment ?: return
    frag.undoManager.push(AttrRemovedAction(view = view, attr = attribute as UiAttribute))
  }

  override fun onAttributeUpdated(
      view: com.tom.rv2ide.inflater.IView,
      attribute: com.tom.rv2ide.inflater.IAttribute,
      oldValue: String,
  ) {
    val frag = this.fragment ?: return

    // if (attribute.name.equals("background", ignoreCase = true) && view is ViewImpl) {
    // // Get the layout file from the fragment's viewModel
    // val layoutFile = frag.viewModel.file
    // view.applyBackgroundPreview(frag.requireContext(), layoutFile)
    // }
    if (view is ViewImpl) {
      val layoutFile = frag.viewModel.file
      (view as ViewImpl).applyAttributePreview(
          attribute.name,
          attribute.value,
          frag.requireContext(),
          null,
          layoutFile,
      )
    }

    frag.undoManager.push(
        AttrUpdatedAction(view = view, attr = attribute as UiAttribute, oldValue = oldValue)
    )
  }
}
