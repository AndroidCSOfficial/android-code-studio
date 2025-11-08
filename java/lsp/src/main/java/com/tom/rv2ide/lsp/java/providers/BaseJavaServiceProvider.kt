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

package com.tom.rv2ide.lsp.java.providers

import com.tom.rv2ide.lookup.Lookup
import com.tom.rv2ide.lsp.api.IServerSettings
import com.tom.rv2ide.lsp.java.compiler.JavaCompilerService
import com.tom.rv2ide.progress.ICancelChecker
import java.nio.file.Path

/**
 * Base class for java service providers.
 *
 * @author Akash Yadav
 */
abstract class BaseJavaServiceProvider(
    protected val file: Path,
    protected val compiler: JavaCompilerService,
    protected val settings: IServerSettings,
) {

  /** Abort the completion if cancelled. */
  fun abortCompletionIfCancelled() {
    val checker = Lookup.getDefault().lookup(ICancelChecker::class.java)
    checker?.abortIfCancelled()
  }
}
