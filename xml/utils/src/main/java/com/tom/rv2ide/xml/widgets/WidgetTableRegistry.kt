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

package com.tom.rv2ide.xml.widgets

import com.tom.rv2ide.utils.ServiceLoader
import com.tom.rv2ide.xml.registry.XmlRegistry

/**
 * Information about widgets, layouts and layout params extracted from `widgets.txt` from the
 * Android SDK.
 *
 * @author Akash Yadav
 */
interface WidgetTableRegistry : XmlRegistry<WidgetTable> {

  companion object {

    private var sInstance: WidgetTableRegistry? = null

    /** Get the default instance of [WidgetTableRegistry]. */
    @JvmStatic
    fun getInstance(): WidgetTableRegistry {
      val klass = WidgetTableRegistry::class.java
      return sInstance ?: ServiceLoader.load(klass, klass.classLoader).findFirstOrThrow()
        .also { sInstance = it }
    }
  }
}
