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

package com.tom.rv2ide.uidesigner.utils

import android.content.Context
import android.view.View
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.uidesigner.utils.views.*
import java.io.File
import org.slf4j.LoggerFactory

/**
 * Material Design 3 Preview Renderer - Main Registry Routes M3 preview requests to appropriate
 * view-specific extensions
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class MaterialDesign3Renderer(private val workspace: IWorkspace? = null) {

  companion object {
    private val log = LoggerFactory.getLogger(MaterialDesign3Renderer::class.java)

    // M3 Color tokens (shared across all views)
    const val M3_PRIMARY = "#1050A4"
    const val M3_ON_PRIMARY = "#FFFFFF"
    const val M3_SECONDARY = "#625B71"
    const val M3_ON_SECONDARY = "#FFFFFF"
    const val M3_SURFACE = "#FFFBFE"
    const val M3_ON_SURFACE = "#1C1B1F"
    const val M3_SURFACE_VARIANT = "#E7E0EC"
    const val M3_ON_SURFACE_VARIANT = "#49454F"
    const val M3_ERROR = "#BA1A1A"
    const val M3_ON_ERROR = "#FFFFFF"
    const val M3_OUTLINE = "#79747E"
    const val M3_OUTLINE_VARIANT = "#CAC4D0"
  }

  /** Main entry point - dispatches to registered view handlers */
  fun applyM3Preview(
      view: View,
      attributeName: String,
      attributeValue: String,
      context: Context,
      layoutFile: File? = null,
  ): Boolean {
    return when (view) {
      is MaterialButton ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is TextInputEditText ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is TextInputLayout ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is FloatingActionButton ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is BadgeDrawable ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is AppBarLayout ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is MaterialToolbar ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is Chip -> view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      is ChipGroup ->
          view.applyM3Preview(attributeName, attributeValue, context, workspace, layoutFile)
      // Add new view types here
      else -> {
        log.debug("No M3 preview support for view type: ${view::class.java.simpleName}")
        false
      }
    }
  }
}
