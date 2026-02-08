package com.tom.rv2ide.preview

import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.projects.ModuleProject
import com.tom.rv2ide.lsp.kotlin.KotlinCompilerProvider
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object PreviewPackager {

  data class Result(val success: Boolean, val artifactPath: String?, val logs: String)

  fun packagePreview(sourceFile: String, previewFunction: String?): Result {
    val logs = StringBuilder()
    try {
      val tmp = createTempDir(prefix = "preview_pack_")

      val outJar = File(tmp, "out.jar")
      // Try to obtain project classpath for proper compilation
      val projectClasspath = try {
        val workspace = IProjectManager.getInstance().getWorkspace()
        val module: ModuleProject? = workspace?.findModuleForFile(File(sourceFile), false)
        if (module != null) {
          val compilerService = KotlinCompilerProvider.get(module)
          val paths = compilerService.getFileManager().getAllClassPaths().map { it.absolutePath }
          paths.joinToString(File.pathSeparator)
        } else null
      } catch (e: Exception) {
        null
      }
      val kotlinc = System.getenv("KOTLINC") ?: "kotlinc"

      // Build list of sources to compile; include wrapper if previewFunction provided
      val sourcesToCompile = mutableListOf<String>()
      sourcesToCompile.add(sourceFile)
      var wrapperFile: File? = null
      if (!previewFunction.isNullOrEmpty()) {
        try {
          val srcText = File(sourceFile).readText()
          val pkgLine = srcText.lines().firstOrNull { it.trim().startsWith("package ") }
          val pkg = pkgLine?.substringAfter("package")?.trim() ?: ""
          wrapperFile = File(tmp, "PreviewWrapper.kt")
          val wrapperSource = buildString {
            if (pkg.isNotEmpty()) append("package previewwrap\n\n")
            append("import androidx.compose.runtime.Composable\n")
            if (pkg.isNotEmpty()) append("import $pkg.*\n")
            append("@Composable\n")
            append("fun __PreviewEntry() {\n")
            append("    ${previewFunction}()\n")
            append("}\n")
          }
          wrapperFile.writeText(wrapperSource)
          sourcesToCompile.add(wrapperFile.absolutePath)
        } catch (e: Exception) {
          logs.append("Failed to generate wrapper: ${e.message}\n")
        }
      }

      val compileCmd = mutableListOf<String>()
      compileCmd.add(kotlinc)
      compileCmd.addAll(sourcesToCompile)
      if (!projectClasspath.isNullOrEmpty()) {
        compileCmd.addAll(listOf("-classpath", projectClasspath))
      }
      compileCmd.addAll(listOf("-d", outJar.absolutePath))

      logs.append("Running: ${compileCmd.joinToString(" ")}\n")
      val proc = ProcessBuilder(compileCmd).redirectErrorStream(true).start()
      proc.inputStream.bufferedReader().use { reader ->
        reader.forEachLine { logs.append(it).append('\n') }
      }
      val exit = proc.waitFor()
      if (exit != 0) {
        return Result(false, null, logs.toString())
      }

      // Convert to DEX using d8 (from Android SDK). Try 'd8' on PATH.
      val dexOut = File(tmp, "dex")
      dexOut.mkdirs()
      val d8 = System.getenv("D8") ?: "d8"
      val d8Cmd = listOf(d8, outJar.absolutePath, "--output", dexOut.absolutePath)
      logs.append("Running: ${d8Cmd.joinToString(" ")}\n")
      val proc2 = ProcessBuilder(d8Cmd).redirectErrorStream(true).start()
      proc2.inputStream.bufferedReader().use { reader ->
        reader.forEachLine { logs.append(it).append('\n') }
      }
      val exit2 = proc2.waitFor()
      if (exit2 != 0) {
        return Result(false, null, logs.toString())
      }

      // Return dex output directory
      return Result(true, dexOut.absolutePath, logs.toString())
    } catch (e: Exception) {
      logs.append("Exception: ").append(e.toString())
      return Result(false, null, logs.toString())
    }
  }
}
