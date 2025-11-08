/**
 * original author: Akash Yadav modified version by Mohammed-baqer-null @
 * https://github.com/Mohammed-baqer-null
 * - NDK Support
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

package com.tom.rv2ide.templates.base.modules.android

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tom.rv2ide.templates.Language.Kotlin
import com.tom.rv2ide.templates.ModuleType
import com.tom.rv2ide.templates.base.AndroidModuleTemplateBuilder
import com.tom.rv2ide.templates.base.ModuleTemplateBuilder
import com.tom.rv2ide.templates.base.modules.dependencies
import com.tom.rv2ide.utils.Environment
import java.io.File

private const val compose_kotlinCompilerExtensionVersion = "1.5.11"
private const val compose_kotlinExtraPlugin = "org.jetbrains.kotlin.plugin.compose"
private const val NATIVE_LIB_NAME = "tomaslib"

public var composeExtraPluginKt: String = ""
public var composeExtraPluginGr: String = ""
public var isCompose = false

private val AndroidModuleTemplateBuilder.androidPlugin: String
  get() {
    return if (data.type == ModuleType.AndroidLibrary) "com.android.library"
    else "com.android.application"
  }

fun AndroidModuleTemplateBuilder.buildGradleSrc(
    isComposeModule: Boolean,
    context: Context? = null,
): String {
  val shouldUseNdk = data.useNdk
  val hasNativeFiles = hasNativeFiles()
  val ndkInstalled = isNdkInstalled()

  // Generate JNI template files if useNdk is true
  if (shouldUseNdk) {
    generateJniTemplateFiles()
  }

  if ((shouldUseNdk || hasNativeFiles) && !ndkInstalled && context != null) {
    showNdkNotInstalledDialog(context)
  }

  return if (data.useKts) buildGradleSrcKts(isComposeModule)
  else buildGradleSrcGroovy(isComposeModule)
}

private fun AndroidModuleTemplateBuilder.generateJniTemplateFiles() {
  val jniDir = File(data.projectDir, "src/main/jni")
  jniDir.mkdirs()

  // Generate Android.mk
  generateAndroidMk(jniDir)

  // Generate Application.mk
  generateApplicationMk(jniDir)

  // Generate tomaslib.cpp
  generateTomaslibCpp(jniDir)

  // Generate tomaslib.h
  generateTomaslibHeader(jniDir)
}

private fun AndroidModuleTemplateBuilder.generateAndroidMk(jniDir: File) {
  val androidMk = File(jniDir, "Android.mk")
  val content =
      """
LOCAL_PATH := ${'$'}(call my-dir)

include ${'$'}(CLEAR_VARS)

LOCAL_MODULE    := $NATIVE_LIB_NAME
LOCAL_SRC_FILES := $NATIVE_LIB_NAME.cpp

include ${'$'}(BUILD_SHARED_LIBRARY)
    """
          .trimIndent()

  executor.save(content, androidMk)
}

private fun AndroidModuleTemplateBuilder.generateApplicationMk(jniDir: File) {
  val applicationMk = File(jniDir, "Application.mk")
  val content =
      """
APP_ABI := all
APP_PLATFORM := android-${data.versions.minSdk.api}
APP_STL := c++_shared
APP_CPPFLAGS += -std=c++17
    """
          .trimIndent()

  executor.save(content, applicationMk)
}

private fun AndroidModuleTemplateBuilder.generateTomaslibCpp(jniDir: File) {
  val tomaslibCpp = File(jniDir, "$NATIVE_LIB_NAME.cpp")
  val packagePath = data.packageName.replace('.', '_')

  val content =
      """
#include <jni.h>
#include <string>
#include "$NATIVE_LIB_NAME.h"

extern "C" JNIEXPORT jstring JNICALL
Java_${packagePath}_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from TomasLib C++";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT jint JNICALL
Java_${packagePath}_MainActivity_addNumbers(
        JNIEnv* env,
        jobject /* this */,
        jint a,
        jint b) {
    return a + b;
}

extern "C" JNIEXPORT void JNICALL
Java_${packagePath}_MainActivity_initTomasLib(
        JNIEnv* env,
        jobject /* this */) {
    // Initialize TomasLib native library
    // Add your initialization code here
}
    """
          .trimIndent()

  executor.save(content, tomaslibCpp)
}

private fun AndroidModuleTemplateBuilder.generateTomaslibHeader(jniDir: File) {
  val tomaslibH = File(jniDir, "$NATIVE_LIB_NAME.h")
  val packagePath = data.packageName.replace('.', '_')
  val content =
      """
#ifndef TOMASLIB_H
#define TOMASLIB_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Returns a greeting string from TomasLib C++
 */
JNIEXPORT jstring JNICALL
Java_${packagePath}_MainActivity_stringFromJNI(JNIEnv* env, jobject thiz);

/**
 * Adds two integers and returns the result
 */
JNIEXPORT jint JNICALL
Java_${packagePath}_MainActivity_addNumbers(JNIEnv* env, jobject thiz, jint a, jint b);

/**
 * Says hello to a person with their name
 */
JNIEXPORT jstring JNICALL
Java_${packagePath}_MainActivity_sayHello(JNIEnv* env, jobject thiz, jstring name);

/**
 * Initialize the TomasLib native library
 */
JNIEXPORT void JNICALL
Java_${packagePath}_MainActivity_initTomasLib(JNIEnv* env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif // TOMASLIB_H
    """
          .trimIndent()

  executor.save(content, tomaslibH)
}

private fun AndroidModuleTemplateBuilder.hasNativeFiles(): Boolean {
  val androidMkFile = File(data.projectDir, "src/main/jni/Android.mk")
  val cmakeListsFile = File(data.projectDir, "src/main/jni/CMakeLists.txt")
  return androidMkFile.exists() || cmakeListsFile.exists()
}

private fun AndroidModuleTemplateBuilder.isNdkInstalled(): Boolean {
  val ndkBuildFile = File(Environment.ANDROID_HOME, "ndk/28.2.13676358/ndk-build")
  return ndkBuildFile.exists()
}

private fun AndroidModuleTemplateBuilder.showNdkNotInstalledDialog(context: Context) {
  MaterialAlertDialogBuilder(context)
      .setTitle("NDK Not Found")
      .setMessage(
          "A compatible NDK (version 28.2.13676358) is not installed.\n\n" +
              "Native code features will be disabled for this project.\n\n" +
              "To enable native development, please install NDK version 28.2.13676358 " +
              "open a terminal then run: 'idesetup -y -c -wn'."
      )
      .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
      .setCancelable(false)
      .show()
}

private fun AndroidModuleTemplateBuilder.buildGradleSrcKts(isComposeModule: Boolean): String {
  val shouldUseNdk = data.useNdk
  val hasNative = hasNativeFiles() || shouldUseNdk
  val ndkInstalled = isNdkInstalled()

  // Calculate compileSdk value first
  val compileSdkValue = if (isComposeModule) 36 else data.versions.compileSdk.api
  composeExtraPluginKt = if (isComposeModule) "id(\"$compose_kotlinExtraPlugin\")" else ""
  composeExtraPluginGr = if (isComposeModule) "id '$compose_kotlinExtraPlugin'" else ""

  return """
plugins {
    id("$androidPlugin")
    ${ktPlugin()}
}

android {
    namespace = "${data.packageName}"
    compileSdk = $compileSdkValue
    ${if (hasNative && ndkInstalled) """ndkVersion = "28.2.13676358"""" else ""}
    
    defaultConfig {
        applicationId = "${data.packageName}"
        minSdk = ${data.versions.minSdk.api}
        targetSdk = ${data.versions.targetSdk.api}
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
        ${if (hasNative && ndkInstalled) """
        externalNativeBuild {
            ndkBuild {
                abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64", "x86"))
            }
        }
        """ else ""}
    }
    
    compileOptions {
        sourceCompatibility = ${data.versions.javaSource()}
        targetCompatibility = ${data.versions.javaTarget()}
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    ${if (hasNative && ndkInstalled) """
    externalNativeBuild {
        ndkBuild {
            path = file("src/main/jni/Android.mk")
        }
    }""" else ""}
    buildFeatures {
        ${if (!isComposeModule) "viewBinding = true" else ""}
        ${if (isComposeModule) "compose = true" else ""}
    }
    ${if(isComposeModule) composeConfigKts() else ""}
}
${ktJvmTarget()}
${dependencies()}
"""
}

private fun AndroidModuleTemplateBuilder.buildGradleSrcGroovy(isComposeModule: Boolean): String {
  val shouldUseNdk = data.useNdk
  val hasNative = hasNativeFiles() || shouldUseNdk
  val ndkInstalled = isNdkInstalled()

  // Calculate compileSdk value first
  val compileSdkValue = if (isComposeModule) 36 else data.versions.compileSdk.api
  // Ensure compose plugin application variables are set for Groovy, same as KTS path
  composeExtraPluginKt = if (isComposeModule) "id(\"$compose_kotlinExtraPlugin\")" else ""
  composeExtraPluginGr = if (isComposeModule) "id '$compose_kotlinExtraPlugin'" else ""

  return """
plugins {
    id '$androidPlugin'
    ${ktPlugin()}
}

android {
    namespace '${data.packageName}'
    compileSdk $compileSdkValue
    ${if (hasNative && ndkInstalled) """ndkVersion '28.2.13676358'""" else ""}
    
    defaultConfig {
        applicationId "${data.packageName}"
        minSdk ${data.versions.minSdk.api}
        targetSdk ${data.versions.targetSdk.api}
        versionCode 1
        versionName "1.0"
        
        vectorDrawables { 
            useSupportLibrary true
        }
        ${if (hasNative && ndkInstalled) """
        externalNativeBuild {
            ndkBuild {
                abiFilters 'armeabi-v7a', 'arm64-v8a', 'x86_64', 'x86'
            }
        }
        """ else ""}
    }

    compileOptions {
        sourceCompatibility ${data.versions.javaSource()}
        targetCompatibility ${data.versions.javaTarget()}
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    ${if (hasNative && ndkInstalled) """
    externalNativeBuild {
        ndkBuild {
            path file('src/main/jni/Android.mk')
        }
    }""" else ""}
    buildFeatures {
        ${if (!isComposeModule) "viewBinding true" else ""}
        ${if (isComposeModule) "compose true" else ""}
    }
    ${if(isComposeModule) composeConfigGroovy() else ""}
}
${ktJvmTarget()}
${dependencies()}
"""
}

fun composeConfigGroovy(): String =
    """
    composeOptions {
        kotlinCompilerExtensionVersion '$compose_kotlinCompilerExtensionVersion'
    }
    packaging {
        resources {
            excludes += '/META-INF/{AL2.0,LGPL2.1}'
        }
    }
"""
        .trim()

fun composeConfigKts(): String =
    """
    composeOptions {
        kotlinCompilerExtensionVersion = "$compose_kotlinCompilerExtensionVersion"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
"""
        .trim()

private fun ModuleTemplateBuilder.ktJvmTarget(): String {
  if (data.language != Kotlin) {
    return ""
  }

  return if (data.useKts) ktJvmTargetKts() else ktJvmTargetGroovy()
}

private fun ModuleTemplateBuilder.ktJvmTargetKts(): String {
  return """
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("${data.versions.javaTarget}"))
    }
}
"""
}

private fun ModuleTemplateBuilder.ktJvmTargetGroovy(): String {
  return """
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget("${data.versions.javaTarget}")
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
  return """
  id("kotlin-android")
  $composeExtraPluginKt
  """
}

private fun ktPluginGroovy(): String {
  return """
  id 'kotlin-android'
  $composeExtraPluginGr
  """
}
