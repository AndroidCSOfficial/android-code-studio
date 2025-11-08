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

import com.tom.rv2ide.treesitter.TSQueryPredicateStep
import com.tom.rv2ide.utils.DefaultRecyclable
import com.tom.rv2ide.utils.RecyclableObjectPool

/** @author Akash Yadav */
class TreeSitterQueryPredicateStep
@JvmOverloads
internal constructor(type: Int = 0, valueId: Int = 0) :
    TSQueryPredicateStep(type, valueId), RecyclableObjectPool.Recyclable by DefaultRecyclable() {

  companion object {

    @JvmStatic
    fun obtain(type: Int, valueId: Int): TreeSitterQueryPredicateStep {
      return obtainFromPool<TreeSitterQueryPredicateStep>().apply {
        this.type = type
        this.valueId = valueId
      }
    }
  }

  override fun recycle() {
    this.type = 0
    this.valueId = 0
    this.cachedType = null
    returnToPool()
  }
}
