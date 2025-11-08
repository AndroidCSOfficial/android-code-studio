package com.tom.rv2ide.actions

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorDestinationBuilder
import kotlin.reflect.KClass

interface SidebarActionItem : ActionItem {
  val fragmentClass: KClass<out Fragment>?
  val iconRes: Int

  // val label: String

  fun FragmentNavigatorDestinationBuilder.buildNavigation() {}
}
