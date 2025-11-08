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

package com.tom.rv2ide.templates.android

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object TemplateRegistry {
  private val templates = mutableListOf<Template>()

  init {
    // Register all templates here
    register(NoActivity())
    register(BasicActivity())
    register(EmptyActivity())
    register(ComposeEmptyActivity())
    register(BottomNavigationActivity())
    register(NavigationDrawerActivity())
    register(ResponsiveActivity())
    register(GameActivity())
    register(NativeCpp())
    // register(FullscreenActivity())
    // register(TabbedActivity())
    // register(ScrollingActivity())
    // register(EmptyComposeActivity())
    // register(BlankFragment())
    // register(ListFragment())
    // register(CppProject())
  }

  private fun register(template: Template) {
    templates.add(template)
  }

  fun getAllTemplates(): List<Template> = templates.toList()

  fun getTemplateByName(displayName: String): Template? {
    return templates.find { it.displayName == displayName }
  }
}
