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

import com.tom.rv2ide.treesitter.TSInputEdit
import com.tom.rv2ide.treesitter.TSLanguage
import com.tom.rv2ide.treesitter.TSLookaheadIterator
import com.tom.rv2ide.treesitter.TSNode
import com.tom.rv2ide.treesitter.TSParser
import com.tom.rv2ide.treesitter.TSPoint
import com.tom.rv2ide.treesitter.TSQuery
import com.tom.rv2ide.treesitter.TSQueryCapture
import com.tom.rv2ide.treesitter.TSQueryCursor
import com.tom.rv2ide.treesitter.TSQueryMatch
import com.tom.rv2ide.treesitter.TSQueryPredicateStep
import com.tom.rv2ide.treesitter.TSRange
import com.tom.rv2ide.treesitter.TSTree
import com.tom.rv2ide.treesitter.TSTreeCursor
import com.tom.rv2ide.treesitter.TSTreeCursorNode
import com.tom.rv2ide.treesitter.string.SynchronizedUTF16String
import com.tom.rv2ide.treesitter.string.UTF16String
import com.tom.rv2ide.treesitter.util.TSObjectFactory

/**
 * [TSObjectFactory] implementation for AndroidIDE.
 *
 * @author Akash Yadav
 */
class TreeSitterObjectFactory : TSObjectFactory {

  override fun createInputEdit(
      startByte: Int,
      oldEndByte: Int,
      newEndByte: Int,
      startPoint: TSPoint,
      oldEndPoint: TSPoint,
      newEndPoint: TSPoint,
  ): TSInputEdit {
    return TreeSitterInputEdit.obtain(
        startByte,
        oldEndByte,
        newEndByte,
        startPoint,
        oldEndPoint,
        newEndPoint,
    )
  }

  override fun createParser(parserPointer: Long): TSParser {
    return TreeSitterParser.obtain(parserPointer)
  }

  override fun createQuery(queryPointer: Long): TSQuery {
    return TreeSitterQuery.obtain(queryPointer)
  }

  override fun createQueryCursor(pointer: Long): TSQueryCursor {
    return TreeSitterQueryCursor.obtain(pointer)
  }

  override fun createPoint(row: Int, column: Int): TSPoint {
    return TreeSitterPoint.obtain(row, column)
  }

  override fun createRange(
      startByte: Int,
      endByte: Int,
      startPoint: TSPoint,
      endPoint: TSPoint?,
  ): TSRange {
    return TreeSitterRange.obtain(startByte, endByte, startPoint, endPoint)
  }

  override fun createRangeArr(size: Int): Array<TSRange?> {
    return arrayOfNulls(size)
  }

  override fun createTree(pointer: Long): TSTree {
    return TreeSitterTree.obtain(pointer)
  }

  override fun createTreeCursor(pointer: Long): TSTreeCursor {
    return TreeSitterTreeCursor.obtain(pointer)
  }

  override fun createNode(
      context0: Int,
      context1: Int,
      context2: Int,
      context3: Int,
      id: Long,
      treePointer: Long,
  ): TSNode {
    return TreeSitterNode.obtain(context0, context1, context2, context3, id, treePointer)
  }

  override fun createQueryCapture(node: TSNode?, index: Int): TSQueryCapture {
    return TreeSitterQueryCapture.obtain(node, index)
  }

  override fun createQueryMatch(
      id: Int,
      patternIndex: Int,
      captures: Array<TSQueryCapture?>?,
  ): TSQueryMatch {
    return TreeSitterQueryMatch.obtain(id, patternIndex, captures)
  }

  override fun createQueryPredicateStep(type: Int, valueId: Int): TSQueryPredicateStep {
    return TreeSitterQueryPredicateStep.obtain(type, valueId)
  }

  override fun createQueryPredicateStepArr(size: Int): Array<TSQueryPredicateStep?> {
    return arrayOfNulls(size)
  }

  override fun createTreeCursorNode(
      type: String?,
      name: String?,
      startByte: Int,
      endByte: Int,
  ): TSTreeCursorNode {
    return TreeSitterTreeCursorNode.obtain(type, name, startByte, endByte)
  }

  override fun createLookaheadIterator(pointer: Long): TSLookaheadIterator {
    return TreeSitterLookaheadIterator.obtain(pointer)
  }

  override fun createLanguage(name: String?, pointers: LongArray?): TSLanguage {
    return TreeSitterNativeLanguage.obtain(name, pointers)
  }

  override fun createString(pointer: Long, isSynchronized: Boolean): UTF16String {
    return if (isSynchronized) SynchronizedUTF16String(pointer) else UTF16String(pointer)
  }
}
