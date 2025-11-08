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

package com.tom.rv2ide.templates.android.cpp

import com.tom.rv2ide.templates.preferences.Options

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object CppSources {

  fun mainActivityKotlin(packageId: String): String =
      """
    package $packageId
    
    import android.os.Bundle
    import androidx.appcompat.app.AppCompatActivity
    import $packageId.databinding.ActivityMainBinding
    
    class MainActivity : AppCompatActivity() {
        
        private var _binding: ActivityMainBinding? = null
        private val binding: ActivityMainBinding
            get() = checkNotNull(_binding) { "Activity has been destroyed" }
        
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            _binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            
            binding.textView.text = stringFromJNI()
        }
        
        override fun onDestroy() {
            super.onDestroy()
            _binding = null
        }
        
        external fun stringFromJNI(): String
    
        companion object {
            // Used to load the 'myapplication' library on application startup.
            init {
                System.loadLibrary("myapplication")
            }
        }
    }
  """
          .trimIndent()

  fun mainActivityJava(packageId: String): String =
      """
      package $packageId;
      
      import android.os.Bundle;
      import androidx.appcompat.app.AppCompatActivity;
      import $packageId.databinding.ActivityMainBinding;
      
      public class MainActivity extends AppCompatActivity {
          private ActivityMainBinding binding;
          
          @Override
          protected void onCreate(Bundle savedInstanceState) {
              super.onCreate(savedInstanceState);
              binding = ActivityMainBinding.inflate(getLayoutInflater());
              setContentView(binding.getRoot());
              
              // Set text from native code
              binding.textView.setText(stringFromJNI());
          }
          
          @Override
          protected void onDestroy() {
              super.onDestroy();
              binding = null;
          }
          
          public native String stringFromJNI();
          
          static {
              // Used to load the 'myapplication' library on application startup.
              System.loadLibrary("myapplication");
          }
      }
  """
          .trimIndent()

  fun jniNativelib(packageId: String): String =
      """
    #include <jni.h>
    #include <string>
    
    extern "C" JNIEXPORT jstring JNICALL
    Java_${packageToJniTransformer(packageId)}_MainActivity_stringFromJNI(
            JNIEnv* env,
            jobject /* this */) {
        std::string hello = "Hello from C++";
        return env->NewStringUTF(hello.c_str());
    }
  """
          .trimIndent()

  fun jniNativelibInC(packageId: String): String =
      """
      #include <jni.h>
      #include <string.h>
      
      JNIEXPORT jstring JNICALL
      Java_${packageToJniTransformer(packageId)}_MainActivity_stringFromJNI(
              JNIEnv* env,
              jobject this) {
          char hello[] = "Hello from C";
          return (*env)->NewStringUTF(env, hello);
      }
  """
          .trimIndent()

  // Android.mk
  fun jniBuildSystemNdk(): String =
      """
        LOCAL_PATH := $(call my-dir)
        
        include $(CLEAR_VARS)
        
        LOCAL_MODULE    := myapplication
        LOCAL_SRC_FILES := native-lib.cpp
        
        include $(BUILD_SHARED_LIBRARY)
      """
          .trimIndent()

  // Application.mk
  fun jniBuildSystemApplicatNdk(): String =
      """
        APP_ABI := all
        APP_PLATFORM := android-21
        APP_STL := c++_shared
        APP_CPPFLAGS += -std=c++17
      """
          .trimIndent()

  fun jniBuildSystemCMake(): String =
      """
      # For more information about using CMake with Android Studio, read the
      # documentation: https://d.android.com/studio/projects/add-native-code.html.
      # For more examples on how to use CMake, see https://github.com/android/ndk-samples.
      
      # Sets the minimum CMake version required for this project.
      cmake_minimum_required(VERSION 3.22.1)
      
      # Declares the project name. The project name can be accessed via ${'$'}{PROJECT_NAME},
      # Since this is the top level CMakeLists.txt, the project name is also accessible
      # with ${'$'}{CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
      # build script scope).
      project("myapplication")
      
      # Creates and names a library, sets it as either STATIC
      # or SHARED, and provides the relative paths to its source code.
      # You can define multiple libraries, and CMake builds them for you.
      # Gradle automatically packages shared libraries with your APK.
      #
      # In this top level CMakeLists.txt, ${'$'}{CMAKE_PROJECT_NAME} is used to define
      # the target library name; in the sub-module's CMakeLists.txt, ${'$'}{PROJECT_NAME}
      # is preferred for the same purpose.
      #
      # In order to load a library into your app from Java/Kotlin, you must call
      # System.loadLibrary() and pass the name of the library defined here;
      # for GameActivity/NativeActivity derived applications, the same library name must be
      # used in the AndroidManifest.xml file.
      add_library(myapplication SHARED
              # List C/C++ source files with relative paths to this CMakeLists.txt.
              native-lib.${Options.OPT_NATIVE_LANGUAGE})
      
      # Specifies libraries CMake should link to your target library. You
      # can link libraries from various origins, such as libraries defined in this
      # build script, prebuilt third-party libraries, or Android system libraries.
      target_link_libraries(myapplication
              # List libraries link to the target library
              android
              log)
  """
          .trimIndent()

  private fun packageToJniTransformer(id: String): String {
    return id.replace(".", "_")
  }
}
