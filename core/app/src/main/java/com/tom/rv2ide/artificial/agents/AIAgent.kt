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
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import com.tom.rv2ide.artificial.file.FileWriteResult
import com.tom.rv2ide.artificial.tools.AIToolDef
import com.tom.rv2ide.artificial.tools.AIToolResult
import com.tom.rv2ide.artificial.tools.ToolAwareResponse
import com.tom.rv2ide.artificial.tools.ToolCallProtocol

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
*/

interface AIAgent {
    val providerId: String
    val providerName: String
    
    fun initialize(apiKey: String, context: Context)
    fun reinitializeWithNewModel(apiKey: String, context: Context)
    fun setContext(context: Context)
    fun setProjectData(projectTreeResult: ProjectTreeResult)
    fun clearConversation()
    
    suspend fun generateCode(
        prompt: String,
        context: String?,
        language: String,
        projectStructure: String?
    ): Result<String>
    
    fun recordModification(filePath: String, oldContent: String?, newContent: String, success: Boolean)
    fun undoLastModification(): Boolean
    fun getModificationHistory(): List<ModificationAttempt>
    
    fun resetAttemptCount()
    fun incrementAttemptCount()
    fun getCurrentAttemptCount(): Int
    fun canRetry(): Boolean
    
    fun writeFile(filePath: String, content: String): FileWriteResult
    fun isInitialized(): Boolean

    /** Native tool/function-calling support (optional). */
    fun supportsNativeTools(): Boolean = false

    suspend fun generateWithTools(
        prompt: String,
        context: String?,
        language: String,
        projectStructure: String?,
        tools: List<AIToolDef>
    ): Result<ToolAwareResponse> {
        return generateCode(prompt, context, language, projectStructure).map { text ->
            val tc = ToolCallProtocol.parse(text)
            if (tc != null) ToolAwareResponse("", listOf(tc)) else ToolAwareResponse(text)
        }
    }

    suspend fun submitToolResults(
        toolResults: List<AIToolResult>,
        tools: List<AIToolDef>
    ): Result<ToolAwareResponse> {
        val prompt = buildString {
            append("=== TOOL RESULTS ===\n")
            toolResults.forEach { tr -> append("Tool: ${tr.name}\nResult:\n${tr.result}\n\n") }
            append("Continue. If you need another tool, call it. Otherwise provide the final answer and use FILE_TO_MODIFY for file edits.")
        }
        return generateWithTools(prompt, null, "kotlin", null, tools)
    }
}

data class ModificationAttempt(
    val timestamp: Long,
    val filePath: String,
    val previousContent: String?,
    val newContent: String,
    val attemptNumber: Int = 0,
    val success: Boolean = false
)
