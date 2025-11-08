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

package com.tom.rv2ide.editor.language.treesitter

import android.content.Context
import com.tom.rv2ide.editor.language.treesitter.TreeSitterLanguage.Factory
import com.tom.rv2ide.lsp.api.ILanguageServer
import com.tom.rv2ide.lsp.api.ILanguageServerRegistry
import com.tom.rv2ide.lsp.clang.ClangLanguageServer
import com.tom.rv2ide.treesitter.cpp.TSLanguageCpp

/**
 * [TreeSitterLanguage] implementation for Cpp.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
open class CppLang(context: Context) :
    TreeSitterLanguage(context, TSLanguageCpp.getInstance(), TS_TYPE_CPP) {

  companion object {

    val FACTORY = Factory { CppLang(it) }
    const val TS_TYPE_CPP = "cpp"
  }

  override val languageServer: ILanguageServer?
    get() = ILanguageServerRegistry.getDefault().getServer(ClangLanguageServer.SERVER_ID)
}
