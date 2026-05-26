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

package com.tom.rv2ide.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.R
import com.tom.rv2ide.databinding.DialogEditModuleConfigBinding
import com.tom.rv2ide.databinding.LayoutBuildVariantItemBinding
import com.tom.rv2ide.tooling.api.IAndroidProject
import com.tom.rv2ide.tooling.api.models.BuildVariantInfo
import com.tom.rv2ide.tooling.api.models.BuildVariantInfo.Companion.withSelection
import com.tom.rv2ide.viewmodel.BuildVariantsViewModel
import java.util.Objects

/**
 * [RecyclerView] adapter for showing the list of Android modules and their selected build variant.
 *
 * @property items
 * @author Akash Yadav
 */
 
class BuildVariantsAdapter(
    private val viewModel: BuildVariantsViewModel,
    private var items: List<BuildVariantInfo>,
) : RecyclerView.Adapter<BuildVariantsAdapter.ViewHolder>() {

  class ViewHolder(internal val binding: LayoutBuildVariantItemBinding) :
      RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    val binding =
        LayoutBuildVariantItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(binding)
  }

  override fun getItemCount(): Int {
    return items.size
  }

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    val binding = holder.binding
    val variantInfo = items[position]

    binding.moduleName.text = variantInfo.projectPath

    updateModuleInfo(binding, variantInfo)

    binding.editConfig.setOnClickListener {
      showEditDialog(binding, variantInfo, position)
    }

    binding.variantName.apply {
      val viewModel = viewModel

      setAdapter(
          ArrayAdapter(
              binding.root.context,
              R.layout.support_simple_spinner_dropdown_item,
              variantInfo.buildVariants,
          )
      )

      var listSelection = variantInfo.buildVariants.indexOf(variantInfo.selectedVariant)
      if (listSelection < 0 || listSelection >= variantInfo.buildVariants.size) {
        listSelection = 0
      }

      this.listSelection = listSelection
      
      val validSelectedVariant = if (variantInfo.buildVariants.contains(variantInfo.selectedVariant)) {
        variantInfo.selectedVariant
      } else {
        variantInfo.buildVariants.getOrNull(listSelection) ?: IAndroidProject.DEFAULT_VARIANT
      }
      
      setText(validSelectedVariant, false)

      addTextChangedListener { editable ->
        viewModel.updatedBuildVariants =
            viewModel.updatedBuildVariants.also { variants ->

              val newSelection = editable?.toString() ?: IAndroidProject.DEFAULT_VARIANT

              if (!Objects.equals(variantInfo.selectedVariant, newSelection) && 
                  variantInfo.buildVariants.contains(newSelection)) {
                variants[variantInfo.projectPath] = variantInfo.withSelection(newSelection)
              } else {
                variants.remove(variantInfo.projectPath)
              }
            }
      }
    }
  }

  private fun updateModuleInfo(binding: LayoutBuildVariantItemBinding, variantInfo: BuildVariantInfo) {
    binding.moduleInfo.text = buildString {
      variantInfo.versionName?.let { append("v$it") }
      
      if (variantInfo.minSdk != null || variantInfo.targetSdk != null) {
        if (isNotEmpty()) append(" • ")
        variantInfo.minSdk?.let { append("Min $it") }
        if (variantInfo.minSdk != null && variantInfo.targetSdk != null) append(" • ")
        variantInfo.targetSdk?.let { append("Target $it") }
      }
    }.takeIf { it.isNotEmpty() }
  }

  private fun showEditDialog(binding: LayoutBuildVariantItemBinding, variantInfo: BuildVariantInfo, position: Int) {
    val context = binding.root.context
    val dialogBinding = DialogEditModuleConfigBinding.inflate(LayoutInflater.from(context))

    dialogBinding.versionName.setText(variantInfo.versionName ?: "")
    dialogBinding.versionCode.setText(variantInfo.versionCode?.toString() ?: "")
    dialogBinding.minSdk.setText(variantInfo.minSdk?.toString() ?: "")
    dialogBinding.targetSdk.setText(variantInfo.targetSdk?.toString() ?: "")
    dialogBinding.compileSdk.setText(variantInfo.compileSdk?.toString() ?: "")

    MaterialAlertDialogBuilder(context)
        .setTitle(context.getString(R.string.build_edit_variant, variantInfo.projectPath))
        .setView(dialogBinding.root)
        .setPositiveButton(context.getString(R.string.action_save)) { _, _ ->
          val updatedInfo = variantInfo.copy(
              versionName = dialogBinding.versionName.text?.toString()?.takeIf { it.isNotEmpty() },
              versionCode = dialogBinding.versionCode.text?.toString()?.toIntOrNull(),
              minSdk = dialogBinding.minSdk.text?.toString()?.toIntOrNull(),
              targetSdk = dialogBinding.targetSdk.text?.toString()?.toIntOrNull(),
              compileSdk = dialogBinding.compileSdk.text?.toString()?.toIntOrNull()
          )
          
          items = items.toMutableList().apply { set(position, updatedInfo) }
          notifyItemChanged(position)
          
          viewModel.updateModuleConfig(variantInfo.projectPath, updatedInfo)
        }
        .setNegativeButton(context.getString(R.string.action_cancel), null)
        .show()
  }
}