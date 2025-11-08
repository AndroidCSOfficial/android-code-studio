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

package com.tom.rv2ide.templates.base.util

import com.tom.rv2ide.templates.SrcSet
import com.tom.rv2ide.templates.SrcSet.Main
import com.tom.rv2ide.templates.base.AndroidModuleTemplateBuilder
import com.tom.rv2ide.templates.base.util.AndroidModuleJniManager.JniFileType.*
import java.io.File

/**
 * Handles creation of JNI/C++ files in an Android module template.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class AndroidModuleJniManager {

  enum class JniFileType(val extension: String) {
    CPPFILE(".cpp"),
    CFILE(".c"),
    HEADERFILE(".h"),
    HPPFILE(".hpp"),
  }

  /**
   * Get the JNI directory for the given [source set][SrcSet].
   *
   * @param srcSet The source set.
   */
  fun AndroidModuleTemplateBuilder.jniDir(srcSet: SrcSet = Main): File {
    return File(srcFolder(srcSet), "jni").also { it.mkdirs() }
  }

  /**
   * Create a new JNI source file for the given [file type][JniFileType].
   *
   * @param name The name of the file without extension.
   * @param type The JNI file type.
   * @param srcSet The source set.
   * @param source The source code for the file.
   */
  fun AndroidModuleTemplateBuilder.writeJniSource(
      name: String,
      type: JniFileType,
      srcSet: SrcSet = Main,
      source: String,
  ) {
    val file = File(jniDir(srcSet), "${name}${type.extension}")
    executor.save(source, file)
  }

  /**
   * Create a new JNI source file for the given [file type][JniFileType].
   *
   * @param name The name of the file without extension.
   * @param type The JNI file type.
   * @param srcSet The source set.
   * @param source Function which returns the source code for the file.
   */
  inline fun AndroidModuleTemplateBuilder.writeJniSource(
      name: String,
      type: JniFileType,
      srcSet: SrcSet = Main,
      crossinline source: () -> String,
  ) {
    writeJniSource(name, type, srcSet, source())
  }

  /**
   * Converts a Java package name to JNI function prefix. Example: "com.example.myapp" ->
   * "Java_com_example_myapp"
   *
   * @param packageName The Java package name.
   * @param className The class name (default: "MainActivity").
   * @return The JNI function prefix.
   */
  fun packageToJniPrefix(packageName: String, className: String = "MainActivity"): String {
    return "Java_${packageName.replace('.', '_')}_${className}"
  }

  /**
   * Creates a basic JNI C++ source file with a sample function.
   *
   * @param packageName The Java package name.
   * @param className The class name (default: "MainActivity").
   * @param functionName The native function name (default: "sayHello").
   */
  fun createBasicJniCppSource(
      packageName: String,
      className: String = "MainActivity",
      functionName: String = "sayHello",
  ): String {
    val jniPrefix = packageToJniPrefix(packageName, className)
    return """#include <jni.h>
#include <string>

extern "C" JNIEXPORT jstring JNICALL
${jniPrefix}_${functionName}(JNIEnv *env, jobject /* this */) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}
"""
  }

  /**
   * Creates a basic JNI header file.
   *
   * @param packageName The Java package name.
   * @param className The class name (default: "MainActivity").
   * @param functionName The native function name (default: "sayHello").
   */
  fun createBasicJniHeader(
      packageName: String,
      className: String = "MainActivity",
      functionName: String = "sayHello",
  ): String {
    val jniPrefix = packageToJniPrefix(packageName, className)
    val headerGuard = "${packageName.replace('.', '_').uppercase()}_${className.uppercase()}_H"

    return """#ifndef ${headerGuard}
#define ${headerGuard}

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
${jniPrefix}_${functionName}(JNIEnv *env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif // ${headerGuard}
"""
  }

  /**
   * Creates a CMakeLists.txt file for building the native library.
   *
   * @param libraryName The name of the native library.
   * @param sourceFiles List of source files to compile.
   */
  fun createCMakeListsFile(
      libraryName: String,
      sourceFiles: List<String> = listOf("${libraryName}.cpp"),
  ): String {
    val sources = sourceFiles.joinToString("\n        ")
    return """cmake_minimum_required(VERSION 3.22.1)

project("${libraryName}")

add_library(
        ${libraryName}
        SHARED
        ${sources}
)

find_library(
        log-lib
        log
)

target_link_libraries(
        ${libraryName}
        ${'$'}{log-lib}
)
"""
  }

  /**
   * Creates a basic Android.mk file for building the native library.
   *
   * @param libraryName The name of the native library.
   * @param sourceFiles List of source files to compile.
   */
  fun createAndroidMkFile(
      libraryName: String,
      sourceFiles: List<String> = listOf("${libraryName}.cpp"),
  ): String {
    val sources = sourceFiles.joinToString(" ")
    return """LOCAL_PATH := ${'$'}(call my-dir)

include ${'$'}(CLEAR_VARS)

LOCAL_MODULE := ${libraryName}
LOCAL_SRC_FILES := ${sources}
LOCAL_LDLIBS := -llog

include ${'$'}(BUILD_SHARED_LIBRARY)
"""
  }

  /**
   * Creates an Application.mk file.
   *
   * @param abis List of target ABIs (default: all supported ABIs).
   */
  fun createApplicationMkFile(
      abis: List<String> = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
  ): String {
    val abiList = abis.joinToString(" ")
    return """APP_ABI := ${abiList}
APP_PLATFORM := android-21
APP_STL := c++_shared
"""
  }
}

/** Extension functions for easier JNI template creation */

/** Creates a complete JNI setup with C++ source, header, and build files. */
internal fun AndroidModuleTemplateBuilder.writeJniLibrary(
    libraryName: String,
    packageName: String,
    className: String = "MainActivity",
    functionName: String = "sayHello",
) {
  val jniManager = AndroidModuleJniManager()

  with(jniManager) {
    // Create C++ source file
    writeJniSource(libraryName, CPPFILE) {
      createBasicJniCppSource(packageName, className, functionName)
    }

    // Create header file
    writeJniSource(libraryName, HPPFILE) {
      createBasicJniHeader(packageName, className, functionName)
    }

    // Create CMakeLists.txt in the same directory
    val cmakeFile = File(jniDir(), "CMakeLists.txt")
    executor.save(createCMakeListsFile(libraryName), cmakeFile)
  }
}

/** Creates JNI files with Android.mk build system. */
internal fun AndroidModuleTemplateBuilder.writeJniLibraryWithAndroidMk(
    libraryName: String,
    packageName: String,
    className: String = "MainActivity",
    functionName: String = "sayHello",
) {
  val jniManager = AndroidModuleJniManager()

  with(jniManager) {
    // Create C++ source file
    writeJniSource(libraryName, CPPFILE) {
      createBasicJniCppSource(packageName, className, functionName)
    }

    // Create header file
    writeJniSource(libraryName, HPPFILE) {
      createBasicJniHeader(packageName, className, functionName)
    }

    // Create Android.mk
    val androidMkFile = File(jniDir(), "Android.mk")
    executor.save(createAndroidMkFile(libraryName), androidMkFile)

    // Create Application.mk
    val appMkFile = File(jniDir(), "Application.mk")
    executor.save(createApplicationMkFile(), appMkFile)
  }
}
