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

package com.tom.rv2ide.activities.IdeConfigurations

/** * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null */
import android.os.Build

fun getCpuArchitecture(): String {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    // For newer devices, get the primary ABI
    Build.SUPPORTED_ABIS[0]
  } else {
    // Fallback for older devices
    Build.CPU_ABI
  }
}

fun getAllSupportedArchitectures(): List<String> {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
    Build.SUPPORTED_ABIS.toList()
  } else {
    listOf(Build.CPU_ABI, Build.CPU_ABI2).filter { !it.isNullOrEmpty() }
  }
}
