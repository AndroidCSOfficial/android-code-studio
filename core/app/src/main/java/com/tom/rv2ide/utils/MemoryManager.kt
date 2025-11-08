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
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory

/**
 * Comprehensive memory management system for AndroidIDE. Handles memory pressure, cleanup, and
 * optimization strategies.
 *
 * @author AndroidIDE Team
 */
class MemoryManager private constructor(private val context: WeakReference<Context>?) {

  private val log = LoggerFactory.getLogger(MemoryManager::class.java)
  private val memoryPressureListeners = mutableSetOf<MemoryPressureListener>()
  private val cleanupTasks = ConcurrentHashMap<String, CleanupTask>()
  private val isMonitoring = AtomicBoolean(false)
  private val monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

  companion object {
    @Volatile private var INSTANCE: MemoryManager? = null

    fun getInstance(context: Context? = null): MemoryManager {
      return INSTANCE
          ?: synchronized(this) { INSTANCE ?: MemoryManager(context).also { INSTANCE = it } }
    }

    const val CRITICAL_MEMORY_THRESHOLD = 90 // Percentage
    const val HIGH_MEMORY_THRESHOLD = 80 // Percentage
    const val MEDIUM_MEMORY_THRESHOLD = 70 // Percentage
    const val MEMORY_CHECK_INTERVAL = 5000L // 5 seconds
  }

  private constructor(context: Context?) : this(WeakReference(context)) {}

  /** Start monitoring memory usage and perform automatic cleanup. */
  fun startMonitoring() {
    if (isMonitoring.get()) {
      log.warn("Memory monitoring is already active")
      return
    }

    isMonitoring.set(true)
    log.info("Starting memory monitoring")

    monitoringScope.launch {
      while (isMonitoring.get()) {
        checkMemoryPressure()
        delay(MEMORY_CHECK_INTERVAL)
      }
    }
  }

  /** Stop memory monitoring. */
  fun stopMonitoring() {
    isMonitoring.set(false)
    monitoringScope.cancel()
    log.info("Stopped memory monitoring")
  }

  /** Check current memory pressure and trigger appropriate cleanup. */
  private fun checkMemoryPressure() {
    val memoryInfo = getMemoryInfo() ?: return
    val pressureLevel = calculateMemoryPressure(memoryInfo)

    when (pressureLevel) {
      MemoryPressureLevel.CRITICAL -> {
        log.warn("Critical memory pressure detected: ${memoryInfo.usedPercent}%")
        performCriticalCleanup()
        notifyMemoryPressure(MemoryPressureLevel.CRITICAL, memoryInfo)
      }
      MemoryPressureLevel.HIGH -> {
        log.warn("High memory pressure detected: ${memoryInfo.usedPercent}%")
        performHighCleanup()
        notifyMemoryPressure(MemoryPressureLevel.HIGH, memoryInfo)
      }
      MemoryPressureLevel.MEDIUM -> {
        log.info("Medium memory pressure detected: ${memoryInfo.usedPercent}%")
        performMediumCleanup()
        notifyMemoryPressure(MemoryPressureLevel.MEDIUM, memoryInfo)
      }
      MemoryPressureLevel.LOW -> {
        // No action needed
      }
    }
  }

  /** Get current memory information. */
  private fun getMemoryInfo(): MemoryInfo? {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory = runtime.maxMemory()
    val usedPercent = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt()

    return MemoryInfo(
        usedMemory = usedMemory,
        maxMemory = maxMemory,
        freeMemory = runtime.freeMemory(),
        usedPercent = usedPercent,
    )
  }

  /** Calculate memory pressure level. */
  private fun calculateMemoryPressure(memoryInfo: MemoryInfo): MemoryPressureLevel {
    return when {
      memoryInfo.usedPercent >= CRITICAL_MEMORY_THRESHOLD -> MemoryPressureLevel.CRITICAL
      memoryInfo.usedPercent >= HIGH_MEMORY_THRESHOLD -> MemoryPressureLevel.HIGH
      memoryInfo.usedPercent >= MEDIUM_MEMORY_THRESHOLD -> MemoryPressureLevel.MEDIUM
      else -> MemoryPressureLevel.LOW
    }
  }

  /** Perform critical memory cleanup. */
  private fun performCriticalCleanup() {
    log.warn("Performing critical memory cleanup")

    // Force garbage collection multiple times
    System.gc()
    Thread.sleep(100)
    System.gc()
    Thread.sleep(100)
    System.gc()

    // Execute all cleanup tasks
    cleanupTasks.values.forEach { task ->
      try {
        task.performCriticalCleanup()
      } catch (e: Exception) {
        log.error("Error during critical cleanup task: ${task.name}", e)
      }
    }

    // Clear caches
    clearSystemCaches()

    // Additional aggressive cleanup
    try {
      Runtime.getRuntime().gc()
      Thread.sleep(50)
    } catch (e: Exception) {
      log.error("Error during additional cleanup", e)
    }
  }

  /** Perform high memory cleanup. */
  private fun performHighCleanup() {
    log.info("Performing high memory cleanup")

    // Execute high priority cleanup tasks
    cleanupTasks.values
        .filter { it.priority >= CleanupPriority.HIGH }
        .forEach { task ->
          try {
            task.performHighCleanup()
          } catch (e: Exception) {
            log.error("Error during high cleanup task: ${task.name}", e)
          }
        }
  }

  /** Perform medium memory cleanup. */
  private fun performMediumCleanup() {
    log.info("Performing medium memory cleanup")

    // Execute medium priority cleanup tasks
    cleanupTasks.values
        .filter { it.priority >= CleanupPriority.MEDIUM }
        .forEach { task ->
          try {
            task.performMediumCleanup()
          } catch (e: Exception) {
            log.error("Error during medium cleanup task: ${task.name}", e)
          }
        }
  }

  /** Clear system caches. */
  private fun clearSystemCaches() {
    try {
      // DO NOT clear application user data - this deletes all project files!
      // Only clear memory caches, not file system data
      log.info("Skipping system cache clearing to preserve user data")
    } catch (e: Exception) {
      log.error("Error clearing system caches", e)
    }
  }

  /** Register a cleanup task. */
  fun registerCleanupTask(task: CleanupTask) {
    cleanupTasks[task.name] = task
    log.info("Registered cleanup task: ${task.name}")
  }

  /** Unregister a cleanup task. */
  fun unregisterCleanupTask(name: String) {
    cleanupTasks.remove(name)
    log.info("Unregistered cleanup task: $name")
  }

  /** Add memory pressure listener. */
  fun addMemoryPressureListener(listener: MemoryPressureListener) {
    memoryPressureListeners.add(listener)
  }

  /** Remove memory pressure listener. */
  fun removeMemoryPressureListener(listener: MemoryPressureListener) {
    memoryPressureListeners.remove(listener)
  }

  /** Notify listeners about memory pressure. */
  private fun notifyMemoryPressure(level: MemoryPressureLevel, memoryInfo: MemoryInfo) {
    memoryPressureListeners.forEach { listener ->
      try {
        listener.onMemoryPressure(level, memoryInfo)
      } catch (e: Exception) {
        log.error("Error notifying memory pressure listener", e)
      }
    }
  }

  /** Get current memory statistics. */
  fun getMemoryStatistics(): MemoryInfo? {
    return getMemoryInfo()
  }

  /** Force memory cleanup. */
  fun forceCleanup() {
    log.info("Forcing memory cleanup")
    performCriticalCleanup()
  }

  /** Memory pressure levels. */
  enum class MemoryPressureLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
  }

  /** Cleanup priority levels. */
  enum class CleanupPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL,
  }

  /** Memory information data class. */
  data class MemoryInfo(
      val usedMemory: Long,
      val maxMemory: Long,
      val freeMemory: Long,
      val usedPercent: Int,
  )

  /** Memory pressure listener interface. */
  interface MemoryPressureListener {
    fun onMemoryPressure(level: MemoryPressureLevel, memoryInfo: MemoryInfo)
  }

  /** Cleanup task interface. */
  interface CleanupTask {
    val name: String
    val priority: CleanupPriority

    fun performMediumCleanup()

    fun performHighCleanup()

    fun performCriticalCleanup()
  }
}
