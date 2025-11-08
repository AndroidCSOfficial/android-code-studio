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
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tom.rv2ide.R
import com.tom.rv2ide.databinding.FragmentSubModuleBinding
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.utils.ModuleCreator
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Fragment for creating new sub-modules in the project.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class SubModuleFragment : Fragment() {

  private var _binding: FragmentSubModuleBinding? = null
  private val binding
    get() = _binding!!

  private var selectedLanguage: ModuleLanguage = ModuleLanguage.KOTLIN
  private val moduleCreator = ModuleCreator()

  enum class ModuleLanguage {
    KOTLIN,
    JAVA,
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    _binding = FragmentSubModuleBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    setupUI()
    setupLanguageChips()
    setupModuleNameInput()
    setupCreateButton()
  }

  private fun setupUI() {
    binding.apply {
      // Set initial state
      createModuleButton.isEnabled = false

      // Set up language selection
      kotlinChip.isChecked = true
      selectedLanguage = ModuleLanguage.KOTLIN
    }
  }

  private fun setupLanguageChips() {
    binding.apply {
      kotlinChip.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          selectedLanguage = ModuleLanguage.KOTLIN
          javaChip.isChecked = false
        }
      }

      javaChip.setOnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
          selectedLanguage = ModuleLanguage.JAVA
          kotlinChip.isChecked = false
        }
      }
    }
  }

  private fun setupModuleNameInput() {
    binding.moduleNameInput.addTextChangedListener(
        object : TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

          override fun afterTextChanged(s: Editable?) {
            val moduleName = s?.toString()?.trim()
            binding.createModuleButton.isEnabled =
                !moduleName.isNullOrEmpty() && isValidModuleName(moduleName)

            // Update helper text
            if (moduleName.isNullOrEmpty()) {
              binding.moduleNameInputLayout.helperText = getString(R.string.sub_module_name_hint)
            } else {
              val normalized = normalizeModuleName(moduleName)
              if (!isValidModuleName(moduleName)) {
                binding.moduleNameInputLayout.helperText =
                    getString(R.string.sub_module_name_invalid)
                binding.moduleNameInputLayout.setHelperTextColor(
                    requireContext().getColorStateList(R.color.error)
                )
              } else {
                val helperText =
                    if (normalized != moduleName) {
                      "Valid: $normalized"
                    } else {
                      getString(R.string.sub_module_name_valid)
                    }
                binding.moduleNameInputLayout.helperText = helperText
                binding.moduleNameInputLayout.setHelperTextColor(
                    requireContext().getColorStateList(R.color.success)
                )
              }
            }
          }
        }
    )
  }

  private fun setupCreateButton() {
    binding.createModuleButton.setOnClickListener {
      val moduleName = binding.moduleNameInput.text.toString().trim()
      if (isValidModuleName(moduleName)) {
        createModule(moduleName)
      }
    }
  }

  private fun normalizeModuleName(name: String): String {
    // Remove leading/trailing whitespace
    var normalized = name.trim()

    // If empty, return as is
    if (normalized.isEmpty()) return normalized

    // Convert first letter to lowercase if it's uppercase
    if (normalized[0].isUpperCase()) {
      normalized = normalized[0].lowercase() + normalized.substring(1)
    }

    // Handle spaces by converting to camelCase
    if (normalized.contains(" ")) {
      val words = normalized.split("\\s+".toRegex())
      normalized =
          words[0].lowercase() +
              words.drop(1).joinToString("") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
              }
    }

    // Remove any remaining special characters except underscores
    normalized = normalized.replace(Regex("[^a-zA-Z0-9_]"), "")

    return normalized
  }

  private fun isValidModuleName(name: String): Boolean {
    val normalized = normalizeModuleName(name)
    // Module names should be valid Java identifiers and not contain special characters
    return normalized.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$")) && normalized.length >= 2
  }

  private fun createModule(moduleName: String) {
    binding.createModuleButton.isEnabled = false
    binding.progressIndicator.visibility = View.VISIBLE

    lifecycleScope.launch {
      try {
        val result =
            withContext(Dispatchers.IO) {
              // Get the actual project root directory
              val projectRoot = getProjectRootDirectory()
              val normalizedModuleName = normalizeModuleName(moduleName)
              moduleCreator.createModule(
                  moduleName = normalizedModuleName,
                  language = selectedLanguage,
                  projectRoot = projectRoot,
              )
            }

        withContext(Dispatchers.Main) {
          binding.progressIndicator.visibility = View.GONE
          binding.createModuleButton.isEnabled = true

          if (result.success) {
            val normalizedModuleName = normalizeModuleName(moduleName)
            Toast.makeText(
                    requireContext(),
                    getString(R.string.sub_module_created_success, normalizedModuleName),
                    Toast.LENGTH_LONG,
                )
                .show()

            // Clear the input
            binding.moduleNameInput.text?.clear()
          } else {
            Toast.makeText(
                    requireContext(),
                    getString(R.string.sub_module_created_error, result.errorMessage),
                    Toast.LENGTH_LONG,
                )
                .show()
          }
        }
      } catch (e: Exception) {
        withContext(Dispatchers.Main) {
          binding.progressIndicator.visibility = View.GONE
          binding.createModuleButton.isEnabled = true
          Toast.makeText(
                  requireContext(),
                  getString(R.string.sub_module_created_error, e.message),
                  Toast.LENGTH_LONG,
              )
              .show()
        }
      }
    }
  }

  private fun getProjectRootDirectory(): File {
    // Get the project root from the project manager
    val projectManager = IProjectManager.getInstance()
    return File(projectManager.projectDirPath)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}
