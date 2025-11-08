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


import com.android.SdkConstants
import com.tom.rv2ide.build.config.AGP_VERSION_MINIMUM
import com.tom.rv2ide.build.config.BuildConfig
import com.tom.rv2ide.build.config.ProjectConfig
import com.tom.rv2ide.build.config.VersionUtils
import com.tom.rv2ide.build.config.downloadVersion
import com.tom.rv2ide.build.config.replaceContents
import com.tom.rv2ide.build.config.simpleVersionName

plugins {
  //noinspection JavaPluginLanguageLevel
  id("java-library")
}

description = "Information about the AndroidIDE build"

val buildInfoGenDir: Provider<Directory> = project.layout.buildDirectory.dir("generated/buildInfo")
  .also { 
    val dir = it.get().asFile
    if (!dir.exists()) {
      dir.mkdirs()
    }
  }

sourceSets { getByName("main").java.srcDir(buildInfoGenDir) }

tasks.create("generateBuildInfo") {
  val buildInfoPath = "com/tom/rv2ide/buildinfo/BuildInfo.java"
  val buildInfo = buildInfoGenDir.get().file(buildInfoPath)
  val buildInfoIn = project.file("src/main/java/${buildInfoPath}.in")

  doLast {
    buildInfoIn.replaceContents(
      dest = buildInfo.asFile,
      comment = "//",
      candidates =
      arrayOf(
        "PACKAGE_NAME" to BuildConfig.packageName,
        // "MVN_GROUP_ID" to BuildConfig.packageName,
        "MVN_GROUP_ID" to "io.github.mohammed-baqer-null",

        "VERSION_NAME" to rootProject.version.toString(),
        "VERSION_NAME_SIMPLE" to rootProject.simpleVersionName,
        "VERSION_NAME_DOWNLOAD" to rootProject.downloadVersion,

        "REPO_HOST" to ProjectConfig.REPO_HOST,
        "REPO_OWNER" to ProjectConfig.REPO_OWNER,
        "REPO_NAME" to ProjectConfig.REPO_NAME,
        "PROJECT_SITE" to ProjectConfig.PROJECT_SITE,

        "AGP_VERSION_MININUM" to AGP_VERSION_MINIMUM,
        "AGP_VERSION_LATEST" to libs.versions.agp.tooling.get(),
        "AGP_VERSION_GRADLE_LATEST" to SdkConstants.GRADLE_LATEST_VERSION
      )
    )
  }
}

tasks.withType<JavaCompile> { dependsOn("generateBuildInfo") }
tasks.withType<Jar> { dependsOn("generateBuildInfo") }