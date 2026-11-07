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
import com.tom.rv2ide.artificial.agents.AIAgent
import com.tom.rv2ide.artificial.agents.AIAgentRegistry
import com.tom.rv2ide.artificial.agents.Agents
import com.tom.rv2ide.artificial.agents.ModificationAttempt
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.profiles.AgentPromptProfileStore
import com.tom.rv2ide.artificial.profiles.CustomProviderProfileStore
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.rules.WritingRules
import com.tom.rv2ide.artificial.tools.ToolCallProtocol
import com.tom.rv2ide.artificial.tools.BuiltInToolExecutor
import com.tom.rv2ide.artificial.mcp.McpServerStore
import com.tom.rv2ide.artificial.mcp.McpToolExecutor
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
 * Custom OpenAI-compatible provider support with profiles, tools, and MCP.
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
  private var appContext: Context? = null
  override val providerId = "custom"
  override val providerName = "Custom Provider"

  companion object {
    fun registerAgent() {
      AIAgentRegistry.register(
          "custom",
          object : AIAgentRegistry.AgentFactory {
            override fun create(context: Context): AIAgent = CustomProvider()
            override fun hasValidApiKey(): Boolean {
              val p = CustomProviderProfileStore.getActive()
              return p != null && p.baseUrl.isNotBlank() && p.modelName.isNotBlank()
            }
            override fun getApiKey(): String? {
              val p = CustomProviderProfileStore.getActive() ?: return null
              return if (p.apiKey.isBlank()) "custom" else p.apiKey
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
    appContext = context.applicationContext
    agents = Agents(context)
    val profile = CustomProviderProfileStore.getActive()
    baseUrl = profile?.baseUrl
    modelName = profile?.modelName
    this.apiKey = profile?.apiKey
    if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) {
      throw IllegalStateException("Custom provider not configured. Please add/select a custom provider profile.")
    }
  }

  override fun isInitialized(): Boolean = !baseUrl.isNullOrEmpty() && !modelName.isNullOrEmpty()

  override fun reinitializeWithNewModel(apiKey: String, context: Context) { initialize(apiKey, context) }
  override fun setContext(context: Context) { appContext = context.applicationContext; fileWriter = AIFileWriter(context) }
  override fun setProjectData(projectTreeResult: ProjectTreeResult) { this.projectTreeResult = projectTreeResult }
  override fun clearConversation() { conversationHistory.clear(); modificationHistory.clear(); currentAttemptCount = 0 }

  override fun recordModification(filePath: String, oldContent: String?, newContent: String, success: Boolean) {
    modificationHistory.add(ModificationAttempt(System.currentTimeMillis(), filePath, oldContent, newContent, currentAttemptCount, success))
  }

  override fun undoLastModification(): Boolean {
    if (modificationHistory.isEmpty()) return false
    val lastMod = modificationHistory.lastOrNull { it.success } ?: return false
    if (lastMod.previousContent != null) {
      val result = writeFile(lastMod.filePath, lastMod.previousContent)
      if (result is FileWriteResult.Success) { modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod)); return true }
    } else {
      try { File(lastMod.filePath).delete(); modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod)); return true } catch (_: Exception) {}
    }
    return false
  }

  override fun getModificationHistory(): List<ModificationAttempt> = modificationHistory.toList()
  override fun resetAttemptCount() { currentAttemptCount = 0 }
  override fun incrementAttemptCount() { currentAttemptCount++ }
  override fun getCurrentAttemptCount(): Int = currentAttemptCount
  override fun canRetry(): Boolean = currentAttemptCount < maxRetryAttempts

  private fun isUserRequestingCorrection(message: String): Boolean {
    val correctionKeywords = listOf("wrong","not what","mistake","error","incorrect","that's not","not right","fix","undo","revert","different","try again","not working")
    return correctionKeywords.any { message.lowercase().contains(it) }
  }

  private fun composeSystemPrompt(context: Context): String {
    val base = writingRules.useThis()
    val agentProfile = AgentPromptProfileStore.getActive()
    val sb = StringBuilder(base)
    val extra = agentProfile?.systemPrompt?.trim().orEmpty()
    if (extra.isNotEmpty()) sb.append("\n\n=== AGENT PROFILE INSTRUCTIONS ===\n").append(extra).append("\n")
    if (agentProfile?.enableTools != false) {
      val builtIn = BuiltInToolExecutor(context, projectTreeResult, null, agentProfile?.allowCommandExecution == true)
      val mcpDefs = if (agentProfile?.enableMcpTools != false) {
        try {
          val servers = McpServerStore.load().filter { s ->
            agentProfile?.enabledMcpServerIds.isNullOrEmpty() || agentProfile!!.enabledMcpServerIds.contains(s.id)
          }
          McpToolExecutor(servers).definitions()
        } catch (_: Exception) { emptyList() }
      } else emptyList()
      sb.append(ToolCallProtocol.formatDefinitions(builtIn.definitions() + mcpDefs))
    }
    return sb.toString()
  }

  override suspend fun generateCode(
      prompt: String,
      context: String?,
      language: String,
      projectStructure: String?,
  ): Result<String> = withContext(Dispatchers.IO) {
    try {
      if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) return@withContext Result.failure(IllegalStateException("Custom provider not configured"))
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      val fileContents = readRelevantFiles()
      val needsCorrection = isUserRequestingCorrection(prompt)
      val fullPrompt = buildString {
        append("=== PROJECT STRUCTURE (THESE ARE THE EXACT PATHS YOU MUST USE) ===\n")
        if (projectTreeResult != null) {
          append(projectTreeResult!!.tree)
          append("\n\nCRITICAL: Use ONLY the paths shown above. Do NOT make up fake paths.\nCRITICAL: Look at the actual paths above and use those EXACT paths.\n\n")
        }
        if (fileContents.isNotEmpty()) {
          append("=== CURRENT FILES CONTENT ===\n")
          fileContents.forEach { (path, content) -> append("FILE: $path\nCONTENT:\n").append(content).append("\n\n") }
        }
        if (context != null) append("=== ADDITIONAL CONTEXT ===\n").append(context).append("\n\n")
        if (conversationHistory.isNotEmpty()) {
          append("=== CONVERSATION HISTORY ===\n")
          conversationHistory.takeLast(12).forEach { msg -> append("${msg.role.uppercase()}: ${msg.content.take(4000)}\n\n") }
        }
        if (needsCorrection && modificationHistory.isNotEmpty()) {
          append("=== CORRECTION REQUIRED ===\nPrevious failed attempts:\n")
          modificationHistory.takeLast(3).forEach { attempt -> append("Attempt ${attempt.attemptNumber}: ${attempt.filePath} - ${if (attempt.success) "Applied but user rejected" else "Failed"}\n") }
          append("Provide a COMPLETELY DIFFERENT approach.\n\n")
        }
        if (currentAttemptCount > 0) append("=== RETRY ATTEMPT $currentAttemptCount/$maxRetryAttempts ===\nTry a different solution.\n\n")
        append("=== USER REQUEST ===\n").append(prompt)
      }

      val messages = JSONArray()
      messages.put(JSONObject().apply { put("role", "system"); put("content", composeSystemPrompt(ctx)) })
      messages.put(JSONObject().apply { put("role", "user"); put("content", fullPrompt) })

      val requestBody = JSONObject().apply { put("model", modelName); put("messages", messages); put("temperature", 0.7); put("stream", false) }
      val endpoint = buildEndpoint(baseUrl!!)
      val requestBuilder = Request.Builder().url(endpoint).post(requestBody.toString().toRequestBody("application/json".toMediaType())).addHeader("Content-Type", "application/json")
      val key = apiKey?.trim()
      if (!key.isNullOrEmpty()) requestBuilder.addHeader("Authorization", "Bearer $key")

      val response = try { httpClient.newCall(requestBuilder.build()).execute() } catch (e: Exception) {
        val errorMessage = e.message ?: ""
        if (errorMessage.contains("timeout") || errorMessage.contains("connect")) throw com.tom.rv2ide.artificial.exceptions.RateLimitException("Connection timeout. Please check your custom provider endpoint.")
        throw e
      }
      if (!response.isSuccessful) {
        val errorBody = response.body?.string() ?: "Unknown error"
        return@withContext Result.failure(Exception("Custom provider error ${response.code}: $errorBody"))
      }
      val responseBody = response.body?.string() ?: ""
      val jsonResponse = JSONObject(responseBody)
      val generatedResponse = jsonResponse.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
      if (generatedResponse.isBlank()) return@withContext Result.failure(Exception("Empty response from custom provider"))

      conversationHistory.add(ConversationMessage("user", prompt))
      conversationHistory.add(ConversationMessage("assistant", generatedResponse))
      while (conversationHistory.size > 20) { conversationHistory.removeAt(0) }
      Result.success(generatedResponse)
    } catch (e: com.tom.rv2ide.artificial.exceptions.RateLimitException) { Result.failure(e)
    } catch (e: com.tom.rv2ide.artificial.exceptions.QuotaExceededException) { Result.failure(e)
    } catch (e: com.tom.rv2ide.artificial.exceptions.InvalidApiKeyException) { Result.failure(e)
    } catch (e: Exception) { Result.failure(e) }
  }

  private fun readRelevantFiles(): Map<String, String> {
    val filesContent = mutableMapOf<String, String>()
    val tree = projectTreeResult?.tree ?: return filesContent
    tree.lines().filter { it.isNotBlank() }.forEach { filePath ->
      val trimmedPath = filePath.trim()
      val file = File(trimmedPath)
      if (file.isFile && (trimmedPath.endsWith(".kt") || trimmedPath.endsWith(".java") || trimmedPath.endsWith(".xml") || trimmedPath.endsWith(".gradle") || trimmedPath.endsWith(".gradle.kts")) && !trimmedPath.contains("/build/") && !trimmedPath.contains("/.gradle/")) {
        try { filesContent[trimmedPath] = file.readText() } catch (_: Exception) {}
      }
    }
    return filesContent
  }

  override fun writeFile(filePath: String, content: String): FileWriteResult {
    val writer = fileWriter ?: return FileWriteResult.Error("File writer not initialized")
    return writer.writeFile(filePath, content, createBackup = true)
  }
}

data class ConversationMessage(val role: String, val content: String)
