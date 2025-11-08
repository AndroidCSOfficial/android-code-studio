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
import com.tom.rv2ide.templates.android.navigation.drawer.Fragment
import com.tom.rv2ide.templates.android.navigation.drawer.NavigationDrawerSources
import com.tom.rv2ide.templates.android.navigation.drawer.ViewModel
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class NavigationDrawerActivity : Template {
  override val displayName = "Navigation drawer"
  override val templateType = Template.TemplateType.ACTIVITY

  private val projectStructBuilder = ProjectStructBuilder()
  private val activityWriter = ActivityWriter()
  private val topLevelGradleWriter = TopLevelGradleWriter()
  private val settingsGradleWriter = SettingsGradleWriter()
  private val versionCatalogWriter = VersionCatalogWriter()
  private val gradlePropertiesWriter = GradlePropertiesWriter()
  private val moduleGradleWriter = MLGradleWriter()
  private val proguardRulesWriter = ProguardRulesWriter()

  private val primaryModule = ":app"
  // Asset paths
  private val ASSETS_BASE_PATH = NavigationDrawerActivity::class.simpleName
  private val ASSETS_RESOURCES_PATH = "$ASSETS_BASE_PATH/resources"
  private val ASSETS_GRADLE_PATH = "$ASSETS_BASE_PATH/gradle"

  private val TAG = NavigationDrawerActivity::class.simpleName

  override suspend fun create(
      context: Context,
      listener: AtcInterface.TemplateCreationListener?,
      options: TemplateOptions,
  ) =
      withContext(Dispatchers.IO) {
        try {
          Log.d(TAG, "create() called - START")

          // Show toast on main thread
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "Creating Navigation drawer Activity...", Toast.LENGTH_SHORT)
                .show()
          }

          val packageHelper =
              PackageHelper.createForProject(context, options.projectName.lowercase() + "_project")
          packageHelper.setPackageIdBlocking(options.packageId)

          val sdkHelper = SdkVersionHelper.getInstance(context)
          sdkHelper.setAllSdkVersionsBlocking(options.minSdk, 34, 34)

          val projectRoot = File(options.saveLocation, options.projectName)
          Log.d(TAG, "Project root: ${projectRoot.absolutePath}")

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
            Log.e(TAG, "Structure creation failed: ${structResult.message}")
            withContext(Dispatchers.Main) {
              listener?.onTemplateCreated(false, structResult.message)
            }
            return@withContext
          }

          Log.d(TAG, "Project structure created successfully")

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
                  catalogVersion {
                    name("vectordrawable")
                    version(ANDROIDX_VECTORDRAWABLE)
                  },
                  catalogVersion {
                    name("lifecycleLivedataKtx")
                    version(ANDROIDX_LIFECYCLE_LIVE_DATA_KTX)
                  },
                  catalogVersion {
                    name("lifecycleViewmodelKtx")
                    version(ANDROIDX_LIFE_CYCLE_VIEWMODEL_KTX)
                  },
                  catalogVersion {
                    name("navigationFragmentKtx")
                    version(ANDROIDX_NAVIGATION_FRAGMENT_KTX)
                  },
                  catalogVersion {
                    name("navigationUiKtx")
                    version(ANDROIDX_NAVIGATION_UI_KTX)
                  },
              )

          val plugins =
              listOf(
                  catalogPlugin {
                    alias("android-application")
                    id("com.android.application")
                    versionRef("agp")
                  },
                  catalogPlugin {
                    alias("kotlin-android")
                    id("org.jetbrains.kotlin.android")
                    versionRef("kotlin")
                  },
              )

          val libraries =
              listOf(
                  catalogLibrary {
                    alias("androidx-core-ktx")
                    group("androidx.core")
                    name("core-ktx")
                    versionRef("coreKtx")
                  },
                  catalogLibrary {
                    alias("androidx-appcompat")
                    group("androidx.appcompat")
                    name("appcompat")
                    versionRef("appcompat")
                  },
                  catalogLibrary {
                    alias("material")
                    group("com.google.android.material")
                    name("material")
                    versionRef("material")
                  },
                  catalogLibrary {
                    alias("androidx-constraintlayout")
                    group("androidx.constraintlayout")
                    name("constraintlayout")
                    versionRef("constraintlayout")
                  },
                  catalogLibrary {
                    alias("androidx-vectordrawable")
                    group("androidx.vectordrawable")
                    name("vectordrawable")
                    versionRef("vectordrawable")
                  },
                  catalogLibrary {
                    alias("androidx-lifecycle-livedata-ktx")
                    group("androidx.lifecycle")
                    name("lifecycle-livedata-ktx")
                    versionRef("lifecycleLivedataKtx")
                  },
                  catalogLibrary {
                    alias("androidx-lifecycle-viewmodel-ktx")
                    group("androidx.lifecycle")
                    name("lifecycle-viewmodel-ktx")
                    versionRef("lifecycleViewmodelKtx")
                  },
                  catalogLibrary {
                    alias("androidx-navigation-fragment-ktx")
                    group("androidx.navigation")
                    name("navigation-fragment-ktx")
                    versionRef("navigationFragmentKtx")
                  },
                  catalogLibrary {
                    alias("androidx-navigation-ui-ktx")
                    group("androidx.navigation")
                    name("navigation-ui-ktx")
                    versionRef("navigationUiKtx")
                  },
              )

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
            include(primaryModule)
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
              enableKotlinOptions(false)
            }
            namespace(packageHelper.getPackageId())
            compileSdk(PROJECTS_COMPILE_SDK_VERSION)
            defaultConfig(
                DefaultConfig(
                    applicationId = packageHelper.getPackageId(),
                    minSdk = 21,
                    targetSdk = 34,
                    versionCode = 1,
                    versionName = "1.0",
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner",
                )
            )
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
            addDependency(GradleDependency("implementation(libs.androidx.vectordrawable)"))
            addDependency(GradleDependency("implementation(libs.androidx.lifecycle.livedata.ktx)"))
            addDependency(GradleDependency("implementation(libs.androidx.lifecycle.viewmodel.ktx)"))
            addDependency(GradleDependency("implementation(libs.androidx.navigation.fragment.ktx)"))
            addDependency(GradleDependency("implementation(libs.androidx.navigation.ui.ktx)"))
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

          // Create MainActivity in selected language (content from sources)
          val mainActivityContent =
              if (options.languageType == LanguageType.KOTLIN)
                  NavigationDrawerSources.mainActivityKotlin(packageHelper.getPackageId())
              else NavigationDrawerSources.mainActivityJava(packageHelper.getPackageId())

          val activityConfig = activityConfig {
            moduleName("app")
            languageType(options.languageType)
            packageId(packageHelper.getPackageId())
            activityName("MainActivity")
            content(mainActivityContent)
          }

          activityWriter.writeToFile(projectRoot, activityConfig)

          // Copy additional resource files from assets
          copyResourceFiles(context, projectRoot)

          // Professional UI component structure setup
          val uiComponents = listOf("gallery", "home", "slideshow")
          val sourceRoot = if (options.languageType == LanguageType.KOTLIN) "kotlin" else "java"
          val packagePath = packageHelper.getPackageId().replace(".", "/")
          val moduleName = primaryModule.removePrefix(":")

          // Create UI package directories
          val uiBaseDir = File(projectRoot, "$moduleName/src/main/$sourceRoot/$packagePath/ui")
          val uiDirs =
              uiComponents.associateWith { component ->
                File(uiBaseDir, component).also { createIfNotExists(it) }
              }

          // Component configuration
          data class ComponentConfig(val fragmentName: String, val viewModelName: String)

          val componentConfigs =
              mapOf(
                  "gallery" to
                      ComponentConfig(
                          fragmentName = "GalleryFragment",
                          viewModelName = "GalleryViewModel",
                      ),
                  "home" to
                      ComponentConfig(
                          fragmentName = "HomeFragment",
                          viewModelName = "HomeViewModel",
                      ),
                  "slideshow" to
                      ComponentConfig(
                          fragmentName = "SlideshowFragment",
                          viewModelName = "SlideshowViewModel",
                      ),
              )

          // Generate all fragments and view models
          val fileExtension = if (options.languageType == LanguageType.KOTLIN) "kt" else "java"
          val packageId = packageHelper.getPackageId()

          componentConfigs.forEach { (componentKey, config) ->
            val dir = uiDirs[componentKey] ?: return@forEach

            // Create Fragment
            val fragmentContent =
                if (options.languageType == LanguageType.KOTLIN) {
                  Fragment.templateTypeKt(packageId, componentKey)
                } else {
                  Fragment.templateTypeJava(packageId, componentKey)
                }
            activityWriter.createFile(dir, config.fragmentName, fileExtension, fragmentContent)

            // Create ViewModel
            val viewModelContent =
                if (options.languageType == LanguageType.KOTLIN) {
                  ViewModel.templateTypeKt(packageId, componentKey)
                } else {
                  ViewModel.templateTypeJava(packageId, componentKey)
                }
            activityWriter.createFile(dir, config.viewModelName, fileExtension, viewModelContent)
          }

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
                          android:theme="@style/Theme.AppTheme">
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
                    <string name="navigation_drawer_open">Open navigation drawer</string>
                    <string name="navigation_drawer_close">Close navigation drawer</string>
                    <string name="nav_header_title">Android Studio</string>
                    <string name="nav_header_subtitle">android.studio@android.com</string>
                    <string name="nav_header_desc">Navigation header</string>
                    <string name="action_settings">Settings</string>
                
                    <string name="menu_home">Home</string>
                    <string name="menu_gallery">Gallery</string>
                    <string name="menu_slideshow">Slideshow</string>
                </resources>
            """
                  .trimIndent()

          val valuesDir = File(projectRoot, "app/src/main/res/values")
          activityWriter.createFile(valuesDir, "strings", "xml", stringsContent)

          // Track project
          val projectManager = ProjectManager.getInstance(context)
          val projectInfo =
              projectManager.createTemplateProjectInfo(
                  projectName = options.projectName,
                  projectDir = projectRoot.absolutePath,
                  projectType = "Navigation drawer Activity",
              )
          projectManager.addProjectBlocking(projectInfo)

          Log.d(TAG, "Project created successfully")

          withContext(Dispatchers.Main) {
            listener?.onTemplateCreated(
                true,
                "Navigation drawer Activity project created successfully at ${projectRoot.absolutePath}",
            )
            listener?.onTemplateCreated(true, "", projectRoot)
          }
        } catch (e: Exception) {
          Log.e(TAG, "Error creating project", e)
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
      Log.d(TAG, "Copying wrapper files...")

      val assetManager = context.assets

      // Copy gradlew
      copyAssetFile(context, "$ASSETS_GRADLE_PATH/gradlew", File(projectRoot, "gradlew"))
      File(projectRoot, "gradlew").setExecutable(true, false)

      // Copy gradlew.bat
      copyAssetFile(context, "$ASSETS_GRADLE_PATH/gradlew.bat", File(projectRoot, "gradlew.bat"))

      // Copy gradle/wrapper folder
      val wrapperDestDir = File(projectRoot, "gradle/wrapper")
      copyAssetFolder(context, "$ASSETS_GRADLE_PATH/wrapper", wrapperDestDir)

      Log.d(TAG, "Wrapper files copied successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error copying wrapper files", e)
      // Don't throw, just log - wrapper files are optional
    }
  }

  /** Copy resource files from assets/NavigationDrawerActivity/resources to app/src/main/res */
  private fun copyResourceFiles(context: Context, projectRoot: File) {
    try {
      Log.d(TAG, "Copying resource files...")

      val resDestDir = File(projectRoot, "app/src/main/res")
      copyAssetFolder(context, ASSETS_RESOURCES_PATH, resDestDir)

      Log.d(TAG, "Resource files copied successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Error copying resource files", e)
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
      Log.e(TAG, "Error copying asset folder: $assetPath", e)
    }
  }

  /** Copy a single file from assets to destination */
  private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
    try {
      val inputStream: InputStream = context.assets.open(assetPath)
      val outputStream = FileOutputStream(destFile)

      inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }

      Log.d(TAG, "Copied: $assetPath -> ${destFile.absolutePath}")
    } catch (e: Exception) {
      Log.e(TAG, "Error copying file: $assetPath", e)
      throw e
    }
  }
}
