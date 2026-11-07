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

package com.tom.rv2ide.artificial.agents

import android.content.Context
import com.tom.rv2ide.artificial.agents.google.Gemini
import com.tom.rv2ide.artificial.agents.openai.OpenAI
import com.tom.rv2ide.artificial.agents.anthropic.Anthropic
import com.tom.rv2ide.artificial.agents.grok.Grok
import com.tom.rv2ide.artificial.agents.deepseek.DeepSeek
import com.tom.rv2ide.artificial.agents.local.LocalLLM
import com.tom.rv2ide.artificial.agents.custom.CustomProvider
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.mcp.McpServerStore
import com.tom.rv2ide.artificial.mcp.McpToolExecutor
import com.tom.rv2ide.artificial.parser.SnippetParser
import com.tom.rv2ide.artificial.permissions.AIPermissionManager
import com.tom.rv2ide.artificial.profiles.AgentPromptProfileStore
import com.tom.rv2ide.artificial.project.awareness.ProjectData
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.tools.BuiltInToolExecutor
import com.tom.rv2ide.artificial.tools.ToolCallProtocol
import java.io.File
import kotlinx.coroutines.delay
import com.tom.rv2ide.artificial.dialogs.ProviderSwitchDialog

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class AIAgentManager(private val context: Context) {

    private val snippetParser = SnippetParser()
    private val permissionManager = AIPermissionManager(context)
    private var currentProjectRoot: File? = null
    private var currentProjectTree: ProjectTreeResult? = null
    private var currentProviderId: String = "gemini"
    private var currentAgent: AIAgent? = null
    private val providerSwitchDialog = ProviderSwitchDialog(context)

    init {
        Gemini.registerAgent()
        OpenAI.registerAgent()
        Anthropic.registerAgent()
        Grok.registerAgent()
        DeepSeek.registerAgent()
        LocalLLM.registerAgent()
        CustomProvider.registerAgent()
        
        permissionManager.setFileWriteEnabled(true)
        permissionManager.setRequireConfirmation(false)
        
        setProvider(currentProviderId)
    }
    
    fun getCurrentAgent(): AIAgent? = currentAgent

    fun setProvider(providerId: String): Boolean {
        android.util.Log.d("AIAgentManager", "setProvider called with: $providerId")
        
        val factory = AIAgentRegistry.getFactory(providerId)
        if (factory == null) {
            android.util.Log.e("AIAgentManager", "No factory found for provider: $providerId")
            return false
        }
        
        if (!factory.hasValidApiKey()) {
            android.util.Log.e("AIAgentManager", "No valid API key for provider: $providerId")
            return false
        }
        
        currentProviderId = providerId
        currentAgent = factory.create(context)
        android.util.Log.d("AIAgentManager", "Agent created: ${currentAgent != null}")
        
        factory.getApiKey()?.let { apiKey ->
            android.util.Log.d("AIAgentManager", "Initializing agent with API key")
            currentAgent?.initialize(apiKey, context)
            currentAgent?.setContext(context)
            currentProjectTree?.let { currentAgent?.setProjectData(it) }
            android.util.Log.d("AIAgentManager", "Agent initialized: ${currentAgent?.isInitialized()}")
        }
        
        return currentAgent?.isInitialized() ?: false
    }

    fun getCurrentProviderId(): String = currentProviderId
    fun getCurrentProviderName(): String = currentAgent?.providerName ?: "Unknown"
    
    fun getAvailableProviders(): List<ProviderInfo> {
        return AIAgentRegistry.getAvailableProviders().mapNotNull { providerId ->
            val factory = AIAgentRegistry.getFactory(providerId)
            val agent = factory?.create(context)
            agent?.let { ProviderInfo(id = it.providerId, name = it.providerName, isAvailable = factory.hasValidApiKey()) }
        }
    }

    fun setProjectRoot(projectPath: String): Boolean {
        val projectRoot = File(projectPath)
        if (!projectRoot.exists()) return false
        currentProjectRoot = projectRoot
        val projectData = ProjectData(context)
        val projectTree = projectData.showProjectTree(projectRoot)
        currentProjectTree = projectTree
        currentAgent?.setProjectData(projectTree)
        permissionManager.addAllowedDirectory(projectRoot.absolutePath)
        return true
    }

    fun clearConversation() { currentAgent?.clearConversation() }

    private fun executeToolCall(name: String, args: org.json.JSONObject): String {
        val profile = AgentPromptProfileStore.getActive()
        if (name.startsWith("mcp__")) {
            if (profile?.enableMcpTools == false) return "Error: MCP tools disabled by active agent profile"
            val servers = McpServerStore.load().filter { s ->
                profile?.enabledMcpServerIds.isNullOrEmpty() || profile!!.enabledMcpServerIds.contains(s.id)
            }
            return McpToolExecutor(servers).execute(name, args)
        }
        if (profile?.enableTools == false) return "Error: built-in tools disabled by active agent profile"
        val builtIn = BuiltInToolExecutor(context, currentProjectTree, currentProjectRoot, profile?.allowCommandExecution == true)
        return builtIn.execute(name, args)
    }

    suspend fun executeRequest(userRequest: String, callback: AIAgentCallback) {
        var success = false
        var providerSwitched = false
        currentAgent?.resetAttemptCount()
        callback.onProcessing("Analyzing your request...")

        while (!success && (currentAgent?.canRetry() == true)) {
            try {
                val currentAttempt = currentAgent?.getCurrentAttemptCount() ?: 0
                if (currentAttempt > 0 && !providerSwitched) {
                    callback.onRetry(currentAttempt, "Thinking differently...")
                    delay(1000)
                }

                val previousFileStates = captureCurrentFileStates()
                var promptForModel = userRequest
                var toolIterations = 0
                val maxToolIterations = 6
                var finalResponse: String? = null

                while (toolIterations < maxToolIterations) {
                    val result = currentAgent?.generateCode(promptForModel, null, "kotlin", null)
                        ?: Result.failure(Exception("No agent initialized"))

                    val response = result.getOrElse { throw it }
                    val toolCall = ToolCallProtocol.parse(response)
                    if (toolCall != null) {
                        toolIterations++
                        callback.onProcessing("Using tool: ${toolCall.name} (${toolIterations}/$maxToolIterations)")
                        val toolResult = try { executeToolCall(toolCall.name, toolCall.arguments) } catch (e: Exception) { "Error: ${e.message}" }
                        val clipped = if (toolResult.length > 8000) toolResult.take(8000) + "\n...[truncated]" else toolResult
                        promptForModel = "TOOL_RESULT for ${toolCall.name}:\n$clipped\n\nContinue with the user's request. If you need another tool, emit TOOL_CALL. Otherwise provide the final answer and use FILE_TO_MODIFY for any file edits."
                        continue
                    }
                    finalResponse = response
                    break
                }

                val response = finalResponse ?: "Tool iteration limit reached. Please try a more specific request."
                if (response.contains("FILE_TO_MODIFY:")) {
                    callback.onProcessing("Modifying files...")
                    val modifications = processModifications(response, previousFileStates, callback)
                    if (modifications.isNotEmpty()) {
                        val allSuccessful = modifications.all { it.writeResult is FileWriteResult.Success }
                        if (allSuccessful) {
                            val results = modifications.map { mod ->
                                val isNewFile = !previousFileStates.containsKey(mod.filePath)
                                ModificationResult(mod.filePath, mod.content, true, "Modified successfully", isNewFile)
                            }
                            callback.onSuccess(response, results, createSummary(results))
                            success = true
                        } else {
                            callback.onProcessing("Some files failed. Retrying...")
                            currentAgent?.incrementAttemptCount()
                            delay(1500)
                        }
                    } else {
                        callback.onProcessing("No files were modified. Retrying...")
                        currentAgent?.incrementAttemptCount()
                        delay(1500)
                    }
                } else {
                    callback.onTextResponse(response, ModificationSummary(0, 0, 0, 0, 0, emptyList()))
                    success = true
                }
            } catch (error: Exception) {
                android.util.Log.e("AIAgentManager", "Error occurred: ${error.message}", error)
                val shouldSwitchProvider = error is com.tom.rv2ide.artificial.exceptions.RateLimitException ||
                        error is com.tom.rv2ide.artificial.exceptions.QuotaExceededException ||
                        error is com.tom.rv2ide.artificial.exceptions.InsufficientBalanceException ||
                        error is com.tom.rv2ide.artificial.exceptions.InvalidApiKeyException
                if (shouldSwitchProvider && !providerSwitched) {
                    if (providerSwitchDialog.isAutoSwitchEnabled()) {
                        val alternativeProvider = getAlternativeProvider()
                        if (alternativeProvider != null && setProvider(alternativeProvider)) {
                            providerSwitched = true
                            currentAgent?.resetAttemptCount()
                            callback.onProcessing("✅ Switched to ${currentAgent?.providerName ?: "Unknown"}")
                        } else {
                            callback.onError("${formatErrorMessage(error)}\n\n❌ Failed to switch providers.")
                            success = true
                        }
                    } else {
                        callback.onError("PROVIDER_SWITCH_REQUIRED::${formatErrorMessage(error)}")
                        success = true
                    }
                } else if ((currentAgent?.canRetry() == true) && !providerSwitched) {
                    callback.onRetry(currentAgent?.getCurrentAttemptCount() ?: 0, "Error: ${error.message?.take(50) ?: "Unknown error"}. Retrying...")
                    currentAgent?.incrementAttemptCount()
                    delay(1500)
                } else {
                    callback.onError(formatErrorMessage(error))
                    success = true
                }
            }
        }

        if (!success) {
            callback.onError("Failed after ${currentAgent?.getCurrentAttemptCount() ?: 0} attempts with ${currentAgent?.providerName ?: "No agent initialized"}.\n\nPlease check your configuration and try again.")
            undoLastModification()
        }
    }

    private fun getAlternativeProvider(): String? = AIAgentRegistry.getAvailableProviders().firstOrNull { it != currentProviderId }

    private suspend fun processModifications(
        response: String,
        previousFileStates: Map<String, String>,
        callback: AIAgentCallback
    ): List<BaseFileModification> {
        val modifications = mutableListOf<BaseFileModification>()
        val parser = SnippetParser()
        if (response.contains("FILE_TO_MODIFY:")) {
            val lines = response.lines()
            var currentFile: String? = null
            val contentBuilder = StringBuilder()
            var inContent = false
            fun flush(file: String?, content: StringBuilder) {
                if (file != null && content.isNotEmpty()) {
                    val fileName = File(file).name
                    callback.onFileModifying(file, fileName)
                    val cleanedContent = parser.cleanFileContent(content.toString().trim())
                    val previousContent = previousFileStates[file]
                    val writeResult = currentAgent?.writeFile(file, cleanedContent) ?: FileWriteResult.Error("No agent initialized")
                    val ok = writeResult is FileWriteResult.Success
                    currentAgent?.recordModification(file, previousContent, cleanedContent, ok)
                    callback.onFileModified(file, fileName, ok)
                    modifications.add(BaseFileModification(file, cleanedContent, writeResult))
                }
            }
            for (line in lines) {
                if (line.startsWith("FILE_TO_MODIFY:")) {
                    flush(currentFile, contentBuilder)
                    currentFile = line.substringAfter("FILE_TO_MODIFY:").trim()
                    contentBuilder.clear()
                    inContent = true
                } else if (inContent) contentBuilder.append(line).append("\n")
            }
            flush(currentFile, contentBuilder)
        }
        return modifications
    }

    private fun formatErrorMessage(error: Throwable): String {
        val errorMessage = error.message ?: "Unknown error occurred"
        val providerName = currentAgent?.providerName ?: "Unknown"
        return when (error) {
            is com.tom.rv2ide.artificial.exceptions.RateLimitException -> "⚠️ RATE LIMIT EXCEEDED\n\n$errorMessage"
            is com.tom.rv2ide.artificial.exceptions.QuotaExceededException -> "⚠️ QUOTA EXCEEDED\n\n$errorMessage"
            is com.tom.rv2ide.artificial.exceptions.InsufficientBalanceException -> "💳 INSUFFICIENT BALANCE\n\nProvider: $providerName\n\n$errorMessage"
            is com.tom.rv2ide.artificial.exceptions.InvalidApiKeyException -> "❌ INVALID API KEY\n\n$errorMessage"
            is java.net.UnknownHostException -> "🌐 NETWORK ERROR\n\n$errorMessage"
            is java.net.SocketTimeoutException -> "⏱️ TIMEOUT ERROR\n\n$errorMessage"
            is org.json.JSONException -> "📄 JSON PARSING ERROR\n\n$errorMessage"
            else -> "❌ ERROR OCCURRED\n\nProvider: $providerName\nError: ${error.javaClass.simpleName}\n\n$errorMessage"
        }
    }

    fun showProviderErrorDialogFromFragment(activity: android.app.Activity, errorMessage: String, onProviderSelected: (String) -> Unit) {
        val currentProviderName = currentAgent?.providerName ?: "Unknown"
        val availableProviders = getAvailableProviders().filter { it.id != currentProviderId && it.isAvailable }.map { Pair(it.id, it.name) }
        providerSwitchDialog.showProviderErrorDialog(currentProviderName, errorMessage, availableProviders,
            onProviderSelected = { providerId ->
                setProvider(providerId)
                val agents = Agents(context)
                val availableModels = agents.getModelsForProvider(providerId)
                if (availableModels.isNotEmpty()) agents.setAgent(availableModels[0])
                reinitializeWithSelectedModel()
                onProviderSelected(providerId)
            },
            onEnableAutoSwitch = {
                getAlternativeProvider()?.let {
                    setProvider(it)
                    val agents = Agents(context)
                    val availableModels = agents.getModelsForProvider(it)
                    if (availableModels.isNotEmpty()) agents.setAgent(availableModels[0])
                    reinitializeWithSelectedModel()
                }
            }
        )
    }
    
    fun isAutoSwitchEnabled(): Boolean = providerSwitchDialog.isAutoSwitchEnabled()
    fun setAutoSwitch(enabled: Boolean) = providerSwitchDialog.setAutoSwitch(enabled)

    private fun createSummary(results: List<ModificationResult>): ModificationSummary {
        val successful = results.count { it.success }
        val failed = results.count { !it.success }
        val newFiles = results.count { it.isNewFile }
        val modifiedFiles = results.count { !it.isNewFile }
        val fileDetails = results.map { FileDetail(File(it.filePath).name, it.filePath, if (it.success) FileStatus.SUCCESS else FileStatus.FAILED, if (it.isNewFile) ChangeType.CREATED else ChangeType.MODIFIED) }
        return ModificationSummary(results.size, successful, failed, newFiles, modifiedFiles, fileDetails)
    }

    private fun captureCurrentFileStates(): Map<String, String> {
        val states = mutableMapOf<String, String>()
        val projectRoot = currentProjectRoot ?: return states
        if (!projectRoot.exists()) return states
        projectRoot.walkTopDown().filter { it.isFile }.filter { it.extension in listOf("kt","java","xml","gradle","kts") && !it.path.contains("/build/") && !it.path.contains("/.gradle/") }.forEach { file ->
            try { states[file.absolutePath] = file.readText() } catch (_: Exception) {}
        }
        return states
    }

    fun undoLastModification(): Boolean = currentAgent?.undoLastModification() ?: false
    fun reinitializeWithSelectedModel() { AIAgentRegistry.getFactory(currentProviderId)?.getApiKey()?.let { currentAgent?.reinitializeWithNewModel(it, context) } }
    fun getCurrentModelName(): String = Agents(context).getAgent()
    fun getConversationHistory(): List<UnifiedModificationAttempt> = currentAgent?.getModificationHistory()?.map { UnifiedModificationAttempt(it.timestamp, it.filePath, it.previousContent, it.newContent, it.attemptNumber, it.success) } ?: emptyList()

    interface AIAgentCallback {
        fun onProcessing(message: String)
        fun onFileModifying(filePath: String, fileName: String)
        fun onFileModified(filePath: String, fileName: String, success: Boolean)
        fun onSuccess(response: String, modifications: List<ModificationResult>, summary: ModificationSummary)
        fun onTextResponse(response: String, summary: ModificationSummary)
        fun onError(message: String)
        fun onRetry(attemptNumber: Int, message: String)
    }

    data class ModificationResult(val filePath: String, val content: String, val success: Boolean, val message: String, val isNewFile: Boolean = false)
    data class ModificationSummary(val totalFiles: Int, val successfulFiles: Int, val failedFiles: Int, val newFiles: Int, val modifiedFiles: Int, val fileDetails: List<FileDetail>)
    data class FileDetail(val fileName: String, val filePath: String, val status: FileStatus, val changeType: ChangeType)
    enum class FileStatus { SUCCESS, FAILED }
    enum class ChangeType { CREATED, MODIFIED }
    data class ProviderInfo(val id: String, val name: String, val isAvailable: Boolean)
}

data class BaseFileModification(val filePath: String, val content: String, val writeResult: FileWriteResult)
data class UnifiedModificationAttempt(val timestamp: Long, val filePath: String, val previousContent: String?, val newContent: String, val attemptNumber: Int = 0, val success: Boolean = false)
