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

package com.tom.androidcodestudio.project.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/** A helper class for managing Android SDK versions (minimum, target, and maximum). */
class SdkVersionHelper private constructor(private val context: Context) {

  companion object {
    private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "sdk_version_settings")
    private val MIN_SDK_KEY = intPreferencesKey("pref_min_sdk_version")
    private val TARGET_SDK_KEY = intPreferencesKey("pref_target_sdk_version")
    private val MAX_SDK_KEY = intPreferencesKey("pref_max_sdk_version")

    @Volatile private var instance: SdkVersionHelper? = null

    /**
     * Gets the singleton instance of SdkVersionHelper.
     *
     * @param context Application context
     */
    fun getInstance(context: Context): SdkVersionHelper {
      return instance
          ?: synchronized(this) {
            instance ?: SdkVersionHelper(context.applicationContext).also { instance = it }
          }
    }
  }

  private var currentMinSdk: Int = 0
  private var currentTargetSdk: Int = 0
  private var currentMaxSdk: Int = 0

  init {
    runBlocking {
      context.dataStore.data
          .map { preferences ->
            currentMinSdk = preferences[MIN_SDK_KEY] ?: 0
            currentTargetSdk = preferences[TARGET_SDK_KEY] ?: 0
            currentMaxSdk = preferences[MAX_SDK_KEY] ?: 0
          }
          .first()
    }
  }

  /**
   * Sets the minimum SDK version and persists it to DataStore.
   *
   * @param minSdk The minimum SDK version to set
   */
  suspend fun setMinSdk(minSdk: Int) {
    synchronized(this) { currentMinSdk = minSdk }

    context.dataStore.edit { preferences -> preferences[MIN_SDK_KEY] = minSdk }
  }

  /**
   * Sets the minimum SDK version synchronously (blocks the current thread). Use the suspend version
   * when possible.
   *
   * @param minSdk The minimum SDK version to set
   */
  fun setMinSdkBlocking(minSdk: Int) = runBlocking { setMinSdk(minSdk) }

  /**
   * Sets the target SDK version and persists it to DataStore.
   *
   * @param targetSdk The target SDK version to set
   */
  suspend fun setTargetSdk(targetSdk: Int) {
    synchronized(this) { currentTargetSdk = targetSdk }

    context.dataStore.edit { preferences -> preferences[TARGET_SDK_KEY] = targetSdk }
  }

  /**
   * Sets the target SDK version synchronously (blocks the current thread). Use the suspend version
   * when possible.
   *
   * @param targetSdk The target SDK version to set
   */
  fun setTargetSdkBlocking(targetSdk: Int) = runBlocking { setTargetSdk(targetSdk) }

  /**
   * Sets the maximum SDK version and persists it to DataStore.
   *
   * @param maxSdk The maximum SDK version to set
   */
  suspend fun setMaxSdk(maxSdk: Int) {
    synchronized(this) { currentMaxSdk = maxSdk }

    context.dataStore.edit { preferences -> preferences[MAX_SDK_KEY] = maxSdk }
  }

  /**
   * Sets the maximum SDK version synchronously (blocks the current thread). Use the suspend version
   * when possible.
   *
   * @param maxSdk The maximum SDK version to set
   */
  fun setMaxSdkBlocking(maxSdk: Int) = runBlocking { setMaxSdk(maxSdk) }

  /**
   * Gets the current minimum SDK version.
   *
   * @return The minimum SDK version, or 0 if not set
   */
  fun getMinSdk(): Int {
    return currentMinSdk
  }

  /**
   * Gets the current target SDK version.
   *
   * @return The target SDK version, or 0 if not set
   */
  fun getTargetSdk(): Int {
    return currentTargetSdk
  }

  /**
   * Gets the current maximum SDK version.
   *
   * @return The maximum SDK version, or 0 if not set
   */
  fun getMaxSdk(): Int {
    return currentMaxSdk
  }

  /**
   * Gets the minimum SDK version as a Flow for reactive updates.
   *
   * @return Flow of minimum SDK version
   */
  fun getMinSdkFlow(): Flow<Int> {
    return context.dataStore.data.map { preferences -> preferences[MIN_SDK_KEY] ?: 0 }
  }

  /**
   * Gets the target SDK version as a Flow for reactive updates.
   *
   * @return Flow of target SDK version
   */
  fun getTargetSdkFlow(): Flow<Int> {
    return context.dataStore.data.map { preferences -> preferences[TARGET_SDK_KEY] ?: 0 }
  }

  /**
   * Gets the maximum SDK version as a Flow for reactive updates.
   *
   * @return Flow of maximum SDK version
   */
  fun getMaxSdkFlow(): Flow<Int> {
    return context.dataStore.data.map { preferences -> preferences[MAX_SDK_KEY] ?: 0 }
  }

  /**
   * Gets the stored minimum SDK version from DataStore.
   *
   * @return The stored minimum SDK version or 0 if not set
   */
  suspend fun getStoredMinSdk(): Int {
    return context.dataStore.data.map { preferences -> preferences[MIN_SDK_KEY] ?: 0 }.first()
  }

  /**
   * Gets the stored target SDK version from DataStore.
   *
   * @return The stored target SDK version or 0 if not set
   */
  suspend fun getStoredTargetSdk(): Int {
    return context.dataStore.data.map { preferences -> preferences[TARGET_SDK_KEY] ?: 0 }.first()
  }

  /**
   * Gets the stored maximum SDK version from DataStore.
   *
   * @return The stored maximum SDK version or 0 if not set
   */
  suspend fun getStoredMaxSdk(): Int {
    return context.dataStore.data.map { preferences -> preferences[MAX_SDK_KEY] ?: 0 }.first()
  }

  /** Clears all stored SDK versions from DataStore. */
  suspend fun clear() {
    synchronized(this) {
      currentMinSdk = 0
      currentTargetSdk = 0
      currentMaxSdk = 0
    }

    context.dataStore.edit { preferences ->
      preferences.remove(MIN_SDK_KEY)
      preferences.remove(TARGET_SDK_KEY)
      preferences.remove(MAX_SDK_KEY)
    }
  }

  /**
   * Clears all stored SDK versions synchronously (blocks the current thread). Use the suspend
   * version when possible.
   */
  fun clearBlocking() = runBlocking { clear() }

  /**
   * Sets all SDK versions at once.
   *
   * @param minSdk The minimum SDK version
   * @param targetSdk The target SDK version
   * @param maxSdk The maximum SDK version
   */
  suspend fun setAllSdkVersions(minSdk: Int, targetSdk: Int, maxSdk: Int) {
    synchronized(this) {
      currentMinSdk = minSdk
      currentTargetSdk = targetSdk
      currentMaxSdk = maxSdk
    }

    context.dataStore.edit { preferences ->
      preferences[MIN_SDK_KEY] = minSdk
      preferences[TARGET_SDK_KEY] = targetSdk
      preferences[MAX_SDK_KEY] = maxSdk
    }
  }

  /**
   * Sets all SDK versions synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun setAllSdkVersionsBlocking(minSdk: Int, targetSdk: Int, maxSdk: Int) = runBlocking {
    setAllSdkVersions(minSdk, targetSdk, maxSdk)
  }

  /**
   * Validates if the SDK versions are properly configured.
   *
   * @return true if minSdk <= targetSdk <= maxSdk, false otherwise
   */
  fun isValidConfiguration(): Boolean {
    return currentMinSdk > 0 &&
        currentTargetSdk >= currentMinSdk &&
        (currentMaxSdk == 0 || currentMaxSdk >= currentTargetSdk)
  }
}
