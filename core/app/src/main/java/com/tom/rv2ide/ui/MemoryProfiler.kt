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
 *  along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.ui

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import com.tom.rv2ide.utils.MemoryOptimizationConfig
import org.slf4j.LoggerFactory

/**
 * Memory Profiler para monitoreo en tiempo real
 * Integración con UI y sistema de optimización
 *
 * Mejora: Detección automática de memory pressure
 * Triggerea optimizaciones adaptativas
 */
class MemoryProfiler(private val context: Context) {
    
    private val log = LoggerFactory.getLogger(MemoryProfiler::class.java)
    private val activityManager = 
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryConfig = MemoryOptimizationConfig.getInstance(context)
    
    private val memoryHistory = mutableListOf<MemorySnapshot>()
    private val MAX_HISTORY = 100
    
    /**
     * Snapshot de memoria en un punto en tiempo
     */
    data class MemorySnapshot(
        val timestamp: Long,
        val totalMemory: Long,
        val freeMemory: Long,
        val usedMemory: Long,
        val maxMemory: Long,
        val pressureRatio: Float,
        val isLowMemory: Boolean
    )
    
    /**
     * Obtiene estadísticas de memoria del sistema
     */
    fun getMemoryStats(): MemoryStats {
        val runtime = Runtime.getRuntime()
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()
        val nativeHeap = Debug.getNativePss()
        
        // Calcular ratio de memory pressure
        val pressureRatio = if (memInfo.totalMem > 0) {
            ((memInfo.totalMem - memInfo.availMem) * 100f / memInfo.totalMem)
        } else {
            0f
        }
        
        val stats = MemoryStats(
            totalMemory = totalMemory,
            freeMemory = freeMemory,
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            nativeHeap = nativeHeap,
            systemMemory = memInfo.totalMem,
            availableMemory = memInfo.availMem,
            isLowMemory = memInfo.lowMemory,
            memoryPressureRatio = pressureRatio
        )
        
        // Agregar a historial
        addSnapshot(stats)
        
        return stats
    }
    
    /**
     * Monitorea memory alerts
     */
    fun checkMemoryAlert() {
        val stats = getMemoryStats()
        
        if (stats.memoryPressureRatio > memoryConfig.memoryPressureThreshold) {
            // ⚠️ ALERTA: Memory pressure alta
            log.warn(
                "High memory pressure detected: ${String.format("%.1f%%", stats.memoryPressureRatio)} " +
                "(threshold: ${memoryConfig.memoryPressureThreshold}%)"
            )
            triggerOptimizations(stats)
        }
    }
    
    /**
     * Triggerea optimizaciones cuando detecta memory pressure
     */
    private fun triggerOptimizations(stats: MemoryStats) {
        if (!memoryConfig.isOptimizationEnabled) {
            log.debug("Optimizations disabled")
            return
        }
        
        if (memoryConfig.isAggressiveCleanupEnabled) {
            log.info("Triggering aggressive memory cleanup...")
            performAggressiveCleanup()
        } else {
            log.info("Triggering standard memory cleanup...")
            performStandardCleanup()
        }
    }
    
    /**
     * Limpieza estándar de memoria
     */
    private fun performStandardCleanup() {
        try {
            System.gc()
            log.debug("Standard cleanup completed")
        } catch (e: Exception) {
            log.error("Error during standard cleanup", e)
        }
    }
    
    /**
     * Limpieza agresiva de memoria
     */
    private fun performAggressiveCleanup() {
        try {
            System.gc()
            System.runFinalization()
            clearSystemCaches()
            log.info("Aggressive cleanup completed")
        } catch (e: Exception) {
            log.error("Error during aggressive cleanup", e)
        }
    }
    
    /**
     * Limpia caches del sistema
     */
    private fun clearSystemCaches() {
        try {
            // Limpiar runtime caches
            Runtime.getRuntime().gc()
            log.debug("System caches cleared")
        } catch (e: Exception) {
            log.error("Error clearing system caches", e)
        }
    }
    
    /**
     * Agrega un snapshot al historial
     */
    private fun addSnapshot(stats: MemoryStats) {
        val snapshot = MemorySnapshot(
            timestamp = System.currentTimeMillis(),
            totalMemory = stats.totalMemory,
            freeMemory = stats.freeMemory,
            usedMemory = stats.usedMemory,
            maxMemory = stats.maxMemory,
            pressureRatio = stats.memoryPressureRatio,
            isLowMemory = stats.isLowMemory
        )
        
        memoryHistory.add(0, snapshot)
        if (memoryHistory.size > MAX_HISTORY) {
            memoryHistory.removeAt(memoryHistory.size - 1)
        }
    }
    
    /**
     * Obtiene el historial de memoria
     */
    fun getMemoryHistory(): List<MemorySnapshot> {
        return memoryHistory.toList()
    }
    
    /**
     * Calcula promedios del historial
     */
    fun getHistoryAverage(): MemorySnapshot? {
        if (memoryHistory.isEmpty()) return null
        
        val avgPressure = memoryHistory.map { it.pressureRatio }.average().toFloat()
        val recentSnapshot = memoryHistory.first()
        
        return recentSnapshot.copy(pressureRatio = avgPressure)
    }
    
    /**
     * Obtiene reporte de tendencias
     */
    fun getTrendReport(): MemoryTrend {
        if (memoryHistory.size < 2) {
            return MemoryTrend.STABLE
        }
        
        val oldest = memoryHistory.last()
        val newest = memoryHistory.first()
        val difference = newest.pressureRatio - oldest.pressureRatio
        
        return when {
            difference > 10 -> MemoryTrend.INCREASING
            difference < -10 -> MemoryTrend.DECREASING
            else -> MemoryTrend.STABLE
        }
    }
    
    enum class MemoryTrend {
        INCREASING,
        DECREASING,
        STABLE
    }
}

/**
 * Estadísticas de memoria del sistema
 */
data class MemoryStats(
    val totalMemory: Long,           // Heap total
    val freeMemory: Long,            // Heap libera
    val usedMemory: Long,            // Heap usada
    val maxMemory: Long,             // Max heap
    val nativeHeap: Int,             // Native memory
    val systemMemory: Long,          // Total del sistema
    val availableMemory: Long,       // Disponible en sistema
    val isLowMemory: Boolean,        // Flag de low memory
    val memoryPressureRatio: Float   // Porcentaje de presión
) {
    fun toFormattedString(): String {
        return """
            Memory Statistics
            ├─ Heap: ${formatBytes(usedMemory)} / ${formatBytes(maxMemory)}
            ├─ Free: ${formatBytes(freeMemory)}
            ├─ System Available: ${formatBytes(availableMemory)} / ${formatBytes(systemMemory)}
            ├─ Pressure: ${String.format("%.1f%%", memoryPressureRatio)}
            ├─ Native: ${formatBytes(nativeHeap.toLong())}
            ├─ Low Memory: $isLowMemory
            └─ Timestamp: ${System.currentTimeMillis()}
        """.trimIndent()
    }
    
    private companion object {
        private fun formatBytes(bytes: Long): String {
            return when {
                bytes < 1024L -> "$bytes B"
                bytes < 1024L * 1024L -> "${bytes / 1024L} KB"
                bytes < 1024L * 1024L * 1024L -> "${bytes / (1024L * 1024L)} MB"
                else -> "${bytes / (1024L * 1024L * 1024L)} GB"
            }
        }
    }
}
