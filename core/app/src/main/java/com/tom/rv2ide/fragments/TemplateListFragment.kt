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
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.fragment.app.viewModels
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import com.tom.rv2ide.R
import com.tom.rv2ide.activities.MainActivity
import com.tom.rv2ide.adapters.TemplateListAdapter
import com.tom.rv2ide.databinding.FragmentTemplateListBinding
import com.tom.rv2ide.templates.AtcInterface
import com.tom.rv2ide.utils.FlexboxUtils
import com.tom.rv2ide.viewmodel.MainViewModel
import java.io.File
import org.slf4j.LoggerFactory

/**
 * A fragment to show the list of available templates.
 *
 * @author Akash Yadav
 */
class TemplateListFragment :
    FragmentWithBinding<FragmentTemplateListBinding>(
        R.layout.fragment_template_list,
        FragmentTemplateListBinding::bind,
    ) {

  private var adapter: TemplateListAdapter? = null
  private var layoutManager: FlexboxLayoutManager? = null

  private lateinit var globalLayoutListener: OnGlobalLayoutListener

  private val viewModel by viewModels<MainViewModel>(ownerProducer = { requireActivity() })

  companion object {

    private val log = LoggerFactory.getLogger(TemplateListFragment::class.java)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    layoutManager = FlexboxLayoutManager(requireContext(), FlexDirection.ROW)
    layoutManager!!.justifyContent = JustifyContent.SPACE_EVENLY

    binding.list.layoutManager = layoutManager

    // This makes sure that the items are evenly distributed in the list
    // and the last row is always aligned to the start
    globalLayoutListener =
        FlexboxUtils.createGlobalLayoutListenerToDistributeFlexboxItemsEvenly(
            { adapter },
            { layoutManager },
        ) { adapter, diff ->
          adapter.fillDiff(diff)
        }

    binding.list.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

    binding.exitButton.setOnClickListener { viewModel.setScreen(MainViewModel.SCREEN_MAIN) }

    viewModel.currentScreen.observe(viewLifecycleOwner) { current ->
      if (current == MainViewModel.SCREEN_TEMPLATE_LIST) {
        reloadTemplates()
      }
    }
  }

  override fun onDestroyView() {
    binding.list.viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutListener)
    super.onDestroyView()
  }

  private fun reloadTemplates() {
    _binding ?: return

    log.debug("Opening ATC templates dialog...")

    // Hide this fragment while the bottom sheet wizard is shown
    binding.root.visibility = View.GONE

    // Open the Android Template Creator dialog; handle callbacks
    AtcInterface()
        .create(
            requireContext(),
            object : AtcInterface.TemplateCreationListener {
              override fun onTemplateSelected(templateName: String) {
                // No-op
              }

              override fun onCreationCancelled() {
                viewModel.setScreen(MainViewModel.SCREEN_MAIN)
              }

              override fun onTemplateCreated(success: Boolean, message: String) {
                // Navigate back to main after attempt; success/failure toasts are handled inside
                // ATC
                viewModel.setScreen(MainViewModel.SCREEN_MAIN)
              }

              override fun onTemplateCreated(success: Boolean, message: String, projectDir: File?) {
                viewModel.setScreen(MainViewModel.SCREEN_MAIN)
                if (success && projectDir != null) {
                  (requireActivity() as MainActivity).openProject(projectDir)
                }
              }
            },
        )
  }
}
