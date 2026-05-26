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

package com.tom.rv2ide.activities.editor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller.SessionCallback
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Process
import android.text.SpannableStringBuilder
import android.widget.FrameLayout
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.GravityInt
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.collection.MutableIntIntMap
import androidx.core.graphics.Insets
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.constant.MemoryConstants
import com.blankj.utilcode.util.ConvertUtils.byte2MemorySize
import com.blankj.utilcode.util.FileUtils
import com.blankj.utilcode.util.ThreadUtils
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.tom.rv2ide.R
import com.tom.rv2ide.R.string
import com.tom.rv2ide.actions.ActionItem.Location.EDITOR_FILE_TABS
import com.tom.rv2ide.adapters.DiagnosticsAdapter
import com.tom.rv2ide.adapters.SearchListAdapter
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity
import com.tom.rv2ide.databinding.ActivityEditorBinding
import com.tom.rv2ide.databinding.ContentEditorBinding
import com.tom.rv2ide.databinding.LayoutDiagnosticInfoBinding
import com.tom.rv2ide.events.InstallationResultEvent
import com.tom.rv2ide.fragments.SearchResultFragment
import com.tom.rv2ide.fragments.sidebar.EditorSidebarFragment
import com.tom.rv2ide.fragments.sidebar.FileTreeFragment
import com.tom.rv2ide.handlers.EditorActivityLifecyclerObserver
import com.tom.rv2ide.handlers.LspHandler.registerLanguageServers
import com.tom.rv2ide.interfaces.DiagnosticClickListener
import com.tom.rv2ide.lookup.Lookup
import com.tom.rv2ide.lsp.models.DiagnosticItem
import com.tom.rv2ide.models.DiagnosticGroup
import com.tom.rv2ide.models.OpenedFile
import com.tom.rv2ide.models.Range
import com.tom.rv2ide.models.SearchResult
import com.tom.rv2ide.preferences.internal.BuildPreferences
import com.tom.rv2ide.projectdata.state.lsp.Index
import com.tom.rv2ide.indexing.views.IndexingBanner
import com.tom.rv2ide.projectdata.state.Initialization
import com.tom.rv2ide.projectdata.logs.LogStream
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.tasks.cancelIfActive
import com.tom.rv2ide.ui.CodeEditorView
import com.tom.rv2ide.ui.ContentTranslatingDrawerLayout
import com.tom.rv2ide.ui.SwipeRevealLayout
import com.tom.rv2ide.uidesigner.UIDesignerActivity
import com.tom.rv2ide.utils.ActionMenuUtils.createMenu
import com.tom.rv2ide.utils.ApkInstallationSessionCallback
import com.tom.rv2ide.utils.DialogUtils.newMaterialDialogBuilder
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.InstallationResultHandler.onResult
import com.tom.rv2ide.utils.IntentUtils
import com.tom.rv2ide.utils.MemoryUsageWatcher
import com.tom.rv2ide.utils.flashError
import com.tom.rv2ide.utils.flashInfo
import com.tom.rv2ide.utils.flashSuccess
import com.tom.rv2ide.utils.resolveAttr
import com.tom.rv2ide.viewmodel.EditorViewModel
import com.tom.rv2ide.xml.resources.ResourceTableRegistry
import com.tom.rv2ide.xml.versions.ApiVersionsRegistry
import com.tom.rv2ide.xml.widgets.WidgetTableRegistry
import com.tom.rv2ide.setup.Setup
import com.tom.rv2ide.experimental.depsupdater.DependencyUpdaterDialog
import com.tom.rv2ide.preferences.internal.BuildPreferences.isKtIndexingNotificationEnabled
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.tom.rv2ide.projects.internal.ProjectManagerImpl
import io.github.mohammedbaqernull.seasonal.SeasonalEffects

/**
 * Base class for EditorActivity which handles most of the view related things.
 *
 * original @author Akash Yadav
 * @modification Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseEditorActivity :
    EdgeToEdgeIDEActivity(), TabLayout.OnTabSelectedListener, DiagnosticClickListener {

  protected val mLifecycleObserver = EditorActivityLifecyclerObserver()
  protected var diagnosticInfoBinding: LayoutDiagnosticInfoBinding? = null
  protected var filesTreeFragment: FileTreeFragment? = null
  protected var editorBottomSheet: BottomSheetBehavior<out View?>? = null
  protected val memoryUsageWatcher = MemoryUsageWatcher()
  protected val pidToDatasetIdxMap = MutableIntIntMap(initialCapacity = 3)
  private val bottomSheetHeaderHideReasons = mutableSetOf<String>()
  private var bottomSheetCardVisibilitySnapshot: Int = View.VISIBLE
  private var bottomSheetHeaderVisibilitySnapshot: Int = View.VISIBLE

  private var autoSaveCoroutineJob: Job? = null
  private val autoSaveMutex = Mutex()
  private val pendingSaveFiles = ConcurrentHashMap<File, Boolean>()
  private val editorTextWatchers = ConcurrentHashMap<CodeEditorView, TextWatcher>()
  private val editorContentHashes = ConcurrentHashMap<CodeEditorView, Int>()
  private var lastAutoSaveCheck = 0L
  
  private lateinit var dependencyUpdater: DependencyUpdaterDialog

  var isDestroying = false
    protected set

  /** Editor activity's [CoroutineScope] for executing tasks in the background. */
  protected val editorActivityScope = CoroutineScope(Dispatchers.Default)

  internal var installationCallback: ApkInstallationSessionCallback? = null

  var uiDesignerResultLauncher: ActivityResultLauncher<Intent>? = null
  val editorViewModel by viewModels<EditorViewModel>()

  internal var _binding: ActivityEditorBinding? = null
  val binding: ActivityEditorBinding
    get() = checkNotNull(_binding) { "Activity has been destroyed" }

  val content: ContentEditorBinding
    get() = binding.content

  override val subscribeToEvents: Boolean
    get() = true

  private val onBackPressedCallback: OnBackPressedCallback =
      object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
          if (binding.root.isDrawerOpen(GravityCompat.START)) {
            binding.root.closeDrawer(GravityCompat.START)
          } else if (editorBottomSheet?.state != BottomSheetBehavior.STATE_COLLAPSED) {
            editorBottomSheet?.setState(BottomSheetBehavior.STATE_COLLAPSED)
          } else if (binding.swipeReveal.isOpen) {
            binding.swipeReveal.close()
          } else {
            doConfirmProjectClose()
          }
        }
      }

  private var isReinitializingChart = false

  private val memoryUsageListener =
      MemoryUsageWatcher.MemoryUsageListener { memoryUsage ->
        // Check available memory before updating chart
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100

        // Skip if already reinitializing to prevent loops
        if (isReinitializingChart) {
          return@MemoryUsageListener
        }

        memoryUsage.forEachValue { proc ->
          _binding?.memUsageView?.chart?.apply {
            try {
              val datasetIndex = pidToDatasetIdxMap[proc.pid]
              if (datasetIndex == null) {
                log.debug(
                    "Process ${proc.pid} (${proc.pname}) not found in dataset mapping, reinitializing chart"
                )
                // Reinitialize the chart with current processes
                isReinitializingChart = true
                resetMemUsageChart()
                isReinitializingChart = false
                return@forEachValue
              }

              val dataset =
                  (data.getDataSetByIndex(datasetIndex) as LineDataSet?)
                      ?: run {
                        log.warn(
                            "Dataset not found for process: ${proc.pid} (${proc.pname}) at index $datasetIndex, reinitializing chart"
                        )
                        // Reinitialize the chart with current processes
                        isReinitializingChart = true
                        resetMemUsageChart()
                        isReinitializingChart = false
                        return@forEachValue
                      }

              // Optimize memory usage by reusing entries
              val entries = dataset.entries
              val usageHistory = proc.usageHistory
              for (index in entries.indices) {
                if (index < usageHistory.size) {
                  entries[index].y =
                      byte2MemorySize(usageHistory[index], MemoryConstants.MB).toFloat()
                }
              }

              // Use StringBuilder to reduce string allocations
              val labelBuilder = StringBuilder(proc.pname.length + 20)
              labelBuilder.append(proc.pname)
              labelBuilder.append(" - ")
              labelBuilder.append(String.format("%.2fMB", entries.lastOrNull()?.y ?: 0f))
              dataset.label = labelBuilder.toString()

              dataset.notifyDataSetChanged()
              data.notifyDataChanged()
              notifyDataSetChanged()
              invalidate()
            } catch (e: Exception) {
              log.error("Error updating chart for process: ${proc.pname}", e)
            }
          }
        }
      }

  private var isImeVisible = false
  private var contentCardRealHeight: Int? = null
  private val editorSurfaceContainerBackground by lazy { resolveAttr(R.attr.colorSurfaceDim) }
  private val editorLayoutCorners by lazy {
    resources.getDimensionPixelSize(R.dimen.editor_container_corners).toFloat()
  }

  private var optionsMenuInvalidator: Runnable? = null

  companion object {

    @JvmStatic protected val PROC_IDE = "IDE"

    @JvmStatic protected val PROC_GRADLE_TOOLING = "Gradle Tooling"

    @JvmStatic protected val PROC_GRADLE_DAEMON = "Gradle Daemon"

    @JvmStatic protected val log: Logger = LoggerFactory.getLogger(BaseEditorActivity::class.java)

    private const val OPTIONS_MENU_INVALIDATION_DELAY = 150L
    private const val AUTO_SAVE_DELAY_MS = 2000L // 2 seconds

    const val EDITOR_CONTAINER_SCALE_FACTOR = 0.87f
    const val KEY_BOTTOM_SHEET_SHOWN = "editor_bottomSheetShown"
    const val KEY_STRING_EXT_HELPER = "editor_stringExtHelper"
    const val KEY_PROJECT_PATH = "saved_projectPath"
    const val KEY_AUTO_SAVE_ENABLED = "auto_save_enabled"
    private const val BOTTOM_SHEET_HIDE_REASON_EDITOR_SEARCH = "editor_search_overlay"
  }

  protected abstract fun provideCurrentEditor(): CodeEditorView?

  protected abstract fun provideEditorAt(index: Int): CodeEditorView?

  protected abstract fun doOpenFile(file: File, selection: Range?)

  protected abstract fun doDismissSearchProgress()

  protected abstract fun getOpenedFiles(): List<OpenedFile>

  internal abstract fun doConfirmProjectClose()

  /**
   * Save a file. Subclasses can override this method to implement custom saving logic. Default
   * implementation attempts to save using the editor's save functionality.
   */
  protected abstract fun doSaveFile(editor: CodeEditorView): Boolean

  /** Check if auto-save is enabled in preferences */
  protected open fun isAutoSaveEnabled(): Boolean {
    return app.prefManager.getBoolean(KEY_AUTO_SAVE_ENABLED, true)
  }

  /** Check if string extractor helper is enabled */
  private fun isStringExtHelperEnabled(): Boolean {
    return app.prefManager.getBoolean(KEY_STRING_EXT_HELPER)
  }

  /** Auto-save TextWatcher implementation */
  private fun checkForContentChanges() {
    if (!isAutoSaveEnabled() || isDestroying) {
      return
    }

    try {
      val openedFiles = getOpenedFiles()
      val editors = mutableListOf<CodeEditorView>()

      for (i in openedFiles.indices) {
        val editor = provideEditorAt(i) ?: continue
        val file = editor.file ?: continue

        editors.add(editor)

        if (file.exists() && file.canWrite()) {
          val currentContent = editor.editor?.text?.toString() ?: ""
          val currentHash = currentContent.hashCode()
          val lastHash = editorContentHashes[editor] ?: 0

          if (currentHash != lastHash && currentContent.isNotEmpty()) {
            editorContentHashes[editor] = currentHash
            pendingSaveFiles[file] = true
            log.debug("Content changed, marked for auto-save: ${file.absolutePath}")
          }
        }
      }
    } catch (e: Exception) {
      // log.error("Error checking content changes", e)
    }
  }

  /** Start auto-save mechanism */
  private fun startAutoSave() {
    if (autoSaveCoroutineJob?.isActive == true) {
      return
    }

    autoSaveCoroutineJob =
        editorActivityScope.launch {
          while (!isDestroying) {
            try {
              delay(AUTO_SAVE_DELAY_MS)

              if (isAutoSaveEnabled() && !isDestroying) {
                withContext(Dispatchers.Main) { checkForContentChanges() }

                delay(100)

                if (pendingSaveFiles.isNotEmpty()) {
                  performAutoSave()
                }
              }
            } catch (e: Exception) {
              if (!isDestroying) {
                log.error("Error in auto-save coroutine", e)
              }
              break
            }
          }
        }

    log.debug("Auto-save mechanism started")
  }

  /** Stop auto-save mechanism */
  private fun stopAutoSave() {
    autoSaveCoroutineJob?.cancel()
    autoSaveCoroutineJob = null
    log.debug("Auto-save mechanism stopped")
  }

  /** Perform auto-save for all pending files */
  private suspend fun performAutoSave() {
    if (pendingSaveFiles.isEmpty() || isDestroying) {
      return
    }

    autoSaveMutex.withLock {
      val filesToSave = pendingSaveFiles.keys.toList()
      pendingSaveFiles.clear()

      for (file in filesToSave) {
        try {
          val editor = withContext(Dispatchers.Main) { findEditorForFile(file) }

          if (editor != null && !isDestroying) {
            val currentContent =
                withContext(Dispatchers.Main) { editor.editor?.text?.toString() ?: "" }

            if (currentContent.isNotEmpty()) {
              val fileContent =
                  withContext(Dispatchers.IO) {
                    try {
                      if (file.exists()) file.readText() else ""
                    } catch (e: Exception) {
                      ""
                    }
                  }

              if (currentContent != fileContent) {
                val saveSuccess =
                    withContext(Dispatchers.IO) {
                      try {
                        file.writeText(currentContent)
                        true
                      } catch (e: Exception) {
                        log.error("Failed to auto-save file: ${file.absolutePath}", e)
                        false
                      }
                    }

                if (saveSuccess) {
                  withContext(Dispatchers.Main) { showAutoSaveIndicator(file.name) }
                }
              }
            }
          }
        } catch (e: Exception) {
          log.error("Error processing auto-save for file: ${file.absolutePath}", e)
        }
      }
    }
  }

  /** Fallback method to save file directly when SaveFileAction fails */
  private fun fallbackSaveFile(editor: CodeEditorView, content: String) {
    try {
      val file = editor.file
      if (file != null && file.exists() && file.canWrite()) {
        file.writeText(content)
        log.debug("Fallback save successful for: ${file.absolutePath}")
        showAutoSaveIndicator(file.name)
      }
    } catch (e: Exception) {
      log.error("Fallback save failed for: ${editor.file?.absolutePath}", e)
    }
  }

  /** Save all pending files immediately using SaveFileAction */
  protected fun saveAllPendingFiles() {
    if (pendingSaveFiles.isEmpty()) {
      return
    }

    editorActivityScope.launch { performAutoSave() }
  }

  /** Find the editor instance for a given file */
  private fun findEditorForFile(file: File): CodeEditorView? {
    val openedFiles = getOpenedFiles()
    for (i in openedFiles.indices) {
      val editor = provideEditorAt(i)
      if (editor?.file?.absolutePath == file.absolutePath) {
        return editor
      }
    }
    return null
  }

  /** Show a subtle indicator that a file was auto-saved */
  private fun showAutoSaveIndicator(fileName: String) {
    // Update status to show auto-save happened
    val statusText = "Auto-saved: $fileName"
    doSetStatus(statusText, android.view.Gravity.CENTER)

    // Clear the status after a short delay
    ThreadUtils.runOnUiThreadDelayed(
        {
          if (!isDestroying) {
            doSetStatus("", android.view.Gravity.START)
          }
        },
        1500,
    )
  }

  /** Initialize auto-save for an editor (alternative to TextWatcher) */
  protected fun initializeAutoSaveForEditor(editor: CodeEditorView) {
    if (!isAutoSaveEnabled()) {
      return
    }

    // Initialize content hash for change detection
    try {
      val currentContent = editor.editor?.text?.toString() ?: ""
      editorContentHashes[editor] = currentContent.hashCode()
      log.debug("Initialized auto-save for file: ${editor.file?.absolutePath}")
    } catch (e: Exception) {
      log.error("Failed to initialize auto-save for editor", e)
    }
  }

  protected fun cleanupAutoSaveForEditor(editor: CodeEditorView) {
    editorContentHashes.remove(editor)

    // Remove from pending saves if present
    val file = editor.file
    if (file != null) {
      pendingSaveFiles.remove(file)
    }

    log.debug("Cleaned up auto-save for file: ${editor.file?.absolutePath}")
  }

  /** Called when manual save is triggered to sync with auto-save state */
  open fun onManualSave() {
    pendingSaveFiles.clear()

    try {
      val openedFiles = getOpenedFiles()
      for (i in openedFiles.indices) {
        val editor = provideEditorAt(i)
        if (editor != null) {
          val currentContent = editor.editor?.text?.toString() ?: ""
          editorContentHashes[editor] = currentContent.hashCode()
        }
      }
      log.debug("Manual save completed, auto-save state synchronized")
    } catch (e: Exception) {
      log.error("Error updating content hash after manual save", e)
    }
  }

  /** Called when a new editor is created or file is opened */
  protected fun onEditorCreated(editor: CodeEditorView) {
    initializeAutoSaveForEditor(editor)
  }

  /** Called when an editor is closed or destroyed */
  protected fun onEditorDestroyed(editor: CodeEditorView) {
    cleanupAutoSaveForEditor(editor)
  }

  protected fun onFileLoaded(editor: CodeEditorView, file: File) {
      if (file.name == "build.gradle.kts" || file.name == "build.gradle") {
          log.debug("Build file detected: ${file.name}, setting up dependency updater")
          setupDependencyUpdater()
      }
  }
  
  protected open fun preDestroy() {
    _binding = null

    optionsMenuInvalidator?.also { ThreadUtils.getMainHandler().removeCallbacks(it) }

    optionsMenuInvalidator = null

    installationCallback?.destroy()
    installationCallback = null
    SeasonalEffects.setSnowflakeCount(20)

    if (isDestroying) {
      saveAllPendingFiles()
      stopAutoSave()

      // Clear content hashes and pending files
      editorContentHashes.clear()
      editorTextWatchers.clear()
      pendingSaveFiles.clear()

      try {
        val openedFiles = getOpenedFiles()
        for (i in openedFiles.indices) {
          provideEditorAt(i)?.editor?.release()
        }
      } catch (e: Exception) {
        log.warn("Failed to release editors: ${e.message}")
      }
      
      try {
        memoryUsageWatcher.stopWatching(true)
        memoryUsageWatcher.listener = null
      } catch (e: Exception) {
        log.warn("Failed to stop memory monitoring: ${e.message}")
      }
      editorActivityScope.cancelIfActive("Activity is being destroyed")
    }
    
  }

  protected open fun postDestroy() {
    if (isDestroying) {
      Lookup.getDefault().unregisterAll()
      ApiVersionsRegistry.getInstance().clear()
      ResourceTableRegistry.getInstance().clear()
      WidgetTableRegistry.getInstance().clear()
    }
  }

  override fun bindLayout(): View {
    this._binding = ActivityEditorBinding.inflate(layoutInflater)
    this.diagnosticInfoBinding = this.content.diagnosticInfo
    return this.binding.root
  }

  override fun onApplyWindowInsets(insets: WindowInsetsCompat) {
    super.onApplyWindowInsets(insets)
    val height = contentCardRealHeight ?: return
    val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())

    _binding?.content?.bottomSheet?.setImeVisible(imeInsets.bottom > 0)

    val isImeVisible = imeInsets.bottom > 0
    if (this.isImeVisible != isImeVisible) {
      this.isImeVisible = isImeVisible
      onSoftInputChanged()
    }
  }

override fun onApplySystemBarInsets(insets: Insets) {
    super.onApplySystemBarInsets(insets)
    this._binding?.apply {
      drawerSidebar.getFragment<EditorSidebarFragment>().onApplyWindowInsets(insets)

      content.apply {
        // editorAppBarLayout.updatePadding(top = insets.top)  // THIS IS THE PROBLEM!
        editorToolbar.updatePaddingRelative(
            start = editorToolbar.paddingStart + insets.left,
            end = editorToolbar.paddingEnd + insets.right,
        )
      }
    }
}

  @Subscribe(threadMode = MAIN)
  open fun onInstallationResult(event: InstallationResultEvent) {
    val intent = event.intent
    if (isDestroying) {
      return
    }

    val packageName = onResult(this, intent) ?: return

    if (BuildPreferences.launchAppAfterInstall) {
      IntentUtils.launchApp(this, packageName)
      return
    }

    Snackbar.make(content.realContainer, string.msg_action_open_application, Snackbar.LENGTH_LONG)
        .setAction(string.yes) { IntentUtils.launchApp(this, packageName) }
        .show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    WindowCompat.setDecorFitsSystemWindows(window, false)

    this.optionsMenuInvalidator = Runnable { super.invalidateOptionsMenu() }

    registerLanguageServers(this)

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PROJECT_PATH)) {
      IProjectManager.getInstance().openProject(savedInstanceState.getString(KEY_PROJECT_PATH)!!)
    }

    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    lifecycle.addObserver(mLifecycleObserver)

    setSupportActionBar(content.editorToolbar)
    supportActionBar?.setDisplayShowTitleEnabled(false)

    setupDrawers()
    content.tabs.addOnTabSelectedListener(this)

    setupViews()

    setupContainers()
    setupDiagnosticInfo()
    
    ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
        val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

        binding.content.editorToolbar.updatePaddingRelative(
            start = systemBars.left,
            end = systemBars.right,
        )
        windowInsets
    }
    
    
    uiDesignerResultLauncher =
        registerForActivityResult(StartActivityForResult(), this::handleUiDesignerResult)

    setupMemUsageChart()
    watchMemory()

    // Start auto-save mechanism
    startAutoSave()
    
    SeasonalEffects.setSnowflakeCount(0)
    // Initialize lsp setup
    val setup = Setup(this)
    setup.scanProjectForLanguageServers(ProjectManagerImpl.getInstance().projectDir) { isSuccessfullyInstalled ->
      if (isSuccessfullyInstalled) {
        flashSuccess(R.string.msg_installation_succeeded)
        if (!editorViewModel.isInitializing) {
          flashInfo(R.string.msg_reinitializing_project)
          (this as? ProjectHandlerActivity)?.initializeProject()
        }
      }
    }

    if (BuildPreferences.isKtIndexingNotificationEnabled) {
      // Use IndexingBanner to show progress
      val indexingBanner = IndexingBanner(this)
      
      lifecycleScope.launch {
        LogStream.outputFlow.collect { logMessage ->
          if (Index.isIndexing() && !Initialization.isProjectInitializing.value) {
            indexingBanner.updateMessage(logMessage)
          }
        }
      }
      
      lifecycleScope.launch {
        combine(
          Initialization.isProjectInitializing,
          Index.isProjectIndexing
        ) { isInitializing, isIndexing ->
          !isInitializing && isIndexing
        }.collect { shouldShowIndexing ->
          if (shouldShowIndexing) {
            indexingBanner.show()
          } else {
            indexingBanner.hide()
          }
        }
      }
    }
    
    
  }

  private fun setupDependencyUpdater() {
      if (BuildPreferences.isDependenciesUpdaterEnabled) {
        val projectDir = ProjectManagerImpl.getInstance().projectDir
        
        val buildFile = findModuleBuildFile(projectDir)
        val tomlFile = findLibsVersionsToml(projectDir)
        
        if (buildFile == null) {
            log.debug("No module-level build file found for dependency updater")
            return
        }
        
        log.debug("Setting up dependency updater with build file: ${buildFile.absolutePath}")
        
        dependencyUpdater = DependencyUpdaterDialog(
            context = this,
            lifecycleOwner = this,
            buildGradleFile = buildFile,
            libsVersionsTomlFile = tomlFile,
            onDependenciesUpdated = {
                val currentFile = provideCurrentEditor()?.file
                if (currentFile != null && currentFile.absolutePath == buildFile.absolutePath) {
                    provideCurrentEditor()?.editor?.text?.let { text ->
                        val updatedContent = buildFile.readText()
                        text.delete(0, 0, text.lineCount - 1, text.getColumnCount(text.lineCount - 1))
                        text.insert(0, 0, updatedContent)
                    }
                }
            }
        )
        
        log.debug("Calling checkForUpdates()")
        dependencyUpdater.checkForUpdates()
      }
  }
  
  private fun findModuleBuildFile(projectDir: File): File? {
      val possibleNames = listOf("build.gradle.kts", "build.gradle")
      
      projectDir.listFiles()?.forEach { file ->
          if (file.isDirectory && !file.name.startsWith(".")) {
              for (name in possibleNames) {
                  val buildFile = File(file, name)
                  if (buildFile.exists() && buildFile.isFile) {
                      try {
                          val content = buildFile.readText()
                          if (content.contains("android {") && 
                              (content.contains("namespace") || content.contains("applicationId"))) {
                              log.debug("Found module build file: ${buildFile.absolutePath}")
                              return buildFile
                          }
                      } catch (e: Exception) {
                          log.warn("Failed to read build file: ${buildFile.absolutePath}", e)
                      }
                  }
              }
          }
      }
      
      for (name in possibleNames) {
          val buildFile = File(projectDir, name)
          if (buildFile.exists() && buildFile.isFile) {
              log.debug("Found root build file: ${buildFile.absolutePath}")
              return buildFile
          }
      }
      
      log.warn("No module-level build file found in project directory")
      return null
  }
  
  private fun findLibsVersionsToml(projectDir: File): File? {
      val tomlFile = File(projectDir, "gradle/libs.versions.toml")
      if (tomlFile.exists() && tomlFile.isFile) {
          log.debug("Found toml file: ${tomlFile.absolutePath}")
          return tomlFile
      }
      log.debug("No libs.versions.toml file found")
      return null
  }
    
  private fun onSwipeRevealDragProgress(progress: Float) {
    _binding?.apply {
      contentCard.progress = progress
      val insetsTop = systemBarInsets?.top ?: 0
      // content.editorAppBarLayout.updatePadding(top = (insetsTop * (1f - progress)).roundToInt())
      memUsageView.chart.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        topMargin = (insetsTop * progress).roundToInt()
      }
    }
  }

  private fun setupMemUsageChart() {
    binding.memUsageView.chart.apply {
      val colorAccent = resolveAttr(R.attr.colorAccent)

      isDragEnabled = false
      description.isEnabled = false
      xAxis.axisLineColor = colorAccent
      axisRight.axisLineColor = colorAccent

      setPinchZoom(false)
      setBackgroundColor(editorSurfaceContainerBackground)
      setDrawGridBackground(true)
      setScaleEnabled(true)

      axisLeft.isEnabled = false
      axisRight.valueFormatter =
          object : IAxisValueFormatter {
            override fun getFormattedValue(value: Float, axis: AxisBase?): String {
              return "%dMB".format(value.roundToLong())
            }
          }

      // Register chart for memory cleanup
      (application as? com.tom.rv2ide.app.IDEApplication)
          ?.getChartCleanupTask()
          ?.registerChart(this)
    }
  }

  private fun watchMemory() {
    try {
      memoryUsageWatcher.listener = memoryUsageListener
      memoryUsageWatcher.watchProcess(Process.myPid(), PROC_IDE)
    } catch (e: Exception) {
      log.warn("Failed to start memory monitoring: ${e.message}")
    }

    // Watch for gradle tooling processes
    watchGradleProcesses()

    resetMemUsageChart()
  }

  private fun watchGradleProcesses() {
    log.info("Gradle process monitoring disabled")
  }

  protected fun resetMemUsageChart() {
    try {
      val processes = memoryUsageWatcher.getMemoryUsages()

      // Clear existing dataset mapping
      pidToDatasetIdxMap.clear()

      val datasets =
          Array(processes.size) { index ->
            LineDataSet(
                List(MemoryUsageWatcher.MAX_USAGE_ENTRIES) { Entry(it.toFloat(), 0f) },
                processes[index].pname,
            )
          }

      val bgColor = editorSurfaceContainerBackground
      val textColor = resolveAttr(R.attr.colorOnSurface)

      for ((index, proc) in processes.withIndex()) {
        val dataset = datasets[index]
        dataset.color = getMemUsageLineColorFor(proc)
        dataset.setDrawIcons(false)
        dataset.setDrawCircles(false)
        dataset.setDrawCircleHole(false)
        dataset.setDrawValues(false)
        dataset.formLineWidth = 1f
        dataset.formSize = 15f
        dataset.isHighlightEnabled = false

        // Map process ID to dataset index
        pidToDatasetIdxMap[proc.pid] = index
        log.debug("Mapped process ${proc.pid} (${proc.pname}) to dataset index $index")
      }

      binding.memUsageView.chart.setBackgroundColor(bgColor)

      binding.memUsageView.chart.apply {
        data = LineData(*datasets)
        axisRight.textColor = textColor
        axisLeft.textColor = textColor
        legend.textColor = textColor

        data.setValueTextColor(textColor)
        setBackgroundColor(bgColor)
        setGridBackgroundColor(bgColor)
        notifyDataSetChanged()
        invalidate()
      }

      log.info("Memory usage chart initialized with ${processes.size} processes")
    } catch (e: Exception) {
      log.warn("Failed to reset memory usage chart: ${e.message}")
    }
  }

  private fun getMemUsageLineColorFor(proc: MemoryUsageWatcher.ProcessMemoryInfo): Int {
    return when (proc.pname) {
      PROC_IDE -> Color.BLUE
      PROC_GRADLE_TOOLING -> Color.RED
      PROC_GRADLE_DAEMON -> Color.GREEN
      "Gradle" -> Color.YELLOW // Handle Gradle processes
      "GradleDaemon" -> Color.GREEN // Handle Gradle daemon processes
      "GradleJava" -> Color.MAGENTA // Handle Gradle Java processes
      "Java" -> Color.CYAN // Handle Java processes
      else -> Color.GRAY // Default color for unknown processes
    }
  }

  override fun onPause() {
    super.onPause()

    // Save all pending files before pausing
    saveAllPendingFiles()

    try {
      memoryUsageWatcher.listener = null
      memoryUsageWatcher.stopWatching(false)
    } catch (e: Exception) {
      log.warn("Failed to pause memory monitoring: ${e.message}")
    }

    this.isDestroying = isFinishing
    getFileTreeFragment()?.saveTreeState()
  }

  override fun onResume() {
    super.onResume()
    invalidateOptionsMenu()

    try {
      memoryUsageWatcher.listener = memoryUsageListener
      memoryUsageWatcher.startWatching()
    } catch (e: Exception) {
      log.warn("Failed to resume memory monitoring: ${e.message}")
    }

    // Watch for gradle processes
    watchGradleProcesses()

    // Restart auto-save if it was stopped
    if (autoSaveCoroutineJob?.isActive != true) {
      startAutoSave()
    }

    try {
      getFileTreeFragment()?.listProjectFiles()
    } catch (th: Throwable) {
      log.error("Failed to update files list", th)
      flashError(string.msg_failed_list_files)
    }
  }

  override fun onStop() {
    super.onStop()

    saveAllPendingFiles()

    checkIsDestroying()
  }

  override fun onDestroy() {
    checkIsDestroying()
    preDestroy()
    super.onDestroy()
    postDestroy()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    // Save all pending files before saving instance state
    saveAllPendingFiles()

    outState.putString(KEY_PROJECT_PATH, IProjectManager.getInstance().projectDirPath)
    super.onSaveInstanceState(outState)
  }

  override fun invalidateOptionsMenu() {
    val mainHandler = ThreadUtils.getMainHandler()
    optionsMenuInvalidator?.also {
      mainHandler.removeCallbacks(it)
      mainHandler.postDelayed(it, OPTIONS_MENU_INVALIDATION_DELAY)
    }
  }

  override fun onTabSelected(tab: Tab) {
      val position = tab.position
      editorViewModel.displayedFileIndex = position
  
      val editorView = provideEditorAt(position)!!
      editorView.onEditorSelected()
  
      editorViewModel.setCurrentFile(position, editorView.file)
      refreshSymbolInput(editorView)
      invalidateOptionsMenu()
  
      initializeAutoSaveForEditor(editorView)
  }

  override fun onTabUnselected(tab: Tab) {}

  override fun onTabReselected(tab: Tab) {
    createMenu(this, tab.view, EDITOR_FILE_TABS, true).show()
  }

  override fun onGroupClick(group: DiagnosticGroup?) {
    if (group?.file?.exists() == true && FileUtils.isUtf8(group.file)) {
      doOpenFile(group.file, null)
      hideBottomSheet()
    }
  }

  override fun onDiagnosticClick(file: File, diagnostic: DiagnosticItem) {
    doOpenFile(file, diagnostic.range)
    hideBottomSheet()
  }

  open fun handleSearchResults(map: Map<File, List<SearchResult>>?) {
    val results = map ?: emptyMap()
    setSearchResultAdapter(
        SearchListAdapter(
            results,
            { file ->
              doOpenFile(file, null)
              hideBottomSheet()
            },
        ) { match ->
          doOpenFile(match.file, match)
          hideBottomSheet()
        }
    )

    showSearchResults()
    doDismissSearchProgress()
  }

  open fun setSearchResultAdapter(adapter: SearchListAdapter) {
    content.bottomSheet.setSearchResultAdapter(adapter)
  }

  open fun setDiagnosticsAdapter(adapter: DiagnosticsAdapter) {
    content.bottomSheet.setDiagnosticsAdapter(adapter)
  }

  open fun hideBottomSheet() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_COLLAPSED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
      content.bottomSheet.binding.headerContainer.visibility = View.VISIBLE
    }
  }

  open fun showSearchResults() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
    }
  
    val index =
        content.bottomSheet.pagerAdapter.findIndexOfFragmentByClass(
            SearchResultFragment::class.java
        )
  
    if (index >= 0 && index < content.bottomSheet.binding.tabs.tabCount) {
      content.bottomSheet.binding.tabs.getTabAt(index)?.select()
      content.bottomSheet.binding.headerContainer.visibility = View.GONE
    }
  }

  open fun handleDiagnosticsResultVisibility(errorVisible: Boolean) {
    content.bottomSheet.handleDiagnosticsResultVisibility(errorVisible)
  }

  open fun handleSearchResultVisibility(errorVisible: Boolean) {
    content.bottomSheet.handleSearchResultVisibility(errorVisible)
  }

  open fun showFirstBuildNotice() {
    newMaterialDialogBuilder(this)
        .setPositiveButton(android.R.string.ok, null)
        .setTitle(string.title_first_build)
        .setMessage(string.msg_first_build)
        .setCancelable(false)
        .create()
        .show()
  }

  open fun getFileTreeFragment(): FileTreeFragment? {
    if (filesTreeFragment == null) {
      filesTreeFragment =
          supportFragmentManager.findFragmentByTag(FileTreeFragment.TAG) as FileTreeFragment?
    }
    return filesTreeFragment
  }

  private fun hasNativeFiles(projectRoot: File): Boolean {
    val androidMkFile = File(projectRoot, "src/main/jni/Android.mk")
    val cmakeListsFile = File(projectRoot, "src/main/jni/CMakeLists.txt")
    return androidMkFile.exists() || cmakeListsFile.exists()
  }

  private fun isNdkInstalled(): Boolean {
    val ndkBuildFile = File(Environment.ANDROID_HOME, "ndk/28.2.13676358/ndk-build")
    return ndkBuildFile.exists()
  }

  private fun showNdkNotInstalledDialog(context: Context, onDismiss: () -> Unit = {}) {
    MaterialAlertDialogBuilder(context)
        .setTitle(getString(R.string.native_error_title))
        .setMessage(
            "A compatible NDK (version 28.2.13676358) is not installed.\n\n" +
                "Native code features will be disabled for this project.\n\n" +
                "To enable native development, please install NDK version 28.2.13676358 " +
                "open a terminal then run: 'idesetup -y -c -wn'."
        )
        .setPositiveButton(getString(R.string.lsp_ok_button)) { dialog, _ ->
          dialog.dismiss()
          onDismiss()
        }
        .setCancelable(false)
        .show()
  }

  fun doSetStatus(text: CharSequence, @GravityInt gravity: Int) {
    editorViewModel.statusText = text
    editorViewModel.statusGravity = gravity
  }

  fun refreshSymbolInput() {
    provideCurrentEditor()?.also { refreshSymbolInput(it) }
  }

  fun refreshSymbolInput(editor: CodeEditorView) {
    content.bottomSheet.refreshSymbolInput(editor)
  }

  protected fun requestBottomSheetHeaderHide(reason: String) {
    runOnUiThread {
      if (_binding == null) {
        return@runOnUiThread
      }
      val wasAdded = bottomSheetHeaderHideReasons.add(reason)
      if (!wasAdded || bottomSheetHeaderHideReasons.size > 1) {
        return@runOnUiThread
      }
      val bottomSheetBinding = content.bottomSheet.binding
      bottomSheetCardVisibilitySnapshot = bottomSheetBinding.cardView.visibility
      bottomSheetHeaderVisibilitySnapshot = bottomSheetBinding.headerContainer.visibility
      bottomSheetBinding.cardView.visibility = View.INVISIBLE
      bottomSheetBinding.headerContainer.visibility = View.INVISIBLE
    }
  }

  protected fun releaseBottomSheetHeaderHide(reason: String) {
    runOnUiThread {
      if (_binding == null) {
        return@runOnUiThread
      }
      val wasRemoved = bottomSheetHeaderHideReasons.remove(reason)
      if (!wasRemoved || bottomSheetHeaderHideReasons.isNotEmpty()) {
        return@runOnUiThread
      }
      val bottomSheetBinding = content.bottomSheet.binding
      bottomSheetBinding.cardView.visibility = bottomSheetCardVisibilitySnapshot
      bottomSheetBinding.headerContainer.visibility = bottomSheetHeaderVisibilitySnapshot
    }
  }

  fun onEditorSearchVisibilityChanged(isVisible: Boolean) {
    if (isVisible) {
      requestBottomSheetHeaderHide(BOTTOM_SHEET_HIDE_REASON_EDITOR_SEARCH)
    } else {
      releaseBottomSheetHeaderHide(BOTTOM_SHEET_HIDE_REASON_EDITOR_SEARCH)
    }
  }

  private fun checkIsDestroying() {
    if (!isDestroying && isFinishing) {
      isDestroying = true
    }
  }

  private fun handleUiDesignerResult(result: ActivityResult) {
    if (result.resultCode != RESULT_OK || result.data == null) {
      log.warn(
          "UI Designer returned invalid result: resultCode={}, data={}",
          result.resultCode,
          result.data,
      )
      return
    }
    val generated = result.data!!.getStringExtra(UIDesignerActivity.RESULT_GENERATED_XML)
    if (TextUtils.isEmpty(generated)) {
      log.warn("UI Designer returned blank generated XML code")
      return
    }
    val view = provideCurrentEditor()
    val text =
        view?.editor?.text
            ?: run {
              log.warn("No file opened to append UI designer result")
              return
            }
    val endLine = text.lineCount - 1
    text.replace(0, 0, endLine, text.getColumnCount(endLine), generated)
  }

  private fun setupDrawers() {
    val toggle =
        ActionBarDrawerToggle(
            this,
            binding.editorDrawerLayout,
            content.editorToolbar,
            string.app_name,
            string.app_name,
        )

    binding.editorDrawerLayout.addDrawerListener(toggle)
    toggle.syncState()
    binding.apply {
      editorDrawerLayout.apply {
        childId = contentCard.id
        translationBehaviorStart = ContentTranslatingDrawerLayout.TranslationBehavior.FULL
        translationBehaviorEnd = ContentTranslatingDrawerLayout.TranslationBehavior.FULL
        setScrimColor(Color.TRANSPARENT)
      }
    }
  }

  private fun onBuildStatusChanged() {
    log.debug(
        "onBuildStatusChanged: isInitializing: ${editorViewModel.isInitializing}, isBuildInProgress: ${editorViewModel.isBuildInProgress}"
    )
    Initialization.setInitializing(editorViewModel.isInitializing)
    val visible = editorViewModel.isBuildInProgress || editorViewModel.isInitializing
    content.progressIndicator.visibility = if (visible) View.VISIBLE else View.GONE
    invalidateOptionsMenu()
  }

  private fun setupViews() {
    editorViewModel._isBuildInProgress.observe(this) { onBuildStatusChanged() }
    editorViewModel._isInitializing.observe(this) { onBuildStatusChanged() }
    editorViewModel._statusText.observe(this) { content.bottomSheet.setStatus(it.first, it.second) }

    editorViewModel.observeFiles(this) { files ->
      content.apply {
        if (files.isNullOrEmpty()) {
          tabs.visibility = View.GONE
          viewContainer.displayedChild = 1
        } else {
          tabs.visibility = View.VISIBLE
          viewContainer.displayedChild = 0

          // Add auto-save initialization to all open editors
          files.forEachIndexed { index, _ ->
            val editor = provideEditorAt(index)
            if (editor != null) {
              initializeAutoSaveForEditor(editor)
            }
          }
        }
      }

      invalidateOptionsMenu()
    }

    setupNoEditorView()
    setupBottomSheet()
    ViewCompat.setOnApplyWindowInsetsListener(content.bottomSheet) { v, windowInsets ->
      val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

      v.updatePadding(bottom = systemBars.bottom)

      windowInsets
    }
    if (
        !app.prefManager.getBoolean(KEY_BOTTOM_SHEET_SHOWN) &&
            editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED
    ) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
      ThreadUtils.runOnUiThreadDelayed(
          {
            editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
            app.prefManager.putBoolean(KEY_BOTTOM_SHEET_SHOWN, true)
          },
          1500,
      )
    }

    binding.contentCard.progress = 0f
    binding.swipeReveal.dragListener =
        object : SwipeRevealLayout.OnDragListener {
          override fun onDragStateChanged(swipeRevealLayout: SwipeRevealLayout, state: Int) {}

          override fun onDragProgress(swipeRevealLayout: SwipeRevealLayout, progress: Float) {
            onSwipeRevealDragProgress(progress)
          }
        }
  }

  private fun setupNoEditorView() {
    content.noEditorSummary.movementMethod = LinkMovementMethod()
    val filesSpan: ClickableSpan =
        object : ClickableSpan() {
          override fun onClick(widget: View) {
            binding.root.openDrawer(GravityCompat.START)
          }
        }
    val bottomSheetSpan: ClickableSpan =
        object : ClickableSpan() {
          override fun onClick(widget: View) {
            editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
          }
        }
    val sb = SpannableStringBuilder()
    appendClickableSpan(sb, string.msg_drawer_for_files, filesSpan)
    appendClickableSpan(sb, string.msg_swipe_for_output, bottomSheetSpan)
    content.noEditorSummary.text = sb
  }

  private fun appendClickableSpan(
      sb: SpannableStringBuilder,
      @StringRes textRes: Int,
      span: ClickableSpan,
  ) {
    val str = getString(textRes)
    val split = str.split("@@", limit = 3)
    if (split.size != 3) {
      // Not a valid format
      sb.append(str)
      sb.append('\n')
      return
    }
    sb.append(split[0])
    sb.append(split[1], span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    sb.append(split[2])
    sb.append('\n')
  }

  private fun setupBottomSheet() {
    editorBottomSheet = BottomSheetBehavior.from<View>(content.bottomSheet)
    editorBottomSheet?.addBottomSheetCallback(
        object : BottomSheetCallback() {
          override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
              val editor = provideCurrentEditor()
              editor?.editor?.ensureWindowsDismissed()
            }
          }

          override fun onSlide(bottomSheet: View, slideOffset: Float) {
            content.apply {
              val editorScale = 1 - slideOffset * (1 - EDITOR_CONTAINER_SCALE_FACTOR)
              this.bottomSheet.onSlide(slideOffset)
              this.viewContainer.scaleX = editorScale
              this.viewContainer.scaleY = editorScale
            }
          }
        }
    )

    val observer: OnGlobalLayoutListener =
        object : OnGlobalLayoutListener {
          override fun onGlobalLayout() {
            contentCardRealHeight = binding.contentCard.height
            content.also {
              it.realContainer.pivotX = it.realContainer.width.toFloat() / 2f
              it.realContainer.pivotY =
                  (it.realContainer.height.toFloat() / 2f) +
                      (systemBarInsets?.run { bottom - top } ?: 0)
              it.viewContainer.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
          }
        }

    content.apply {
      viewContainer.viewTreeObserver.addOnGlobalLayoutListener(observer)
      bottomSheet.setOffsetAnchor(editorAppBarLayout)
    }
  }

  private fun setupDiagnosticInfo() {
    val gd = GradientDrawable()
    gd.shape = GradientDrawable.RECTANGLE
    gd.setColor(-0xdededf)
    gd.setStroke(1, -0x1)
    gd.cornerRadius = 8f
    diagnosticInfoBinding?.root?.background = gd
    diagnosticInfoBinding?.root?.visibility = View.GONE
  }

  private fun setupContainers() {
    handleDiagnosticsResultVisibility(true)
    handleSearchResultVisibility(true)
  }

  private fun onSoftInputChanged() {
    if (!isDestroying) {
      invalidateOptionsMenu()
      content.bottomSheet.onSoftInputChanged()
    }
  }

  private fun showNeedHelpDialog() {
    val builder = newMaterialDialogBuilder(this)
    builder.setTitle(string.need_help)
    builder.setMessage(string.msg_need_help)
    builder.setPositiveButton(android.R.string.ok, null)
    builder.create().show()
  }

  open fun installationSessionCallback(): SessionCallback {
    return ApkInstallationSessionCallback(this).also { installationCallback = it }
  }
}