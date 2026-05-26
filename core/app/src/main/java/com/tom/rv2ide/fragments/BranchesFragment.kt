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
import com.tom.rv2ide.adapters.BranchesAdapter
import com.tom.rv2ide.databinding.FragmentBranchesBinding
import com.tom.rv2ide.viewmodel.GitViewModel
import com.tom.rv2ide.utils.*

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class BranchesFragment : Fragment() {
    
    private var _binding: FragmentBranchesBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: GitViewModel by activityViewModels()
    private lateinit var adapter: BranchesAdapter
    private var currentBranch: String = ""
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBranchesBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupObservers()
        setupButtons()
        
        viewModel.refreshBranches()
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.refreshBranches()
    }
    
    private fun setupRecyclerView() {
        adapter = BranchesAdapter(
            onCheckoutClick = { branch ->
                if (branch != currentBranch) {
                    viewModel.checkoutBranch(branch)
                }
            },
            onDeleteClick = { branch ->
                showDeleteConfirmation(branch)
            },
            getCurrentBranch = { currentBranch }
        )
        
        binding.recyclerViewBranches.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewBranches.adapter = adapter
    }
    
    private fun setupObservers() {
        viewModel.branches.observe(viewLifecycleOwner) { branches ->
            adapter.submitList(branches.toList())
        }
        
        viewModel.currentBranch.observe(viewLifecycleOwner) { branch ->
            currentBranch = branch
            binding.textCurrentBranch.text = getString(R.string.git_current_branch, branch)
            adapter.notifyDataSetChanged()
        }
        
        viewModel.operationResult.observe(viewLifecycleOwner) { result ->
            Snackbar.make(binding.root, result.message, Snackbar.LENGTH_SHORT).show()
            if (result.success) {
                viewModel.refreshBranches()
            }
        }
    }
    
    private fun setupButtons() {
        binding.fabNewBranch.setOnClickListener {
            showCreateBranchDialog()
        }
        
        binding.buttonRefresh.setOnClickListener {
            viewModel.refreshBranches()
        }
    }
    
    private fun showCreateBranchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_branch_name, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.editTextBranchName)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_create_new_branch))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.git_create_action)) { _, _ ->
                val branchName = editText.text.toString().trim()
                if (branchName.isNotBlank()) {
                    viewModel.createBranch(branchName)
                } else {
                    Snackbar.make(binding.root, getString(R.string.git_branch_name_empty), Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    private fun showDeleteConfirmation(branch: String) {
        if (branch == currentBranch) {
            Snackbar.make(binding.root, getString(R.string.git_cannot_delete_current_branch), Snackbar.LENGTH_SHORT).show()
            return
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.git_delete_branch))
            .setMessage(getString(R.string.git_delete_branch_message, branch))
            .setPositiveButton(getString(R.string.git_delete_action)) { _, _ ->
                viewModel.deleteBranch(branch)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}