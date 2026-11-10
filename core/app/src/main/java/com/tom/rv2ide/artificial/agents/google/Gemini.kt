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

package com.tom.rv2ide.artificial.agents.google

import android.content.Context
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import com.tom.rv2ide.artificial.secrets.ApiKey
import com.tom.rv2ide.artificial.services.ArtificialService
import com.tom.rv2ide.artificial.rules.WritingRules
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.tom.rv2ide.artificial.agents.Agents

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
*/

class Gemini : AIAgent {

  private var generativeModel: GenerativeModel? = null
  private var apiKey: String? = null
  private var selectedModel: String = "gemini-2.5-pro"
  private var appContext: Context? = null
  private val nativeContents = JSONArray()
  private var nativeToolsActive = false
  private val writingRules = WritingRules.Instructions()
  private var projectTreeResult: ProjectTreeResult? = null
  private var fileWriter: AIFileWriter? = null
  private val conversationHistory = mutableListOf<ConversationMessage>()
  private val modificationHistory = mutableListOf<ModificationAttempt>()
  private var currentAttemptCount = 0
  private val maxRetryAttempts = 3
  private var agents: Agents? = null
  override val providerId = "gemini"
  override val providerName = "Google Gemini"

  companion object {
      fun registerAgent() {
          AIAgentRegistry.register("gemini", object : AIAgentRegistry.AgentFactory {
              override fun create(context: Context): AIAgent {
                  return Gemini()
              }
              
              override fun hasValidApiKey(): Boolean {
                  val key = ApiKey.getApiKey()
                  android.util.Log.d("Gemini", "hasValidApiKey check: ${key != null && key.isNotEmpty()}, key length: ${key?.length ?: 0}")
                  return key != null && key.isNotEmpty()
              }
              
              override fun getApiKey(): String? {
                  val key = ApiKey.getApiKey()
                  android.util.Log.d("Gemini", "getApiKey called, returning key of length: ${key?.length ?: 0}")
                  return key
              }
          })
      }
  }
        
  override fun initialize(apiKey: String, context: Context) {
      try {
          agents = Agents(context)
          var selectedModel = agents?.getAgent() ?: "gemini-2.5-pro"
          
          // Ensure we're using a valid Gemini model
          if (!agents!!.isValidModelForProvider(selectedModel, "gemini")) {
              selectedModel = "gemini-2.5-pro"
              agents?.setAgent(selectedModel)
              agents?.setProvider("gemini")
          }
          
          this.apiKey = apiKey
          this.selectedModel = selectedModel
          generativeModel = GenerativeModel(
              modelName = selectedModel,
              apiKey = apiKey,
              systemInstruction = content { text(writingRules.useThis()) }
          )
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
    nativeToolsActive = false; while (nativeContents.length() > 0) nativeContents.remove(0)
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
          val model =
              generativeModel
                  ?: return@withContext Result.failure(
                      IllegalStateException("Gemini AI service not initialized")
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

          val response = try {
            model.generateContent(content { text(fullPrompt) })
          } catch (e: Exception) {
            // Parse Gemini-specific errors
            val errorMessage = e.message ?: ""
            when {
              errorMessage.contains("RESOURCE_EXHAUSTED") || 
              errorMessage.contains("quota") ||
              errorMessage.contains("429") -> 
                throw com.tom.rv2ide.artificial.exceptions.QuotaExceededException(
                  "Gemini API quota exceeded. Switching to another provider..."
                )
              errorMessage.contains("RATE_LIMIT") ||
              errorMessage.contains("rate limit") -> 
                throw com.tom.rv2ide.artificial.exceptions.RateLimitException(
                  "Gemini rate limit exceeded. Switching to another provider..."
                )
              errorMessage.contains("INVALID_ARGUMENT") ||
              errorMessage.contains("API key") -> 
                throw com.tom.rv2ide.artificial.exceptions.InvalidApiKeyException(
                  "Invalid Gemini API key. Please check your configuration."
                )
              else -> throw e
            }
          }

          val generatedResponse = response.text ?: ""
          if (generatedResponse.isBlank()) {
            return@withContext Result.failure(Exception("Empty response from AI"))
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
        } catch (e: Exception) {
          // Skip files that can't be read
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
  private fun clearNative() { nativeToolsActive = false; while (nativeContents.length() > 0) nativeContents.remove(0) }

  private fun geminiToolsJson(tools: List<AIToolDef>): JSONArray {
    val decls = JSONArray()
    tools.forEach { t ->
      decls.put(JSONObject().apply {
        put("name", t.name)
        put("description", t.description)
        put("parameters", try { JSONObject(t.parametersJsonSchema) } catch (_: Exception) { JSONObject().put("type","object").put("properties", JSONObject()) })
      })
    }
    return JSONArray().put(JSONObject().put("functionDeclarations", decls))
  }

  private fun callGeminiNative(contents: JSONArray, system: String, tools: List<AIToolDef>?): ToolAwareResponse {
    val key = apiKey ?: throw IllegalStateException("Gemini not initialized")
    val model = selectedModel.ifBlank { "gemini-2.5-pro" }
    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$key"
    val connection = URL(endpoint).openConnection() as HttpURLConnection
    try {
      connection.requestMethod = "POST"
      connection.setRequestProperty("Content-Type", "application/json")
      connection.doOutput = true
      connection.connectTimeout = 30000
      connection.readTimeout = 90000
      val body = JSONObject().apply {
        put("contents", contents)
        put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
        if (!tools.isNullOrEmpty()) put("tools", geminiToolsJson(tools))
        put("generationConfig", JSONObject().put("temperature", 0.7))
      }
      connection.outputStream.use { it.write(body.toString().toByteArray()) }
      val code = connection.responseCode
      val text = (if (code in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.readText().orEmpty()
      if (code !in 200..299) throw Exception("Gemini API error ($code): $text")
      val json = JSONObject(text)
      val parts = json.optJSONArray("candidates")?.optJSONObject(0)
        ?.optJSONObject("content")?.optJSONArray("parts") ?: JSONArray()
      val sb = StringBuilder()
      val calls = mutableListOf<AIToolCall>()
      for (i in 0 until parts.length()) {
        val p = parts.optJSONObject(i) ?: continue
        if (p.has("text")) sb.append(p.optString("text"))
        if (p.has("functionCall")) {
          val fc = p.optJSONObject("functionCall") ?: continue
          val name = fc.optString("name")
          val args = fc.optJSONObject("args") ?: JSONObject()
          if (name.isNotBlank()) calls.add(AIToolCall(name, args, fc.toString(), null))
        }
      }
      val out = sb.toString()
      if (calls.isEmpty()) ToolCallProtocol.parse(out)?.let { calls.add(it) }
      return ToolAwareResponse(out, calls)
    } finally { connection.disconnect() }
  }

  override fun supportsNativeTools(): Boolean = true
  override suspend fun generateWithTools(prompt: String, context: String?, language: String, projectStructure: String?, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      clearNative(); nativeToolsActive = true
      nativeContents.put(JSONObject().put("role","user").put("parts", JSONArray().put(JSONObject().put("text", prompt))))
      val res = callGeminiNative(nativeContents, composeSystemPrompt(ctx, false), tools)
      // store model response
      val modelParts = JSONArray()
      if (res.text.isNotBlank()) modelParts.put(JSONObject().put("text", res.text))
      res.toolCalls.forEach { tc -> modelParts.put(JSONObject().put("functionCall", JSONObject().put("name", tc.name).put("args", tc.arguments))) }
      if (modelParts.length() > 0) nativeContents.put(JSONObject().put("role","model").put("parts", modelParts))
      Result.success(res)
    } catch (e: Exception) { Result.failure(e) }
  }
  override suspend fun submitToolResults(toolResults: List<AIToolResult>, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      if (!nativeToolsActive) return@withContext generateWithTools(toolResults.joinToString("\n"){it.result}, null, "kotlin", null, tools)
      val parts = JSONArray()
      toolResults.forEach { tr ->
        parts.put(JSONObject().put("functionResponse", JSONObject().put("name", tr.name).put("response", JSONObject().put("name", tr.name).put("content", tr.result))))
      }
      nativeContents.put(JSONObject().put("role","user").put("parts", parts))
      val res = callGeminiNative(nativeContents, composeSystemPrompt(ctx, false), tools)
      val modelParts = JSONArray()
      if (res.text.isNotBlank()) modelParts.put(JSONObject().put("text", res.text))
      res.toolCalls.forEach { tc -> modelParts.put(JSONObject().put("functionCall", JSONObject().put("name", tc.name).put("args", tc.arguments))) }
      if (modelParts.length() > 0) nativeContents.put(JSONObject().put("role","model").put("parts", modelParts))
      Result.success(res)
    } catch (e: Exception) { Result.failure(e) }
  }

  override fun isInitialized(): Boolean = generativeModel != null
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
