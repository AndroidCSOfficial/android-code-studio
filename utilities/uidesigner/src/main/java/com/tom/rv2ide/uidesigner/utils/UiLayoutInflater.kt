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

package com.tom.rv2ide.uidesigner.utils

// M3 Components
import android.view.ViewGroup
import com.android.aapt.Resources.XmlElement
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.divider.MaterialDivider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.inflater.ILayoutInflater
import com.tom.rv2ide.inflater.IView
import com.tom.rv2ide.inflater.IViewGroup
import com.tom.rv2ide.inflater.internal.LayoutInflaterImpl
import com.tom.rv2ide.inflater.internal.ViewGroupImpl
import com.tom.rv2ide.inflater.internal.ViewImpl
import com.tom.rv2ide.lookup.Lookup
import com.tom.rv2ide.projects.android.AndroidModule
import com.tom.rv2ide.xml.widgets.WidgetTable
import org.slf4j.LoggerFactory

/**
 * Layout inflater implementation for the UI designer with Material Design support. Uses the modular
 * M3 renderer architecture for better component handling.
 *
 * @author Akash Yadav
 * @modification Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class UiLayoutInflater : LayoutInflaterImpl() {

  companion object {
    private val log = LoggerFactory.getLogger(UiLayoutInflater::class.java)

    // Map of Material component qualified names to their factory functions
    private val MATERIAL_COMPONENT_FACTORIES =
        mapOf<String, (ViewGroup) -> android.view.View>(
            "com.google.android.material.button.MaterialButton" to
                { parent ->
                  MaterialButton(parent.context).apply { text = "Button" }
                },
            "com.google.android.material.card.MaterialCardView" to
                { parent ->
                  MaterialCardView(parent.context)
                },
            "com.google.android.material.textview.MaterialTextView" to
                { parent ->
                  MaterialTextView(parent.context).apply { text = "Text" }
                },
            "com.google.android.material.floatingactionbutton.FloatingActionButton" to
                { parent ->
                  FloatingActionButton(parent.context)
                },
            "com.google.android.material.chip.Chip" to
                { parent ->
                  Chip(parent.context).apply { text = "Chip" }
                },
            "com.google.android.material.textfield.TextInputLayout" to
                { parent ->
                  TextInputLayout(parent.context)
                },
            "com.google.android.material.textfield.TextInputEditText" to
                { parent ->
                  TextInputEditText(parent.context)
                },
            "com.google.android.material.appbar.MaterialToolbar" to
                { parent ->
                  MaterialToolbar(parent.context).apply { title = "Toolbar" }
                },
            "com.google.android.material.bottomappbar.BottomAppBar" to
                { parent ->
                  BottomAppBar(parent.context)
                },
            "com.google.android.material.bottomnavigation.BottomNavigationView" to
                { parent ->
                  BottomNavigationView(parent.context)
                },
            "com.google.android.material.navigation.NavigationView" to
                { parent ->
                  NavigationView(parent.context)
                },
            "com.google.android.material.navigationrail.NavigationRailView" to
                { parent ->
                  NavigationRailView(parent.context)
                },
            "com.google.android.material.tabs.TabLayout" to { parent -> TabLayout(parent.context) },
            "com.google.android.material.slider.Slider" to { parent -> Slider(parent.context) },
            "com.google.android.material.switchmaterial.SwitchMaterial" to
                { parent ->
                  SwitchMaterial(parent.context).apply { text = "Switch" }
                },
            "com.google.android.material.checkbox.MaterialCheckBox" to
                { parent ->
                  MaterialCheckBox(parent.context).apply { text = "Checkbox" }
                },
            "com.google.android.material.radiobutton.MaterialRadioButton" to
                { parent ->
                  MaterialRadioButton(parent.context).apply { text = "Radio Button" }
                },
            "com.google.android.material.progressindicator.LinearProgressIndicator" to
                { parent ->
                  LinearProgressIndicator(parent.context)
                },
            "com.google.android.material.progressindicator.CircularProgressIndicator" to
                { parent ->
                  CircularProgressIndicator(parent.context)
                },
            "com.google.android.material.search.SearchBar" to
                { parent ->
                  SearchBar(parent.context).apply { hint = "Search" }
                },
            "com.google.android.material.search.SearchView" to
                { parent ->
                  SearchView(parent.context)
                },
            "com.google.android.material.appbar.AppBarLayout" to
                { parent ->
                  AppBarLayout(parent.context)
                },
            "com.google.android.material.divider.MaterialDivider" to
                { parent ->
                  MaterialDivider(parent.context)
                },
        )
  }

  init {
    this.componentFactory = UiInflaterComponentFactory()
    Lookup.getDefault().update(ILayoutInflater.LOOKUP_KEY, this)
  }

  /**
   * Override onCreateView to intercept Material Design components Uses the modular renderer
   * architecture for component registration
   */
  override fun onCreateView(
      element: XmlElement,
      parent: IViewGroup,
      module: AndroidModule,
      widgets: WidgetTable,
  ): List<IView> {
    log.debug("onCreateView called for element: ${element.name}")

    // Check if this is a registered Material Design component
    if (MATERIAL_COMPONENT_FACTORIES.containsKey(element.name)) {
      log.debug("Intercepting Material Design component: ${element.name}")
      return createMaterialDesignComponent(element, parent, module)
    }

    // Fall back to parent implementation for regular views
    return super.onCreateView(element, parent, module, widgets)
  }

  /** Create Material Design component using the factory pattern */
  private fun createMaterialDesignComponent(
      element: XmlElement,
      parent: IViewGroup,
      module: AndroidModule,
  ): List<IView> {
    val parentView = parent.view as ViewGroup

    // Get factory and create the Material Design view
    val factory =
        MATERIAL_COMPONENT_FACTORIES[element.name]
            ?: throw IllegalStateException("No factory registered for: ${element.name}")

    val materialView = factory(parentView)
    log.debug("Created Material view: ${materialView.javaClass.simpleName}")

    // Wrap in appropriate IView
    val view: ViewImpl =
        if (materialView is ViewGroup) {
          ViewGroupImpl(currentLayoutFile, element.name, materialView)
        } else {
          ViewImpl(currentLayoutFile, element.name, materialView)
        }

    // Add namespace declarations
    addNamespaceDecls(element, view)

    // Apply attributes and add to parent
    applyAttributes(element, view, parent)

    // Handle child views if this is a ViewGroup
    if (element.childCount > 0 && view is IViewGroup) {
      for (child in element.childList) {
        if (child.nodeCase == com.android.aapt.Resources.XmlNode.NodeCase.ELEMENT) {
          onCreateView(element = child.element, parent = view, module = module)
        }
      }
    }

    // Notify inflation listener
    inflationEventListener?.onEvent(com.tom.rv2ide.inflater.events.OnInflateViewEvent(view))

    return listOf(view)
  }

  /** Check if a component type is supported (for external use) */
  fun isMaterialComponentSupported(qualifiedName: String): Boolean {
    return MATERIAL_COMPONENT_FACTORIES.containsKey(qualifiedName)
  }

  /** Get all supported Material component names */
  fun getSupportedMaterialComponents(): Set<String> {
    return MATERIAL_COMPONENT_FACTORIES.keys
  }

  /**
   * Register a new Material component factory dynamically This allows extensions to add new
   * Material components at runtime
   */
  fun registerMaterialComponent(qualifiedName: String, factory: (ViewGroup) -> android.view.View) {
    if (!MATERIAL_COMPONENT_FACTORIES.containsKey(qualifiedName)) {
      (MATERIAL_COMPONENT_FACTORIES as MutableMap)[qualifiedName] = factory
      log.debug("Registered new Material component: $qualifiedName")
    } else {
      log.warn("Material component already registered: $qualifiedName")
    }
  }
}
