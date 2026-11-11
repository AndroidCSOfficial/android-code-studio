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
package com.tom.rv2ide.artificial.secrets

import com.tom.rv2ide.preferences.internal.prefManager

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
*/

object ApiKey {
    
    // Check if AI Agent is enabled
    fun isAIAgentEnabled(): Boolean {
        return prefManager.getBoolean("ai_agent_enabled", false)
    }
    
    // Gemini API Key
    fun getGeminiApiKey(): String {
        return prefManager.getString("ai_agent_gemini_api_key", "")
    }
    
    fun hasGeminiKey(): Boolean {
        val key = getGeminiApiKey()
        return key.isNotBlank() && key.length > 20
    }
    
    // OpenAI API Key
    fun getOpenAIApiKey(): String {
        return prefManager.getString("ai_agent_openai_api_key", "")
    }
    
    fun hasOpenAIKey(): Boolean {
        val key = getOpenAIApiKey()
        return key.isNotBlank() && key.length > 20
    }
    
    // Deepseek API Key
    fun getDeepseekApiKey(): String {
        return prefManager.getString("ai_agent_deepseek_api_key", "")
    }
    
    fun hasDeepseekKey(): Boolean {
        val key = getDeepseekApiKey()
        return key.isNotBlank() && key.length > 20
    }
    
    // Anthropic API Key
    fun getAnthropicApiKey(): String {
        return prefManager.getString("ai_agent_anthropic_api_key", "")
    }
    
    fun hasAnthropicKey(): Boolean {
        val key = getAnthropicApiKey()
        return key.isNotBlank() && key.length > 20
    }
    
    // Grok API Key
    fun getGrokApiKey(): String {
        return prefManager.getString("ai_agent_grok_api_key", "")
    }
    
    fun hasGrokKey(): Boolean {
        val key = getGrokApiKey()
        return key.isNotBlank() && key.length > 20
    }
    
    // Legacy methods for backward compatibility
    @Deprecated("Use getGeminiApiKey() instead", ReplaceWith("getGeminiApiKey()"))
    fun getApiKey(): String {
        return getGeminiApiKey()
    }
    
    // Get list of available providers
    fun getAvailableProviders(): List<String> {
        val providers = mutableListOf<String>()
        if (hasGeminiKey()) providers.add("Gemini")
        if (hasOpenAIKey()) providers.add("OpenAI")
        if (hasDeepseekKey()) providers.add("Deepseek")
        if (hasAnthropicKey()) providers.add("Anthropic")
        if (hasGrokKey()) providers.add("Grok")
        return providers
    }
    
    // Get all API keys as a map
    fun getAllApiKeys(): Map<String, String> {
        return mapOf(
            "gemini" to getGeminiApiKey(),
            "openai" to getOpenAIApiKey(),
            "deepseek" to getDeepseekApiKey(),
            "anthropic" to getAnthropicApiKey(),
            "grok" to getGrokApiKey()
        ).filterValues { it.isNotBlank() }
    }
    
    // Custom Endpoint API Key + Base URL + Model
    fun getCustomApiKey(): String {
        return prefManager.getString("ai_agent_custom_api_key", "")
    }
    fun hasCustomKey(): Boolean {
        val key = getCustomApiKey()
        return key.isNotBlank() && key.length > 3
    }
    fun getCustomBaseUrl(): String {
        return prefManager.getString("ai_agent_custom_base_url", "").trim().trimEnd('/')
    }
    fun getCustomModel(): String {
        return prefManager.getString("ai_agent_custom_model", "custom-model").trim()
    }

    // Check if any API key is configured
    fun hasAnyApiKey(): Boolean {
        return hasGeminiKey() || hasOpenAIKey() || hasDeepseekKey() || 
               hasAnthropicKey() || hasGrokKey() || hasCustomKey()
    }
}