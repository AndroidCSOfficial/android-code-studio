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

package com.tom.rv2ide.plugins.util

import com.tom.rv2ide.build.config.AUTH_PASS
import com.tom.rv2ide.build.config.AUTH_USER
import com.tom.rv2ide.build.config.KEY_BIN
import com.tom.rv2ide.build.config.KEY_URL
import org.gradle.api.Project
import com.tom.rv2ide.build.config.signingKey
import org.gradle.api.invocation.Gradle
import java.util.Base64

/**
 * Helper class for downloading and setting up the signing key.
 *
 * @author Akash Yadav
 */
object SigningKeyUtils {
  
  private val _warned = mutableMapOf<String, Boolean>()

  internal fun Project.getEnvOrProp(key: String, warn: Boolean = true): String? {
    var value: String? = System.getenv(key)
    if (value.isNullOrBlank()) {
      value = project.properties[key] as? String?
    }

    if (value.isNullOrBlank()) {
      if (warn && _warned.putIfAbsent(key, true) != true) {
        logger.warn("$key is not set. Debug key will be used to sign the APK")
      }
      return null
    }
    return value
  }

}