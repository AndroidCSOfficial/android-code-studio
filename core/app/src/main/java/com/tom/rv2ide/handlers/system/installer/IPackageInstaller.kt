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
package com.tom.rv2ide.handlers.system.installer

import android.util.Log
import com.tom.rv2ide.utils.Environment
import java.io.*

/**
 * * Installer for the packages downloaded by acs provider
 * * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class IPackageInstaller {

  companion object {
    private const val TAG = "IPackageInstaller"
  }

  @Throws(IOException::class, IllegalArgumentException::class)
  fun extractXzArchive(xzFilePath: String, outputDir: String) {
    val xzFile = File(xzFilePath)
    val outputDirectory = File(outputDir).canonicalFile

    if (!xzFile.exists()) {
      throw IllegalArgumentException("XZ file does not exist: $xzFilePath")
    }

    if (!xzFile.isFile) {
      throw IllegalArgumentException("Path is not a file: $xzFilePath")
    }

    if (!outputDirectory.exists()) {
      val created = outputDirectory.mkdirs()
      Log.d(TAG, "Created output directory: $created")
    }

    if (!outputDirectory.isDirectory) {
      throw IllegalArgumentException("Output path is not a directory: $outputDir")
    }

    // Check if output directory is writable
    if (!outputDirectory.canWrite()) {
      throw IllegalArgumentException("Output directory is not writable: $outputDir")
    }

    val tarBinary = File(Environment.BIN_DIR, "tar")

    if (!tarBinary.exists()) {
      throw IOException("tar binary not found at: ${tarBinary.absolutePath}")
    }

    if (!tarBinary.canExecute()) {
      throw IOException("tar binary is not executable: ${tarBinary.absolutePath}")
    }

    Log.d(TAG, "Starting tar extraction...")

    val processBuilder =
        ProcessBuilder(
            tarBinary.absolutePath,
            "-xvf", // verbose to see what's happening
            xzFile.absolutePath,
            "-C",
            outputDirectory.absolutePath,
        )

    // Set up environment for the process
    val env = processBuilder.environment()
    env["HOME"] = Environment.HOME.absolutePath
    env["PREFIX"] = Environment.PREFIX.absolutePath
    env["PATH"] = "${Environment.BIN_DIR.absolutePath}:/system/bin"
    env["LD_LIBRARY_PATH"] = Environment.LIB_DIR.absolutePath
    env["TMPDIR"] = Environment.TMP_DIR.absolutePath

    val process = processBuilder.start()

    val outputLines = mutableListOf<String>()
    val errorLines = mutableListOf<String>()

    // Capture stdout
    val outputReader = Thread {
      try {
        process.inputStream.bufferedReader().useLines { lines ->
          lines.forEach { line ->
            outputLines.add(line)
            Log.d(TAG, "tar stdout: $line")
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error reading stdout", e)
      }
    }

    // Capture stderr
    val errorReader = Thread {
      try {
        process.errorStream.bufferedReader().useLines { lines ->
          lines.forEach { line ->
            errorLines.add(line)
            Log.e(TAG, "tar stderr: $line")
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error reading stderr", e)
      }
    }

    outputReader.start()
    errorReader.start()

    val exitCode = process.waitFor()

    outputReader.join(5000) // Wait max 5 seconds
    errorReader.join(5000)

    // List what's in the output directory
    val extractedFiles = outputDirectory.listFiles()
    Log.d(TAG, "Files in output directory: ${extractedFiles?.size ?: 0}")
    extractedFiles?.forEach { file ->
      Log.d(TAG, "  - ${file.name} (${if (file.isDirectory) "dir" else "file"})")
    }

    if (exitCode != 0) {
      val errorMsg =
          "tar extraction failed with exit code: $exitCode\n" +
              "stdout: ${outputLines.joinToString("\n")}\n" +
              "stderr: ${errorLines.joinToString("\n")}"
      Log.e(TAG, errorMsg)
      throw IOException(errorMsg)
    }

    if (extractedFiles.isNullOrEmpty()) {
      throw IOException("Extraction completed but no files found in output directory")
    }

    Log.d(TAG, "Extraction successful!")
  }

  fun extractXzArchiveSimple(xzFilePath: String, outputDir: String): Boolean {
    return try {
      extractXzArchive(xzFilePath, outputDir)
      true
    } catch (e: Exception) {
      Log.e(TAG, "Extraction failed", e)
      false
    }
  }
}
