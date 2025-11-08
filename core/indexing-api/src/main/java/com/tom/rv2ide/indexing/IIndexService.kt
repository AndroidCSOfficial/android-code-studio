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

package com.tom.rv2ide.indexing

import com.tom.rv2ide.projects.IWorkspace
import java.io.File
import kotlinx.coroutines.Deferred

/**
 * An index service is responsible for indexing symbols.
 *
 * @author Akash Yadav
 */
interface IIndexService {

  /** The display name of the service. */
  val displayName: String

  /**
   * Called to scan files that need to be indexed by this index service.
   *
   * @param workspace The root workspace model which can be used to query the workspace properties.
   * @return A [Deferred] collection of files that need to be indexed.
   */
  fun scanFiles(workspace: IWorkspace): Collection<File>

  /**
   * Called to index the given files.
   *
   * @param workspace The root workspace model which can be used to query the workspace properties.
   * @param files The files to index.
   */
  suspend fun indexFiles(workspace: IWorkspace, files: Collection<File>)
}
