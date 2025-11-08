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

package com.tom.rv2ide.fragments.sidebar.utils

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

public fun showErrorDialog(
    ctx: Context,
    title: String,
    message: String,
    negativeBtnTitle: String,
    positiveBtnTitle: String? = null,
    onPositiveClick: (() -> Unit)? = null,
    onNegativeClick: (() -> Unit)? = null,
) {
  val builder = MaterialAlertDialogBuilder(ctx).setTitle(title).setMessage(message)

  positiveBtnTitle?.let { title ->
    builder.setPositiveButton(title) { dialog, _ ->
      onPositiveClick?.invoke()
      dialog.dismiss()
    }
  }

  builder
      .setNegativeButton(negativeBtnTitle) { dialog, _ ->
        onNegativeClick?.invoke()
        dialog.dismiss()
      }
      .show()
}
