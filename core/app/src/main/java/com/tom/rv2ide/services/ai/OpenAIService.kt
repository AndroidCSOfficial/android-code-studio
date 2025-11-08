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

import com.tom.rv2ide.services.ai.Instructions.system_instructions
import com.tom.rv2ide.services.ai.preferences.getAgentName
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.slf4j.LoggerFactory

/**
 * Service for interacting with OpenAI GPT API with full project context.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class OpenAIService : AIService {

  companion object {
    private val log = LoggerFactory.getLogger(OpenAIService::class.java)
    private const val BASE_URL = "https://api.openai.com/v1/chat/completions"
    private const val DEFAULT_MODEL_NAME = "gpt-4o"
    private const val TIMEOUT_SECONDS = 60L

    // Fixed MODEL_NAME property with proper null handling
    private val MODEL_NAME: String
      get() {
        val agentName = getAgentName()
        return if (
            !agentName.isNullOrEmpty() &&
                (agentName.startsWith("gpt", ignoreCase = true) ||
                    agentName.startsWith("o1", ignoreCase = true))
        ) {
          agentName
        } else {
          DEFAULT_MODEL_NAME
        }
      }
  }

  private var apiKey: String? = null
  private val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
  }

  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
  }

  private val systemInstruction = system_instructions

  /** Initialize the service with API key. */
  fun initialize(apiKey: String) {
    try {
      this.apiKey = apiKey
      log.info("OpenAI service initialized successfully with project awareness")
    } catch (e: Exception) {
      log.error("Failed to initialize OpenAI service", e)
      throw e
    }
  }

  /** Generate code based on user prompt with full project context awareness. */
  override suspend fun generateCode(
      prompt: String,
      context: String?,
      language: String,
      projectStructure: String?,
  ): Result<String> =
      withContext(Dispatchers.IO) {
        try {
          if (apiKey == null) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI service not initialized")
            )
          }

          val requestBody =
              OpenAIChatRequest(
                  model = MODEL_NAME,
                  messages =
                      listOf(
                          ChatMessage(role = "system", content = systemInstruction),
                          ChatMessage(role = "user", content = prompt),
                      ),
                  temperature = 0.1,
                  maxTokens = 8000,
              )

          val response = makeApiCall(requestBody)
          if (response.isFailure) {
            return@withContext Result.failure(response.exceptionOrNull()!!)
          }

          val chatResponse =
              response.getOrNull()
                  ?: return@withContext Result.failure(Exception("Empty response from OpenAI"))

          val generatedResponse = chatResponse.choices.firstOrNull()?.message?.content ?: ""
          if (generatedResponse.isBlank()) {
            return@withContext Result.failure(Exception("Empty content in response from AI"))
          }

          log.info(
              "Successfully generated project-aware response for prompt: ${prompt.take(100)}..."
          )
          Result.success(generatedResponse)
        } catch (e: Exception) {
          log.error("Failed to generate code", e)
          Result.failure(e)
        }
      }

  /** Generate code for specific Android development tasks with context. */
  suspend fun generateAndroidCode(
      task: AndroidTask,
      projectContext: ProjectContext,
  ): Result<AndroidCodeResponse> =
      withContext(Dispatchers.IO) {
        try {
          if (apiKey == null) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI service not initialized")
            )
          }

          val aIPrompt = buildAndroidTaskPrompt(task, projectContext)

          val requestBody =
              OpenAIChatRequest(
                  model = MODEL_NAME,
                  messages =
                      listOf(
                          ChatMessage(role = "system", content = systemInstruction),
                          ChatMessage(role = "user", content = aIPrompt),
                      ),
                  temperature = 0.1,
                  maxTokens = 4000,
              )

          val response = makeApiCall(requestBody)
          if (response.isFailure) {
            return@withContext Result.failure(response.exceptionOrNull()!!)
          }

          val chatResponse =
              response.getOrNull()
                  ?: return@withContext Result.failure(Exception("Empty response from OpenAI"))

          val generatedResponse = chatResponse.choices.firstOrNull()?.message?.content ?: ""
          if (generatedResponse.isBlank()) {
            return@withContext Result.failure(Exception("Empty content in response from AI"))
          }

          val androidResponse = parseAndroidResponse(generatedResponse)
          log.info("Successfully generated Android code for task: ${task.type}")
          Result.success(androidResponse)
        } catch (e: Exception) {
          log.error("Failed to generate Android code", e)
          Result.failure(e)
        }
      }

  /** Analyze project structure and suggest improvements. */
  suspend fun analyzeProject(projectStructure: String): Result<ProjectAnalysis> =
      withContext(Dispatchers.IO) {
        try {
          if (apiKey == null) {
            return@withContext Result.failure(
                IllegalStateException("OpenAI service not initialized")
            )
          }

          val analysisPrompt = buildString {
            append(
                "Analyze this Android project structure and provide suggestions for improvement:\n\n"
            )
            append("PROJECT STRUCTURE:\n")
            append(projectStructure)
            append("\n\nPlease provide:\n")
            append("1. Code quality assessment\n")
            append("2. Architecture suggestions\n")
            append("3. Performance optimization opportunities\n")
            append("4. Security considerations\n")
            append("5. Best practices compliance\n")
            append("\nProvide response in structured format with clear sections.")
          }

          val requestBody =
              OpenAIChatRequest(
                  model = MODEL_NAME,
                  messages =
                      listOf(
                          ChatMessage(role = "system", content = systemInstruction),
                          ChatMessage(role = "user", content = analysisPrompt),
                      ),
                  temperature = 0.1,
                  maxTokens = 4000,
              )

          val response = makeApiCall(requestBody)
          if (response.isFailure) {
            return@withContext Result.failure(response.exceptionOrNull()!!)
          }

          val chatResponse =
              response.getOrNull()
                  ?: return@withContext Result.failure(Exception("Empty response from OpenAI"))

          val analysis = chatResponse.choices.firstOrNull()?.message?.content ?: ""
          if (analysis.isBlank()) {
            return@withContext Result.failure(Exception("Empty analysis from AI"))
          }

          Result.success(ProjectAnalysis(analysis))
        } catch (e: Exception) {
          log.error("Failed to analyze project", e)
          Result.failure(e)
        }
      }

  private suspend fun makeApiCall(requestBody: OpenAIChatRequest): Result<OpenAIChatResponse> {
    return try {
      val jsonBody = json.encodeToString(OpenAIChatRequest.serializer(), requestBody)

      val request =
          Request.Builder()
              .url(BASE_URL)
              .addHeader("Authorization", "Bearer $apiKey")
              .addHeader("Content-Type", "application/json")
              .post(jsonBody.toRequestBody("application/json".toMediaType()))
              .build()

      val response = httpClient.newCall(request).execute()

      if (!response.isSuccessful) {
        val errorBody = response.body?.string() ?: "Unknown error"
        log.error("OpenAI API error: ${response.code} - $errorBody")
        return Result.failure(Exception("API call failed with code ${response.code}: $errorBody"))
      }

      val responseBody =
          response.body?.string() ?: return Result.failure(Exception("Empty response body"))

      val chatResponse = json.decodeFromString(OpenAIChatResponse.serializer(), responseBody)
      Result.success(chatResponse)
    } catch (e: Exception) {
      log.error("Error making API call to OpenAI", e)
      Result.failure(e)
    }
  }

  private fun buildAndroidTaskPrompt(task: AndroidTask, context: ProjectContext): String {
    return buildString {
      append("You are working on an Android project. Here's the context:\n\n")
      append("=== PROJECT CONTEXT ===\n")
      append("Modules: ${context.modules.joinToString(", ")}\n")
      append("Current Module: ${context.currentModule}\n")
      append("Package Name: ${context.packageName}\n")
      append("Target SDK: ${context.targetSdk}\n\n")

      append("=== AVAILABLE RESOURCES ===\n")
      context.resourceFiles.forEach { (type, files) ->
        append("$type: ${files.joinToString(", ")}\n")
      }
      append("\n")

      append("=== TASK ===\n")
      append("Type: ${task.type}\n")
      append("Description: ${task.description}\n")
      append("Target Files: ${task.targetFiles.joinToString(", ")}\n\n")

      append("=== INSTRUCTIONS ===\n")
      append("1. Generate complete, working Android code\n")
      append("2. Follow Android best practices and conventions\n")
      append("3. Use appropriate resource files (strings.xml, colors.xml, etc.)\n")
      append("4. Maintain proper project structure\n")
      append("5. Include proper imports and package declarations\n")
      append("6. Add appropriate comments for complex code\n\n")

      append("=== RESPONSE FORMAT ===\n")
      append("Provide your response in the following format:\n")
      append("FILE_MODIFICATIONS:\n")
      append("==== FILE: path/to/file.ext ====\n")
      append("[complete file content]\n")
      append("==== END_FILE ====\n")
      append("(repeat for each file)\n\n")

      append("Please generate the necessary code modifications.")
    }
  }

  private fun parseAndroidResponse(response: String): AndroidCodeResponse {
    val fileModifications = mutableMapOf<String, String>()
    val suggestions = mutableListOf<String>()

    // Parse file modifications
    val lines = response.lines()
    var currentFile: String? = null
    val currentContent = StringBuilder()
    var inFileContent = false
    var inSuggestions = false

    for (line in lines) {
      when {
        line.startsWith("==== FILE:") -> {
          currentFile?.let { file -> fileModifications[file] = currentContent.toString().trim() }
          currentFile = line.substringAfter("==== FILE:").substringBefore("====").trim()
          currentContent.clear()
          inFileContent = true
          inSuggestions = false
        }
        line.startsWith("==== END_FILE") -> {
          inFileContent = false
        }
        line.startsWith("=== SUGGESTIONS") || line.startsWith("SUGGESTIONS:") -> {
          inSuggestions = true
          inFileContent = false
        }
        inFileContent && currentFile != null -> {
          currentContent.append(line).append("\n")
        }
        inSuggestions && line.trim().isNotEmpty() -> {
          suggestions.add(line.trim())
        }
      }
    }

    // Save last file
    currentFile?.let { file -> fileModifications[file] = currentContent.toString().trim() }

    return AndroidCodeResponse(fileModifications, suggestions, response)
  }

  /** Check if the service is properly initialized. */
  fun isInitialized(): Boolean = apiKey != null

  /** Data classes for OpenAI API communication */
  @Serializable
  data class OpenAIChatRequest(
      val model: String,
      val messages: List<ChatMessage>,
      val temperature: Double = 0.1,
      @SerialName("max_tokens") val maxTokens: Int = 4000,
      val stream: Boolean = false,
  )

  @Serializable data class ChatMessage(val role: String, val content: String)

  @Serializable
  data class OpenAIChatResponse(
      val id: String,
      val `object`: String,
      val created: Long,
      val model: String,
      val choices: List<Choice>,
      val usage: Usage? = null,
  )

  @Serializable
  data class Choice(
      val index: Int,
      val message: ChatMessage,
      @SerialName("finish_reason") val finishReason: String? = null,
  )

  @Serializable
  data class Usage(
      @SerialName("prompt_tokens") val promptTokens: Int,
      @SerialName("completion_tokens") val completionTokens: Int,
      @SerialName("total_tokens") val totalTokens: Int,
  )

  /** Data classes for Android development support */
  data class AndroidTask(
      val type: TaskType,
      val description: String,
      val targetFiles: List<String> = emptyList(),
      val parameters: Map<String, Any> = emptyMap(),
  )

  enum class TaskType {
    ADD_TOAST,
    CREATE_ACTIVITY,
    ADD_FRAGMENT,
    MODIFY_LAYOUT,
    ADD_STRING_RESOURCE,
    ADD_COLOR_RESOURCE,
    IMPLEMENT_ONCLICK,
    ADD_PERMISSION,
    CREATE_SERVICE,
    ADD_BROADCAST_RECEIVER,
    CUSTOM_REQUEST,
  }

  data class ProjectContext(
      val modules: List<String>,
      val currentModule: String?,
      val packageName: String?,
      val targetSdk: Int?,
      val resourceFiles: Map<String, List<String>>,
  )

  data class AndroidCodeResponse(
      val fileModifications: Map<String, String>,
      val suggestions: List<String>,
      val rawResponse: String,
  )

  data class ProjectAnalysis(val analysis: String)
}
