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
package com.tom.rv2ide.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import com.tom.rv2ide.R
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity
import com.tom.rv2ide.databinding.ActivityPreferencesBinding
import com.tom.rv2ide.fragments.IDEPreferencesFragment
import com.tom.rv2ide.preferences.IDEPreferences as prefs
import com.tom.rv2ide.preferences.addRootPreferences
import com.tom.rv2ide.utils.Environment
import kotlin.system.exitProcess
import android.provider.OpenableColumns
import android.net.Uri
import java.io.File
import android.app.Activity
import android.content.Intent

class PreferencesActivity : EdgeToEdgeIDEActivity() {

  private var _binding: ActivityPreferencesBinding? = null
  private val binding: ActivityPreferencesBinding
    get() = checkNotNull(_binding) { "Activity has been destroyed" }

  private val rootFragment by lazy { IDEPreferencesFragment() }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setSupportActionBar(binding.toolbar)
    supportActionBar!!.setTitle(R.string.ide_preferences)
    supportActionBar!!.setDisplayHomeAsUpEnabled(true)

    binding.toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

    if (savedInstanceState != null) {
      return
    }

    (prefs.children as MutableList?)?.clear()

    prefs.addRootPreferences()

    val args = Bundle()
    args.putParcelableArrayList(IDEPreferencesFragment.EXTRA_CHILDREN, ArrayList(prefs.children))

    rootFragment.arguments = args
    loadFragment(rootFragment)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
      super.onActivityResult(requestCode, resultCode, data)
      
      if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
          data?.data?.let { uri ->
              try {
                  val fontDir = File("${Environment.HOME}/.androidide/ui")
                  if (!fontDir.exists()) {
                      fontDir.mkdirs()
                  }
                  
                  val fileName = getFileName(uri) ?: "custom_font.ttf"
                  val destFile = File(fontDir, fileName)
                  
                  contentResolver.openInputStream(uri)?.use { input ->
                      destFile.outputStream().use { output ->
                          input.copyTo(output)
                      }
                  }
                  
                  Toast.makeText(this, getString(R.string.pref_font_copy_success, fileName), Toast.LENGTH_SHORT).show()
              } catch (e: Exception) {
                  Toast.makeText(this, getString(R.string.pref_font_copy_error, e.message), Toast.LENGTH_LONG).show()
              }
          }
      }
  }
  
  private fun getFileName(uri: Uri): String? {
      var result: String? = null
      if (uri.scheme == "content") {
          contentResolver.query(uri, null, null, null, null)?.use { cursor ->
              if (cursor.moveToFirst()) {
                  val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                  if (columnIndex != -1) {
                      result = cursor.getString(columnIndex)
                  }
              }
          }
      }
      if (result == null) {
          result = uri.path?.let { path ->
              val cut = path.lastIndexOf('/')
              if (cut != -1) path.substring(cut + 1) else path
          }
      }
      return result
  }

  /** Force restart the entire application Call this method when theme changes need to be applied */
  fun forceRestartApp() {
    finishAffinity() // Close all activities

    // Restart the application
    val intent = packageManager.getLaunchIntentForPackage(packageName)
    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)

    // Force exit to ensure clean restart
    exitProcess(0)
  }

  override fun onApplySystemBarInsets(insets: Insets) {
    if (_binding == null) return // Skip if binding not initialized yet

    val toolbar: View = binding.toolbar
    toolbar.setPadding(
        toolbar.paddingLeft + insets.left,
        toolbar.paddingTop,
        toolbar.paddingRight + insets.right,
        toolbar.paddingBottom,
    )

    val fragmentContainer: View = binding.fragmentContainerParent
    fragmentContainer.setPadding(
        fragmentContainer.paddingLeft + insets.left,
        fragmentContainer.paddingTop,
        fragmentContainer.paddingRight + insets.right,
        fragmentContainer.paddingBottom,
    )
  }

  override fun bindLayout(): View {
    _binding = ActivityPreferencesBinding.inflate(layoutInflater)
    return binding.root
  }

  private fun loadFragment(fragment: Fragment) {
    super.loadFragment(fragment, binding.fragmentContainer.id)
  }

  override fun onDestroy() {
    super.onDestroy()
    _binding = null
  }
}
