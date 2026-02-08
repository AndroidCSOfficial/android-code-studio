package com.tom.rv2ide.uidesigner

import com.google.gson.JsonElement
import com.google.gson.JsonParser

object ComposePreviewDetector {

  fun detect(text: String, symbolsJson: String?): List<String> {
    if (!symbolsJson.isNullOrEmpty()) {
      val fromSymbols = detectFromSymbols(text, symbolsJson)
      if (fromSymbols.isNotEmpty()) return fromSymbols
    }
    return detectFromRegex(text)
  }

  fun detectFromRegex(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val regex = Regex("@Preview[\\s\\S]*?fun\\s+(\\w+)\\s*\\(")
    return regex.findAll(text).map { it.groupValues[1] }.toList()
  }

  fun detectFromSymbols(text: String, symbolsJson: String): List<String> {
    val previews = mutableListOf<String>()
    try {
      val elem = JsonParser.parseString(symbolsJson)
      val arr = when {
        elem.isJsonArray -> elem.asJsonArray
        elem.isJsonObject && elem.asJsonObject.has("result") && elem.asJsonObject.get("result").isJsonArray -> elem.asJsonObject.getAsJsonArray("result")
        else -> null
      } ?: return emptyList()

      fun walk(j: JsonElement) {
        if (!j.isJsonObject) return
        val obj = j.asJsonObject

        if (obj.has("kind") && obj.get("kind").isJsonPrimitive) {
          val kind = obj.get("kind").asInt
          if (kind == 12) {
            val name = obj.get("name")?.asString ?: ""
            val startLine = try {
              obj.getAsJsonObject("range").getAsJsonObject("start").get("line").asInt
            } catch (e: Exception) { -1 }
            if (startLine >= 0) if (checkPreviewAbove(text, startLine)) previews.add(name)
          }
        }

        if (obj.has("location") && obj.get("location").isJsonObject) {
          try {
            val name = obj.get("name")?.asString ?: ""
            val loc = obj.getAsJsonObject("location")
            val startLine = loc.getAsJsonObject("range").getAsJsonObject("start").get("line").asInt
            if (startLine >= 0 && checkPreviewAbove(text, startLine)) previews.add(name)
          } catch (e: Exception) {
          }
        }

        if (obj.has("children") && obj.get("children").isJsonArray) {
          obj.getAsJsonArray("children").forEach { walk(it) }
        }
      }

      arr.forEach { walk(it) }
    } catch (e: Exception) {
      return emptyList()
    }

    return previews.distinct()
  }

  private fun checkPreviewAbove(text: String, startLine: Int): Boolean {
    val lines = text.split('\n')
    val from = kotlin.math.max(0, startLine - 6)
    for (i in startLine - 1 downTo from) {
      val l = lines.getOrNull(i) ?: continue
      if (l.contains("@Preview")) return true
      if (l.trim().startsWith("fun ") || l.trim().startsWith("class ") || l.trim().startsWith("object ")) return false
    }
    return false
  }
}
