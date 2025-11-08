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

package com.tom.androidcodestudio.acsprovider.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
object HashUtils {

  /** Calculate SHA-256 hash of a file */
  fun calculateSHA256(file: File): String {
    if (!file.exists()) {
      throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
    }

    val digest = MessageDigest.getInstance("SHA-256")
    FileInputStream(file).use { fis ->
      val buffer = ByteArray(8192)
      var bytesRead: Int
      while (fis.read(buffer).also { bytesRead = it } != -1) {
        digest.update(buffer, 0, bytesRead)
      }
    }

    return digest.digest().joinToString("") { "%02x".format(it) }
  }

  /** Verify SHA-256 checksum */
  fun verifySHA256(file: File, expectedHash: String): Boolean {
    val actualHash = calculateSHA256(file)
    return actualHash.equals(expectedHash, ignoreCase = true)
  }
}
