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
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

class ComposeEmptyActivity : Template {
  override val displayName = "Compose Activity"
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
  private val ASSETS_BASE_PATH = ComposeEmptyActivity::class.simpleName
  private val ASSETS_RESOURCES_PATH = "$ASSETS_BASE_PATH/resources"
  private val ASSETS_GRADLE_PATH = "$ASSETS_BASE_PATH/gradle"

  private var projectLang = ProjectType.KOTLIN // default

  override suspend fun create(
      context: Context,
      listener: AtcInterface.TemplateCreationListener?,
      options: TemplateOptions,
  ) =
      withContext(Dispatchers.IO) {
        try {
          Log.d("ComposeEmptyActivity", "create() called - START")

          // Show toast on main thread
          withContext(Dispatchers.Main) {
            Toast.makeText(context, "Creating Compose Empty Activity...", Toast.LENGTH_SHORT).show()
          }

          val packageHelper =
              PackageHelper.createForProject(context, options.projectName.lowercase() + "_project")
          packageHelper.setPackageIdBlocking(options.packageId)

          val sdkHelper = SdkVersionHelper.getInstance(context)
          sdkHelper.setAllSdkVersionsBlocking(options.minSdk, 34, 34)

          val projectRoot = File(options.saveLocation, options.projectName)
          Log.d("ComposeEmptyActivity", "Project root: ${projectRoot.absolutePath}")

          projectRoot.mkdirs()

          // Create project structure (no layout needed for Compose)
          projectLang =
              if (options.languageType == LanguageType.KOTLIN)
                  com.tom.androidcodestudio.project.manager.builder.ProjectType.KOTLIN
              else com.tom.androidcodestudio.project.manager.builder.ProjectType.JAVA
          val structResult =
              projectStructBuilder.buildProjectStructure(
                  moduleName = "app",
                  projectType = projectLang,
                  packageId = packageHelper.getPackageId(),
                  baseDir = projectRoot,
                  hasLayout = false,
              )

          if (!structResult.success) {
            Log.e("ComposeEmptyActivity", "Structure creation failed: ${structResult.message}")
            listener?.onTemplateCreated(false, structResult.message)
            return@withContext
          }

          Log.d("ComposeEmptyActivity", "Project structure created successfully")

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
                    name("lifecycleRuntimeKtx")
                    version(ANDROIDX_LIFECYCLE_KOTLIN_EXTENSIONS)
                  },
                  catalogVersion {
                    name("activityCompose")
                    version(ANDROIDX_ACTIVITY_COMPOSE)
                  },
                  catalogVersion {
                    name("composeBom")
                    version(ANDROIDX_JETPACK_COMPOSE_LIBRARIES_BOM)
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
                  catalogPlugin {
                    alias("kotlin-compose")
                    id("org.jetbrains.kotlin.plugin.compose")
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
                    alias("androidx-lifecycle-runtime-ktx")
                    group("androidx.lifecycle")
                    name("lifecycle-runtime-ktx")
                    versionRef("lifecycleRuntimeKtx")
                  },
                  catalogLibrary {
                    alias("androidx-activity-compose")
                    group("androidx.activity")
                    name("activity-compose")
                    versionRef("activityCompose")
                  },
                  catalogLibrary {
                    alias("androidx-compose-bom")
                    group("androidx.compose")
                    name("compose-bom")
                    versionRef("composeBom")
                  },
                  catalogLibrary {
                    alias("androidx-ui")
                    group("androidx.compose.ui")
                    name("ui")
                  },
                  catalogLibrary {
                    alias("androidx-ui-graphics")
                    group("androidx.compose.ui")
                    name("ui-graphics")
                  },
                  catalogLibrary {
                    alias("androidx-ui-tooling-preview")
                    group("androidx.compose.ui")
                    name("ui-tooling-preview")
                  },
                  catalogLibrary {
                    alias("androidx-material3")
                    group("androidx.compose.material3")
                    name("material3")
                  },
                  catalogLibrary {
                    alias("androidx-ui-tooling")
                    group("androidx.compose.ui")
                    name("ui-tooling")
                  },
                  catalogLibrary {
                    alias("androidx-ui-test-manifest")
                    group("androidx.compose.ui")
                    name("ui-test-manifest")
                  },
              )

          val gradleDir = File(projectRoot, "gradle")
          gradleDir.mkdirs()
          versionCatalogWriter.writeToFile(gradleDir, versions, plugins, libraries)

          // Create top-level build.gradle.kts
          val topLevelPlugins =
              listOf(
                  com.tom.androidcodestudio.project.manager.builder.toplevel.GradlePlugin(
                      id = "android.application",
                      version = null,
                      apply = false,
                      useAlias = true,
                  ),
                  com.tom.androidcodestudio.project.manager.builder.toplevel.GradlePlugin(
                      id = "kotlin.android",
                      version = null,
                      apply = false,
                      useAlias = true,
                  ),
                  com.tom.androidcodestudio.project.manager.builder.toplevel.GradlePlugin(
                      id = "kotlin.compose",
                      version = null,
                      apply = false,
                      useAlias = true,
                  ),
              )
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
            addPlugin(
                com.tom.androidcodestudio.project.manager.builder.module.GradlePlugin(
                    "alias",
                    "libs.plugins.kotlin.android",
                )
            )
            addPlugin(
                com.tom.androidcodestudio.project.manager.builder.module.GradlePlugin(
                    "alias",
                    "libs.plugins.kotlin.compose",
                )
            )
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
            addBuildFeature(BuildFeature.COMPOSE)
            javaVersion(JavaVersion.VERSION_17)
            addDependency(GradleDependency("implementation(libs.androidx.core.ktx)"))
            addDependency(GradleDependency("implementation(libs.androidx.lifecycle.runtime.ktx)"))
            addDependency(GradleDependency("implementation(libs.androidx.activity.compose)"))
            addDependency(GradleDependency("implementation(platform(libs.androidx.compose.bom))"))
            addDependency(GradleDependency("implementation(libs.androidx.ui)"))
            addDependency(GradleDependency("implementation(libs.androidx.ui.graphics)"))
            addDependency(GradleDependency("implementation(libs.androidx.ui.tooling.preview)"))
            addDependency(GradleDependency("implementation(libs.androidx.material3)"))
            addDependency(GradleDependency("debugImplementation(libs.androidx.ui.tooling)"))
            addDependency(GradleDependency("debugImplementation(libs.androidx.ui.test.manifest)"))
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

          // Create MainActivity with Compose
          val mainActivityContent =
              """
                package ${packageHelper.getPackageId()}
                
                import android.os.Bundle
                import androidx.activity.ComponentActivity
                import androidx.activity.compose.setContent
                import androidx.activity.enableEdgeToEdge
                import androidx.compose.foundation.layout.fillMaxSize
                import androidx.compose.foundation.layout.padding
                import androidx.compose.material3.Scaffold
                import androidx.compose.material3.Text
                import androidx.compose.runtime.Composable
                import androidx.compose.ui.Modifier
                import androidx.compose.ui.tooling.preview.Preview
                import androidx.compose.foundation.layout.Box
                import androidx.compose.foundation.layout.Column
                import androidx.compose.foundation.layout.Row
                import androidx.compose.foundation.layout.Arrangement
                import androidx.compose.ui.Alignment
                import ${packageHelper.getPackageId()}.ui.theme.ComposeEmptyActivityTheme
                
                class MainActivity : ComponentActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        enableEdgeToEdge()
                        setContent {
                            ComposeEmptyActivityTheme {
                                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                                    Greeting(
                                        name = "Android",
                                        modifier = Modifier.padding(innerPadding)
                                    )
                                }
                            }
                        }
                    }
                }
                
                @Composable
                fun Greeting(name: String, modifier: Modifier = Modifier) {
                    Row(
                        modifier = modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hello ${'$'}name!"
                        )
                    }
                }
                
                @Preview(showBackground = true)
                @Composable
                fun GreetingPreview() {
                    ComposeEmptyActivityTheme {
                        Greeting("Android")
                    }
                }
            """
                  .trimIndent()

          val activityConfig = activityConfig {
            moduleName("app")
            languageType(options.languageType)
            packageId(packageHelper.getPackageId())
            activityName("MainActivity")
            content(mainActivityContent)
          }

          activityWriter.writeToFile(projectRoot, activityConfig)

          // Create Compose Theme files
          createComposeTheme(projectRoot, packageHelper.getPackageId())

          // Copy additional resource files from assets
          copyResourceFiles(context, projectRoot)

          // Create AndroidManifest.xml (without package attribute)
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
                          android:theme="@style/Theme.ComposeEmptyActivity">
                          <activity
                              android:name=".MainActivity"
                              android:exported="true"
                              android:theme="@style/Theme.ComposeEmptyActivity">
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

          val valuesDir = File(projectRoot, "app/src/main/res/values")
          activityWriter.createFile(valuesDir, "strings", "xml", stringsContent)

          // Create themes.xml
          val themesContent =
              """
                  <resources xmlns:tools="http://schemas.android.com/tools">
                      <style name="Theme.ComposeEmptyActivity" parent="android:Theme.Material.Light.NoActionBar" />
                  </resources>
              """
                  .trimIndent()

          activityWriter.createFile(valuesDir, "themes", "xml", themesContent)

          // Track project
          val projectManager = ProjectManager.getInstance(context)
          val projectInfo =
              projectManager.createTemplateProjectInfo(
                  projectName = options.projectName,
                  projectDir = projectRoot.absolutePath,
                  projectType = "Compose Empty Activity",
              )
          projectManager.addProjectBlocking(projectInfo)

          Log.d("ComposeEmptyActivity", "Project created successfully")

          withContext(Dispatchers.Main) {
            listener?.onTemplateCreated(
                true,
                "Compose Empty Activity project created successfully at ${projectRoot.absolutePath}",
            )
            listener?.onTemplateCreated(true, "", projectRoot)
          }
        } catch (e: Exception) {
          Log.e("ComposeEmptyActivity", "Error creating project", e)
          withContext(Dispatchers.Main) {
            listener?.onTemplateCreated(false, "Error creating project: ${e.message}")
          }
        }
      }

  /** Create Compose theme files (Color.kt, Type.kt, Theme.kt) */
  private fun createComposeTheme(projectRoot: File, packageId: String) {
    val themeDir =
        File(projectRoot, "app/src/main/$projectLang/${packageId.replace('.', '/')}/ui/theme")
    themeDir.mkdirs()

    // Color.kt
    val colorContent =
        """
            package ${packageId}.ui.theme
            
            import androidx.compose.ui.graphics.Color
            
            val Purple80 = Color(0xFFD0BCFF)
            val PurpleGrey80 = Color(0xFFCCC2DC)
            val Pink80 = Color(0xFFEFB8C8)
            
            val Purple40 = Color(0xFF6650a4)
            val PurpleGrey40 = Color(0xFF625b71)
            val Pink40 = Color(0xFF7D5260)
        """
            .trimIndent()

    File(themeDir, "Color.kt").writeText(colorContent)

    // Type.kt
    val typeContent =
        """
            package ${packageId}.ui.theme
            
            import androidx.compose.material3.Typography
            import androidx.compose.ui.text.TextStyle
            import androidx.compose.ui.text.font.FontFamily
            import androidx.compose.ui.text.font.FontWeight
            import androidx.compose.ui.unit.sp
            
            val Typography = Typography(
                bodyLarge = TextStyle(
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    letterSpacing = 0.5.sp
                )
            )
        """
            .trimIndent()

    File(themeDir, "Type.kt").writeText(typeContent)

    // Theme.kt
    val themeContent =
        """
            package ${packageId}.ui.theme
            
            import android.os.Build
            import androidx.compose.foundation.isSystemInDarkTheme
            import androidx.compose.material3.MaterialTheme
            import androidx.compose.material3.darkColorScheme
            import androidx.compose.material3.dynamicDarkColorScheme
            import androidx.compose.material3.dynamicLightColorScheme
            import androidx.compose.material3.lightColorScheme
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.platform.LocalContext
            
            private val DarkColorScheme = darkColorScheme(
                primary = Purple80,
                secondary = PurpleGrey80,
                tertiary = Pink80
            )
            
            private val LightColorScheme = lightColorScheme(
                primary = Purple40,
                secondary = PurpleGrey40,
                tertiary = Pink40
            )
            
            @Composable
            fun ComposeEmptyActivityTheme(
                darkTheme: Boolean = isSystemInDarkTheme(),
                dynamicColor: Boolean = true,
                content: @Composable () -> Unit
            ) {
                val colorScheme = when {
                    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                        val context = LocalContext.current
                        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
                    }
                    darkTheme -> DarkColorScheme
                    else -> LightColorScheme
                }
                
                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = Typography,
                    content = content
                )
            }
        """
            .trimIndent()

    File(themeDir, "Theme.kt").writeText(themeContent)

    Log.d("ComposeEmptyActivity", "Compose theme files created")
  }

  /**
   * Copy wrapper files (gradlew, gradlew.bat, gradle/wrapper folder) from assets to project root
   */
  private fun copyWrapperFiles(context: Context, projectRoot: File) {
    try {
      Log.d("ComposeEmptyActivity", "Copying wrapper files...")

      val assetManager = context.assets

      // Copy gradlew
      copyAssetFile(context, "$ASSETS_GRADLE_PATH/gradlew", File(projectRoot, "gradlew"))
      File(projectRoot, "gradlew").setExecutable(true, false)

      // Copy gradlew.bat
      copyAssetFile(context, "$ASSETS_GRADLE_PATH/gradlew.bat", File(projectRoot, "gradlew.bat"))

      // Copy gradle/wrapper folder
      val wrapperDestDir = File(projectRoot, "gradle/wrapper")
      copyAssetFolder(context, "$ASSETS_GRADLE_PATH/wrapper", wrapperDestDir)

      Log.d("ComposeEmptyActivity", "Wrapper files copied successfully")
    } catch (e: Exception) {
      Log.e("ComposeEmptyActivity", "Error copying wrapper files", e)
      // Don't throw, just log - wrapper files are optional
    }
  }

  /** Copy resource files from assets/ComposeEmptyActivity/resources to app/src/main/res */
  private fun copyResourceFiles(context: Context, projectRoot: File) {
    try {
      Log.d("ComposeEmptyActivity", "Copying resource files...")

      val resDestDir = File(projectRoot, "app/src/main/res")
      copyAssetFolder(context, ASSETS_RESOURCES_PATH, resDestDir)

      Log.d("ComposeEmptyActivity", "Resource files copied successfully")
    } catch (e: Exception) {
      Log.e("ComposeEmptyActivity", "Error copying resource files", e)
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
      Log.e("ComposeEmptyActivity", "Error copying asset folder: $assetPath", e)
    }
  }

  /** Copy a single file from assets to destination */
  private fun copyAssetFile(context: Context, assetPath: String, destFile: File) {
    try {
      val inputStream: InputStream = context.assets.open(assetPath)
      val outputStream = FileOutputStream(destFile)

      inputStream.use { input -> outputStream.use { output -> input.copyTo(output) } }

      Log.d("ComposeEmptyActivity", "Copied: $assetPath -> ${destFile.absolutePath}")
    } catch (e: Exception) {
      Log.e("ComposeEmptyActivity", "Error copying file: $assetPath", e)
      throw e
    }
  }
}
