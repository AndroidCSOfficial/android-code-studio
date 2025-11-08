package com.tom.rv2ide.services.ai.preferences

import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.managers.PreferenceManager

fun getAgentName(): String? {
  val prefManager: PreferenceManager = BaseApplication.getBaseInstance().prefManager
  return prefManager.getString("ai_agent_name", null)
}
