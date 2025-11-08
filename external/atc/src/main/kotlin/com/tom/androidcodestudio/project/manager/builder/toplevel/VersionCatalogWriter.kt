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

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

package com.tom.androidcodestudio.project.manager.builder.toplevel

import java.io.File

/**
 * Data class representing a version entry in the catalog.
 *
 * @property name The version reference name (e.g., "agp", "coreKtx")
 * @property version The version string (e.g., "8.13.0", "1.12.0")
 */
data class CatalogVersion(val name: String, val version: String)

/**
 * Data class representing a plugin entry in the catalog.
 *
 * @property alias The plugin alias (e.g., "android-application")
 * @property id The plugin ID (e.g., "com.android.application")
 * @property versionRef Optional version reference (e.g., "agp")
 * @property version Optional direct version string (used if versionRef is null)
 */
data class CatalogPlugin(
    val alias: String,
    val id: String,
    val versionRef: String? = null,
    val version: String? = null,
)

/**
 * Data class representing a library entry in the catalog.
 *
 * @property alias The library alias (e.g., "androidx-core-ktx")
 * @property group The Maven group ID (e.g., "androidx.core")
 * @property name The artifact name (e.g., "core-ktx")
 * @property versionRef Optional version reference (e.g., "coreKtx")
 * @property version Optional direct version string (used if versionRef is null)
 */
data class CatalogLibrary(
    val alias: String,
    val group: String,
    val name: String,
    val versionRef: String? = null,
    val version: String? = null,
)

/**
 * Data class representing a custom section in the catalog.
 *
 * @property name The section name (e.g., "bundles", "custom-section")
 * @property entries Map of key-value pairs or complex entries
 */
data class CatalogSection(val name: String, val entries: Map<String, Any>)

/** Interface for writing Gradle version catalog (libs.versions.toml) files. */
interface GLCatalog {

  /**
   * Generates the libs.versions.toml file content.
   *
   * @param versions List of version entries
   * @param plugins List of plugin entries
   * @param libraries List of library entries
   * @param customSections List of custom sections
   * @return The generated catalog content as a String
   */
  fun generate(
      versions: List<CatalogVersion> = emptyList(),
      plugins: List<CatalogPlugin> = emptyList(),
      libraries: List<CatalogLibrary> = emptyList(),
      customSections: List<CatalogSection> = emptyList(),
  ): String

  /**
   * Writes the generated content to a file.
   *
   * @param outputDir The directory where the libs.versions.toml file will be created
   * @param versions List of version entries
   * @param plugins List of plugin entries
   * @param libraries List of library entries
   * @param customSections List of custom sections
   * @return The created File object
   */
  fun writeToFile(
      outputDir: File,
      versions: List<CatalogVersion> = emptyList(),
      plugins: List<CatalogPlugin> = emptyList(),
      libraries: List<CatalogLibrary> = emptyList(),
      customSections: List<CatalogSection> = emptyList(),
  ): File
}

/** Default implementation of GLCatalog for generating Gradle version catalog files. */
class VersionCatalogWriter : GLCatalog {

  companion object {
    const val CATALOG_FILE_NAME = "libs.versions.toml"
  }

  override fun generate(
      versions: List<CatalogVersion>,
      plugins: List<CatalogPlugin>,
      libraries: List<CatalogLibrary>,
      customSections: List<CatalogSection>,
  ): String {
    val builder = StringBuilder()

    // Generate [versions] section
    if (versions.isNotEmpty()) {
      builder.appendLine("[versions]")
      versions.forEach { version -> builder.appendLine("${version.name} = \"${version.version}\"") }
      builder.appendLine()
    }

    // Generate [plugins] section
    if (plugins.isNotEmpty()) {
      builder.appendLine("[plugins]")
      plugins.forEach { plugin ->
        val pluginLine = buildPluginLine(plugin)
        builder.appendLine("$pluginLine")
      }
      builder.appendLine()
    }

    // Generate [libraries] section
    if (libraries.isNotEmpty()) {
      builder.appendLine("[libraries]")
      libraries.forEach { library ->
        val libraryLine = buildLibraryLine(library)
        builder.appendLine("$libraryLine")
      }
      builder.appendLine()
    }

    // Generate custom sections
    customSections.forEach { section ->
      builder.appendLine("[${section.name}]")
      section.entries.forEach { (key, value) ->
        val entryLine = buildCustomEntryLine(key, value)
        builder.appendLine("$entryLine")
      }
      builder.appendLine()
    }

    return builder.toString().trimEnd() + "\n"
  }

  override fun writeToFile(
      outputDir: File,
      versions: List<CatalogVersion>,
      plugins: List<CatalogPlugin>,
      libraries: List<CatalogLibrary>,
      customSections: List<CatalogSection>,
  ): File {
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val content = generate(versions, plugins, libraries, customSections)
    val file = File(outputDir, CATALOG_FILE_NAME)
    file.writeText(content)

    return file
  }

  /** Builds a plugin declaration line. */
  private fun buildPluginLine(plugin: CatalogPlugin): String {
    val parts = mutableListOf<String>()
    parts.add("id = \"${plugin.id}\"")

    when {
      plugin.versionRef != null -> parts.add("version.ref = \"${plugin.versionRef}\"")
      plugin.version != null -> parts.add("version = \"${plugin.version}\"")
    }

    return "${plugin.alias} = { ${parts.joinToString(", ")} }"
  }

  /** Builds a library declaration line. */
  private fun buildLibraryLine(library: CatalogLibrary): String {
    val parts = mutableListOf<String>()
    parts.add("group = \"${library.group}\"")
    parts.add("name = \"${library.name}\"")

    when {
      library.versionRef != null -> parts.add("version.ref = \"${library.versionRef}\"")
      library.version != null -> parts.add("version = \"${library.version}\"")
    }

    return "${library.alias} = { ${parts.joinToString(", ")} }"
  }

  /** Builds a custom section entry line. */
  private fun buildCustomEntryLine(key: String, value: Any): String {
    return when (value) {
      is String -> "$key = \"$value\""
      is List<*> -> "$key = [${value.joinToString(", ") { "\"$it\"" }}]"
      is Map<*, *> -> {
        val mapContent = value.entries.joinToString(", ") { (k, v) -> "$k = \"$v\"" }
        "$key = { $mapContent }"
      }
      else -> "$key = \"$value\""
    }
  }
}

/** Builder class for creating CatalogVersion instances with a fluent API. */
class CatalogVersionBuilder {
  private var name: String = ""
  private var version: String = ""

  fun name(name: String) = apply { this.name = name }

  fun version(version: String) = apply { this.version = version }

  fun build(): CatalogVersion {
    require(name.isNotBlank()) { "Version name cannot be blank" }
    require(version.isNotBlank()) { "Version string cannot be blank" }
    return CatalogVersion(name, version)
  }
}

/** Builder class for creating CatalogPlugin instances with a fluent API. */
class CatalogPluginBuilder {
  private var alias: String = ""
  private var id: String = ""
  private var versionRef: String? = null
  private var version: String? = null

  fun alias(alias: String) = apply { this.alias = alias }

  fun id(id: String) = apply { this.id = id }

  fun versionRef(versionRef: String?) = apply { this.versionRef = versionRef }

  fun version(version: String?) = apply { this.version = version }

  fun build(): CatalogPlugin {
    require(alias.isNotBlank()) { "Plugin alias cannot be blank" }
    require(id.isNotBlank()) { "Plugin ID cannot be blank" }
    return CatalogPlugin(alias, id, versionRef, version)
  }
}

/** Builder class for creating CatalogLibrary instances with a fluent API. */
class CatalogLibraryBuilder {
  private var alias: String = ""
  private var group: String = ""
  private var name: String = ""
  private var versionRef: String? = null
  private var version: String? = null

  fun alias(alias: String) = apply { this.alias = alias }

  fun group(group: String) = apply { this.group = group }

  fun name(name: String) = apply { this.name = name }

  fun versionRef(versionRef: String?) = apply { this.versionRef = versionRef }

  fun version(version: String?) = apply { this.version = version }

  fun build(): CatalogLibrary {
    require(alias.isNotBlank()) { "Library alias cannot be blank" }
    require(group.isNotBlank()) { "Library group cannot be blank" }
    require(name.isNotBlank()) { "Library name cannot be blank" }
    return CatalogLibrary(alias, group, name, versionRef, version)
  }
}

/** DSL functions for creating catalog entries. */
fun catalogVersion(block: CatalogVersionBuilder.() -> Unit): CatalogVersion {
  return CatalogVersionBuilder().apply(block).build()
}

fun catalogPlugin(block: CatalogPluginBuilder.() -> Unit): CatalogPlugin {
  return CatalogPluginBuilder().apply(block).build()
}

fun catalogLibrary(block: CatalogLibraryBuilder.() -> Unit): CatalogLibrary {
  return CatalogLibraryBuilder().apply(block).build()
}

/** Extension function to create a custom section with a builder DSL. */
fun catalogSection(name: String, block: MutableMap<String, Any>.() -> Unit): CatalogSection {
  val entries = mutableMapOf<String, Any>()
  entries.apply(block)
  return CatalogSection(name, entries)
}
