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

import androidx.annotation.VisibleForTesting
import com.google.auto.service.AutoService
import com.tom.rv2ide.xml.versions.ApiVersion
import com.tom.rv2ide.xml.versions.ApiVersions
import com.tom.rv2ide.xml.versions.ApiVersionsRegistry
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.slf4j.LoggerFactory

/**
 * Default implementation of [ApiVersionsRegistry] that bypasses XML parsing.
 * This effectively disables API version checks and prevents parsing issues.
 *
 * @author Akash Yadav
 * Modified by Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
@AutoService(ApiVersionsRegistry::class)
@VisibleForTesting
class DefaultApiVersionsRegistry : ApiVersionsRegistry {

  private val versions = ConcurrentHashMap<String, ApiVersions>()

  companion object {
    private val log = LoggerFactory.getLogger(DefaultApiVersionsRegistry::class.java)
  }

  override var isLoggingEnabled: Boolean = true

  override fun forPlatformDir(platform: File): ApiVersions? {
    var version = versions[platform.path]
    if (version != null) {
      return version
    }

    version = readApiVersions(platform) ?: return null
    versions[platform.path] = version
    return version
  }

  private fun readApiVersions(platform: File): ApiVersions? {
    if (isLoggingEnabled) {
      log.info("API version parsing bypassed for platform dir: $platform")
    }
    return DefaultApiVersions()
  }

  override fun clear() {
    versions.clear()
  }
}