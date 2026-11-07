package com.tom.rv2ide.artificial.profiles

import com.tom.rv2ide.app.BaseApplication
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class CustomProviderProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val baseUrl: String,
    val modelName: String,
    val apiKey: String = "",
)

object CustomProviderProfileStore {
    private const val KEY = "ai_agent_custom_profiles_v1"
    private const val LEGACY_BASE_URL = "ai_agent_custom_base_url"
    private const val LEGACY_MODEL = "ai_agent_custom_model_name"
    private const val LEGACY_API_KEY = "ai_agent_custom_api_key"

    private fun prefs() = BaseApplication.getBaseInstance().prefManager

    @Synchronized
    fun load(): Pair<String?, List<CustomProviderProfile>> {
        migrateLegacyIfNeeded()
        val raw = prefs().getString(KEY, null) ?: return null to emptyList()
        return try {
            val obj = JSONObject(raw)
            val activeId = obj.optString("activeId").ifBlank { null }
            val arr = obj.optJSONArray("profiles") ?: JSONArray()
            val profiles = mutableListOf<CustomProviderProfile>()
            for (i in 0 until arr.length()) {
                val p = arr.optJSONObject(i) ?: continue
                val name = p.optString("name").ifBlank { continue }
                profiles.add(
                    CustomProviderProfile(
                        id = p.optString("id").ifBlank { UUID.randomUUID().toString() },
                        name = name,
                        baseUrl = p.optString("baseUrl", ""),
                        modelName = p.optString("modelName", ""),
                        apiKey = p.optString("apiKey", ""),
                    )
                )
            }
            activeId to profiles
        } catch (_: Exception) {
            null to emptyList()
        }
    }

    @Synchronized
    fun save(activeId: String?, profiles: List<CustomProviderProfile>) {
        val obj = JSONObject()
        obj.put("activeId", activeId ?: "")
        val arr = JSONArray()
        profiles.forEach { p ->
            arr.put(
                JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("baseUrl", p.baseUrl)
                    put("modelName", p.modelName)
                    put("apiKey", p.apiKey)
                }
            )
        }
        obj.put("profiles", arr)
        prefs().putString(KEY, obj.toString())
        syncLegacy(activeId, profiles)
    }

    fun getActive(): CustomProviderProfile? {
        val (activeId, profiles) = load()
        if (profiles.isEmpty()) return null
        return profiles.firstOrNull { it.id == activeId } ?: profiles.first()
    }

    fun upsert(profile: CustomProviderProfile, makeActive: Boolean = false): CustomProviderProfile {
        val (activeId, profiles) = load()
        val mutable = profiles.toMutableList()
        val idx = mutable.indexOfFirst { it.id == profile.id }
        if (idx >= 0) mutable[idx] = profile else mutable.add(profile)
        val newActive = if (makeActive || activeId == null) profile.id else activeId
        save(newActive, mutable)
        return profile
    }

    fun delete(id: String) {
        val (activeId, profiles) = load()
        val remaining = profiles.filterNot { it.id == id }
        val newActive = if (activeId == id) remaining.firstOrNull()?.id else activeId
        save(newActive, remaining)
    }

    fun setActive(id: String) {
        val (_, profiles) = load()
        if (profiles.any { it.id == id }) save(id, profiles)
    }

    @Synchronized
    private fun migrateLegacyIfNeeded() {
        if (prefs().getString(KEY, null) != null) return
        val baseUrl = prefs().getString(LEGACY_BASE_URL, "") ?: ""
        val model = prefs().getString(LEGACY_MODEL, "") ?: ""
        val apiKey = prefs().getString(LEGACY_API_KEY, "") ?: ""
        if (baseUrl.isBlank() && model.isBlank() && apiKey.isBlank()) return
        val profile =
            CustomProviderProfile(
                name = "Default",
                baseUrl = baseUrl,
                modelName = model.ifBlank { "gpt-4o-mini" },
                apiKey = apiKey,
            )
        save(profile.id, listOf(profile))
    }

    private fun syncLegacy(activeId: String?, profiles: List<CustomProviderProfile>) {
        val active = profiles.firstOrNull { it.id == activeId } ?: profiles.firstOrNull()
        prefs().putString(LEGACY_BASE_URL, active?.baseUrl ?: "")
        prefs().putString(LEGACY_MODEL, active?.modelName ?: "")
        prefs().putString(LEGACY_API_KEY, active?.apiKey ?: "")
    }
}
