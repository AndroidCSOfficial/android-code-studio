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

package com.tom.rv2ide.actions.editor

import android.content.Context
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.R
import com.tom.rv2ide.actions.ActionData
import com.tom.rv2ide.actions.BaseEditorAction
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

/** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null */
class ExtractAction(private val context: Context, override val order: Int) : BaseEditorAction() {

  companion object {
    private val log = LoggerFactory.getLogger(ExtractAction::class.java)
    private const val MIN_STRING_LENGTH = 4
    private const val MAX_STRING_LENGTH = 100

    private var lastExtraction: ExtractionInfo? = null

    fun getLastExtraction(): ExtractionInfo? = lastExtraction

    fun clearLastExtraction() {
      lastExtraction = null
    }

    fun setLastExtraction(extraction: ExtractionInfo?) {
      lastExtraction = extraction
    }
  }

  data class ProjectModule(val name: String, val path: String, val stringsFile: File)

  data class ExtractionInfo(
      val stringName: String,
      val originalText: String,
      val stringsFile: File,
      val fileType: String,
      val start: Int,
      val end: Int,
  )

  init {
    label = context.getString(R.string.extract)

    icon =
        ContextCompat.getDrawable(context, R.drawable.ic_move_down)?.let {
          tintDrawable(context, it)
        }
  }

  override fun prepare(data: ActionData) {
    super.prepare(data)

    if (!visible) {
      return
    }

    val editor = getEditor(data)
    visible = editor?.isEditable ?: false
    enabled = visible && hasSelectedText(data)
  }

  private fun hasSelectedText(data: ActionData): Boolean {
    val editor = getEditor(data) ?: return false
    val cursor = editor.cursor
    return cursor.isSelected
  }

  override val id: String = "ide.editor.code.text.extract"

  override suspend fun execAction(data: ActionData): Boolean {
    val editor = getEditor(data) ?: return false

    val file =
        data.get(File::class.java)
            ?: run {
              showToast("Could not determine current file")
              return false
            }

    if (!isValidFileType(file)) {
      showToast("File type not supported for string extraction")
      return false
    }

    val cursor = editor.cursor
    if (!cursor.isSelected) {
      showToast("Please select text to extract")
      return false
    }

    val selectedText = editor.text.subSequence(cursor.left, cursor.right).toString()

    val (cleanText, hasQuotes) = processSelectedText(selectedText)

    if (!isExtractableString(cleanText)) {
      showToast("Selected text is not suitable for extraction")
      return false
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val stringName = sanitizeStringName(cleanText)
        val stringsXmlFile = findStringsXmlFile(file)

        if (stringsXmlFile == null) {
          withContext(Dispatchers.Main) {
            showToast("Could not find or create strings.xml file in project")
          }
          return@launch
        }

        val finalStringName = addStringToXml(stringsXmlFile, stringName, cleanText)

        if (finalStringName != null) {
          lastExtraction =
              ExtractionInfo(
                  stringName = finalStringName,
                  originalText = selectedText,
                  stringsFile = stringsXmlFile,
                  fileType = file.extension.lowercase(),
                  start = cursor.left,
                  end = cursor.right,
              )

          withContext(Dispatchers.Main) {
            replaceSelectedText(editor, finalStringName, file, hasQuotes, cursor.left, cursor.right)
          }
        } else {
          withContext(Dispatchers.Main) { showToast("Failed to add string to strings.xml") }
        }
      } catch (e: Exception) {
        log.error("Error extracting string to resource", e)
        withContext(Dispatchers.Main) { showToast("Error: ${e.message}") }
      }
    }

    return true
  }

  private fun processSelectedText(selectedText: String): Pair<String, Boolean> {
    val trimmed = selectedText.trim()
    var hasQuotes = false
    var cleanText = trimmed

    if (
        (trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))
    ) {
      hasQuotes = true
      cleanText = trimmed.substring(1, trimmed.length - 1)
    }

    return Pair(cleanText, hasQuotes)
  }

  private fun isValidFileType(file: File): Boolean {
    val extension = file.extension.lowercase()
    val name = file.name.lowercase()
    val path = file.absolutePath.lowercase()

    return when (extension) {
      "kt" -> !name.endsWith(".gradle.kts")
      "java" -> !name.contains("gradle")
      "xml" -> {
        path.contains("/res/layout/") ||
            path.contains("/res/menu/") ||
            path.contains("/res/xml/") ||
            path.contains("\\res\\layout\\") ||
            path.contains("\\res\\menu\\") ||
            path.contains("\\res\\xml\\") ||
            (path.contains("/res/") && !path.contains("/values/")) ||
            (path.contains("\\res\\") && !path.contains("\\values\\"))
      }
      else -> false
    }
  }

  private suspend fun showModuleSelectionDialog(
      modules: List<ProjectModule>,
      onModuleSelected: (ProjectModule) -> Unit,
  ) {
    withContext(Dispatchers.Main) {
      val moduleNames = modules.map { it.name }.toTypedArray()

      MaterialAlertDialogBuilder(context)
          .setTitle("Select Module")
          .setItems(moduleNames) { dialog, which ->
            val selectedModule = modules[which]
            onModuleSelected(selectedModule)
            dialog.dismiss()
          }
          .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  private suspend fun findStringsXmlFile(currentFile: File): File? {
    var dir = currentFile.parentFile

    while (
        dir != null &&
            !File(dir, "settings.gradle").exists() &&
            !File(dir, "settings.gradle.kts").exists()
    ) {
      dir = dir.parentFile
    }

    if (dir == null) {
      log.debug("Could not find project root with settings.gradle")
      return null
    }

    log.debug("Found project root: ${dir.absolutePath}")

    val modules = scanProjectModules(dir)

    if (modules.isEmpty()) {
      log.debug("No modules found, using app fallback")
      val appDir = File(dir, "app")
      val baseDir = if (appDir.exists()) appDir else dir
      val stringsFile = File(baseDir, "src/main/res/values/strings.xml")

      stringsFile.parentFile?.mkdirs()

      if (!stringsFile.exists()) {
        stringsFile.writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">My App</string>
</resources>"""
        )
      }

      return stringsFile
    }

    if (modules.size == 1) {
      log.debug("Only one module found: ${modules[0].name}")
      val stringsFile = modules[0].stringsFile
      stringsFile.parentFile?.mkdirs()

      if (!stringsFile.exists()) {
        stringsFile.writeText(
            """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${modules[0].name}</string>
</resources>"""
        )
      }

      return stringsFile
    }

    log.debug("Multiple modules found (${modules.size}), showing dialog")
    return suspendCoroutine { continuation ->
      CoroutineScope(Dispatchers.Main).launch {
        showModuleSelectionDialog(modules) { selectedModule ->
          log.debug("User selected module: ${selectedModule.name}")
          val stringsFile = selectedModule.stringsFile
          stringsFile.parentFile?.mkdirs()

          if (!stringsFile.exists()) {
            stringsFile.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">${selectedModule.name}</string>
</resources>"""
            )
          }

          continuation.resume(stringsFile)
        }
      }
    }
  }

  private fun isExtractableString(content: String): Boolean {
    val lengthOk = content.length >= MIN_STRING_LENGTH && content.length <= MAX_STRING_LENGTH
    val notEmpty = content.trim().isNotEmpty()
    val hasLetter = content.any { it.isLetter() }

    val notUrl =
        !content.lowercase().let {
          it.startsWith("http") || it.startsWith("ftp://") || it.startsWith("file://")
        }
    val notNumber = !content.matches(Regex("^\\d+(\\.\\d+)?[a-zA-Z%]*$"))
    val notFilePath = !content.contains("/") && !content.contains("\\") && !content.contains(":")
    val notPackageName = !content.matches(Regex("^[a-z]+\\.[a-z]+(\\.[a-z]+)+$"))
    val notHexColor = !content.matches(Regex("^#[0-9a-fA-F]{3,8}$"))
    val notMimeType = !content.matches(Regex("^[a-z]+/[a-z+]+$"))

    return lengthOk &&
        notEmpty &&
        hasLetter &&
        notUrl &&
        notNumber &&
        notFilePath &&
        notPackageName &&
        notHexColor &&
        notMimeType
  }

  private fun sanitizeStringName(content: String): String {
    return content
        .lowercase()
        .replace(Regex("[^a-z0-9\\s]"), "")
        .trim()
        .replace(Regex("\\s+"), "_")
        .take(30)
        .let { if (it.isEmpty()) "extracted_string" else it }
  }

  private fun scanProjectModules(projectRoot: File): List<ProjectModule> {
    val modules = mutableListOf<ProjectModule>()

    val settingsFile =
        when {
          File(projectRoot, "settings.gradle.kts").exists() ->
              File(projectRoot, "settings.gradle.kts")
          File(projectRoot, "settings.gradle").exists() -> File(projectRoot, "settings.gradle")
          else -> {
            log.debug(
                "No settings.gradle or settings.gradle.kts found in: ${projectRoot.absolutePath}"
            )
            return modules
          }
        }

    log.debug("Reading settings file: ${settingsFile.absolutePath}")

    try {
      val content = settingsFile.readText()
      log.debug("Settings file content:\n$content")

      val includePattern = Regex("""include\s*\(([^)]+)\)""", RegexOption.DOT_MATCHES_ALL)

      val matches = includePattern.findAll(content).toList()
      log.debug("Found ${matches.size} include() statements")

      matches.forEach { match ->
        val includeBlock = match.groupValues[1]
        log.debug("Processing include block: $includeBlock")

        val modulePattern = Regex("""["']:?([a-zA-Z0-9_-]+)["']""")
        val moduleMatches = modulePattern.findAll(includeBlock).toList()
        log.debug("Found ${moduleMatches.size} modules in this block")

        moduleMatches.forEach { moduleMatch ->
          val moduleName = moduleMatch.groupValues[1]
          log.debug("Extracted module name: $moduleName")

          val moduleDir = File(projectRoot, moduleName)
          log.debug(
              "Checking module directory: ${moduleDir.absolutePath}, exists: ${moduleDir.exists()}"
          )

          if (moduleDir.exists() && moduleDir.isDirectory) {
            val stringsFile = File(moduleDir, "src/main/res/values/strings.xml")
            modules.add(
                ProjectModule(
                    name = moduleName,
                    path = moduleDir.absolutePath,
                    stringsFile = stringsFile,
                )
            )
            log.debug("Added module: $moduleName")
          }
        }
      }

      if (modules.isEmpty()) {
        log.debug("No modules found, adding app as fallback")
        val appDir = File(projectRoot, "app")
        if (appDir.exists()) {
          val stringsFile = File(appDir, "src/main/res/values/strings.xml")
          modules.add(
              ProjectModule(name = "app", path = appDir.absolutePath, stringsFile = stringsFile)
          )
        }
      }

      log.debug("Total modules found: ${modules.size} - ${modules.map { it.name }}")
    } catch (e: Exception) {
      log.error("Error scanning project modules", e)
    }

    return modules.sortedBy { it.name }
  }

  private suspend fun addStringToXml(
      stringsFile: File,
      stringName: String,
      stringContent: String,
  ): String? {
    return withContext(Dispatchers.IO) {
      try {
        val content = stringsFile.readText()

        var finalName = stringName
        var counter = 1
        while (content.contains("""name="$finalName"""")) {
          finalName = "${stringName}_$counter"
          counter++
        }

        val escapedContent =
            stringContent
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "\\'")

        val newStringEntry = """    <string name="$finalName">$escapedContent</string>"""

        val updatedContent =
            if (content.contains("</resources>")) {
              val resourcesEnd = content.lastIndexOf("</resources>")
              val beforeEnd = content.substring(0, resourcesEnd)
              val afterEnd = content.substring(resourcesEnd)

              val needsNewline = !beforeEnd.endsWith("\n") && !beforeEnd.endsWith("\r\n")
              val prefix = if (needsNewline) "\n" else ""

              beforeEnd + prefix + newStringEntry + "\n" + afterEnd
            } else {
              """<?xml version="1.0" encoding="utf-8"?>
<resources>
$newStringEntry
</resources>"""
            }

        stringsFile.writeText(updatedContent)
        log.debug("Added string to XML: $finalName = $stringContent")
        finalName
      } catch (e: Exception) {
        log.error("Error adding string to XML", e)
        null
      }
    }
  }

  private fun replaceSelectedText(
      editor: com.tom.rv2ide.editor.ui.IDEEditor,
      stringName: String,
      file: File,
      hasQuotes: Boolean,
      start: Int,
      end: Int,
  ) {
    val replacement =
        when (file.extension.lowercase()) {
          "xml" -> "@string/$stringName"
          "kt",
          "java" -> "getString(R.string.$stringName)"
          else -> "R.string.$stringName"
        }

    try {
      editor.text.replace(start, end, replacement)

      val fileType = file.extension.uppercase()
      showToast("String extracted to R.string.$stringName in $fileType")
      log.debug("Replaced selected text with '$replacement'")
    } catch (e: Exception) {
      log.error("Error replacing selected text", e)
      showToast("Failed to replace selected text")
    }
  }

  private fun showToast(message: String) {
    try {
      android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      log.debug("Could not show toast: ${e.message}")
    }
  }
}
