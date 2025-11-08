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

package com.tom.rv2ide.fragments.sidebar

import android.os.Bundle
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import androidx.core.view.updateMarginsRelative
import androidx.core.view.updatePadding
import com.tom.rv2ide.databinding.FragmentEditorSidebarBinding
import com.tom.rv2ide.fragments.FragmentWithBinding
import com.tom.rv2ide.utils.EditorSidebarActions

/**
 * Fragment for showing the default items in the editor activity's sidebar.
 *
 * @author Akash Yadav
 */
class EditorSidebarFragment :
    FragmentWithBinding<FragmentEditorSidebarBinding>(FragmentEditorSidebarBinding::inflate) {

  internal fun onApplyWindowInsets(insets: Insets) {
    _binding?.apply {
      title.updateLayoutParams<MarginLayoutParams> {
        updateMarginsRelative(
            top = title.marginTop + insets.top,
        )
      }
      fragmentContainer.updateLayoutParams<MarginLayoutParams> {
        updateMarginsRelative(
            bottom = fragmentContainer.marginBottom + insets.bottom,
        )
      }
      sidebarContainer.updatePadding(
          top = sidebarContainer.paddingTop + insets.top,
          bottom = sidebarContainer.paddingBottom + insets.bottom,
          left = sidebarContainer.paddingLeft + insets.left,
      )
      navigation.updatePadding(bottom = insets.bottom)
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    ViewCompat.setOnApplyWindowInsetsListener(binding.navigation) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.updatePadding(bottom = systemBars.bottom)
      insets
    }

    EditorSidebarActions.setup(this)
  }

  /** Get the (nullable) binding object for this fragment. */
  internal fun getBinding() = _binding
}
