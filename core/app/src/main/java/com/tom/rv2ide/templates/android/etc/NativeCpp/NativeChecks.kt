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

package com.tom.rv2ide.templates.android.etc.NativeCpp

import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.GeneralFileUtils
import java.io.File
import java.util.concurrent.TimeUnit

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object Check {

  /** Returns all installed NDK versions sorted from highest to lowest. */
  fun getAllNdkVersions(): List<String> {
    val ndkDir = File(Environment.HOME, "android-sdk/ndk")
    if (!ndkDir.exists() || !ndkDir.isDirectory) return emptyList()

    return GeneralFileUtils.listDirsInDirectory(ndkDir)
        .map { it.name to versionStringToList(it.name) }
        .filter { it.second.isNotEmpty() }
        .sortedWith(
            Comparator { a, b ->
              compareVersionLists(b.second, a.second) // reversed for descending
            }
        )
        .map { it.first }
  }

  /**
   * Returns the highest installed NDK version as a string, e.g. "27.3.13750724", or null if no NDK
   * versions are found.
   */
  fun getHighestNdkVersion(): String? = getAllNdkVersions().firstOrNull()

  /**
   * Returns the highest installed CMake version as a string, e.g. "3.22.1", or null if no CMake
   * versions are found.
   */
  fun getHighestCMakeVersion(): String? = getAllCMakeVersions().firstOrNull()

  /** Checks if at least one NDK is installed. */
  fun isAtLeastOneInstalled(): Boolean = getAllNdkVersions().isNotEmpty()

  /** Validates a specific NDK version by running clang -v. */
  fun validateNdkVersion(version: String): Boolean {
    val ndkPath = File(Environment.HOME, "android-sdk/ndk/$version")
    val clangPath = findClangExecutable(ndkPath) ?: return false

    return try {
      val process = ProcessBuilder(clangPath.absolutePath, "-v").redirectErrorStream(true).start()

      if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroy()
        false
      } else {
        val output = process.inputStream.bufferedReader().readText()
        process.exitValue() == 0 || output.contains("clang version")
      }
    } catch (e: Exception) {
      false
    }
  }

  /** Finds all CMake installations in android-sdk/cmake directory. */
  fun getAllCMakeVersions(): List<String> {
    val cmakeDir = File(Environment.HOME, "android-sdk/cmake")
    if (!cmakeDir.exists() || !cmakeDir.isDirectory) return emptyList()

    return GeneralFileUtils.listDirsInDirectory(cmakeDir)
        .map { it.name to versionStringToList(it.name) }
        .filter { it.second.isNotEmpty() }
        .sortedWith(
            Comparator { a, b ->
              compareVersionLists(b.second, a.second) // reversed for descending
            }
        )
        .map { it.first }
  }

  /** Validates a specific CMake version by running cmake --version. */
  fun validateCMakeVersion(version: String): String? {
    val cmakePath = findCMakeExecutable(version) ?: return null

    return try {
      val process =
          ProcessBuilder(cmakePath.absolutePath, "--version").redirectErrorStream(true).start()

      if (!process.waitFor(10, TimeUnit.SECONDS)) {
        process.destroy()
        null
      } else {
        if (process.exitValue() == 0) cmakePath.absolutePath else null
      }
    } catch (e: Exception) {
      null
    }
  }

  /** Finds the cmake binary for a given version. */
  private fun findCMakeExecutable(version: String): File? {
    val cmakeDir = File(Environment.HOME, "android-sdk/cmake/$version/bin")
    if (!cmakeDir.exists()) return null

    val cmakeFile = File(cmakeDir, "cmake")
    return if (cmakeFile.exists() && cmakeFile.canExecute()) cmakeFile else null
  }

  /**
   * Converts a version string like "27.3.13750724" into a list of integers [27, 3, 13750724] for
   * proper numerical comparison.
   */
  private fun versionStringToList(version: String): List<Int> =
      version.split('.').mapNotNull { it.toIntOrNull() }

  /**
   * Compares two version lists element by element. Returns: negative if v1 < v2, positive if v1 >
   * v2, zero if equal
   */
  private fun compareVersionLists(v1: List<Int>, v2: List<Int>): Int {
    val minSize = minOf(v1.size, v2.size)
    for (i in 0 until minSize) {
      val cmp = v1[i].compareTo(v2[i])
      if (cmp != 0) return cmp
    }
    return v1.size.compareTo(v2.size)
  }

  /** Finds the clang binary inside the NDK directory. */
  private fun findClangExecutable(ndkPath: File): File? {
    val prebuiltDir = File(ndkPath, "toolchains/llvm/prebuilt")
    if (!prebuiltDir.exists()) return null

    prebuiltDir.walkTopDown().forEach { file ->
      if (file.isFile && file.name == "clang") return file
    }
    return null
  }
}
