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

package com.tom.rv2ide.ideconfigurations.utils

import android.content.Context
import android.widget.Toast
import com.tom.rv2ide.utils.Environment
import java.io.File
import java.io.IOException

/**
 * IDE utility methods
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class IDEUtils {

  /**
   * Checks if the specified NDK version exists and is executable
   *
   * @param context The application context
   * @param ndkVersion The NDK version to check
   * @return true if the NDK exists and is executable, false otherwise
   */
  fun ensureNdkExists(context: Context, ndkVersion: String): Boolean {
    val buildFile = File(Environment.HOME, "android-sdk/ndk/$ndkVersion/ndk-build")
    return buildFile.exists()
  }

  /**
   * Checks if the specified CMake version exists and is executable
   *
   * @param context The application context
   * @param cmakeVersion The CMake version to check
   * @return true if the CMake exists and is executable, false otherwise
   */
  fun ensureCMakeExists(context: Context, ndkVersion: String): Boolean {
    val cmakeExec = File(Environment.HOME, "android-sdk/cmake/$ndkVersion/bin/cmake")
    return cmakeExec.exists()
  }

  /**
   * Helper function to toast the NDK path for debugging/verification
   *
   * @param context The application context
   * @param ndkVersion The NDK version to show path for
   */
  fun toastNdkPath(context: Context, ndkVersion: String) {
    val ndkDir = File(Environment.HOME, "android-sdk/ndk/$ndkVersion")
    val buildFile = File(ndkDir, "ndk-build")

    val message =
        "NDK Directory: " +
            ndkDir.getAbsolutePath() +
            "\nNDK Build File: " +
            buildFile.getAbsolutePath() +
            "\nExists: " +
            buildFile.exists()

    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
  }

  /**
   * Shorter version - just toast the directory path
   *
   * @param context The application context
   * @param ndkVersion The NDK version to show path for
   */
  fun toastNdkDir(context: Context, ndkVersion: String) {
    val ndkDir = File(Environment.HOME, "android-sdk/ndk/$ndkVersion")

    Toast.makeText(context, "NDK Path: " + ndkDir.getAbsolutePath(), Toast.LENGTH_LONG).show()
  }

  fun deleteNdk(context: Context, ndkVersion: String): Boolean {
    val ndkDir = File(Environment.HOME, "android-sdk/ndk/$ndkVersion")

    return if (ndkDir.exists() && ndkDir.isDirectory()) {
      try {
        deleteDirectory(ndkDir)
        true
      } catch (e: IOException) {
        false
      }
    } else {
      false
    }
  }

  fun deleteCMake(context: Context, cmakeVersion: String): Boolean {
    val cmakeDir = File(Environment.HOME, "android-sdk/cmake/$cmakeVersion")

    return if (cmakeDir.exists() && cmakeDir.isDirectory()) {
      try {
        deleteDirectory(cmakeDir)
        true
      } catch (e: IOException) {
        false
      }
    } else {
      false
    }
  }

  @Throws(IOException::class)
  private fun deleteDirectory(directory: File) {
    if (directory.isDirectory()) {
      val files = directory.listFiles()
      if (files != null) {
        for (file in files) {
          deleteDirectory(file)
        }
      }
    }
    if (!directory.delete()) {
      throw IOException("Failed to delete: " + directory.getAbsolutePath())
    }
  }
}
