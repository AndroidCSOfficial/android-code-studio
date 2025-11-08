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

package com.tom.rv2ide.lsp.xml.providers.completion.layout

import com.android.aaptcompiler.ResourcePathData
import com.tom.rv2ide.lookup.Lookup
import com.tom.rv2ide.lsp.api.ICompletionProvider
import com.tom.rv2ide.lsp.models.CompletionItem
import com.tom.rv2ide.lsp.models.CompletionParams
import com.tom.rv2ide.lsp.models.CompletionResult
import com.tom.rv2ide.lsp.models.MatchLevel.NO_MATCH
import com.tom.rv2ide.lsp.xml.providers.completion.match
import com.tom.rv2ide.lsp.xml.utils.XmlUtils.NodeType
import com.tom.rv2ide.projects.ModuleProject
import com.tom.rv2ide.utils.ClassTrie
import com.tom.rv2ide.xml.internal.widgets.DefaultWidgetTable
import com.tom.rv2ide.xml.widgets.WidgetTable
import org.eclipse.lemminx.dom.DOMDocument

/**
 * Completes tags from
 *
 * @author Akash Yadav
 */
class QualifiedTagCompleter(provider: ICompletionProvider) : LayoutTagCompletionProvider(provider) {

  override fun doComplete(
    params: CompletionParams,
    pathData: ResourcePathData,
    document: DOMDocument,
    type: NodeType,
    prefix: String
  ): CompletionResult {
    val result = mutableListOf<CompletionItem>()
    val (widgets, module) = doLookup()
    var fqn = prefix
    if (prefix.endsWith('.')) {
      fqn = fqn.substringBeforeLast('.')
    }

    widgets.getNode(name = fqn, createIfNotPresent = false)?.children?.values?.forEach {
      val qualifiedName = "$fqn.${it.name}"
      val match = match(it.name, qualifiedName, prefix)
      result.add(createTagCompletionItem(it.name, qualifiedName, match))
    }

    addFromTrie(module.compileClasspathClasses, fqn, prefix, result)
    addFromTrie(module.compileJavaSourceClasses, fqn, prefix, result)

    return CompletionResult(result)
  }

  private fun addFromTrie(
    trie: ClassTrie,
    fqn: String,
    prefix: String,
    result: MutableList<CompletionItem>
  ) {
    val node = trie.findNode(fqn) ?: trie.findNode(fqn.substringBeforeLast('.')) ?: return
    node.children.values.forEach {
      val match = match(it.name, it.qualifiedName, prefix)
      if (match == NO_MATCH) {
        return@forEach
      }
      
      result.add(createTagCompletionItem(it.name, it.qualifiedName, match))
    }
  }

  private fun doLookup(): Pair<DefaultWidgetTable, ModuleProject> {
    val widgets =
      Lookup.getDefault().lookup(WidgetTable.COMPLETION_LOOKUP_KEY)
        ?: throw IllegalStateException("No widget table provided")
    val module =
      Lookup.getDefault().lookup(ModuleProject.COMPLETION_MODULE_KEY)
        ?: throw IllegalStateException("No module project provided")
    return widgets as DefaultWidgetTable to module
  }
}
