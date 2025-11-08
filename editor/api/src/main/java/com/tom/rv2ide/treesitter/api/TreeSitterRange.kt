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
import com.tom.rv2ide.treesitter.TSRange
import com.tom.rv2ide.utils.DefaultRecyclable
import com.tom.rv2ide.utils.RecyclableObjectPool

/** @author Akash Yadav */
class TreeSitterRange
@JvmOverloads
internal constructor(
    startByte: Int = 0,
    endByte: Int = 0,
    startPoint: TSPoint? = null,
    endPoint: TSPoint? = null,
) :
    TSRange(startByte, endByte, startPoint, endPoint),
    RecyclableObjectPool.Recyclable by DefaultRecyclable() {

  companion object {

    @JvmStatic
    fun obtain(
        startByte: Int,
        endByte: Int,
        startPoint: TSPoint?,
        endPoint: TSPoint?,
    ): TreeSitterRange {
      return obtainFromPool<TreeSitterRange>().apply {
        this.startByte = startByte
        this.endByte = endByte
        this.startPoint = startPoint
        this.endPoint = endPoint
      }
    }
  }

  override fun recycle() {
    (this.startPoint as? TreeSitterPoint?)?.recycle()
    (this.endPoint as? TreeSitterPoint?)?.recycle()

    this.startByte = 0
    this.endByte = 0
    this.startPoint = null
    this.endPoint = null
    returnToPool()
  }
}
