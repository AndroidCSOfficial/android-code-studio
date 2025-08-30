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

// Modified by Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
// ++ auto save

package com.itsaky.androidide.activities.editor

import android.content.Intent
import android.content.pm.PackageInstaller.SessionCallback
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Process
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.activity.viewModels
import androidx.annotation.GravityInt
import androidx.annotation.StringRes
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.collection.MutableIntIntMap
import androidx.core.graphics.Insets
import androidx.core.view.GravityCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.core.view.updatePaddingRelative
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
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.Tab
import com.itsaky.androidide.R
import com.itsaky.androidide.R.string
import com.itsaky.androidide.actions.ActionItem.Location.EDITOR_FILE_TABS
import com.itsaky.androidide.adapters.DiagnosticsAdapter
import com.itsaky.androidide.adapters.SearchListAdapter
import com.itsaky.androidide.app.EdgeToEdgeIDEActivity
import com.itsaky.androidide.databinding.ActivityEditorBinding
import com.itsaky.androidide.databinding.ContentEditorBinding
import com.itsaky.androidide.databinding.LayoutDiagnosticInfoBinding
import com.itsaky.androidide.events.InstallationResultEvent
import com.itsaky.androidide.fragments.SearchResultFragment
import com.itsaky.androidide.fragments.sidebar.EditorSidebarFragment
import com.itsaky.androidide.fragments.sidebar.FileTreeFragment
import com.itsaky.androidide.handlers.EditorActivityLifecyclerObserver
import com.itsaky.androidide.handlers.LspHandler.registerLanguageServers
import com.itsaky.androidide.interfaces.DiagnosticClickListener
import com.itsaky.androidide.lookup.Lookup
import com.itsaky.androidide.lsp.models.DiagnosticItem
import com.itsaky.androidide.models.DiagnosticGroup
import com.itsaky.androidide.models.OpenedFile
import com.itsaky.androidide.models.Range
import com.itsaky.androidide.models.SearchResult
import com.itsaky.androidide.preferences.internal.BuildPreferences
import com.itsaky.androidide.projects.IProjectManager
import com.itsaky.androidide.tasks.cancelIfActive
import com.itsaky.androidide.ui.CodeEditorView
import com.itsaky.androidide.ui.ContentTranslatingDrawerLayout
import com.itsaky.androidide.ui.SwipeRevealLayout
import com.itsaky.androidide.uidesigner.UIDesignerActivity
import com.itsaky.androidide.utils.ActionMenuUtils.createMenu
import com.itsaky.androidide.utils.ApkInstallationSessionCallback
import com.itsaky.androidide.utils.DialogUtils.newMaterialDialogBuilder
import com.itsaky.androidide.utils.InstallationResultHandler.onResult
import com.itsaky.androidide.utils.IntentUtils
import com.itsaky.androidide.utils.MemoryUsageWatcher
import com.itsaky.androidide.utils.SingleTextWatcher
import com.itsaky.androidide.utils.flashError
import com.itsaky.androidide.utils.resolveAttr
import com.itsaky.androidide.viewmodel.EditorViewModel
import com.itsaky.androidide.xml.resources.ResourceTableRegistry
import com.itsaky.androidide.xml.versions.ApiVersionsRegistry
import com.itsaky.androidide.xml.widgets.WidgetTableRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.itsaky.androidide.actions.ActionData
import com.itsaky.androidide.actions.file.SaveFileAction
import android.content.Context 
import kotlinx.coroutines.withContext

import com.itsaky.androidide.utils.Environment
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Base class for EditorActivity which handles most of the view related things.
 *
 * @author Akash Yadav
 */
@Suppress("MemberVisibilityCanBePrivate")
abstract class BaseEditorActivity : EdgeToEdgeIDEActivity(), TabLayout.OnTabSelectedListener,
  DiagnosticClickListener {

  protected val mLifecycleObserver = EditorActivityLifecyclerObserver()
  protected var diagnosticInfoBinding: LayoutDiagnosticInfoBinding? = null
  protected var filesTreeFragment: FileTreeFragment? = null
  protected var editorBottomSheet: BottomSheetBehavior<out View?>? = null
  protected val memoryUsageWatcher = MemoryUsageWatcher()
  protected val pidToDatasetIdxMap = MutableIntIntMap(initialCapacity = 3)

  // Auto-save related properties
  private var autoSaveCoroutineJob: Job? = null
  private val autoSaveMutex = Mutex()
  private val pendingSaveFiles = ConcurrentHashMap<File, Boolean>()
  private val editorTextWatchers = ConcurrentHashMap<CodeEditorView, TextWatcher>()
  private val editorContentHashes = ConcurrentHashMap<CodeEditorView, Int>()
  private var lastAutoSaveCheck = 0L

  var isDestroying = false
    protected set

  /**
   * Editor activity's [CoroutineScope] for executing tasks in the background.
   */
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

  private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
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

  private val memoryUsageListener = MemoryUsageWatcher.MemoryUsageListener { memoryUsage ->
    memoryUsage.forEachValue { proc ->
      _binding?.memUsageView?.chart?.apply {
        val dataset = (data.getDataSetByIndex(pidToDatasetIdxMap[proc.pid]) as LineDataSet?)
          ?: run {
            log.error("No dataset found for process: {}: {}", proc.pid, proc.pname)
            return@forEachValue
          }

        dataset.entries.mapIndexed { index, entry ->
          entry.y = byte2MemorySize(proc.usageHistory[index], MemoryConstants.MB).toFloat()
        }

        dataset.label = "%s - %.2fMB".format(proc.pname, dataset.entries.last().y)
        dataset.notifyDataSetChanged()
        data.notifyDataChanged()
        notifyDataSetChanged()
        invalidate()
      }
    }
  }

  private var isImeVisible = false
  private var contentCardRealHeight: Int? = null
  private val editorSurfaceContainerBackground by lazy {
    resolveAttr(R.attr.colorSurfaceDim)
  }
  private val editorLayoutCorners by lazy {
    resources.getDimensionPixelSize(R.dimen.editor_container_corners).toFloat()
  }

  private var optionsMenuInvalidator: Runnable? = null

  companion object {

    @JvmStatic
    protected val PROC_IDE = "IDE"

    @JvmStatic
    protected val PROC_GRADLE_TOOLING = "Gradle Tooling"

    @JvmStatic
    protected val PROC_GRADLE_DAEMON = "Gradle Daemon"

    @JvmStatic
    protected val log: Logger = LoggerFactory.getLogger(BaseEditorActivity::class.java)

    private const val OPTIONS_MENU_INVALIDATION_DELAY = 150L
    private const val AUTO_SAVE_DELAY_MS = 2000L // 2 seconds

    const val EDITOR_CONTAINER_SCALE_FACTOR = 0.87f
    const val KEY_BOTTOM_SHEET_SHOWN = "editor_bottomSheetShown"
    const val KEY_PROJECT_PATH = "saved_projectPath"
    const val KEY_AUTO_SAVE_ENABLED = "auto_save_enabled"
  }

  protected abstract fun provideCurrentEditor(): CodeEditorView?

  protected abstract fun provideEditorAt(index: Int): CodeEditorView?

  protected abstract fun doOpenFile(file: File, selection: Range?)

  protected abstract fun doDismissSearchProgress()

  protected abstract fun getOpenedFiles(): List<OpenedFile>

  internal abstract fun doConfirmProjectClose()

  /**
   * Save a file. Subclasses can override this method to implement custom saving logic.
   * Default implementation attempts to save using the editor's save functionality.
   */
    protected abstract fun doSaveFile(editor: CodeEditorView): Boolean


  /**
   * Check if auto-save is enabled in preferences
   */
  protected open fun isAutoSaveEnabled(): Boolean {
    return app.prefManager.getBoolean(KEY_AUTO_SAVE_ENABLED, true)
  }

  /**
   * Auto-save TextWatcher implementation
   */
    private fun checkForContentChanges() {
        if (!isAutoSaveEnabled() || isDestroying) {
            return
        }
    
        try {
            val openedFiles = getOpenedFiles()
            for (i in openedFiles.indices) {
                val editor = provideEditorAt(i) ?: continue
                val file = editor.file ?: continue
                
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
            log.error("Error checking content changes", e)
        }
    }
      

  /**
   * Start auto-save mechanism
   */
    private fun startAutoSave() {
        if (autoSaveCoroutineJob?.isActive == true) {
            return
        }
    
        autoSaveCoroutineJob = editorActivityScope.launch {
            while (!isDestroying) {
                try {
                    delay(AUTO_SAVE_DELAY_MS)
                    
                    if (isAutoSaveEnabled() && !isDestroying) {
                        withContext(Dispatchers.Main) {
                            checkForContentChanges()
                        }
                        
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

  /**
   * Stop auto-save mechanism
   */
  private fun stopAutoSave() {
    autoSaveCoroutineJob?.cancel()
    autoSaveCoroutineJob = null
    log.debug("Auto-save mechanism stopped")
  }

  /**
   * Perform auto-save for all pending files
   */
    private suspend fun performAutoSave() {
        if (pendingSaveFiles.isEmpty() || isDestroying) {
            return
        }
    
        autoSaveMutex.withLock {
            val filesToSave = pendingSaveFiles.keys.toList()
            pendingSaveFiles.clear()
            
            for (file in filesToSave) {
                try {
                    val editor = withContext(Dispatchers.Main) {
                        findEditorForFile(file)
                    }
                    
                    if (editor != null && !isDestroying) {
                        val currentContent = withContext(Dispatchers.Main) {
                            editor.editor?.text?.toString() ?: ""
                        }
                        
                        if (currentContent.isNotEmpty()) {
                            val fileContent = withContext(Dispatchers.IO) {
                                try {
                                    if (file.exists()) file.readText() else ""
                                } catch (e: Exception) {
                                    ""
                                }
                            }
                            
                            if (currentContent != fileContent) {
                                val saveSuccess = withContext(Dispatchers.IO) {
                                    try {
                                        file.writeText(currentContent)
                                        true
                                    } catch (e: Exception) {
                                        log.error("Failed to auto-save file: ${file.absolutePath}", e)
                                        false
                                    }
                                }
                                
                                if (saveSuccess) {
                                    withContext(Dispatchers.Main) {
                                        showAutoSaveIndicator(file.name)
                                    }
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

    /**
     * Fallback method to save file directly when SaveFileAction fails
     */
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

    /**
     * Save all pending files immediately using SaveFileAction
     */
    protected fun saveAllPendingFiles() {
        if (pendingSaveFiles.isEmpty()) {
            return
        }
        
        editorActivityScope.launch {
            performAutoSave()
        }
    }

  /**
   * Find the editor instance for a given file
   */
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

  /**
   * Show a subtle indicator that a file was auto-saved
   */
  private fun showAutoSaveIndicator(fileName: String) {
    // Update status to show auto-save happened
    val statusText = "Auto-saved: $fileName"
    doSetStatus(statusText, android.view.Gravity.CENTER)
    
    // Clear the status after a short delay
    ThreadUtils.runOnUiThreadDelayed({
      if (!isDestroying) {
        doSetStatus("", android.view.Gravity.START)
      }
    }, 1500)
  }

  /**
   * Initialize auto-save for an editor (alternative to TextWatcher)
   */
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

  /**
   * Clean up auto-save for an editor
   */
  protected fun cleanupAutoSaveForEditor(editor: CodeEditorView) {
    editorContentHashes.remove(editor)
    
    // Remove from pending saves if present
    val file = editor.file
    if (file != null) {
      pendingSaveFiles.remove(file)
    }
    
    log.debug("Cleaned up auto-save for file: ${editor.file?.absolutePath}")
  }

    /**
     * Called when manual save is triggered to sync with auto-save state
     */
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

  /**
   * Called when a new editor is created or file is opened
   */
  protected fun onEditorCreated(editor: CodeEditorView) {
    initializeAutoSaveForEditor(editor)
  }

  /**
   * Called when an editor is closed or destroyed
   */
  protected fun onEditorDestroyed(editor: CodeEditorView) {
    cleanupAutoSaveForEditor(editor)
  }

  protected open fun preDestroy() {
    _binding = null

    optionsMenuInvalidator?.also {
      ThreadUtils.getMainHandler().removeCallbacks(it)
    }

    optionsMenuInvalidator = null

    installationCallback?.destroy()
    installationCallback = null

    if (isDestroying) {
      // Save all pending files before destroying
      saveAllPendingFiles()
      
      // Stop auto-save mechanism
      stopAutoSave()
      
      // Clear content hashes and pending files
      editorContentHashes.clear()
      editorTextWatchers.clear()
      pendingSaveFiles.clear()
      
      memoryUsageWatcher.stopWatching(true)
      memoryUsageWatcher.listener = null
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
    _binding?.contentCard?.updateLayoutParams<ViewGroup.LayoutParams> {
      this.height = height - imeInsets.bottom
    }

    val isImeVisible = imeInsets.bottom > 0
    if (this.isImeVisible != isImeVisible) {
      this.isImeVisible = isImeVisible
      onSoftInputChanged()
    }
  }

  override fun onApplySystemBarInsets(insets: Insets) {
    super.onApplySystemBarInsets(insets)
    this._binding?.apply {
      drawerSidebar.getFragment<EditorSidebarFragment>()
        .onApplyWindowInsets(insets)

      content.apply {
        editorAppBarLayout.updatePadding(
          top = insets.top
        )
        editorToolbar.updatePaddingRelative(
          start = editorToolbar.paddingStart + insets.left,
          end = editorToolbar.paddingEnd + insets.right
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
      .setAction(string.yes) { IntentUtils.launchApp(this, packageName) }.show()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    this.optionsMenuInvalidator = Runnable { super.invalidateOptionsMenu() }

    registerLanguageServers()

    if (savedInstanceState != null && savedInstanceState.containsKey(KEY_PROJECT_PATH)) {
      IProjectManager.getInstance()
        .openProject(savedInstanceState.getString(KEY_PROJECT_PATH)!!)
    }

    onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    lifecycle.addObserver(mLifecycleObserver)

    setSupportActionBar(content.editorToolbar)

    setupDrawers()
    content.tabs.addOnTabSelectedListener(this)

    setupViews()

    setupContainers()
    setupDiagnosticInfo()

    uiDesignerResultLauncher = registerForActivityResult(
      StartActivityForResult(),
      this::handleUiDesignerResult
    )

    setupMemUsageChart()
    watchMemory()
    
    // Start auto-save mechanism
    startAutoSave()
  }

  private fun onSwipeRevealDragProgress(progress: Float) {
    _binding?.apply {
      contentCard.progress = progress
      val insetsTop = systemBarInsets?.top ?: 0
      content.editorAppBarLayout.updatePadding(
        top = (insetsTop * (1f - progress)).roundToInt()
      )
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
      axisRight.valueFormatter = object :
        IAxisValueFormatter {
        override fun getFormattedValue(value: Float, axis: AxisBase?): String {
          return "%dMB".format(value.roundToLong())
        }
      }
    }
  }

  private fun watchMemory() {
    memoryUsageWatcher.listener = memoryUsageListener
    memoryUsageWatcher.watchProcess(Process.myPid(), PROC_IDE)
    resetMemUsageChart()
  }

  protected fun resetMemUsageChart() {
    val processes = memoryUsageWatcher.getMemoryUsages()
    val datasets = Array(processes.size) { index ->
      LineDataSet(
        List(MemoryUsageWatcher.MAX_USAGE_ENTRIES) { Entry(it.toFloat(), 0f) },
        processes[index].pname
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
      pidToDatasetIdxMap[proc.pid] = index
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
  }

  private fun getMemUsageLineColorFor(proc: MemoryUsageWatcher.ProcessMemoryInfo): Int {
    return when (proc.pname) {
      PROC_IDE -> Color.BLUE
      PROC_GRADLE_TOOLING -> Color.RED
      PROC_GRADLE_DAEMON -> Color.GREEN
      else -> throw IllegalArgumentException("Unknown process: $proc")
    }
  }

  override fun onPause() {
    super.onPause()
    
    // Save all pending files before pausing
    saveAllPendingFiles()
    
    memoryUsageWatcher.listener = null
    memoryUsageWatcher.stopWatching(false)

    this.isDestroying = isFinishing
    getFileTreeFragment()?.saveTreeState()
  }

  override fun onResume() {
    super.onResume()
    invalidateOptionsMenu()

    memoryUsageWatcher.listener = memoryUsageListener
    memoryUsageWatcher.startWatching()
    
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
    
    // Save all pending files before stopping
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
    
    // Initialize auto-save for the newly selected editor
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
    setSearchResultAdapter(SearchListAdapter(results, { file ->
      doOpenFile(file, null)
      hideBottomSheet()
    }) { match ->
      doOpenFile(match.file, match)
      hideBottomSheet()
    })

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
    }
  }

  open fun showSearchResults() {
    if (editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    val index = content.bottomSheet.pagerAdapter.findIndexOfFragmentByClass(
      SearchResultFragment::class.java
    )

    if (index >= 0 && index < content.bottomSheet.binding.tabs.tabCount) {
      content.bottomSheet.binding.tabs.getTabAt(index)?.select()
    }
  }

  open fun handleDiagnosticsResultVisibility(errorVisible: Boolean) {
    content.bottomSheet.handleDiagnosticsResultVisibility(errorVisible)
  }

  open fun handleSearchResultVisibility(errorVisible: Boolean) {
    content.bottomSheet.handleSearchResultVisibility(errorVisible)
  }

  open fun showFirstBuildNotice() {
    newMaterialDialogBuilder(this).setPositiveButton(android.R.string.ok, null)
      .setTitle(string.title_first_build).setMessage(string.msg_first_build).setCancelable(false)
      .create().show()
  }

  open fun getFileTreeFragment(): FileTreeFragment? {
    if (filesTreeFragment == null) {
      filesTreeFragment = supportFragmentManager.findFragmentByTag(
        FileTreeFragment.TAG
      ) as FileTreeFragment?
    }
    return filesTreeFragment
  }

    private fun hasNativeFiles(projectRoot: File): Boolean {
        val androidMkFile = File(projectRoot, "src/main/jni/Android.mk")
        val cmakeListsFile = File(projectRoot, "src/main/jni/CMakeLists.txt")
        return androidMkFile.exists() || cmakeListsFile.exists()
    }
    
    private fun isNdkInstalled(): Boolean {
        val ndkBuildFile = File(Environment.ANDROID_HOME, "ndk/27.1.12297006/ndk-build")
        return ndkBuildFile.exists()
    }
    
    private fun showNdkNotInstalledDialog(context: Context, onDismiss: () -> Unit = {}) {
        MaterialAlertDialogBuilder(context)
            .setTitle("NDK Not Found")
            .setMessage("A compatible NDK (version 27.1.12297006) is not installed.\n\n" +
                       "Native code features will be disabled for this project.\n\n" +
                       "To enable native development, please install NDK version 27.1.12297006 " +
                       "open a terminal then run: 'idesetup -y -c -wn'.")
            .setPositiveButton("OK") { dialog, _ ->
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

  private fun checkIsDestroying() {
    if (!isDestroying && isFinishing) {
      isDestroying = true
    }
  }

  private fun handleUiDesignerResult(result: ActivityResult) {
    if (result.resultCode != RESULT_OK || result.data == null) {
      log.warn(
        "UI Designer returned invalid result: resultCode={}, data={}", result.resultCode,
        result.data
      )
      return
    }
    val generated = result.data!!.getStringExtra(UIDesignerActivity.RESULT_GENERATED_XML)
    if (TextUtils.isEmpty(generated)) {
      log.warn("UI Designer returned blank generated XML code")
      return
    }
    val view = provideCurrentEditor()
    val text = view?.editor?.text ?: run {
      log.warn("No file opened to append UI designer result")
      return
    }
    val endLine = text.lineCount - 1
    text.replace(0, 0, endLine, text.getColumnCount(endLine), generated)
  }

  private fun setupDrawers() {
    val toggle = ActionBarDrawerToggle(
      this, binding.editorDrawerLayout, content.editorToolbar,
      string.app_name, string.app_name
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

    if (!app.prefManager.getBoolean(
        KEY_BOTTOM_SHEET_SHOWN
      ) && editorBottomSheet?.state != BottomSheetBehavior.STATE_EXPANDED
    ) {
      editorBottomSheet?.state = BottomSheetBehavior.STATE_EXPANDED
      ThreadUtils.runOnUiThreadDelayed({
        editorBottomSheet?.state = BottomSheetBehavior.STATE_COLLAPSED
        app.prefManager.putBoolean(KEY_BOTTOM_SHEET_SHOWN, true)
      }, 1500)
    }

    binding.contentCard.progress = 0f
    binding.swipeReveal.dragListener = object : SwipeRevealLayout.OnDragListener {
      override fun onDragStateChanged(swipeRevealLayout: SwipeRevealLayout, state: Int) {}
      override fun onDragProgress(swipeRevealLayout: SwipeRevealLayout, progress: Float) {
        onSwipeRevealDragProgress(progress)
      }
    }
  }

  private fun setupNoEditorView() {
    content.noEditorSummary.movementMethod = LinkMovementMethod()
    val filesSpan: ClickableSpan = object : ClickableSpan() {
      override fun onClick(widget: View) {
        binding.root.openDrawer(GravityCompat.START)
      }
    }
    val bottomSheetSpan: ClickableSpan = object : ClickableSpan() {
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
    editorBottomSheet?.addBottomSheetCallback(object : BottomSheetCallback() {
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
    })

    val observer: OnGlobalLayoutListener = object : OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        contentCardRealHeight = binding.contentCard.height
        content.also {
          it.realContainer.pivotX = it.realContainer.width.toFloat() / 2f
          it.realContainer.pivotY =
            (it.realContainer.height.toFloat() / 2f) + (systemBarInsets?.run { bottom - top }
              ?: 0)
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