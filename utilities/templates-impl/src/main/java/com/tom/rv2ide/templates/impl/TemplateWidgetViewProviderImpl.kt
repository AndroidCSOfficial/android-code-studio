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

package com.tom.rv2ide.templates.impl

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.google.auto.service.AutoService
import com.tom.rv2ide.templates.BooleanParameter
import com.tom.rv2ide.templates.CheckBoxWidget
import com.tom.rv2ide.templates.EnumParameter
import com.tom.rv2ide.templates.ITemplateWidgetViewProvider
import com.tom.rv2ide.templates.Parameter
import com.tom.rv2ide.templates.Parameter.DefaultObserver
import com.tom.rv2ide.templates.ParameterWidget
import com.tom.rv2ide.templates.SpinnerWidget
import com.tom.rv2ide.templates.StringParameter
import com.tom.rv2ide.templates.TextFieldParameter
import com.tom.rv2ide.templates.TextFieldWidget
import com.tom.rv2ide.templates.Widget
import com.tom.rv2ide.templates.impl.databinding.LayoutCheckboxBinding
import com.tom.rv2ide.templates.impl.databinding.LayoutSpinnerBinding
import com.tom.rv2ide.templates.impl.databinding.LayoutTextfieldBinding
import com.tom.rv2ide.utils.ServiceLoader
import com.tom.rv2ide.utils.SingleTextWatcher

/**
 * Default implementation of [ITemplateWidgetViewProvider].
 *
 * @author Akash Yadav
 */
@AutoService(ITemplateWidgetViewProvider::class)
class TemplateWidgetViewProviderImpl : ITemplateWidgetViewProvider {

  companion object {

    @JvmStatic private var service: ITemplateWidgetViewProvider? = null

    @JvmStatic
    fun getInstance(reload: Boolean = false): ITemplateWidgetViewProvider {
      if (reload) {
        service = null
      }
      return ServiceLoader.load(ITemplateWidgetViewProvider::class.java).findFirstOrThrow().also {
        service = it
      }
    }
  }

  override fun <T> createView(context: Context, widget: Widget<T>): View {
    if (widget is ParameterWidget<T>) {
      widget.parameter.apply {
        beforeCreateView()
        setValue(value, false)
      }
    }

    return when (widget) {
      is TextFieldWidget -> createTextField(context, widget)
      is CheckBoxWidget -> createCheckBox(context, widget)
      is SpinnerWidget -> createSpinner(context, widget)
      else -> throw IllegalArgumentException("Unknown widget type : $widget")
    }.also {
      if (widget is ParameterWidget<T>) {
        widget.parameter.afterCreateView()
      }
    }
  }

  private fun createCheckBox(context: Context, widget: CheckBoxWidget): View {
    return LayoutCheckboxBinding.inflate(LayoutInflater.from(context))
        .apply {
          val param = widget.parameter as BooleanParameter
          root.setText(param.name)
          root.isChecked = param.value

          val observer =
              object : DefaultObserver<Boolean>() {
                override fun onChanged(parameter: Parameter<Boolean>) {
                  disableAndRun { root.isChecked = param.value }
                }
              }

          root.addOnCheckedStateChangedListener { checkbox, _ ->
            observer.disableAndRun { param.setValue(checkbox.isChecked) }
          }

          param.observe(observer)
        }
        .root
  }

  private fun createTextField(context: Context, widget: TextFieldWidget): View {
    return LayoutTextfieldBinding.inflate(LayoutInflater.from(context))
        .apply {
          val param = widget.parameter as StringParameter
          android.util.Log.e(
              "TemplateWidget",
              "createTextField called for param with hint: ${param.name}",
          )
          android.util.Log.e("TemplateWidget", "Has endIcon: ${param.endIcon != null}")
          android.util.Log.e("TemplateWidget", "Has startIcon: ${param.startIcon != null}")

          val observer =
              object : DefaultObserver<String>() {
                override fun onChanged(parameter: Parameter<String>) {
                  disableAndRun { input.setText(param.value) }
                }
              }

          param.configureTextField(context, root) { value ->
            observer.disableAndRun {
              param.setValue(value)
              param.resetStartAndEndIcons(root.context, root)
            }

            val err = ConstraintVerifier.verify(value, constraints = param.constraints)

            root.isErrorEnabled = err != null
            if (err != null) {
              root.error = err
            }
          }

          input.setText(param.value)

          param.resetStartAndEndIcons(context, root)

          param.observe(observer)
        }
        .root
  }

  private fun createSpinner(context: Context, widget: SpinnerWidget<*>): View {
    return LayoutSpinnerBinding.inflate(LayoutInflater.from(context))
        .apply {
          val param = widget.parameter as EnumParameter<Enum<*>>

          val nameToEnum = mutableMapOf<String, Enum<*>>()
          val enumToName = mutableMapOf<Enum<*>, String>()
          param.value.javaClass.enumConstants?.forEach {

            // remove the elements for which the filter fails
            if (param.filter?.invoke(it) == false) {
              return@forEach
            }

            val displayName = param.displayName?.invoke(it) ?: it.name
            nameToEnum[displayName] = it
            enumToName[it] = displayName
          }

          check(nameToEnum.isNotEmpty()) {
            "Cannot retrive values for enum parameter $param with default value ${param.default}"
          }

          val array = nameToEnum.keys.toTypedArray()

          input.setAdapter(
              ArrayAdapter(
                  context,
                  androidx.appcompat.R.layout.support_simple_spinner_dropdown_item,
                  array,
              )
          )

          val defaultName = enumToName[param.default] ?: param.default.name

          root.isEnabled = nameToEnum.size > 1
          input.listSelection = array.indexOf(defaultName)
          input.setText(defaultName, false)

          val observer =
              object : DefaultObserver<Enum<*>>() {
                override fun onChanged(parameter: Parameter<Enum<*>>) {
                  (parameter as EnumParameter<*>).apply {
                    disableAndRun { input.setText(enumToName[value]) }
                  }
                }
              }

          if (param.default != nameToEnum[defaultName]) {
            // the default value may have been filtered
            // reset the value to the first item
            val first =
                checkNotNull(nameToEnum.values.firstOrNull()) {
                  "No entries available for enum parameter (all entries filtered out?)."
                }
            param.setValue(first)
          }

          param.configureTextField(context, root) {
            observer.disableAndRun {
              param.setValue(nameToEnum[it] ?: param.default)
              param.resetStartAndEndIcons(root.context, root)
            }
          }

          param.observe(observer)
        }
        .root
  }

  private inline fun TextFieldParameter<*>.configureTextField(
      context: Context,
      root: TextInputLayout,
      crossinline onTextChanged: (String) -> Unit = {},
  ) {
    root.setHint(name)
    resetStartAndEndIcons(context, root)
    root.editText!!.addTextChangedListener(
        object : SingleTextWatcher() {
          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onTextChanged(s?.toString() ?: "")
          }
        }
    )
  }

  private fun <T> TextFieldParameter<T>.resetStartAndEndIcons(
      context: Context,
      root: TextInputLayout,
  ) {
    startIcon?.let {
      root.startIconDrawable = ContextCompat.getDrawable(context, it(this))
      onStartIconClick?.let(root::setStartIconOnClickListener)
      android.util.Log.d("TemplateWidget", "Set start icon for ${this.javaClass.simpleName}")
    }

    endIcon?.let {
      val iconRes = it(this)
      val drawable = ContextCompat.getDrawable(context, iconRes)
      android.util.Log.d(
          "TemplateWidget",
          "Setting end icon: res=$iconRes, drawable=$drawable, hasClickListener=${onEndIconClick != null}",
      )
      root.endIconMode = TextInputLayout.END_ICON_CUSTOM
      root.endIconDrawable = drawable
      onEndIconClick?.let { listener ->
        android.util.Log.d("TemplateWidget", "Setting end icon click listener")
        root.setEndIconOnClickListener(listener)
      }
    }
        ?: run {
          android.util.Log.d(
              "TemplateWidget",
              "No end icon for parameter: ${this.javaClass.simpleName}",
          )
        }
  }
}
