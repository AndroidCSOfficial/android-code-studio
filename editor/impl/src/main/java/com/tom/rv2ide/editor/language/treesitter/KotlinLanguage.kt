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

package com.tom.rv2ide.editor.language.treesitter

import android.content.Context
import com.tom.rv2ide.editor.language.treesitter.TreeSitterLanguage.Factory
import com.tom.rv2ide.lsp.api.ILanguageServer
import com.tom.rv2ide.lsp.api.ILanguageServerRegistry
import com.tom.rv2ide.lsp.kotlin.KotlinLanguageServer
import com.tom.rv2ide.treesitter.kotlin.TSLanguageKotlin
import io.github.rosemoe.sora.lang.Language.INTERRUPTION_LEVEL_STRONG
import io.github.rosemoe.sora.util.MyCharacter

/**
 * [TreeSitterLanguage] implementation for Kotlin.
 *
 * @author Akash Yadav
 */
open class KotlinLanguage(context: Context) :
    TreeSitterLanguage(context, TSLanguageKotlin.getInstance(), TS_TYPE_KT) {

  companion object {

    val FACTORY = Factory { KotlinLanguage(it) }
    const val TS_TYPE_KT = "kt"
    const val TS_TYPE_KTS = "kts"
  }

  override val languageServer: ILanguageServer?
    get() = ILanguageServerRegistry.getDefault().getServer(KotlinLanguageServer.SERVER_ID)

  override fun checkIsCompletionChar(c: Char): Boolean {
    return MyCharacter.isJavaIdentifierPart(c) || c == '.'
  }

  override fun getInterruptionLevel(): Int {
    return INTERRUPTION_LEVEL_STRONG
  }
}
