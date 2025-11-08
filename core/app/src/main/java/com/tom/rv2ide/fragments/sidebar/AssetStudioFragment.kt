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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.tom.rv2ide.activities.AssetStudioActivity
import com.tom.rv2ide.databinding.FragmentAssetStudioBinding

/**
 * Fragment for the Asset Studio sidebar. Provides quick access to asset creation tools.
 *
 * @author Tom
 */
class AssetStudioFragment : Fragment() {

  private var _binding: FragmentAssetStudioBinding? = null
  private val binding
    get() = _binding!!

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentAssetStudioBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupUI()
  }

  private fun setupUI() {
    binding.apply {
      // Set up launch button
      launchStudioButton.setOnClickListener { launchAssetStudio() }

      // Set up quick actions
      setupQuickActions()

      // Set up recent assets
      setupRecentAssets()
    }
  }

  private fun setupQuickActions() {
    binding.apply {
      // Create drawable button
      createDrawableButton.setOnClickListener { launchAssetStudioWithAction("create_drawable") }

      // Create icon button
      createIconButton.setOnClickListener { launchAssetStudioWithAction("create_icon") }

      // Browse Material Icons button
      browseMaterialIconsButton.setOnClickListener { launchAssetStudioWithAction("material_icons") }

      // Import image button
      importImageButton.setOnClickListener { launchAssetStudioWithAction("import_image") }
    }
  }

  private fun setupRecentAssets() {
    // TODO: Implement recent assets list
    binding.recentAssetsContainer.visibility = View.GONE
  }

  private fun launchAssetStudio() {
    val intent = Intent(requireContext(), AssetStudioActivity::class.java)
    startActivity(intent)
  }

  private fun launchAssetStudioWithAction(action: String) {
    val intent =
        Intent(requireContext(), AssetStudioActivity::class.java).apply {
          putExtra("action", action)
        }
    startActivity(intent)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
