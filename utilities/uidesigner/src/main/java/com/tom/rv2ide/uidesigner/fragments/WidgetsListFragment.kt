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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.tom.rv2ide.fragments.BaseFragment
import com.tom.rv2ide.uidesigner.adapters.WidgetsCategoryAdapter
import com.tom.rv2ide.uidesigner.databinding.FragmentUiWidgetsBinding
import com.tom.rv2ide.uidesigner.utils.Widgets
import com.tom.rv2ide.uidesigner.viewmodel.WorkspaceViewModel

/**
 * A fragment that shows the list of widgets available to use in the UI designer.
 *
 * @author Akash Yadav
 */
class WidgetsListFragment : BaseFragment() {

  private var binding: FragmentUiWidgetsBinding? = null
  private val viewModel by viewModels<WorkspaceViewModel>(ownerProducer = { requireActivity() })

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    this.binding = FragmentUiWidgetsBinding.inflate(inflater, container, false)
    return this.binding!!.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    this.binding!!.widgets.adapter = WidgetsCategoryAdapter(Widgets.categories, viewModel)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }
}
