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

import com.tom.rv2ide.lsp.models.DiagnosticItem
import com.tom.rv2ide.lsp.models.DiagnosticResult
import com.tom.rv2ide.lsp.models.DiagnosticSeverity
import com.tom.rv2ide.models.Position
import com.tom.rv2ide.models.Range
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import org.slf4j.LoggerFactory

/**
 * Kotlin diagnostic provider that works independently of the KLS server's document state. This
 * provides basic syntax and type checking for Kotlin files.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class KotlinDiagnosticProvider {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinDiagnosticProvider::class.java)
  }

  private var analyzing = AtomicBoolean(false)

  fun analyze(file: Path): DiagnosticResult {
    if (analyzing.get()) {
      log.debug("Already analyzing, skipping: {}", file)
      return DiagnosticResult.NO_UPDATE
    }

    analyzing.set(true)

    try {
      log.debug("Analyzing Kotlin file: {}", file)

      val content = file.toFile().readText()
      val diagnostics = mutableListOf<DiagnosticItem>()

      // Basic syntax checking
      analyzeSyntax(content, diagnostics)

      // Basic type checking
      analyzeTypes(content, diagnostics)

      val result = DiagnosticResult(file, diagnostics)
      log.info("Kotlin analysis completed. Found {} diagnostic items", diagnostics.size)

      // Debug: log the first few diagnostics
      diagnostics.take(3).forEach { diag ->
        log.debug(
            "Diagnostic: {} at line {}:{} - {}:{}",
            diag.message,
            diag.range.start.line,
            diag.range.start.column,
            diag.range.end.line,
            diag.range.end.column,
        )
      }

      return result
    } catch (e: Exception) {
      log.error("Error analyzing Kotlin file: {}", file, e)
      return DiagnosticResult.NO_UPDATE
    } finally {
      analyzing.set(false)
    }
  }

  private fun analyzeSyntax(content: String, diagnostics: MutableList<DiagnosticItem>) {
    val lines = content.split("\n")

    lines.forEachIndexed { lineIndex, line ->
      // Only check for obvious type mismatches - be very conservative
      if (line.contains("val ") && line.contains(": ") && line.contains(" = ")) {
        val valMatch = Regex("val\\s+(\\w+)\\s*:\\s*(\\w+)\\s*=\\s*(.+)").find(line)
        if (valMatch != null) {
          val type = valMatch.groupValues[2]
          val value = valMatch.groupValues[3].trim()

          // Only flag obvious mismatches - be very strict
          when (type) {
            "String" -> {
              // Only flag if it's clearly a number or boolean
              if (value.matches(Regex("\\d+")) || value.matches(Regex("true|false"))) {
                val startCol = line.indexOf(value)
                diagnostics.add(
                    createDiagnosticWithColumn(
                        lineIndex,
                        startCol,
                        startCol + value.length,
                        "Type mismatch: expected String but found ${value}",
                        "TYPE_MISMATCH",
                        DiagnosticSeverity.ERROR,
                    )
                )
              }
            }
            "Int" -> {
              // Only flag if it's clearly a string or boolean
              if (
                  value.startsWith("\"") ||
                      value.startsWith("'") ||
                      value.matches(Regex("true|false"))
              ) {
                val startCol = line.indexOf(value)
                diagnostics.add(
                    createDiagnosticWithColumn(
                        lineIndex,
                        startCol,
                        startCol + value.length,
                        "Type mismatch: expected Int but found ${value}",
                        "TYPE_MISMATCH",
                        DiagnosticSeverity.ERROR,
                    )
                )
              }
            }
            "Boolean" -> {
              // Only flag if it's clearly a string or number
              if (value.startsWith("\"") || value.startsWith("'") || value.matches(Regex("\\d+"))) {
                val startCol = line.indexOf(value)
                diagnostics.add(
                    createDiagnosticWithColumn(
                        lineIndex,
                        startCol,
                        startCol + value.length,
                        "Type mismatch: expected Boolean but found ${value}",
                        "TYPE_MISMATCH",
                        DiagnosticSeverity.ERROR,
                    )
                )
              }
            }
          }
        }
      }
    }
  }

  private fun analyzeTypes(content: String, diagnostics: MutableList<DiagnosticItem>) {
    // Skip unresolved reference checking for now - it's too complex and error-prone
    // The KLS server should handle this properly
  }

  private fun isKnownReference(ref: String): Boolean {
    val knownRefs =
        setOf(
            "println",
            "print",
            "readLine",
            "readln",
            "readlnOrNull",
            "String",
            "Int",
            "Long",
            "Double",
            "Float",
            "Boolean",
            "Char",
            "Byte",
            "Short",
            "Array",
            "List",
            "Set",
            "Map",
            "MutableList",
            "mutableListOf",
            "listOf",
            "setOf",
            "mapOf",
            "arrayOf",
            "if",
            "when",
            "for",
            "while",
            "do",
            "try",
            "catch",
            "finally",
            "throw",
            "return",
            "break",
            "continue",
            "class",
            "interface",
            "object",
            "fun",
            "val",
            "var",
            "private",
            "public",
            "protected",
            "internal",
            "abstract",
            "final",
            "open",
            "sealed",
            "data",
            "enum",
            "annotation",
            "companion",
            "override",
            "super",
            "this",
            "null",
            "true",
            "false",
            "as",
            "is",
            "in",
            "out",
            "vararg",
            "const",
            "lateinit",
            "noinline",
            "crossinline",
            "reified",
            "infix",
            "operator",
            "inline",
            "external",
            "expect",
            "actual",
        )
    return knownRefs.contains(ref)
  }

  private fun createDiagnostic(
      line: Int,
      message: String,
      code: String,
      severity: DiagnosticSeverity,
  ): DiagnosticItem {
    // Calculate proper character indices for the range
    val startIndex = line * 80 // Rough approximation
    val endIndex = startIndex + 50 // Assume diagnostic spans about 50 characters

    return DiagnosticItem(
        message = message,
        code = code,
        range = Range(start = Position(line, 0, startIndex), end = Position(line, 50, endIndex)),
        source = "kotlin-analyzer",
        severity = severity,
    )
  }

  private fun createDiagnosticWithColumn(
      line: Int,
      startCol: Int,
      endCol: Int,
      message: String,
      code: String,
      severity: DiagnosticSeverity,
  ): DiagnosticItem {
    // Don't set index - let asDiagnosticRegion handle the conversion
    return DiagnosticItem(
        message = message,
        code = code,
        range = Range(start = Position(line, startCol), end = Position(line, endCol)),
        source = "kotlin-analyzer",
        severity = severity,
    )
  }

  fun isAnalyzing(): Boolean = analyzing.get()

  fun cancel() {
    analyzing.set(false)
  }
}
