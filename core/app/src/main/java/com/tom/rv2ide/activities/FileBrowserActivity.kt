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
package com.tom.rv2ide.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.tom.rv2ide.R
import com.tom.rv2ide.app.EdgeToEdgeIDEActivity
import com.tom.rv2ide.viewmodel.MainViewModel
import com.tom.rv2ide.viewmodel.MainViewModel.Companion.SCREEN_MAIN
import com.tom.rv2ide.fragments.FileBrowserFragment

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class FileBrowserActivity : EdgeToEdgeIDEActivity() {

    private val viewModel by viewModels<MainViewModel>()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                loadFileBrowserFragment()
            } else {
                Toast.makeText(this, getString(R.string.file_permission_denied), Toast.LENGTH_SHORT).show()
            }
        }

    private val manageStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    loadFileBrowserFragment()
                } else {
                    Toast.makeText(this, getString(R.string.file_permission_manage_denied), Toast.LENGTH_SHORT).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setScreen(SCREEN_MAIN)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                loadFileBrowserFragment()
            } else {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                manageStoragePermissionLauncher.launch(intent)
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    loadFileBrowserFragment()
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun loadFileBrowserFragment() {
        if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, FileBrowserFragment.newInstance())
                .commit()
        }
    }

    override fun bindLayout(): View {
        return layoutInflater.inflate(R.layout.activity_filebrowser, null)
    }
}