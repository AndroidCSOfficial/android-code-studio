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
package com.tom.rv2ide.editor.adapters

import android.content.res.Resources
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.tom.rv2ide.editor.R
import com.tom.rv2ide.editor.databinding.LayoutCompletionItemBinding
import com.tom.rv2ide.lookup.Lookup
import com.tom.rv2ide.lsp.java.utils.JavaType
import com.tom.rv2ide.lsp.models.ClassCompletionData
import com.tom.rv2ide.lsp.models.CompletionItem as LspCompletionItem
import com.tom.rv2ide.lsp.models.CompletionItemKind.CLASS
import com.tom.rv2ide.lsp.models.CompletionItemKind.CONSTRUCTOR
import com.tom.rv2ide.lsp.models.CompletionItemKind.ENUM
import com.tom.rv2ide.lsp.models.CompletionItemKind.FIELD
import com.tom.rv2ide.lsp.models.CompletionItemKind.INTERFACE
import com.tom.rv2ide.lsp.models.CompletionItemKind.METHOD
import com.tom.rv2ide.lsp.models.MemberCompletionData
import com.tom.rv2ide.lsp.models.MethodCompletionData
import com.tom.rv2ide.preferences.internal.EditorPreferences
import com.tom.rv2ide.resources.R.string.msg_api_info_deprecated
import com.tom.rv2ide.resources.R.string.msg_api_info_removed
import com.tom.rv2ide.resources.R.string.msg_api_info_since
import com.tom.rv2ide.syntax.colorschemes.SchemeAndroidIDE
import com.tom.rv2ide.syntax.colorschemes.SchemeAndroidIDE.COMPLETION_WND_TEXT_API
import com.tom.rv2ide.syntax.colorschemes.SchemeAndroidIDE.COMPLETION_WND_TEXT_DETAIL
import com.tom.rv2ide.syntax.colorschemes.SchemeAndroidIDE.COMPLETION_WND_TEXT_LABEL
import com.tom.rv2ide.syntax.colorschemes.SchemeAndroidIDE.COMPLETION_WND_TEXT_TYPE
import com.tom.rv2ide.tasks.executeAsync
import com.tom.rv2ide.utils.customOrJBMono
import com.tom.rv2ide.xml.versions.ApiVersions
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.eclipse.jdt.core.Signature

class CompletionListAdapter : EditorCompletionAdapter() {

  override fun getItemHeight(): Int {
    return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            40f,
            Resources.getSystem().displayMetrics,
        )
        .toInt()
  }

  override fun getView(
      position: Int,
      convertView: View?,
      parent: ViewGroup?,
      isCurrentCursorPosition: Boolean,
  ): View {
    val binding =
        convertView?.let { LayoutCompletionItemBinding.bind(it) }
            ?: LayoutCompletionItemBinding.inflate(LayoutInflater.from(context), parent, false)
    val item = getItem(position) as LspCompletionItem
    val label = item.ideLabel
    val desc = item.detail
    var type: String? = item.completionKind.toString()
    val header = if (type!!.isEmpty()) "O" else type[0].toString()
    if (item.overrideTypeText != null) {
      type = item.overrideTypeText
    }
    binding.completionIconText.text = header
    binding.completionLabel.text = label
    binding.completionType.text = type
    binding.completionDetail.text = desc
    binding.completionIconText.setTypeface(
        customOrJBMono(EditorPreferences.useCustomFont),
        Typeface.BOLD,
    )
    if (desc.isEmpty()) {
      binding.completionDetail.visibility = View.GONE
    }

    binding.completionApiInfo.visibility = View.GONE

    applyColorScheme(binding, isCurrentCursorPosition)
    showApiInfoIfNeeded(item, binding.completionApiInfo)
    return binding.root
  }

  private fun applyColorScheme(binding: LayoutCompletionItemBinding, isCurrent: Boolean) {
    setItemBackground(binding, isCurrent)
    var color = getThemeColor(COMPLETION_WND_TEXT_LABEL)
    if (color != 0) {
      binding.completionLabel.setTextColor(color)
      binding.completionIconText.setTextColor(color)
    }

    color = getThemeColor(COMPLETION_WND_TEXT_DETAIL)
    if (color != 0) {
      binding.completionDetail.setTextColor(color)
    }

    color = getThemeColor(COMPLETION_WND_TEXT_API)
    if (color != 0) {
      binding.completionApiInfo.setTextColor(color)
    }

    color = getThemeColor(COMPLETION_WND_TEXT_TYPE)
    if (color != 0) {
      binding.completionType.setTextColor(color)
    }
  }

  private fun setItemBackground(binding: LayoutCompletionItemBinding, isCurrent: Boolean) {
    val color = if (isCurrent) getThemeColor(SchemeAndroidIDE.COMPLETION_WND_BG_CURRENT_ITEM) else 0

    val cornerRadius =
        binding.root.context.resources
            .getDimensionPixelSize(R.dimen.completion_window_corner_radius)
            .toFloat()

    val gd =
        GradientDrawable().apply {
          setColor(color)
          setCornerRadius(cornerRadius)
        }

    binding.root.background = gd
  }

  private fun showApiInfoIfNeeded(item: LspCompletionItem, textView: TextView) {
    executeAsync({
      if (!isValidForApiVersion(item)) {
        return@executeAsync null
      }

      val data = item.data
      val versions =
          Lookup.getDefault().lookup(ApiVersions.COMPLETION_LOOKUP_KEY) ?: return@executeAsync null
      val info =
          when (data) {
            is ClassCompletionData -> versions.classInfo(data.className)
            is MemberCompletionData -> {
              if (data is MethodCompletionData) {
                // if the member is a method
                // build the method identifier by joining the method name and the erased parameter
                // types
                // for method 'int some(String)', the identifier becomes 'some(Ljava/lang/String;)'
                // return type of the method is ignored
                versions.memberInfo(
                    data.classInfo.flatName,
                    methodIdentifier(data.memberName, data.erasedParameterTypes),
                )
              } else {
                versions.memberInfo(data.classInfo.flatName, data.memberName)
              }
            }

            else -> return@executeAsync null
          }

      val sb = StringBuilder()
      if (info!!.since > 1) {
        sb.append(textView.context.getString(msg_api_info_since, info.since))
        sb.append("\n")
      }

      if (info.removedIn > 0) {
        sb.append(textView.context.getString(msg_api_info_removed, info.removedIn))
        sb.append("\n")
      }

      if (info.deprecatedIn > 0) {
        sb.append(textView.context.getString(msg_api_info_deprecated, info.deprecatedIn))
        sb.append("\n")
      }

      return@executeAsync sb
    }) {
      if (it.isNullOrBlank()) {
        textView.visibility = View.GONE
        return@executeAsync
      }

      textView.text = it
      textView.visibility = View.VISIBLE
    }
  }

  private fun methodIdentifier(memberName: String, erasedParameterTypes: List<String>): String {
    val sb = StringBuilder()
    sb.append(memberName)
    sb.append('(')
    for (type in erasedParameterTypes) {
      if (type.length == 1 && JavaType.primitiveFor(type[0]) != null) {
        sb.append(type)
      } else {
        sb.append(Signature.createTypeSignature(type, true))
      }
    }
    sb.append(')')
    return sb.toString()
  }

  private fun isValidForApiVersion(item: LspCompletionItem?): Boolean {
    if (item == null) {
      return false
    }
    val type = item.completionKind
    val data = item.data
    return if ( // These represent a class type
        (type === CLASS ||
            type === INTERFACE ||
            type === ENUM ||

            // These represent a method type
            type === METHOD ||
            type === CONSTRUCTOR ||

            // A field type
            type === FIELD) && data != null
    ) {
      val className =
          when (data) {
            is ClassCompletionData -> data.className
            is MemberCompletionData -> data.classInfo.className
            else -> null
          }
      !className.isNullOrBlank()
    } else false
  }
}
