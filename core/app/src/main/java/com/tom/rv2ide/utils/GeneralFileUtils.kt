/*
 *  This file is part of Android Code Studio.
 *
 *  Android Code Studio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Android Code Studio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with Android Code Studio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.utils

/** * @Author Tom */
import java.io.File

class GeneralFileUtils {
  companion object {
    /**
     * Checks if the file exists. Returns false if file is null. Ensure your project includes the
     * Kotlin standard library dependency.
     */
    fun isFileExists(file: File?): Boolean {
      return file?.exists() ?: false
    }

    /** * List files in a dir */
    fun listFilesInDirectory(directory: File?): List<File> {
      return directory?.listFiles()?.toList() ?: emptyList()
    }

    /** * List directories in a dir */
    fun listDirsInDirectory(directory: File?): List<File> {
      return directory?.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }
  }
}
