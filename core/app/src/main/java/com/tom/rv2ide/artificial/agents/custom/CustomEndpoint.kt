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
import com.tom.rv2ide.artificial.agents.ModificationAttempt
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

class CustomEndpoint : AIAgent {

  private var apiKey: String? = null
  private val writingRules = WritingRules.Instructions()
  private var projectTreeResult: ProjectTreeResult? = null
  private var fileWriter: AIFileWriter? = null
  private val conversationHistory = mutableListOf<ConversationMessage>()
  private val modificationHistory = mutableListOf<ModificationAttempt>()
  private var currentAttemptCount = 0
  private val maxRetryAttempts = 3
  private var agents: Agents? = null
  private var selectedModel: String = "custom-model"
  override val providerId = "custom"
  override val providerName = "Custom Endpoint"

  companion object {
      private val CUSTOM_TOOLS_HINT = "\n[ TOOL CALLS ]\n" +
          "Format eksekusi langsung (hasil balik ke kamu):\n" +
          "mkdir: mkdir path=/absolut/folder\n" +
          "tulis: writefile path=/absolut/f.kt + content\n" +
          "baca: readfile path=/absolut/f.kt\n" +
          "SATU format per aksi. mkdir folder saja, writefile file baru/penuh.\n"

      fun registerAgent() {
          AIAgentRegistry.register("custom", object : AIAgentRegistry.AgentFactory {
              override fun create(context: Context): AIAgent {
                  return CustomEndpoint()
              }
              
              override fun hasValidApiKey(): Boolean {
                  val apiKey = getApiKey()
                  return apiKey != null && apiKey.isNotBlank() && apiKey.length > 20
              }
              
              override fun getApiKey(): String? {
                  val prefManager = com.tom.rv2ide.preferences.internal.prefManager
                  return prefManager.getString("ai_agent_custom_api_key", "")?.takeIf { it.isNotBlank() }
              }
          })
      }
  }
        
  override fun initialize(apiKey: String, context: Context) {
      try {
          this.apiKey = apiKey
          agents = Agents(context)
          // custom: model dibaca dari prefs sendiri (bebas ketik, cth: cbai/deepseek-v4.1-flash)
          var selectedModel = com.tom.rv2ide.preferences.internal.prefManager
              .getString("ai_agent_custom_model", "custom-model").trim()
          if (selectedModel.isBlank()) selectedModel = "custom-model"
          agents?.setAgent(selectedModel)
          agents?.setProvider("custom")
          
          this.selectedModel = selectedModel
          restoreHistory()
      } catch (e: Exception) {
          throw e
      }
  }

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
    try { prefs().putString(histKey(), "") } catch (ignored: Exception) { }
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
                  IllegalStateException("DeepSeek service not initialized")
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

          val response = callDeepSeekAPI(key, fullPrompt)

          if (response.isBlank()) {
            return@withContext Result.failure(Exception("Empty response from AI"))
          }

          // eksekusi tool_call XML (mkdir/writefile) sebelum disimpan ke history
          val toolResults = parseAndExecuteToolCalls(response)
          val storedResponse = if (toolResults.isNotEmpty()) {
              response + "\n\n[TOOL RESULTS]\n" + toolResults.joinToString("\n") { r ->
                  "- ${r.function}: ${if (r.success) "OK" else "FAIL"} - ${r.message}"
              }
          } else response
          conversationHistory.add(ConversationMessage("user", prompt))
          conversationHistory.add(ConversationMessage("assistant", storedResponse))
          persistHistory()

          if (conversationHistory.size > 20) {
            conversationHistory.removeAt(0)
            conversationHistory.removeAt(0)
          }

          Result.success(response)
        } catch (e: Exception) {
          Result.failure(e)
        }
      }

  private fun callDeepSeekAPI(apiKey: String, prompt: String): String {
    android.util.Log.d("CustomEndpoint", "Starting API call to DeepSeek")
    
    val baseUrl = com.tom.rv2ide.preferences.internal.prefManager.getString("ai_agent_custom_base_url", "https://api.openai.com/v1").trim().trimEnd('/')
    val url = URL("$baseUrl/chat/completions")
    val connection = url.openConnection() as HttpURLConnection
    
    try {
      connection.requestMethod = "POST"
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setRequestProperty("Authorization", "Bearer $apiKey")
      connection.doOutput = true
      connection.connectTimeout = 30000
      connection.readTimeout = 30000
      
      val messages = JSONArray()
      
      val systemMessage = JSONObject()
      systemMessage.put("role", "system")
      systemMessage.put("content", writingRules.useThis() + CUSTOM_TOOLS_HINT)
      messages.put(systemMessage)
      
      val userMessage = JSONObject()
      userMessage.put("role", "user")
      userMessage.put("content", prompt)
      messages.put(userMessage)
      
      val requestBody = JSONObject()
      requestBody.put("model", selectedModel)
      requestBody.put("messages", messages)
      requestBody.put("temperature", 0.7)
      requestBody.put("max_tokens", 4096)
      
      android.util.Log.d("CustomEndpoint", "Request body: ${requestBody.toString()}")
      
      connection.outputStream.use { os ->
        os.write(requestBody.toString().toByteArray())
      }
      
      val responseCode = connection.responseCode
      android.util.Log.d("CustomEndpoint", "Response code: $responseCode")
      
      if (responseCode != HttpURLConnection.HTTP_OK) {
        val errorStream = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
        android.util.Log.e("CustomEndpoint", "Error response: $errorStream")
        
        try {
          val errorJson = JSONObject(errorStream)
          val errorObj = errorJson.optJSONObject("error")
          val errorMessage = errorObj?.optString("message") ?: errorStream
          val errorType = errorObj?.optString("type") ?: ""
          val errorCode = errorObj?.optString("code") ?: ""
          
          android.util.Log.e("CustomEndpoint", "Error type: $errorType, code: $errorCode, message: $errorMessage")
          
          when {
            responseCode == 429 || errorType.contains("rate_limit") || errorCode.contains("rate_limit") -> 
              throw RateLimitException("DeepSeek rate limit exceeded: $errorMessage")
            errorType.contains("insufficient_quota") || errorMessage.contains("quota") || errorMessage.contains("billing") -> 
              throw QuotaExceededException("DeepSeek quota exceeded: $errorMessage")
            errorType.contains("invalid_api_key") || errorCode.contains("invalid_api_key") -> 
              throw InvalidApiKeyException("Invalid DeepSeek API key: $errorMessage")
            responseCode == 401 -> 
              throw InvalidApiKeyException("DeepSeek authentication failed: $errorMessage")
            else -> 
              throw Exception("DeepSeek API error ($responseCode) - Type: $errorType, Code: $errorCode, Message: $errorMessage")
          }
        } catch (e: RateLimitException) {
          throw e
        } catch (e: QuotaExceededException) {
          throw e
        } catch (e: InvalidApiKeyException) {
          throw e
        } catch (e: Exception) {
          throw Exception("DeepSeek API error ($responseCode): $errorStream")
        }
      }
      
      val responseBody = connection.inputStream.bufferedReader().readText()
      android.util.Log.d("CustomEndpoint", "Success response received, length: ${responseBody.length}")
      
      val jsonResponse = JSONObject(responseBody)
      
      val choices = jsonResponse.getJSONArray("choices")
      if (choices.length() > 0) {
        val firstChoice = choices.getJSONObject(0)
        val message = firstChoice.getJSONObject("message")
        return message.getString("content")
      }
      
      throw Exception("No response from DeepSeek API")
    } catch (e: RateLimitException) {
      android.util.Log.e("CustomEndpoint", "Rate limit exception", e)
      throw e
    } catch (e: QuotaExceededException) {
      android.util.Log.e("CustomEndpoint", "Quota exceeded exception", e)
      throw e
    } catch (e: InvalidApiKeyException) {
      android.util.Log.e("CustomEndpoint", "Invalid API key exception", e)
      throw e
    } catch (e: java.net.SocketTimeoutException) {
      android.util.Log.e("CustomEndpoint", "Timeout exception", e)
      throw Exception("DeepSeek request timeout: ${e.message}")
    } catch (e: java.net.UnknownHostException) {
      android.util.Log.e("CustomEndpoint", "Network exception", e)
      throw Exception("Network error - cannot reach DeepSeek: ${e.message}")
    } catch (e: Exception) {
      android.util.Log.e("CustomEndpoint", "General exception", e)
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

  override fun isInitialized(): Boolean = apiKey != null



  // ---- persist chat history ke SharedPreferences (max 40 pesan, survive restart) ----
  private fun histKey(): String = "ai_agent_custom_history"

  private fun prefs() = com.tom.rv2ide.preferences.internal.prefManager

  private fun persistHistory() {
      try {
          val take = conversationHistory.takeLast(40)
          val json = org.json.JSONArray()
          for (m in take) {
              json.put(org.json.JSONObject().put("role", m.role).put("content", m.content.take(4000)))
          }
          prefs().putString(histKey(), json.toString())
      } catch (ignored: Exception) { }
  }

  private fun restoreHistory() {
      try {
          val raw = prefs().getString(histKey(), "")
          if (raw.isBlank()) return
          val json = org.json.JSONArray(raw)
          conversationHistory.clear()
          for (i in 0 until minOf(json.length(), 40)) {
              val o = json.getJSONObject(i)
              conversationHistory.add(ConversationMessage(o.optString("role", "user"), o.optString("content", "")))
          }
      } catch (ignored: Exception) { }
  }

  // ---- tool_call XML: <tool_call><function=bash><parameter=mkdir path=.../> + writefile ----
  private val toolCallRegex = Regex("<tool_call>\\s*<function=(\\w+)>(.*?)</function>\\s*</tool_call>", RegexOption.DOT_MATCHES_ALL)
  private val paramRegex = Regex("<parameter=(\\w+)\\s+([^>/]*?)(?:/>|>(.*?)</parameter>)", RegexOption.DOT_MATCHES_ALL)
  private val attrRegex = Regex("(\\w+)=([^\\s]+)")

  data class ToolCallResult(
      val function: String,
      val success: Boolean,
      val message: String
  )

  data class FileModification(
      val filePath: String,
      val content: String,
      val writeResult: FileWriteResult
  )

  data class ConversationMessage(
      val role: String,
      val content: String
  )

  fun parseAndExecuteToolCalls(response: String): List<ToolCallResult> {
      val results = mutableListOf<ToolCallResult>()
      for (m in toolCallRegex.findAll(response)) {
          val fn = m.groupValues[1].lowercase()
          val inner = m.groupValues[2]
          try {
              when (fn) {
                  "bash" -> {
                      val params = parseParams(inner)
                      val cmd = (params["command"] ?: params["cmd"] ?: "").trim()
                      // hanya izinkan mkdir -p (aman, tanpa shell injection)
                      val mk = Regex("mkdir\\s+(?:-p\\s+)?([\\w/.-]+)").find(cmd)
                      if (mk != null) {
                          val dir = java.io.File(mk.groupValues[1])
                          val ok = dir.mkdirs() || dir.isDirectory
                          results.add(ToolCallResult(fn, ok, if (ok) "mkdir ok: ${dir.path}" else "mkdir failed: ${dir.path}"))
                      } else {
                          // dukung <parameter=mkdir path=.../>
                          val p = params["mkdir"] ?: params["path"] ?: ""
                          if (p.isNotBlank()) {
                              val dir = java.io.File(p)
                              val ok = dir.mkdirs() || dir.isDirectory
                              results.add(ToolCallResult(fn, ok, if (ok) "mkdir ok: ${dir.path}" else "mkdir failed: ${dir.path}"))
                          } else {
                              results.add(ToolCallResult(fn, false, "bash: hanya mkdir yang didukung, cmd ditolak"))
                          }
                      }
                  }
                  "readfile", "read_file", "read", "cat" -> {
                      val params = parseParams(inner)
                      val path = (params["path"] ?: params["file"] ?: "").trim()
                      if (path.isBlank()) {
                          results.add(ToolCallResult(fn, false, "readfile: path kosong"))
                      } else {
                          val text = readFile(path)
                          if (text != null) {
                              val preview = if (text.length > 3000) text.take(3000) + "\n...[dipotong, total ${text.length} char]" else text
                              results.add(ToolCallResult(fn, true, "read $path (${text.length} char):\n$preview"))
                          } else {
                              results.add(ToolCallResult(fn, false, "read failed / tidak ada: $path"))
                          }
                      }
                  }
                  "writefile", "write_file", "write" -> {
                      val params = parseParams(inner)
                      val path = (params["path"] ?: params["file"] ?: "").trim()
                      val content = params["content"] ?: params["text"] ?: ""
                      if (path.isBlank()) {
                          results.add(ToolCallResult(fn, false, "writefile: path kosong"))
                      } else {
                          val wr = writeFile(path, content)
                          val ok = wr is FileWriteResult.Success
                          if (ok) recordModification(path, null, content, true)
                          results.add(ToolCallResult(fn, ok, if (ok) "wrote: $path" else "write failed: $path"))
                      }
                  }
                  else -> results.add(ToolCallResult(fn, false, "fungsi tidak didukung: $fn"))
              }
          } catch (e: Exception) {
              results.add(ToolCallResult(fn, false, "error: ${e.message}"))
          }
      }
      return results
  }

  private fun parseParams(inner: String): Map<String, String> {
      val out = mutableMapOf<String, String>()
      for (pm in paramRegex.findAll(inner)) {
          val key = pm.groupValues[1].lowercase()
          val attrStr = pm.groupValues[2]
          val body = pm.groupValues[3]
          val attrs = attrRegex.findAll(attrStr).associate { it.groupValues[1].lowercase() to it.groupValues[2] }
          if (key == "mkdir" && attrs.containsKey("path")) {
              out["mkdir"] = attrs["path"] ?: ""
          } else {
              out[key] = if (body.isNotBlank()) body.trim() else (attrs["value"] ?: attrs.values.firstOrNull() ?: "")
          }
          // atribut path generik (mis. <parameter=command path=...>jarang, tapi tampung)
          if (attrs.containsKey("path") && !out.containsKey("path")) out["path"] = attrs["path"] ?: ""
      }
      return out
  }
}


