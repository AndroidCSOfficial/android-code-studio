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

package com.tom.rv2ide.services.ai

import com.tom.rv2ide.projects.android.AndroidModule
import java.io.File
import org.slf4j.LoggerFactory

/**
 * ProjectContextManager that provides better file structure awareness and prevents AI confusion
 * about project layout and file types.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class ProjectContextManager {

  companion object {
    private val log = LoggerFactory.getLogger(ProjectContextManager::class.java)

    @Volatile private var instance: ProjectContextManager? = null

    fun getInstance(): ProjectContextManager {
      return instance
          ?: synchronized(this) { instance ?: ProjectContextManager().also { instance = it } }
    }
  }

  private var cachedContext: ProjectContextInfo? = null
  private var lastAnalysisTime: Long = 0
  private val cacheValidityDuration = 10 * 60 * 1000L // 10 minutes

  // File mapping to prevent duplicates and confusion
  private val fileRegistry = mutableMapOf<String, FileInfo>()

  data class FileInfo(
      val absolutePath: String,
      val relativePath: String,
      val fileName: String,
      val fileType: FileType,
      val module: String?,
      val packagePath: String?,
  )

  enum class FileType {
    BUILD_GRADLE_KTS,
    BUILD_GRADLE_GROOVY,
    KOTLIN_SOURCE,
    JAVA_SOURCE,
    XML_LAYOUT,
    XML_MANIFEST,
    XML_RESOURCE,
    PROPERTIES,
    JSON,
    OTHER,
  }

  data class ProjectContextInfo(
      val projectType: ProjectType,
      val projectStructure: String,
      val fileRegistry: Map<String, FileInfo>, // Complete file registry
      val buildSystemInfo: BuildSystemInfo, // Detailed build system info
      val androidModules: List<AndroidModule>,
      val packageName: String?,
      val targetSdk: Int?,
      val compileSDK: Int?,
      val minSDK: Int?,
      val usesCompose: Boolean,
      val usesViewBinding: Boolean,
      val usesDataBinding: Boolean,
      val usesKotlin: Boolean,
      val usesJava: Boolean,
      val architecturePattern: ArchitecturePattern,
      val dependencyInjection: DependencyInjectionFramework,
      val keyDependencies: List<String>,
      val resourceDirectories: Map<String, File>,
      val sourceDirectories: List<File>,
      val manifestInfo: ManifestInfo,
      val strictFileMapping: Map<String, String>, // filename -> full path mapping
  )

  data class BuildSystemInfo(
      val rootBuildFile: String?, // "build.gradle" or "build.gradle.kts"
      val moduleBuildFiles: Map<String, String>, // module -> build file name
      val settingsFile: String?, // "settings.gradle" or "settings.gradle.kts"
      val usesKts: Boolean,
      val applicationId: String?,
      val versionName: String?,
      val versionCode: Int?,
      val buildTypes: List<String>,
      val productFlavors: List<String>,
  )

  enum class ProjectType {
    COMPOSE_ONLY,
    VIEW_BASED_ONLY,
    HYBRID_COMPOSE_VIEW,
    LIBRARY,
    UNKNOWN,
  }

  enum class ArchitecturePattern {
    MVP,
    MVVM,
    MVI,
    CLEAN_ARCHITECTURE,
    TRADITIONAL,
    UNKNOWN,
  }

  enum class DependencyInjectionFramework {
    DAGGER_HILT,
    DAGGER,
    KOIN,
    MANUAL,
    NONE,
  }

  data class ManifestInfo(
      val manifestPath: String,
      val permissions: List<String>,
      val activities: List<String>,
      val services: List<String>,
      val receivers: List<String>,
  )

  /** Get project context with file mapping */
  fun getProjectContext(
      projectRoot: File,
      androidModules: List<AndroidModule>,
      forceRefresh: Boolean = false,
  ): ProjectContextInfo {
    val currentTime = System.currentTimeMillis()

    if (
        !forceRefresh &&
            cachedContext != null &&
            (currentTime - lastAnalysisTime) < cacheValidityDuration
    ) {
      log.debug("Using cached project context")
      return cachedContext!!
    }

    log.info("Performing project analysis...")

    // Clear and rebuild file registry
    fileRegistry.clear()

    val context = analyzeProjectContext(projectRoot, androidModules)

    cachedContext = context
    lastAnalysisTime = currentTime

    // Update system instructions with context
    updateSystemInstructionsWithContext(context)

    log.info("Project analysis complete: Files mapped=${context.fileRegistry.size}")
    return context
  }

  /** project analysis with file registry building */
  private fun analyzeProjectContext(
      projectRoot: File,
      androidModules: List<AndroidModule>,
  ): ProjectContextInfo {

    // Build complete file registry first
    val completeFileRegistry = buildCompleteFileRegistry(projectRoot, androidModules)

    // Analyze build system with precise file detection
    val buildSystemInfo = analyzeBuildSystem(projectRoot, androidModules)

    // Detect project type with better accuracy
    val projectType = detectProjectType(projectRoot, androidModules, completeFileRegistry)

    // Analyze other project aspects
    val usesCompose = detectComposeUsage(completeFileRegistry)
    val usesViewBinding = detectViewBinding(completeFileRegistry)
    val usesDataBinding = detectDataBinding(completeFileRegistry)
    val usesKotlin = completeFileRegistry.values.any { it.fileType == FileType.KOTLIN_SOURCE }
    val usesJava = completeFileRegistry.values.any { it.fileType == FileType.JAVA_SOURCE }

    val architecturePattern = detectArchitecturePattern(completeFileRegistry)
    val dependencyInjection = detectDependencyInjection(completeFileRegistry)
    val keyDependencies = extractKeyDependencies(completeFileRegistry)

    // Build project structure description
    val projectStructure = buildProjectStructure(projectRoot, androidModules, completeFileRegistry)

    // Get resource directories
    val resourceDirectories = getResourceDirectories(androidModules)
    val sourceDirectories = getSourceDirectories(androidModules)

    // Analyze manifest
    val manifestInfo = analyzeManifest(androidModules, completeFileRegistry)

    // Create strict file mapping to prevent duplicates
    val strictFileMapping = createStrictFileMapping(completeFileRegistry)

    return ProjectContextInfo(
        projectType = projectType,
        projectStructure = projectStructure,
        fileRegistry = completeFileRegistry,
        buildSystemInfo = buildSystemInfo,
        androidModules = androidModules,
        packageName = buildSystemInfo.applicationId,
        targetSdk = extractTargetSDK(androidModules, completeFileRegistry),
        compileSDK = extractCompileSDK(androidModules, completeFileRegistry),
        minSDK = extractMinSDK(androidModules, completeFileRegistry),
        usesCompose = usesCompose,
        usesViewBinding = usesViewBinding,
        usesDataBinding = usesDataBinding,
        usesKotlin = usesKotlin,
        usesJava = usesJava,
        architecturePattern = architecturePattern,
        dependencyInjection = dependencyInjection,
        keyDependencies = keyDependencies,
        resourceDirectories = resourceDirectories,
        sourceDirectories = sourceDirectories,
        manifestInfo = manifestInfo,
        strictFileMapping = strictFileMapping,
    )
  }

  /** Build complete file registry to prevent confusion */
  private fun buildCompleteFileRegistry(
      projectRoot: File,
      androidModules: List<AndroidModule>,
  ): Map<String, FileInfo> {
    val registry = mutableMapOf<String, FileInfo>()

    log.debug("Building complete file registry...")

    // Register root level files
    registerFilesInDirectory(projectRoot, registry, null, "")

    // Register module files
    androidModules.forEach { module ->
      val moduleRelativePath = module.projectDir.relativeTo(projectRoot).path
      registerFilesInDirectory(module.projectDir, registry, module.path, moduleRelativePath)

      // Register source directories
      module.mainSourceSet?.sourceProvider?.let { sourceProvider ->
        sourceProvider.javaDirectories.forEach { javaDir ->
          if (javaDir.exists()) {
            val relativePath = javaDir.relativeTo(projectRoot).path
            registerSourceFiles(javaDir, registry, module.path, relativePath)
          }
        }

        sourceProvider.resDirectories?.forEach { resDir ->
          if (resDir.exists()) {
            val relativePath = resDir.relativeTo(projectRoot).path
            registerResourceFiles(resDir, registry, module.path, relativePath)
          }
        }
      }
    }

    log.info("File registry built with ${registry.size} files")
    return registry
  }

  /** Register files in a directory with proper categorization */
  private fun registerFilesInDirectory(
      directory: File,
      registry: MutableMap<String, FileInfo>,
      modulePath: String?,
      relativePath: String,
  ) {
    if (!directory.exists() || !directory.isDirectory) return

    directory.listFiles()?.forEach { file ->
      if (file.isFile) {
        val fileType = determineFileType(file)
        val fileRelativePath =
            if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"

        // Use absolute path as key to avoid conflicts
        val key = file.absolutePath

        registry[key] =
            FileInfo(
                absolutePath = file.absolutePath,
                relativePath = fileRelativePath,
                fileName = file.name,
                fileType = fileType,
                module = modulePath,
                packagePath = null,
            )

        log.debug("Registered file: ${file.name} -> $fileRelativePath (${fileType})")
      }
    }
  }

  /** Register source files with package detection */
  private fun registerSourceFiles(
      sourceDir: File,
      registry: MutableMap<String, FileInfo>,
      modulePath: String,
      basePath: String,
  ) {
    sourceDir
        .walkTopDown()
        .filter { it.isFile }
        .forEach { file ->
          val fileType = determineFileType(file)
          if (fileType == FileType.KOTLIN_SOURCE || fileType == FileType.JAVA_SOURCE) {
            val relativePath = file.relativeTo(sourceDir).path
            val packagePath = extractPackageFromSourceFile(file)
            val fullRelativePath = "$basePath/$relativePath"

            registry[file.absolutePath] =
                FileInfo(
                    absolutePath = file.absolutePath,
                    relativePath = fullRelativePath,
                    fileName = file.name,
                    fileType = fileType,
                    module = modulePath,
                    packagePath = packagePath,
                )
          }
        }
  }

  /** Register resource files */
  private fun registerResourceFiles(
      resDir: File,
      registry: MutableMap<String, FileInfo>,
      modulePath: String,
      basePath: String,
  ) {
    resDir
        .walkTopDown()
        .filter { it.isFile }
        .forEach { file ->
          val fileType = determineFileType(file)
          val relativePath = file.relativeTo(resDir).path
          val fullRelativePath = "$basePath/$relativePath"

          registry[file.absolutePath] =
              FileInfo(
                  absolutePath = file.absolutePath,
                  relativePath = fullRelativePath,
                  fileName = file.name,
                  fileType = fileType,
                  module = modulePath,
                  packagePath = null,
              )
        }
  }

  /** Determine file type based on name and content */
  private fun determineFileType(file: File): FileType {
    return when {
      file.name == "build.gradle.kts" -> FileType.BUILD_GRADLE_KTS
      file.name == "build.gradle" -> FileType.BUILD_GRADLE_GROOVY
      file.name.endsWith(".kt") -> FileType.KOTLIN_SOURCE
      file.name.endsWith(".java") -> FileType.JAVA_SOURCE
      file.name == "AndroidManifest.xml" -> FileType.XML_MANIFEST
      file.name.endsWith(".xml") && file.parent?.endsWith("layout") == true -> FileType.XML_LAYOUT
      file.name.endsWith(".xml") -> FileType.XML_RESOURCE
      file.name.endsWith(".properties") -> FileType.PROPERTIES
      file.name.endsWith(".json") -> FileType.JSON
      else -> FileType.OTHER
    }
  }

  /** Extract package name from source file */
  private fun extractPackageFromSourceFile(file: File): String? {
    return try {
      val content = file.readText()
      val packageRegex = Regex("package\\s+([a-zA-Z_][a-zA-Z0-9_.]*)")
      packageRegex.find(content)?.groupValues?.get(1)
    } catch (e: Exception) {
      log.error("Error extracting package from ${file.name}", e)
      null
    }
  }

  /** Analyze build system with precise file detection */
  private fun analyzeBuildSystem(
      projectRoot: File,
      androidModules: List<AndroidModule>,
  ): BuildSystemInfo {
    // Detect root build file
    val rootBuildFile =
        when {
          File(projectRoot, "build.gradle.kts").exists() -> "build.gradle.kts"
          File(projectRoot, "build.gradle").exists() -> "build.gradle"
          else -> null
        }

    // Detect settings file
    val settingsFile =
        when {
          File(projectRoot, "settings.gradle.kts").exists() -> "settings.gradle.kts"
          File(projectRoot, "settings.gradle").exists() -> "settings.gradle"
          else -> null
        }

    val usesKts = rootBuildFile?.endsWith(".kts") == true

    // Map module build files
    val moduleBuildFiles =
        androidModules.associate { module ->
          val ktsFile = File(module.projectDir, "build.gradle.kts")
          val groovyFile = File(module.projectDir, "build.gradle")

          val buildFile =
              when {
                ktsFile.exists() -> "build.gradle.kts"
                groovyFile.exists() -> "build.gradle"
                else -> "build.gradle" // default assumption
              }

          module.path to buildFile
        }

    // Extract build configuration
    val appModule = androidModules.find { it.path == ":app" }
    var applicationId: String? = null
    var versionName: String? = null
    var versionCode: Int? = null

    appModule?.let { module ->
      val buildFile = File(module.projectDir, moduleBuildFiles[module.path] ?: "build.gradle")
      if (buildFile.exists()) {
        try {
          val content = buildFile.readText()
          applicationId = extractValueFromBuildFile(content, "applicationId")
          versionName = extractValueFromBuildFile(content, "versionName")
          versionCode = extractValueFromBuildFile(content, "versionCode")?.toIntOrNull()
        } catch (e: Exception) {
          log.error("Error parsing build file ${buildFile.name}", e)
        }
      }
    }

    return BuildSystemInfo(
        rootBuildFile = rootBuildFile,
        moduleBuildFiles = moduleBuildFiles,
        settingsFile = settingsFile,
        usesKts = usesKts,
        applicationId = applicationId,
        versionName = versionName,
        versionCode = versionCode,
        buildTypes = emptyList(), // TODO: extract build types
        productFlavors = emptyList(), // TODO: extract product flavors
    )
  }

  /** Extract value from build file content */
  private fun extractValueFromBuildFile(content: String, key: String): String? {
    val regex = Regex("$key\\s*=?\\s*[\"']([^\"']+)[\"']")
    return regex.find(content)?.groupValues?.get(1)
  }

  /** Create strict file mapping to prevent AI confusion */
  private fun createStrictFileMapping(fileRegistry: Map<String, FileInfo>): Map<String, String> {
    val mapping = mutableMapOf<String, String>()

    fileRegistry.values.forEach { fileInfo ->
      // Map by filename
      mapping[fileInfo.fileName] = fileInfo.relativePath

      // Map by filename without extension for certain files
      if (fileInfo.fileName.contains(".")) {
        val nameWithoutExt = fileInfo.fileName.substringBeforeLast(".")
        if (!mapping.containsKey(nameWithoutExt)) {
          mapping[nameWithoutExt] = fileInfo.relativePath
        }
      }
    }

    return mapping
  }

  /** project type detection using file registry */
  private fun detectProjectType(
      projectRoot: File,
      modules: List<AndroidModule>,
      fileRegistry: Map<String, FileInfo>,
  ): ProjectType {
    val layoutFiles = fileRegistry.values.filter { it.fileType == FileType.XML_LAYOUT }
    val kotlinFiles = fileRegistry.values.filter { it.fileType == FileType.KOTLIN_SOURCE }
    val buildFiles =
        fileRegistry.values.filter {
          it.fileType == FileType.BUILD_GRADLE_KTS || it.fileType == FileType.BUILD_GRADLE_GROOVY
        }

    var hasComposeDependencies = false
    var hasComposeCode = false

    // Check build files for Compose dependencies
    buildFiles.forEach { buildFileInfo ->
      try {
        val content = File(buildFileInfo.absolutePath).readText()
        if (
            content.contains("compose", ignoreCase = true) ||
                content.contains("androidx.compose") ||
                content.contains("compose-bom")
        ) {
          hasComposeDependencies = true
        }
      } catch (e: Exception) {
        log.error("Error reading build file ${buildFileInfo.fileName}", e)
      }
    }

    // Check Kotlin files for Compose code
    kotlinFiles.forEach { kotlinFile ->
      try {
        val content = File(kotlinFile.absolutePath).readText()
        if (
            content.contains("@Composable") ||
                content.contains("setContent") ||
                content.contains("androidx.compose")
        ) {
          hasComposeCode = true
        }
      } catch (e: Exception) {
        log.error("Error reading Kotlin file ${kotlinFile.fileName}", e)
      }
    }

    val hasLayouts = layoutFiles.isNotEmpty()
    val usesCompose = hasComposeDependencies || hasComposeCode

    return when {
      usesCompose && !hasLayouts -> ProjectType.COMPOSE_ONLY
      !usesCompose && hasLayouts -> ProjectType.VIEW_BASED_ONLY
      usesCompose && hasLayouts -> ProjectType.HYBRID_COMPOSE_VIEW
      else -> ProjectType.UNKNOWN
    }
  }

  //  detection methods using file registry
  private fun detectComposeUsage(fileRegistry: Map<String, FileInfo>): Boolean {
    // Implementation using file registry
    return fileRegistry.values.any { fileInfo ->
      when (fileInfo.fileType) {
        FileType.BUILD_GRADLE_KTS,
        FileType.BUILD_GRADLE_GROOVY -> {
          try {
            val content = File(fileInfo.absolutePath).readText()
            content.contains("compose", ignoreCase = true)
          } catch (e: Exception) {
            false
          }
        }
        FileType.KOTLIN_SOURCE -> {
          try {
            val content = File(fileInfo.absolutePath).readText()
            content.contains("@Composable") || content.contains("setContent")
          } catch (e: Exception) {
            false
          }
        }
        else -> false
      }
    }
  }

  private fun detectViewBinding(fileRegistry: Map<String, FileInfo>): Boolean {
    return fileRegistry.values.any { fileInfo ->
      if (
          fileInfo.fileType == FileType.BUILD_GRADLE_KTS ||
              fileInfo.fileType == FileType.BUILD_GRADLE_GROOVY
      ) {
        try {
          val content = File(fileInfo.absolutePath).readText()
          content.contains("viewBinding") && content.contains("true")
        } catch (e: Exception) {
          false
        }
      } else false
    }
  }

  private fun detectDataBinding(fileRegistry: Map<String, FileInfo>): Boolean {
    return fileRegistry.values.any { fileInfo ->
      if (
          fileInfo.fileType == FileType.BUILD_GRADLE_KTS ||
              fileInfo.fileType == FileType.BUILD_GRADLE_GROOVY
      ) {
        try {
          val content = File(fileInfo.absolutePath).readText()
          content.contains("dataBinding") && content.contains("true")
        } catch (e: Exception) {
          false
        }
      } else false
    }
  }

  private fun detectArchitecturePattern(fileRegistry: Map<String, FileInfo>): ArchitecturePattern {
    val sourceFiles =
        fileRegistry.values.filter {
          it.fileType == FileType.KOTLIN_SOURCE || it.fileType == FileType.JAVA_SOURCE
        }

    var hasViewModel = false
    var hasRepository = false
    var hasPresenter = false
    var hasUseCases = false

    sourceFiles.forEach { fileInfo ->
      val fileName = fileInfo.fileName.lowercase()
      when {
        fileName.contains("viewmodel") -> hasViewModel = true
        fileName.contains("repository") || fileName.contains("datasource") -> hasRepository = true
        fileName.contains("presenter") -> hasPresenter = true
        fileName.contains("usecase") || fileName.contains("interactor") -> hasUseCases = true
      }

      // Also check file content for ViewModel usage
      try {
        val content = File(fileInfo.absolutePath).readText()
        if (content.contains("ViewModel") || content.contains("AndroidViewModel")) {
          hasViewModel = true
        }
      } catch (e: Exception) {
        // Ignore file reading errors
      }
    }

    return when {
      hasUseCases && hasRepository && hasViewModel -> ArchitecturePattern.CLEAN_ARCHITECTURE
      hasViewModel && hasRepository -> ArchitecturePattern.MVVM
      hasPresenter -> ArchitecturePattern.MVP
      hasViewModel -> ArchitecturePattern.MVVM
      else -> ArchitecturePattern.TRADITIONAL
    }
  }

  private fun detectDependencyInjection(
      fileRegistry: Map<String, FileInfo>
  ): DependencyInjectionFramework {
    val buildFiles =
        fileRegistry.values.filter {
          it.fileType == FileType.BUILD_GRADLE_KTS || it.fileType == FileType.BUILD_GRADLE_GROOVY
        }

    buildFiles.forEach { buildFileInfo ->
      try {
        val content = File(buildFileInfo.absolutePath).readText()
        when {
          content.contains("dagger.hilt") || content.contains("hilt-android") ->
              return DependencyInjectionFramework.DAGGER_HILT
          content.contains("dagger") -> return DependencyInjectionFramework.DAGGER
          content.contains("koin") -> return DependencyInjectionFramework.KOIN
        }
      } catch (e: Exception) {
        log.error("Error reading build file ${buildFileInfo.fileName}", e)
      }
    }

    return DependencyInjectionFramework.NONE
  }

  private fun extractKeyDependencies(fileRegistry: Map<String, FileInfo>): List<String> {
    val dependencies = mutableSetOf<String>()
    val buildFiles =
        fileRegistry.values.filter {
          it.fileType == FileType.BUILD_GRADLE_KTS || it.fileType == FileType.BUILD_GRADLE_GROOVY
        }

    buildFiles.forEach { buildFileInfo ->
      try {
        val content = File(buildFileInfo.absolutePath).readText()

        // Extract key Android dependencies
        val dependencyRegex = Regex("implementation\\s+['\"]([^'\"]+)['\"]")
        dependencyRegex.findAll(content).forEach { match ->
          val dep = match.groupValues[1]
          if (
              dep.startsWith("androidx.") ||
                  dep.startsWith("com.google.android") ||
                  dep.contains("retrofit") ||
                  dep.contains("okhttp") ||
                  dep.contains("glide") ||
                  dep.contains("room") ||
                  dep.contains("navigation") ||
                  dep.contains("lifecycle")
          ) {
            dependencies.add(dep.substringBefore(":"))
          }
        }
      } catch (e: Exception) {
        log.error("Error extracting dependencies from ${buildFileInfo.fileName}", e)
      }
    }

    return dependencies.toList()
  }

  /** Build project structure description */
  private fun buildProjectStructure(
      projectRoot: File,
      androidModules: List<AndroidModule>,
      fileRegistry: Map<String, FileInfo>,
  ): String {
    return buildString {
      append("=== PROJECT STRUCTURE ===\n")

      // Group files by module
      val filesByModule = fileRegistry.values.groupBy { it.module ?: "root" }

      filesByModule.forEach { (module, files) ->
        append("\n[$module]\n")

        // Sort files by type and name
        val sortedFiles = files.sortedWith(compareBy({ it.fileType }, { it.fileName }))

        var currentType: FileType? = null
        sortedFiles.forEach { fileInfo ->
          if (currentType != fileInfo.fileType) {
            currentType = fileInfo.fileType
            append("  ${fileInfo.fileType}:\n")
          }
          append("    ${fileInfo.relativePath}\n")
        }
      }

      append("\n=== FILE TYPE SUMMARY ===\n")
      val fileTypeCounts = fileRegistry.values.groupBy { it.fileType }.mapValues { it.value.size }

      fileTypeCounts.forEach { (type, count) -> append("$type: $count files\n") }
    }
  }

  /** Update system instructions with context including file mappings */
  private fun updateSystemInstructionsWithContext(context: ProjectContextInfo) {
    val contextString = buildString {
      append("=== PROJECT CONTEXT ===\n")
      append("PROJECT TYPE: ${context.projectType}\n")

      if (context.packageName != null) {
        append("PACKAGE NAME: ${context.packageName}\n")
      }

      // Build system information
      append("\n=== BUILD SYSTEM ===\n")
      append("Root build file: ${context.buildSystemInfo.rootBuildFile}\n")
      append("Uses Kotlin DSL: ${context.buildSystemInfo.usesKts}\n")
      append("Settings file: ${context.buildSystemInfo.settingsFile}\n")

      context.buildSystemInfo.moduleBuildFiles.forEach { (module, buildFile) ->
        append("$module: $buildFile\n")
      }

      // File structure overview
      append("\n=== FILE STRUCTURE OVERVIEW ===\n")
      append("Total files registered: ${context.fileRegistry.size}\n")

      val fileTypeCounts =
          context.fileRegistry.values.groupBy { it.fileType }.mapValues { it.value.size }
      fileTypeCounts.forEach { (type, count) -> append("$type: $count\n") }

      // Critical file mappings to prevent confusion
      append("\n=== CRITICAL FILE MAPPINGS ===\n")
      context.strictFileMapping.forEach { (fileName, fullPath) ->
        if (
            fileName.contains("build.gradle") ||
                fileName.contains("MainActivity") ||
                fileName.contains("AndroidManifest")
        ) {
          append("'$fileName' -> '$fullPath'\n")
        }
      }

      // UI Framework information
      append("\n=== UI FRAMEWORK ===\n")
      when {
        context.usesCompose &&
            !context.fileRegistry.values.any { it.fileType == FileType.XML_LAYOUT } ->
            append("Jetpack Compose ONLY - NO XML layouts\n")
        context.usesCompose &&
            context.fileRegistry.values.any { it.fileType == FileType.XML_LAYOUT } ->
            append("HYBRID - Both Compose and XML layouts\n")
        else -> append("Traditional Views - XML layouts only\n")
      }

      append("LANGUAGES: ")
      val languages = mutableListOf<String>()
      if (context.usesKotlin) languages.add("Kotlin")
      if (context.usesJava) languages.add("Java")
      append(languages.joinToString(", "))
      append("\n")

      if (context.architecturePattern != ArchitecturePattern.UNKNOWN) {
        append("ARCHITECTURE: ${context.architecturePattern}\n")
      }

      if (context.dependencyInjection != DependencyInjectionFramework.NONE) {
        append("DEPENDENCY INJECTION: ${context.dependencyInjection}\n")
      }

      append("\n=== IMPORTANT NOTES ===\n")
      append("- Always use EXACT file paths from the mapping above\n")
      append("- Do NOT create duplicate build.gradle files\n")
      append("- Respect the project's UI framework (Compose vs Views)\n")
      append("- Place content in the correct file type and location\n")
    }

    com.tom.rv2ide.services.ai.Instructions.SystemInstructions.updateProjectContext(contextString)
  }

  // helper methods
  private fun getResourceDirectories(modules: List<AndroidModule>): Map<String, File> {
    val resourceDirs = mutableMapOf<String, File>()

    modules.forEach { module ->
      module.mainSourceSet?.sourceProvider?.resDirectories?.forEachIndexed { index, resDir ->
        if (resDir.exists()) {
          val key = "${module.path}/res$index"
          resourceDirs[key] = resDir
        }
      }
    }

    return resourceDirs
  }

  private fun getSourceDirectories(modules: List<AndroidModule>): List<File> {
    return modules.flatMap { module ->
      module.mainSourceSet?.sourceProvider?.javaDirectories?.filter { it.exists() } ?: emptyList()
    }
  }

  private fun analyzeManifest(
      modules: List<AndroidModule>,
      fileRegistry: Map<String, FileInfo>,
  ): ManifestInfo {
    val manifestFile = fileRegistry.values.find { it.fileType == FileType.XML_MANIFEST }

    if (manifestFile == null) {
      return ManifestInfo("", emptyList(), emptyList(), emptyList(), emptyList())
    }

    try {
      val content = File(manifestFile.absolutePath).readText()

      // Extract permissions
      val permissionRegex = Regex("<uses-permission\\s+android:name=\"([^\"]+)\"")
      val permissions = permissionRegex.findAll(content).map { it.groupValues[1] }.toList()

      // Extract activities
      val activityRegex = Regex("<activity\\s+[^>]*android:name=\"([^\"]+)\"")
      val activities = activityRegex.findAll(content).map { it.groupValues[1] }.toList()

      // Extract services
      val serviceRegex = Regex("<service\\s+[^>]*android:name=\"([^\"]+)\"")
      val services = serviceRegex.findAll(content).map { it.groupValues[1] }.toList()

      // Extract receivers
      val receiverRegex = Regex("<receiver\\s+[^>]*android:name=\"([^\"]+)\"")
      val receivers = receiverRegex.findAll(content).map { it.groupValues[1] }.toList()

      return ManifestInfo(
          manifestPath = manifestFile.relativePath,
          permissions = permissions,
          activities = activities,
          services = services,
          receivers = receivers,
      )
    } catch (e: Exception) {
      log.error("Error analyzing manifest file", e)
      return ManifestInfo(
          manifestFile.relativePath,
          emptyList(),
          emptyList(),
          emptyList(),
          emptyList(),
      )
    }
  }

  private fun extractTargetSDK(
      modules: List<AndroidModule>,
      fileRegistry: Map<String, FileInfo>,
  ): Int? {
    return extractSDKValue(fileRegistry, "targetSdkVersion", "targetSdk")
  }

  private fun extractCompileSDK(
      modules: List<AndroidModule>,
      fileRegistry: Map<String, FileInfo>,
  ): Int? {
    return extractSDKValue(fileRegistry, "compileSdkVersion", "compileSdk")
  }

  private fun extractMinSDK(
      modules: List<AndroidModule>,
      fileRegistry: Map<String, FileInfo>,
  ): Int? {
    return extractSDKValue(fileRegistry, "minSdkVersion", "minSdk")
  }

  private fun extractSDKValue(fileRegistry: Map<String, FileInfo>, vararg keys: String): Int? {
    val buildFiles =
        fileRegistry.values.filter {
          it.fileType == FileType.BUILD_GRADLE_KTS || it.fileType == FileType.BUILD_GRADLE_GROOVY
        }

    buildFiles.forEach { buildFileInfo ->
      try {
        val content = File(buildFileInfo.absolutePath).readText()

        keys.forEach { key ->
          val regex = Regex("$key\\s*=?\\s*(\\d+)")
          val match = regex.find(content)
          if (match != null) {
            return match.groupValues[1].toIntOrNull()
          }
        }
      } catch (e: Exception) {
        log.error("Error extracting SDK values from ${buildFileInfo.fileName}", e)
      }
    }

    return null
  }

  /** Clear cached context and file registry */
  fun clearCache() {
    cachedContext = null
    lastAnalysisTime = 0
    fileRegistry.clear()
    com.tom.rv2ide.services.ai.Instructions.SystemInstructions.clearCache()
    log.info("project context cache cleared")
  }

  /** Get a formatted context summary for AI services with file mappings */
  fun getContextSummary(): String? {
    return cachedContext?.let { context ->
      buildString {
        append("=== PROJECT SUMMARY ===\n")
        append("Type: ${context.projectType}\n")
        append(
            "Build System: ${if (context.buildSystemInfo.usesKts) "Kotlin DSL" else "Groovy DSL"}\n"
        )
        append(
            "UI: ${when {
                    context.usesCompose && context.fileRegistry.values.none { it.fileType == FileType.XML_LAYOUT } -> "Compose Only"
                    context.usesCompose && context.fileRegistry.values.any { it.fileType == FileType.XML_LAYOUT } -> "Hybrid (Compose + Views)"
                    else -> "Traditional Views"
                }}\n"
        )
        append("Architecture: ${context.architecturePattern}\n")

        if (context.packageName != null) {
          append("Package: ${context.packageName}\n")
        }

        append("Languages: ")
        val langs = mutableListOf<String>()
        if (context.usesKotlin) langs.add("Kotlin")
        if (context.usesJava) langs.add("Java")
        append(langs.joinToString(", "))
        append("\n")

        append("Files: ${context.fileRegistry.size} total\n")

        // Add critical file paths to prevent confusion
        append("\n=== KEY FILES ===\n")
        context.fileRegistry.values
            .filter { fileInfo ->
              fileInfo.fileName.startsWith("build.gradle") ||
                  fileInfo.fileName == "AndroidManifest.xml" ||
                  fileInfo.fileName.contains("MainActivity")
            }
            .forEach { fileInfo -> append("${fileInfo.fileName} -> ${fileInfo.relativePath}\n") }
      }
    }
  }

  /** Get specific file path to prevent AI confusion */
  fun getFilePathForName(fileName: String): String? {
    return cachedContext?.strictFileMapping?.get(fileName)
  }

  /** Validate that a file path matches the expected file type */
  fun validateFilePath(fileName: String, expectedPath: String): Boolean {
    val actualPath = getFilePathForName(fileName)
    return actualPath == expectedPath
  }

  /** Get all files of a specific type */
  fun getFilesByType(fileType: FileType): List<FileInfo> {
    return cachedContext?.fileRegistry?.values?.filter { it.fileType == fileType } ?: emptyList()
  }

  /** Check if project has specific file */
  fun hasFile(fileName: String): Boolean {
    return cachedContext?.fileRegistry?.values?.any { it.fileName == fileName } == true
  }

  /** Get module for a specific file */
  fun getModuleForFile(fileName: String): String? {
    return cachedContext?.fileRegistry?.values?.find { it.fileName == fileName }?.module
  }
}
