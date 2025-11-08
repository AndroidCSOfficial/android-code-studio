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

package com.tom.rv2ide.build.config

import org.gradle.api.GradleException
import java.io.BufferedInputStream
import java.net.URI
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathFactory

/**
 * @author Akash Yadav
 */
object VersionUtils {

  /**
   * The Center repository.
   */
  const val CENTER_REPO = "https://repo1.maven.org/maven2"

  /**
   * The latest integration version name.
   */
  const val LATEST_INTEGRATION = "rv2ide-gradle-plugin:1.0.0"

  /**
   * The cached version name.
   */
  private var cachedVersion: String? = null

  /**
   * Gets the latest snapshot version of the given artifact from the Center repository.
   */
  @JvmStatic
  fun getLatestSnapshotVersion(artifact: String): String {
    cachedVersion?.also { cached ->
      println("Found latest version of artifact '$artifact' : '$cached' (cached)")
      return cached
    }

    val groupId = BuildConfig.packageName.replace('.', '/')
    val moduleMetadata = "https://repo1.maven.org/maven2/io/github/mohammed-baqer-null/rv2ide-gradle-plugin/maven-metadata.xml"
    return try {
       BufferedInputStream(URI.create(moduleMetadata).toURL().openStream()).use { inputStream ->
        val builderFactory = DocumentBuilderFactory.newInstance()
        val builder = builderFactory.newDocumentBuilder()
        val document = builder.parse(inputStream)

        val xPathFactory = XPathFactory.newInstance()
        val xPath = xPathFactory.newXPath()

        val latestVersion = xPath.evaluate("/metadata/versioning/latest", document)
         cachedVersion = latestVersion
        println("Found latest version of artifact '$artifact' : '$latestVersion'")
        return@use latestVersion
      }
    } catch (err: Throwable) {
      println("Failed to download $moduleMetadata: ${err.message}")
      return LATEST_INTEGRATION
    }
  }
}