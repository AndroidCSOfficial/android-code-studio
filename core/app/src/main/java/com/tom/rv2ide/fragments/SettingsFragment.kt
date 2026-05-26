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
package com.tom.rv2ide.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.tom.rv2ide.R
import com.tom.rv2ide.databinding.FragmentSettingsBinding
import com.tom.rv2ide.utils.PreferencesManager
import com.tom.rv2ide.viewmodel.GitViewModel

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class SettingsFragment : Fragment() {
    
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GitViewModel by activityViewModels()
    private lateinit var prefsManager: PreferencesManager
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        prefsManager = PreferencesManager(requireContext())
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        loadSettings()
        setupButtons()
    }
    
    private fun loadSettings() {
        binding.textUserName.text = prefsManager.getGitUserName()
        binding.textUserEmail.text = prefsManager.getGitUserEmail()
        binding.switchRememberCredentials.isChecked = prefsManager.shouldRememberCredentials()
        
        val hasCredentials = prefsManager.getUsername() != null
        binding.textCredentialsStatus.text = if (hasCredentials) getString(R.string.git_credentials_saved) else getString(R.string.git_credentials_not_saved)
    }
    
    private fun setupButtons() {
        binding.buttonEditUserConfig.setOnClickListener {
            showEditUserConfigDialog()
        }
        
        binding.switchRememberCredentials.setOnCheckedChangeListener { _, isChecked ->
            prefsManager.setRememberCredentials(isChecked)
            if (!isChecked) {
                prefsManager.clearCredentials()
                binding.textCredentialsStatus.text = getString(R.string.git_credentials_not_saved)
            }
        }
        
        binding.buttonClearCredentials.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.git_clear_credentials))
                .setMessage(getString(R.string.git_clear_credentials_message))
                .setPositiveButton(getString(R.string.git_clear_action)) { _, _ ->
                    prefsManager.clearCredentials()
                    binding.textCredentialsStatus.text = getString(R.string.git_credentials_not_saved)
                    Snackbar.make(binding.root, getString(R.string.git_credentials_cleared), Snackbar.LENGTH_SHORT).show()
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }
    
    private fun showEditUserConfigDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_config, null)
        val editTextName = dialogView.findViewById<TextInputEditText>(R.id.editTextUserName)
        val editTextEmail = dialogView.findViewById<TextInputEditText>(R.id.editTextUserEmail)
        
        editTextName.setText(prefsManager.getGitUserName())
        editTextEmail.setText(prefsManager.getGitUserEmail())
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_user_config))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.git_save_action)) { _, _ ->
                val name = editTextName.text.toString().trim()
                val email = editTextEmail.text.toString().trim()
                
                if (name.isNotBlank() && email.isNotBlank()) {
                    prefsManager.setGitUserName(name)
                    prefsManager.setGitUserEmail(email)
                    viewModel.setUserConfig(name, email)
                    loadSettings()
                    Snackbar.make(binding.root, getString(R.string.git_user_config_updated), Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(binding.root, getString(R.string.git_name_email_empty), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}