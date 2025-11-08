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

package com.tom.rv2ide.templates.impl.noActivity

import com.tom.rv2ide.templates.base.modules.android.defaultAppModule
import com.tom.rv2ide.templates.impl.R
import com.tom.rv2ide.templates.impl.base.createRecipe
import com.tom.rv2ide.templates.impl.base.emptyThemesAndColors
import com.tom.rv2ide.templates.impl.baseProjectImpl

fun noActivityProjectTemplate() = baseProjectImpl {
  templateName = R.string.template_no_activity
  thumb = R.drawable.no_activity
  defaultAppModule { recipe = createRecipe { res { emptyThemesAndColors(actionBar = true) } } }
}
