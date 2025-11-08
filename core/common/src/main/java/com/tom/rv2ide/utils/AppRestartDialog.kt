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

package com.tom.rv2ide.utils

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.resources.R

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class AppRestartDialog private constructor() {

  companion object {
    fun show(context: Context, onCancel: (() -> Unit)? = null) {
      MaterialAlertDialogBuilder(context)
          .setTitle(context.getString(R.string.restart_app_title))
          .setMessage(context.getString(R.string.restart_app_message))
          .setPositiveButton(context.getString(R.string.restart_action)) { dialog, _ ->
            dialog.dismiss()
            restartApp(context)
          }
          .setNegativeButton(context.getString(R.string.cancel_action)) { dialog, _ ->
            dialog.dismiss()
            onCancel?.invoke()
          }
          .setCancelable(false)
          .create()
          .show()
    }

    private fun restartApp(context: Context) {
      val packageManager = context.packageManager
      val intent = packageManager.getLaunchIntentForPackage(context.packageName)
      val componentName = intent?.component
      val mainIntent = Intent.makeRestartActivityTask(componentName)
      context.startActivity(mainIntent)

      // If the context is an Activity, finish it
      if (context is AppCompatActivity) {
        context.finishAffinity()
      }

      // Add fade transition if it's an Activity
      if (context is AppCompatActivity) {
        context.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
      }

      // Force kill the process to ensure clean restart
      Runtime.getRuntime().exit(0)
    }
  }
}
