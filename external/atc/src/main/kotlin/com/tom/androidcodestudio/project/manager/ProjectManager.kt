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
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/**
 * Data class representing a project tracked by the Android Template Creator.
 *
 * @property projectId Unique identifier for the project
 * @property projectName Human-readable name of the project
 * @property projectDir Absolute path to the project directory
 * @property projectType Type of the project (from ProjectType enum or "External")
 */
data class ProjectInfo(
    val projectId: String,
    val projectName: String,
    val projectDir: String,
    val projectType: String,
)

/** Enum representing the source of a project. */
enum class ProjectSource {
  TEMPLATE_CREATOR, // Created by Android Template Creator
  EXTERNAL, // External project detected and tracked
}

/**
 * A manager class for tracking and managing projects using DataStore. Handles both projects created
 * by the Android Template Creator and external projects.
 */
class ProjectManager private constructor(private val context: Context) {

  companion object {
    private val Context.dataStore: DataStore<Preferences> by
        preferencesDataStore(name = "project_tracking")

    @Volatile private var instance: ProjectManager? = null

    /**
     * Gets the singleton instance of ProjectManager.
     *
     * @param context Application context
     */
    fun getInstance(context: Context): ProjectManager {
      return instance
          ?: synchronized(this) {
            instance ?: ProjectManager(context.applicationContext).also { instance = it }
          }
    }
  }

  private val PROJECTS_KEY = stringPreferencesKey("tracked_projects")
  private val PROJECT_COUNTER_KEY = stringPreferencesKey("project_counter")

  /**
   * Adds a new project to the tracking system.
   *
   * @param projectInfo The project information to track
   * @param source The source of the project (template creator or external)
   */
  suspend fun addProject(
      projectInfo: ProjectInfo,
      source: ProjectSource = ProjectSource.TEMPLATE_CREATOR,
  ) {
    val projects = getTrackedProjects().toMutableList()

    // Check if project already exists
    if (projects.any { it.projectDir == projectInfo.projectDir }) {
      return // Project already tracked
    }

    projects.add(projectInfo)
    saveProjects(projects)
  }

  /**
   * Adds a new project synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun addProjectBlocking(
      projectInfo: ProjectInfo,
      source: ProjectSource = ProjectSource.TEMPLATE_CREATOR,
  ) = runBlocking { addProject(projectInfo, source) }

  /**
   * Removes a project from tracking.
   *
   * @param projectId The ID of the project to remove
   */
  suspend fun removeProject(projectId: String) {
    val projects = getTrackedProjects().toMutableList()
    projects.removeAll { it.projectId == projectId }
    saveProjects(projects)
  }

  /**
   * Removes a project synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun removeProjectBlocking(projectId: String) = runBlocking { removeProject(projectId) }

  /**
   * Updates an existing project's information.
   *
   * @param projectId The ID of the project to update
   * @param updatedInfo The updated project information
   */
  suspend fun updateProject(projectId: String, updatedInfo: ProjectInfo) {
    val projects = getTrackedProjects().toMutableList()
    val index = projects.indexOfFirst { it.projectId == projectId }

    if (index != -1) {
      projects[index] = updatedInfo
      saveProjects(projects)
    }
  }

  /**
   * Updates a project synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun updateProjectBlocking(projectId: String, updatedInfo: ProjectInfo) = runBlocking {
    updateProject(projectId, updatedInfo)
  }

  /**
   * Gets all tracked projects.
   *
   * @return List of all tracked projects
   */
  suspend fun getTrackedProjects(): List<ProjectInfo> {
    return context.dataStore.data
        .map { preferences ->
          val projectsJson = preferences[PROJECTS_KEY] ?: "[]"
          parseProjectsFromJson(projectsJson)
        }
        .first()
  }

  /**
   * Gets all tracked projects synchronously (blocks the current thread). Use the suspend version
   * when possible.
   */
  fun getTrackedProjectsBlocking(): List<ProjectInfo> = runBlocking { getTrackedProjects() }

  /**
   * Gets a specific project by its ID.
   *
   * @param projectId The ID of the project to retrieve
   * @return The project information, or null if not found
   */
  suspend fun getProjectById(projectId: String): ProjectInfo? {
    return getTrackedProjects().find { it.projectId == projectId }
  }

  /**
   * Gets a project by its ID synchronously (blocks the current thread). Use the suspend version
   * when possible.
   */
  fun getProjectByIdBlocking(projectId: String): ProjectInfo? = runBlocking {
    getProjectById(projectId)
  }

  /**
   * Gets a project by its directory path.
   *
   * @param projectDir The directory path of the project
   * @return The project information, or null if not found
   */
  suspend fun getProjectByDirectory(projectDir: String): ProjectInfo? {
    return getTrackedProjects().find { it.projectDir == projectDir }
  }

  /**
   * Gets a project by its directory path synchronously (blocks the current thread). Use the suspend
   * version when possible.
   */
  fun getProjectByDirectoryBlocking(projectDir: String): ProjectInfo? = runBlocking {
    getProjectByDirectory(projectDir)
  }

  /**
   * Gets all projects of a specific type.
   *
   * @param projectType The type of projects to retrieve
   * @return List of projects with the specified type
   */
  suspend fun getProjectsByType(projectType: String): List<ProjectInfo> {
    return getTrackedProjects().filter { it.projectType == projectType }
  }

  /**
   * Gets projects by type synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun getProjectsByTypeBlocking(projectType: String): List<ProjectInfo> = runBlocking {
    getProjectsByType(projectType)
  }

  /**
   * Gets all tracked projects as a Flow for reactive updates.
   *
   * @return Flow of tracked projects
   */
  fun getTrackedProjectsFlow(): Flow<List<ProjectInfo>> {
    return context.dataStore.data.map { preferences ->
      val projectsJson = preferences[PROJECTS_KEY] ?: "[]"
      parseProjectsFromJson(projectsJson)
    }
  }

  /**
   * Checks if a project is already tracked.
   *
   * @param projectDir The directory path of the project
   * @return true if the project is tracked, false otherwise
   */
  suspend fun isProjectTracked(projectDir: String): Boolean {
    return getTrackedProjects().any { it.projectDir == projectDir }
  }

  /**
   * Checks if a project is tracked synchronously (blocks the current thread). Use the suspend
   * version when possible.
   */
  fun isProjectTrackedBlocking(projectDir: String): Boolean = runBlocking {
    isProjectTracked(projectDir)
  }

  /**
   * Scans a directory for projects and automatically tracks external projects.
   *
   * @param directory The directory to scan for projects
   * @return List of newly tracked projects
   */
  suspend fun scanAndTrackExternalProjects(directory: File): List<ProjectInfo> {
    val newlyTracked = mutableListOf<ProjectInfo>()

    if (!directory.exists() || !directory.isDirectory) {
      return newlyTracked
    }

    // Look for Android projects (directories containing build.gradle or build.gradle.kts)
    directory.listFiles()?.forEach { subDir ->
      if (subDir.isDirectory) {
        val buildFile = File(subDir, "build.gradle")
        val buildKtsFile = File(subDir, "build.gradle.kts")

        if (buildFile.exists() || buildKtsFile.exists()) {
          // Check if this project is already tracked
          if (!isProjectTracked(subDir.absolutePath)) {
            val projectInfo = createExternalProjectInfo(subDir)
            addProject(projectInfo, ProjectSource.EXTERNAL)
            newlyTracked.add(projectInfo)
          }
        }
      }
    }

    return newlyTracked
  }

  /**
   * Scans and tracks external projects synchronously (blocks the current thread). Use the suspend
   * version when possible.
   */
  fun scanAndTrackExternalProjectsBlocking(directory: File): List<ProjectInfo> = runBlocking {
    scanAndTrackExternalProjects(directory)
  }

  /**
   * Creates a project info for a template created by Android Template Creator.
   *
   * @param projectName The name of the project
   * @param projectDir The directory of the project
   * @param projectType The type of the project
   * @return ProjectInfo for the template project
   */
  fun createTemplateProjectInfo(
      projectName: String,
      projectDir: String,
      projectType: String,
  ): ProjectInfo {
    return ProjectInfo(
        projectId = generateProjectId(),
        projectName = projectName,
        projectDir = projectDir,
        projectType = projectType,
    )
  }

  /**
   * Creates a project info for an external project.
   *
   * @param projectDir The directory of the project
   * @return ProjectInfo for the external project
   */
  private fun createExternalProjectInfo(projectDir: File): ProjectInfo {
    val projectName = sanitizeProjectName(projectDir.name)
    return ProjectInfo(
        projectId = generateProjectId(),
        projectName = projectName,
        projectDir = projectDir.absolutePath,
        projectType = "External",
    )
  }

  /**
   * Generates a unique project ID.
   *
   * @return A unique project ID
   */
  private fun generateProjectId(): String {
    return "project_${UUID.randomUUID().toString().replace("-", "").substring(0, 8)}"
  }

  /**
   * Sanitizes a project name to make it suitable for display.
   *
   * @param name The raw project name
   * @return Sanitized project name
   */
  private fun sanitizeProjectName(name: String): String {
    return name
        .replace("_", " ")
        .replace("-", " ")
        .split(" ")
        .joinToString(" ") { word ->
          word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        .trim()
  }

  /**
   * Saves the list of projects to DataStore.
   *
   * @param projects The list of projects to save
   */
  private suspend fun saveProjects(projects: List<ProjectInfo>) {
    context.dataStore.edit { preferences ->
      val projectsJson = convertProjectsToJson(projects)
      preferences[PROJECTS_KEY] = projectsJson
    }
  }

  /**
   * Converts a list of projects to JSON string.
   *
   * @param projects The list of projects
   * @return JSON string representation
   */
  private fun convertProjectsToJson(projects: List<ProjectInfo>): String {
    val jsonBuilder = StringBuilder()
    jsonBuilder.append("[")

    projects.forEachIndexed { index, project ->
      if (index > 0) jsonBuilder.append(",")
      jsonBuilder.append("{")
      jsonBuilder.append("\"projectId\":\"${project.projectId}\",")
      jsonBuilder.append("\"projectName\":\"${project.projectName}\",")
      jsonBuilder.append("\"projectDir\":\"${project.projectDir}\",")
      jsonBuilder.append("\"projectType\":\"${project.projectType}\"")
      jsonBuilder.append("}")
    }

    jsonBuilder.append("]")
    return jsonBuilder.toString()
  }

  /**
   * Parses projects from JSON string.
   *
   * @param json The JSON string
   * @return List of projects
   */
  private fun parseProjectsFromJson(json: String): List<ProjectInfo> {
    if (json == "[]" || json.isBlank()) {
      return emptyList()
    }

    try {
      val projects = mutableListOf<ProjectInfo>()
      val cleanJson = json.trim()

      if (cleanJson.startsWith("[") && cleanJson.endsWith("]")) {
        val content = cleanJson.substring(1, cleanJson.length - 1)
        if (content.isNotBlank()) {
          val projectStrings = content.split("},{")

          projectStrings.forEach { projectString ->
            val cleanString = projectString.replace("{", "").replace("}", "")
            val parts = cleanString.split(",")

            if (parts.size >= 4) {
              val projectId = extractValue(parts[0])
              val projectName = extractValue(parts[1])
              val projectDir = extractValue(parts[2])
              val projectType = extractValue(parts[3])

              if (
                  projectId.isNotBlank() &&
                      projectName.isNotBlank() &&
                      projectDir.isNotBlank() &&
                      projectType.isNotBlank()
              ) {
                projects.add(ProjectInfo(projectId, projectName, projectDir, projectType))
              }
            }
          }
        }
      }

      return projects
    } catch (e: Exception) {
      return emptyList()
    }
  }

  /**
   * Extracts value from JSON key-value pair.
   *
   * @param pair The key-value pair string
   * @return The extracted value
   */
  private fun extractValue(pair: String): String {
    val colonIndex = pair.indexOf(":")
    return if (colonIndex != -1) {
      pair.substring(colonIndex + 1).trim().replace("\"", "")
    } else {
      ""
    }
  }

  /** Clears all tracked projects. */
  suspend fun clearAllProjects() {
    context.dataStore.edit { preferences -> preferences.remove(PROJECTS_KEY) }
  }

  /**
   * Clears all tracked projects synchronously (blocks the current thread). Use the suspend version
   * when possible.
   */
  fun clearAllProjectsBlocking() = runBlocking { clearAllProjects() }

  /**
   * Gets the total number of tracked projects.
   *
   * @return The number of tracked projects
   */
  suspend fun getProjectCount(): Int {
    return getTrackedProjects().size
  }

  /**
   * Gets the project count synchronously (blocks the current thread). Use the suspend version when
   * possible.
   */
  fun getProjectCountBlocking(): Int = runBlocking { getProjectCount() }
}
