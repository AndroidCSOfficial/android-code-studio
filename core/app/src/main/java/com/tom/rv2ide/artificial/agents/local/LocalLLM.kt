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

package com.tom.rv2ide.artificial.agents.local

import android.content.Context
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.artificial.agents.AIAgent
import com.tom.rv2ide.artificial.agents.AIAgentRegistry
import com.tom.rv2ide.artificial.agents.Agents
import com.tom.rv2ide.artificial.agents.ModificationAttempt
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.mcp.McpServerStore
import com.tom.rv2ide.artificial.mcp.McpToolExecutor
import com.tom.rv2ide.artificial.profiles.AgentPromptProfileStore
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

class LocalLLM : AIAgent {
  private var baseUrl: String? = null
  private var modelName: String? = null
  private val httpClient = OkHttpClient.Builder().connectTimeout(60, TimeUnit.SECONDS).readTimeout(120, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
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
  override val providerId = "localllm"
  override val providerName = "Local LLM"

  companion object {
    fun registerAgent() {
      AIAgentRegistry.register("localllm", object : AIAgentRegistry.AgentFactory {
        override fun create(context: Context): AIAgent = LocalLLM()
        override fun hasValidApiKey(): Boolean { val prefs = BaseApplication.getBaseInstance().prefManager; val url = prefs.getString("local_llm_base_url", null); val model = prefs.getString("local_llm_model_name", null); return !url.isNullOrEmpty() && !model.isNullOrEmpty() }
        override fun getApiKey(): String? = "local"
      })
    }
  }

  override fun initialize(apiKey: String, context: Context) {
    appContext = context.applicationContext
    val prefs = BaseApplication.getBaseInstance().prefManager
    baseUrl = prefs.getString("local_llm_base_url", null); modelName = prefs.getString("local_llm_model_name", null)
    if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) throw IllegalStateException("Local LLM not configured. Please set base URL and model name.")
  }
  override fun isInitialized(): Boolean = !baseUrl.isNullOrEmpty() && !modelName.isNullOrEmpty()
  override fun reinitializeWithNewModel(apiKey: String, context: Context) { initialize(apiKey, context) }
  override fun setContext(context: Context) { appContext = context.applicationContext; fileWriter = AIFileWriter(context) }
  override fun setProjectData(projectTreeResult: ProjectTreeResult) { this.projectTreeResult = projectTreeResult }
  override fun clearConversation() { conversationHistory.clear(); modificationHistory.clear(); currentAttemptCount = 0; clearNative() }
  override fun recordModification(filePath: String, oldContent: String?, newContent: String, success: Boolean) { modificationHistory.add(ModificationAttempt(System.currentTimeMillis(), filePath, oldContent, newContent, currentAttemptCount, success)) }
  override fun undoLastModification(): Boolean { if (modificationHistory.isEmpty()) return false; val lastMod = modificationHistory.lastOrNull { it.success } ?: return false; if (lastMod.previousContent != null) { val result = writeFile(lastMod.filePath, lastMod.previousContent); if (result is FileWriteResult.Success) { modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod)); return true } } else { try { File(lastMod.filePath).delete(); modificationHistory.removeAt(modificationHistory.lastIndexOf(lastMod)); return true } catch (_: Exception) {} }; return false }
  override fun getModificationHistory(): List<ModificationAttempt> = modificationHistory.toList()
  override fun resetAttemptCount() { currentAttemptCount = 0 }
  override fun incrementAttemptCount() { currentAttemptCount++ }
  override fun getCurrentAttemptCount(): Int = currentAttemptCount
  override fun canRetry(): Boolean = currentAttemptCount < maxRetryAttempts

  private fun isUserRequestingCorrection(message: String): Boolean = listOf("wrong","not what","mistake","error","incorrect","that's not","not right","fix","undo","revert","different","try again","not working").any { message.lowercase().contains(it) }
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
    val fileContents = readRelevantFiles(); val needsCorrection = isUserRequestingCorrection(prompt)
    return buildString {
      append("=== PROJECT STRUCTURE (THESE ARE THE EXACT PATHS YOU MUST USE) ===\n")
      if (projectTreeResult != null) { append(projectTreeResult!!.tree); append("\n\nCRITICAL: Use ONLY the paths shown above.\n\n") }
      if (fileContents.isNotEmpty()) { append("=== CURRENT FILES CONTENT ===\n"); fileContents.forEach { (p,c)-> append("FILE: $p\nCONTENT:\n").append(c).append("\n\n") } }
      if (context != null) append("=== ADDITIONAL CONTEXT ===\n").append(context).append("\n\n")
      if (conversationHistory.isNotEmpty()) { append("=== CONVERSATION HISTORY ===\n"); conversationHistory.takeLast(8).forEach { msg -> append("${msg.role.uppercase()}: ${msg.content.take(3000)}\n\n") } }
      if (needsCorrection && modificationHistory.isNotEmpty()) append("=== CORRECTION REQUIRED ===\nTry a COMPLETELY DIFFERENT approach.\n\n")
      if (currentAttemptCount > 0) append("=== RETRY ATTEMPT $currentAttemptCount/$maxRetryAttempts ===\n\n")
      append("=== USER REQUEST ===\n").append(prompt)
    }
  }

  override suspend fun generateCode(prompt: String, context: String?, language: String, projectStructure: String?): Result<String> = withContext(Dispatchers.IO) {
    try { val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context")); clearNative(); val res = callChat(buildMessages(composeSystemPrompt(ctx,true), buildUserPrompt(prompt,context)), null); conversationHistory.add(ConversationMessage("user", prompt)); conversationHistory.add(ConversationMessage("assistant", res.text)); while (conversationHistory.size>20) conversationHistory.removeAt(0); Result.success(res.text) } catch (e: Exception) { Result.failure(e) }
  }

  override fun supportsNativeTools(): Boolean = true
  override suspend fun generateWithTools(prompt: String, context: String?, language: String, projectStructure: String?, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try { val ctx = appContext ?: return@withContext Result.failure(IllegalStateException("No context")); clearNative(); nativeToolsActive=true; nativeMessages.put(JSONObject().put("role","system").put("content", composeSystemPrompt(ctx,false))); nativeMessages.put(JSONObject().put("role","user").put("content", buildUserPrompt(prompt,context))); val res = callChat(nativeMessages, tools); nativeMessages.put(if (res.hasToolCalls()) assistantToolCallMessage(res) else JSONObject().put("role","assistant").put("content",res.text)); Result.success(res) } catch (e: Exception) { Result.failure(e) }
  }
  override suspend fun submitToolResults(toolResults: List<AIToolResult>, tools: List<AIToolDef>): Result<ToolAwareResponse> = withContext(Dispatchers.IO) {
    try { if (!nativeToolsActive) return@withContext generateWithTools(toolResults.joinToString("\n"){it.result}, null, "kotlin", null, tools); toolResults.forEach { tr -> nativeMessages.put(JSONObject().put("role","tool").put("tool_call_id", tr.toolCallId ?: tr.name).put("content", tr.result)) }; val res = callChat(nativeMessages, tools); nativeMessages.put(if (res.hasToolCalls()) assistantToolCallMessage(res) else JSONObject().put("role","assistant").put("content",res.text)); Result.success(res) } catch (e: Exception) { Result.failure(e) }
  }

  private fun assistantToolCallMessage(res: ToolAwareResponse): JSONObject = JSONObject().apply { put("role","assistant"); put("content",res.text); val arr = JSONArray(); res.toolCalls.forEach { tc -> arr.put(JSONObject().apply { put("id", tc.id ?: "call_${tc.name}_${System.nanoTime()}"); put("type","function"); put("function", JSONObject().put("name",tc.name).put("arguments",tc.arguments.toString())) }) }; put("tool_calls",arr) }
  private fun clearNative() { nativeToolsActive=false; while(nativeMessages.length()>0) nativeMessages.remove(0) }
  private fun buildMessages(system: String, user: String): JSONArray = JSONArray().apply { put(JSONObject().put("role","system").put("content",system)); put(JSONObject().put("role","user").put("content",user)) }
  private fun callChat(messages: JSONArray, tools: List<AIToolDef>?): ToolAwareResponse {
    if (baseUrl.isNullOrEmpty() || modelName.isNullOrEmpty()) throw IllegalStateException("Local LLM not configured")
    val requestBody = JSONObject().apply { put("model", modelName); put("messages", messages); put("temperature", 0.7); put("stream", false); if (!tools.isNullOrEmpty()) { put("tools", ToolCallProtocol.openAIToolsJson(tools)); put("tool_choice","auto") } }
    val req = Request.Builder().url("${baseUrl!!.trimEnd('/')}/v1/chat/completions").post(requestBody.toString().toRequestBody("application/json".toMediaType())).addHeader("Content-Type","application/json").build()
    val response = httpClient.newCall(req).execute()
    if (!response.isSuccessful) throw Exception("Local LLM error ${response.code}: ${response.body?.string() ?: "Unknown"}")
    val body = response.body?.string().orEmpty(); val json = JSONObject(body); val msg = json.getJSONArray("choices").getJSONObject(0).getJSONObject("message")
    val text = msg.optString("content",""); val calls = mutableListOf<AIToolCall>()
    val arr = msg.optJSONArray("tool_calls")
    if (arr != null) for (i in 0 until arr.length()) { val tc = arr.optJSONObject(i) ?: continue; val fn = tc.optJSONObject("function") ?: continue; val n = fn.optString("name"); if (n.isBlank()) continue; val a = try { JSONObject(fn.optString("arguments","{}").ifBlank{"{}"}) } catch (_: Exception){ JSONObject() }; calls.add(AIToolCall(n,a,tc.toString(), tc.optString("id").ifBlank{null})) }
    if (calls.isEmpty()) { ToolCallProtocol.parse(text)?.let { calls.add(it) } }
    return ToolAwareResponse(text, calls)
  }
  private fun readRelevantFiles(): Map<String, String> { val filesContent = mutableMapOf<String, String>(); val tree = projectTreeResult?.tree ?: return filesContent; tree.lines().filter { it.isNotBlank() }.forEach { filePath -> val t=filePath.trim(); val f=File(t); if (f.isFile && (t.endsWith(".kt")||t.endsWith(".java")||t.endsWith(".xml")||t.endsWith(".gradle")||t.endsWith(".gradle.kts")) && !t.contains("/build/") && !t.contains("/.gradle/")) try { filesContent[t]=f.readText() } catch (_: Exception) {} }; return filesContent }
  override fun writeFile(filePath: String, content: String): FileWriteResult = fileWriter?.writeFile(filePath, content, createBackup=true) ?: FileWriteResult.Error("File writer not initialized")
}
data class ConversationMessage(val role: String, val content: String)
