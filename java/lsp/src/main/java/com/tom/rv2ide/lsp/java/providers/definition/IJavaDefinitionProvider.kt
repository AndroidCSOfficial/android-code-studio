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

package com.tom.rv2ide.lsp.java.providers.definition

import com.tom.rv2ide.lsp.api.IServerSettings
import com.tom.rv2ide.lsp.java.compiler.JavaCompilerService
import com.tom.rv2ide.lsp.java.providers.BaseJavaServiceProvider
import com.tom.rv2ide.lsp.java.providers.DefinitionProvider
import com.tom.rv2ide.models.Location
import com.tom.rv2ide.models.Position
import com.tom.rv2ide.progress.ICancelChecker
import java.nio.file.Path
import jdkx.lang.model.element.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Provides definition for a specific symbol in Java source code.
 *
 * @author Akash Yadav
 */
abstract class IJavaDefinitionProvider(
    protected val position: Position,
    completingFile: Path,
    compiler: JavaCompilerService,
    settings: IServerSettings,
    cancelChecker: ICancelChecker,
) : BaseJavaServiceProvider(completingFile, compiler, settings), ICancelChecker by cancelChecker {

  protected val line = position.line
  protected val column = position.column

  companion object {

    @JvmStatic
    protected val log: Logger = LoggerFactory.getLogger(IJavaDefinitionProvider::class.java)
  }

  /**
   * Finds the definition for the given element.
   *
   * @param element The element to find definition for.
   */
  fun findDefinition(element: Element?): List<Location> {
    if (element == null) {
      return DefinitionProvider.NOT_SUPPORTED
    }

    return doFindDefinition(element)
  }

  abstract fun doFindDefinition(element: Element): List<Location>
}
