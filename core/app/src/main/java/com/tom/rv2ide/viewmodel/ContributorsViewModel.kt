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

package com.tom.rv2ide.viewmodel

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom.rv2ide.contributors.Contributor
import com.tom.rv2ide.contributors.CrowdinTranslator
import com.tom.rv2ide.contributors.CrowdinTranslators
import com.tom.rv2ide.contributors.GitHubContributor
import com.tom.rv2ide.contributors.GitHubContributors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** @author Akash Yadav */
class ContributorsViewModel : ViewModel() {

  internal val _crowdinTranslators = MutableLiveData(emptyList<CrowdinTranslator>())
  internal val _githubContributors = MutableLiveData(emptyList<GitHubContributor>())

  private val _crowdinTranslatorsLoading = MutableLiveData(false)
  private val _githubContributorsLoading = MutableLiveData(false)

  val isLoading: Boolean
    get() = _githubContributorsLoading.value!! || _crowdinTranslatorsLoading.value!!

  companion object {

    private const val CONTRIBUTORS_MAX_SIZE = 30
  }

  fun observeLoadingState(owner: LifecycleOwner, observer: Observer<Boolean>) {
    _crowdinTranslatorsLoading.observe(owner) { observer.onChanged(isLoading) }
    _githubContributorsLoading.observe(owner) { observer.onChanged(isLoading) }
  }

  fun fetchCrowdinTranslators() {
    _crowdinTranslatorsLoading.value = true
    viewModelScope.launch(Dispatchers.Default) {
      val translators = CrowdinTranslators.getAllTranslators()
      withContext(Dispatchers.Main) {
        _crowdinTranslators.value = translators.trimToMaxSize()
        _crowdinTranslatorsLoading.value = false
      }
    }
  }

  fun fetchGitHubTranslators() {
    _githubContributorsLoading.value = true
    viewModelScope.launch(Dispatchers.Default) {
      val contributors = GitHubContributors.getAllContributors()
      withContext(Dispatchers.Main) {
        _githubContributors.value = contributors.trimToMaxSize()
        _githubContributorsLoading.value = false
      }
    }
  }

  fun fetchAll() {
    fetchCrowdinTranslators()
    fetchGitHubTranslators()
  }

  private fun <T : Contributor> List<T>.trimToMaxSize(): List<T> {
    return if (size > CONTRIBUTORS_MAX_SIZE) {
      subList(0, CONTRIBUTORS_MAX_SIZE)
    } else {
      this
    }
  }
}
