package com.tom.rv2ide.editor.lsp

import com.tom.rv2ide.lsp.models.DiagnosticItem
import io.github.rosemoe.sora.widget.CodeEditor

/**
 * * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null Handles diagnostic clicks
 *   in the code editor Shows quick fix options when user clicks on warnings/errors
 */
class EditorDiagnosticHandler(private val editor: CodeEditor) {

  private val diagnosticsByLine = mutableMapOf<Int, List<DiagnosticItem>>()

  /** Updates diagnostics for the editor */
  fun updateDiagnostics(diagnostics: List<DiagnosticItem>) {
    diagnosticsByLine.clear()

    diagnostics.forEach { diagnostic ->
      val line = diagnostic.range.start.line
      diagnosticsByLine.getOrPut(line) { mutableListOf() }.let { it as MutableList }.add(diagnostic)
    }
  }

  /** Gets diagnostics at a specific line */
  fun getDiagnosticsAtLine(line: Int): List<DiagnosticItem> {
    return diagnosticsByLine[line] ?: emptyList()
  }

  /** Gets diagnostic at a specific position */
  fun getDiagnosticAt(line: Int, column: Int): DiagnosticItem? {
    return diagnosticsByLine[line]?.firstOrNull { diagnostic ->
      val range = diagnostic.range
      line == range.start.line && column >= range.start.column && column <= range.end.column
    }
  }

  /** Checks if there's a quick fix available at position */
  fun hasQuickFixAt(line: Int, column: Int): Boolean {
    val diagnostic = getDiagnosticAt(line, column)
    return diagnostic?.code == "missing_import"
  }

  fun clear() {
    diagnosticsByLine.clear()
  }
}
