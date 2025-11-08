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
import com.tom.rv2ide.models.Location
import com.tom.rv2ide.models.Position
import com.tom.rv2ide.progress.ICancelChecker
import java.nio.file.Path
import jdkx.lang.model.element.Element
import jdkx.tools.JavaFileObject

/**
 * Finds definition of an element in other source locations.
 *
 * @author Akash Yadav
 */
class RemoteDefinitionProvider(
    position: Position,
    completingFile: Path,
    compiler: JavaCompilerService,
    settings: IServerSettings,
    cancelChecker: ICancelChecker,
) : IJavaDefinitionProvider(position, completingFile, compiler, settings, cancelChecker) {

  private lateinit var otherFile: JavaFileObject

  fun setOtherFile(jfo: JavaFileObject): RemoteDefinitionProvider {
    this.otherFile = jfo
    return this
  }

  override fun doFindDefinition(element: Element): List<Location> {
    //    val task = compiler.compile(listOf(SourceFileObject(file), otherFile))
    val provider = LocalDefinitionProvider(position, file, compiler, settings, this)
    return provider.findDefinition(element)
    //    return provider
    //      .findDefinition(task.get { NavigationHelper.findElement(it, file, line, column) })
  }
}
