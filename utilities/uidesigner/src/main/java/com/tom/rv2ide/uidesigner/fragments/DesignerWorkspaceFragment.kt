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

package com.tom.rv2ide.uidesigner.fragments

// import com.tom.rv2ide.uidesigner.utils.applyBackgroundPreview
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration.get
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.viewModels
import com.blankj.utilcode.util.SizeUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.fragments.BaseFragment
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.internal.AttributeImpl
import com.tom.rv2ide.inflater.internal.LayoutFile
import com.tom.rv2ide.inflater.internal.ViewGroupImpl
import com.tom.rv2ide.inflater.internal.ViewImpl
import com.tom.rv2ide.inflater.utils.endParse
import com.tom.rv2ide.inflater.utils.startParse
import com.tom.rv2ide.uidesigner.R
import com.tom.rv2ide.uidesigner.UIDesignerActivity
import com.tom.rv2ide.uidesigner.databinding.FragmentDesignerWorkspaceBinding
import com.tom.rv2ide.uidesigner.drag.WidgetDragListener
import com.tom.rv2ide.uidesigner.drag.WidgetTouchListener
import com.tom.rv2ide.uidesigner.drawable.UiViewLayeredForeground
import com.tom.rv2ide.uidesigner.models.CommonUiView
import com.tom.rv2ide.uidesigner.models.PlaceholderView
import com.tom.rv2ide.uidesigner.models.RootWorkspaceView
import com.tom.rv2ide.uidesigner.models.UiViewGroup
import com.tom.rv2ide.uidesigner.undo.UndoManager
import com.tom.rv2ide.uidesigner.utils.UiLayoutInflater
import com.tom.rv2ide.uidesigner.utils.applyPreview
import com.tom.rv2ide.uidesigner.utils.bgDesignerView
import com.tom.rv2ide.uidesigner.utils.layeredForeground
import com.tom.rv2ide.uidesigner.viewmodel.M3ComponentState
import com.tom.rv2ide.uidesigner.viewmodel.WorkspaceViewModel
import java.io.File
import org.slf4j.LoggerFactory

/**
 * The fragement that previews the inflated layout.
 *
 * @author Akash Yadav
 */
class DesignerWorkspaceFragment : BaseFragment() {

  private var binding: FragmentDesignerWorkspaceBinding? = null
  internal val viewModel by viewModels<WorkspaceViewModel>(ownerProducer = { requireActivity() })

  private val touchSlop by lazy { get(requireContext()).scaledTouchSlop }

  internal var isInflating = false
  internal val workspaceView by lazy {
    RootWorkspaceView(
        LayoutFile(File(""), ""),
        LinearLayout::class.qualifiedName!!,
        binding!!.workspace,
    )
  }

  val undoManager: UndoManager
    get() = viewModel.undoManager

  internal val placeholder by lazy {
    val view =
        View(requireContext()).apply {
          setBackgroundResource(R.drawable.bg_widget_drag_placeholder)
          layoutParams =
              ViewGroup.LayoutParams(
                  SizeUtils.dp2px(PLACEHOLDER_WIDTH_DP),
                  SizeUtils.dp2px(PLACEHOLDER_HEIGHT_DP),
              )
        }
    PlaceholderView(view)
  }

  private val hierarchyHandler by lazy { WorkspaceViewHierarchyHandler() }
  private val attrHandler by lazy { WorkspaceViewAttrHandler() }

  companion object {

    private val log = LoggerFactory.getLogger(DesignerWorkspaceFragment::class.java)

    const val DRAGGING_WIDGET = "DRAGGING_WIDGET"
    const val DRAGGING_WIDGET_MIME = "androidide/uidesigner_widget"
    const val HIERARCHY_CHANGE_TRANSITION_DURATION = 100L

    private const val PLACEHOLDER_WIDTH_DP = 40f
    private const val PLACEHOLDER_HEIGHT_DP = 20f
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    this.binding = FragmentDesignerWorkspaceBinding.inflate(inflater, container, false)
    hierarchyHandler.init(this)
    attrHandler.init(this)
    return this.binding!!.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewModel._workspaceScreen.observe(viewLifecycleOwner) { binding?.flipper?.displayedChild = it }
    viewModel._errText.observe(viewLifecycleOwner) { binding?.errText?.text = it }

    val inflationHandler = WorkspaceLayoutInflationHandler()
    inflationHandler.init(this)

    val inflater = UiLayoutInflater()
    inflater.inflationEventListener = inflationHandler

    val inflated =
        try {
          startParse(viewModel.file)
          inflater.inflate(viewModel.file, workspaceView).also { viewModel.layoutHasError = false }
        } catch (e: Throwable) {
          log.error("Failed to inflate layout", e)
          viewModel.errText = "${e.message}${e.cause?.message?.let { "\n$it" } ?: ""}"
          viewModel.layoutHasError = true
          emptyList()
        } finally {
          inflationHandler.release()
          inflater.close()
        }

    if (inflated.isEmpty() && !viewModel.layoutHasError) {
      viewModel.errText = getString(R.string.msg_empty_ui_layout)
    }

    // FIXED: Restore M3 components from workspace state AFTER inflation
    val workspaceState = viewModel.getWorkspaceState()
    if (workspaceState?.m3Components?.isNotEmpty() == true) {
      log.debug("Restoring ${workspaceState.m3Components.size} M3 components from workspace state")
      restoreM3ComponentsFromState(workspaceState.m3Components)
    }

    // FIXED: Ensure M3 components are properly styled after inflation
    // This prevents M3 components from appearing invisible after re-entering preview
    if (inflated.isNotEmpty()) {
      setupM3ComponentsAfterInflation()
    }

    binding!!
        .workspace
        .setOnDragListener(WidgetDragListener(workspaceView, this.placeholder, touchSlop))
  }

  override fun onDestroyView() {
    super.onDestroyView()
    this.binding = null
    this.hierarchyHandler.release()
    this.attrHandler.release()

    endParse()
  }

  internal fun setupView(view: IView) {
    if (view is CommonUiView && !view.needSetup) {
      return
    }

    view.registerAttributeChangeListener(attrHandler)
    view.view.setOnTouchListener(
        WidgetTouchListener(view, requireContext()) {
          showViewInfo(it)
          true
        }
    )
    // if (view is ViewImpl) view.applyBackgroundPreview(requireContext(), viewModel.file)
    if (view is ViewImpl) view.applyPreview(requireContext(), null, viewModel.file)

    when (val fg = view.view.foreground) {
      null -> view.view.foreground = bgDesignerView(requireContext())
      is UiViewLayeredForeground ->
          log.warn(
              "Attempt to reset UiViewLayeredForeground on view {} with foreground drawable type {}",
              view.name,
              fg::class.java,
          )

      else -> view.view.foreground = layeredForeground(requireContext(), fg)
    }

    if (view is UiViewGroup && view.canModifyChildViews()) {
      setupViewGroup(view)
    }

    if (view is CommonUiView) {
      view.needSetup = false
    }
  }

  internal fun showViewInfo(view: IView) {
    viewModel.view = view

    val existing = childFragmentManager.findFragmentByTag(ViewInfoSheet.TAG)
    if (existing == null) {
      val viewInfo = ViewInfoSheet()
      viewInfo.show(childFragmentManager, ViewInfoSheet.TAG)
    }
  }

  private fun setupViewGroup(viewGroup: UiViewGroup) {
    viewGroup.view.setOnDragListener(WidgetDragListener(viewGroup, placeholder, touchSlop))
    viewGroup.addOnHierarchyChangeListener(hierarchyHandler)
  }

  fun updateHierarchy() {
    if (workspaceView.childCount > 0) {
      (requireActivity() as UIDesignerActivity).setupHierarchy(workspaceView[0])
    }
  }

  /**
   * FIXED: Setup M3 components after inflation to ensure they are properly styled This prevents M3
   * components from appearing invisible after re-entering preview
   */
  private fun setupM3ComponentsAfterInflation() {
    try {
      log.debug("Setting up M3 components after inflation")

      // Use a post delay to ensure the view is fully inflated and ready
      workspaceView.view.post {
        try {
          // Always recursively setup M3 components in the workspace to ensure proper styling
          setupM3ComponentsRecursively(workspaceView)
          log.debug("M3 components setup completed")
        } catch (e: Exception) {
          log.error("Failed to setup M3 components in post", e)
        }
      }
    } catch (e: Exception) {
      log.error("Failed to setup M3 components after inflation", e)
    }
  }

  /** Recursively setup M3 components in a view hierarchy */
  private fun setupM3ComponentsRecursively(view: IView) {
    log.debug("Processing view: ${view.name} (${view.view::class.simpleName})")

    if (view is ViewImpl) {
      // Apply M3 preview to ensure proper styling
      view.applyPreview(requireContext(), null, viewModel.file)

      // FIXED: Ensure M3 components are properly styled
      if (isM3Component(view.view) || isM3ComponentByName(view)) {
        log.debug("*** FOUND M3 COMPONENT: ${view.name} (${view.view::class.simpleName}) ***")
        log.debug("M3 component attributes count: ${view.attributes.size}")
        view.attributes.forEach { attr ->
          log.debug("  M3 attribute: ${attr.name} = ${attr.value}")
        }

        // Force M3 styling by applying default M3 attributes if none exist
        ensureM3ComponentHasDefaultStyling(view)

        // FIXED: Force visibility by setting minimum dimensions and ensuring the view is visible
        forceM3ComponentVisibility(view)
      }
    }

    // Recursively setup child views
    if (view is UiViewGroup) {
      log.debug("ViewGroup ${view.name} has ${view.childCount} children")
      for (i in 0 until view.childCount) {
        setupM3ComponentsRecursively(view[i])
      }
    }
  }

  /**
   * FIXED: Restore M3 components from saved workspace state This ensures M3 components added during
   * preview are restored when re-entering
   */
  private fun restoreM3ComponentsFromState(m3Components: List<M3ComponentState>) {
    try {
      for (m3Component in m3Components) {
        // Create and add M3 component to workspace
        val restoredView = createM3ComponentFromState(m3Component)
        if (restoredView != null) {
          workspaceView.addChild(restoredView)
          setupView(restoredView)
        }
      }
    } catch (e: Exception) {
      log.error("Failed to restore M3 components from state", e)
    }
  }

  /** FIXED: Create M3 component from saved state with proper view creation */
  private fun createM3ComponentFromState(m3Component: M3ComponentState): IView? {
    return try {
      log.debug(
          "Restoring M3 component: ${m3Component.type} with ${m3Component.attributes.size} attributes"
      )

      // Create the actual M3 component based on type
      val context = requireContext()
      val view =
          when (m3Component.type) {
            "com.google.android.material.button.MaterialButton" -> {
              // Create MaterialButton with proper context
              MaterialButton(context).apply {
                // Set default text if not specified
                if (text.isNullOrEmpty()) {
                  text = "Button"
                }
                // Apply default MaterialButton styling
                // setBackgroundColor(android.graphics.Color.parseColor("#6200EE"))
                // setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
              }
            }
            "com.google.android.material.card.MaterialCardView" -> {
              MaterialCardView(context).apply {
                // setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
                radius = 20f
              }
            }
            "com.google.android.material.textview.MaterialTextView" -> {
              MaterialTextView(context).apply {
                if (text.isNullOrEmpty()) {
                  text = "Text"
                }
                setTextColor(android.graphics.Color.parseColor("#000000"))
              }
            }
            "com.google.android.material.floatingactionbutton.FloatingActionButton" -> {
              FloatingActionButton(context).apply {
                setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#6200EE")
                    )
                )
              }
            }
            "com.google.android.material.chip.Chip" -> {
              Chip(context).apply {
                setChipBackgroundColorResource(android.R.color.white)
                if (text.isNullOrEmpty()) {
                  text = "Chip"
                }
              }
            }
            "com.google.android.material.textfield.TextInputLayout" -> {
              TextInputLayout(context).apply {
                setBoxBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
              }
            }
            else -> {
              log.warn("Unknown M3 component type: ${m3Component.type}")
              return null
            }
          }

      // CRITICAL FIX: Create IView wrapper with the CORRECT view type name
      val layoutFile = com.tom.rv2ide.inflater.internal.LayoutFile(File(""), "")
      val iView =
          when (view) {
            is MaterialButton -> {
              // Use ViewImpl instead of checking the actual view type
              com.tom.rv2ide.inflater.internal.ViewImpl(layoutFile, m3Component.type, view)
            }
            is MaterialCardView -> {
              com.tom.rv2ide.inflater.internal.ViewGroupImpl(layoutFile, m3Component.type, view)
            }
            is MaterialTextView -> {
              com.tom.rv2ide.inflater.internal.ViewImpl(layoutFile, m3Component.type, view)
            }
            is FloatingActionButton -> {
              com.tom.rv2ide.inflater.internal.ViewImpl(layoutFile, m3Component.type, view)
            }
            is Chip -> {
              com.tom.rv2ide.inflater.internal.ViewImpl(layoutFile, m3Component.type, view)
            }
            is TextInputLayout -> {
              com.tom.rv2ide.inflater.internal.ViewGroupImpl(layoutFile, m3Component.type, view)
            }
            else -> return null
          }

      // Set proper layout parameters BEFORE adding attributes
      val layoutParams =
          android.widget.LinearLayout.LayoutParams(
              if (m3Component.size.first > 0) m3Component.size.first
              else android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
              if (m3Component.size.second > 0) m3Component.size.second
              else android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
          )
      view.layoutParams = layoutParams

      // Apply saved attributes
      for ((attrName, attrValue) in m3Component.attributes) {
        try {
          val namespace =
              if (attrName.startsWith("app:")) {
                com.tom.rv2ide.inflater.INamespace.APP
              } else {
                com.tom.rv2ide.inflater.INamespace.ANDROID
              }
          val attr =
              com.tom.rv2ide.inflater.internal.AttributeImpl(
                  namespace = namespace,
                  name = attrName.replace("app:", "").replace("android:", ""),
                  value = attrValue,
              )
          iView.addAttribute(attr, apply = true)
        } catch (e: Exception) {
          log.debug("Failed to apply attribute $attrName: ${e.message}")
        }
      }

      log.debug("Successfully created M3 component: ${m3Component.type}")
      iView
    } catch (e: Exception) {
      log.error("Failed to create M3 component from state", e)
      null
    }
  }

  /** FIXED: Create M3 component state from view with correct type detection */
  private fun createM3ComponentState(view: IView): M3ComponentState? {
    return try {
      val viewRect = view.getViewRect()
      val attributes = mutableMapOf<String, String>()

      // Extract attributes from the view
      for (attr in view.attributes) {
        val attrName =
            if (attr.namespace != null && attr.namespace?.prefix != null) {
              "${attr.namespace?.prefix}:${attr.name}"
            } else {
              attr.name
            }
        attributes[attrName] = attr.value
      }

      // CRITICAL FIX: Determine the correct component type
      val componentType =
          when {
            view.view is MaterialButton || view.name.contains("MaterialButton") ->
                "com.google.android.material.button.MaterialButton"
            view.view is MaterialCardView || view.name.contains("MaterialCardView") ->
                "com.google.android.material.card.MaterialCardView"
            view.view is MaterialTextView || view.name.contains("MaterialTextView") ->
                "com.google.android.material.textview.MaterialTextView"
            view.view is FloatingActionButton || view.name.contains("FloatingActionButton") ->
                "com.google.android.material.floatingactionbutton.FloatingActionButton"
            view.view is Chip || view.name.contains("Chip") ->
                "com.google.android.material.chip.Chip"
            view.view is TextInputLayout || view.name.contains("TextInputLayout") ->
                "com.google.android.material.textfield.TextInputLayout"
            else -> {
              // Fallback to view.name if it's already a proper class name
              if (view.name.startsWith("com.google.android.material")) {
                view.name
              } else {
                log.warn(
                    "Unable to determine M3 component type for view: ${view.name} (${view.view::class.java.name})"
                )
                view.view::class.java.name
              }
            }
          }

      log.debug("Creating M3ComponentState with type: $componentType for view: ${view.name}")

      M3ComponentState(
          id = componentType + "_" + System.currentTimeMillis(),
          type = componentType,
          attributes = attributes,
          position = Pair(viewRect.left.toInt(), viewRect.top.toInt()),
          size = Pair(viewRect.width().toInt(), viewRect.height().toInt()),
      )
    } catch (e: Exception) {
      log.error("Failed to create M3 component state", e)
      null
    }
  }

  /**
   * FIXED: Save M3 component to workspace state when it's added This ensures M3 components are
   * persisted between preview sessions
   */
  fun saveM3ComponentToState(view: IView) {
    try {
      log.debug("Checking if view is M3 component: ${view.name} (${view.view::class.simpleName})")
      if (isM3Component(view.view) || isM3ComponentByName(view)) {
        log.debug("*** SAVING M3 COMPONENT: ${view.name} ***")
        val m3Component = createM3ComponentState(view)
        if (m3Component != null) {
          val currentState = viewModel.getWorkspaceState()
          val currentM3Components = currentState?.m3Components ?: emptyList()
          val updatedM3Components = currentM3Components + m3Component

          viewModel.saveWorkspaceState(updatedM3Components)
          log.debug("Saved M3 component to workspace state: ${m3Component.type}")
          log.debug("Total M3 components in state: ${updatedM3Components.size}")
        } else {
          log.debug("Failed to create M3 component state for ${view.name}")
        }
      } else {
        log.debug("View is not an M3 component: ${view.name}")
      }
    } catch (e: Exception) {
      log.error("Failed to save M3 component to state", e)
    }
  }

  /** FIXED: Check if a view is an M3 component */
  private fun isM3Component(view: android.view.View): Boolean {
    val className = view::class.java.name
    log.debug("Checking M3 component: ${view::class.simpleName} -> $className")

    val isM3 =
        view is MaterialButton ||
            view is MaterialCardView ||
            view is MaterialTextView ||
            view is FloatingActionButton ||
            view is Chip ||
            view is TextInputLayout ||
            className.contains("MaterialButton") ||
            className.contains("MaterialCardView") ||
            className.contains("MaterialTextView") ||
            className.contains("FloatingActionButton") ||
            className.contains("Chip") ||
            className.contains("TextInputLayout")

    log.debug("Is M3 component: $isM3")
    return isM3
  }

  /** FIXED: Check if an IView is an M3 component by checking the view name */
  private fun isM3ComponentByName(view: IView): Boolean {
    val viewName = view.name
    log.debug("Checking M3 component by name: $viewName")

    val isM3 =
        viewName.contains("MaterialButton") ||
            viewName.contains("MaterialCardView") ||
            viewName.contains("MaterialTextView") ||
            viewName.contains("FloatingActionButton") ||
            viewName.contains("Chip") ||
            viewName.contains("TextInputLayout")

    log.debug("Is M3 component by name: $isM3")
    return isM3
  }

  /** FIXED: Ensure M3 component has default styling to make it visible */
  private fun ensureM3ComponentHasDefaultStyling(view: IView) {
    try {
      val androidView = view.view
      val className = androidView::class.java.name
      val viewName = view.name
      log.debug(
          "Applying M3 styling to: ${androidView::class.simpleName} ($className) - View name: $viewName"
      )

      when {
        androidView is MaterialButton ||
            className.contains("MaterialButton") ||
            viewName.contains("MaterialButton") -> {
          // Force button visibility with actual colors
          val button = androidView as? MaterialButton ?: androidView as? android.widget.Button
          if (button != null) {
            if (button.text.isNullOrEmpty()) {
              button.text = "Button"
            }
            // Force background color directly
            // button.setBackgroundColor(android.graphics.Color.parseColor("#6200EE"))
            // button.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
            log.debug("Applied default styling to MaterialButton")
          }
        }
        androidView is MaterialCardView ||
            className.contains("MaterialCardView") ||
            viewName.contains("MaterialCardView") -> {
          // Force card visibility
          val cardView = androidView as? MaterialCardView
          if (cardView != null) {
            // cardView.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            // cardView.radius = 12f
            log.debug("Applied default styling to MaterialCardView")
          }
        }
        androidView is MaterialTextView ||
            className.contains("MaterialTextView") ||
            viewName.contains("MaterialTextView") -> {
          // Force text view visibility
          val textView = androidView as? MaterialTextView ?: androidView as? android.widget.TextView
          if (textView != null) {
            if (textView.text.isNullOrEmpty()) {
              textView.text = "Text"
            }
            textView.setTextColor(android.graphics.Color.parseColor("#000000"))
            log.debug("Applied default styling to MaterialTextView")
          }
        }
        androidView is FloatingActionButton ||
            className.contains("FloatingActionButton") ||
            viewName.contains("FloatingActionButton") -> {
          // Force FAB visibility
          val fab = androidView as? FloatingActionButton
          if (fab != null) {
            fab.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#6200EE")
                )
            )
            log.debug("Applied default styling to FloatingActionButton")
          }
        }
        androidView is Chip || className.contains("Chip") || viewName.contains("Chip") -> {
          // Force chip visibility
          val chip = androidView as? Chip
          if (chip != null) {
            chip.setChipBackgroundColorResource(android.R.color.white)
            if (chip.text.isNullOrEmpty()) {
              chip.text = "Chip"
            }
            log.debug("Applied default styling to Chip")
          }
        }
        androidView is TextInputLayout ||
            className.contains("TextInputLayout") ||
            viewName.contains("TextInputLayout") -> {
          // Force TextInputLayout visibility
          val textInputLayout = androidView as? TextInputLayout
          if (textInputLayout != null) {
            textInputLayout.setBoxBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
            log.debug("Applied default styling to TextInputLayout")
          }
        }
        else -> {
          log.debug("Unknown M3 component type: $className")
        }
      }
    } catch (e: Exception) {
      log.error("Failed to ensure M3 component default styling", e)
    }
  }

  /** FIXED: Force M3 component visibility by ensuring proper dimensions and visibility */
  private fun forceM3ComponentVisibility(view: IView) {
    try {
      val androidView = view.view

      // Ensure the view is visible
      androidView.visibility = android.view.View.VISIBLE

      // Set minimum dimensions if they are too small
      val layoutParams = androidView.layoutParams
      if (layoutParams != null) {
        if (layoutParams.width <= 0) {
          layoutParams.width = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        }
        if (layoutParams.height <= 0) {
          layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        }
        androidView.layoutParams = layoutParams
      }

      // Force a layout pass to ensure the view is properly rendered
      androidView.requestLayout()
      androidView.invalidate()

      log.debug("Forced visibility for M3 component: ${view.name}")
    } catch (e: Exception) {
      log.error("Failed to force M3 component visibility", e)
    }
  }
}
