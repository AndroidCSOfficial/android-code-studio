package com.tom.rv2ide.artificial.profiles

import com.tom.rv2ide.app.BaseApplication
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class AgentPromptProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val systemPrompt: String = "",
    val enableTools: Boolean = true,
    val enableMcpTools: Boolean = true,
    val allowCommandExecution: Boolean = false,
    val enabledMcpServerIds: List<String> = emptyList(),
    val skillPreset: String? = null,
)

object SkillPresets {
    fun all(): List<AgentPromptProfile> = listOf(
        AgentPromptProfile(id = "skill-file-reader", name = "Skill: File Reader", systemPrompt = "Focus on reading and understanding project files. Use read_file/list_files/search_project/get_project_tree before making claims. Prefer citing exact paths and line numbers.", enableTools = true, enableMcpTools = false, skillPreset = "file_reader"),
        AgentPromptProfile(id = "skill-command-run", name = "Skill: Command Run (opt-in)", systemPrompt = "You may use run_command only when explicitly enabled and necessary. Prefer safe, read-only commands. Explain what a command does before running it. Never run destructive commands without explicit user confirmation in chat.", enableTools = true, enableMcpTools = false, allowCommandExecution = false, skillPreset = "command_run"),
        AgentPromptProfile(id = "skill-file-modification", name = "Skill: File Modification", systemPrompt = "Focus on precise code/file edits. Read relevant files first when needed, then produce FILE_TO_MODIFY blocks with complete, correct file contents. Preserve unrelated code.", enableTools = true, enableMcpTools = false, skillPreset = "file_modification"),
        AgentPromptProfile(id = "skill-stitch", name = "Skill: Stitch", systemPrompt = "You are a UI/product implementation assistant for Stitch-style workflows. Turn requirements into clear implementation steps and Android/UI code changes. Use available MCP/tools if configured. When editing files, use FILE_TO_MODIFY with exact project paths.", enableTools = true, enableMcpTools = true, skillPreset = "stitch"),
        AgentPromptProfile(id = "skill-design", name = "Skill: Design", systemPrompt = "You are a software design assistant. Produce clear architecture, UX, and implementation guidance. Prefer practical Android/Gradle/Kotlin patterns. When the user asks for file changes, output FILE_TO_MODIFY blocks.", enableTools = true, enableMcpTools = true, skillPreset = "design"),
        AgentPromptProfile(id = "skill-custom", name = "Skill: Custom", systemPrompt = "", enableTools = true, enableMcpTools = true, skillPreset = "custom"),
    )
}

object AgentPromptProfileStore {
    private const val KEY = "ai_agent_prompt_profiles_v1"
    private fun prefs() = BaseApplication.getBaseInstance().prefManager

    @Synchronized
    fun load(): Pair<String?, List<AgentPromptProfile>> {
        ensureDefault()
        val raw = prefs().getString(KEY, null) ?: return null to emptyList()
        return try {
            val obj = JSONObject(raw)
            val activeId = obj.optString("activeId").ifBlank { null }
            val arr = obj.optJSONArray("profiles") ?: JSONArray()
            val profiles = mutableListOf<AgentPromptProfile>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val name = p.optString("name").ifBlank { continue }
                val serverIds = mutableListOf<String>()
                val sArr = p.optJSONArray("enabledMcpServerIds") ?: JSONArray()
                for (j in 0 until sArr.length()) serverIds.add(sArr.optString(j))
                profiles.add(
                    AgentPromptProfile(
                        id = p.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = name,
                        systemPrompt = p.optString("systemPrompt", ""),
                        enableTools = p.optBoolean("enableTools", true),
                        enableMcpTools = p.optBoolean("enableMcpTools", true),
                        allowCommandExecution = p.optBoolean("allowCommandExecution", false),
                        enabledMcpServerIds = serverIds,
                        skillPreset = p.optString("skillPreset").ifBlank { null },
                    )
                )
            }
            activeId to profiles
        } catch (_: Exception) { null to emptyList() }
    }

    @Synchronized
    fun save(activeId: String?, profiles: List<AgentPromptProfile>) {
        val obj = JSONObject()
        obj.put("activeId", activeId ?: "")
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id); put("name", p.name); put("systemPrompt", p.systemPrompt)
                put("enableTools", p.enableTools); put("enableMcpTools", p.enableMcpTools)
                put("allowCommandExecution", p.allowCommandExecution)
                put("enabledMcpServerIds", JSONArray(p.enabledMcpServerIds))
                put("skillPreset", p.skillPreset ?: "")
            })
        }
        obj.put("profiles", arr)
        prefs().putString(KEY, obj.toString())
    }

    fun getActive(): AgentPromptProfile? {
        val (activeId, profiles) = load()
        return profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
    }

    fun upsert(profile: AgentPromptProfile, makeActive: Boolean = false): AgentPromptProfile {
        val (activeId, profiles) = load()
        val mutable = profiles.toMutableList()
        val idx = mutable.indexOfFirst { it.id == profile.id }
        if (idx >= 0) mutable[idx] = profile else mutable.add(profile)
        save(if (makeActive || activeId == null) profile.id else activeId, mutable)
        return profile
    }

    fun delete(id: String) {
        val (activeId, profiles) = load()
        if (profiles.size <= 1) return
        val remaining = profiles.filterNot { it.id == id }
        save(if (activeId == id) remaining.firstOrNull()?.id else activeId, remaining)
    }

    fun setActive(id: String) {
        val (_, profiles) = load()
        if (profiles.any { it.id == id }) save(id, profiles)
    }

    @Synchronized
    private fun ensureDefault() {
        if (prefs().getString(KEY, null) != null) return
        val def = AgentPromptProfile(name = "Default", systemPrompt = "", enableTools = true, enableMcpTools = true)
        save(def.id, listOf(def))
    }

    fun addPreset(preset: AgentPromptProfile): AgentPromptProfile {
        val copy = preset.copy(id = UUID.randomUUID().toString())
        return upsert(copy, makeActive = false)
    }
}
