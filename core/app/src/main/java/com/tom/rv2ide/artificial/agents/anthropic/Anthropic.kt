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

package com.tom.rv2ide.artificial.agents.anthropic

import android.content.Context
import com.tom.rv2ide.artificial.agents.AIAgent
import com.tom.rv2ide.artificial.agents.AIAgentRegistry
import com.tom.rv2ide.artificial.agents.ModificationAttempt
import com.tom.rv2ide.artificial.tools.AIToolCall
import com.tom.rv2ide.artificial.tools.AIToolDef
import com.tom.rv2ide.artificial.tools.AIToolResult
import com.tom.rv2ide.artificial.tools.ToolAwareResponse
import com.tom.rv2ide.artificial.tools.ToolCallProtocol
import com.tom.rv2ide.artificial.tools.BuiltInToolExecutor
import com.tom.rv2ide.artificial.profiles.AgentPromptProfileStore
import com.tom.rv2ide.artificial.mcp.McpServerStore
import com.tom.rv2ide.artificial.mcp.McpToolExecutor
import com.tom.rv2ide.artificial.agents.Agents
import com.tom.rv2ide.artificial.rules.WritingRules
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.exceptions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class Anthropic : AIAgent {

  private var apiKey: String? = null
  private val writingRules = WritingRules.Instructions()
  private var projectTreeResult: ProjectTreeResult? = null
  private var fileWriter: AIFileWriter? = null
  private val conversationHistory = mutableListOf<ConversationMessage>()
  private val modificationHistory = mutableListOf<ModificationAttempt>()
  private var currentAttemptCount = 0
  private val maxRetryAttempts = 3
  private var agents: Agents? = null
  private var selectedModel: String = "claude-sonnet-4-20250514"
  private val nativeMessages = JSONArray()
  private var nativeToolsActive = false
  private var appContext: Context? = null
  override val providerId = "claude"
  override val providerName = "Anthropic Claude"

  companion object {
      fun registerAgent() {
          AIAgentRegistry.register("claude", object : AIAgentRegistry.AgentFactory {
              override fun create(context: Context): AIAgent {
                  return Anthropic()
              }
              
              override fun hasValidApiKey(): Boolean {
                  val apiKey = getApiKey()
                  return apiKey != null && apiKey.isNotBlank() && apiKey.length > 20
              }
              
              override fun getApiKey(): String? {
                  val prefManager = com.tom.rv2ide.preferences.internal.prefManager
                  return prefManager.getString("ai_agent_anthropic_api_key", "")?.takeIf { it.isNotBlank() }
              }
          })
      }
  }
        
  override fun initialize(apiKey: String, context: Context) {
      try {
          this.apiKey = apiKey
          agents = Agents(context)
          var selectedModel = agents?.getAgent() ?: "claude-sonnet-4-20250514"
          
          if (!agents!!.isValidModelForProvider(selectedModel, "claude")) {
              selectedModel = "claude-sonnet-4-20250514"
              agents?.setAgent(selectedModel)
              agents?.setProvider("claude")
          }
          
          this.selectedModel = selectedModel
      } catch (e: Exception) {
          throw e
      }
  }

  override fun reinitializeWithNewModel(apiKey: String, context: Context) {
    initialize(apiKey, context)
  }

  override fun setContext(context: Context) {
    appContext = context.applicationContext
    fileWriter = AIFileWriter(context)
  }

  override fun setProjectData(projectTreeResult: ProjectTreeResult) {
    this.projectTreeResult = projectTreeResult
  }

  override fun clearConversation() {
    nativeToolsActive = false; while (nativeMessages.length() > 0) nativeMessages.remove(0)
    conversationHistory.clear()
    modificationHistory.clear()
    currentAttemptCount = 0
  }

  override fun recordModification(filePath: String, oldContent: String?, newContent: String, success: Boolean) {
    modificationHistory.add(
        ModificationAttempt(
            timestamp = System.currentTimeMillis(),
            filePath = filePath,
            previousContent = oldContent,
            newContent = newContent,
            attemptNumber = currentAttemptCount,
            success = success
        )
    )
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
    val correctionKeywords = listOf(
        "wrong", "not what", "mistake", "error", "incorrect", 
        "that's not", "not right", "fix", "undo", "revert",
        "different", "try again", "not working"
    )
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
          val key = apiKey
              ?: return@withContext Result.failure(
                  IllegalStateException("Anthropic service not initialized")
              )

          val fileContents = readRelevantFiles()
          val needsCorrection = isUserRequestingCorrection(prompt)

          val fullPrompt = buildString {
            append("=== PROJECT STRUCTURE (THESE ARE THE EXACT PATHS YOU MUST USE) ===\n")
            if (projectTreeResult != null) {
              append(projectTreeResult!!.tree)
              append("\n\n")
              append("CRITICAL: Use ONLY the paths shown above. Do NOT make up fake paths like '/storage/emulated/0/project' or 'com.example.yourproject'.\n")
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
              conversationHistory.forEach { msg ->
                append("${msg.role.uppercase()}: ${msg.content}\n\n")
              }
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

          val response = callAnthropicAPI(key, fullPrompt)

          if (response.isBlank()) {
            return@withContext Result.failure(Exception("Empty response from AI"))
          }

          conversationHistory.add(ConversationMessage("user", prompt))
          conversationHistory.add(ConversationMessage("assistant", response))

          if (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
            conversationHistory.removeAt(0)
          }

          Result.success(response)
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  private fun callAnthropicAPI(apiKey: String, prompt: String): String {
    android.util.Log.d("Anthropic", "Starting API call to Anthropic")
    
    val url = URL("https://api.anthropic.com/v1/messages")
    val connection = url.openConnection() as HttpURLConnection
    
    try {
      connection.requestMethod = "POST"
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("x-api-key", apiKey)
      connection.setRequestProperty("anthropic-version", "2023-06-01")
      connection.doOutput = true
      connection.connectTimeout = 30000
      connection.readTimeout = 30000
      
      val messages = JSONArray()
      val userMessage = JSONObject()
      userMessage.put("role", "user")
      userMessage.put("content", prompt)
      messages.put(userMessage)
      
      val requestBody = JSONObject()
      requestBody.put("model", selectedModel)
      requestBody.put("max_tokens", 4096)
      requestBody.put("system", writingRules.useThis())
      requestBody.put("messages", messages)
      
      android.util.Log.d("Anthropic", "Request body: ${requestBody.toString()}")
      
      connection.outputStream.use { os ->
        os.write(requestBody.toString().toByteArray())
      }
      
      val responseCode = connection.responseCode
      android.util.Log.d("Anthropic", "Response code: $responseCode")
      
      if (responseCode != HttpURLConnection.HTTP_OK) {
        val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
        android.util.Log.e("Anthropic", "Error response: $errorStream")
        
        try {
          val errorJson = JSONObject(errorStream)
          val errorObj = errorJson.optJSONObject("error")
          val errorMessage = errorObj?.optString("message") ?: errorStream
          val errorType = errorObj?.optString("type") ?: ""
          
          android.util.Log.e("Anthropic", "Error type: $errorType, message: $errorMessage")
          
          when {
            responseCode == 429 || errorType.contains("rate_limit") -> 
              throw RateLimitException("Anthropic rate limit exceeded: $errorMessage")
            errorType.contains("insufficient_quota") || errorMessage.contains("quota") -> 
              throw QuotaExceededException("Anthropic quota exceeded: $errorMessage")
            errorType.contains("authentication") || responseCode == 401 -> 
              throw InvalidApiKeyException("Invalid Anthropic API key: $errorMessage")
            else -> 
              throw Exception("Anthropic API error ($responseCode) - Type: $errorType, Message: $errorMessage")
          }
        } catch (e: RateLimitException) {
          throw e
        } catch (e: QuotaExceededException) {
          throw e
        } catch (e: InvalidApiKeyException) {
          throw e
        } catch (e: Exception) {
          throw Exception("Anthropic API error ($responseCode): $errorStream")
        }
      }
      
      val responseBody = connection.inputStream.bufferedReader().readText()
      android.util.Log.d("Anthropic", "Success response received, length: ${responseBody.length}")
      
      val jsonResponse = JSONObject(responseBody)
      val content = jsonResponse.getJSONArray("content")
      
      if (content.length() > 0) {
        val firstContent = content.getJSONObject(0)
        return firstContent.getString("text")
      }
      
      throw Exception("No response from Anthropic API")
    } catch (e: RateLimitException) {
      android.util.Log.e("Anthropic", "Rate limit exception", e)
      throw e
    } catch (e: QuotaExceededException) {
      android.util.Log.e("Anthropic", "Quota exceeded exception", e)
      throw e
    } catch (e: InvalidApiKeyException) {
      android.util.Log.e("Anthropic", "Invalid API key exception", e)
      throw e
    } catch (e: java.net.SocketTimeoutException) {
      android.util.Log.e("Anthropic", "Timeout exception", e)
      throw Exception("Anthropic request timeout: ${e.message}")
    } catch (e: java.net.UnknownHostException) {
      android.util.Log.e("Anthropic", "Network exception", e)
      throw Exception("Network error - cannot reach Anthropic: ${e.message}")
    } catch (e: Exception) {
      android.util.Log.e("Anthropic", "General exception", e)
      throw e
    } finally {
      connection.disconnect()
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
        } catch (e: Exception) {
        }
      }
    }
    
    return filesContent
  }

  fun readFile(filePath: String): String? {
    return try {
      if (File(filePath).exists()) {
        File(filePath).readText()
      } else {
        projectTreeResult?.readFileContent(File(filePath).name)
      }
    } catch (e: Exception) {
      null
    }
  }

  override fun writeFile(filePath: String, content: String): FileWriteResult {
    val writer = fileWriter ?: return FileWriteResult.Error("File writer not initialized")
    return writer.writeFile(filePath, content, createBackup = true)
  }

  fun parseAndApplyModifications(response: String, capturedStates: Map<String, String>): List<FileModification> {
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


  private fun composeSystemPrompt(ctx: Context, includeTextToolDefs: Boolean): String {
    val sb = StringBuilder(writingRules.useThis())
    val agentProfile = AgentPromptProfileStore.getActive()
    val extra = agentProfile?.systemPrompt?.trim().orEmpty()
    if (extra.isNotEmpty()) sb.append("\n\n=== AGENT PROFILE INSTRUCTIONS ===\n").append(extra).append("\n")
    if (includeTextToolDefs && agentProfile?.enableTools != false) {
      val builtIn = BuiltInToolExecutor(ctx, projectTreeResult, null, agentProfile?.allowCommandExecution == true, agentProfile?.disabledTools?.toSet() ?: emptySet())
      val mcpDefs = if (agentProfile?.enableMcpTools != false) { try { val servers = McpServerStore.load().filter { s -> agentProfile?.enabledMcpServerIds.isNullOrEmpty() || agentProfile!!.enabledMcpServerIds.contains(s.id) }; McpToolExecutor(servers).definitions().filterNot { it.name in (agentProfile?.disabledTools?.toSet() ?: emptySet()) } } catch (_: Exception) { emptyList() } } else emptyList()
      sb.append(ToolCallProtocol.formatDefinitions(builtIn.definitions() + mcpDefs))
    }
    return sb.toString()
  }
  private fun clearNative() { nativeToolsActive = false; while (nativeMessages.length() > 0) nativeMessages.remove(0) }

  private fun anthropicToolsJson(tools: List<AIToolDef>): JSONArray {
    val arr = JSONArray()
    tools.forEach { t ->
      arr.put(JSONObject().apply {
        put("name", t.name)
        put("description", t.description)
        put("input_schema", try { JSONObject(t.parametersJsonSchema) } catch (_: Exception) { JSONObject().put("type","object").put("properties", JSONObject()) })
      })
    }
    return arr
  }

  private fun callAnthropicNative(messages: JSONArray, system: String, tools: List<AIToolDef>?): ToolAwareResponse {
    val key = apiKey ?: throw IllegalStateException("Anthropic service not initialized")
    val url = URL("https://api.anthropic.com/v1/messages")
    val connection = url.openConnection() as HttpURLConnection
    try {
      connection.requestMethod = "POST"
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("x-api-key", key)
      connection.setRequestProperty("anthropic-version", "2023-06-01")
      connection.doOutput = true
      connection.connectTimeout = 30000
      connection.readTimeout = 90000
      val requestBody = JSONObject().apply {
        put("model", selectedModel)
        put("max_tokens", 4096)
        put("system", system)
        put("messages", messages)
        if (!tools.isNullOrEmpty()) put("tools", anthropicToolsJson(tools))
      }
      connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }
      val responseCode = connection.responseCode
      if (responseCode != HttpURLConnection.HTTP_OK) {
        val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
        throw Exception("Anthropic API error ($responseCode): $errorStream")
      }
      val jsonResponse = JSONObject(connection.inputStream.bufferedReader().readText())
      val content = jsonResponse.optJSONArray("content") ?: JSONArray()
      val sb = StringBuilder()
      val calls = mutableListOf<AIToolCall>()
      for (i in 0 until content.length()) {
        val block = content.optJSONObject(i) ?: continue
        when (block.optString("type")) {
          "text" -> sb.append(block.optString("text"))
          "tool_use" -> {
            val name = block.optString("name")
            val input = block.optJSONObject("input") ?: JSONObject()
            if (name.isNotBlank()) calls.add(AIToolCall(name, input, block.toString(), block.optString("id").ifBlank { null }))
          }
        }
      }
      val text = sb.toString()
      if (calls.isEmpty()) ToolCallProtocol.parse(text)?.let { calls.add(it) }
      return ToolAwareResponse(text, calls)
    } finally { connection.disconnect() }
  }

  override fun supportsNativeTools(): Boolean = true
  override suspend fun generateWithTools(prompt: String, context: String?, language: String, projectStructure: String?, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      clearNative(); nativeToolsActive = true
      nativeMessages.put(JSONObject().put("role","user").put("content", prompt))
      val res = callAnthropicNative(nativeMessages, composeSystemPrompt(ctx, false), tools)
      // store assistant message with content blocks
      val assistantContent = JSONArray()
      if (res.text.isNotBlank()) assistantContent.put(JSONObject().put("type","text").put("text", res.text))
      res.toolCalls.forEach { tc -> assistantContent.put(JSONObject().put("type","tool_use").put("id", tc.id ?: "toolu_${tc.name}_${System.nanoTime()}").put("name", tc.name).put("input", tc.arguments)) }
      if (assistantContent.length() > 0) nativeMessages.put(JSONObject().put("role","assistant").put("content", assistantContent))
      Result.success(res)
    } catch (e: Exception) { Result.failure(e) }
  }
  override suspend fun submitToolResults(toolResults: List<AIToolResult>, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      if (!nativeToolsActive) return@withContext generateWithTools(toolResults.joinToString("\n"){it.result}, null, "kotlin", null, tools)
      val resultBlocks = JSONArray()
      toolResults.forEach { tr -> resultBlocks.put(JSONObject().put("type","tool_result").put("tool_use_id", tr.toolCallId ?: tr.name).put("content", tr.result)) }
      nativeMessages.put(JSONObject().put("role","user").put("content", resultBlocks))
      val res = callAnthropicNative(nativeMessages, composeSystemPrompt(ctx, false), tools)
      val assistantContent = JSONArray()
      if (res.text.isNotBlank()) assistantContent.put(JSONObject().put("type","text").put("text", res.text))
      res.toolCalls.forEach { tc -> assistantContent.put(JSONObject().put("type","tool_use").put("id", tc.id ?: "toolu_${tc.name}_${System.nanoTime()}").put("name", tc.name).put("input", tc.arguments)) }
      if (assistantContent.length() > 0) nativeMessages.put(JSONObject().put("role","assistant").put("content", assistantContent))
      Result.success(res)
    } catch (e: Exception) { Result.failure(e) }
  }

  override fun isInitialized(): Boolean = apiKey != null
}

data class FileModification(
    val filePath: String,
    val content: String,
    val writeResult: FileWriteResult
)

data class ConversationMessage(
    val role: String,
    val content: String
)
