/*
 * This file is part of AndroidIDE.
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.xml.internal.versions

import com.tom.rv2ide.xml.versions.ApiVersion
import com.tom.rv2ide.xml.versions.ApiVersions

/**
 * An empty, non-restrictive implementation of [ApiVersions].
 * Returning null for class and member info removes the API level limitations/warnings.
 *
 * @author Akash Yadav
 * Modified by Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
internal class DefaultApiVersions : ApiVersions {

  override fun classInfo(name: String): ApiVersion? {
    // Return null to indicate no restrictions/API check required
    return null
  }

  override fun memberInfo(className: String, identifier: String): ApiVersion? {
    return null
  }

  fun containsClass(name: String): Boolean {
    return false
  }

  fun containsClassMember(name: String, member: String): Boolean {
    return false
  }

  fun putClass(name: String, version: ApiVersion) {
    // No-op
  }

  fun putMember(className: String, identifier: String, version: ApiVersion) {
    // No-op
  }
}