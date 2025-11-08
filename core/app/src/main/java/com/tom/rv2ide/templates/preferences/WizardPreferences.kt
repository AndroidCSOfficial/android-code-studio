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

package com.tom.rv2ide.templates.preferences

import android.content.Context
import android.content.SharedPreferences

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object WizardPreferences {
  private const val PREFS_NAME = "atc_wizard_prefs"
  private const val KEY_LAST_SAVE_LOCATION = "last_save_location"
  private const val KEY_RECENT_PROJECTS = "recent_projects"
  private const val MAX_RECENT_PROJECTS = 10

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }

  fun getLastSaveLocation(context: Context): String? {
    return getPrefs(context).getString(KEY_LAST_SAVE_LOCATION, null)
  }

  fun setLastSaveLocation(context: Context, path: String) {
    getPrefs(context).edit().putString(KEY_LAST_SAVE_LOCATION, path).apply()
  }

  // Add a project to recent projects list
  fun addRecentProject(context: Context, projectPath: String) {
    val prefs = getPrefs(context)
    val recentProjects = getRecentProjects(context).toMutableList()

    // Remove if already exists (we'll add it to the front)
    recentProjects.remove(projectPath)

    // Add to front
    recentProjects.add(0, projectPath)

    // Keep only MAX_RECENT_PROJECTS
    val trimmedList = recentProjects.take(MAX_RECENT_PROJECTS)

    // Save as comma-separated string
    prefs.edit().putString(KEY_RECENT_PROJECTS, trimmedList.joinToString(",")).apply()
  }

  // Get list of recent project paths
  fun getRecentProjects(context: Context): List<String> {
    val prefs = getPrefs(context)
    val projectsString = prefs.getString(KEY_RECENT_PROJECTS, "") ?: ""

    if (projectsString.isEmpty()) {
      return emptyList()
    }

    return projectsString
        .split(",")
        .filter { it.isNotBlank() }
        .mapNotNull { path ->
          val file = java.io.File(path)
          // Only return paths that still exist
          if (file.exists() && file.isDirectory) path else null
        }
  }

  // Check if a project is in recent list
  fun isRecentProject(context: Context, projectPath: String): Boolean {
    return getRecentProjects(context).contains(projectPath)
  }

  // Get the position/rank of a project in recent list (0 = most recent)
  fun getRecentProjectRank(context: Context, projectPath: String): Int {
    return getRecentProjects(context).indexOf(projectPath)
  }
}
