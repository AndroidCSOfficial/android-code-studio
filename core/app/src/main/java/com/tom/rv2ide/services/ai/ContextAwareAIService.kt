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

package com.tom.rv2ide.services.ai

import java.util.UUID
import org.slf4j.LoggerFactory

/**
 * AI service wrapper that maintains conversation context and project awareness without requiring
 * complete reinitialization.
 *
 * @Author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class ContextAwareAIService(
    private val underlyingService: AIService,
    private val serviceName: String,
) : AIService {

  companion object {
    private val log = LoggerFactory.getLogger(ContextAwareAIService::class.java)
  }

  private val conversationHistory = mutableListOf<ConversationEntry>()
  private val sessionId = UUID.randomUUID().toString()
  private var projectContextSet = false
  private var lastContextSummary: String? = null
  private var lastUserPrompt: String? = null
  private var samePromptCount = 0
  private val fileValidationCache = mutableMapOf<String, String>()

  data class ConversationEntry(
      val timestamp: Long,
      val userPrompt: String,
      val aiResponse: String,
      val contextUsed: String?,
  )

  /** Generate code with context awareness */
  override suspend fun generateCode(
      prompt: String,
      context: String?,
      language: String,
      projectStructure: String?,
  ): Result<String> {
    try {
      // Detect if user is repeating the same request (loop detection)
      if (prompt.trim() == lastUserPrompt?.trim()) {
        samePromptCount++
        log.warn("Detected repeated prompt (count: $samePromptCount): ${prompt.take(50)}...")

        if (samePromptCount >= 2) {
          log.warn("Loop detected! Forcing project context refresh")
          ProjectContextManager.getInstance().clearCache()
          hardReset() // Force complete reset to break the loop
        }
      } else {
        samePromptCount = 0
        lastUserPrompt = prompt.trim()
      }

      // Auto-clear conversation to prevent confusion
      smartReset()

      // Get or build context
      val AIContext = buildAIContext(prompt, context, projectStructure)

      log.debug("Generating code with context (session: ${sessionId.take(8)})")

      // Use underlying service with context
      val result =
          underlyingService.generateCode(
              prompt = AIContext.prompt,
              context = AIContext.context,
              language = language,
              projectStructure = AIContext.projectStructure,
          )

      result.onSuccess { response ->
        // Always clear conversation after successful response to prevent confusion
        conversationHistory.clear()
        log.debug("Conversation history cleared to prevent AI confusion")
      }

      return result
    } catch (e: Exception) {
      log.error("Error in context-aware code generation", e)
      return Result.failure(e)
    }
  }

  /** Build context that includes conversation history and project awareness */
  private fun buildAIContext(
      userPrompt: String,
      originalContext: String?,
      projectStructure: String?,
  ): AIContext {

    val contextManager = ProjectContextManager.getInstance()
    val projectContext = contextManager.getContextSummary()

    // Check if this is the first request or project context has changed
    if (!projectContextSet || projectContext != lastContextSummary) {
      setProjectContext(projectContext)
    }

    val AIPrompt = buildString {
      // Add project context reminder (simplified)
      if (projectContext != null) {
        append("=== PROJECT CONTEXT ===\n")
        append(projectContext)
        append("\n\n")
      }

      // Add current request directly (no conversation history to avoid confusion)
      append(userPrompt)

      // Add critical output format reminder
      append(
          "\n\nIMPORTANT: Provide clean, working code without explanatory text or markdown formatting unless specifically requested."
      )
      append("\nTreat this as a fresh request - focus only on the current task.")
    }

    val AIProjectStructure =
        if (projectStructure != null && projectContext != null) {
          buildString {
            append(projectContext)
            append("\n\n=== DETAILED STRUCTURE ===\n")
            append(projectStructure)
          }
        } else {
          projectStructure ?: projectContext
        }

    return AIContext(
        prompt = AIPrompt,
        context = originalContext,
        projectStructure = AIProjectStructure,
        contextSummary = projectContext,
    )
  }

  private data class AIContext(
      val prompt: String,
      val context: String?,
      val projectStructure: String?,
      val contextSummary: String?,
  )

  /** Set project context without reinitializing the service */
  private fun setProjectContext(projectContext: String?) {
    lastContextSummary = projectContext
    projectContextSet = true

    if (projectContext != null) {
      log.info(
          "Updated project context for AI service ($serviceName): ${projectContext.take(100)}..."
      )
      updateFileValidationCache()
    }
  }

  /** Update file validation cache with current project structure */
  private fun updateFileValidationCache() {
    fileValidationCache.clear()
    val contextManager = ProjectContextManager.getInstance()

    // Cache critical file paths
    val criticalFiles =
        listOf(
            "build.gradle",
            "build.gradle.kts",
            "AndroidManifest.xml",
            "MainActivity.kt",
            "MainActivity.java",
        )

    criticalFiles.forEach { fileName ->
      try {
        contextManager.getFilePathForName(fileName)?.let { path ->
          fileValidationCache[fileName] = path
        }
      } catch (e: Exception) {
        // Ignore if method doesn't exist in current implementation
        log.debug("File validation cache update skipped for $fileName")
      }
    }

    log.debug("File validation cache updated with ${fileValidationCache.size} entries")
  }

  /** Soft reset: Clear conversation history but maintain project context */
  fun softReset() {
    log.info(
        "Performing soft reset for AI service ($serviceName) - clearing ${conversationHistory.size} conversation entries"
    )
    conversationHistory.clear()
    // Note: We don't reset projectContextSet or lastContextSummary
    // This maintains project awareness while clearing conversation history
    log.info("Soft reset complete - conversation history cleared, project context preserved")
  }

  /** Hard reset: Clear everything and reinitialize (equivalent to current reset) */
  fun hardReset() {
    log.info("Performing hard reset for AI service ($serviceName)")
    conversationHistory.clear()
    projectContextSet = false
    lastContextSummary = null
    fileValidationCache.clear()
    ProjectContextManager.getInstance().clearCache()
    log.info("Hard reset complete - all context cleared")
  }

  /** Smart reset: Automatically clear conversation if it's getting too large or problematic */
  fun smartReset() {
    val shouldReset =
        conversationHistory.size > 2 ||
            conversationHistory.any { it.aiResponse.contains("error", ignoreCase = true) }

    if (shouldReset) {
      log.info("Smart reset triggered for AI service ($serviceName)")
      conversationHistory.clear()
    }
  }

  /** Check if service has project context */
  fun hasProjectContext(): Boolean = projectContextSet && lastContextSummary != null

  /** Get conversation summary for status display */
  fun getConversationSummary(): String {
    return buildString {
      append("Session: ${sessionId.take(8)}\n")
      append("Service: $serviceName\n")
      append("Project Context: ${if (projectContextSet) "Set" else "Not Set"}\n")
      append("Conversation Memory: Disabled (prevents confusion)\n")
      append("Mode: Fresh request mode (no conversation history)\n")

      if (lastContextSummary != null) {
        append("Project Summary: ${lastContextSummary!!.take(100)}...\n")
      }

      if (fileValidationCache.isNotEmpty()) {
        append("File Validation Cache: ${fileValidationCache.size} entries\n")
        append("Cached Files: ${fileValidationCache.keys.joinToString(", ")}\n")
      }
    }
  }

  /** Add context from external source (useful for maintaining context across sessions) */
  fun addExternalContext(userPrompt: String, aiResponse: String) {
    conversationHistory.add(
        ConversationEntry(
            timestamp = System.currentTimeMillis(),
            userPrompt = userPrompt,
            aiResponse = aiResponse,
            contextUsed = lastContextSummary,
        )
    )
  }

  /** Get file path validation for specific file */
  fun validateFilePath(fileName: String): String? {
    return fileValidationCache[fileName]
        ?: try {
          ProjectContextManager.getInstance().getFilePathForName(fileName)
        } catch (e: Exception) {
          log.debug("Could not validate file path for $fileName")
          null
        }
  }

  /** Check if service has project context */
  fun hasAIProjectContext(): Boolean = projectContextSet && lastContextSummary != null

  /** Get the underlying service for direct access if needed */
  fun getUnderlyingService(): AIService = underlyingService
}
