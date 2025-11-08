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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.termux.shared.termux.TermuxConstants.TERMUX_APP.TERMUX_ACTIVITY
import com.tom.rv2ide.R
import com.tom.rv2ide.activities.IDEConfigurations
import com.tom.rv2ide.activities.MainActivity
import com.tom.rv2ide.activities.PreferencesActivity
import com.tom.rv2ide.activities.TerminalActivity
import com.tom.rv2ide.adapters.MainActionsListAdapter
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.app.BaseIDEActivity
import com.tom.rv2ide.common.databinding.LayoutDialogProgressBinding
import com.tom.rv2ide.databinding.BottomsheetGitCloneBinding
import com.tom.rv2ide.databinding.FragmentMainBinding
import com.tom.rv2ide.models.MainScreenAction
import com.tom.rv2ide.resources.R.string
import com.tom.rv2ide.tasks.runOnUiThread
import com.tom.rv2ide.templates.preferences.WizardPreferences
import com.tom.rv2ide.utils.DialogUtils
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.GeneralFileUtils
import com.tom.rv2ide.utils.flashError
import com.tom.rv2ide.utils.flashSuccess
import com.tom.rv2ide.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.slf4j.LoggerFactory

/*
 * Based on original work from AndroidIDE
 * Modified by Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class MainFragment : BaseFragment() {

  private val viewModel by viewModels<MainViewModel>(ownerProducer = { requireActivity() })
  private var binding: FragmentMainBinding? = null

  companion object {
    private val log = LoggerFactory.getLogger(MainFragment::class.java)

    // Common git clone options
    private val COMMON_GIT_OPTIONS =
        listOf(
            GitOption("--depth 1", "Shallow clone (faster)"),
            GitOption("--single-branch", "Clone single branch only"),
            GitOption("--recursive", "Clone with submodules"),
            GitOption("--no-tags", "Don't fetch tags"),
            GitOption("--bare", "Create bare repository"),
        )
  }

  data class GitOption(val flag: String, val description: String)

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    binding = FragmentMainBinding.inflate(inflater, container, false)
    return binding!!.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val actions =
        MainScreenAction.all().also { actions ->
          val onClick = { action: MainScreenAction, _: View ->
            when (action.id) {
              MainScreenAction.ACTION_CREATE_PROJECT -> showCreateProject()
              MainScreenAction.ACTION_OPEN_PROJECT -> showProjectsBottomSheet()
              MainScreenAction.ACTION_CLONE_REPO -> showGitCloneBottomSheet()
              MainScreenAction.ACTION_OPEN_TERMINAL ->
                  startActivity(Intent(requireActivity(), TerminalActivity::class.java))

              MainScreenAction.ACTION_PREFERENCES -> gotoPreferences()
              MainScreenAction.ACTION_DONATE -> {
                startActivity(Intent(requireActivity(), IDEConfigurations::class.java))
              }
              MainScreenAction.ACTION_DOCS -> BaseApplication.getBaseInstance().openDocs()
            }
          }

          actions.forEach { action ->
            action.onClick = onClick

            if (action.id == MainScreenAction.ACTION_OPEN_TERMINAL) {
              action.onLongClick = { _: MainScreenAction, _: View ->
                val intent =
                    Intent(requireActivity(), TerminalActivity::class.java).apply {
                      putExtra(TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION, true)
                    }
                startActivity(intent)
                true
              }
            }
          }
        }

    binding!!.actions.adapter = MainActionsListAdapter(actions)
  }

  override fun onDestroyView() {
    super.onDestroyView()
    binding = null
  }

  private fun showProjectsBottomSheet() {
    val bottomSheet = BottomSheetDialog(requireContext())
    val sheetView = layoutInflater.inflate(R.layout.bottomsheet_project_list, null)

    val recyclerView = sheetView.findViewById<RecyclerView>(R.id.projectsRecyclerView)
    val emptyText = sheetView.findViewById<TextView>(R.id.emptyText)
    val btnBrowse =
        sheetView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBrowse)
    val searchEditText = sheetView.findViewById<TextInputEditText>(R.id.searchEditText)

    recyclerView.layoutManager = LinearLayoutManager(requireContext())

    val projectDirProjects =
        GeneralFileUtils.listDirsInDirectory(Environment.PROJECTS_DIR).filter {
          isValidAndroidProject(it)
        }

    val recentProjectPaths = WizardPreferences.getRecentProjects(requireContext())
    val recentProjectFiles =
        recentProjectPaths.mapNotNull { path ->
          val file = File(path)
          if (file.exists() && file.isDirectory && isValidAndroidProject(file)) file else null
        }

    val allProjectsMap = mutableMapOf<String, File>()

    recentProjectFiles.forEach { file -> allProjectsMap[file.absolutePath] = file }

    projectDirProjects.forEach { file -> allProjectsMap[file.absolutePath] = file }

    val projectDirs =
        allProjectsMap.values
            .toList()
            .sortedWith(
                compareBy<File> { project ->
                      val recentIndex = recentProjectPaths.indexOf(project.absolutePath)
                      if (recentIndex >= 0) recentIndex else Int.MAX_VALUE
                    }
                    .thenByDescending { it.lastModified() }
            )

    var adapter: ProjectsListAdapter? = null

    if (projectDirs.isEmpty()) {
      recyclerView.visibility = View.GONE
      emptyText.visibility = View.VISIBLE
    } else {
      recyclerView.visibility = View.VISIBLE
      emptyText.visibility = View.GONE

      adapter =
          ProjectsListAdapter(
              projects = projectDirs,
              onProjectClick = { selectedDir ->
                bottomSheet.dismiss()
                openProject(selectedDir)
              },
              onProjectLongClick = { project ->
                showProjectOptionsDialog(project) {
                  val updatedProjectDirProjects =
                      GeneralFileUtils.listDirsInDirectory(Environment.PROJECTS_DIR).filter {
                        isValidAndroidProject(it)
                      }
                  val updatedRecentProjectPaths =
                      WizardPreferences.getRecentProjects(requireContext())
                  val updatedRecentProjectFiles =
                      updatedRecentProjectPaths.mapNotNull { path ->
                        val file = File(path)
                        if (file.exists() && file.isDirectory && isValidAndroidProject(file)) file
                        else null
                      }

                  val updatedAllProjectsMap = mutableMapOf<String, File>()
                  updatedRecentProjectFiles.forEach { file ->
                    updatedAllProjectsMap[file.absolutePath] = file
                  }
                  updatedProjectDirProjects.forEach { file ->
                    updatedAllProjectsMap[file.absolutePath] = file
                  }

                  val updatedDirs =
                      updatedAllProjectsMap.values
                          .toList()
                          .sortedWith(
                              compareBy<File> { proj ->
                                    val recentIndex =
                                        updatedRecentProjectPaths.indexOf(proj.absolutePath)
                                    if (recentIndex >= 0) recentIndex else Int.MAX_VALUE
                                  }
                                  .thenByDescending { it.lastModified() }
                          )

                  adapter?.updateProjects(updatedDirs)

                  if (updatedDirs.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyText.visibility = View.VISIBLE
                    emptyText.text = getString(string.no_projects_found)
                  }
                }
              },
          )
      recyclerView.adapter = adapter

      searchEditText?.addTextChangedListener(
          object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
              adapter?.filter(s?.toString() ?: "")

              if (adapter?.itemCount == 0) {
                recyclerView.visibility = View.GONE
                emptyText.visibility = View.VISIBLE
                emptyText.text = getString(string.no_projects_match_search)
              } else {
                recyclerView.visibility = View.VISIBLE
                emptyText.visibility = View.GONE
              }
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
          }
      )
    }

    btnBrowse.setOnClickListener {
      bottomSheet.dismiss()
      pickDirectory(this::openProject)
    }

    bottomSheet.setContentView(sheetView)
    bottomSheet.show()
  }

  private fun showProjectOptionsDialog(project: File, onActionComplete: () -> Unit) {
    val options = arrayOf("Backup project", "Delete project", "Rename")

    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    builder.setTitle(project.name)
    builder.setItems(options) { dialog, which ->
      when (which) {
        0 -> {
          backupProject(project, onActionComplete)
        }
        1 -> {
          showDeleteProjectConfirmation(project, onActionComplete)
        }
        2 -> {
          showRenameDialog(project, onActionComplete)
        }
      }
      dialog.dismiss()
    }
    builder.show()
  }

  private fun showRenameDialog(project: File, onComplete: () -> Unit) {
    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    builder.setTitle(getString(string.rename_project))

    val inputLayout =
        TextInputLayout(requireContext()).apply {
          boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
          hint = getString(string.new_project_name)
          setPadding(64, 16, 64, 16)
        }

    val input =
        TextInputEditText(requireContext()).apply {
          setText(project.name)
          selectAll()
        }

    inputLayout.addView(input)
    builder.setView(inputLayout)

    builder.setPositiveButton(getString(string.rename)) { dialog, _ ->
      val newName = input.text?.toString()?.trim()

      when {
        newName.isNullOrBlank() -> {
          flashError(string.error)
        }
        newName == project.name -> {
          dialog.dismiss()
        }
        !newName.matches(Regex("^[a-zA-Z][a-zA-Z0-9_]*$")) -> {
          com.google.android.material.dialog
              .MaterialAlertDialogBuilder(requireContext())
              .setTitle(getString(string.invalid_name))
              .setMessage(getString(string.invalid_project_name_message))
              .setPositiveButton(getString(android.R.string.ok), null)
              .show()
        }
        else -> {
          val newProjectDir = File(project.parentFile, newName)
          if (newProjectDir.exists()) {
            com.google.android.material.dialog
                .MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(string.name_already_exists))
                .setMessage(getString(string.project_name_exists_message, newName))
                .setPositiveButton(getString(android.R.string.ok), null)
                .show()
          } else {
            renameProject(project, newProjectDir, onComplete)
            dialog.dismiss()
          }
        }
      }
    }

    builder.setNegativeButton(getString(string.cancel)) { dialog, _ -> dialog.dismiss() }

    val alertDialog = builder.show()

    input.requestFocus()
    val imm =
        requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE)
            as android.view.inputmethod.InputMethodManager
    input.postDelayed(
        { imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT) },
        100,
    )
  }

  private fun renameProject(oldProject: File, newProject: File, onComplete: () -> Unit) {
    val coroutineScope = (activity as? BaseIDEActivity?)?.activityScope ?: viewLifecycleScope

    coroutineScope.launch(Dispatchers.IO) {
      try {
        val renamed = oldProject.renameTo(newProject)

        withContext(Dispatchers.Main) {
          if (renamed) {
            val recentProjects =
                WizardPreferences.getRecentProjects(requireContext()).toMutableList()
            val oldIndex = recentProjects.indexOf(oldProject.absolutePath)

            if (oldIndex >= 0) {
              recentProjects.removeAt(oldIndex)
              recentProjects.add(oldIndex, newProject.absolutePath)

              requireContext()
                  .getSharedPreferences("atc_wizard_prefs", android.content.Context.MODE_PRIVATE)
                  .edit()
                  .putString("recent_projects", recentProjects.joinToString(","))
                  .apply()
            } else {
              WizardPreferences.addRecentProject(requireContext(), newProject.absolutePath)
            }

            flashSuccess(string.project_renamed_success)
            onComplete()
          } else {
            flashError(string.project_rename_failed)
          }
        }
      } catch (e: Exception) {
        log.error("Error renaming project: ${oldProject.absolutePath}", e)
        withContext(Dispatchers.Main) { flashError(string.project_rename_failed) }
      }
    }
  }

  private fun showDeleteProjectConfirmation(project: File, onDeleted: () -> Unit) {
    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    builder.setTitle(getString(string.delete_project_title))
    builder.setMessage(getString(string.delete_project_message, project.name))
    builder.setPositiveButton(getString(string.delete)) { dialog, _ ->
      deleteProject(project, onDeleted)
      dialog.dismiss()
    }
    builder.setNegativeButton(getString(string.cancel)) { dialog, _ -> dialog.dismiss() }
    builder.show()
  }

  private fun backupProject(project: File, onComplete: () -> Unit) {
    val coroutineScope = (activity as? BaseIDEActivity?)?.activityScope ?: viewLifecycleScope

    val backupDir = File(Environment.PROJECTS_DIR, "backed_up_projects")
    if (!backupDir.exists()) {
      backupDir.mkdirs()
    }

    val timestamp =
        java.text
            .SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault())
            .format(java.util.Date())
    val backupFileName = "${project.name}_backup_$timestamp.zip"
    val backupFile = File(backupDir, backupFileName)

    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    val binding = LayoutDialogProgressBinding.inflate(layoutInflater)

    binding.message.visibility = View.VISIBLE
    binding.message.text = "Backing up project..."
    binding.progress.isIndeterminate = true

    builder.setTitle("Backup in Progress")
    builder.setMessage("Creating backup of ${project.name}")
    builder.setView(binding.root)
    builder.setCancelable(false)

    val dialog = builder.show()

    coroutineScope.launch(Dispatchers.IO) {
      try {
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(backupFile)).use { zipOut ->
          project.walkTopDown().forEach { file ->
            if (file.isFile) {
              val relativePath = file.relativeTo(project).path
              if (
                  !relativePath.startsWith("build/") &&
                      !relativePath.contains("/build/") &&
                      !relativePath.startsWith(".androidide/") &&
                      !relativePath.startsWith(".gradle/") &&
                      !relativePath.contains("/.gradle/") &&
                      !relativePath.startsWith(".idea/") &&
                      !relativePath.contains("/.idea/")
              ) {

                val zipEntry = java.util.zip.ZipEntry(relativePath)
                zipOut.putNextEntry(zipEntry)
                file.inputStream().use { input -> input.copyTo(zipOut) }
                zipOut.closeEntry()
              }
            }
          }
        }

        withContext(Dispatchers.Main) {
          dialog.dismiss()

          val successBuilder = DialogUtils.newMaterialDialogBuilder(requireContext())
          successBuilder.setTitle("Backup Completed")
          successBuilder.setMessage(
              "Project backed up successfully!\n\nLocation:\n${backupFile.absolutePath}"
          )
          successBuilder.setPositiveButton("OK") { d, _ ->
            d.dismiss()
            onComplete()
          }
          successBuilder.show()
        }
      } catch (e: Exception) {
        log.error("Error backing up project: ${project.absolutePath}", e)
        withContext(Dispatchers.Main) {
          dialog.dismiss()

          val errorBuilder = DialogUtils.newMaterialDialogBuilder(requireContext())
          errorBuilder.setTitle("Backup Failed")
          errorBuilder.setMessage("Failed to backup project: ${e.localizedMessage}")
          errorBuilder.setPositiveButton("OK", null)
          errorBuilder.show()
        }
      }
    }
  }

  private fun isValidAndroidProject(dir: File): Boolean {
    if (!dir.isDirectory) return false

    val gradleWrapper = File(dir, "gradle/wrapper/gradle-wrapper.properties")
    if (!gradleWrapper.exists()) return false

    val buildGradle = File(dir, "build.gradle")
    val buildGradleKts = File(dir, "build.gradle.kts")

    return buildGradle.exists() || buildGradleKts.exists()
  }

  private fun deleteProject(project: File, onDeleted: () -> Unit) {
    val coroutineScope = (activity as? BaseIDEActivity?)?.activityScope ?: viewLifecycleScope

    coroutineScope.launch(Dispatchers.IO) {
      try {
        val deleted = project.deleteRecursively()
        withContext(Dispatchers.Main) {
          if (deleted) {
            flashSuccess(string.project_deleted_success)
            onDeleted()
          } else {
            flashError(string.project_delete_failed)
          }
        }
      } catch (e: Exception) {
        log.error("Error deleting project: ${project.absolutePath}", e)
        withContext(Dispatchers.Main) { flashError(string.project_delete_failed) }
      }
    }
  }

  private fun showCreateProject() {
    com.tom.rv2ide.templates
        .AtcInterface()
        .create(
            requireContext(),
            object : com.tom.rv2ide.templates.AtcInterface.TemplateCreationListener {
              override fun onTemplateSelected(templateName: String) {
                /* no-op */
              }

              override fun onCreationCancelled() {
                /* stay on main */
              }

              override fun onTemplateCreated(success: Boolean, message: String) {
                /* handled in projectDir overload or via toasts */
              }

              override fun onTemplateCreated(
                  success: Boolean,
                  message: String,
                  projectDir: java.io.File?,
              ) {
                if (success && projectDir != null) {
                  openProject(projectDir)
                }
              }
            },
        )
  }

  fun openProject(root: File) {
    WizardPreferences.addRecentProject(requireContext(), root.absolutePath)

    (requireActivity() as MainActivity).openProject(root)
  }

  private fun showGitCloneBottomSheet() {
    val bottomSheet = BottomSheetDialog(requireContext())
    val sheetBinding = BottomsheetGitCloneBinding.inflate(layoutInflater)

    COMMON_GIT_OPTIONS.forEach { option ->
      val chip = Chip(requireContext())
      chip.text = option.description
      chip.isCheckable = true
      chip.isCheckedIconVisible = true
      chip.tag = option.flag
      sheetBinding.chipGroup.addView(chip)
    }

    sheetBinding.btnClone.setOnClickListener {
      val url = sheetBinding.etRepoUrl.text?.toString()
      val branch = sheetBinding.etBranch.text?.toString()?.takeIf { it.isNotBlank() }
      val customOptions = sheetBinding.etCustomOptions.text?.toString()

      val selectedOptions = mutableListOf<String>()
      for (i in 0 until sheetBinding.chipGroup.childCount) {
        val chip = sheetBinding.chipGroup.getChildAt(i) as? Chip
        if (chip?.isChecked == true) {
          selectedOptions.add(chip.tag as String)
        }
      }

      if (!customOptions.isNullOrBlank()) {
        selectedOptions.addAll(customOptions.split(" ").filter { it.isNotBlank() })
      }

      bottomSheet.dismiss()
      doClone(url, branch, selectedOptions)
    }

    sheetBinding.btnCancel.setOnClickListener { bottomSheet.dismiss() }

    bottomSheet.setContentView(sheetBinding.root)
    bottomSheet.show()
  }

  private fun doClone(repo: String?, branch: String? = null, options: List<String> = emptyList()) {
    if (repo.isNullOrBlank()) {
      log.warn("Unable to clone repo. Invalid repo URL : {}'", repo)
      flashError(string.git_clone_invalid_url)
      return
    }

    var url = repo.trim()
    if (!url.endsWith(".git")) {
      url += ".git"
    }

    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    val binding = LayoutDialogProgressBinding.inflate(layoutInflater)

    binding.message.visibility = View.VISIBLE

    builder.setTitle(string.git_clone_in_progress)
    builder.setMessage(url)
    builder.setView(binding.root)
    builder.setCancelable(false)

    val repoName = url.substringAfterLast('/').substringBeforeLast(".git")
    val targetDir = File(Environment.PROJECTS_DIR, repoName)

    val progress = GitCloneProgressMonitor(binding.progress, binding.message)
    val coroutineScope = (activity as? BaseIDEActivity?)?.activityScope ?: viewLifecycleScope

    var getDialog: Function0<AlertDialog?>? = null

    val cloneJob =
        coroutineScope.launch(Dispatchers.IO) {
          val git =
              try {
                val cloneCommand =
                    Git.cloneRepository()
                        .setURI(url)
                        .setDirectory(targetDir)
                        .setProgressMonitor(progress)

                branch?.let { cloneCommand.setBranch(it) }

                options.forEach { option ->
                  when (option) {
                    "--depth 1" -> cloneCommand.setDepth(1)
                    "--single-branch" -> cloneCommand.setCloneAllBranches(false)
                    "--recursive" -> cloneCommand.setCloneSubmodules(true)
                    "--no-tags" -> cloneCommand.setNoTags()
                    "--bare" -> cloneCommand.setBare(true)
                  }
                }

                cloneCommand.call()
              } catch (err: Throwable) {
                if (!progress.isCancelled) {
                  err.printStackTrace()
                  withContext(Dispatchers.Main) {
                    getDialog?.invoke()?.also { if (it.isShowing) it.dismiss() }
                    showCloneError(err)
                  }
                }
                null
              }

          try {
            git?.close()
          } finally {
            val success = git != null
            withContext(Dispatchers.Main) {
              getDialog?.invoke()?.also { dialog ->
                if (dialog.isShowing) dialog.dismiss()
                if (success) flashSuccess(string.git_clone_success)
              }
            }
          }
        }

    builder.setPositiveButton(android.R.string.cancel) { iface, _ ->
      iface.dismiss()
      progress.cancel()
      cloneJob.cancel(CancellationException("Cancelled by user"))
    }

    val dialog = builder.show()
    getDialog = { dialog }
  }

  private fun showCloneError(error: Throwable?) {
    if (error == null) {
      flashError(string.git_clone_failed)
      return
    }

    val builder = DialogUtils.newMaterialDialogBuilder(requireContext())
    builder.setTitle(string.git_clone_failed)
    builder.setMessage(error.localizedMessage)
    builder.setPositiveButton(android.R.string.ok, null)
    builder.show()
  }

  private fun gotoPreferences() {
    startActivity(Intent(requireActivity(), PreferencesActivity::class.java))
  }

  class GitCloneProgressMonitor(val progress: LinearProgressIndicator, val message: TextView) :
      ProgressMonitor {

    private var cancelled = false

    fun cancel() {
      cancelled = true
    }

    override fun start(totalTasks: Int) {
      runOnUiThread { progress.max = totalTasks }
    }

    override fun beginTask(title: String?, totalWork: Int) {
      runOnUiThread { message.text = title }
    }

    override fun update(completed: Int) {
      runOnUiThread { progress.progress = completed }
    }

    override fun showDuration(enabled: Boolean) {}

    override fun endTask() {}

    override fun isCancelled(): Boolean {
      return cancelled || Thread.currentThread().isInterrupted
    }
  }

  private class ProjectsListAdapter(
      private var projects: List<File>,
      private val onProjectClick: (File) -> Unit,
      private val onProjectLongClick: (File) -> Unit,
  ) : RecyclerView.Adapter<ProjectsListAdapter.ProjectViewHolder>() {

    private var filteredProjects: List<File> = projects

    inner class ProjectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
      val projectName: TextView = view.findViewById(R.id.projectName)
      val projectPath: TextView = view.findViewById(R.id.projectPath)
      val recentBadge: TextView = view.findViewById(R.id.recentBadge)
      val root: View = view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.item_project, parent, false)
      return ProjectViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
      val project = filteredProjects[position]
      holder.projectName.text = project.name
      holder.projectPath.text = project.absolutePath

      val recentRank =
          WizardPreferences.getRecentProjectRank(holder.root.context, project.absolutePath)
      val isRecent = recentRank in 0..2 // Top 3 most recent projects
      holder.recentBadge.visibility = if (isRecent) View.VISIBLE else View.GONE

      holder.root.setOnClickListener { onProjectClick(project) }

      holder.root.setOnLongClickListener {
        onProjectLongClick(project)
        true
      }
    }

    override fun getItemCount() = filteredProjects.size

    fun filter(query: String) {
      filteredProjects =
          if (query.isBlank()) {
            projects
          } else {
            projects.filter { project ->
              project.name.contains(query, ignoreCase = true) ||
                  project.absolutePath.contains(query, ignoreCase = true)
            }
          }
      notifyDataSetChanged()
    }

    fun updateProjects(newProjects: List<File>) {
      projects = newProjects
      filteredProjects = newProjects
      notifyDataSetChanged()
    }
  }
}
