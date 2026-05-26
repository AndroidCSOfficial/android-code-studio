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
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.tom.rv2ide.adapter.FileAdapter
import com.tom.rv2ide.databinding.DialogFileBrowserBinding
import com.tom.rv2ide.R
import com.tom.rv2ide.models.FileItem
import com.tom.rv2ide.utils.PathPreferences
import com.tom.rv2ide.utils.FileIconManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlinx.coroutines.*
import com.tom.rv2ide.configurations.FBProperties
import com.google.android.material.button.MaterialButton

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class FileBrowserDialogFragment : DialogFragment() {

    enum class OperationType {
        COPY, MOVE
    }

    private var _binding: DialogFileBrowserBinding? = null
    private val binding get() = _binding!!
    private lateinit var fileAdapter: FileAdapter
    private lateinit var sourceFilePath: String
    private var operationType: OperationType = OperationType.COPY
    private var onOperationCompleteListener: (() -> Unit)? = null

    private val rootPath: String by lazy { 
        Environment.getExternalStorageDirectory().path 
    }
    private var currentPath: String = ""

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            handleBackPress()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceFilePath = requireArguments().getString(ARG_SOURCE_FILE_PATH)!!
        operationType = OperationType.valueOf(
            requireArguments().getString(ARG_OPERATION_TYPE, OperationType.COPY.name)
        )
        setStyle(STYLE_NORMAL, android.R.style.Theme_Translucent_NoTitleBar)
        
        // Initialize currentPath here, BEFORE onViewCreated
        val argInitialPath = arguments?.getString(ARG_INITIAL_PATH)
        currentPath = when {
            argInitialPath != null && File(argInitialPath).exists() -> argInitialPath
            else -> PathPreferences.getDialogPath(requireContext(), rootPath)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, 
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupBackPressedHandler()
        setupCancelOnTouchOutside()
        listFiles(currentPath)
        animateDialogEntry()

        binding.gotoMyProj.setOnClickListener {
            listFiles(FBProperties.userProject)
        }

    }

    private fun setupCancelOnTouchOutside() {
        // Disable default dismiss behavior
        dialog?.setCanceledOnTouchOutside(false)
        
        // Handle clicks on the background
        binding.root.setOnClickListener {
            // Click on the background (outside the card)
            animateDialogExit()
        }
        
        // Prevent clicks on the card from dismissing
        binding.cardContainer.setOnClickListener {
            // Do nothing, just consume the click
        }
    }

    private fun setupToolbar() {
        val title = when (operationType) {
            OperationType.COPY -> "Select Destination (Copy)"
            OperationType.MOVE -> "Select Destination (Move)"
        }
        binding.toolbarDialog.title = title
        
        // Set navigation icon with proper color
        val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
        icon?.setTint(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.material_on_surface_emphasis_high_type))
        binding.toolbarDialog.navigationIcon = icon
        
        binding.toolbarDialog.setNavigationOnClickListener {
            handleBackPress()
        }
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            backPressedCallback
        )
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun animateDialogEntry() {
        binding.root.findViewById<View>(R.id.card_container)?.apply {
            alpha = 0f
            scaleX = 0.8f
            scaleY = 0.8f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
        
        binding.fabPaste.apply {
            alpha = 0f
            scaleX = 0f
            scaleY = 0f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setStartDelay(150)
                .setInterpolator(android.view.animation.OvershootInterpolator())
                .start()
        }
    }

    private fun handleBackPress() {
        if (currentPath != rootPath) {
            val parent = File(currentPath).parent
            if (parent != null) {
                listFiles(parent)
            } else {
                animateDialogExit()
            }
        } else {
            animateDialogExit()
        }
    }

    private fun animateDialogExit() {
        // Disable back button during animation to prevent multiple dismissals
        backPressedCallback.isEnabled = false
        
        val cardView = binding.root.findViewById<View>(R.id.card_container)
        cardView?.animate()
            ?.alpha(0f)
            ?.scaleX(0.8f)
            ?.scaleY(0.8f)
            ?.setDuration(200)
            ?.setInterpolator(android.view.animation.AccelerateInterpolator())
            ?.withEndAction { 
                try {
                    if (isAdded && dialog != null) {
                        dismissNow()
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
            ?.start()
        
        binding.fabPaste.animate()
            .alpha(0f)
            .scaleX(0f)
            .scaleY(0f)
            .setDuration(200)
            .start()
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            emptyList(), 
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    listFiles(fileItem.path)
                } else {
                    Toast.makeText(
                        requireContext(), 
                        "Please select a folder to paste into", 
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, 
            onItemLongClick = {},
            iconProvider = { fileName, isDirectory ->
                FileIconManager.getIconForFile(fileName, isDirectory)
            }
        )
        binding.rvFilesDialog.adapter = fileAdapter
    }

    private fun setupFab() {
        val fabText = when (operationType) {
            OperationType.COPY -> "Paste Here"
            OperationType.MOVE -> "Move Here"
        }
        binding.fabPaste.contentDescription = fabText
        binding.fabPaste.setOnClickListener {
            performOperation()
        }
    }

    fun setOnOperationCompleteListener(listener: () -> Unit) {
        onOperationCompleteListener = listener
    }

    private fun performOperation() {
        when (operationType) {
            OperationType.COPY -> performCopy()
            OperationType.MOVE -> performMove()
        }
    }

    private fun performMove() {
        val sourceFile = File(sourceFilePath)
        val destinationDir = File(currentPath)
        val destinationFile = File(destinationDir, sourceFile.name)

        if (!sourceFile.exists()) {
            Toast.makeText(requireContext(), getString(R.string.file_source_not_exist), Toast.LENGTH_SHORT).show()
            return
        }

        if (destinationFile.exists()) {
            Toast.makeText(
                requireContext(), 
                "File already exists in destination", 
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (sourceFile.absolutePath == destinationFile.absolutePath) {
            Toast.makeText(
                requireContext(), 
                "Source and destination are the same", 
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Check if trying to move a directory into itself
        if (sourceFile.isDirectory && destinationFile.absolutePath.startsWith(sourceFile.absolutePath)) {
            Toast.makeText(
                requireContext(), 
                "Cannot move a folder into itself", 
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Toast.makeText(
            requireContext(), 
            "Moving ${sourceFile.name} to ${destinationDir.name}...", 
            Toast.LENGTH_SHORT
        ).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (sourceFile.renameTo(destinationFile)) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(), 
                            "File moved successfully!", 
                            Toast.LENGTH_SHORT
                        ).show()
                        onOperationCompleteListener?.invoke()
                        animateDialogExit()
                    }
                } else {
                    // If rename fails (different partitions), copy then delete
                    copyFile(sourceFile, destinationFile)
                    sourceFile.deleteRecursively()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(), 
                            "File moved successfully!", 
                            Toast.LENGTH_SHORT
                        ).show()
                        onOperationCompleteListener?.invoke()
                        animateDialogExit()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), 
                        "Failed to move: ${e.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun performCopy() {
        val sourceFile = File(sourceFilePath)
        val destinationDir = File(currentPath)
        val destinationFile = File(destinationDir, sourceFile.name)

        if (!sourceFile.exists()) {
            Toast.makeText(requireContext(), getString(R.string.file_source_not_exist), Toast.LENGTH_SHORT).show()
            return
        }

        if (destinationFile.exists()) {
            Toast.makeText(
                requireContext(), 
                "File already exists in destination", 
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (sourceFile.absolutePath == destinationFile.absolutePath) {
            Toast.makeText(
                requireContext(), 
                "Source and destination are the same", 
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Toast.makeText(
            requireContext(), 
            "Copying ${sourceFile.name} to ${destinationDir.name}...", 
            Toast.LENGTH_SHORT
        ).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                copyFile(sourceFile, destinationFile)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), 
                        "File copied successfully!", 
                        Toast.LENGTH_SHORT
                    ).show()
                    animateDialogExit()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        requireContext(), 
                        "Failed to copy: ${e.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun copyFile(source: File, destination: File) {
        if (source.isDirectory) {
            copyDirectory(source, destination)
        } else {
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            destination.mkdirs()
        }

        source.listFiles()?.forEach { file ->
            val destFile = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile)
            } else {
                FileInputStream(file).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    private fun listFiles(path: String) {
        currentPath = path
        binding.tvCurrentPathDialog.text = path
        
        val gotoMyProj: MaterialButton = binding.gotoMyProj
        
        if (currentPath != FBProperties.userProject) {
            if (gotoMyProj.visibility != View.VISIBLE) {
                gotoMyProj.apply {
                    alpha = 0f
                    visibility = View.VISIBLE
                    animate()
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                }
            }
        } else {
            if (gotoMyProj.visibility == View.VISIBLE) {
                gotoMyProj.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    gotoMyProj.visibility = View.GONE
                }
                .start()
            }
        }
        
        // Save the current path
        PathPreferences.saveDialogPath(requireContext(), path)

        binding.toolbarDialog.navigationIcon = if (path == rootPath) {
            null
        } else {
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_arrow_back)
        }

        val file = File(path)

        if (!file.exists()) {
            Toast.makeText(requireContext(), getString(R.string.file_dir_not_exist, file.name), Toast.LENGTH_SHORT).show()
            return
        }

        if (!file.canRead()) {
            Toast.makeText(requireContext(), getString(R.string.file_dir_cannot_read, file.name), Toast.LENGTH_SHORT).show()
            return
        }

        val filesAndFolders = file.listFiles()

        if (filesAndFolders == null) {
            Toast.makeText(requireContext(), getString(R.string.file_cannot_access_folder), Toast.LENGTH_SHORT).show()
            return
        }

        val fileItems = filesAndFolders.map {
            FileItem(it.name, it.path, it.isDirectory)
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        fileAdapter.updateData(fileItems)
        binding.rvFilesDialog.scrollToPosition(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "FileBrowserDialog"
        private const val ARG_SOURCE_FILE_PATH = "source_file_path"
        private const val ARG_INITIAL_PATH = "initial_path"
        private const val ARG_OPERATION_TYPE = "operation_type"

        fun newInstance(
            sourcePath: String, 
            initialPath: String? = null,
            operationType: OperationType = OperationType.COPY
        ): FileBrowserDialogFragment {
            val args = Bundle().apply {
                putString(ARG_SOURCE_FILE_PATH, sourcePath)
                initialPath?.let { putString(ARG_INITIAL_PATH, it) }
                putString(ARG_OPERATION_TYPE, operationType.name)
            }
            return FileBrowserDialogFragment().apply {
                arguments = args
            }
        }
    }
}