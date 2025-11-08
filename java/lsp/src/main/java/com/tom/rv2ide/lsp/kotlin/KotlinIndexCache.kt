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

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.tom.rv2ide.utils.Environment
import java.io.File
import java.security.MessageDigest
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class KotlinIndexCache(private val projectPath: String) {

  companion object {
    private val log = LoggerFactory.getLogger(KotlinIndexCache::class.java)
    private const val CACHE_VERSION = 1
    private const val CACHE_DIR_NAME = "kls-cache"
    private const val CACHE_FILE_NAME = "index-cache.json"
    private const val CLASSPATH_HASH_FILE = "classpath-hash.txt"
  }

  private val gson = Gson()

  // Use ANDROIDIDE_HOME for global cache storage
  private val globalCacheDir = File(Environment.ANDROIDIDE_HOME, CACHE_DIR_NAME)

  // Create project-specific cache directory using project path hash
  private val projectHash = computeHash(projectPath)
  private val cacheDir = File(globalCacheDir, projectHash)
  private val cacheFile = File(cacheDir, CACHE_FILE_NAME)
  private val hashFile = File(cacheDir, CLASSPATH_HASH_FILE)

  init {
    cacheDir.mkdirs()
    KslLogs.debug("Cache directory: {}", cacheDir.absolutePath)
  }

  /** Compute hash for a string (used for project path) */
  private fun computeHash(content: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(content.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }.take(16)
  }

  /** Compute hash of classpath to detect changes */
  fun computeClasspathHash(classpath: List<String>): String {
    val content = classpath.sorted().joinToString("\n")
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(content.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
  }

  /** Check if cache is valid */
  fun isCacheValid(currentClasspathHash: String): Boolean {
    if (!cacheFile.exists() || !hashFile.exists()) {
      KslLogs.info("Cache files don't exist for project")
      return false
    }

    try {
      val cachedHash = hashFile.readText().trim()
      val isValid = cachedHash == currentClasspathHash
      KslLogs.info(
          "Cache validation: cached={}, current={}, valid={}",
          cachedHash.take(8),
          currentClasspathHash.take(8),
          isValid,
      )
      return isValid
    } catch (e: Exception) {
      KslLogs.error("Failed to validate cache", e)
      return false
    }
  }

  /** Save indexed symbols to cache */
  fun saveCache(symbols: JsonArray, classpathHash: String) {
    try {
      val cacheData =
          JsonObject().apply {
            addProperty("version", CACHE_VERSION)
            addProperty("timestamp", System.currentTimeMillis())
            addProperty("projectPath", projectPath)
            addProperty("classpathHash", classpathHash)
            add("symbols", symbols)
          }

      cacheFile.writeText(gson.toJson(cacheData))
      hashFile.writeText(classpathHash)

      KslLogs.info(
          "Saved cache with {} symbols (hash: {}) at {}",
          symbols.size(),
          classpathHash.take(8),
          cacheFile.absolutePath,
      )
    } catch (e: Exception) {
      KslLogs.error("Failed to save cache", e)
    }
  }

  /** Load cached symbols */
  fun loadCache(): JsonArray? {
    if (!cacheFile.exists()) {
      return null
    }

    try {
      val cacheData = gson.fromJson(cacheFile.readText(), JsonObject::class.java)
      val version = cacheData.get("version")?.asInt ?: 0

      if (version != CACHE_VERSION) {
        KslLogs.warn("Cache version mismatch: expected={}, found={}", CACHE_VERSION, version)
        return null
      }

      val symbols = cacheData.getAsJsonArray("symbols")
      KslLogs.info(
          "Loaded cache with {} symbols from {}",
          symbols?.size() ?: 0,
          cacheFile.absolutePath,
      )
      return symbols
    } catch (e: Exception) {
      KslLogs.error("Failed to load cache", e)
      return null
    }
  }

  /** Clear cache for this project */
  fun clearCache() {
    try {
      cacheFile.delete()
      hashFile.delete()
      KslLogs.info("Cache cleared for project")
    } catch (e: Exception) {
      KslLogs.error("Failed to clear cache", e)
    }
  }

  /** Clear all caches (for all projects) */
  fun clearAllCaches() {
    try {
      globalCacheDir.deleteRecursively()
      globalCacheDir.mkdirs()
      KslLogs.info("All caches cleared")
    } catch (e: Exception) {
      KslLogs.error("Failed to clear all caches", e)
    }
  }

  /** Get cache statistics */
  fun getCacheStats(): String {
    return buildString {
      append("Project hash: $projectHash\n")
      append("Cache dir: ${cacheDir.absolutePath}\n")
      append("Cache exists: ${cacheFile.exists()}\n")
      if (cacheFile.exists()) {
        append("Cache size: ${cacheFile.length() / 1024} KB\n")
        append("Last modified: ${java.util.Date(cacheFile.lastModified())}\n")
      }
    }
  }

  /** Get total cache size for all projects */
  fun getTotalCacheSize(): Long {
    return try {
      globalCacheDir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
    } catch (e: Exception) {
      0L
    }
  }
}
