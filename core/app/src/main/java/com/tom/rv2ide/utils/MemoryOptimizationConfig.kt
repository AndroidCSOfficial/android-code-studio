/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.utils

import android.content.Context
import android.content.SharedPreferences
import org.slf4j.LoggerFactory

/**
 * Configuration for memory optimization settings. Allows users to customize memory management
 * behavior.
 *
 * @author AndroidIDE Team
 */
class MemoryOptimizationConfig private constructor(context: Context) {

  private val log = LoggerFactory.getLogger(MemoryOptimizationConfig::class.java)
  private val prefs: SharedPreferences =
      context.getSharedPreferences("memory_optimization", Context.MODE_PRIVATE)

  companion object {
    @Volatile private var INSTANCE: MemoryOptimizationConfig? = null

    fun getInstance(context: Context): MemoryOptimizationConfig {
      return INSTANCE
          ?: synchronized(this) {
            INSTANCE ?: MemoryOptimizationConfig(context).also { INSTANCE = it }
          }
    }

    // Default values
    private const val DEFAULT_MEMORY_PRESSURE_THRESHOLD = 85
    private const val DEFAULT_CHART_UPDATE_INTERVAL = 2000L
    private const val DEFAULT_CACHE_SIZE = 50
    private const val DEFAULT_LARGE_FILE_THRESHOLD = 1024 * 1024 // 1MB
    private const val DEFAULT_LARGE_PROJECT_THRESHOLD = 1000
  }

  /** Memory pressure threshold percentage. */
  var memoryPressureThreshold: Int
    get() = prefs.getInt("memory_pressure_threshold", DEFAULT_MEMORY_PRESSURE_THRESHOLD)
    set(value) {
      prefs.edit().putInt("memory_pressure_threshold", value).apply()
      log.info("Memory pressure threshold set to: $value%")
    }

  /** Chart update interval in milliseconds. */
  var chartUpdateInterval: Long
    get() = prefs.getLong("chart_update_interval", DEFAULT_CHART_UPDATE_INTERVAL)
    set(value) {
      prefs.edit().putLong("chart_update_interval", value).apply()
      log.info("Chart update interval set to: ${value}ms")
    }

  /** Maximum cache size for file content. */
  var maxCacheSize: Int
    get() = prefs.getInt("max_cache_size", DEFAULT_CACHE_SIZE)
    set(value) {
      prefs.edit().putInt("max_cache_size", value).apply()
      log.info("Max cache size set to: $value")
    }

  /** Large file threshold in bytes. */
  var largeFileThreshold: Long
    get() = prefs.getLong("large_file_threshold", DEFAULT_LARGE_FILE_THRESHOLD.toLong())
    set(value) {
      prefs.edit().putLong("large_file_threshold", value).apply()
      log.info("Large file threshold set to: $value bytes")
    }

  /** Large project threshold (number of files). */
  var largeProjectThreshold: Int
    get() = prefs.getInt("large_project_threshold", DEFAULT_LARGE_PROJECT_THRESHOLD)
    set(value) {
      prefs.edit().putInt("large_project_threshold", value).apply()
      log.info("Large project threshold set to: $value files")
    }

  /** Enable memory optimization. */
  var isOptimizationEnabled: Boolean
    get() = prefs.getBoolean("optimization_enabled", true)
    set(value) {
      prefs.edit().putBoolean("optimization_enabled", value).apply()
      log.info("Memory optimization ${if (value) "enabled" else "disabled"}")
    }

  /** Enable aggressive cleanup. */
  var isAggressiveCleanupEnabled: Boolean
    get() = prefs.getBoolean("aggressive_cleanup_enabled", false)
    set(value) {
      prefs.edit().putBoolean("aggressive_cleanup_enabled", value).apply()
      log.info("Aggressive cleanup ${if (value) "enabled" else "disabled"}")
    }

  /** Enable chart memory optimization. */
  var isChartOptimizationEnabled: Boolean
    get() = prefs.getBoolean("chart_optimization_enabled", true)
    set(value) {
      prefs.edit().putBoolean("chart_optimization_enabled", value).apply()
      log.info("Chart optimization ${if (value) "enabled" else "disabled"}")
    }

  /** Enable large project optimization. */
  var isLargeProjectOptimizationEnabled: Boolean
    get() = prefs.getBoolean("large_project_optimization_enabled", true)
    set(value) {
      prefs.edit().putBoolean("large_project_optimization_enabled", value).apply()
      log.info("Large project optimization ${if (value) "enabled" else "disabled"}")
    }

  /** Reset all settings to defaults. */
  fun resetToDefaults() {
    prefs.edit().clear().apply()
    log.info("Memory optimization settings reset to defaults")
  }

  /** Get all current settings as a map. */
  fun getAllSettings(): Map<String, Any> {
    return mapOf(
        "memory_pressure_threshold" to memoryPressureThreshold,
        "chart_update_interval" to chartUpdateInterval,
        "max_cache_size" to maxCacheSize,
        "large_file_threshold" to largeFileThreshold,
        "large_project_threshold" to largeProjectThreshold,
        "optimization_enabled" to isOptimizationEnabled,
        "aggressive_cleanup_enabled" to isAggressiveCleanupEnabled,
        "chart_optimization_enabled" to isChartOptimizationEnabled,
        "large_project_optimization_enabled" to isLargeProjectOptimizationEnabled,
    )
  }

  /** Apply optimized settings for low-memory devices. */
  fun applyLowMemorySettings() {
    memoryPressureThreshold = 70
    chartUpdateInterval = 5000L
    maxCacheSize = 25
    largeFileThreshold = 512 * 1024L // 512KB
    largeProjectThreshold = 500
    isOptimizationEnabled = true
    isAggressiveCleanupEnabled = true
    isChartOptimizationEnabled = true
    isLargeProjectOptimizationEnabled = true

    log.info("Applied low memory settings")
  }

  /** Apply optimized settings for high-memory devices. */
  fun applyHighMemorySettings() {
    memoryPressureThreshold = 90
    chartUpdateInterval = 1000L
    maxCacheSize = 100
    largeFileThreshold = 2 * 1024 * 1024L // 2MB
    largeProjectThreshold = 2000
    isOptimizationEnabled = true
    isAggressiveCleanupEnabled = false
    isChartOptimizationEnabled = false
    isLargeProjectOptimizationEnabled = false

    log.info("Applied high memory settings")
  }
}
