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
import com.tom.rv2ide.projects.IProjectManager
import com.tom.rv2ide.projects.ModuleProject
import com.tom.rv2ide.projects.android.AndroidModule
import com.tom.rv2ide.projects.classpath.ClassInfo
import com.tom.rv2ide.projects.classpath.IClasspathReader
import com.tom.rv2ide.projects.classpath.JarFsClasspathReader
import java.io.File
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinClasspathProvider {

  private var compilerService: KotlinCompilerService? = null
  private val classpathReader: IClasspathReader = JarFsClasspathReader()
  private val log = LoggerFactory.getLogger(KotlinClasspathProvider::class.java)

  private var cachedClasspathList: List<String>? = null
  private var cachedClasspath: String? = null

  fun initialize(service: KotlinCompilerService?) {
    this.compilerService = service
    // Clear cache on re-initialization
    cachedClasspathList = null
    cachedClasspath = null
  }

  fun getClasspath(): String {
    if (cachedClasspath != null) {
      return cachedClasspath!!
    }
    cachedClasspath = getClasspathList().joinToString(":")
    return cachedClasspath!!
  }

  fun getClasspathList(): List<String> {
    if (cachedClasspathList != null) {
      return cachedClasspathList!!
    }

    val classpaths = mutableSetOf<String>()

    // First, try to get classpaths from the compiler service
    val service = compilerService
    if (service != null) {
      try {
        val allClassPaths = service.getFileManager().getAllClassPaths()
        for (cp in allClassPaths) {
          classpaths.add(cp.absolutePath)
        }
      } catch (e: Exception) {
        log.error("Failed to get classpath from compiler service", e)
      }
    }

    // Then, enhance with project system classpaths
    try {
      val projectManager = IProjectManager.getInstance()
      val workspace = projectManager.getWorkspace()

      if (workspace != null) {
        // Get all projects (root + subprojects)
        val allProjects = mutableListOf(workspace.getRootProject())
        allProjects.addAll(workspace.getSubProjects())

        for (project in allProjects) {
          if (project is ModuleProject) {
            // Add compile classpaths from each module
            val compileClasspaths = project.getCompileClasspaths()
            for (cp in compileClasspaths) {
              classpaths.add(cp.absolutePath)
            }

            // Add module-specific classpaths (includes external dependencies)
            val moduleClasspaths = project.getModuleClasspaths()
            for (cp in moduleClasspaths) {
              classpaths.add(cp.absolutePath)
            }

            // If it's an Android module, add additional Android-specific classpaths
            if (project is AndroidModule) {
              // Add boot classpaths (android.jar, etc.)
              for (bootCp in project.bootClassPaths) {
                classpaths.add(bootCp.absolutePath)
              }

              // Add generated jar
              val generatedJar = project.getGeneratedJar()
              if (generatedJar.exists()) {
                classpaths.add(generatedJar.absolutePath)
                log.info("Added generated JAR: {}", generatedJar.absolutePath)
              }

              // Add selected variant's class jars
              val variant = project.getSelectedVariant()
              if (variant != null) {
                for (classJar in variant.mainArtifact.classJars) {
                  classpaths.add(classJar.absolutePath)
                }
              }

              // *** ADD THIS CALL HERE ***
              // Add Android generated sources (R.java, BuildConfig, DataBinding, etc.)
              addAndroidGeneratedSources(project, classpaths)
            }
          }
        }
      }
    } catch (e: Exception) {
      log.error("Failed to get classpath from project system", e)
    }

    addKotlinScriptingJarsFromGradleCache(classpaths)

    val existingPaths = classpaths.filter { File(it).exists() }.toList()
    log.info("Total classpath entries: {}, existing: {}", classpaths.size, existingPaths.size)

    cachedClasspathList = existingPaths
    return existingPaths
  }

  private fun addAndroidGeneratedSources(module: AndroidModule, classpaths: MutableSet<String>) {
    try {
      val moduleDir = File(module.path)
      val buildDir = File(moduleDir, "build")

      if (!buildDir.exists()) {
        log.warn("Build directory not found for module: {}", module.path)
        return
      }

      log.info("Scanning for generated sources in: {}", buildDir.absolutePath)
      addExternalLibraryJars(buildDir, classpaths)
      // List of potential generated source directories
      val generatedPaths =
          listOf(
              // R.java and resource classes
              "generated/source/r/debug",
              "generated/not_namespaced_r_class_sources/debug/r",
              "generated/not_namespaced_r_class_sources/debug/processDebugResources/r",

              // BuildConfig
              "generated/source/buildConfig/debug",

              // Data Binding
              "generated/data_binding_base_class_source_out/",
              "generated/data_binding_base_class_source_out/debug/out",
              "generated/source/dataBinding/debug",
              "generated/ap_generated_sources/debug/out",

              // View Binding (often bundled with data binding)
              "generated/source/viewBinding/debug",

              // AIDL
              "generated/aidl_source_output_dir/debug/out",
              "generated/source/aidl/debug",

              // Annotation Processing (KAPT)
              "generated/source/kapt/debug",
              "generated/source/kaptKotlin/debug",
              "tmp/kapt3/classes/debug",
              "generated/ap_generated_sources/debug/out",

              // RenderScript
              "generated/source/rs/debug",

              // Navigation component generated args
              "generated/source/navigation-args/debug",

              // Assets
              "generated/assets",

              // Merged resources
              "intermediates/packaged_res/debug/packageDebugResources",
              "intermediates/merged_res/debug/mergeDebugResources",

              // Compiled resources (res values)
              "generated/res/resValues/debug",
          )

      var addedCount = 0
      for (path in generatedPaths) {
        val dir = File(buildDir, path)
        if (dir.exists() && dir.isDirectory) {
          classpaths.add(dir.absolutePath)
          addedCount++
          log.info("✓ Added generated source: {}", path)
        } else {
          log.debug("✗ Not found: {}", path)
        }
      }

      // Scan the entire generated directory for any missed directories
      val generatedDir = File(buildDir, "generated")
      if (generatedDir.exists() && generatedDir.isDirectory) {
        scanForSourceDirectories(generatedDir, classpaths, 4)
      }

      // Also check intermediates directory for compiled classes
      val intermediatesDir = File(buildDir, "intermediates")
      if (intermediatesDir.exists() && intermediatesDir.isDirectory) {
        // Add javac output
        val javacDir = File(intermediatesDir, "javac/debug/classes")
        if (javacDir.exists()) {
          classpaths.add(javacDir.absolutePath)
          addedCount++
          log.info("✓ Added javac classes: {}", javacDir.absolutePath)
        }

        // Add compiled classes from other tasks
        findCompiledClassDirectories(intermediatesDir, classpaths)
      }

      log.info("Added {} generated source paths for module: {}", addedCount, module.path)
    } catch (e: Exception) {
      log.error("Failed to add Android-generated sources for module: {}", module.path, e)
    }
  }

  /**
   * Adds Kotlin scripting JARs from Gradle's cache These are needed for .kts file support and are
   * already downloaded by Gradle
   */
  private fun addKotlinScriptingJarsFromGradleCache(classpaths: MutableSet<String>) {
    try {
      // Gradle cache locations
      val gradleHomeDirs =
          listOf(
              File(System.getProperty("user.home", ""), ".gradle"),
              File("/data/data/com.tom.rv2ide/files/home/.gradle"),
              File("/storage/emulated/0/.gradle"),
              // Android app's own gradle cache
              File(System.getProperty("user.home", ""), "../../.gradle"),
          )

      val kotlinVersion = getKotlinVersionFromProject()
      log.info("Looking for Kotlin scripting JARs (version: {})", kotlinVersion ?: "any")

      val scriptingArtifacts =
          listOf(
              "kotlin-script-runtime",
              "kotlin-scripting-common",
              "kotlin-scripting-jvm",
              "kotlin-scripting-compiler-embeddable",
          )

      var foundCount = 0

      for (gradleHome in gradleHomeDirs) {
        if (!gradleHome.exists()) continue

        val modulesCache = File(gradleHome, "caches/modules-2/files-2.1/org.jetbrains.kotlin")
        if (!modulesCache.exists()) {
          log.debug("Gradle cache not found at: {}", modulesCache.absolutePath)
          continue
        }

        log.info("Scanning Gradle cache: {}", modulesCache.absolutePath)

        scriptingArtifacts.forEach { artifactName ->
          val artifactDir = File(modulesCache, artifactName)

          if (artifactDir.exists()) {
            // Find the version directory (prefer matching kotlin version if detected)
            val versionDirs = artifactDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

            val preferredVersion =
                if (kotlinVersion != null) {
                  versionDirs.find { it.name == kotlinVersion }
                } else {
                  null
                }

            val versionDir = preferredVersion ?: versionDirs.maxByOrNull { it.name }

            if (versionDir != null) {
              // Navigate to the JAR: version/hash/artifact-version.jar
              versionDir.listFiles()?.forEach { hashDir ->
                if (hashDir.isDirectory) {
                  hashDir.listFiles()?.forEach { file ->
                    if (
                        file.isFile &&
                            file.extension == "jar" &&
                            !file.name.contains("sources") &&
                            !file.name.contains("javadoc")
                    ) {

                      classpaths.add(file.absolutePath)
                      foundCount++
                      log.info("✓ Added Kotlin scripting JAR: {}", file.name)
                    }
                  }
                }
              }
            }
          }
        }

        // If we found JARs in this Gradle home, no need to check others
        if (foundCount > 0) {
          log.info("Found {} Kotlin scripting JARs in Gradle cache", foundCount)
          break
        }
      }

      if (foundCount == 0) {
        log.warn("⚠ No Kotlin scripting JARs found in Gradle cache!")
        log.warn("⚠ This might happen if:")
        log.warn("⚠   1. Gradle hasn't been run yet (run a build first)")
        log.warn("⚠   2. Using a custom Gradle installation")
        log.warn("⚠   3. Gradle cache was cleared")
      }
    } catch (e: Exception) {
      log.error("Failed to add Kotlin scripting JARs from Gradle cache", e)
    }
  }

  /** Attempts to detect the Kotlin version used in the project */
  private fun getKotlinVersionFromProject(): String? {
    try {
      val projectManager = IProjectManager.getInstance()
      val workspace = projectManager.getWorkspace() ?: return null

      // Check build.gradle.kts for kotlin version
      val rootProject = workspace.getRootProject()
      val buildFile = File(rootProject.path, "build.gradle.kts")

      if (buildFile.exists()) {
        val content = buildFile.readText()

        // Look for kotlin("jvm") version or kotlin plugin version
        val versionRegex = """kotlin\("jvm"\)\s+version\s+"([^"]+)"""".toRegex()
        val match = versionRegex.find(content)
        if (match != null) {
          val version = match.groupValues[1]
          log.info("Detected Kotlin version from build.gradle.kts: {}", version)
          return version
        }

        // Alternative pattern: id("org.jetbrains.kotlin.jvm") version "x.y.z"
        val altRegex = """id\("org\.jetbrains\.kotlin\.[^"]+"\)\s+version\s+"([^"]+)"""".toRegex()
        val altMatch = altRegex.find(content)
        if (altMatch != null) {
          val version = altMatch.groupValues[1]
          log.info("Detected Kotlin version: {}", version)
          return version
        }
      }

      // Fallback: check gradle.properties or libs.versions.toml
      val propertiesFile = File(rootProject.path, "gradle.properties")
      if (propertiesFile.exists()) {
        val props = java.util.Properties()
        propertiesFile.inputStream().use { props.load(it) }
        val version = props.getProperty("kotlin.version") ?: props.getProperty("kotlinVersion")
        if (version != null) {
          log.info("Detected Kotlin version from gradle.properties: {}", version)
          return version
        }
      }
    } catch (e: Exception) {
      log.debug("Could not detect Kotlin version", e)
    }

    return null
  }

  /**
   * Adds external library JARs from Gradle's resolved dependencies This includes
   * kotlin-script-runtime and other Gradle dependencies
   */
  private fun addExternalLibraryJars(buildDir: File, classpaths: MutableSet<String>) {
    try {
      // AGP stores resolved external JARs in these locations:
      val externalLibLocations =
          listOf(
              // AGP 7.0+
              "intermediates/external_libs_dex/debug",
              "intermediates/external_file_lib_dex_archives/debug",

              // Compile classpath JARs
              "intermediates/compile_library_classes_jar/debug",
              "intermediates/compile_app_classes_jar/debug",

              // Runtime classpath
              "intermediates/runtime_library_classes_jar/debug",

              // Transforms (older AGP versions)
              "intermediates/transforms/mergeJavaRes/debug",

              // AAR extracted JARs
              "intermediates/aar_libs_jars/debug",
          )

      var foundScriptRuntime = false

      externalLibLocations.forEach { location ->
        val dir = File(buildDir, location)
        if (dir.exists() && dir.isDirectory) {
          // Recursively find all JARs
          dir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "jar") {
              classpaths.add(file.absolutePath)

              // Check if this is the script runtime
              if (
                  file.name.contains("kotlin-script-runtime") ||
                      file.name.contains("kotlin-scripting")
              ) {
                foundScriptRuntime = true
                log.info("✓ Found Kotlin script JAR: {}", file.name)
              }
            }
          }
        }
      }

      if (!foundScriptRuntime) {
        log.warn("⚠ kotlin-script-runtime NOT found in build artifacts!")
        log.warn("⚠ Make sure your build.gradle.kts includes:")
        log.warn("⚠   implementation(\"org.jetbrains.kotlin:kotlin-script-runtime:<version>\")")
      }
    } catch (e: Exception) {
      log.error("Failed to add external library JARs", e)
    }
  }

  /** Recursively scans directories for Java/Kotlin source files */
  private fun scanForSourceDirectories(dir: File, classpaths: MutableSet<String>, maxDepth: Int) {
    if (maxDepth <= 0) return

    try {
      val files = dir.listFiles() ?: return

      // Check if current directory contains source files
      val hasSourceFiles =
          files.any { it.isFile && (it.extension == "java" || it.extension == "kt") }

      if (hasSourceFiles && !classpaths.contains(dir.absolutePath)) {
        classpaths.add(dir.absolutePath)
        log.debug("Discovered source directory: {}", dir.absolutePath)
      }

      // Recurse into subdirectories
      files
          .filter { it.isDirectory }
          .forEach { subDir -> scanForSourceDirectories(subDir, classpaths, maxDepth - 1) }
    } catch (e: Exception) {
      log.debug("Error scanning directory: {}", dir.absolutePath, e)
    }
  }

  /** Find compiled .class directories in intermediates */
  private fun findCompiledClassDirectories(intermediatesDir: File, classpaths: MutableSet<String>) {
    try {
      val classDirectories =
          listOf(
              "compile_library_classes_jar/debug/classes.jar",
              "compile_app_classes_jar/debug/classes.jar",
              "transforms/classes/debug",
              "javac/debug/classes",
              "kotlin-classes/debug",
          )

      classDirectories.forEach { path ->
        val dir = File(intermediatesDir, path)
        if (dir.exists()) {
          classpaths.add(dir.absolutePath)
          log.info("✓ Added compiled classes: {}", path)
        }
      }
    } catch (e: Exception) {
      log.debug("Error finding compiled class directories", e)
    }
  }

  fun getAndroidSdkPath(): String {
    // First try from compiler service
    val serviceResult =
        try {
          compilerService?.let { service ->
            val bootClassPaths = service.getFileManager().getBootClassPaths()
            val androidJar = bootClassPaths.find { it.name == "android.jar" }
            androidJar?.parentFile?.parentFile?.parentFile?.absolutePath
          }
        } catch (e: Exception) {
          log.error("Failed to get Android SDK path from compiler service", e)
          null
        }

    if (!serviceResult.isNullOrEmpty()) {
      return serviceResult
    }

    // Fallback to project system
    return try {
      val projectManager = IProjectManager.getInstance()
      val workspace = projectManager.getWorkspace()

      if (workspace != null) {
        val androidModules = workspace.androidProjects()
        val firstModule = androidModules.firstOrNull()

        if (firstModule != null) {
          val androidJar = firstModule.bootClassPaths.find { it.name == "android.jar" }
          if (androidJar != null) {
            val platformDir = androidJar.parentFile
            if (platformDir != null) {
              val sdkRoot = platformDir.parentFile
              if (sdkRoot != null) {
                return sdkRoot.absolutePath
              }
            }
          }
        }
      }
      ""
    } catch (e: Exception) {
      log.error("Failed to get Android SDK path from project system", e)
      ""
    }
  }

  /** Lists all classes available in the current classpath. */
  fun listClassesInClasspath(): Set<ClassInfo> {
    val classpathFiles = getClasspathList().map { File(it) }.filter { it.exists() }
    return try {
      classpathReader.listClasses(classpathFiles).toSet()
    } catch (e: Exception) {
      log.error("Failed to list classes in classpath", e)
      emptySet()
    }
  }

  /** Gets the module classpath for a specific project path. */
  fun getModuleClasspath(projectPath: String): List<String> {
    return try {
      val projectManager = IProjectManager.getInstance()
      val workspace = projectManager.getWorkspace() ?: return emptyList()

      val module = workspace.findProject(projectPath) as? ModuleProject ?: return emptyList()

      val classpaths = mutableListOf<String>()
      for (cp in module.getModuleClasspaths()) {
        classpaths.add(cp.absolutePath)
      }
      classpaths
    } catch (e: Exception) {
      log.error("Failed to get module classpath for project: $projectPath", e)
      emptyList()
    }
  }

  /** Gets all source directories from the project system. */
  fun getSourceDirectories(): List<String> {
    return try {
      val projectManager = IProjectManager.getInstance()
      val workspace = projectManager.getWorkspace() ?: return emptyList()

      val sourceDirs = mutableSetOf<String>()

      val allProjects = mutableListOf(workspace.getRootProject())
      allProjects.addAll(workspace.getSubProjects())

      for (project in allProjects) {
        if (project is ModuleProject) {
          for (sourceDir in project.getSourceDirectories()) {
            sourceDirs.add(sourceDir.absolutePath)
          }
        }
      }

      sourceDirs.toList()
    } catch (e: Exception) {
      log.error("Failed to get source directories", e)
      emptyList()
    }
  }

  /** Invalidate the classpath cache - call this when build completes */
  fun invalidateCache() {
    cachedClasspathList = null
    cachedClasspath = null
    log.info("Classpath cache invalidated")
  }
}
