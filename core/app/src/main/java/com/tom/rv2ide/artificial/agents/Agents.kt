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
import android.content.SharedPreferences
import android.preference.PreferenceManager

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class Agents(ctx: Context) {

  private val sp: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx)
  private val AGENT_KEY = "ai_agent_model_name"
  private val PROVIDER_KEY = "ai_provider_name"
  
  private val openai_models = arrayOf(
    "gpt-5.1-codex-max",
    "gpt-5.1-codex",
    "gpt-5.1-codex-mini",
    "gpt-5-codex",

    // GPT-5 family (text models)
    "gpt-5-chat-latest",
    "gpt-5-2025-08-07",
    "gpt-5",
    "gpt-5-mini-2025-08-07",
    "gpt-5-mini",
    "gpt-5-nano-2025-08-07",
    "gpt-5-nano",
    "gpt-5-pro-2025-10-06",
    "gpt-5-pro",
    "gpt-5-search-api",           // produces text, coding-capable even if optimized for search
    "gpt-5-search-api-2025-10-14",

    // GPT-5.1 models
    "gpt-5.1-chat-latest",
    "gpt-5.1",
    "gpt-5.1-2025-11-13",

    // GPT-4.1 family (all text)
    "gpt-4.1-2025-04-14",
    "gpt-4.1",
    "gpt-4.1-mini-2025-04-14",
    "gpt-4.1-mini",
    "gpt-4.1-nano-2025-04-14",
    "gpt-4.1-nano",

    // GPT-4o (all text/omni variants except audio, tts, transcribe)
    "minimax-m2.1",
    "gpt-4o-2024-05-13",
    "gpt-4o-mini-2024-07-18",
    "gpt-4o-mini",
    "gpt-4o-2024-08-06",
    "gpt-4o-2024-11-20",
    "gpt-4o-search-preview-2025-03-11",
    "gpt-4o-search-preview",
    "gpt-4o-mini-search-preview-2025-03-11",
    "gpt-4o-mini-search-preview",

    // O-series (general purpose = coding-capable)
    "o1-2024-12-17",
    "o1",
    "o3-mini",
    "o3-mini-2025-01-31",
    "o3-2025-04-16",
    "o3",
    "o4-mini-2025-04-16",
    "o4-mini",

    // GPT-3.5 (text models, all coding capable)
    "gpt-3.5-turbo",
    "gpt-3.5-turbo-1106",
    "gpt-3.5-turbo-0125",
    "gpt-3.5-turbo-instruct",
    "gpt-3.5-turbo-instruct-0914",
    "gpt-3.5-turbo-16k",

    // Legacy general-purpose LLMs (still text)
    "davinci-002",
    "babbage-002"
  )
  
  private val claude_models = arrayOf(
    "claude-sonnet-4-5-20250929",
    "claude-haiku-4-5-20251001",
    "claude-opus-4-5-20251101",
    "claude-opus-4-1-20250805",
    "claude-opus-4-20250514",
    "claude-sonnet-4-20250514",
    "claude-3-7-sonnet-20250219",
    "claude-3-5-haiku-20241022",
    "claude-3-haiku-20240307"
  )
  
  private val gemini_models = arrayOf(
    "gemini-3-pro-preview",
    "gemini-2.5-pro",
    "gemini-2.5-flash",
    "gemini-2.5-flash-lite",
    "gemini-2.0-flash",
    "gemini-2.0-flash-lite",
    "gemini-1.5-flash",
    "gemini-1.5-pro"
  )
  
  private val deepseek_models = arrayOf(
    "deepseek-chat",
    "deepseek-reasoner"
  )
  
  private val grok_models = arrayOf(
    "grok-4-1-fast-reasoning",
    "grok-4-1-fast-non-reasoning",
    "grok-code-fast-1",
    "grok-4-fast-reasoning",
    "grok-4-fast-non-reasoning",
    "grok-4-0709",
    "grok-3",
    "grok-3-mini",
    "grok-beta",
    "grok-2",
    "grok-2-mini"
  )
  
  private val localllm_models = arrayOf(
    "local-model"
  )
  
  val ai_agents = openai_models + claude_models + gemini_models + deepseek_models + grok_models + localllm_models
  
  fun getModelsForProvider(providerId: String): Array<String> {
    return when(providerId) {
      "openai" -> openai_models
      "gemini" -> gemini_models
      "claude" -> claude_models
      "deepseek" -> deepseek_models
      "grok" -> grok_models
      "localllm" -> localllm_models
      else -> gemini_models
    }
  }
  
  fun getProviderForModel(modelName: String): String? {
    return when {
      modelName in openai_models -> "openai"
      modelName in gemini_models -> "gemini"
      modelName in claude_models -> "claude"
      modelName in deepseek_models -> "deepseek"
      modelName in grok_models -> "grok"
      modelName in localllm_models -> "localllm"
      else -> null
    }
  }
  
  fun setAgent(name: String) {
      val provider = when {
          name in openai_models -> "openai"
          name in gemini_models -> "gemini"
          name in claude_models -> "claude"
          name in deepseek_models -> "deepseek"
          name in grok_models -> "grok"
          else -> sp.getString(PROVIDER_KEY, "gemini") ?: "gemini"
      }
      
      sp.edit().putString(PROVIDER_KEY, provider).apply()
      sp.edit().putString(AGENT_KEY, name).apply()
  }
  
  fun getAgent(): String {
    val savedModel = sp.getString(AGENT_KEY, null)
    if (savedModel != null) return savedModel
    
    return when (getProvider()) {
      "openai" -> "gpt-4o"
      "gemini" -> "gemini-2.5-pro"
      "claude" -> "claude-sonnet-4-20250514"
      "deepseek" -> "deepseek-chat"
      "grok" -> "grok-beta"
      else -> "gemini-2.5-pro"
    }
  }
  
  fun setProvider(provider: String) {
    sp.edit().putString(PROVIDER_KEY, provider).apply()
  }
  
  fun getProvider(): String {
    return sp.getString(PROVIDER_KEY, "gemini") ?: "gemini"
  }
  
  fun isValidModelForProvider(modelName: String, providerId: String): Boolean {
    return modelName in getModelsForProvider(providerId)
  }
}
