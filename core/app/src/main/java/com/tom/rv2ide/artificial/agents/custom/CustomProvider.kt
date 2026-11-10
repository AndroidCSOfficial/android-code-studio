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
import android.util.Log
import com.tom.rv2ide.artificial.agents.AIAgent
import com.tom.rv2ide.artificial.agents.AIAgentRegistry
import com.tom.rv2ide.artificial.agents.Agents
import com.tom.rv2ide.artificial.agents.ModificationAttempt
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.mcp.McpServerStore
import com.tom.rv2ide.artificial.mcp.McpToolExecutor
import com.tom.rv2ide.artificial.profiles.AgentPromptProfileStore
import com.tom.rv2ide.artificial.profiles.CustomProviderProfileStore
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.rules.WritingRules
import com.tom.rv2ide.artificial.tools.AIToolCall
import com.tom.rv2ide.artificial.tools.AIToolDef
import com.tom.rv2ide.artificial.tools.AIToolResult
import com.tom.rv2ide.artificial.tools.BuiltInToolExecutor
import com.tom.rv2ide.artificial.tools.ToolAwareResponse
import com.tom.rv2ide.artificial.tools.ToolCallProtocol
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

class CustomProvider : AIAgent {
  private var baseUrl: String? = null
  private var modelName: String? = null
  private var apiKey: String? = null
  private val httpClient = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
  private val writingRules = WritingRules.Instructions()
  private var projectTreeResult: ProjectTreeResult? = null
  private var fileWriter: AIFileWriter? = null
  private val conversationHistory = mutableListOf<ConversationMessage>()
  private val modificationHistory = mutableListOf<ModificationAttempt>()
  private var currentAttemptCount = 0
  private val maxRetryAttempts = 3
  private var appContext: Context? = null
  private val nativeMessages = JSONArray()
  private var nativeToolsActive = false
  override val providerId = "custom"
  override val providerName = "Custom Provider"

  companion object {
    private const val TAG = "CustomProvider"
    fun registerAgent() {
      AIAgentRegistry.register("custom", object : AIAgentRegistry.AgentFactory {
        override fun create(context: Context): AIAgent = CustomProvider()
        override fun hasValidApiKey(): Boolean { val p = CustomProviderProfileStore.getActive(); return p != null && p.baseUrl.isNotBlank() && p.modelName.isNotBlank() }
        override fun getApiKey(): String? { val p = CustomProviderProfileStore.getActive() ?: return null; return if (p.apiKey.isBlank()) "custom" else p.apiKey }
      })
    }
    private fun normalizedBase(baseUrl: String): String = baseUrl.trim().trimEnd('/')
    private fun buildEndpoint(baseUrl: String): String {
      val trimmed = normalizedBase(baseUrl)
      return when { trimmed.contains("/chat/completions") -> trimmed; trimmed.endsWith("/v1") -> "$trimmed/chat/completions"; else -> "$trimmed/v1/chat/completions" }
    }
    private fun buildModelsEndpoint(baseUrl: String): String {
      val trimmed = normalizedBase(baseUrl)
      return when { trimmed.endsWith("/models") -> trimmed; trimmed.endsWith("/v1") -> "$trimmed/models"; trimmed.contains("/v1/") -> trimmed.substringBefore("/v1/") + "/v1/models"; else -> "$trimmed/v1/models" }
    }
    /** Fetch available model IDs from an OpenAI-compatible /v1/models endpoint (e.g. Nvidia, OpenAI, Ollama). */
    fun fetchModels(baseUrl: String, apiKey: String?): Result<List<String>> {
      return try {
        val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
        val req = Request.Builder().url(buildModelsEndpoint(baseUrl)).get().addHeader("Accept", "application/json")
        val key = apiKey?.trim(); if (!key.isNullOrEmpty()) req.addHeader("Authorization", "Bearer $key")
        client.newCall(req.build()).execute().use { resp ->
          val body = resp.body?.string().orEmpty()
          if (!resp.isSuccessful) return Result.failure(Exception("HTTP ${resp.code}: ${body.take(300)}"))
          val json = JSONObject(body)
          val data = json.optJSONArray("data") ?: json.optJSONArray("models") ?: return Result.success(emptyList())
          val ids = mutableListOf<String>()
          for (i in 0 until data.length()) {
            val item = data.opt(i)
            val id = when (item) {
              is JSONObject -> item.optString("id").ifBlank { item.optString("name") }
              is String -> item
              else -> ""
            }
            if (id.isNotBlank()) ids.add(id)
          }
          Result.success(ids.distinct().sorted())
        }
      } catch (e: Exception) { Result.failure(e) }
    }
  }

  override fun initialize(apiKey: String, context: Context) {
    appContext = context.applicationContext
    val profile = CustomProviderProfileStore.getActive()
    baseUrl = profile?.baseUrl; modelName = profile?.modelName; this.apiKey = profile?.apiKey
    if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) throw IllegalStateException("Custom provider not configured. Add/select a custom provider profile.")
    Log.d(TAG, "Initialized custom provider: endpoint=${buildEndpoint(baseUrl!!)}, model=$modelName")
  }
  override fun isInitialized(): Boolean = !baseUrl.isNullOrEmpty() && !modelName.isNullOrEmpty()
  override fun reinitializeWithNewModel(apiKey: String, context: Context) { initialize(apiKey, context) }
  override fun setContext(context: Context) { appContext = context.applicationContext; fileWriter = AIFileWriter(context) }
  override fun setProjectData(projectTreeResult: ProjectTreeResult) { this.projectTreeResult = projectTreeResult }
  override fun clearConversation() { conversationHistory.clear(); modificationHistory.clear(); currentAttemptCount = 0; clearNative() }
  override fun recordModification(filePath: String, oldContent: String?, newContent: String, success: Boolean) { modificationHistory.add(ModificationAttempt(System.currentTimeMillis(), filePath, oldContent, newContent, currentAttemptCount, success)) }
  override fun undoLastModification(): Boolean {
    if (modificationHistory.isEmpty()) return false
    val lastMod = modificationHistory.lastOrNull { it.success } ?: return false
    if (lastMod.previousContent != null) { val result = writeFile(lastMod.filePath, lastMod.previousContent); if (result is FileWriteResult.Success) { modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod)); return true } }
    else { try { File(lastMod.filePath).delete(); modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod)); return true } catch (_: Exception) {} }
    return false
  }
  override fun getModificationHistory(): List<ModificationAttempt> = modificationHistory.toList()
  override fun resetAttemptCount() { currentAttemptCount = 0 }
  override fun incrementAttemptCount() { currentAttemptCount++ }
  override fun getCurrentAttemptCount(): Int = currentAttemptCount
  override fun canRetry(): Boolean = currentAttemptCount < maxRetryAttempts

  private fun isUserRequestingCorrection(message: String): Boolean = listOf("wrong","not what","mistake","error","incorrect","that's not","not right","fix","undo","revert","different","try again","not working").any { message.lowercase().contains(it) }

  private fun readRelevantFiles(maxFiles: Int = 12, maxCharsPerFile: Int = 6000, maxTotalChars: Int = 40000): Map<String, String> {
    val filesContent = linkedMapOf<String, String>(); val tree = projectTreeResult?.tree ?: return filesContent
    var total = 0
    tree.lines().filter { it.isNotBlank() }.forEach { filePath ->
      if (filesContent.size >= maxFiles || total >= maxTotalChars) return@forEach
      val trimmedPath = filePath.trim(); val file = File(trimmedPath)
      if (file.isFile && (trimmedPath.endsWith(".kt") || trimmedPath.endsWith(".java") || trimmedPath.endsWith(".xml") || trimmedPath.endsWith(".gradle") || trimmedPath.endsWith(".gradle.kts")) && !trimmedPath.contains("/build/") && !trimmedPath.contains("/.gradle/")) {
        try {
          val content = file.readText()
          val clipped = if (content.length > maxCharsPerFile) content.take(maxCharsPerFile) + "\n...[truncated]..." else content
          if (total + clipped.length > maxTotalChars) return@forEach
          filesContent[trimmedPath] = clipped; total += clipped.length
        } catch (_: Exception) {}
      }
    }
    return filesContent
  }

  private fun composeSystemPrompt(context: Context, includeTextToolDefs: Boolean): String {
    val sb = StringBuilder(writingRules.useThis())
    val agentProfile = AgentPromptProfileStore.getActive()
    val extra = agentProfile?.systemPrompt?.trim().orEmpty()
    if (extra.isNotEmpty()) sb.append("\n\n=== AGENT PROFILE INSTRUCTIONS ===\n").append(extra).append("\n")
    if (includeTextToolDefs && agentProfile?.enableTools != false) {
      val builtIn = BuiltInToolExecutor(context, projectTreeResult, null, agentProfile?.allowCommandExecution == true, agentProfile?.disabledTools?.toSet() ?: emptySet())
      val mcpDefs = if (agentProfile?.enableMcpTools != false) { try { val servers = McpServerStore.load().filter { s -> agentProfile?.enabledMcpServerIds.isNullOrEmpty() || agentProfile!!.enabledMcpServerIds.contains(s.id) }; McpToolExecutor(servers).definitions().filterNot { it.name in (agentProfile?.disabledTools?.toSet() ?: emptySet()) } } catch (_: Exception) { emptyList() } } else emptyList()
      sb.append(ToolCallProtocol.formatDefinitions(builtIn.definitions() + mcpDefs))
    }
    return sb.toString()
  }

  private fun buildUserPrompt(prompt: String, context: String?): String {
    val fileContents = readRelevantFiles()
    val needsCorrection = isUserRequestingCorrection(prompt)
    return buildString {
      if (projectTreeResult != null) {
        val tree = projectTreeResult!!.tree
        append("=== PROJECT STRUCTURE (use exact paths) ===\n")
        append(if (tree.length > 12000) tree.take(12000) + "\n...[truncated]..." else tree)
        append("\n\n")
      }
      if (fileContents.isNotEmpty()) { append("=== CURRENT FILES (sample) ===\n"); fileContents.forEach { (path, content) -> append("FILE: $path\nCONTENT:\n").append(content).append("\n\n") } }
      if (!context.isNullOrBlank()) append("=== ADDITIONAL CONTEXT ===\n").append(if (context.length > 8000) context.take(8000) + "\n...[truncated]" else context).append("\n\n")
      if (conversationHistory.isNotEmpty()) { append("=== RECENT CONVERSATION ===\n"); conversationHistory.takeLast(6).forEach { msg -> append("${msg.role.uppercase()}: ${msg.content.take(2000)}\n\n") } }
      if (needsCorrection && modificationHistory.isNotEmpty()) append("=== CORRECTION REQUIRED ===\nTry a different approach.\n\n")
      append("=== USER REQUEST ===\n").append(prompt)
    }
  }

  override suspend fun generateCode(prompt: String, context: String?, language: String, projectStructure: String?): Result<String> = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      clearNative()
      val response = callChat(buildMessages(composeSystemPrompt(ctx, includeTextToolDefs = true), buildUserPrompt(prompt, context)), tools = null)
      conversationHistory.add(ConversationMessage("user", prompt)); conversationHistory.add(ConversationMessage("assistant", response.text))
      while (conversationHistory.size > 20) conversationHistory.removeAt(0)
      Result.success(response.text)
    } catch (e: Exception) { Log.e(TAG, "generateCode failed", e); Result.failure(e) }
  }

  override fun supportsNativeTools(): Boolean = true

  override suspend fun generateWithTools(prompt: String, context: String?, language: String, projectStructure: String?, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try {
      val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context"))
      clearNative(); nativeToolsActive = true
      nativeMessages.put(JSONObject().put("role", "system").put("content", composeSystemPrompt(ctx, includeTextToolDefs = false)))
      nativeMessages.put(JSONObject().put("role", "user").put("content", buildUserPrompt(prompt, context)))
      val res = try { callChat(nativeMessages, tools) } catch (e: Exception) {
        Log.w(TAG, "Native tool call failed, falling back to text mode: ${e.message}")
        clearNative()
        val fallback = callChat(buildMessages(composeSystemPrompt(ctx, includeTextToolDefs = true), buildUserPrompt(prompt, context)), tools = null)
        return@withContext Result.success(fallback)
      }
      if (res.hasToolCalls()) nativeMessages.put(assistantToolCallMessage(res)) else nativeMessages.put(JSONObject().put("role","assistant").put("content", res.text))
      Result.success(res)
    } catch (e: Exception) { Log.e(TAG, "generateWithTools failed", e); Result.failure(e) }
  }

  override suspend fun submitToolResults(toolResults: List<AIToolResult>, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try {
      if (!nativeToolsActive) return@withContext generateWithTools(toolResults.joinToString("\n") { "${it.name}: ${it.result}" }, null, "kotlin", null, tools)
      toolResults.forEach { tr ->
        nativeMessages.put(JSONObject().apply { put("role", "tool"); put("tool_call_id", tr.toolCallId ?: tr.name); put("content", tr.result) })
      }
      val res = callChat(nativeMessages, tools)
      if (res.hasToolCalls()) nativeMessages.put(assistantToolCallMessage(res)) else nativeMessages.put(JSONObject().put("role","assistant").put("content", res.text))
      Result.success(res)
    } catch (e: Exception) { Log.e(TAG, "submitToolResults failed", e); Result.failure(e) }
  }

  private fun assistantToolCallMessage(res: ToolAwareResponse): JSONObject = JSONObject().apply {
    put("role", "assistant")
    if (res.text.isNotBlank()) put("content", res.text) else put("content", JSONObject.NULL)
    val arr = JSONArray()
    res.toolCalls.forEach { tc ->
      arr.put(JSONObject().apply {
        put("id", tc.id ?: "call_${tc.name.take(20)}_${System.nanoTime()}")
        put("type", "function")
        put("function", JSONObject().apply { put("name", tc.name); put("arguments", tc.arguments.toString()) })
      })
    }
    put("tool_calls", arr)
  }

  private fun clearNative() { nativeToolsActive = false; while (nativeMessages.length() > 0) nativeMessages.remove(0) }
  private fun buildMessages(system: String, user: String): JSONArray = JSONArray().apply { put(JSONObject().put("role","system").put("content", system)); put(JSONObject().put("role","user").put("content", user)) }

  private fun callChat(messages: JSONArray, tools: List<AIToolDef>?): ToolAwareResponse {
    if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) throw IllegalStateException("Custom provider not configured. Set base URL and model.")
    val requestBody = JSONObject().apply {
      put("model", modelName); put("messages", messages); put("temperature", 0.7); put("stream", false)
      if (!tools.isNullOrEmpty()) { put("tools", ToolCallProtocol.openAIToolsJson(tools)); put("tool_choice", "auto") }
    }
    val endpoint = buildEndpoint(baseUrl!!)
    val requestBuilder = Request.Builder().url(endpoint).post(requestBody.toString().toRequestBody("application/json".toMediaType()))
      .addHeader("Content-Type", "application/json").addHeader("Accept", "application/json")
    val key = apiKey?.trim(); if (!key.isNullOrEmpty()) requestBuilder.addHeader("Authorization", "Bearer $key")
    Log.d(TAG, "POST $endpoint model=$modelName tools=${tools?.size ?: 0}")
    val response = try { httpClient.newCall(requestBuilder.build()).execute() } catch (e: Exception) { throw Exception("Connection to $endpoint failed: ${e.message}. Check base URL, network, and API key.", e) }
    val body = response.body?.string().orEmpty()
    if (!response.isSuccessful) {
      Log.e(TAG, "API error ${response.code}: ${body.take(500)}")
      throw Exception("Custom provider error ${response.code}: ${body.take(400).ifBlank { response.message }}")
    }
    if (body.isBlank()) throw Exception("Empty response from custom provider ($endpoint)")
    val json = try { JSONObject(body) } catch (e: Exception) { throw Exception("Invalid JSON response: ${body.take(300)}") }
    if (json.has("error")) throw Exception("API error: ${json.opt("error")}")
    val choices = json.optJSONArray("choices")
    if (choices == null || choices.length() == 0) throw Exception("No choices in response: ${body.take(300)}")
    val msg = choices.getJSONObject(0).optJSONObject("message") ?: JSONObject()
    val text = when { msg.isNull("content") -> ""; else -> msg.optString("content", "") }
    val toolCallsArr = msg.optJSONArray("tool_calls")
    val calls = mutableListOf<AIToolCall>()
    if (toolCallsArr != null) {
      for (i in 0 until toolCallsArr.length()) {
        val tc = toolCallsArr.optJSONObject(i) ?: continue
        val fn = tc.optJSONObject("function") ?: continue
        val name = fn.optString("name"); if (name.isBlank()) continue
        val argsStr = fn.optString("arguments", "{}")
        val args = try { JSONObject(if (argsStr.isBlank() || argsStr == "null") "{}" else argsStr) } catch (_: Exception) { JSONObject() }
        calls.add(AIToolCall(name, args, tc.toString(), tc.optString("id").ifBlank { null }))
      }
    }
    if (calls.isEmpty() && text.isNotBlank()) { val parsed = ToolCallProtocol.parse(text); if (parsed != null) calls.add(parsed) }
    return ToolAwareResponse(text = text ?: "", toolCalls = calls)
  }

  override fun writeFile(filePath: String, content: String): FileWriteResult { val writer = fileWriter ?: return FileWriteResult.Error("File writer not initialized"); return writer.writeFile(filePath, content, createBackup = true) }
}
data class ConversationMessage(val role: String, val content: String)
