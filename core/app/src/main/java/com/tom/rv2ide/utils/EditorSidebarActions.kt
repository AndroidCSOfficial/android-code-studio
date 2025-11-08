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

package com.tom.rv2ide.utils

import android.content.Context
import androidx.annotation.IdRes
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.createGraph
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.get
import androidx.navigation.navOptions
import androidx.recyclerview.widget.LinearLayoutManager
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.ActionItem
import com.tom.rv2ide.actions.ActionsRegistry
import com.tom.rv2ide.actions.SidebarActionItem
import com.tom.rv2ide.actions.internal.DefaultActionsRegistry
import com.tom.rv2ide.actions.sidebar.AIAgentSidebarAction
import com.tom.rv2ide.actions.sidebar.AssetStudioSidebarAction
import com.tom.rv2ide.actions.sidebar.BuildVariantsSidebarAction
import com.tom.rv2ide.actions.sidebar.CloseProjectSidebarAction
import com.tom.rv2ide.actions.sidebar.FileTreeSidebarAction
import com.tom.rv2ide.actions.sidebar.PreferencesSidebarAction
import com.tom.rv2ide.actions.sidebar.SubModuleSidebarAction
import com.tom.rv2ide.actions.sidebar.TerminalSidebarAction
import com.tom.rv2ide.fragments.sidebar.EditorSidebarFragment
import java.lang.ref.WeakReference

/**
 * Sets up the actions that are shown in the
 * [EditorActivityKt][com.tom.rv2ide.activities.editor.EditorActivityKt]'s drawer's sidebar.
 *
 * @author Akash Yadav
 */
internal object EditorSidebarActions {

  @JvmStatic
  fun registerActions(context: Context) {
    val registry = ActionsRegistry.getInstance()
    var order = -1

    @Suppress("KotlinConstantConditions")
    registry.registerAction(FileTreeSidebarAction(context, ++order))
    registry.registerAction(BuildVariantsSidebarAction(context, ++order))
    registry.registerAction(AIAgentSidebarAction(context, ++order))
    registry.registerAction(AssetStudioSidebarAction(context, ++order))
    registry.registerAction(SubModuleSidebarAction(context, ++order))
    registry.registerAction(TerminalSidebarAction(context, ++order))
    registry.registerAction(PreferencesSidebarAction(context, ++order))
    registry.registerAction(CloseProjectSidebarAction(context, ++order))
  }

  @JvmStatic
  fun setup(sidebarFragment: EditorSidebarFragment) {
    val binding = sidebarFragment.getBinding() ?: return
    val navHostFragment =
        sidebarFragment.childFragmentManager.findFragmentById(binding.fragmentContainer.id)
            as? NavHostFragment ?: return
    val controller = navHostFragment.navController
    val context = sidebarFragment.requireContext()
    val navigationRecycler =
        binding.navigation.findViewById<androidx.recyclerview.widget.RecyclerView>(
            com.tom.rv2ide.R.id.navigation_recycler
        )

    val registry = ActionsRegistry.getInstance()
    val actions = registry.getActions(ActionItem.Location.EDITOR_SIDEBAR)
    if (actions.isEmpty()) {
      return
    }

    val data = ActionData()
    data.put(Context::class.java, context)

    val titleRef = WeakReference(binding.title)
    val subtitleRef = WeakReference(binding.subtitle)

    // Sort actions by their order property to maintain registration order
    val sortedActions =
        actions.entries.sortedBy { (_, action) ->
          (action as? SidebarActionItem)?.order ?: Int.MAX_VALUE
        }

    // Create navigation items from sorted actions
    val navigationItems =
        sortedActions.map { (actionId, action) ->
          action as SidebarActionItem

          // Prepare the action to ensure subtitle is updated
          action.prepare(data)

          SidebarNavigationItem(
              id = actionId,
              icon = ContextCompat.getDrawable(context, action.iconRes),
              title = action.label,
              subtitle = action.subtitle,
              isSelected = actionId == FileTreeSidebarAction.ID,
              action = action,
          )
        }

    // Set up RecyclerView adapter
    val adapter =
        SidebarNavigationAdapter(
            onItemClick = { item ->
              val action = item.action

              if (action.fragmentClass == null) {
                (registry as DefaultActionsRegistry).executeAction(action, data)
                return@SidebarNavigationAdapter
              }

              try {
                controller.navigate(
                    action.id,
                    navOptions {
                      launchSingleTop = true
                      restoreState = true
                    },
                )

                titleRef.get()?.text = item.title

                // Update subtitle in header
                val subtitle = item.subtitle
                subtitleRef.get()?.let { subtitleView ->
                  if (!subtitle.isNullOrEmpty()) {
                    subtitleView.text = subtitle
                    subtitleView.visibility = android.view.View.VISIBLE
                  } else {
                    subtitleView.visibility = android.view.View.GONE
                  }
                }
              } catch (e: IllegalArgumentException) {
                // Navigation failed
              }
            },
            onItemLongClick = { item ->
              if (item.action is TerminalSidebarAction) {
                TerminalSidebarAction.startTerminalActivity(data, true)
                true
              } else {
                false
              }
            },
        )

    navigationRecycler.layoutManager =
        LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    navigationRecycler.adapter = adapter
    adapter.submitList(navigationItems)

    // Set up navigation graph with sorted actions
    controller.graph =
        controller.createGraph(startDestination = FileTreeSidebarAction.ID) {
          sortedActions.forEach { (actionId, action) ->
            if (action !is SidebarActionItem) {
              throw IllegalStateException(
                  "Actions registered at location ${ActionItem.Location.EDITOR_SIDEBAR}" +
                      " must implement ${SidebarActionItem::class.java.simpleName}"
              )
            }

            val fragment = action.fragmentClass ?: return@forEach

            val builder =
                FragmentNavigatorDestinationBuilder(
                        this@createGraph.provider[FragmentNavigator::class],
                        actionId,
                        fragment,
                    )
                    .apply { action.apply { buildNavigation() } }

            addDestination(builder.build())
          }
        }

    // Listen for navigation changes
    controller.addOnDestinationChangedListener { _, destination, _ ->
      val matchingItem = navigationItems.find { item -> destination.matchDestination(item.id) }
      matchingItem?.let { item ->
        val updatedItems =
            navigationItems.map { navItem -> navItem.copy(isSelected = navItem.id == item.id) }
        adapter.submitList(updatedItems)
        titleRef.get()?.text = item.title

        // Update subtitle in header
        val subtitle = item.subtitle
        subtitleRef.get()?.let { subtitleView ->
          if (!subtitle.isNullOrEmpty()) {
            subtitleView.text = subtitle
            subtitleView.visibility = android.view.View.VISIBLE
          } else {
            subtitleView.visibility = android.view.View.GONE
          }
        }
      }
    }

    // Set initial selection
    val firstItem = navigationItems.first()
    titleRef.get()?.text = firstItem.title

    // Set initial subtitle
    val firstSubtitle = firstItem.subtitle
    subtitleRef.get()?.let { subtitleView ->
      if (!firstSubtitle.isNullOrEmpty()) {
        subtitleView.text = firstSubtitle
        subtitleView.visibility = android.view.View.VISIBLE
      } else {
        subtitleView.visibility = android.view.View.GONE
      }
    }
  }

  /** Determines whether the given `route` matches the NavDestination. */
  @JvmStatic
  internal fun NavDestination.matchDestination(route: String): Boolean =
      hierarchy.any { it.route == route }

  @JvmStatic
  internal fun NavDestination.matchDestination(@IdRes destId: Int): Boolean =
      hierarchy.any { it.id == destId }
}
