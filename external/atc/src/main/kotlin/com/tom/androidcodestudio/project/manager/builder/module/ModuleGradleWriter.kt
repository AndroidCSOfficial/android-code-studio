/*
 *  This file is part of AndroidTC.
 *
 *  AndroidTC is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidTC is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with AndroidTC.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.androidcodestudio.project.manager.builder.module

import java.io.File

/**
 * Enum representing Java version compatibility.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
enum class JavaVersion(val versionNumber: String, val versionName: String) {
  VERSION_1_8("1.8", "VERSION_1_8"),
  VERSION_11("11", "VERSION_11"),
  VERSION_17("17", "VERSION_17"),
  VERSION_21("21", "VERSION_21");

  fun toJvmName(): String {
    return versionName.replace("VERSION_", "JVM_")
  }

  companion object {
    fun fromVersionNumber(version: String): JavaVersion {
      return entries.find { it.versionNumber == version }
          ?: throw IllegalArgumentException("Unsupported Java version: $version")
    }
  }
}

/** Enum representing build feature types. */
enum class BuildFeature(val featureName: String) {
  VIEW_BINDING("viewBinding"),
  DATA_BINDING("dataBinding"),
  COMPOSE("compose"),
  BUILD_CONFIG("buildConfig"),
  PREFAB("prefab"),
  AIDL("aidl"),
  RENDER_SCRIPT("renderScript"),
  RES_VALUES("resValues"),
  SHADERS("shaders"),
  ML_MODEL_BINDING("mlModelBinding"),
}

/**
 * Data class representing CMake configuration.
 *
 * @property path Path to CMakeLists.txt file (e.g., "src/main/cpp/CMakeLists.txt")
 * @property version CMake version (optional, e.g., "3.22.1")
 * @property arguments Additional CMake arguments (optional)
 * @property cFlags C compiler flags (optional)
 * @property cppFlags C++ compiler flags (optional)
 * @property abiFilters ABI filters specific to CMake (optional, overrides NDK abiFilters if set)
 * @property targets Build targets (optional)
 */
data class CMakeConfig(
    val path: String,
    val version: String? = null,
    val arguments: List<String> = emptyList(),
    val cFlags: String? = null,
    val cppFlags: String? = null,
    val abiFilters: List<String> = emptyList(),
    val targets: List<String> = emptyList(),
)

/**
 * Data class representing ndk-build configuration.
 *
 * @property path Path to Android.mk file (e.g., "src/main/cpp/Android.mk")
 * @property abiFilters ABI filters specific to ndk-build (optional)
 */
data class NdkBuildConfig(val path: String, val abiFilters: List<String> = emptyList())

/**
 * Data class representing external native build configuration. Only one of cmake or ndkBuild should
 * be set.
 *
 * @property cmake CMake configuration
 * @property ndkBuild NDK Build configuration
 */
data class ExternalNativeBuild(
    val cmake: CMakeConfig? = null,
    val ndkBuild: NdkBuildConfig? = null,
) {
  init {
    require(cmake != null || ndkBuild != null) {
      "At least one of cmake or ndkBuild must be configured"
    }
    require(!(cmake != null && ndkBuild != null)) {
      "Cannot configure both cmake and ndkBuild simultaneously"
    }
  }
}

/**
 * Data class representing NDK configuration in defaultConfig.
 *
 * @property abiFilters List of ABI filters (e.g., ["armeabi-v7a", "arm64-v8a", "x86", "x86_64"])
 */
data class NdkConfig(val abiFilters: List<String> = emptyList())

/**
 * Data class representing a Gradle dependency. User provides the complete dependency string.
 *
 * Examples:
 * - GradleDependency("implementation(libs.androidx.core.ktx)")
 * - GradleDependency("implementation(\"androidx.appcompat:appcompat:1.7.1\")")
 * - GradleDependency("testImplementation(libs.junit)")
 */
data class GradleDependency(val dependency: String)

/**
 * Data class representing default config settings.
 *
 * @property applicationId The application ID
 * @property minSdk Minimum SDK version
 * @property targetSdk Target SDK version
 * @property versionCode Version code
 * @property versionName Version name
 * @property testInstrumentationRunner Test instrumentation runner (null to exclude)
 * @property ndk NDK configuration for ABI filters in defaultConfig
 */
data class DefaultConfig(
    val applicationId: String,
    val minSdk: Int,
    val targetSdk: Int,
    val versionCode: Int = 1,
    val versionName: String = "1.0",
    val testInstrumentationRunner: String? = null,
    val ndk: NdkConfig? = null,
)

/** Data class representing the module-level Gradle configuration. */
data class ModuleGradleConfig(
    val plugins: List<GradlePlugin>,
    val namespace: String,
    val compileSdk: Int,
    val defaultConfig: DefaultConfig,
    val buildFeatures: List<BuildFeature> = emptyList(),
    val javaVersion: JavaVersion = JavaVersion.VERSION_17,
    val enableKotlinOptions: Boolean = true,
    val enableCompose: Boolean = false,
    val composeCompilerVersion: String? = null,
    val dependencies: List<GradleDependency> = emptyList(),
    val externalNativeBuild: ExternalNativeBuild? = null,
    val ndkVersion: String? = null,
)

/**
 * Data class representing a Gradle plugin. User provides the type and plugin string.
 *
 * Examples:
 * - GradlePlugin("id", "com.android.application")
 * - GradlePlugin("alias", "libs.plugins.android.application")
 * - GradlePlugin("anything", "any.plugin.notation")
 *
 * @property type The plugin type (e.g., "id", "alias", or any custom type)
 * @property plugin The plugin notation string
 */
data class GradlePlugin(val type: String, val plugin: String)

/** Enum representing supported Gradle file types. */
enum class GradleFileType(val extension: String, val fileName: String) {
  GROOVY("gradle", "build.gradle"),
  KTS("gradle.kts", "build.gradle.kts"),
}

/** Interface for writing module-level Gradle build files. */
interface IMLGradleWriter {

  /**
   * Generates the module-level build.gradle file content.
   *
   * @param fileType The type of Gradle file (Groovy or KTS)
   * @param config The module Gradle configuration
   * @return The generated build file content as a String
   */
  fun generate(fileType: GradleFileType, config: ModuleGradleConfig): String

  /**
   * Writes the generated content to a file.
   *
   * @param outputDir The directory where the build file will be created
   * @param fileType The type of Gradle file (Groovy or KTS)
   * @param config The module Gradle configuration
   * @return The created File object
   */
  fun writeToFile(outputDir: File, fileType: GradleFileType, config: ModuleGradleConfig): File
}

/** Implementation of IMLGradleWriter for generating module-level Gradle build files. */
class MLGradleWriter : IMLGradleWriter {

  override fun generate(fileType: GradleFileType, config: ModuleGradleConfig): String {
    return when (fileType) {
      GradleFileType.GROOVY -> generateGroovy(config)
      GradleFileType.KTS -> generateKts(config)
    }
  }

  override fun writeToFile(
      outputDir: File,
      fileType: GradleFileType,
      config: ModuleGradleConfig,
  ): File {
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val content = generate(fileType, config)
    val file = File(outputDir, fileType.fileName)
    file.writeText(content)

    return file
  }

  /** Generates Groovy-style build.gradle content. */
  private fun generateGroovy(config: ModuleGradleConfig): String {
    val builder = StringBuilder()

    // Plugins block
    if (config.plugins.isNotEmpty()) {
      builder.appendLine("plugins {")
      builder.appendLine("    // Plugins automatically generated by MLGradleWriter.kt")
      config.plugins.forEach { plugin ->
        builder.appendLine("    ${buildPluginLine(plugin, false)}")
      }
      builder.appendLine("}")
      builder.appendLine()
    }

    // Android block
    builder.appendLine("android {")
    builder.appendLine("    namespace '${config.namespace}'")
    builder.appendLine("    compileSdk ${config.compileSdk}")

    // NDK version (top-level in android block)
    config.ndkVersion?.let { builder.appendLine("    ndkVersion '$it'") }

    builder.appendLine()

    // Default config
    builder.appendLine("    defaultConfig {")
    builder.appendLine("        applicationId '${config.defaultConfig.applicationId}'")
    builder.appendLine("        minSdk ${config.defaultConfig.minSdk}")
    builder.appendLine("        targetSdk ${config.defaultConfig.targetSdk}")
    builder.appendLine("        versionCode ${config.defaultConfig.versionCode}")
    builder.appendLine("        versionName '${config.defaultConfig.versionName}'")

    config.defaultConfig.testInstrumentationRunner?.let {
      builder.appendLine()
      builder.appendLine("        testInstrumentationRunner '$it'")
    }

    // NDK ABI filters in defaultConfig
    config.defaultConfig.ndk?.let { ndkConfig ->
      if (ndkConfig.abiFilters.isNotEmpty()) {
        builder.appendLine()
        builder.appendLine("        ndk {")
        builder.append("            abiFilters ")
        builder.appendLine(ndkConfig.abiFilters.joinToString(", ") { "'$it'" })
        builder.appendLine("        }")
      }
    }

    builder.appendLine("    }")

    // Build features
    if (config.buildFeatures.isNotEmpty()) {
      builder.appendLine()
      builder.appendLine("    buildFeatures {")
      config.buildFeatures.forEach { feature ->
        builder.appendLine("        ${feature.featureName} true")
      }
      builder.appendLine("    }")
    }

    // External native build
    config.externalNativeBuild?.let { nativeBuild ->
      builder.appendLine()
      builder.appendLine("    externalNativeBuild {")

      nativeBuild.cmake?.let { cmake ->
        builder.appendLine("        cmake {")
        builder.appendLine("            path '${cmake.path}'")
        cmake.version?.let { version -> builder.appendLine("            version '$version'") }
        builder.appendLine("        }")
      }

      nativeBuild.ndkBuild?.let { ndkBuild ->
        builder.appendLine("        ndkBuild {")
        builder.appendLine("            path '${ndkBuild.path}'")
        builder.appendLine("        }")
      }

      builder.appendLine("    }")
    }

    // Compile options
    builder.appendLine()
    builder.appendLine("    compileOptions {")
    builder.appendLine("        sourceCompatibility JavaVersion.${config.javaVersion.versionName}")
    builder.appendLine("        targetCompatibility JavaVersion.${config.javaVersion.versionName}")
    builder.appendLine("    }")

    // Kotlin options
    if (config.enableKotlinOptions) {
      builder.appendLine("    kotlin {")
      builder.appendLine("        compilerOptions {")
      builder.appendLine(
          "            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.${config.javaVersion.toJvmName()})"
      )
      builder.appendLine("        }")
      builder.appendLine("    }")
    }

    // Compose options
    if (config.enableCompose && config.composeCompilerVersion != null) {
      builder.appendLine("    composeOptions {")
      builder.appendLine(
          "        kotlinCompilerExtensionVersion = '${config.composeCompilerVersion}'"
      )
      builder.appendLine("    }")
      builder.appendLine("    packaging {")
      builder.appendLine("        resources {")
      builder.appendLine("            excludes += '/META-INF/{AL2.0,LGPL2.1}'")
      builder.appendLine("        }")
      builder.appendLine("    }")
    }

    builder.appendLine("}")

    // Dependencies
    if (config.dependencies.isNotEmpty()) {
      builder.appendLine()
      builder.appendLine("dependencies {")
      config.dependencies.forEach { dep ->
        builder.appendLine("    ${dep.dependency.replace(Regex("[()]"), "  ")}")
      }
      builder.appendLine("}")
    }

    return builder.toString()
  }

  /** Generates Kotlin DSL-style build.gradle.kts content. */
  private fun generateKts(config: ModuleGradleConfig): String {
    val builder = StringBuilder()

    // Plugins block
    if (config.plugins.isNotEmpty()) {
      builder.appendLine("plugins {")
      builder.appendLine("    // Plugins automatically generated by MLGradleWriter.kt")
      config.plugins.forEach { plugin ->
        builder.appendLine("    ${buildPluginLine(plugin, true)}")
      }
      builder.appendLine("}")
      builder.appendLine()
    }

    // Android block
    builder.appendLine("android {")
    builder.appendLine("    namespace = \"${config.namespace}\"")
    builder.appendLine("    compileSdk = ${config.compileSdk}")

    // NDK version (top-level in android block)
    config.ndkVersion?.let { builder.appendLine("    ndkVersion = \"$it\"") }

    builder.appendLine()

    // Default config
    builder.appendLine("    defaultConfig {")
    builder.appendLine("        applicationId = \"${config.defaultConfig.applicationId}\"")
    builder.appendLine("        minSdk = ${config.defaultConfig.minSdk}")
    builder.appendLine("        targetSdk = ${config.defaultConfig.targetSdk}")
    builder.appendLine("        versionCode = ${config.defaultConfig.versionCode}")
    builder.appendLine("        versionName = \"${config.defaultConfig.versionName}\"")

    config.defaultConfig.testInstrumentationRunner?.let {
      builder.appendLine()
      builder.appendLine("        testInstrumentationRunner = \"$it\"")
    }

    // NDK ABI filters in defaultConfig
    config.defaultConfig.ndk?.let { ndkConfig ->
      if (ndkConfig.abiFilters.isNotEmpty()) {
        builder.appendLine()
        builder.appendLine("        ndk {")
        builder.append("            abiFilters.addAll(listOf(")
        builder.append(ndkConfig.abiFilters.joinToString(", ") { "\"$it\"" })
        builder.appendLine("))")
        builder.appendLine("        }")
      }
    }

    builder.appendLine("    }")

    // Build features
    if (config.buildFeatures.isNotEmpty()) {
      builder.appendLine()
      builder.appendLine("    buildFeatures {")
      config.buildFeatures.forEach { feature ->
        builder.appendLine("        ${feature.featureName} = true")
      }
      builder.appendLine("    }")
    }

    // External native build
    config.externalNativeBuild?.let { nativeBuild ->
      builder.appendLine()
      builder.appendLine("    externalNativeBuild {")

      nativeBuild.cmake?.let { cmake ->
        builder.appendLine("        cmake {")
        builder.appendLine("            path = file(\"${cmake.path}\")")
        cmake.version?.let { version -> builder.appendLine("            version = \"$version\"") }
        builder.appendLine("        }")
      }

      nativeBuild.ndkBuild?.let { ndkBuild ->
        builder.appendLine("        ndkBuild {")
        builder.appendLine("            path = file(\"${ndkBuild.path}\")")
        builder.appendLine("        }")
      }

      builder.appendLine("    }")
    }

    // Compile options
    builder.appendLine()
    builder.appendLine("    compileOptions {")
    builder.appendLine(
        "        sourceCompatibility = JavaVersion.${config.javaVersion.versionName}"
    )
    builder.appendLine(
        "        targetCompatibility = JavaVersion.${config.javaVersion.versionName}"
    )
    builder.appendLine("    }")

    // Kotlin compiler options
    if (config.enableKotlinOptions) {
      builder.appendLine("    kotlin {")
      builder.appendLine("        compilerOptions {")
      builder.appendLine(
          "            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(\"${config.javaVersion.versionNumber}\"))"
      )
      builder.appendLine("        }")
      builder.appendLine("    }")
    }

    // Compose options
    if (config.enableCompose && config.composeCompilerVersion != null) {
      builder.appendLine("    composeOptions {")
      builder.appendLine(
          "        kotlinCompilerExtensionVersion = \"${config.composeCompilerVersion}\""
      )
      builder.appendLine("    }")
      builder.appendLine("    packaging {")
      builder.appendLine("        resources {")
      builder.appendLine("            excludes += \"/META-INF/{AL2.0,LGPL2.1}\"")
      builder.appendLine("        }")
      builder.appendLine("    }")
    }

    builder.appendLine("}")

    // Dependencies
    if (config.dependencies.isNotEmpty()) {
      builder.appendLine()
      builder.appendLine("dependencies {")
      config.dependencies.forEach { dep -> builder.appendLine("    ${dep.dependency}") }
      builder.appendLine("}")
    }

    return builder.toString()
  }

  /**
   * Builds a plugin declaration line based on the type and format. The user provides the complete
   * plugin notation, we just format it correctly.
   */
  private fun buildPluginLine(plugin: GradlePlugin, isKts: Boolean): String {
    return when (plugin.type.lowercase()) {
      "id" -> {
        if (isKts) {
          "id(\"${plugin.plugin}\")"
        } else {
          "id '${plugin.plugin}'"
        }
      }
      "alias" -> {
        // User provides like: "libs.plugins.android.application"
        "alias(${plugin.plugin})"
      }
      else -> {
        // For any custom type, just use it as-is with proper formatting
        if (isKts) {
          "${plugin.type}(\"${plugin.plugin}\")"
        } else {
          "${plugin.type} '${plugin.plugin}'"
        }
      }
    }
  }
}

/** Builder class for creating ModuleGradleConfig with a fluent API. */
class ModuleGradleConfigBuilder {
  private val plugins = mutableListOf<GradlePlugin>()
  private var namespace: String = ""
  private var compileSdk: Int = 0
  private var defaultConfig: DefaultConfig? = null
  private val buildFeatures = mutableListOf<BuildFeature>()
  private var javaVersion: JavaVersion = JavaVersion.VERSION_17
  private var enableKotlinOptions: Boolean = true
  private var enableCompose: Boolean = false
  private var composeCompilerVersion: String? = null
  private val dependencies = mutableListOf<GradleDependency>()
  private var externalNativeBuild: ExternalNativeBuild? = null
  private var ndkVersion: String? = null

  fun addPlugin(plugin: GradlePlugin) = apply { plugins.add(plugin) }

  fun addPlugins(vararg plugins: GradlePlugin) = apply { this.plugins.addAll(plugins) }

  fun namespace(namespace: String) = apply { this.namespace = namespace }

  fun compileSdk(sdk: Int) = apply { this.compileSdk = sdk }

  fun defaultConfig(config: DefaultConfig) = apply { this.defaultConfig = config }

  fun addBuildFeature(feature: BuildFeature) = apply { buildFeatures.add(feature) }

  fun addBuildFeatures(vararg features: BuildFeature) = apply { buildFeatures.addAll(features) }

  fun javaVersion(version: JavaVersion) = apply { this.javaVersion = version }

  fun enableKotlinOptions(enable: Boolean) = apply { this.enableKotlinOptions = enable }

  fun enableCompose(enable: Boolean, compilerVersion: String? = null) = apply {
    this.enableCompose = enable
    this.composeCompilerVersion = compilerVersion
  }

  fun addDependency(dependency: GradleDependency) = apply { dependencies.add(dependency) }

  fun addDependencies(vararg deps: GradleDependency) = apply { dependencies.addAll(deps) }

  fun externalNativeBuild(build: ExternalNativeBuild) = apply { this.externalNativeBuild = build }

  fun ndkVersion(version: String) = apply { this.ndkVersion = version }

  fun build(): ModuleGradleConfig {
    require(namespace.isNotBlank()) { "Namespace cannot be blank" }
    require(compileSdk > 0) { "Compile SDK must be greater than 0" }
    requireNotNull(defaultConfig) { "Default config must be set" }

    return ModuleGradleConfig(
        plugins = plugins.toList(),
        namespace = namespace,
        compileSdk = compileSdk,
        defaultConfig = defaultConfig!!,
        buildFeatures = buildFeatures.toList(),
        javaVersion = javaVersion,
        enableKotlinOptions = enableKotlinOptions,
        enableCompose = enableCompose,
        composeCompilerVersion = composeCompilerVersion,
        dependencies = dependencies.toList(),
        externalNativeBuild = externalNativeBuild,
        ndkVersion = ndkVersion,
    )
  }
}

/** DSL function for creating ModuleGradleConfig. */
fun moduleGradleConfig(block: ModuleGradleConfigBuilder.() -> Unit): ModuleGradleConfig {
  return ModuleGradleConfigBuilder().apply(block).build()
}
