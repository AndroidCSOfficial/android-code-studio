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

package com.tom.rv2ide.annotations.uidesigner

/**
 * Indicates that the view (whose view adapter is annotated with [ViewAdapter]
 * [com.tom.rv2ide.annotations.inflater.ViewAdapter] and [IncludeInDesigner]) should be included in
 * the UI Designer and made accessible to the user.
 *
 * @author Akash Yadav
 * @modification [++GOOGLE] Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
annotation class IncludeInDesigner(val group: Group) {

  /** The widget groups that are available in the UI Designer. */
  enum class Group {
    /** Includes the Android platform widgets. */
    WIDGETS,

    /** Includes the Android platform layouts. */
    LAYOUTS,

    /** Includes the Google layouts. */
    GOOGLE,
  }
}
