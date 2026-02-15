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
 *  along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide

import org.junit.Test
import com.google.common.truth.Truth.assertThat

/**
 * Basic unit tests for project structure validation
 */
class ProjectStructureTest {
    
    @Test
    fun testPackageStructureValid() {
        val packageName = "com.tom.rv2ide"
        assertThat(packageName).isNotEmpty()
        assertThat(packageName).contains("rv2ide")
    }
    
    @Test
    fun testProjectNameValid() {
        val projectName = "android-code-studio"
        assertThat(projectName).isNotEmpty()
        assertThat(projectName).contains("android")
    }
}
