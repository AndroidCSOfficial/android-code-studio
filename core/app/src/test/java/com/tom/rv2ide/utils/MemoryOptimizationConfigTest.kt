/*
 *  This file is part of Android Code Studio.
 *
 *  Android Code Studio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Android Code Studio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Android Code Studio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for MemoryOptimizationConfig
 * 
 * @author Android Code Studio Team
 */
@RunWith(RobolectricTestRunner::class)
class MemoryOptimizationConfigTest {
    
    private lateinit var context: Context
    private lateinit var config: MemoryOptimizationConfig
    
    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences
        context.getSharedPreferences("memory_optimization", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
        config = MemoryOptimizationConfig.getInstance(context)
    }
    
    @Test
    fun testDefaultMemoryPressureThreshold() {
        assertEquals(85, config.memoryPressureThreshold)
    }
    
    @Test
    fun testDefaultChartUpdateInterval() {
        assertEquals(2000L, config.chartUpdateInterval)
    }
    
    @Test
    fun testDefaultCacheSize() {
        assertEquals(50, config.maxCacheSize)
    }
    
    @Test
    fun testDefaultLargeFileThreshold() {
        assertEquals(1024 * 1024L, config.largeFileThreshold)
    }
    
    @Test
    fun testDefaultLargeProjectThreshold() {
        assertEquals(1000, config.largeProjectThreshold)
    }
    
    @Test
    fun testOptimizationEnabledByDefault() {
        assertTrue(config.isOptimizationEnabled)
    }
    
    @Test
    fun testLowMemorySettingsApplied() {
        config.applyLowMemorySettings()
        
        assertEquals(70, config.memoryPressureThreshold)
        assertEquals(5000L, config.chartUpdateInterval)
        assertEquals(25, config.maxCacheSize)
        assertEquals(512 * 1024L, config.largeFileThreshold)
        assertEquals(500, config.largeProjectThreshold)
        assertTrue(config.isOptimizationEnabled)
        assertTrue(config.isAggressiveCleanupEnabled)
        assertTrue(config.isChartOptimizationEnabled)
        assertTrue(config.isLargeProjectOptimizationEnabled)
    }
    
    @Test
    fun testHighMemorySettingsApplied() {
        config.applyHighMemorySettings()
        
        assertEquals(90, config.memoryPressureThreshold)
        assertEquals(1000L, config.chartUpdateInterval)
        assertEquals(100, config.maxCacheSize)
        assertEquals(2 * 1024 * 1024L, config.largeFileThreshold)
        assertEquals(2000, config.largeProjectThreshold)
        assertTrue(config.isOptimizationEnabled)
    }
    
    @Test
    fun testResetToDefaults() {
        // Apply low memory settings first
        config.applyLowMemorySettings()
        
        // Reset to defaults
        config.resetToDefaults()
        
        // Verify defaults are restored
        assertEquals(85, config.memoryPressureThreshold)
        assertEquals(2000L, config.chartUpdateInterval)
        assertEquals(50, config.maxCacheSize)
        assertEquals(1024 * 1024L, config.largeFileThreshold)
        assertEquals(1000, config.largeProjectThreshold)
    }
    
    @Test
    fun testGetAllSettingsReturnsComplete() {
        val settings = config.getAllSettings()
        
        assertTrue(settings.containsKey("memory_pressure_threshold"))
        assertTrue(settings.containsKey("chart_update_interval"))
        assertTrue(settings.containsKey("max_cache_size"))
        assertTrue(settings.containsKey("large_file_threshold"))
        assertTrue(settings.containsKey("large_project_threshold"))
        assertTrue(settings.containsKey("optimization_enabled"))
        assertTrue(settings.containsKey("aggressive_cleanup_enabled"))
        assertTrue(settings.containsKey("chart_optimization_enabled"))
        assertTrue(settings.containsKey("large_project_optimization_enabled"))
        
        assertEquals(9, settings.size)
    }
    
    @Test
    fun testMemoryPressureThresholdPersistence() {
        config.memoryPressureThreshold = 75
        
        // Recreate config instance to verify persistence
        val newConfig = MemoryOptimizationConfig.getInstance(context)
        assertEquals(75, newConfig.memoryPressureThreshold)
    }
    
    @Test
    fun testOptimizationToggle() {
        config.isOptimizationEnabled = false
        assertTrue(!config.isOptimizationEnabled)
        
        config.isOptimizationEnabled = true
        assertTrue(config.isOptimizationEnabled)
    }
    
    @Test
    fun testAggressiveCleanupToggle() {
        config.isAggressiveCleanupEnabled = true
        assertTrue(config.isAggressiveCleanupEnabled)
        
        config.isAggressiveCleanupEnabled = false
        assertTrue(!config.isAggressiveCleanupEnabled)
    }
    
    @Test
    fun testCacheSettingsValidation() {
        config.maxCacheSize = 100
        config.largeFileThreshold = 2048 * 1024L
        
        val settings = config.getAllSettings()
        assertEquals(100, settings["max_cache_size"])
        assertEquals(2048 * 1024L, settings["large_file_threshold"])
    }
}
