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
import com.tom.rv2ide.configurations.GCProperties
import com.tom.rv2ide.databinding.FragmentInitBinding
import com.tom.rv2ide.viewmodel.GitViewModel
import com.tom.rv2ide.viewmodel.RepositoryStatus
import com.tom.rv2ide.utils.*
import java.io.File

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class InitFragment : Fragment() {
    
    private var _binding: FragmentInitBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GitViewModel by activityViewModels()
    private lateinit var progressDialog: ProgressDialogHelper
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInitBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        progressDialog = ProgressDialogHelper(requireContext())
        
        setupButtons()
        setupObservers()
    }
    
    private fun setupButtons() {
        binding.buttonInitRepo.setOnClickListener {
            showInitializeDialog()
        }
        
        binding.buttonCloneRepo.setOnClickListener {
            showCloneDialog()
        }
        
        binding.buttonOpenRepo.setOnClickListener {
            viewModel.openExistingRepository(GCProperties.userProject)
        }
    }
    
    private fun setupObservers() {
        viewModel.repositoryStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                RepositoryStatus.INITIALIZED, RepositoryStatus.OPENED -> {
                    // Repository is ready, MainActivity will handle navigation
                }
                RepositoryStatus.ERROR -> {
                    Snackbar.make(binding.root, getString(R.string.git_init_failed), Snackbar.LENGTH_LONG).show()
                }
                else -> {}
            }
        }
        
        viewModel.progressMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                progressDialog.show(message)
            } else {
                progressDialog.dismiss()
            }
        }
    }
    
    fun createGitIgnoreAndOthers() {
        val ctGitIgnore = """
            # Ignore Gradle project-specific cache directory
            .gradle
    
            # Ignore ACS editor directory
            .acside
    
            # Ignore Kotlin directory
            .kotlin
    
            # Ignore Gradle build output directory
            build
        """.trimIndent()
    
        val ctGitAttributes = """
            #
            # https://help.github.com/articles/dealing-with-line-endings/
            #
            # Linux start script should use lf
            /gradlew        text eol=lf
    
            # These are Windows script files and should use crlf
            *.bat           text eol=crlf
    
            # Binary files should be left untouched
            *.jar           binary
        """.trimIndent()
        val userProject = GCProperties.userProject
        val files = arrayOf("${userProject}/.gitignore", "${userProject}/.gitattributes")
        val content = arrayOf(ctGitIgnore, ctGitAttributes)
    
        for (i in files.indices) {
            File(files[i]).writeText(content[i])
        }
    }
    
    private fun showInitializeDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_branch_name, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editTextBranchName)
        editText.setText("main")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_initialize_repo))
            .setMessage(getString(R.string.git_initialize_repo_message, GCProperties.userProject))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.git_initialize_action)) { _, _ ->
                val branchName = editText.text.toString().trim()
                val finalBranchName = if (branchName.isBlank()) "main" else branchName
                
                createGitIgnoreAndOthers()
                viewModel.initializeRepository(GCProperties.userProject, finalBranchName)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
        
    private fun showCloneDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_clone, null)
        val editTextUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextCloneUrl)
        val editTextUsername = dialogView.findViewById<TextInputEditText>(R.id.editTextCloneUsername)
        val editTextPassword = dialogView.findViewById<TextInputEditText>(R.id.editTextClonePassword)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_clone_repository))
            .setMessage(getString(R.string.git_clone_to, GCProperties.userProject))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.git_clone_action)) { _, _ ->
                val url = editTextUrl.text.toString()
                val username = editTextUsername.text.toString().takeIf { it.isNotBlank() }
                val password = editTextPassword.text.toString().takeIf { it.isNotBlank() }
                
                if (url.isNotBlank()) {
                    viewModel.cloneRepository(url, GCProperties.userProject, username, password)
                } else {
                    Snackbar.make(binding.root, getString(R.string.git_url_empty), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog.dismiss()
        _binding = null
    }
}