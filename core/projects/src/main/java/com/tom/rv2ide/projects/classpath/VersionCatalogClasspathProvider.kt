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

package com.tom.rv2ide.projects.classpath

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * Resolves dependencies from Gradle version catalogs (libs.versions.toml) and provides them as
 * classpath entries.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class VersionCatalogClasspathProvider {

  companion object {
    private val log = LoggerFactory.getLogger(VersionCatalogClasspathProvider::class.java)

    // Cache resolved dependencies per project directory
    private val catalogCache = ConcurrentHashMap<String, CatalogCacheEntry>()

    // Cache for transitive dependencies to avoid re-parsing POMs
    private val transitiveCache = ConcurrentHashMap<String, List<File>>()

    // Cache for POM files
    private val pomCache = ConcurrentHashMap<String, List<CatalogDependency>>()
  }

  data class CatalogCacheEntry(val timestamp: Long, val resolvedJars: Set<File>)

  data class CatalogDependency(val group: String, val name: String, val version: String) {
    fun toMavenCoordinate(): String = "$group:$name:$version"

    fun toGroupArtifactKey(): String = "$group:$name"
  }

  /**
   * Get classpath entries from version catalog for the given project directory.
   *
   * @param existingDeps Map of group:name -> File for already resolved dependencies to avoid
   *   conflicts
   */
  fun getClasspathFromCatalog(
      projectDir: File,
      existingDeps: Map<String, File> = emptyMap(),
  ): Set<File> {
    val catalogFile =
        findCatalogFile(projectDir)
            ?: run {
              log.debug("No version catalog found in project: {}", projectDir.absolutePath)
              return emptySet()
            }

    // Check cache
    val cacheKey = catalogFile.absolutePath
    val cached = catalogCache[cacheKey]

    if (cached != null && cached.timestamp == catalogFile.lastModified()) {
      log.debug("Using cached catalog dependencies (${cached.resolvedJars.size} JARs)")
      return cached.resolvedJars
    }

    log.info("Found version catalog: {}", catalogFile.absolutePath)

    return try {
      val dependencies = parseCatalog(catalogFile)
      log.info("Parsed {} dependencies from version catalog", dependencies.size)

      // Filter out dependencies that conflict with existing ones
      val newDeps = filterNonConflictingDependencies(dependencies, existingDeps)
      log.info("After conflict resolution, {} new dependencies to resolve", newDeps.size)

      if (newDeps.isEmpty()) {
        log.info("No new dependencies to resolve from catalog (all already present)")
        return emptySet()
      }

      val jars = resolveDependenciesToJars(newDeps)
      log.info("Successfully resolved {} JAR files from version catalog", jars.size)

      // Cache the result
      catalogCache[cacheKey] =
          CatalogCacheEntry(timestamp = catalogFile.lastModified(), resolvedJars = jars.toSet())

      jars.toSet()
    } catch (e: Exception) {
      log.error("Failed to resolve version catalog dependencies", e)
      emptySet()
    }
  }

  /**
   * Filter out dependencies that would conflict with existing ones. Only keeps dependencies where
   * group:name doesn't exist in existing deps.
   */
  private fun filterNonConflictingDependencies(
      dependencies: List<CatalogDependency>,
      existingDeps: Map<String, File>,
  ): List<CatalogDependency> {
    if (existingDeps.isEmpty()) {
      log.debug("No existing dependencies to check against")
      return dependencies
    }

    return dependencies.filter { dep ->
      val key = dep.toGroupArtifactKey()
      val exists = existingDeps.containsKey(key)

      if (exists) {
        log.debug(
            "Skipping conflicting dependency: {} (already exists at: {})",
            dep.toMavenCoordinate(),
            existingDeps[key]?.name,
        )
      } else {
        log.trace("Including new dependency: {}", dep.toMavenCoordinate())
      }

      !exists
    }
  }

  private fun findCatalogFile(projectDir: File): File? {
    val standardLocations =
        listOf(
            File(projectDir, "gradle/libs.versions.toml"),
            File(projectDir, "gradle/catalog/libs.versions.toml"),
            File(projectDir, "buildSrc/libs.versions.toml"),
        )

    return standardLocations.firstOrNull { it.exists() }
  }

  private fun parseCatalog(catalogFile: File): List<CatalogDependency> {
    val dependencies = mutableListOf<CatalogDependency>()
    val content = catalogFile.readText()

    // Parse [versions] section
    val versions = parseVersionsSection(content)
    log.debug("Found {} version definitions", versions.size)

    // Parse [libraries] section
    val libraries = parseLibrariesSection(content, versions)
    dependencies.addAll(libraries)

    return dependencies
  }

  private fun resolveDependenciesToJars(dependencies: List<CatalogDependency>): List<File> {
    val gradleCacheDir =
        File(
            System.getProperty("user.home") ?: "/data/data/com.tom.rv2ide/files",
            ".gradle/caches/modules-2/files-2.1",
        )

    if (!gradleCacheDir.exists()) {
      log.warn("Gradle cache directory not found: {}", gradleCacheDir.absolutePath)
      return emptyList()
    }

    val jars = mutableListOf<File>()
    val resolved = mutableSetOf<String>()

    dependencies.forEach { dep ->
      val cacheKey = dep.toMavenCoordinate()

      // Check transitive cache
      val cachedJars = transitiveCache[cacheKey]
      if (cachedJars != null) {
        log.trace("Cache hit for: {}", cacheKey)
        jars.addAll(cachedJars)
      } else {
        // Resolve with transitive dependencies
        val foundJars = resolveTransitiveDependencies(dep, gradleCacheDir, resolved)

        if (foundJars.isNotEmpty()) {
          jars.addAll(foundJars)
          // Cache it
          transitiveCache[cacheKey] = foundJars
        } else {
          log.warn("Could not find JAR for: {}", dep.toMavenCoordinate())
        }
      }
    }

    return jars
  }

  private fun resolveTransitiveDependencies(
      dep: CatalogDependency,
      cacheDir: File,
      resolved: MutableSet<String>,
  ): List<File> {
    val coordinate = dep.toMavenCoordinate()

    // Avoid circular dependencies
    if (resolved.contains(coordinate)) {
      return emptyList()
    }
    resolved.add(coordinate)

    val jars = mutableListOf<File>()

    // Get the main JAR
    val mainJars = findJarInGradleCache(dep, cacheDir)
    jars.addAll(mainJars)

    // Find and parse POM file to get transitive dependencies
    val pomFile = findPomInGradleCache(dep, cacheDir)
    if (pomFile != null) {
      val pomCacheKey = pomFile.absolutePath

      // Check POM cache
      val cachedDeps = pomCache[pomCacheKey]
      val transitiveDeps =
          if (cachedDeps != null) {
            log.trace("POM cache hit: {}", pomFile.name)
            cachedDeps
          } else {
            val deps = parsePomDependencies(pomFile)
            pomCache[pomCacheKey] = deps
            deps
          }

      transitiveDeps.forEach { transitiveDep ->
        jars.addAll(resolveTransitiveDependencies(transitiveDep, cacheDir, resolved))
      }
    }

    return jars
  }

  private fun findPomInGradleCache(dep: CatalogDependency, cacheDir: File): File? {
    val artifactDir = File(cacheDir, "${dep.group}/${dep.name}")
    if (!artifactDir.exists()) return null

    val versionDirs =
        artifactDir.listFiles { file -> file.isDirectory && file.name == dep.version }?.toList()
            ?: return null

    versionDirs.forEach { versionDir ->
      versionDir.listFiles()?.forEach { hashDir ->
        if (hashDir.isDirectory) {
          val pomFile = hashDir.listFiles { file -> file.extension == "pom" }?.firstOrNull()

          if (pomFile != null) return pomFile
        }
      }
    }

    return null
  }

  private fun parsePomDependencies(pomFile: File): List<CatalogDependency> {
    val dependencies = mutableListOf<CatalogDependency>()

    try {
      val xml = pomFile.readText()

      // Simple regex parsing (you might want to use proper XML parser)
      val dependencyPattern =
          """<dependency>.*?<groupId>(.*?)</groupId>.*?<artifactId>(.*?)</artifactId>.*?<version>(.*?)</version>.*?</dependency>"""
              .toRegex(RegexOption.DOT_MATCHES_ALL)

      dependencyPattern.findAll(xml).forEach { match ->
        val group = match.groupValues[1].trim()
        val name = match.groupValues[2].trim()
        var version = match.groupValues[3].trim()

        // Skip test/provided scope dependencies
        val scopeMatch = """<scope>(.*?)</scope>""".toRegex().find(match.value)
        if (scopeMatch != null) {
          val scope = scopeMatch.groupValues[1].trim()
          if (scope in listOf("test", "provided")) {
            return@forEach
          }
        }

        // Resolve version properties like ${project.version}
        if (version.startsWith("\${")) {
          // For now, skip property versions (they're complex to resolve)
          return@forEach
        }

        dependencies.add(CatalogDependency(group, name, version))
      }
    } catch (e: Exception) {
      log.error("Failed to parse POM: {}", pomFile.name, e)
    }

    return dependencies
  }

  private fun parseVersionsSection(content: String): Map<String, String> {
    val versions = mutableMapOf<String, String>()

    val versionsMatch =
        """\[versions\](.*?)(?=\[|$)""".toRegex(RegexOption.DOT_MATCHES_ALL).find(content)
            ?: return versions

    val versionsSection = versionsMatch.groupValues[1]
    val linePattern = """(\w+)\s*=\s*(?:"([^"]+)"|'([^']+)')""".toRegex()

    linePattern.findAll(versionsSection).forEach { match ->
      val key = match.groupValues[1]
      val value = match.groupValues[2].ifEmpty { match.groupValues[3] }
      if (value.isNotEmpty()) {
        versions[key] = value
      }
    }

    return versions
  }

  private fun parseLibrariesSection(
      content: String,
      versions: Map<String, String>,
  ): List<CatalogDependency> {
    val libraries = mutableListOf<CatalogDependency>()

    val librariesMatch =
        """\[libraries\](.*?)(?=\[|$)""".toRegex(RegexOption.DOT_MATCHES_ALL).find(content)

    if (librariesMatch == null) {
      log.warn("Could not find [libraries] section in catalog")
      return libraries
    }

    val librariesSection = librariesMatch.groupValues[1]
    val lines = librariesSection.lines()

    var i = 0
    while (i < lines.size) {
      val line = lines[i].trim()

      if (line.isEmpty() || line.startsWith("#")) {
        i++
        continue
      }

      // Match: alias = { ... }
      if (line.contains("= {")) {
        val aliasMatch = """^([\w\-]+)\s*=\s*\{""".toRegex().find(line)
        if (aliasMatch == null) {
          i++
          continue
        }

        val alias = aliasMatch.groupValues[1]

        var group: String? = null
        var name: String? = null
        var version: String? = null

        // Check if it's all on one line
        if (line.contains("}")) {
          // Parse module = "group:name"
          val moduleMatch = """module\s*=\s*"([^"]+)"""".toRegex().find(line)
          if (moduleMatch != null) {
            val moduleParts = moduleMatch.groupValues[1].split(":")
            if (moduleParts.size >= 2) {
              group = moduleParts[0]
              name = moduleParts[1]
              if (moduleParts.size >= 3) {
                version = moduleParts[2]
              }
            }
          }

          // Parse group = "..." (overrides module if present)
          val groupMatch = """group\s*=\s*"([^"]+)"""".toRegex().find(line)
          if (groupMatch != null) {
            group = groupMatch.groupValues[1]
          }

          // Parse name = "..." (overrides module if present)
          val nameMatch = """name\s*=\s*"([^"]+)"""".toRegex().find(line)
          if (nameMatch != null) {
            name = nameMatch.groupValues[1]
          }

          // Parse version = "..."
          val versionMatch = """version\s*=\s*"([^"]+)"""".toRegex().find(line)
          if (versionMatch != null) {
            version = versionMatch.groupValues[1]
          }

          // Parse version.ref = "..." (overrides version if present)
          val versionRefMatch = """version\.ref\s*=\s*"([^"]+)"""".toRegex().find(line)
          if (versionRefMatch != null) {
            val versionKey = versionRefMatch.groupValues[1]
            version = versions[versionKey]
            if (version == null) {
              log.warn("Version reference '{}' not found in [versions] for {}", versionKey, alias)
            }
          }

          if (group != null && name != null && version != null) {
            val dep = CatalogDependency(group, name, version)
            libraries.add(dep)
            log.trace("Parsed: {} -> {}", alias, dep.toMavenCoordinate())
          } else {
            log.warn(
                "Skipping incomplete dependency '{}': group={}, name={}, version={}",
                alias,
                group,
                name,
                version,
            )
          }
        } else {
          // Multi-line format
          i++
          while (i < lines.size) {
            val innerLine = lines[i].trim()

            if (innerLine.contains("}")) {
              break
            }

            val moduleMatch = """module\s*=\s*"([^"]+)"""".toRegex().find(innerLine)
            if (moduleMatch != null) {
              val moduleParts = moduleMatch.groupValues[1].split(":")
              if (moduleParts.size >= 2) {
                group = moduleParts[0]
                name = moduleParts[1]
                if (moduleParts.size >= 3) {
                  version = moduleParts[2]
                }
              }
            }

            val groupMatch = """group\s*=\s*"([^"]+)"""".toRegex().find(innerLine)
            if (groupMatch != null) group = groupMatch.groupValues[1]

            val nameMatch = """name\s*=\s*"([^"]+)"""".toRegex().find(innerLine)
            if (nameMatch != null) name = nameMatch.groupValues[1]

            val versionMatch = """version\s*=\s*"([^"]+)"""".toRegex().find(innerLine)
            if (versionMatch != null) version = versionMatch.groupValues[1]

            val versionRefMatch = """version\.ref\s*=\s*"([^"]+)"""".toRegex().find(innerLine)
            if (versionRefMatch != null) {
              val versionKey = versionRefMatch.groupValues[1]
              version = versions[versionKey]
            }

            i++
          }

          if (group != null && name != null && version != null) {
            val dep = CatalogDependency(group, name, version)
            libraries.add(dep)
            log.trace("Parsed: {} -> {}", alias, dep.toMavenCoordinate())
          }
        }
      }

      i++
    }

    log.info("Successfully parsed {} dependencies from version catalog", libraries.size)
    return libraries
  }

  private fun findJarInGradleCache(dep: CatalogDependency, cacheDir: File): List<File> {
    val artifactDir = File(cacheDir, "${dep.group}/${dep.name}")

    if (!artifactDir.exists()) {
      log.trace("Artifact dir not found: {}", artifactDir.absolutePath)
      return emptyList()
    }

    // Find matching version directories
    val versionDirs =
        artifactDir
            .listFiles { file -> file.isDirectory && isVersionMatch(file.name, dep.version) }
            ?.toList() ?: emptyList()

    if (versionDirs.isEmpty()) {
      log.trace("No version directories found for: {}", dep.toMavenCoordinate())
      return emptyList()
    }

    val jars = mutableListOf<File>()

    versionDirs.forEach { versionDir ->
      versionDir.listFiles()?.forEach { hashDir ->
        if (hashDir.isDirectory) {
          // Look for regular JARs (excluding sources/javadoc)
          hashDir
              .listFiles { file ->
                file.extension == "jar" &&
                    !file.name.contains("sources") &&
                    !file.name.contains("javadoc")
              }
              ?.forEach { jar ->
                jars.add(jar)
                log.trace("Found JAR: {}", jar.name)
              }

          // Look for AAR files and extract classes.jar
          hashDir
              .listFiles { file -> file.extension == "aar" }
              ?.forEach { aar ->
                val extractedJar = extractClassesJarFromAAR(aar)
                if (extractedJar != null) {
                  jars.add(extractedJar)
                  log.trace("Extracted classes.jar from AAR: {}", aar.name)
                }
              }
        }
      }
    }

    return jars
  }

  private fun isVersionMatch(dirName: String, requestedVersion: String): Boolean {
    // Exact match
    if (dirName == requestedVersion) return true

    // Wildcard version (e.g., "1.0.+")
    if (requestedVersion.contains("+")) {
      val prefix = requestedVersion.substringBefore("+")
      return dirName.startsWith(prefix)
    }

    return false
  }

  private fun extractClassesJarFromAAR(aarFile: File): File? {
    try {
      val extractDir = File(aarFile.parentFile, "${aarFile.nameWithoutExtension}-extracted")
      val classesJar = File(extractDir, "classes.jar")

      // Return cached extraction if it exists
      if (classesJar.exists()) {
        return classesJar
      }

      extractDir.mkdirs()

      java.util.zip.ZipFile(aarFile).use { zip ->
        val classesEntry = zip.getEntry("classes.jar")
        if (classesEntry != null) {
          zip.getInputStream(classesEntry).use { input ->
            classesJar.outputStream().use { output -> input.copyTo(output) }
          }
          log.debug("Extracted classes.jar from: {}", aarFile.name)
          return classesJar
        } else {
          log.warn("No classes.jar found in AAR: {}", aarFile.name)
        }
      }
    } catch (e: Exception) {
      log.error("Failed to extract classes.jar from AAR: {}", aarFile.name, e)
    }

    return null
  }

  /** Clear all caches (call when libs.versions.toml changes) */
  fun clearCache() {
    catalogCache.clear()
    transitiveCache.clear()
    pomCache.clear()
    log.info("Cleared all version catalog caches")
  }
}
