package com.tom.rv2ide.artificial.tools

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OpenAICompatNative {
    data class CallConfig(val endpoint: String, val apiKey: String?, val model: String, val extraHeaders: Map<String, String> = emptyMap())

    fun call(cfg: CallConfig, messages: JSONArray, tools: List<AIToolDef>?): ToolAwareResponse {
        val body = JSONObject().apply {
            put("model", cfg.model); put("messages", messages); put("temperature", 0.7)
            if (!tools.isNullOrEmpty()) { put("tools", ToolCallProtocol.openAIToolsJson(tools)); put("tool_choice", "auto") }
        }
        val conn = URL(cfg.endpoint).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            cfg.apiKey?.takeIf { it.isNotBlank() }?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            cfg.extraHeaders.forEach { (k,v) -> if (k.isNotBlank()) conn.setRequestProperty(k,v) }
            conn.doOutput = true; conn.connectTimeout = 30000; conn.readTimeout = 90000
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText().orEmpty()
            if (code !in 200..299) throw Exception("API error $code: $text")
            val msg = JSONObject(text).getJSONArray("choices").getJSONObject(0).getJSONObject("message")
            val content = msg.optString("content", "")
            val calls = mutableListOf<AIToolCall>()
            val arr = msg.optJSONArray("tool_calls")
            if (arr != null) for (i in 0 until arr.length()) {
                val tc = arr.optJSONObject(i) ?: continue; val fn = tc.optJSONObject("function") ?: continue
                val n = fn.optString("name"); if (n.isBlank()) continue
                val a = try { JSONObject(fn.optString("arguments","{}").ifBlank{"{}"}) } catch (_: Exception){ JSONObject() }
                calls.add(AIToolCall(n,a,tc.toString(), tc.optString("id").ifBlank{null}))
            }
            if (calls.isEmpty()) ToolCallProtocol.parse(content)?.let { calls.add(it) }
            return ToolAwareResponse(content, calls)
        } finally { conn.disconnect() }
    }

    fun assistantToolCallsMessage(res: ToolAwareResponse): JSONObject = JSONObject().apply {
        put("role","assistant"); put("content", res.text)
        val arr = JSONArray(); res.toolCalls.forEach { tc -> arr.put(JSONObject().apply { put("id", tc.id ?: "call_${tc.name}_${System.nanoTime()}"); put("type","function"); put("function", JSONObject().put("name",tc.name).put("arguments",tc.arguments.toString())) }) }; put("tool_calls", arr)
    }
    fun toolResultMessage(tr: AIToolResult): JSONObject = JSONObject().put("role","tool").put("tool_call_id", tr.toolCallId ?: tr.name).put("content", tr.result)
}
