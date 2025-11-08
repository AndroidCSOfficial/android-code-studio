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
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * A helper class for managing and handling package IDs within the Android project. Provides
 * functionality to set, get, validate, and manage package identifiers. Each instance is associated
 * with a specific project identifier.
 */
class PackageHelper
private constructor(private val context: Context, private val projectId: String) {

  companion object {
    private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "package_settings")

    /**
     * Creates a new PackageHelper instance for a specific project.
     *
     * @param context Application context
     * @param projectId Unique identifier for the project (e.g., module name, project name)
     */
    fun createForProject(context: Context, projectId: String): PackageHelper {
      return PackageHelper(context.applicationContext, projectId)
    }
  }

  private val PACKAGE_ID_KEY = stringPreferencesKey("package_id_$projectId")

  private var currentPackageId: String = runBlocking {
    context.dataStore.data.map { preferences -> preferences[PACKAGE_ID_KEY] ?: "" }.first()
  }

  /**
   * Sets the current package ID and persists it to DataStore.
   *
   * @param packageId The package ID to set
   * @throws IllegalArgumentException if the package ID is invalid
   */
  suspend fun setPackageId(packageId: String) {
    if (!isValidPackageId(packageId)) {
      throw IllegalArgumentException("Invalid package ID format: $packageId")
    }

    synchronized(this) { currentPackageId = packageId }

    context.dataStore.edit { preferences -> preferences[PACKAGE_ID_KEY] = packageId }
  }

  /**
   * Sets the current package ID synchronously (blocks the current thread). Use the suspend version
   * when possible.
   *
   * @param packageId The package ID to set
   * @throws IllegalArgumentException if the package ID is invalid
   */
  fun setPackageIdBlocking(packageId: String) = runBlocking { setPackageId(packageId) }

  /**
   * Gets the current package ID.
   *
   * @return The current package ID, or empty string if not set
   */
  fun getPackageId(): String {
    return currentPackageId
  }

  /**
   * Gets the package ID as a Flow for reactive updates.
   *
   * @return Flow of package ID
   */
  fun getPackageIdFlow(): Flow<String> {
    return context.dataStore.data.map { preferences -> preferences[PACKAGE_ID_KEY] ?: "" }
  }

  /**
   * Gets the package ID from DataStore.
   *
   * @return The stored package ID or empty string if not set
   */
  suspend fun getStoredPackageId(): String {
    return context.dataStore.data.map { preferences -> preferences[PACKAGE_ID_KEY] ?: "" }.first()
  }

  /**
   * Gets the stored package ID synchronously (blocks the current thread). Use the suspend version
   * when possible.
   */
  fun getStoredPackageIdBlocking(): String = runBlocking { getStoredPackageId() }

  /**
   * Validates if the given string is a valid package ID.
   *
   * @param packageId The package ID to validate
   * @return true if the package ID is valid, false otherwise
   */
  fun isValidPackageId(packageId: String): Boolean {
    if (packageId.isBlank()) return false

    // Android package naming conventions:
    // - At least two segments separated by dots
    // - Each segment must start with a letter
    // - Can contain letters, numbers, and underscores
    // - No reserved keywords
    val packagePattern = "^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$".toRegex()

    return packagePattern.matches(packageId) && packageId.length in 3..255
  }

  /** Clears the stored package ID from DataStore. */
  suspend fun clearStoredPackageId() {
    synchronized(this) { currentPackageId = "" }

    context.dataStore.edit { preferences -> preferences.remove(PACKAGE_ID_KEY) }
  }

  /**
   * Clears the stored package ID synchronously (blocks the current thread). Use the suspend version
   * when possible.
   */
  fun clearStoredPackageIdBlocking() = runBlocking { clearStoredPackageId() }

  /**
   * Checks if a package ID is currently set.
   *
   * @return true if a package ID is set, false otherwise
   */
  fun hasPackageId(): Boolean {
    return currentPackageId.isNotBlank()
  }

  /**
   * Extracts the application ID (last segment) from the package ID.
   *
   * @return The application ID segment, or empty string if no package ID is set
   */
  fun getApplicationId(): String {
    if (currentPackageId.isBlank()) return ""
    return currentPackageId.substringAfterLast('.', "app")
  }

  /**
   * Extracts the base package (everything except the last segment) from the package ID.
   *
   * @return The base package segments, or empty string if no package ID is set
   */
  fun getBasePackage(): String {
    if (currentPackageId.isBlank()) return ""

    val lastDotIndex = currentPackageId.lastIndexOf('.')
    return if (lastDotIndex != -1) {
      currentPackageId.substring(0, lastDotIndex)
    } else {
      currentPackageId
    }
  }

  /**
   * Converts package ID to a directory path.
   *
   * @return Directory path representation of the package ID, or empty string if no package ID is
   *   set
   */
  fun toDirectoryPath(): String {
    if (currentPackageId.isBlank()) return ""
    return currentPackageId.replace('.', '/')
  }

  /**
   * Safely sets package ID only if it's valid.
   *
   * @param packageId The package ID to set
   * @return true if package ID was set successfully, false if invalid
   */
  suspend fun safeSetPackageId(packageId: String): Boolean {
    return if (isValidPackageId(packageId)) {
      setPackageId(packageId)
      true
    } else {
      false
    }
  }

  /**
   * Safely sets package ID synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun safeSetPackageIdBlocking(packageId: String): Boolean = runBlocking {
    safeSetPackageId(packageId)
  }

  /** Gets the project identifier associated with this instance. */
  fun getProjectId(): String {
    return projectId
  }
}
