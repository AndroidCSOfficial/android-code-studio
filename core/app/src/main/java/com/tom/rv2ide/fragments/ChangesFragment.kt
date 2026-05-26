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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.tom.rv2ide.R
import com.tom.rv2ide.adapters.FileChangesAdapter
import com.tom.rv2ide.databinding.FragmentChangesBinding
import com.tom.rv2ide.viewmodel.GitViewModel
import com.tom.rv2ide.utils.*

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ChangesFragment : Fragment() {
    
    private var _binding: FragmentChangesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GitViewModel by activityViewModels()
    private lateinit var adapter: FileChangesAdapter

    private lateinit var progressDialog: ProgressDialogHelper

    private fun setupObservers() {
        viewModel.changedFiles.observe(viewLifecycleOwner) { changes ->
            adapter.submitList(changes)
            binding.emptyStateText.visibility = if (changes.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerViewChanges.visibility = if (changes.isEmpty()) View.GONE else View.VISIBLE
            
            val hasStagedFiles = changes.any { it.isStaged }
            binding.fabCommit.visibility = if (hasStagedFiles) View.VISIBLE else View.GONE
        }
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
        }
        
        viewModel.progressMessage.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                progressDialog.show(message)
            } else {
                progressDialog.dismiss()
            }
        }
    }

    private fun showCommitDialog() {
        val prefsManager = PreferencesManager(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_commit, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editTextCommitMessage)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_commit_changes))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.git_commit_action)) { _, _ ->
                val message = editText.text.toString()
                if (message.isNotBlank()) {
                    val author = prefsManager.getGitUserName()
                    val email = prefsManager.getGitUserEmail()
                    viewModel.commit(message, author, email)
                } else {
                    Snackbar.make(binding.root, getString(R.string.git_commit_message_empty), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChangesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        progressDialog = ProgressDialogHelper(requireContext())
        
        setupRecyclerView()
        setupObservers()
        setupButtons()
    }
        
    private fun setupRecyclerView() {
        adapter = FileChangesAdapter(
            onStageClick = { fileChange ->
                viewModel.stageFile(fileChange.path)
            },
            onUnstageClick = { fileChange ->
                viewModel.unstageFile(fileChange.path)
            },
            onDiscardClick = { fileChange ->
                showDiscardConfirmation(fileChange.path)
            }
        )
        
        binding.recyclerViewChanges.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewChanges.adapter = adapter
    }
    
    private fun setupButtons() {
        binding.fabCommit.setOnClickListener {
            showCommitDialog()
        }
        
        binding.buttonStageAll.setOnClickListener {
            viewModel.stageAllFiles()
        }
        
        binding.buttonRefresh.setOnClickListener {
            viewModel.refreshChangedFiles()
        }
    }
    
    private fun showDiscardConfirmation(filePath: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_discard_changes))
            .setMessage(getString(R.string.git_discard_changes_message, filePath))
            .setPositiveButton(getString(R.string.git_discard_action)) { _, _ ->
                viewModel.discardChanges(filePath)
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