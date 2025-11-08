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

package com.tom.rv2ide.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.tom.rv2ide.R
import com.tom.rv2ide.databinding.FragmentTerminalBinding
import com.tom.rv2ide.viewmodel.TerminalFragmentViewModel
import org.slf4j.LoggerFactory

class TerminalFragment :
    FragmentWithBinding<FragmentTerminalBinding>(
        R.layout.fragment_terminal,
        FragmentTerminalBinding::bind,
    ) {
  private val viewModel by viewModels<TerminalFragmentViewModel>()
  private val logger = LoggerFactory.getLogger(TerminalFragment::class.java)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel.initialize(requireContext())
  }

  override fun onDestroyView() {
    super.onDestroyView()
  }

  private fun setCreateButtonsEnabled(enabled: Boolean) {
    binding.btnNewTerminal.isEnabled = enabled
    binding.btnCreateFirstTerminal.isEnabled = enabled
  }

  private fun updateTerminalCount(count: Int) {
    binding.tvTerminalCount.text = "$count/5"
    binding.btnCloseTerminal.isEnabled = count > 0
  }

  private fun updateEmptyState(isEmpty: Boolean) {
    binding.emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
  }
}
