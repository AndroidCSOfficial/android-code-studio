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

package com.tom.rv2ide.viewmodel

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.google.gson.Gson

/**
 * ViewModel for TerminalFragment. Manages terminal session state and persistence.
 *
 * @author Tom
 */
class TerminalFragmentViewModel : ViewModel() {

  private var sharedPreferences: SharedPreferences? = null
  private val gson = Gson()

  companion object {
    private const val PREFS_NAME = "terminal_sessions"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_CURRENT_INDEX = "current_index"
  }

  /** Initialize with context for SharedPreferences */
  fun initialize(context: Context) {
    sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  }
}
