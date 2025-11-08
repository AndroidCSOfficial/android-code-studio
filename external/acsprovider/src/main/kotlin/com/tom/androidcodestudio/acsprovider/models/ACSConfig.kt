/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.androidcodestudio.acsprovider.models

/*
 ** @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

data class ACSConfig(
    val jsonUrl: String? = null,
    val architecture: String,
    val packageId: String? = null,
    val version: String? = null,
    val getField: String? = null,
    val directUrl: String? = null,
    val shouldDownload: Boolean = false,
)
