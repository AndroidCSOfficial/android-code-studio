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
package com.tom.rv2ide.lsp.clang

import android.content.Context
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.tom.rv2ide.projects.IWorkspace
import com.tom.rv2ide.utils.Environment
import java.io.File
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ClangWorkspaceSetup(private val context: Context, private val workspace: IWorkspace) {

  companion object {
    private val log = LoggerFactory.getLogger(ClangWorkspaceSetup::class.java)
  }

  fun setup(processManager: ClangServerProcessManager) {
    val workspaceRoot = workspace.getProjectDir().toURI().toString()
    ClangLogs.info("Setting up workspace with root: {}", workspaceRoot)

    // ALWAYS regenerate compile_commands.json to ensure it's up to date
    val compileCommandsPath = generateCompileCommands()
    processManager.startServer(compileCommandsPath)

    val initParams = createInitParams(workspaceRoot)

    ClangLogs.info("Sending initialize request...")

    processManager.sendRequest("initialize", initParams) { result ->
      ClangLogs.info("Server initialized successfully")
      processManager.sendNotification("initialized", JsonObject())
    }
  }

  private fun generateCompileCommands(): String? {
    try {
      val projectDir = workspace.getProjectDir()
      val buildDir =
          File(Environment.PREFIX, "clanglsp/build").apply {
            deleteRecursively() // Delete old files
            mkdirs()
          }
      val compileCommandsFile = File(buildDir, "compile_commands.json")

      ClangLogs.info("Generating fresh compile_commands.json...")

      // Find all C/C++ source files in project
      val sourceFiles = findSourceFiles(projectDir)

      if (sourceFiles.isEmpty()) {
        ClangLogs.warn("No C/C++ source files found in project")
        return null
      }

      // Generate compile_commands.json with EXACT clang++ invocation
      val jsonArray = JsonArray()
      sourceFiles.forEach { file ->
        val entry =
            JsonObject().apply {
              addProperty("directory", projectDir.absolutePath)
              addProperty("command", buildExactClangCommand(file))
              addProperty("file", file.absolutePath)
            }
        jsonArray.add(entry)
      }

      compileCommandsFile.writeText(jsonArray.toString())
      ClangLogs.info("Generated compile_commands.json with {} entries", sourceFiles.size)

      // Log the first command for verification
      if (sourceFiles.isNotEmpty()) {
        ClangLogs.info("Sample compile command: {}", buildExactClangCommand(sourceFiles[0]))
      }

      return buildDir.absolutePath
    } catch (e: Exception) {
      ClangLogs.error("Failed to generate compile_commands.json", e)
      return null
    }
  }

  private fun findSourceFiles(dir: File, maxDepth: Int = 5, currentDepth: Int = 0): List<File> {
    if (currentDepth > maxDepth) return emptyList()

    val sourceFiles = mutableListOf<File>()
    val validExtensions = setOf("c", "cpp", "cc", "cxx", "c++")

    dir.listFiles()?.forEach { file ->
      when {
        file.isFile && file.extension in validExtensions -> {
          sourceFiles.add(file)
        }
        file.isDirectory && !file.name.startsWith(".") && file.name != "build" -> {
          sourceFiles.addAll(findSourceFiles(file, maxDepth, currentDepth + 1))
        }
      }
    }

    return sourceFiles
  }

  private fun buildExactClangCommand(file: File): String {
    // Build the EXACT command that clang++ -v showed us
    val clangPath = File(Environment.PREFIX, "bin/clang++").absolutePath

    val standard =
        if (file.extension in setOf("cpp", "cc", "cxx", "c++")) {
          "-std=c++17"
        } else {
          "-std=c11"
        }

    // Use the exact paths from clang++ -v output
    // These are relative to PREFIX which is /data/data/com.tom.rv2ide/files/usr
    val includePaths =
        listOf(
            File(Environment.PREFIX, "include/c++/v1").absolutePath,
            File(Environment.PREFIX, "lib/clang/20/include").absolutePath,
            File(Environment.PREFIX, "include/aarch64-linux-android").absolutePath,
            File(Environment.PREFIX, "include").absolutePath,
        )

    // Log each path to verify
    includePaths.forEachIndexed { index, path ->
      val exists = File(path).exists()
      ClangLogs.info("Include path {}: {} (exists: {})", index, path, exists)
    }

    // Build flags exactly as clang++ uses them
    val flags = mutableListOf<String>()
    flags.add("\"$clangPath\"")
    flags.add(standard)

    // Add include paths using -isystem for system headers
    includePaths.forEach { path ->
      flags.add("-isystem")
      flags.add("\"$path\"")
    }

    // Add resource dir explicitly
    flags.add("-resource-dir=${File(Environment.PREFIX, "lib/clang/20").absolutePath}")

    // Add target triple
    flags.add("-target")
    flags.add("aarch64-unknown-linux-android24")

    // Compilation flags
    flags.add("-c")
    flags.add("\"${file.absolutePath}\"")

    return flags.joinToString(" ")
  }

  private fun createInitParams(workspaceRoot: String): JsonObject {
    return JsonObject().apply {
      addProperty("processId", android.os.Process.myPid())
      addProperty("rootUri", workspaceRoot)

      add(
          "capabilities",
          JsonObject().apply {
            add(
                "textDocument",
                JsonObject().apply {
                  add(
                      "completion",
                      JsonObject().apply {
                        add(
                            "completionItem",
                            JsonObject().apply {
                              addProperty("snippetSupport", true)
                              addProperty("commitCharactersSupport", true)
                              add(
                                  "documentationFormat",
                                  JsonArray().apply {
                                    add("plaintext")
                                    add("markdown")
                                  },
                              )
                              addProperty("deprecatedSupport", true)
                              addProperty("preselectSupport", true)
                            },
                        )
                        addProperty("contextSupport", true)
                      },
                  )
                  add(
                      "hover",
                      JsonObject().apply {
                        add(
                            "contentFormat",
                            JsonArray().apply {
                              add("plaintext")
                              add("markdown")
                            },
                        )
                      },
                  )
                  add(
                      "signatureHelp",
                      JsonObject().apply {
                        add(
                            "signatureInformation",
                            JsonObject().apply {
                              add(
                                  "documentationFormat",
                                  JsonArray().apply {
                                    add("plaintext")
                                    add("markdown")
                                  },
                              )
                            },
                        )
                      },
                  )
                  add("definition", JsonObject().apply { addProperty("linkSupport", true) })
                  add("references", JsonObject())
                  add("documentHighlight", JsonObject())
                  add("documentSymbol", JsonObject())
                  add("codeAction", JsonObject())
                  add("codeLens", JsonObject())
                  add("formatting", JsonObject())
                  add("rangeFormatting", JsonObject())
                  add("onTypeFormatting", JsonObject())
                  add("rename", JsonObject())
                  add(
                      "publishDiagnostics",
                      JsonObject().apply { addProperty("relatedInformation", true) },
                  )
                },
            )

            add(
                "workspace",
                JsonObject().apply {
                  addProperty("applyEdit", true)
                  add("workspaceEdit", JsonObject().apply { addProperty("documentChanges", true) })
                  add(
                      "didChangeConfiguration",
                      JsonObject().apply { addProperty("dynamicRegistration", true) },
                  )
                  add(
                      "didChangeWatchedFiles",
                      JsonObject().apply { addProperty("dynamicRegistration", true) },
                  )
                  add("symbol", JsonObject().apply { addProperty("dynamicRegistration", true) })
                  add(
                      "executeCommand",
                      JsonObject().apply { addProperty("dynamicRegistration", true) },
                  )
                },
            )
          },
      )

      // Comprehensive fallback flags matching clang++ exactly
      add(
          "initializationOptions",
          JsonObject().apply {
            addProperty("clangdFileStatus", true)
            addProperty("compilationDatabasePath", "${Environment.PREFIX}/clanglsp/build")

            add(
                "fallbackFlags",
                JsonArray().apply {
                  // Core flags
                  add("-std=c++17")
                  add("-target")
                  add("aarch64-unknown-linux-android24")

                  // System include paths using -isystem
                  add("-isystem")
                  add("${Environment.PREFIX}/include/c++/v1")
                  add("-isystem")
                  add("${Environment.PREFIX}/lib/clang/20/include")
                  add("-isystem")
                  add("${Environment.PREFIX}/include/aarch64-linux-android")
                  add("-isystem")
                  add("${Environment.PREFIX}/include")

                  // Resource directory
                  add("-resource-dir=${Environment.PREFIX}/lib/clang/20")

                  // Warning suppression
                  add("-Wno-unused-parameter")
                  add("-Wno-unused-variable")
                  add("-Wno-unknown-pragmas")
                },
            )
          },
      )
    }
  }
}
