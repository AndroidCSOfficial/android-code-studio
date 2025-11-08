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

import com.tom.rv2ide.lsp.kotlin.compiler.KotlinCompilerService
import com.tom.rv2ide.lsp.models.DiagnosticItem
import com.tom.rv2ide.lsp.models.DiagnosticSeverity
import com.tom.rv2ide.models.Position
import com.tom.rv2ide.models.Range
import java.nio.file.Path
import java.util.jar.JarFile
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Analyzes Kotlin files for missing imports and provides smart import suggestions Shows orange
 * warnings for unresolved references that can be auto-imported
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class KotlinImportAnalyzer {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinImportAnalyzer::class.java)

    // Core Kotlin packages that are always available
    private val CORE_KOTLIN_PACKAGES =
        setOf(
            "kotlin",
            "kotlin.collections",
            "kotlin.ranges",
            "kotlin.sequences",
            "kotlin.text",
            "kotlin.io",
            "kotlin.jvm",
            "kotlin.experimental",
        )
  }

  private val importCache = mutableMapOf<String, MutableList<String>>()
  private val packageCache = mutableMapOf<String, MutableSet<String>>()

  // Store diagnostic metadata separately since DiagnosticItem doesn't support data field
  private val diagnosticMetadata = mutableMapOf<String, DiagnosticMetadata>()
  private val analysisDispatcher =
      Dispatchers.Default.limitedParallelism(Runtime.getRuntime().availableProcessors())

  data class DiagnosticMetadata(
      val className: String,
      val possibleImports: List<String>,
      val quickFixAvailable: Boolean,
  )

  /** Configuration for the analyzer */
  data class AnalyzerConfig(
      val additionalPackages: Set<String> = emptySet(),
      val excludePackages: Set<String> = emptySet(),
      val maxImportSuggestions: Int = 5,
      val enableJarScanning: Boolean = true,
  )

  private var config = AnalyzerConfig()

  /** Updates analyzer configuration */
  fun updateConfig(newConfig: AnalyzerConfig) {
    config = newConfig
    // Clear caches when config changes
    clearCache()
  }

  /** Analyzes file content and returns diagnostics for missing imports */
  suspend fun analyzeMissingImports(file: Path, content: String): List<DiagnosticItem> =
      withContext(analysisDispatcher) {
        val diagnostics = mutableListOf<DiagnosticItem>()
        val existingImports = extractExistingImports(content)

        // Find all unresolved class references
        val unresolvedClasses = findUnresolvedClasses(content, existingImports)

        // Process unresolved classes in parallel
        val results =
            unresolvedClasses
                .map { (className, positions) ->
                  async {
                    val possibleImports = findPossibleImports(className)

                    if (possibleImports.isNotEmpty()) {
                      positions.map { pos ->
                        val diagnostic = createImportDiagnostic(className, pos, possibleImports)

                        // Store metadata
                        val key = "${file}:${pos.start.line}:${pos.start.column}"
                        diagnosticMetadata[key] =
                            DiagnosticMetadata(
                                className = className,
                                possibleImports = possibleImports,
                                quickFixAvailable = true,
                            )

                        diagnostic
                      }
                    } else {
                      emptyList()
                    }
                  }
                }
                .awaitAll()
                .flatten()

        diagnostics.addAll(results)
        return@withContext diagnostics
      }

  /** Gets metadata for a diagnostic */
  fun getMetadata(file: Path, range: Range): DiagnosticMetadata? {
    val key = "${file}:${range.start.line}:${range.start.column}"
    return diagnosticMetadata[key]
  }

  /** Extracts existing import statements from file */
  private fun extractExistingImports(content: String): Set<String> {
    val imports = mutableSetOf<String>()
    val importRegex = """import\s+([\w.$\_]+)(?:\s*\.\s*\*)?""".toRegex()

    importRegex.findAll(content).forEach { match -> imports.add(match.groupValues[1]) }

    return imports
  }

  /** Finds unresolved class references in the code */
  private fun findUnresolvedClasses(
      content: String,
      existingImports: Set<String>,
  ): Map<String, List<Range>> {
    val unresolvedMap = mutableMapOf<String, MutableList<Range>>()
    val lines = content.split("\n")

    // Improved regex to match class names (uppercase start) and avoid false positives
    val classRefRegex = """\b([A-Z][A-Za-z0-9_]*)\b""".toRegex()

    lines.forEachIndexed { lineIndex, line ->
      // Skip import lines, package lines, and comments
      if (shouldSkipLine(line)) {
        return@forEachIndexed
      }

      classRefRegex.findAll(line).forEach { match ->
        val className = match.value

        // Skip if already imported or is a known type
        if (
            !isAlreadyResolved(className, existingImports) &&
                !isKnownType(className) &&
                !isInCurrentPackage(className, content) &&
                isValidClassName(className)
        ) {

          val startCol = match.range.first
          val endCol = match.range.last + 1

          val range =
              Range(start = Position(lineIndex, startCol), end = Position(lineIndex, endCol))

          unresolvedMap.getOrPut(className) { mutableListOf() }.add(range)

          KslLogs.debug("Found unresolved reference: '{}' at line {}", className, lineIndex)
        }
      }
    }

    KslLogs.debug("Found {} unresolved class references", unresolvedMap.size)
    return unresolvedMap
  }

  /** Additional validation for class names to reduce false positives */
  private fun isValidClassName(className: String): Boolean {
    // Skip single character class names (usually type parameters)
    if (className.length == 1) return false

    // Skip common false positives
    val falsePositives = setOf("Companion", "TODO", "TODO0", "TODO1")
    if (className in falsePositives) return false

    return true
  }

  /** Determines if a line should be skipped during analysis */
  private fun shouldSkipLine(line: String): Boolean {
    val trimmed = line.trim()
    return trimmed.startsWith("import ") ||
        trimmed.startsWith("package ") ||
        trimmed.startsWith("//") ||
        trimmed.startsWith("/*") ||
        trimmed.startsWith("*") ||
        trimmed.startsWith("@") ||
        trimmed.isEmpty()
  }

  /** Checks if a class is already resolved (imported or in same package) */
  private fun isAlreadyResolved(className: String, existingImports: Set<String>): Boolean {
    // Check if any import ends with this class name or uses wildcard
    return existingImports.any {
      it.endsWith(".$className") || it.endsWith(".*") || it == className
    }
  }

  /** Checks if a type is a known type (Kotlin standard, Java lang, or current package) */
  private fun isKnownType(className: String): Boolean {
    return isKotlinStandard(className) || isJavaLangType(className) || isCommonType(className)
  }

  /** Checks if a type is in the current package (no import needed) */
  private fun isInCurrentPackage(className: String, content: String): Boolean {
    val packageRegex = """package\s+([\w.]+)""".toRegex()
    val packageMatch = packageRegex.find(content)
    if (packageMatch != null) {
      val currentPackage = packageMatch.groupValues[1]
      // In Kotlin, classes in the same package don't need imports
      // We assume the class exists in the same package
      return true // Conservative approach - we'll refine this with actual project structure later
    }
    return false
  }

  /** Checks if a type is a Kotlin standard library type */
  private fun isKotlinStandard(className: String): Boolean {
    val kotlinStandardTypes =
        setOf(
            "Int",
            "Long",
            "Short",
            "Byte",
            "Float",
            "Double",
            "Boolean",
            "Char",
            "String",
            "Unit",
            "Nothing",
            "Any",
            "List",
            "Map",
            "Set",
            "Array",
            "Pair",
            "Triple",
            "Throwable",
            "Exception",
            "Error",
            "RuntimeException",
            "Iterable",
            "Collection",
            "Sequence",
            "Comparator",
            "Enum",
            "Annotation",
            "Deprecated",
            "Suppress",
        )
    return className in kotlinStandardTypes
  }

  /** Checks if a type is from java.lang package (automatically imported) */
  private fun isJavaLangType(className: String): Boolean {
    val javaLangTypes =
        setOf(
            "Object",
            "Class",
            "String",
            "Integer",
            "Long",
            "Double",
            "Float",
            "Boolean",
            "Short",
            "Byte",
            "Character",
            "Void",
            "Exception",
            "RuntimeException",
            "Error",
            "System",
            "Math",
        )
    return className in javaLangTypes
  }

  /** Checks for other common types that might be available */
  private fun isCommonType(className: String): Boolean {
    // Add any project-specific common types here
    return false
  }

  /** Finds possible fully qualified names for a class */
  private fun findPossibleImports(className: String): List<String> {
    // Check cache first
    if (importCache.containsKey(className)) {
      return importCache[className]!!.take(config.maxImportSuggestions)
    }

    val possibleImports = mutableListOf<String>()

    // Check package cache for dynamic discovery
    if (packageCache.isNotEmpty()) {
      packageCache.forEach { (packageName, classes) ->
        if (classes.contains(className) && !isPackageExcluded(packageName)) {
          possibleImports.add("$packageName.$className")
        }
      }
    }

    // Store in cache (limited to max suggestions)
    importCache[className] = possibleImports.toMutableList()
    return possibleImports.take(config.maxImportSuggestions)
  }

  /** Checks if a package should be excluded from suggestions */
  private fun isPackageExcluded(packageName: String): Boolean {
    return config.excludePackages.any { packageName.startsWith(it) }
  }

  /** Creates an orange warning diagnostic for missing import */
  private fun createImportDiagnostic(
      className: String,
      range: Range,
      possibleImports: List<String>,
  ): DiagnosticItem {
    val message =
        when {
          possibleImports.isEmpty() -> "Unresolved reference: $className"
          possibleImports.size == 1 ->
              "Unresolved reference: $className. Click to import ${possibleImports[0]}"
          else ->
              "Unresolved reference: $className. Click to choose import (${possibleImports.size} options)"
        }

    return DiagnosticItem(
        message = message,
        code = "missing_import",
        range = range,
        source = "kotlin-import-analyzer",
        severity = DiagnosticSeverity.WARNING, // Orange color
    )
  }

  /** Updates import cache from compiler service and project structure */
  fun updateImportCache(compilerService: KotlinCompilerService?) {
    if (!config.enableJarScanning) {
      return
    }

    compilerService?.let { service ->
      try {
        // Clear previous cache
        packageCache.clear()
        importCache.clear()

        // Get all classes from classpath
        val allClasses =
            service.getFileManager().getAllClassPaths().flatMap { jarFile ->
              extractClassNamesFromJar(jarFile)
            }

        KslLogs.info("Found {} classes from classpath", allClasses.size)

        // Build package and import caches
        allClasses.forEach { fqn ->
          val className = fqn.substringAfterLast('.')
          val packageName = fqn.substringBeforeLast('.', "")

          if (packageName.isNotEmpty() && className.isNotEmpty()) {
            // Add to package cache
            packageCache.getOrPut(packageName) { mutableSetOf() }.add(className)

            // Add to import cache
            importCache.getOrPut(className) { mutableListOf() }.add(fqn)
          }
        }

        // Add configured additional packages
        config.additionalPackages.forEach { pkg ->
          if (!packageCache.containsKey(pkg)) {
            packageCache[pkg] = mutableSetOf()
          }
        }

        // Add common Android classes dynamically
        addCommonAndroidClasses()

        log.info(
            "Import cache updated with {} classes from {} packages",
            importCache.size,
            packageCache.size,
        )

        // Log some examples for debugging
        if (importCache.isNotEmpty()) {
          val sampleClasses = importCache.keys.take(5)
          KslLogs.debug("Sample cached classes: {}", sampleClasses)
        }
      } catch (e: Exception) {
        log.error("Failed to update import cache", e)
      }
    }
  }

  /** Add common Android classes that might not be in JARs but are commonly used */
  private fun addCommonAndroidClasses() {
    val commonAndroidClasses =
        mapOf(
            "Toast" to "android.widget.Toast",
            "Context" to "android.content.Context",
            "Activity" to "android.app.Activity",
            "Fragment" to "androidx.fragment.app.Fragment",
            "Intent" to "android.content.Intent",
            "Bundle" to "android.os.Bundle",
            "View" to "android.view.View",
            "TextView" to "android.widget.TextView",
            "Button" to "android.widget.Button",
            "RecyclerView" to "androidx.recyclerview.widget.RecyclerView",
        )

    commonAndroidClasses.forEach { (className, fqn) ->
      importCache.getOrPut(className) { mutableListOf() }.add(fqn)
      val packageName = fqn.substringBeforeLast('.')
      packageCache.getOrPut(packageName) { mutableSetOf() }.add(className)
    }
  }

  /** Manually add packages to the analyzer */
  fun addPackages(packages: List<String>) {
    packages.forEach { pkg -> packageCache.getOrPut(pkg) { mutableSetOf() } }
  }

  /** Manually add specific class mappings */
  fun addClassMapping(className: String, fullyQualifiedName: String) {
    importCache.getOrPut(className) { mutableListOf() }.add(fullyQualifiedName)

    val packageName = fullyQualifiedName.substringBeforeLast('.', "")
    if (packageName.isNotEmpty()) {
      packageCache.getOrPut(packageName) { mutableSetOf() }.add(className)
    }
  }

  private fun extractClassNamesFromJar(jarFile: java.io.File): List<String> {
    val classNames = mutableListOf<String>()

    try {
      if (!jarFile.exists() || !jarFile.name.endsWith(".jar")) {
        return emptyList()
      }

      JarFile(jarFile).use { jar ->
        val entries = jar.entries()
        while (entries.hasMoreElements()) {
          val entry = entries.nextElement()
          val name = entry.name

          // Only process .class files and exclude inner classes
          if (name.endsWith(".class") && !name.contains("$")) {
            // Convert path to FQN: com/example/MyClass.class -> com.example.MyClass
            val fqn = name.substring(0, name.length - 6).replace('/', '.')
            classNames.add(fqn)
          }
        }
      }
    } catch (e: Exception) {
      log.debug("Failed to scan JAR: {}", jarFile.name)
    }

    return classNames
  }

  fun clearCache() {
    importCache.clear()
    packageCache.clear()
    diagnosticMetadata.clear()
  }

  /** Get cache statistics for debugging */
  fun getCacheStats(): Map<String, Any> {
    return mapOf(
        "importCacheSize" to importCache.size,
        "packageCacheSize" to packageCache.size,
        "diagnosticMetadataSize" to diagnosticMetadata.size,
    )
  }
}
