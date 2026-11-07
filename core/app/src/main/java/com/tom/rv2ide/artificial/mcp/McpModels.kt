package com.tom.rv2ide.artificial.mcp

import com.tom.rv2ide.app.BaseApplication
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class McpServerConfig(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val url: String,
    val enabled: Boolean = true,
    val headers: Map<String, String> = emptyMap(),
    val preset: String? = null,
)

object McpPresets {
    fun all(): List<McpServerConfig> = listOf(
        McpServerConfig(id = "preset-github", name = "GitHub (preset)", url = "https://api.githubcopilot.com/mcp/", enabled = false, headers = mapOf("Authorization" to "Bearer <GITHUB_TOKEN>"), preset = "github"),
        McpServerConfig(id = "preset-firebase", name = "Firebase (preset)", url = "https://<YOUR-FIREBASE-MCP-ENDPOINT>", enabled = false, preset = "firebase"),
        McpServerConfig(id = "preset-stitch", name = "Stitch (preset)", url = "https://<YOUR-STITCH-MCP-ENDPOINT>", enabled = false, preset = "stitch"),
        McpServerConfig(id = "preset-vercel", name = "Vercel (preset)", url = "https://<YOUR-VERCEL-MCP-ENDPOINT>", enabled = false, headers = mapOf("Authorization" to "Bearer <VERCEL_TOKEN>"), preset = "vercel"),
        McpServerConfig(id = "preset-cloudflare", name = "Cloudflare (preset)", url = "https://<YOUR-CLOUDFLARE-MCP-ENDPOINT>", enabled = false, headers = mapOf("Authorization" to "Bearer <CLOUDFLARE_TOKEN>"), preset = "cloudflare"),
        McpServerConfig(id = "preset-supabase", name = "Supabase (preset)", url = "https://<YOUR-SUPABASE-MCP-ENDPOINT>", enabled = false, headers = mapOf("Authorization" to "Bearer <SUPABASE_TOKEN>"), preset = "supabase"),
        McpServerConfig(id = "preset-custom", name = "Custom MCP (template)", url = "https://", enabled = false, preset = "custom"),
    )
}

object McpServerStore {
    private const val KEY = "ai_agent_mcp_servers_v1"
    private fun prefs() = BaseApplication.getBaseInstance().prefManager

    @Synchronized
    fun load(): List<McpServerConfig> {
        val raw = prefs().getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val name = o.optString("name").ifBlank { return@mapNotNull null }
                val headers = mutableMapOf<String, String>()
                val h = o.optJSONObject("headers")
                h?.keys()?.forEach { k -> headers[k] = h.optString(k) }
                McpServerConfig(
                    id = o.optString("id").ifBlank { UUID.randomUUID().toString() },
                    name = name,
                    url = o.optString("url", ""),
                    enabled = o.optBoolean("enabled", true),
                    headers = headers,
                    preset = o.optString("preset").ifBlank { null },
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized
    fun save(servers: List<McpServerConfig>) {
        val arr = JSONArray()
        servers.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("url", s.url); put("enabled", s.enabled)
                put("headers", JSONObject(s.headers)); put("preset", s.preset ?: "")
            })
        }
        prefs().putString(KEY, arr.toString())
    }

    fun upsert(server: McpServerConfig) {
        val list = load().toMutableList()
        val idx = list.indexOfFirst { it.id == server.id }
        if (idx >= 0) list[idx] = server else list.add(server)
        save(list)
    }

    fun delete(id: String) = save(load().filterNot { it.id == id })

    fun addPreset(preset: McpServerConfig) {
        val copy = preset.copy(id = UUID.randomUUID().toString(), enabled = false)
        upsert(copy)
    }
}
