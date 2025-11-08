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

package com.tom.rv2ide.inflater.internal.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import com.tom.rv2ide.inflater.InflateException
import com.tom.rv2ide.inflater.internal.ViewAdapterIndexImpl
import java.lang.reflect.Method
import org.slf4j.LoggerFactory

/** @author Akash Yadav */
object ViewFactory {

  private val log = LoggerFactory.getLogger(ViewFactory::class.java)

  fun createViewInstance(name: String, context: Context): View {
    println("ViewFactory.createViewInstance called for: $name")
    val adapter = ViewAdapterIndexImpl.INSTANCE.getViewAdapter(name)
    println("Found adapter for $name: ${adapter?.javaClass?.simpleName}")
    return try {
      if (adapter != null) {
        // Check if adapter can create the view instance
        val view = adapter.onCreateView(name, context)
        if (view != null) {
          println("Adapter created view: ${view::class.java.simpleName}")
          return view
        } else {
          println("Adapter.onCreateView returned null for $name")
        }
      } else {
        println("No adapter found for $name")
      }

      println("Falling back to reflection for $name")
      val klass = javaClass.classLoader!!.loadClass(name)
      val constructor = klass.getConstructor(Context::class.java)
      val view = constructor.newInstance(context) as View
      println("Reflection created view: ${view::class.java.simpleName}")
      view
    } catch (err: Throwable) {
      log.error("Failed to create view instance for view: {}", name, err)
      throw RuntimeException(err)
    }
  }

  fun generateLayoutParams(parent: ViewGroup): LayoutParams {
    return try {
      var clazz: Class<in ViewGroup> = parent.javaClass
      var method: Method?
      while (true) {
        try {
          method = clazz.getDeclaredMethod("generateDefaultLayoutParams")
          break
        } catch (e: Throwable) {
          /* ignored */
        }

        clazz = clazz.superclass
      }
      if (method != null) {
        method.isAccessible = true
        return method.invoke(parent) as LayoutParams
      }
      log.error("Unable to create default params for view parent: {}", parent)
      LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    } catch (th: Throwable) {
      throw InflateException("Unable to create layout params for parent: $parent", th)
    }
  }
}
