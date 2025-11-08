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

package com.tom.rv2ide.projects.util

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.projects.android.AndroidModule
import com.tom.rv2ide.projects.android.androidAppProjects

/** **For testing purposes only!** */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@VisibleForTesting
fun findAppModule(): AndroidModule? {
  return IProjectManager.getInstance().getWorkspace()?.androidAppProjects()?.firstOrNull {
    it.path == ":app"
  }
}
