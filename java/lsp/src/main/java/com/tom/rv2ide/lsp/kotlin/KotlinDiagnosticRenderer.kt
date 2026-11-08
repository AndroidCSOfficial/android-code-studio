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
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticRegion
import io.github.rosemoe.sora.lang.diagnostic.DiagnosticsContainer
import io.github.rosemoe.sora.widget.CodeEditor
import java.util.Collections
import java.util.WeakHashMap
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinDiagnosticRenderer {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinDiagnosticRenderer::class.java)
  }

  private val diagnosticsMap =
      Collections.synchronizedMap(WeakHashMap<CodeEditor, List<DiagnosticRegion>>())
  private val registeredEditors =
      Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<CodeEditor, Boolean>()))

  private fun ensureProviderRegistered(editor: CodeEditor) {
    if (registeredEditors.add(editor)) {
      editor.registerDiagnosticProvider { container ->
        diagnosticsMap[editor]?.let { container.addDiagnostics(it) }
      }
    }
  }

  /**
   * Render diagnostics in the editor.
   *
   * Those changes reflect the changes of nullij editor fork.
   */
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
        // Those changes reflect the changes of nullij editor fork where diagnostics are set via DiagnosticProvider.
        // editor.diagnostics = container
        ensureProviderRegistered(editor)
        diagnosticsMap[editor] = regions
        editor.post { editor.invalidateDiagnostics() }
        log.info("Rendered {} diagnostics in editor", regions.size)
      } else {
        // Clear diagnostics if list is empty
        // Those changes reflect the changes of nullij editor fork where diagnostics are set via DiagnosticProvider.
        // editor.diagnostics = DiagnosticsContainer()
        diagnosticsMap.remove(editor)
        editor.post { editor.invalidateDiagnostics() }
      }
    } catch (e: Exception) {
      log.error("Failed to render diagnostics", e)
    }
  }

  /**
   * Clear diagnostics in the editor.
   *
   * Those changes reflect the changes of nullij editor fork.
   */
  fun clearDiagnostics(editor: CodeEditor) {
    try {
      // Those changes reflect the changes of nullij editor fork where diagnostics are set via DiagnosticProvider.
      // editor.diagnostics = DiagnosticsContainer()
      diagnosticsMap.remove(editor)
      editor.post { editor.invalidateDiagnostics() }
    } catch (e: Exception) {
      log.error("Failed to clear diagnostics", e)
    }
  }
}
