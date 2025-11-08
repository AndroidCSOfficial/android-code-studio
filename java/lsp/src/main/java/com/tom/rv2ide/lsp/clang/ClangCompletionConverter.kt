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
package com.tom.rv2ide.lsp.clang

import com.google.gson.JsonArray
import com.tom.rv2ide.lsp.models.CompletionItem
import com.tom.rv2ide.lsp.models.CompletionItemKind
import com.tom.rv2ide.lsp.models.InsertTextFormat
import com.tom.rv2ide.lsp.models.MatchLevel
import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ClangCompletionConverter {

  companion object {
    private val log = LoggerFactory.getLogger(ClangCompletionConverter::class.java)
  }

  fun convert(jsonItems: JsonArray): List<CompletionItem> {
    return jsonItems.mapNotNull { element ->
      try {
        val item = element.asJsonObject
        val label = item.get("label")?.asString ?: return@mapNotNull null
        val kind = item.get("kind")?.asInt ?: 1
        val detail = item.get("detail")?.asString ?: ""
        val insertText = item.get("insertText")?.asString ?: label

        CompletionItem(
                ideLabel = label,
                detail = detail,
                insertText = insertText,
                insertTextFormat = InsertTextFormat.PLAIN_TEXT,
                sortText = item.get("sortText")?.asString ?: label,
                command = null,
                completionKind = convertKind(kind),
                matchLevel = MatchLevel.NO_MATCH,
                additionalTextEdits = null,
                data = null,
            )
            .apply {
              // Set documentation if available
              item.get("documentation")?.asString?.let { doc -> this.desc = doc }
            }
      } catch (e: Exception) {
        ClangLogs.error("Error converting completion item", e)
        null
      }
    }
  }

  private fun convertKind(lspKind: Int): CompletionItemKind {
    return when (lspKind) {
      2 -> CompletionItemKind.METHOD
      3 -> CompletionItemKind.FUNCTION
      4 -> CompletionItemKind.CONSTRUCTOR
      5 -> CompletionItemKind.FIELD
      6 -> CompletionItemKind.VARIABLE
      7 -> CompletionItemKind.CLASS
      8 -> CompletionItemKind.INTERFACE
      9 -> CompletionItemKind.MODULE
      10 -> CompletionItemKind.PROPERTY
      12 -> CompletionItemKind.VALUE
      13 -> CompletionItemKind.ENUM
      14 -> CompletionItemKind.KEYWORD
      15 -> CompletionItemKind.SNIPPET
      21 -> CompletionItemKind.ENUM_MEMBER
      26 -> CompletionItemKind.TYPE_PARAMETER
      else -> CompletionItemKind.NONE
    }
  }
}
