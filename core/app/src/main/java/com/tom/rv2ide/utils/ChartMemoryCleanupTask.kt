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

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import java.lang.ref.WeakReference
import org.slf4j.LoggerFactory

/**
 * Cleanup task specifically for chart memory optimization. Reduces memory usage by optimizing chart
 * data and rendering.
 *
 * @author AndroidIDE Team
 */
class ChartMemoryCleanupTask : MemoryManager.CleanupTask {

  private val log = LoggerFactory.getLogger(ChartMemoryCleanupTask::class.java)
  private val chartReferences = mutableSetOf<WeakReference<LineChart>>()

  override val name: String = "ChartMemoryCleanup"
  override val priority: MemoryManager.CleanupPriority = MemoryManager.CleanupPriority.HIGH

  /** Register a chart for memory cleanup. */
  fun registerChart(chart: LineChart) {
    chartReferences.add(WeakReference(chart))
    log.debug("Registered chart for memory cleanup")
  }

  /** Unregister a chart from memory cleanup. */
  fun unregisterChart(chart: LineChart) {
    chartReferences.removeAll { it.get() == chart }
    log.debug("Unregistered chart from memory cleanup")
  }

  override fun performMediumCleanup() {
    log.debug("Performing medium chart cleanup")

    // Reduce chart data points
    chartReferences.forEach { ref ->
      ref.get()?.let { chart -> optimizeChartData(chart, reduceDataPoints = true) }
    }
  }

  override fun performHighCleanup() {
    log.debug("Performing high chart cleanup")

    // More aggressive data reduction
    chartReferences.forEach { ref ->
      ref.get()?.let { chart ->
        optimizeChartData(chart, reduceDataPoints = true, clearHistory = true)
      }
    }
  }

  override fun performCriticalCleanup() {
    log.debug("Performing critical chart cleanup")

    // Maximum cleanup - disable charts if necessary
    chartReferences.forEach { ref ->
      ref.get()?.let { chart ->
        if (isMemoryCritical()) {
          disableChart(chart)
        } else {
          optimizeChartData(
              chart,
              reduceDataPoints = true,
              clearHistory = true,
              minimizeRendering = true,
          )
        }
      }
    }
  }

  /** Optimize chart data to reduce memory usage. */
  private fun optimizeChartData(
      chart: LineChart,
      reduceDataPoints: Boolean = false,
      clearHistory: Boolean = false,
      minimizeRendering: Boolean = false,
  ) {
    try {
      val data = chart.data as? LineData ?: return

      // Reduce data points by sampling
      if (reduceDataPoints) {
        data.dataSets.forEach { dataSet ->
          if (dataSet is LineDataSet && dataSet.entryCount > 10) {
            val entries = dataSet.entries.toMutableList()
            val sampledEntries = mutableListOf<Entry>()

            // Sample every other point to reduce memory
            for (i in entries.indices step 2) {
              sampledEntries.add(entries[i])
            }

            dataSet.clear()
            dataSet.values = sampledEntries
          }
        }
      }

      // Clear old history data
      if (clearHistory) {
        data.dataSets.forEach { dataSet ->
          if (dataSet is LineDataSet && dataSet.entryCount > 5) {
            val entries = dataSet.entries.toMutableList()
            // Keep only the last 5 entries
            if (entries.size > 5) {
              val recentEntries = entries.takeLast(5)
              dataSet.clear()
              dataSet.values = recentEntries
            }
          }
        }
      }

      // Minimize rendering features
      if (minimizeRendering) {
        chart.setDrawGridBackground(false)
        chart.setDrawBorders(false)
        chart.description.isEnabled = false
        chart.legend.isEnabled = false

        data.dataSets.forEach { dataSet ->
          if (dataSet is LineDataSet) {
            dataSet.setDrawCircles(false)
            dataSet.setDrawCircleHole(false)
            dataSet.setDrawValues(false)
            dataSet.setDrawIcons(false)
            dataSet.formLineWidth = 1f
          }
        }
      }

      chart.notifyDataSetChanged()
      chart.invalidate()
    } catch (e: Exception) {
      log.error("Error optimizing chart data", e)
    }
  }

  /** Disable chart to save memory. */
  private fun disableChart(chart: LineChart) {
    try {
      chart.clear()
      chart.data = LineData()
      chart.visibility = android.view.View.GONE
      log.warn("Disabled chart due to critical memory pressure")
    } catch (e: Exception) {
      log.error("Error disabling chart", e)
    }
  }

  /** Check if memory is critically low. */
  private fun isMemoryCritical(): Boolean {
    val runtime = Runtime.getRuntime()
    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
    val maxMemory = runtime.maxMemory()
    val usedPercent = (usedMemory.toFloat() / maxMemory.toFloat() * 100).toInt()

    return usedPercent >= 95 // 95% or higher is critical
  }

  /** Clean up weak references to prevent memory leaks. */
  fun cleanupWeakReferences() {
    chartReferences.removeAll { it.get() == null }
  }
}
