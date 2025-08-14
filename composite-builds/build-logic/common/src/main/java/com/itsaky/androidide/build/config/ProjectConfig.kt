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

package com.itsaky.androidide.build.config

import org.gradle.api.Project

/** @author Akash Yadav */
object ProjectConfig {

  const val REPO_HOST = "github.com"
  // const val REPO_OWNER = "AndroidIDEOfficial"
  const val REPO_OWNER = "MiyazKaori"
  // const val REPO_NAME = "AndroidIDE"
  const val REPO_NAME = "AndroidIDE-R"
  const val REPO_URL = "https://$REPO_HOST/$REPO_OWNER/$REPO_NAME"
  const val SCM_GIT =
    "scm:git:git://$REPO_HOST/$REPO_OWNER/$REPO_NAME.git"
  const val SCM_SSH =
    "scm:git:ssh://git@$REPO_HOST/$REPO_OWNER/$REPO_NAME.git"

  // const val PROJECT_SITE = "https://m.androidide.com"
  const val PROJECT_SITE = REPO_URL
}

private var shouldPrintNotAGitRepoWarning = true
private var shouldPrintVersionName = true

val Project.simpleVersionName: String
  get() {
    val version = rootProject.version.toString()
    val simpleVersion = version

    if (shouldPrintVersionName) {
       logger.warn("Version name is '$simpleVersion'")
       shouldPrintVersionName = false
    }
    
    return simpleVersion
  }

private var shouldPrintVersionCode = true
val Project.projectVersionCode: Int
  get() {
    val baseVersionCode = 271
    
    val commitCount = System.getenv("IDE_COMMIT_COUNT")

    val versionCode = "$baseVersionCode$commitCount".toInt()
    
    if (shouldPrintVersionCode) {
      logger.warn("Version code is '$versionCode")
      shouldPrintVersionCode = false
    }
    
    return versionCode
  }
  
/**
 * The version name which is used to download the artifacts at runtime.
 *
 * The value varies based on the following cases :
 * - For CI and F-Droid builds: same as [publishingVersion].
 * - For local builds: `latest.integration` to make sure that Gradle downloads the latest snapshots.
 */
val Project.downloadVersion: String
  get() {
      // sometimes, when working locally, Gradle fails to download the latest snapshot version
      // this may cause issues while initializing the project in AndroidIDE
      return VersionUtils.getLatestSnapshotVersion("gradle-plugin")
  }
  