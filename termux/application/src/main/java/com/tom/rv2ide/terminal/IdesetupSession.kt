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

package com.tom.rv2ide.terminal

import android.content.Context
import com.tom.rv2ide.managers.ToolsManager
import com.tom.rv2ide.utils.Environment
import com.termux.shared.file.FileUtils
import com.termux.shared.shell.command.ExecutionCommand
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession
import com.termux.terminal.TerminalSession
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream

import com.tom.rv2ide.app.configuration.IDEBuildConfigProvider
import com.tom.rv2ide.app.configuration.CpuArch

/**
 * [TermuxSession] implementation that is used to run the `idesetup` script during automatic
 * installation.
 *
 * @author Akash Yadav
 */
class IdesetupSession private constructor(
  terminalSession: TerminalSession,
  executionCommand: ExecutionCommand,
  termuxSessionClient: TermuxSessionClient?,
  setStdoutOnExit: Boolean,
  private val script: File
) : TermuxSession(
  terminalSession,
  executionCommand,
  termuxSessionClient,
  setStdoutOnExit
) {

  companion object {

    private val log = LoggerFactory.getLogger(IdesetupSession::class.java)

    @JvmStatic
    fun wrap(session: TermuxSession?, script: File) : IdesetupSession? {
      return session?.let { IdesetupSession(it, script) }
    }

    @JvmStatic
    fun createScript(context: Context) : File? {
      // Create temp file with proper executable name
      val tempDir = File(context.filesDir, "temp")
      if (!tempDir.exists()) {
        tempDir.mkdirs()
      }
      val script = File(tempDir, "idesetup")

      // write script contents
      if (!writeIdesetupScript(context, script)) {
        return null
      }

      // make it readable and executable
      FileUtils.setFilePermissions("idesetupScript", script.absolutePath, "rwx")

      return script
    }

    private fun writeIdesetupScript(context: Context, script: File): Boolean {
      return try {
        val cpuArch = IDEBuildConfigProvider.getInstance().cpuArch
        val folderName = when (cpuArch) {
          com.tom.rv2ide.app.configuration.CpuArch.AARCH64 -> "arm64"
          com.tom.rv2ide.app.configuration.CpuArch.ARM -> "arm"
          com.tom.rv2ide.app.configuration.CpuArch.X86_64 -> "x86_64"
          com.tom.rv2ide.app.configuration.CpuArch.X86 -> "x86"
        }
        context.assets.open(ToolsManager.getCommonAsset("${folderName}/idesetup")).use { inputStream ->
          FileOutputStream(script).use { outputStream ->
            inputStream.copyTo(outputStream)
          }
        }
        true
      } catch (e: Exception) {
        log.error("Failed to write idesetup script: {}", e.message, e)
        false
      }
    }

  }

  private constructor(src: TermuxSession, script: File) : this(
    src.terminalSession,
    src.executionCommand,
    src.termuxSessionClient,
    src.isSetStdoutOnExit,
    script
  )

  override fun finish() {
    super.finish()
    // Delete the temporary script file once the session is finished
    val error = FileUtils.deleteFile("idesetupScript", script.absolutePath, true)
    if (error != null) {
      log.error(error.errorLogString)
    }
  }
}