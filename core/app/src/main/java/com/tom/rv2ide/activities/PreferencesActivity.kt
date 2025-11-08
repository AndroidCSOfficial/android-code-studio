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
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import com.tom.rv2ide.R
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity
import com.tom.rv2ide.databinding.ActivityPreferencesBinding
import com.tom.rv2ide.fragments.IDEPreferencesFragment
import com.tom.rv2ide.preferences.IDEPreferences as prefs
import com.tom.rv2ide.preferences.addRootPreferences
import kotlin.system.exitProcess

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
