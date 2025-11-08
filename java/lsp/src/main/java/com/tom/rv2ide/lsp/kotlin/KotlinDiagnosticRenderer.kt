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

package com.tom.rv2ide.lsp.kotlin

import com.tom.rv2ide.lsp.models.DiagnosticResult
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.widget.CodeEditor
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinDiagnosticRenderer {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinDiagnosticRenderer::class.java)
  }

  fun renderDiagnostics(editor: CodeEditor, result: DiagnosticResult) {
    if (result == DiagnosticResult.NO_UPDATE) return

    try {
      val content = editor.text
      if (content == null || content.isEmpty()) {
        log.warn("Editor content is empty, cannot render diagnostics")
        return
      }

      // Convert diagnostics to regions
      val regions =
          result.diagnostics.mapNotNull { diagnostic ->
            try {
              diagnostic.asDiagnosticRegion(content)
            } catch (e: Exception) {
              log.error("Failed to convert diagnostic to region", e)
              null
            }
          }

      // Apply diagnostics to editor using DiagnosticsContainer
      if (regions.isNotEmpty()) {
        val container = DiagnosticsContainer()
        regions.forEach { region -> container.addDiagnostic(region) }
        editor.diagnostics = container
        log.info("Rendered {} diagnostics in editor", regions.size)
      } else {
        // Clear diagnostics if list is empty
        editor.diagnostics = DiagnosticsContainer()
      }
    } catch (e: Exception) {
      log.error("Failed to render diagnostics", e)
    }
  }

  fun clearDiagnostics(editor: CodeEditor) {
    try {
      editor.diagnostics = DiagnosticsContainer()
    } catch (e: Exception) {
      log.error("Failed to clear diagnostics", e)
    }
  }
}
