/*
 *  This file is part of AndroidIDE.
 *
 *  AndroidIDE is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidIDE is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tom.rv2ide.editor.language

import com.tom.rv2ide.editor.language.utils.CompletionHelper
import com.tom.rv2ide.lsp.api.ILanguageServer
import com.tom.rv2ide.lsp.models.CompletionItem
import com.tom.rv2ide.lsp.models.CompletionParams
import com.tom.rv2ide.lsp.models.CompletionResult
import com.tom.rv2ide.lsp.models.FailureType.COMPLETION
import com.tom.rv2ide.lsp.models.LSPFailure
import com.tom.rv2ide.lsp.util.setupLookupForCompletion
import com.tom.rv2ide.models.Position
import io.github.rosemoe.sora.lang.completion.CompletionCancelledException
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import java.nio.file.Path
import java.util.concurrent.CancellationException
import org.slf4j.LoggerFactory

/**
 * Common implementation of completion provider which requests completions to provided language
 * server.
 *
 * @author Akash Yadav
 */
internal class CommonCompletionProvider(
    private val server: ILanguageServer,
    private val cancelChecker: CompletionCancelChecker,
) {

  companion object {

    private val log = LoggerFactory.getLogger(CommonCompletionProvider::class.java)
  }

  /**
   * Computes completion items using the provided language server instance.
   *
   * @param content The reference to the content of the editor.
   * @param file The file to compute completions for.
   * @param position The position of the cursor in the content.
   * @return The computed completion items. May return an empty list if the there was an error
   *   computing the completion items.
   */
  inline fun complete(
      content: ContentReference,
      file: Path,
      position: CharPosition,
      prefixMatcher: (Char) -> Boolean,
  ): List<CompletionItem> {
    val completionResult =
        try {
          setupLookupForCompletion(file)
          val prefix = CompletionHelper.computePrefix(content, position, prefixMatcher)
          val params =
              CompletionParams(
                  Position(position.line, position.column, position.index),
                  file,
                  cancelChecker,
              )
          params.content = content
          params.prefix = prefix
          server.complete(params)
        } catch (e: Throwable) {

          if (e is CancellationException) {
            log.debug("Completion process cancelled")
          }

          // Do not log if completion was interrupted or cancelled
          if (!(e is CancellationException || e is CompletionCancelledException)) {
            if (!server.handleFailure(LSPFailure(COMPLETION, e))) {
              log.error("Unable to compute completions", e)
            }
          }
          CompletionResult.EMPTY
        }

    if (completionResult == CompletionResult.EMPTY) {
      return listOf()
    }

    return completionResult.items
  }
}
