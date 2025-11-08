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

package com.tom.rv2ide.templates.android

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.tom.androidcodestudio.project.manager.PackageHelper
import com.tom.androidcodestudio.project.manager.ProjectManager
import com.tom.androidcodestudio.project.manager.SdkVersionHelper
import com.tom.androidcodestudio.project.manager.builder.*
import com.tom.androidcodestudio.project.manager.builder.module.*
import com.tom.androidcodestudio.project.manager.builder.toplevel.*
import com.tom.rv2ide.templates.*
import com.tom.rv2ide.templates.AtcInterface
import com.tom.rv2ide.templates.android.cpp.CppSources
import com.tom.rv2ide.templates.android.etc.NativeCpp.Check.getHighestCMakeVersion
import com.tom.rv2ide.templates.android.etc.NativeCpp.Check.getHighestNdkVersion
import com.tom.rv2ide.templates.preferences.Options
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class NativeCpp : Template {
  override val displayName = "Native C++"
  override val templateType = Template.TemplateType.ACTIVITY

  private val projectStructBuilder = ProjectStructBuilder()
  private val activityWriter = ActivityWriter()
  private val topLevelGradleWriter = TopLevelGradleWriter()
  private val settingsGradleWriter = SettingsGradleWriter()
  private val versionCatalogWriter = VersionCatalogWriter()
  private val gradlePropertiesWriter = GradlePropertiesWriter()
  private val moduleGradleWriter = MLGradleWriter()
  private val proguardRulesWriter = ProguardRulesWriter()

  // Asset paths
  private val ASSETS_BASE_PATH = NativeCpp::class.simpleName
  private val ASSETS_RESOURCES_PATH = "$ASSETS_BASE_PATH/resources"
  private val ASSETS_GRADLE_PATH = "$ASSETS_BASE_PATH/gradle"

  override fun configureOptions() {
    Options.OPT_IS_NATIVE_CPP = true
    Options.OPT_BUILD_SYSTEM_USE_CMAKE = true
  }

  override suspend fun create(
      context: Context,
      listener: AtcInterface.TemplateCreationListener?,
      options: TemplateOptions,
  ) =
      withContext(Dispatchers.IO) {
        try {
          Log.d("NativeCpp", "create() called - START")

          // Show toast on main thread
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "Creating native c++...", Toast.LENGTH_SHORT).show()
          }

          val packageHelper =
              PackageHelper.createForProject(context, options.projectName.lowercase() + "_project")
          packageHelper.setPackageIdBlocking(options.packageId)

          val sdkHelper = SdkVersionHelper.getInstance(context)
          sdkHelper.setAllSdkVersionsBlocking(options.minSdk, 34, 34)

          val projectRoot = File(options.saveLocation, options.projectName)
          Log.d("NativeCpp", "Project root: ${projectRoot.absolutePath}")

          projectRoot.mkdirs()

          // Create project structure
          val structResult =
              projectStructBuilder.buildProjectStructure(
                  moduleName = "app",
                  projectType =
                      if (options.languageType == LanguageType.KOTLIN)
                          com.tom.androidcodestudio.project.manager.builder.ProjectType.KOTLIN
                      else com.tom.androidcodestudio.project.manager.builder.ProjectType.JAVA,
                  packageId = packageHelper.getPackageId(),
                  baseDir = projectRoot,
                  hasLayout = true,
              )

          if (!structResult.success) {
            Log.e("NativeCpp", "Structure creation failed: ${structResult.message}")
            withContext(Dispatchers.Main) {
              listener?.onTemplateCreated(false, structResult.message)
            }
            return@withContext
          }

          Log.d("NativeCpp", "Project structure created successfully")

          // Copy wrapper files (gradlew, gradle folder)
          copyWrapperFiles(context, projectRoot)

          // Create version catalog
          val versions =
              listOf(
                  catalogVersion {
                    name("agp")
                    version(ANDROID_GRADLE_PLUGIN_VERSION)
                  },
                  catalogVersion {
                    name("kotlin")
                    version(KOTLIN_VERSION)
                  },
                  catalogVersion {
                    name("coreKtx")
                    version(ANDROIDX_CORE_KTEXT_VERSION)
                  },
                  catalogVersion {
                    name("appcompat")
                    version(ANDROIDX_APPCOMPAT_VERSION)
                  },
                  catalogVersion {
                    name("material")
                    version(GOOGLE_MATERIAL_COMPONENTS_VERSION)
                  },
                  catalogVersion {
                    name("constraintlayout")
                    version(ANDROIDX_CONSTRAINTLAYOUT_VERSION)
                  },
              )

          val plugins = buildList {
            add(
                catalogPlugin {
                  alias("android-application")
                  id("com.android.application")
                  versionRef("agp")
                }
            )
            if (options.languageType == LanguageType.KOTLIN) {
              add(
                  catalogPlugin {
                    alias("kotlin-android")
                    id("org.jetbrains.kotlin.android")
                    versionRef("kotlin")
                  }
              )
            }
          }

          val libraries = buildList {
            if (options.languageType == LanguageType.KOTLIN) {
              add(
                  catalogLibrary {
                    alias("androidx-core-ktx")
                    group("androidx.core")
                    name("core-ktx")
                    versionRef("coreKtx")
                  }
              )
            } else {
              add(
                  catalogLibrary {
                    alias("androidx-core")
                    group("androidx.core")
                    name("core")
                    versionRef("coreKtx")
                  }
              )
            }
            add(
                catalogLibrary {
                  alias("androidx-appcompat")
                  group("androidx.appcompat")
                  name("appcompat")
                  versionRef("appcompat")
                }
            )
            add(
                catalogLibrary {
                  alias("material")
                  group("com.google.android.material")
                  name("material")
                  versionRef("material")
                }
            )
            add(
                catalogLibrary {
                  alias("androidx-constraintlayout")
                  group("androidx.constraintlayout")
                  name("constraintlayout")
                  versionRef("constraintlayout")
                }
            )
          }

          val gradleDir = File(projectRoot, "gradle")
          gradleDir.mkdirs()
          versionCatalogWriter.writeToFile(gradleDir, versions, plugins, libraries)

          // Create top-level build.gradle.kts
          val topLevelPlugins = buildList {
            add(
                com.tom.androidcodestudio.project.manager.builder.toplevel.GradlePlugin(
                    id = "android.application",
                    version = null,
                    apply = false,
                    useAlias = true,
                )
            )
            if (options.languageType == LanguageType.KOTLIN) {
              add(
                  com.tom.androidcodestudio.project.manager.builder.toplevel.GradlePlugin(
                      id = "kotlin.android",
                      version = null,
                      apply = false,
                      useAlias = true,
                  )
              )
            }
          }
          topLevelGradleWriter.writeToFile(
              projectRoot,
              if (options.useKts)
                  com.tom.androidcodestudio.project.manager.builder.toplevel.GradleFileType.KTS
              else com.tom.androidcodestudio.project.manager.builder.toplevel.GradleFileType.GROOVY,
              topLevelPlugins,
          )

          // Create settings.gradle.kts
          val settingsConfig = settingsGradleConfig {
            pluginManagement(RepositoryPresets.STANDARD_KTS)
            dependencyResolution(RepositoryPresets.DEPENDENCY_RESOLUTION_KTS)
            rootProjectName(options.projectName)
            include(":app")
          }
          settingsGradleWriter.writeToFile(projectRoot, SettingsGradleFileType.KTS, settingsConfig)

          // Create gradle.properties
          gradlePropertiesWriter.writeToFile(projectRoot, GradlePropertiesPresets.STANDARD_ANDROID)

          // Create module build.gradle.kts
          val moduleConfig = moduleGradleConfig {
            addPlugin(
                com.tom.androidcodestudio.project.manager.builder.module.GradlePlugin(
                    "alias",
                    "libs.plugins.android.application",
                )
            )
            if (options.languageType == LanguageType.KOTLIN) {
              addPlugin(
                  com.tom.androidcodestudio.project.manager.builder.module.GradlePlugin(
                      "alias",
                      "libs.plugins.kotlin.android",
                  )
              )
            } else {
              // Disable Kotlin Options
              enableKotlinOptions(false)
            }
            namespace(packageHelper.getPackageId())
            compileSdk(PROJECTS_COMPILE_SDK_VERSION)
            ndkVersion(getHighestNdkVersion() ?: "")

            defaultConfig(
                DefaultConfig(
                    applicationId = packageHelper.getPackageId(),
                    minSdk = 21,
                    targetSdk = 34,
                    versionCode = 1,
                    versionName = "1.0",
                )
            )
            if (!Options.OPT_BUILD_SYSTEM_USE_CMAKE) {
              externalNativeBuild(
                  ExternalNativeBuild(ndkBuild = NdkBuildConfig(path = "src/main/cpp/Android.mk"))
              )
            } else {
              externalNativeBuild(
                  ExternalNativeBuild(
                      cmake =
                          CMakeConfig(
                              path = "src/main/cpp/CMakeLists.txt",
                              version = getHighestCMakeVersion(),
                          )
                  )
              )
            }
            addBuildFeature(BuildFeature.VIEW_BINDING)
            javaVersion(JavaVersion.VERSION_17)
            if (options.languageType == LanguageType.KOTLIN) {
              addDependency(GradleDependency("implementation(libs.androidx.core.ktx)"))
            } else {
              addDependency(GradleDependency("implementation(libs.androidx.core)"))
            }
            addDependency(GradleDependency("implementation(libs.androidx.appcompat)"))
            addDependency(GradleDependency("implementation(libs.material)"))
            addDependency(GradleDependency("implementation(libs.androidx.constraintlayout)"))
          }

          val appDir = File(projectRoot, "app")
          moduleGradleWriter.writeToFile(
              appDir,
              if (options.useKts)
                  com.tom.androidcodestudio.project.manager.builder.module.GradleFileType.KTS
              else com.tom.androidcodestudio.project.manager.builder.module.GradleFileType.GROOVY,
              moduleConfig,
          )

          // Create proguard-rules.pro
          proguardRulesWriter.writeToFile(appDir, ProguardRulesPresets.DEFAULT_ANDROID)

          // Copy additional resource files from assets
          copyResourceFiles(context, projectRoot)

          val mainActivityContent =
              if (options.languageType == LanguageType.KOTLIN)
                  CppSources.mainActivityKotlin(packageHelper.getPackageId())
              else CppSources.mainActivityJava(packageHelper.getPackageId())

          val activityConfig = activityConfig {
            moduleName("app")
            languageType(options.languageType)
            packageId(packageHelper.getPackageId())
            activityName("MainActivity")
            content(mainActivityContent)
          }

          activityWriter.writeToFile(projectRoot, activityConfig)

          // Create layout file
          val layoutContent =
              """
                  <?xml version="1.0" encoding="utf-8"?>
                  <androidx.constraintlayout.widget.ConstraintLayout
                      xmlns:android="http://schemas.android.com/apk/res/android"
                      xmlns:app="http://schemas.android.com/apk/res-auto"
                      xmlns:tools="http://schemas.android.com/tools"
                      android:layout_width="match_parent"
                      android:layout_height="match_parent"
                      tools:context=".MainActivity">
                      
                      <TextView
                          android:id="@+id/textView"
                          android:layout_width="wrap_content"
                          android:layout_height="wrap_content"
                          android:text="Hello World!"
                          android:textSize="24sp"
                          app:layout_constraintBottom_toBottomOf="parent"
                          app:layout_constraintEnd_toEndOf="parent"
                          app:layout_constraintStart_toStartOf="parent"
                          app:layout_constraintTop_toTopOf="parent" />
                          
                  </androidx.constraintlayout.widget.ConstraintLayout>
              """
                  .trimIndent()

          val layoutDir = File(projectRoot, "app/src/main/res/layout")
          activityWriter.createFile(layoutDir, "activity_main", "xml", layoutContent)

          // Create AndroidManifest.xml
          val manifestContent =
              """
                  <?xml version="1.0" encoding="utf-8"?>
                  <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                      <uses-permission 
                          android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />                
                      <application
                          android:allowBackup="true"
                          android:icon="@mipmap/ic_launcher"
                          android:label="@string/app_name"
                          android:roundIcon="@mipmap/ic_launcher_round"
                          android:supportsRtl="true"
                          android:theme="@style/AppTheme">
                          <activity
                              android:name=".MainActivity"
                              android:exported="true">
                              <intent-filter>
                                  <action android:name="android.intent.action.MAIN" />
                                  <category android:name="android.intent.category.LAUNCHER" />
                              </intent-filter>
                          </activity>
                      </application>
                      
                  </manifest>
              """
                  .trimIndent()

          val manifestDir = File(projectRoot, "app/src/main")
          activityWriter.createFile(manifestDir, "AndroidManifest", "xml", manifestContent)

          // Create strings.xml
          val stringsContent =
              """
                <resources>
                    <string name="app_name">${options.projectName}</string>
                </resources>
            """
                  .trimIndent()

          // Native C++ files
          val nativeDir = File(projectRoot, "app/src/main/cpp")
          createIfNotExists(nativeDir)

          // Create native-lib.cpp/c
          val nativeLibContent =
              when (Options.OPT_NATIVE_LANGUAGE) {
                "cpp" -> CppSources.jniNativelib(options.packageId)
                else -> CppSources.jniNativelibInC(options.packageId)
              }
          activityWriter.createFile(
              nativeDir,
              "native-lib",
              Options.OPT_NATIVE_LANGUAGE,
              nativeLibContent,
          )

          // Create build system files based on option
          if (Options.OPT_BUILD_SYSTEM_USE_CMAKE) {
            // CMake build system
            val cmakeContent = CppSources.jniBuildSystemCMake()
            activityWriter.createFile(nativeDir, "CMakeLists", "txt", cmakeContent)
          } else {
            // NDK build system
            val androidMkContent = CppSources.jniBuildSystemNdk()
            val applicationMkContent = CppSources.jniBuildSystemApplicatNdk()
            activityWriter.createFile(nativeDir, "Android", "mk", androidMkContent)
            activityWriter.createFile(nativeDir, "Application", "mk", applicationMkContent)
          }

          val valuesDir = File(projectRoot, "app/src/main/res/values")
          activityWriter.createFile(valuesDir, "strings", "xml", stringsContent)

          // Track project
          val projectManager = ProjectManager.getInstance(context)
          val projectInfo =
              projectManager.createTemplateProjectInfo(
                  projectName = options.projectName,
                  projectDir = projectRoot.absolutePath,
                  projectType = "Native cpp Activity",
              )
          projectManager.addProjectBlocking(projectInfo)

          Log.d("NativeCpp", "Project created successfully")

          withContext(Dispatchers.Main) {
            listener?.onTemplateCreated(
                true,
                "Native C++ project created successfully at ${projectRoot.absolutePath}",
            )
            listener?.onTemplateCreated(true, "", projectRoot)
          }
        } catch (e: Exception) {
          Log.e("NativeCpp", "Error creating project", e)
          withContext(Dispatchers.Main) {
            listener?.onTemplateCreated(false, "Error creating project: ${e.message}")
          }
        }
      }

  /**
   * Copy wrapper files (gradlew, gradlew.bat, gradle/wrapper folder) from assets to project root
   */
  private fun copyWrapperFiles(context: Context, projectRoot: File) {
    try {
      Log.d("NativeCpp", "Copying wrapper files...")

      val assetManager = context.assets

      // Copy gradlew
      copyAssetFile(context, "$ASSETS_GRADLE_PATH/gradlew", File(projectRoot, "gradlew"))
      File(projectRoot, "gradlew").setExecutable(true, false)

      // Copy gradlew.bat
      copyAssetFile(context, "$ASSETS_GRADLE_PATH/gradlew.bat", File(projectRoot, "gradlew.bat"))

      // Copy gradle/wrapper folder
      val wrapperDestDir = File(projectRoot, "gradle/wrapper")
      copyAssetFolder(context, "$ASSETS_GRADLE_PATH/wrapper", wrapperDestDir)

      Log.d("NativeCpp", "Wrapper files copied successfully")
    } catch (e: Exception) {
      Log.e("NativeCpp", "Error copying wrapper files", e)
      // Don't throw, just log - wrapper files are optional
    }
  }

  /** Copy resource files from assets/NativeCpp/resources to app/src/main/res */
  private fun copyResourceFiles(context: Context, projectRoot: File) {
    try {
      Log.d("NativeCpp", "Copying resource files...")

      val resDestDir = File(projectRoot, "app/src/main/res")
      copyAssetFolder(context, ASSETS_RESOURCES_PATH, resDestDir)

      Log.d("NativeCpp", "Resource files copied successfully")
    } catch (e: Exception) {
      Log.e("NativeCpp", "Error copying resource files", e)
      // Don't throw, just log - additional resources are optional
    }
  }

  /** Recursively copy a folder from assets to destination */
  private fun copyAssetFolder(context: Context, assetPath: String, destDir: File) {
    try {
      val assetManager = context.assets
      val files = assetManager.list(assetPath) ?: emptyArray()

      if (files.isEmpty()) {
        // It's a file, not a directory
        copyAssetFile(context, assetPath, destDir)
      } else {
        // It's a directory
        destDir.mkdirs()

        for (fileName in files) {
          val subAssetPath = "$assetPath/$fileName"
          val subDestFile = File(destDir, fileName)
          copyAssetFolder(context, subAssetPath, subDestFile)
        }
      }
    } catch (e: Exception) {
      Log.e("NativeCpp", "Error copying asset folder: $assetPath", e)
    }
  }

  /** Copy a single file from assets to destination */
  private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
    try {
      val inputStream: InputStream = context.assets.open(assetPath)
      val outputStream = FileOutputStream(destFile)

      inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }

      Log.d("NativeCpp", "Copied: $assetPath -> ${destFile.absolutePath}")
    } catch (e: Exception) {
      Log.e("NativeCpp", "Error copying file: $assetPath", e)
      throw e
    }
  }
}

/** * Create directory if it doesn't exist */
fun createIfNotExists(file: File): Boolean {
  return try {
    if (!file.exists()) {
      file.mkdirs()
    } else {
      true
    }
  } catch (e: SecurityException) {
    false
  }
}
