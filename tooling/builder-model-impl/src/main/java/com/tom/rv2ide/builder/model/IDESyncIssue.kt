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

package com.tom.rv2ide.builder.model

import com.android.builder.model.v2.ide.SyncIssue

private const val AAPT2_FROM_MAVEN_OVERRIDE_PROPERTY = "android.aapt2FromMavenOverride"

interface IDESyncIssue : SyncIssue {
  companion object {

    const val TYPE_AGP_VERSION_TOO_NEW = -1
  }
}

fun SyncIssue.shouldBeIgnored(): Boolean {
  if (this.type != SyncIssue.TYPE_UNSUPPORTED_PROJECT_OPTION_USE) {
    return false
  }

  return AAPT2_FROM_MAVEN_OVERRIDE_PROPERTY == this.data
}
