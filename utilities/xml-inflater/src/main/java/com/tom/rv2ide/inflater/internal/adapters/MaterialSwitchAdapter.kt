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

package com.tom.rv2ide.inflater.internal.adapters

import com.google.android.material.materialswitch.MaterialSwitch
import com.tom.rv2ide.annotations.inflater.ViewAdapter
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner
import com.tom.rv2ide.annotations.uidesigner.IncludeInDesigner.Group.GOOGLE
import com.tom.rv2ide.inflater.AttributeHandlerScope
import com.tom.rv2ide.inflater.models.UiWidget
import com.tom.rv2ide.resources.R.drawable
import com.tom.rv2ide.resources.R.string

/**
 * Attribute adapter for [MaterialSwitch].
 *
 * @author Akash Yadav
 */
@ViewAdapter(MaterialSwitch::class)
@IncludeInDesigner(group = GOOGLE)
open class MaterialSwitchAdapter<T : MaterialSwitch> : CompoundButtonAdapter<T>() {

  override fun createAttrHandlers(create: (String, AttributeHandlerScope<T>.() -> Unit) -> Unit) {
    super.createAttrHandlers(create)

    // MaterialSwitch specific attributes
    create("thumbIcon") { view.thumbIconDrawable = parseDrawable(context, value) }
    create("thumbIconTint") { view.thumbIconTintList = parseColorStateList(context, value) }
    create("thumbIconSize") { view.thumbIconSize = parseDimension(context, value, 0) }
    create("trackDecoration") { view.trackDecorationDrawable = parseDrawable(context, value) }
    create("trackDecorationTint") {
      view.trackDecorationTintList = parseColorStateList(context, value)
    }

    // Regular SwitchCompat attributes that also work with MaterialSwitch
    create("showText") { view.showText = parseBoolean(value) }
    create("splitTrack") { view.splitTrack = parseBoolean(value) }
    create("switchMinWidth") { view.switchMinWidth = parseDimension(context, value, 0) }
    create("switchPadding") { view.switchPadding = parseDimension(context, value, 0) }
    create("textOff") { view.textOff = parseString(value) }
    create("textOn") { view.textOn = parseString(value) }
    create("thumb") { view.thumbDrawable = parseDrawable(context, value) }
    create("thumbTextPadding") { view.thumbTextPadding = parseDimension(context, value, 0) }
    create("thumbTint") { view.thumbTintList = parseColorStateList(context, value) }
    create("thumbTintMode") { view.thumbTintMode = parsePorterDuffMode(value) }
    create("track") { view.trackDrawable = parseDrawable(context, value) }
    create("trackTint") { view.trackTintList = parseColorStateList(context, value) }
    create("trackTintMode") { view.trackTintMode = parsePorterDuffMode(value) }
  }

  override fun createUiWidgets(): List<UiWidget> {
    return listOf(
        UiWidget(MaterialSwitch::class.java, string.widget_mswitch, drawable.ic_widget_switch)
    )
  }
}
