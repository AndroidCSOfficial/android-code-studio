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

package com.tom.rv2ide.templates.base

import com.tom.rv2ide.templates.EMPTY_RECIPE
import com.tom.rv2ide.templates.RecipeExecutor
import com.tom.rv2ide.templates.TemplateBuilder
import com.tom.rv2ide.templates.TemplateData
import com.tom.rv2ide.templates.TemplateRecipe
import com.tom.rv2ide.templates.TemplateRecipeConfigurator
import com.tom.rv2ide.templates.TemplateRecipeFinalizer
import com.tom.rv2ide.templates.TemplateRecipeResult

sealed class PrePostRecipeTemplateBuilder<R : TemplateRecipeResult> : TemplateBuilder<R>() {

  @PublishedApi internal var preRecipe: TemplateRecipeConfigurator = {}

  @PublishedApi internal var postRecipe: TemplateRecipeFinalizer = {}

  private var _recipe: TemplateRecipe<R>? = null

  val isRecipeSet: Boolean
    get() = _recipe != null

  override var recipe: TemplateRecipe<R>?
    get() = TemplateRecipe {
      preRecipe(it)
      val result = (_recipe ?: EMPTY_RECIPE).execute(it)
      postRecipe(it)
      result
    }
    set(value) {
      _recipe = value
    }
}

/**
 * @property executor The [RecipeExecutor] instance.
 * @property data The project template data.
 */
sealed class ExecutorDataTemplateBuilder<R : TemplateRecipeResult, D : TemplateData> :
    PrePostRecipeTemplateBuilder<R>() {

  @PublishedApi internal var _executor: RecipeExecutor? = null

  @PublishedApi internal var _data: D? = null

  val executor: RecipeExecutor
    get() = checkNotNull(_executor)

  val data: D
    get() = checkNotNull(_data)
}
