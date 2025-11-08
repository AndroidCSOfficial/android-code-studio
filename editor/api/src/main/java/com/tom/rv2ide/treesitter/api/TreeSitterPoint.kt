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

package com.tom.rv2ide.treesitter.api

import com.tom.rv2ide.treesitter.TSPoint
import com.tom.rv2ide.utils.DefaultRecyclable
import com.tom.rv2ide.utils.RecyclableObjectPool

/** @author Akash Yadav */
class TreeSitterPoint @JvmOverloads internal constructor(row: Int = 0, column: Int = 0) :
    TSPoint(row, column), RecyclableObjectPool.Recyclable by DefaultRecyclable() {

  companion object {

    @JvmStatic
    fun obtain(row: Int, column: Int): TreeSitterPoint {
      return obtainFromPool<TreeSitterPoint>().apply {
        this.row = row
        this.column = column
      }
    }
  }

  override fun recycle() {
    this.row = 0
    this.column = 0
    returnToPool()
  }
}
