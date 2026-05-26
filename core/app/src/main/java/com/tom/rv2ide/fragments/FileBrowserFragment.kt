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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.adapter.FileAdapter
import com.tom.rv2ide.configurations.FBProperties
import com.tom.rv2ide.databinding.FragmentFileBrowserBinding
import com.tom.rv2ide.R
import com.tom.rv2ide.models.ExtensionFilter
import com.tom.rv2ide.models.FileFilter
import com.tom.rv2ide.models.FileItem
import com.tom.rv2ide.models.FilterPreferences
import com.tom.rv2ide.utils.PathPreferences
import com.tom.rv2ide.utils.FileIconManager
import java.io.File
import com.google.android.material.button.MaterialButton

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class FileBrowserFragment : Fragment() {

    private var _binding: FragmentFileBrowserBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileAdapter: FileAdapter

    private val rootPath: String by lazy { 
        arguments?.getString(ARG_ROOT_PATH) ?: Environment.getExternalStorageDirectory().path 
    }
    private var currentPath: String = ""

    private val filterPrefs = FilterPreferences()
    private var allFileItems: List<FileItem> = emptyList()

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            // Check if SearchView is expanded and collapse it first
            val activity = requireActivity()
            val searchItem = activity.findViewById<View>(R.id.action_search)
            if (searchItem != null && searchItem.isShown) {
                // SearchView is handling the back press, let it collapse
                isEnabled = false
                activity.onBackPressedDispatcher.onBackPressed()
                isEnabled = true
                return
            }
            
            if (currentPath != rootPath) {
                val parent = File(currentPath).parent
                if (parent != null) {
                    listFiles(parent)
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            } else {
                isEnabled = false
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentPath = PathPreferences.getMainPath(requireContext(), rootPath)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupToolbar()
        setupRecyclerView()
        setupMenu()
        setupBackButton()
        listFiles(currentPath)
        
        binding.gotoMyProj.setOnClickListener {
            listFiles(FBProperties.userProject)
        }
    }

    private fun setupToolbar() {
        // Set the toolbar as the action bar
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(binding.toolbar)
        (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayShowTitleEnabled(false)
        
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_main, menu)

                // Setup SearchView
                val searchItem = menu.findItem(R.id.action_search)
                val searchView = searchItem.actionView as SearchView

                searchView.queryHint = "Search files..."
                searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean = false

                    override fun onQueryTextChange(newText: String?): Boolean {
                        filterPrefs.searchQuery = newText ?: ""
                        applyFilters()
                        return true
                    }
                })

                // Handle SearchView expand/collapse
                searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                        filterPrefs.searchQuery = ""
                        applyFilters()
                        return true
                    }
                })

                // Restore search query if exists
                if (filterPrefs.searchQuery.isNotEmpty()) {
                    searchItem.expandActionView()
                    searchView.setQuery(filterPrefs.searchQuery, false)
                }

                // Update checked states for all menu items
                updateMenuCheckedStates(menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                // Handle home/up button (back arrow)
                if (menuItem.itemId == android.R.id.home) {
                    if (currentPath != rootPath) {
                        val parent = File(currentPath).parent
                        if (parent != null) {
                            listFiles(parent)
                            return true
                        }
                    }
                    return false
                }
                
                return handleMenuItemSelected(menuItem)
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun updateMenuCheckedStates(menu: Menu) {
        // Update file filter checkboxes
        when (filterPrefs.fileFilter) {
            FileFilter.ALL -> menu.findItem(R.id.filter_all)?.isChecked = true
            FileFilter.FOLDERS_ONLY -> menu.findItem(R.id.filter_folders_only)?.isChecked = true
            FileFilter.FILES_ONLY -> menu.findItem(R.id.filter_files_only)?.isChecked = true
        }

        // Update extension filter checkboxes
        menu.findItem(R.id.ext_all)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.ALL)
        menu.findItem(R.id.ext_xml)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.XML)
        menu.findItem(R.id.ext_txt)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.TXT)
        menu.findItem(R.id.ext_pdf)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.PDF)
        menu.findItem(R.id.ext_image)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.IMAGE)
        menu.findItem(R.id.ext_video)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.VIDEO)
        menu.findItem(R.id.ext_audio)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.AUDIO)
        menu.findItem(R.id.ext_apk)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.APK)
        menu.findItem(R.id.ext_zip)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.ZIP)
        menu.findItem(R.id.ext_custom)?.isChecked = filterPrefs.activeExtensions.contains(ExtensionFilter.CUSTOM)
    }

    private fun handleMenuItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // File type filters
            R.id.filter_all -> {
                filterPrefs.fileFilter = FileFilter.ALL
                item.isChecked = true
                applyFilters()
                true
            }
            R.id.filter_folders_only -> {
                filterPrefs.fileFilter = FileFilter.FOLDERS_ONLY
                item.isChecked = true
                applyFilters()
                true
            }
            R.id.filter_files_only -> {
                filterPrefs.fileFilter = FileFilter.FILES_ONLY
                item.isChecked = true
                applyFilters()
                true
            }
            
            // Extension filters
            R.id.ext_all -> {
                toggleExtensionFilter(item, ExtensionFilter.ALL)
                true
            }
            R.id.ext_xml -> {
                toggleExtensionFilter(item, ExtensionFilter.XML)
                true
            }
            R.id.ext_txt -> {
                toggleExtensionFilter(item, ExtensionFilter.TXT)
                true
            }
            R.id.ext_pdf -> {
                toggleExtensionFilter(item, ExtensionFilter.PDF)
                true
            }
            R.id.ext_image -> {
                toggleExtensionFilter(item, ExtensionFilter.IMAGE)
                true
            }
            R.id.ext_video -> {
                toggleExtensionFilter(item, ExtensionFilter.VIDEO)
                true
            }
            R.id.ext_audio -> {
                toggleExtensionFilter(item, ExtensionFilter.AUDIO)
                true
            }
            R.id.ext_apk -> {
                toggleExtensionFilter(item, ExtensionFilter.APK)
                true
            }
            R.id.ext_zip -> {
                toggleExtensionFilter(item, ExtensionFilter.ZIP)
                true
            }
            R.id.ext_custom -> {
                if (item.isChecked) {
                    // Uncheck and remove custom filter
                    item.isChecked = false
                    filterPrefs.activeExtensions.remove(ExtensionFilter.CUSTOM)
                    filterPrefs.customExtension = null
                    applyFilters()
                } else {
                    // Show dialog to set custom extension
                    showCustomExtensionDialog()
                }
                true
            }
            else -> false
        }
    }

    private fun toggleExtensionFilter(item: MenuItem, filter: ExtensionFilter) {
        item.isChecked = !item.isChecked

        if (item.isChecked) {
            filterPrefs.activeExtensions.add(filter)
        } else {
            filterPrefs.activeExtensions.remove(filter)
        }

        applyFilters()
    }

    private fun showCustomExtensionDialog() {
        val input = EditText(requireContext()).apply {
            hint = "e.g., json, kt, java"
            setText(filterPrefs.customExtension ?: "")
            setPadding(48, 32, 48, 32)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.file_custom_extension_title))
            .setMessage(getString(R.string.file_custom_extension_message))
            .setView(input)
            .setPositiveButton(getString(R.string.action_apply)) { _, _ ->
                val extension = input.text.toString().trim()
                if (extension.isNotEmpty()) {
                    filterPrefs.activeExtensions.add(ExtensionFilter.CUSTOM)
                    filterPrefs.customExtension = extension
                    applyFilters()
                }
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun applyFilters() {
        var filteredList = allFileItems

        // Apply file type filter (All/Folders Only/Files Only)
        filteredList = when (filterPrefs.fileFilter) {
            FileFilter.ALL -> filteredList
            FileFilter.FOLDERS_ONLY -> filteredList.filter { it.isDirectory }
            FileFilter.FILES_ONLY -> filteredList.filter { !it.isDirectory }
        }

        // Apply extension filters
        if (!filterPrefs.activeExtensions.contains(ExtensionFilter.ALL) && filterPrefs.activeExtensions.isNotEmpty()) {
            filteredList = filteredList.filter { fileItem ->
                // Always show directories
                if (fileItem.isDirectory) {
                    return@filter true
                }

                // Check if file matches any active extension filter
                filterPrefs.activeExtensions.any { extensionFilter ->
                    when (extensionFilter) {
                        ExtensionFilter.ALL -> true
                        ExtensionFilter.XML -> fileItem.name.endsWith(".xml", ignoreCase = true)
                        ExtensionFilter.TXT -> fileItem.name.endsWith(".txt", ignoreCase = true)
                        ExtensionFilter.PDF -> fileItem.name.endsWith(".pdf", ignoreCase = true)
                        ExtensionFilter.IMAGE -> fileItem.name.matches(Regex(".*\\.(jpg|jpeg|png|gif|bmp|webp)$", RegexOption.IGNORE_CASE))
                        ExtensionFilter.VIDEO -> fileItem.name.matches(Regex(".*\\.(mp4|mkv|avi|mov|flv|wmv)$", RegexOption.IGNORE_CASE))
                        ExtensionFilter.AUDIO -> fileItem.name.matches(Regex(".*\\.(mp3|wav|m4a|flac|aac|ogg)$", RegexOption.IGNORE_CASE))
                        ExtensionFilter.APK -> fileItem.name.endsWith(".apk", ignoreCase = true)
                        ExtensionFilter.ZIP -> fileItem.name.matches(Regex(".*\\.(zip|rar|7z|tar|gz)$", RegexOption.IGNORE_CASE))
                        ExtensionFilter.CUSTOM -> {
                            val ext = filterPrefs.customExtension
                            !ext.isNullOrEmpty() && fileItem.name.endsWith(".$ext", ignoreCase = true)
                        }
                    }
                }
            }
        }

        // Apply search filter
        if (filterPrefs.searchQuery.isNotEmpty()) {
            filteredList = filteredList.filter { it.name.contains(filterPrefs.searchQuery, ignoreCase = true) }
        }

        fileAdapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.file_no_match), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            emptyList(),
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    listFiles(fileItem.path)
                } else {
                    Toast.makeText(requireContext(), "Clicked on file: ${fileItem.name}", Toast.LENGTH_SHORT).show()
                }
            },
            onItemLongClick = { fileItem -> 
                showFileActionsDialog(fileItem) 
            },
            iconProvider = { fileName, isDirectory ->
                FileIconManager.getIconForFile(fileName, isDirectory)
            }
        )
        binding.rvFiles.adapter = fileAdapter
    }

    private fun showFileActionsDialog(fileItem: FileItem) {
        val actions = arrayOf("Copy to", "Move to", "Rename", "Copy full path", "Delete")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(fileItem.name)
            .setItems(actions) { _, which ->
                when (which) {
                    0 -> showCopyDestinationBrowser(fileItem)
                    1 -> showMoveDestinationBrowser(fileItem)
                    2 -> showRenameDialog(fileItem)
                    3 -> copyFullPathToClipboard(fileItem)
                    4 -> showDeleteConfirmation(fileItem)
                }
            }
            .show()
    }

    private fun showCopyDestinationBrowser(fileItemToCopy: FileItem) {
        // Use the last saved dialog path instead of always using FBProperties.userProject
        val lastDialogPath = PathPreferences.getDialogPath(requireContext(), FBProperties.userProject)
        val dialog = FileBrowserDialogFragment.newInstance(
            sourcePath = fileItemToCopy.path,
            initialPath = lastDialogPath,
            operationType = FileBrowserDialogFragment.OperationType.COPY
        )
        dialog.show(parentFragmentManager, FileBrowserDialogFragment.TAG)
    }

    private fun showMoveDestinationBrowser(fileItemToMove: FileItem) {
        val lastDialogPath = PathPreferences.getDialogPath(requireContext(), FBProperties.userProject)
        val dialog = FileBrowserDialogFragment.newInstance(
            sourcePath = fileItemToMove.path,
            initialPath = lastDialogPath,
            operationType = FileBrowserDialogFragment.OperationType.MOVE
        )
        dialog.setOnOperationCompleteListener {
            // Refresh the current directory after move
            listFiles(currentPath)
        }
        dialog.show(parentFragmentManager, FileBrowserDialogFragment.TAG)
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val input = EditText(requireContext()).apply {
            setText(fileItem.name)
            setPadding(48, 32, 48, 32)
            setSelectAllOnFocus(true)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.file_rename_title))
            .setMessage(getString(R.string.file_rename_message, fileItem.name))
            .setView(input)
            .setPositiveButton(getString(R.string.action_rename)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != fileItem.name) {
                    renameFile(fileItem, newName)
                } else if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.file_name_empty), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
        
        input.requestFocus()
    }

    private fun renameFile(fileItem: FileItem, newName: String) {
        val oldFile = File(fileItem.path)
        val newFile = File(oldFile.parent, newName)

        if (newFile.exists()) {
            Toast.makeText(requireContext(), getString(R.string.file_name_exists), Toast.LENGTH_SHORT).show()
            return
        }

        if (oldFile.renameTo(newFile)) {
            Toast.makeText(requireContext(), getString(R.string.file_renamed_success), Toast.LENGTH_SHORT).show()
            listFiles(currentPath)
        } else {
            Toast.makeText(requireContext(), getString(R.string.file_renamed_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyFullPathToClipboard(fileItem: FileItem) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("File Path", fileItem.path)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(requireContext(), getString(R.string.file_path_copied), Toast.LENGTH_SHORT).show()
    }

    private fun showDeleteConfirmation(fileItem: FileItem) {
        val fileType = if (fileItem.isDirectory) "folder" else "file"
        val message = if (fileItem.isDirectory) {
            "Are you sure you want to delete this folder and all its contents?\n\n${fileItem.name}"
        } else {
            "Are you sure you want to delete this file?\n\n${fileItem.name}"
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.file_delete_title, fileType))
            .setMessage(message)
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                deleteFile(fileItem)
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun deleteFile(fileItem: FileItem) {
        val file = File(fileItem.path)
        
        if (file.deleteRecursively()) {
            Toast.makeText(requireContext(), getString(R.string.file_deleted_success), Toast.LENGTH_SHORT).show()
            listFiles(currentPath)
        } else {
            Toast.makeText(requireContext(), getString(R.string.file_deleted_failed), Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackButton() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    fun listFiles(path: String) {
        currentPath = path
        binding.tvCurrentPath.text = path
        
        val goToMyProj: MaterialButton = binding.gotoMyProj
        
        if (currentPath != FBProperties.userProject) {
            if (goToMyProj.visibility != View.VISIBLE) {
                goToMyProj.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start()
                }
            }
        } else {
            if (goToMyProj.visibility == View.VISIBLE) {
                goToMyProj.animate()
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        goToMyProj.visibility = View.GONE
                    }
                    .start()
            }
        }
        // Save the current path
        PathPreferences.saveMainPath(requireContext(), path)

        // Update navigation icon and handle clicks
        if (path == rootPath) {
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.setDisplayHomeAsUpEnabled(false)
        } else {
            (requireActivity() as? androidx.appcompat.app.AppCompatActivity)?.supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                setHomeAsUpIndicator(R.drawable.ic_arrow_back)
            }
        }

        val file = File(path)
        val filesAndFolders = file.listFiles()

        if (filesAndFolders == null) {
            Toast.makeText(requireContext(), getString(R.string.file_cannot_access), Toast.LENGTH_SHORT).show()
            return
        }

        allFileItems = filesAndFolders
            .map { FileItem(it.name, it.path, it.isDirectory) }
            .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        applyFilters()
        binding.rvFiles.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ROOT_PATH = "root_path"

        fun newInstance(rootPath: String? = null): FileBrowserFragment {
            val args = Bundle().apply {
                rootPath?.let { putString(ARG_ROOT_PATH, it) }
            }
            return FileBrowserFragment().apply {
                arguments = args
            }
        }
    }
}