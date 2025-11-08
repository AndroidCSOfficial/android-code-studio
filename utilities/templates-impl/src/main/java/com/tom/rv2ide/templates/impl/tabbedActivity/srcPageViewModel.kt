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

package com.tom.rv2ide.templates.impl.tabbedActivity

import com.tom.rv2ide.templates.base.AndroidModuleTemplateBuilder

internal fun AndroidModuleTemplateBuilder.tabbedPageViewModelSrcKt() =
    """
package ${data.packageName}.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.ViewModel

class PageViewModel : ViewModel() {

    private val _index = MutableLiveData<Int>()
    val text: LiveData<String> = _index.map {
        "Hello world from section: ${'$'}it"
    }

    fun setIndex(index: Int) {
        _index.value = index
    }
}
"""
        .trim()

internal fun AndroidModuleTemplateBuilder.tabbedPageViewModelSrcJava() =
    """
package ${data.packageName}.ui.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class PageViewModel extends ViewModel {

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private MediatorLiveData<String> mText = new MediatorLiveData<>();

    public PageViewModel() {
        mText.addSource(mIndex, index -> {
            if (index != null) {
                mText.setValue("Hello world from section: " + index);
            }
        });
    }

    public void setIndex(int index) {
        mIndex.setValue(index);
    }

    public LiveData<String> getText() {
        return mText;
    }
}
"""
        .trim()
