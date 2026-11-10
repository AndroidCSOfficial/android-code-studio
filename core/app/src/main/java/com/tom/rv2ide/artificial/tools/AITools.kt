package com.tom.rv2ide.artificial.tools

import android.content.Context
import com.tom.rv2ide.artificial.file.AIFileWriter
import com.tom.rv2ide.artificial.project.awareness.ProjectTreeResult
import java.io.File
import java.util.concurrent.TimeUnit
import org.json.JSONObject

data class AIToolDef(
    val name: String,
    val description: String,
    val parametersJsonSchema: String,
)

data class AIToolCall(val name: String, val arguments: JSONObject, val raw: String = "", val id: String? = null)
data class AIToolResult(val name: String, val result: String, val toolCallId: String? = null)
data class ToolAwareResponse(val text: String = "", val toolCalls: List<AIToolCall> = emptyList()) {
    fun hasToolCalls() = toolCalls.isNotEmpty()
}

interface AIToolExecutor {
    fun definitions(): List<AIToolDef>
    fun execute(name: String, arguments: JSONObject): String
}

class BuiltInToolExecutor(
    private val context: Context,
    private var projectTree: ProjectTreeResult? = null,
    private var projectRoot: File? = null,
    private val allowCommandExecution: Boolean = false,
    private val disabledTools: Set<String> = emptySet(),
) : AIToolExecutor {
    private val writer = AIFileWriter(context)

    fun updateProject(tree: ProjectTreeResult?, root: File?) {
        projectTree = tree
        projectRoot = root
    }

    override fun definitions(): List<AIToolDef> {
        val defs = mutableListOf(
            AIToolDef("read_file", "Read a UTF-8 text file from the project. Use exact paths.", """{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}"""),
            AIToolDef("list_files", "List files under a directory path (non-recursive).", """{"type":"object","properties":{"path":{"type":"string"}},"required":["path"]}"""),
            AIToolDef("search_project", "Search for text in project source files. Returns matching path:line previews.", """{"type":"object","properties":{"query":{"type":"string"},"maxResults":{"type":"integer"}},"required":["query"]}"""),
            AIToolDef("get_project_tree", "Get the project file tree/structure.", """{"type":"object","properties":{}}"""),
            AIToolDef("modify_file", "Create or overwrite a project text file. Prefer FILE_TO_MODIFY blocks for normal edits; this tool is for targeted agentic operations.", """{"type":"object","properties":{"path":{"type":"string"},"content":{"type":"string"}},"required":["path","content"]}"""),
        )
        if (allowCommandExecution) {
            defs.add(AIToolDef("run_command", "Run a shell command in the project directory (opt-in, use with caution). Returns stdout/stderr and exit code.", """{"type":"object","properties":{"command":{"type":"string"},"timeoutMs":{"type":"integer"}},"required":["command"]}"""))
        }
        return defs.filterNot { it.name in disabledTools }
    }

    override fun execute(name: String, arguments: JSONObject): String {
        if (name in disabledTools) return "Error: tool '$name' is disabled by the active agent profile."
        if (name == "run_command" && !allowCommandExecution) return "Error: command execution is disabled by the active agent profile."
        return try {
            when (name) {
                "read_file" -> {
                    val path = arguments.optString("path")
                    if (path.isBlank()) return "Error: path is required"
                    val f = File(path)
                    if (!f.exists() || !f.isFile) return "Error: file not found: $path"
                    val text = f.readText()
                    if (text.length > 12000) text.take(12000) + "\n...[truncated]..." else text
                }
                "list_files" -> {
                    val path = arguments.optString("path")
                    val f = File(path)
                    if (!f.exists() || !f.isDirectory) return "Error: directory not found: $path"
                    f.listFiles()?.sortedBy { it.name }?.joinToString("\n") { if (it.isDirectory) "${it.name}/" else it.name } ?: ""
                }
                "search_project" -> {
                    val query = arguments.optString("query")
                    if (query.isBlank()) return "Error: query is required"
                    val max = arguments.optInt("maxResults", 30).coerceIn(1, 100)
                    val root = projectRoot ?: return "Error: no project root"
                    val results = mutableListOf<String>()
                    root.walkTopDown()
                        .filter { it.isFile && it.extension in listOf("kt","java","xml","gradle","kts","json","md","properties") }
                        .filter { !it.path.contains("/build/") && !it.path.contains("/.gradle/") && !it.path.contains("/.git/") }
                        .forEach { file ->
                            if (results.size >= max) return@forEach
                            try { file.readLines().forEachIndexed { idx, line ->
                                if (results.size >= max) return@forEachIndexed
                                if (line.contains(query, ignoreCase = true)) results.add("${file.absolutePath}:${idx + 1}: ${line.trim().take(200)}")
                            } } catch (_: Exception) {}
                        }
                    if (results.isEmpty()) "No matches." else results.joinToString("\n")
                }
                "get_project_tree" -> projectTree?.tree ?: "No project tree available."
                "modify_file" -> {
                    val path = arguments.optString("path")
                    val content = arguments.optString("content")
                    if (path.isBlank()) return "Error: path is required"
                    when (val r = writer.writeFile(path, content, createBackup = true)) {
                        is com.tom.rv2ide.artificial.file.FileWriteResult.Success -> "OK: wrote ${r.path}"
                        is com.tom.rv2ide.artificial.file.FileWriteResult.Error -> "Error: ${r.message}"
                        else -> "Error: failed to write file"
                    }
                }
                "run_command" -> {
                    val command = arguments.optString("command")
                    if (command.isBlank()) return "Error: command is required"
                    val timeoutMs = arguments.optLong("timeoutMs", 15000L).coerceIn(1000L, 60000L)
                    val workdir = projectRoot ?: File(context.filesDir.path)
                    val proc = ProcessBuilder("sh", "-c", command).directory(workdir).redirectErrorStream(true).start()
                    val finished = proc.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
                    if (!finished) { proc.destroyForcibly(); return "Error: command timed out after ${timeoutMs}ms" }
                    val out = proc.inputStream.bufferedReader().readText()
                    val clipped = if (out.length > 8000) out.take(8000) + "\n...[truncated]" else out
                    "exit=${proc.exitValue()}\n$clipped"
                }
                else -> "Error: unknown tool '$name'"
            }
        } catch (e: Exception) { "Error: ${e.message}" }
    }
}

object ToolCallProtocol {
    private val patterns = listOf(
        Regex("""TOOL_CALL:\s*(\{.*\})""", RegexOption.DOT_MATCHES_ALL),
        Regex("""```tool_call\s*\n(\{.*?\})\s*\n```""", RegexOption.DOT_MATCHES_ALL),
        Regex("""```json\s*\n(\{\s*"(?:name|tool)"\s*:.*?\})\s*\n```""", RegexOption.DOT_MATCHES_ALL),
    )

    fun parse(response: String): AIToolCall? {
        for (re in patterns) {
            val m = re.find(response) ?: continue
            val jsonText = m.groupValues[1].trim()
            try {
                val obj = JSONObject(jsonText)
                val name = obj.optString("name").ifBlank { obj.optString("tool") }
                if (name.isBlank()) continue
                val args = obj.optJSONObject("arguments") ?: obj.optJSONObject("args") ?: try { JSONObject(obj.optString("arguments", "{}")) } catch (_: Exception) { JSONObject() }
                return AIToolCall(name, args ?: JSONObject(), m.value, obj.optString("id").ifBlank { null })
            } catch (_: Exception) {}
        }
        return null
    }

    fun formatDefinitions(tools: List<AIToolDef>): String {
        if (tools.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("\n=== AVAILABLE TOOLS / SKILLS ===\n")
        sb.append("You can use tools to gather information or perform allowed actions.\n")
        sb.append("To call a tool, output ONLY:\nTOOL_CALL: {\"name\":\"tool_name\",\"arguments\":{...}}\n\n")
        sb.append("After TOOL_RESULT, continue. When done, respond normally (use FILE_TO_MODIFY for file edits).\n\n")
        tools.forEach { t -> sb.append("- ${t.name}: ${t.description}\n  parameters: ${t.parametersJsonSchema}\n") }
        sb.append("\n")
        return sb.toString()
    }

    fun openAIToolsJson(tools: List<AIToolDef>): org.json.JSONArray {
        val arr = org.json.JSONArray()
        tools.forEach { t ->
            arr.put(org.json.JSONObject().apply {
                put("type", "function")
                put("function", org.json.JSONObject().apply {
                    put("name", t.name)
                    put("description", t.description)
                    put("parameters", try { org.json.JSONObject(t.parametersJsonSchema) } catch (_: Exception) { org.json.JSONObject().put("type","object").put("properties", org.json.JSONObject()) })
                })
            })
        }
        return arr
    }
}
