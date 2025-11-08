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

package com.tom.rv2ide.preferences

import android.content.Intent
import com.tom.rv2ide.activities.AboutActivity
import com.tom.rv2ide.app.IDEApplication
import com.tom.rv2ide.resources.R

private const val KEY_CHANGELOG = "idepref_changelog"
private const val KEY_ABOUT = "idepref_about"

val changelog =
    SimpleClickablePreference(
        key = KEY_CHANGELOG,
        title = R.string.pref_changelog,
        summary = R.string.idepref_changelog_summary,
    ) {
      IDEApplication.instance.showChangelog()
      true
    }
val about =
    SimpleClickablePreference(
        key = KEY_ABOUT,
        title = R.string.idepref_about_title,
        summary = R.string.idepref_about_summary,
    ) {
      it.context.startActivity(Intent(it.context, AboutActivity::class.java))
      true
    }
