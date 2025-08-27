/**
 * original author: Akash Yadav
 * modified version by Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
 
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

package com.itsaky.androidide.templates.base.modules.android

import com.itsaky.androidide.templates.Language
import com.itsaky.androidide.templates.base.ProjectTemplateBuilder
import java.io.File
import com.itsaky.androidide.templates.Language.Kotlin
import com.itsaky.androidide.templates.ModuleType
import com.itsaky.androidide.templates.base.AndroidModuleTemplateBuilder
import com.itsaky.androidide.templates.base.ModuleTemplateBuilder
import com.itsaky.androidide.templates.base.modules.dependencies

private const val compose_kotlinCompilerExtensionVersion = "1.3.2"

private val AndroidModuleTemplateBuilder.androidPlugin: String
  get() {
    return if (data.type == ModuleType.AndroidLibrary) "com.android.library"
    else "com.android.application"
  }

fun AndroidModuleTemplateBuilder.buildGradleSrc(isComposeModule: Boolean
): String {
  return if (data.useKts) buildGradleSrcKts(
    isComposeModule) else buildGradleSrcGroovy(isComposeModule)
}

internal fun ProjectTemplateBuilder.buildGradleSrcKts(): String {
  return """
    // Top-level build file where you can add configuration options common to all sub-projects/modules.
    plugins {
        id("com.android.application") version "${data.version.gradlePlugin}" apply false
        id("com.android.library") version "${data.version.gradlePlugin}" apply false
        ${ktPlugin()}     
    }

    tasks.register<Delete>("clean") {
        delete(rootProject.buildDir)
    }
  """.trimIndent()
}

internal fun ProjectTemplateBuilder.buildGradleSrcGroovy(): String {
  return """
    // Top-level build file where you can add configuration options common to all sub-projects/modules.
    plugins {
        id 'com.android.application' version '${data.version.gradlePlugin}' apply false
        id 'com.android.library' version '${data.version.gradlePlugin}' apply false
        ${ktPlugin()}     
    }

    task clean(type: Delete) {
        delete rootProject.buildDir
    }
  """.trimIndent()
}

// Module-level build.gradle functions
internal fun ProjectTemplateBuilder.moduleBuildGradleSrcKts(isComposeModule: Boolean = false): String {
  return """
    plugins {
        id("com.android.application")
        ${if (data.language == Language.Kotlin) "id(\"org.jetbrains.kotlin.android\")" else ""}
        ${if (isComposeModule) "id(\"org.jetbrains.kotlin.plugin.compose\") version \"${data.version.kotlin}\"" else ""}
    }

    android {
        namespace = "${data.packageName}"
        compileSdk = ${data.version.compileSdk}

        defaultConfig {
            applicationId = "${data.packageName}"
            minSdk = ${data.version.minSdk}
            targetSdk = ${data.version.targetSdk}
            versionCode = 1
            versionName = "1.0"

            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            ${if (hasNativeBuild()) "ndk { abiFilters += listOf(\"armeabi-v7a\", \"arm64-v8a\", \"x86\", \"x86_64\") }" else ""}
        }

        buildTypes {
            release {
                isMinifyEnabled = false
                proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            }
        }
        
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }
        
        ${if (data.language == Language.Kotlin) kotlinCompileOptions() else ""}
        ${if (isComposeModule) composeOptions() else ""}
        ${if (hasNativeBuild()) externalNativeBuildKts() else ""}
    }

    dependencies {
        // Core dependencies
        implementation("androidx.core:core-ktx:1.12.0")
        implementation("androidx.appcompat:appcompat:1.6.1")
        implementation("com.google.android.material:material:1.11.0")
        implementation("androidx.constraintlayout:constraintlayout:2.1.4")
        
        ${if (isComposeModule) composeDependencies() else ""}
        
        // Test dependencies
        testImplementation("junit:junit:4.13.2")
        androidTestImplementation("androidx.test.ext:junit:1.1.5")
        androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    }
  """.trimIndent()
}

internal fun ProjectTemplateBuilder.moduleBuildGradleSrcGroovy(isComposeModule: Boolean = false): String {
  return """
    plugins {
        id 'com.android.application'
        ${if (data.language == Language.Kotlin) "id 'org.jetbrains.kotlin.android'" else ""}
        ${if (isComposeModule) "id 'org.jetbrains.kotlin.plugin.compose' version '${data.version.kotlin}'" else ""}
    }

    android {
        namespace '${data.packageName}'
        compileSdk ${data.version.compileSdk}

        defaultConfig {
            applicationId "${data.packageName}"
            minSdk ${data.version.minSdk}
            targetSdk ${data.version.targetSdk}
            versionCode 1
            versionName "1.0"

            testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
            ${if (hasNativeBuild()) "ndk { abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86', 'x86_64' }" else ""}
        }

        buildTypes {
            release {
                minifyEnabled false
                proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            }
        }
        
        compileOptions {
            sourceCompatibility JavaVersion.VERSION_1_8
            targetCompatibility JavaVersion.VERSION_1_8
        }
        
        ${if (data.language == Language.Kotlin) kotlinCompileOptionsGroovy() else ""}
        ${if (isComposeModule) composeOptionsGroovy() else ""}
        ${if (hasNativeBuild()) externalNativeBuildGroovy() else ""}
    }

    dependencies {
        // Core dependencies
        implementation 'androidx.core:core-ktx:1.12.0'
        implementation 'androidx.appcompat:appcompat:1.6.1'
        implementation 'com.google.android.material:material:1.11.0'
        implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
        
        ${if (isComposeModule) composeDependenciesGroovy() else ""}
        
        // Test dependencies
        testImplementation 'junit:junit:4.13.2'
        androidTestImplementation 'androidx.test.ext:junit:1.1.5'
        androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
    }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.ktPlugin() = if (data.language == Language.Kotlin) {
  if (data.useKts) ktPluginKts() else ktPluginGroovy()
} else ""

private fun ProjectTemplateBuilder.ktPluginKts(): String {
  return """id("org.jetbrains.kotlin.android") version "${data.version.kotlin}" apply false"""
}

private fun ProjectTemplateBuilder.ktPluginGroovy(): String {
  return "id 'org.jetbrains.kotlin.android' version '${data.version.kotlin}' apply false"
}

private fun ProjectTemplateBuilder.hasNativeBuild(): Boolean {
  val androidMkFile = File(data.projectDir, "src/main/jni/Android.mk")
  val cmakeFile = File(data.projectDir, "src/main/cpp/CMakeLists.txt")
  return androidMkFile.exists() || cmakeFile.exists()
}

private fun ProjectTemplateBuilder.externalNativeBuildKts(): String {
  return """
        externalNativeBuild {
            ndkBuild {
                path = file("src/main/jni/Android.mk")
            }
        }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.externalNativeBuildGroovy(): String {
  return """
        externalNativeBuild {
            ndkBuild {
                path file('src/main/jni/Android.mk')
            }
        }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.kotlinCompileOptions(): String {
  return """
        kotlinOptions {
            jvmTarget = "1.8"
        }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.kotlinCompileOptionsGroovy(): String {
  return """
        kotlinOptions {
            jvmTarget '1.8'
        }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.composeOptions(): String {
  return """
        buildFeatures {
            compose = true
        }
        composeOptions {
            kotlinCompilerExtensionVersion = "1.5.8"
        }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.composeOptionsGroovy(): String {
  return """
        buildFeatures {
            compose true
        }
        composeOptions {
            kotlinCompilerExtensionVersion '1.5.8'
        }
  """.trimIndent()
}

private fun ProjectTemplateBuilder.composeDependencies(): String {
  return """
        // Compose BOM
        implementation(platform("androidx.compose:compose-bom:2024.02.00"))
        implementation("androidx.compose.ui:ui")
        implementation("androidx.compose.ui:ui-tooling-preview")
        implementation("androidx.compose.material3:material3")
        implementation("androidx.activity:activity-compose:1.8.2")
        
        // Compose testing
        androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
        androidTestImplementation("androidx.compose.ui:ui-test-junit4")
        debugImplementation("androidx.compose.ui:ui-tooling")
        debugImplementation("androidx.compose.ui:ui-test-manifest")
  """.trimIndent()
}

private fun ProjectTemplateBuilder.composeDependenciesGroovy(): String {
  return """
        // Compose BOM
        implementation platform('androidx.compose:compose-bom:2024.02.00')
        implementation 'androidx.compose.ui:ui'
        implementation 'androidx.compose.ui:ui-tooling-preview'
        implementation 'androidx.compose.material3:material3'
        implementation 'androidx.activity:activity-compose:1.8.2'
        
        // Compose testing
        androidTestImplementation platform('androidx.compose:compose-bom:2024.02.00')
        androidTestImplementation 'androidx.compose.ui:ui-test-junit4'
        debugImplementation 'androidx.compose.ui:ui-tooling'
        debugImplementation 'androidx.compose.ui:ui-test-manifest'
  """.trimIndent()
}

private fun ModuleTemplateBuilder.ktJvmTarget(): String {
  if (data.language != Kotlin) {
    return ""
  }

  return if (data.useKts) ktJvmTargetKts() else ktJvmTargetGroovy()
}

private fun ModuleTemplateBuilder.ktJvmTargetKts(): String {
  return """
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "${data.versions.javaTarget}"
}
"""
}

private fun ModuleTemplateBuilder.ktJvmTargetGroovy(): String {
  return """
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
  kotlinOptions {
    jvmTarget = "${data.versions.javaTarget}"
  }
}
"""
}

private fun AndroidModuleTemplateBuilder.ktPlugin(): String {
  if (data.language != Kotlin) {
    return ""
  }

  return if (data.useKts) ktPluginKts() else ktPluginGroovy()
}

private fun ktPluginKts(): String {
  return """id("kotlin-android")"""
}

private fun ktPluginGroovy(): String {
  return "id 'kotlin-android'"
}
