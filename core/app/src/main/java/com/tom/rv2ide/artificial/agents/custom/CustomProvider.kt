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

package com.tom.rv2ide.artificial.agents.custom

import android.content.Context
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.artificial.agents.AIAgent
import com.tom.rv2ide.artificial.agents.AIAgentRegistry
import com.tom.rv2ide.artificial.agents.Agents
import com.tom.rv2ide.artificial.agents.ModificationAttempt
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.rules.WritingRules
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/*
 * Custom OpenAI-compatible provider support.
 * Configure base URL, model name, and optional API key in AI settings.
 */
class CustomProvider : AIAgent {

  private var baseUrl: String? = null
  private var modelName: String? = null
  private var apiKey: String? = null
  private val httpClient =
      OkHttpClient.Builder()
          .connectTimeout(60, TimeUnit.SECONDS)
          .readTimeout(120, TimeUnit.SECONDS)
          .writeTimeout(60, TimeUnit.SECONDS)
          .build()
  private val writingRules = WritingRules.Instructions()
  private var projectTreeResult: ProjectTreeResult? = null
  private var fileWriter: AIFileWriter? = null
  private val conversationHistory = mutableListOf<ConversationMessage>()
  private val modificationHistory = mutableListOf<ModificationAttempt>()
  private var currentAttemptCount = 0
  private val maxRetryAttempts = 3
  private var agents: Agents? = null
  override val providerId = "custom"
  override val providerName = "Custom Provider"

  companion object {
    private const val PREF_BASE_URL = "ai_agent_custom_base_url"
    private const val PREF_MODEL_NAME = "ai_agent_custom_model_name"
    private const val PREF_API_KEY = "ai_agent_custom_api_key"

    fun registerAgent() {
      AIAgentRegistry.register(
          "custom",
          object : AIAgentRegistry.AgentFactory {
            override fun create(context: Context): AIAgent {
              return CustomProvider()
            }

            override fun hasValidApiKey(): Boolean {
              val prefs = BaseApplication.getBaseInstance().prefManager
              val url = prefs.getString(PREF_BASE_URL, null)
              val model = prefs.getString(PREF_MODEL_NAME, null)
              android.util.Log.d("CustomProvider", "hasValidApiKey check: url=$url, model=$model")
              return !url.isNullOrEmpty() && !model.isNullOrEmpty()
            }

            override fun getApiKey(): String? {
              val prefs = BaseApplication.getBaseInstance().prefManager
              val key = prefs.getString(PREF_API_KEY, null)
              return if (key.isNullOrBlank()) "custom" else key
            }
          })
    }

    private fun buildEndpoint(baseUrl: String): String {
      val trimmed = baseUrl.trim().trimEnd('/')
      return when {
        trimmed.contains("/chat/completions") -> trimmed
        trimmed.endsWith("/v1") -> "$trimmed/chat/completions"
        else -> "$trimmed/v1/chat/completions"
      }
    }
  }

  override fun initialize(apiKey: String, context: Context) {
    try {
      agents = Agents(context)
      val prefs = BaseApplication.getBaseInstance().prefManager
      baseUrl = prefs.getString(PREF_BASE_URL, null)
      modelName = prefs.getString(PREF_MODEL_NAME, null)
      this.apiKey = prefs.getString(PREF_API_KEY, null)

      android.util.Log.d(
          "CustomProvider", "Initialized with baseUrl=$baseUrl, model=$modelName, hasKey=${!this.apiKey.isNullOrBlank()}")

      if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) {
        throw IllegalStateException(
            "Custom provider not configured. Please set base URL and model name in AI settings.")
      }
    } catch (e: Exception) {
      throw e
    }
  }

  override fun isInitialized(): Boolean =
      !baseUrl.isNullOrEmpty() && !modelName.isNullOrEmpty()

  override fun reinitializeWithNewModel(apiKey: String, context: Context) {
    initialize(apiKey, context)
  }

  override fun setContext(context: Context) {
    fileWriter = AIFileWriter(context)
  }

  override fun setProjectData(projectTreeResult: ProjectTreeResult) {
    this.projectTreeResult = projectTreeResult
  }

  override fun clearConversation() {
    conversationHistory.clear()
    modificationHistory.clear()
    currentAttemptCount = 0
  }

  override fun recordModification(
      filePath: String,
      oldContent: String?,
      newContent: String,
      success: Boolean
  ) {
    modificationHistory.add(
        ModificationAttempt(
            timestamp = System.currentTimeMillis(),
            filePath = filePath,
            previousContent = oldContent,
            newContent = newContent,
            attemptNumber = currentAttemptCount,
            success = success))
  }

  override fun undoLastModification(): Boolean {
    if (modificationHistory.isEmpty()) return false

    val lastMod = modificationHistory.lastOrNull { it.success } ?: return false

    if (lastMod.previousContent != null) {
      val result = writeFile(lastMod.filePath, lastMod.previousContent)
      if (result is FileWriteResult.Success) {
        modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod))
        return true
      }
    } else {
      try {
        File(lastMod.filePath).delete()
        modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod))
        return true
      } catch (e: Exception) {
        return false
      }
    }
    return false
  }

  override fun getModificationHistory(): List<ModificationAttempt> {
    return modificationHistory.toList()
  }

  override fun resetAttemptCount() {
    currentAttemptCount = 0
  }

  override fun incrementAttemptCount() {
    currentAttemptCount++
  }

  override fun getCurrentAttemptCount(): Int = currentAttemptCount

  override fun canRetry(): Boolean = currentAttemptCount < maxRetryAttempts

  private fun isUserRequestingCorrection(message: String): Boolean {
    val correctionKeywords =
        listOf(
            "wrong",
            "not what",
            "mistake",
            "error",
            "incorrect",
            "that's not",
            "not right",
            "fix",
            "undo",
            "revert",
            "different",
            "try again",
            "not working")
    return correctionKeywords.any { message.lowercase().contains(it) }
  }

  override suspend fun generateCode(
      prompt: String,
      context: String?,
      language: String,
      projectStructure: String?,
  ): Result<String> =
      withContext(Dispatchers.IO) {
        try {
          if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) {
            return@withContext Result.failure(IllegalStateException("Custom provider not configured"))
          }

          val fileContents = readRelevantFiles()
          val needsCorrection = isUserRequestingCorrection(prompt)

          val fullPrompt = buildString {
            append("=== PROJECT STRUCTURE (THESE ARE THE EXACT PATHS YOU MUST USE) ===\n")
            if (projectTreeResult != null) {
              append(projectTreeResult!!.tree)
              append("\n\n")
              append(
                  "CRITICAL: Use ONLY the paths shown above. Do NOT make up fake paths like '/storage/emulated/0/project' or 'com.example.yourproject'.\n")
              append("CRITICAL: Look at the actual paths above and use those EXACT paths.\n\n")
            }

            if (fileContents.isNotEmpty()) {
              append("=== CURRENT FILES CONTENT ===\n")
              fileContents.forEach { (path, content) ->
                append("FILE: $path\n")
                append("CONTENT:\n")
                append(content)
                append("\n\n")
              }
            }

            if (context != null) {
              append("=== ADDITIONAL CONTEXT ===\n")
              append(context)
              append("\n\n")
            }

            if (conversationHistory.isNotEmpty()) {
              append("=== CONVERSATION HISTORY ===\n")
              conversationHistory.forEach { msg -> append("${msg.role.uppercase()}: ${msg.content}\n\n") }
            }

            if (needsCorrection && modificationHistory.isNotEmpty()) {
              append("=== CORRECTION REQUIRED ===\n")
              append("The user indicated the previous modification was WRONG.\n")
              append("Previous failed attempts:\n")
              modificationHistory.takeLast(3).forEach { attempt ->
                append("Attempt ${attempt.attemptNumber}: ${attempt.filePath}\n")
                append("Result: ${if (attempt.success) "Applied but user rejected" else "Failed"}\n\n")
              }
              append("You MUST try a DIFFERENT approach. Do NOT repeat the same solution.\n")
              append("Analyze what went wrong and provide a better solution.\n\n")
            }

            if (currentAttemptCount > 0) {
              append("=== RETRY ATTEMPT $currentAttemptCount/$maxRetryAttempts ===\n")
              append("This is retry attempt number $currentAttemptCount.\n")
              append("Previous attempts did not satisfy the user.\n")
              append("Think carefully and provide a different solution.\n\n")
            }

            append("=== USER REQUEST ===\n")
            append(prompt)
          }

          val messages = JSONArray()
          messages.put(
              JSONObject().apply {
                put("role", "system")
                put("content", writingRules.useThis())
              })
          messages.put(
              JSONObject().apply {
                put("role", "user")
                put("content", fullPrompt)
              })

          val requestBody =
              JSONObject().apply {
                put("model", modelName)
                put("messages", messages)
                put("temperature", 0.7)
                put("stream", false)
              }

          val endpoint = buildEndpoint(baseUrl!!)
          val requestBuilder =
              Request.Builder()
                  .url(endpoint)
                  .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                  .addHeader("Content-Type", "application/json")

          val key = apiKey?.trim()
          if (!key.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $key")
          }

          val response =
              try {
                httpClient.newCall(requestBuilder.build()).execute()
              } catch (e: Exception) {
                val errorMessage = e.message ?: ""
                when {
                  errorMessage.contains("timeout") || errorMessage.contains("connect") ->
                      throw com.tom.rv2ide.artificial.exceptions.RateLimitException(
                          "Connection timeout. Please check your custom provider endpoint.")
                  else -> throw e
                }
              }

          if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "Unknown error"
            return@withContext Result.failure(
                Exception("Custom provider error ${response.code}: $errorBody"))
          }

          val responseBody = response.body?.string() ?: ""
          val jsonResponse = JSONObject(responseBody)

          val generatedResponse =
              jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")

          if (generatedResponse.isBlank()) {
            return@withContext Result.failure(Exception("Empty response from custom provider"))
          }

          conversationHistory.add(ConversationMessage("user", prompt))
          conversationHistory.add(ConversationMessage("assistant", generatedResponse))

          if (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
            conversationHistory.removeAt(0)
          }

          Result.success(generatedResponse)
        } catch (e: com.tom.rv2ide.artificial.exceptions.RateLimitException) {
          Result.failure(e)
        } catch (e: com.tom.rv2ide.artificial.exceptions.QuotaExceededException) {
          Result.failure(e)
        } catch (e: com.tom.rv2ide.artificial.exceptions.InvalidApiKeyException) {
          Result.failure(e)
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  private fun readRelevantFiles(): Map<String, String> {
    val filesContent = mutableMapOf<String, String>()
    val tree = projectTreeResult?.tree ?: return filesContent

    val filePaths = tree.lines().filter { it.isNotBlank() }

    filePaths.forEach { filePath ->
      val trimmedPath = filePath.trim()
      val file = File(trimmedPath)

      if (file.isFile &&
          (trimmedPath.endsWith(".kt") ||
              trimmedPath.endsWith(".java") ||
              trimmedPath.endsWith(".xml") ||
              trimmedPath.endsWith(".gradle") ||
              trimmedPath.endsWith(".gradle.kts")) &&
          !trimmedPath.contains("/build/") &&
          !trimmedPath.contains("/.gradle/")) {
        try {
          val content = file.readText()
          filesContent[trimmedPath] = content
        } catch (e: Exception) {}
      }
    }

    return filesContent
  }

  override fun writeFile(filePath: String, content: String): FileWriteResult {
    val writer = fileWriter ?: return FileWriteResult.Error("File writer not initialized")
    return writer.writeFile(filePath, content, createBackup = true)
  }

  fun parseAndApplyModifications(
      response: String,
      capturedStates: Map<String, String>
  ): List<FileModification> {
    val modifications = mutableListOf<FileModification>()
    val parser = com.tom.rv2ide.artificial.parser.SnippetParser()

    if (response.contains("FILE_TO_MODIFY:")) {
      val lines = response.lines()
      var currentFile: String? = null
      val contentBuilder = StringBuilder()
      var inContent = false

      for (line in lines) {
        if (line.startsWith("FILE_TO_MODIFY:")) {
          if (currentFile != null && contentBuilder.isNotEmpty()) {
            val rawContent = contentBuilder.toString().trim()
            val cleanedContent = parser.cleanFileContent(rawContent)
            val previousContent = capturedStates[currentFile]
            val writeResult = writeFile(currentFile, cleanedContent)

            val success = writeResult is FileWriteResult.Success
            recordModification(currentFile, previousContent, cleanedContent, success)

            modifications.add(FileModification(currentFile, cleanedContent, writeResult))
          }

          currentFile = line.substringAfter("FILE_TO_MODIFY:").trim()
          contentBuilder.clear()
          inContent = true
        } else if (inContent) {
          contentBuilder.append(line).append("\n")
        }
      }

      if (currentFile != null && contentBuilder.isNotEmpty()) {
        val rawContent = contentBuilder.toString().trim()
        val cleanedContent = parser.cleanFileContent(rawContent)
        val previousContent = capturedStates[currentFile]
        val writeResult = writeFile(currentFile, cleanedContent)

        val success = writeResult is FileWriteResult.Success
        recordModification(currentFile, previousContent, cleanedContent, success)

        modifications.add(FileModification(currentFile, cleanedContent, writeResult))
      }
    }

    return modifications
  }
}

data class ConversationMessage(val role: String, val content: String)

data class FileModification(
    val filePath: String,
    val content: String,
    val writeResult: FileWriteResult
)
