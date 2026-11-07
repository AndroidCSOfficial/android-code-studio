package com.tom.rv2ide.artificial.mcp
import com.tom.rv2ide.artificial.tools.AIToolDef

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Minimal MCP client (HTTP JSON-RPC). Intended for remote/HTTP MCP servers.
 * Supports initialize, tools/list, tools/call. Some servers use Streamable HTTP
 * semantics; this implementation uses plain JSON-RPC POST requests for broad
 * compatibility and surfaces errors to the agent.
 */
class McpClient(private val config: McpServerConfig) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    private val nextId = AtomicInteger(1)
    @Volatile private var initialized = false

    fun listTools(): List<AIToolDef> {
        ensureInitialized()
        val res = rpc("tools/list", JSONObject()) ?: return emptyList()
        val tools = res.optJSONObject("result")?.optJSONArray("tools") ?: res.optJSONArray("tools") ?: return emptyList()
        val out = mutableListOf<AIToolDef>()
        for (i in 0 until tools.length()) {
            val t = tools.optJSONObject(i) ?: continue
            val name = t.optString("name").ifBlank { continue }
            val desc = t.optString("description", "")
            val schema = t.optJSONObject("inputSchema")?.toString() ?: "{\"type\":\"object\",\"properties\":{}}"
            out.add(AIToolDef(name = "mcp__${safe(config.name)}__${name}", description = "[MCP:${config.name}] $desc", parametersJsonSchema = schema))
        }
        return out
    }

    fun callTool(toolName: String, arguments: JSONObject): String {
        ensureInitialized()
        val actual = toolName.substringAfterLast("__", toolName).let {
            if (toolName.startsWith("mcp__")) toolName.split("__").getOrNull(2) ?: it else toolName
        }
        val params = JSONObject().put("name", actual).put("arguments", arguments)
        val res = rpc("tools/call", params) ?: return "Error: no response from MCP server"
        if (res.has("error")) return "MCP error: ${res.opt("error")}"
        val result = res.optJSONObject("result") ?: return res.toString()
        val content = result.optJSONArray("content")
        if (content != null) {
            val sb = StringBuilder()
            for (i in 0 until content.length()) {
                val c = content.optJSONObject(i) ?: continue
                if (c.optString("type") == "text") sb.append(c.optString("text"))
                else sb.append(c.toString())
            }
            val text = sb.toString()
            if (text.isNotBlank()) return text
        }
        if (result.has("text")) return result.optString("text")
        return result.toString()
    }

    @Synchronized
    private fun ensureInitialized() {
        if (initialized) return
        val params = JSONObject().apply {
            put("protocolVersion", "2024-11-05")
            put("capabilities", JSONObject())
            put("clientInfo", JSONObject().put("name", "AndroidCodeStudio").put("version", "1.0"))
        }
        rpc("initialize", params)
        // best-effort initialized notification (some servers require/ignore)
        try { notify("notifications/initialized", JSONObject()) } catch (_: Exception) {}
        initialized = true
    }

    private fun rpc(method: String, params: JSONObject): JSONObject? {
        val body = JSONObject().apply {
            put("jsonrpc", "2.0"); put("id", nextId.getAndIncrement()); put("method", method); put("params", params)
        }
        return post(body)
    }

    private fun notify(method: String, params: JSONObject) {
        val body = JSONObject().apply { put("jsonrpc", "2.0"); put("method", method); put("params", params) }
        post(body)
    }

    private fun post(body: JSONObject): JSONObject? {
        if (config.url.isBlank()) return null
        val reqBuilder = Request.Builder()
            .url(config.url.trim())
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .addHeader("Accept", "application/json, text/event-stream")
        config.headers.forEach { (k, v) -> if (k.isNotBlank()) reqBuilder.addHeader(k, v) }
        http.newCall(reqBuilder.build()).execute().use { resp ->
            val text = resp.body?.string().orEmpty()
            if (text.isBlank()) return if (resp.isSuccessful) JSONObject() else JSONObject().put("error", "HTTP ${resp.code}")
            // handle SSE responses (data: lines)
            if (text.startsWith("event:") || text.startsWith("data:") || resp.header("Content-Type")?.contains("text/event-stream") == true) {
                val dataLines = text.lines().filter { it.startsWith("data:") }.map { it.removePrefix("data:").trim() }.filter { it.isNotBlank() && it != "[DONE]" }
                for (line in dataLines.asReversed()) {
                    try {
                        val o = JSONObject(line)
                        if (o.has("result") || o.has("error")) return o
                    } catch (_: Exception) {}
                }
                return try { JSONObject(dataLines.lastOrNull() ?: "{}") } catch (_: Exception) { JSONObject().put("error", text.take(500)) }
            }
            return try { JSONObject(text) } catch (_: Exception) { JSONObject().put("error", text.take(500)) }
        }
    }

    private fun safe(s: String) = s.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
}

class McpToolExecutor(private val servers: List<McpServerConfig>) : com.tom.rv2ide.artificial.tools.AIToolExecutor {
    private val clients = servers.filter { it.enabled && it.url.isNotBlank() }.map { it to McpClient(it) }

    override fun definitions(): List<AIToolDef> = clients.flatMap { (cfg, client) ->
        try { client.listTools() } catch (e: Exception) {
            listOf(AIToolDef("mcp__${cfg.name}__status", "[MCP:${cfg.name}] unavailable: ${e.message}", "{\"type\":\"object\",\"properties\":{}}"))
        }
    }

    override fun execute(name: String, arguments: JSONObject): String {
        if (!name.startsWith("mcp__")) return "Error: not an MCP tool"
        val parts = name.split("__")
        val serverName = parts.getOrNull(1) ?: return "Error: invalid MCP tool name"
        val (_, client) = clients.firstOrNull { safe(it.first.name) == serverName || it.first.name == serverName }
            ?: return "Error: MCP server not found/enabled: $serverName"
        return try { client.callTool(name, arguments) } catch (e: Exception) { "MCP error: ${e.message}" }
    }

    private fun safe(s: String) = s.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)
}
