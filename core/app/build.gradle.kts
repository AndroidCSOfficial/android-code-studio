/*
 * This file is part of AndroidIDE.
 */

@file:Suppress("UnstableApiUsage")

import com.tom.rv2ide.build.config.BuildConfig
import com.tom.rv2ide.desugaring.utils.JavaIOReplacements.applyJavaIOReplacements
import com.tom.rv2ide.plugins.AndroidIDEAssetsPlugin
import java.util.Properties

plugins {
  id("com.tom.rv2ide.core-app")
  id("com.android.application")
  id("kotlin-android")
  id("kotlin-kapt")
  id("kotlinx-serialization")
  id("kotlin-parcelize")
  id("androidx.navigation.safeargs.kotlin")
  id("com.tom.rv2ide.desugaring")
}

apply { plugin(AndroidIDEAssetsPlugin::class.java) }

buildscript {
  dependencies {
    classpath(libs.logging.logback.core)
    classpath(libs.composite.desugaringCore)
  }
}

tasks.configureEach {
    if (name.contains("desugar", ignoreCase = true)) {
        enabled = false
    }
}

configurations.all {
  resolutionStrategy {
    force("com.google.guava:guava:32.1.3-android")
    eachDependency {
      if (requested.group == "com.google.guava" && requested.name == "guava") {
        if (requested.version?.contains("jre") == true) {
          useVersion("32.1.3-android")
        }
      }
    }
  }
}

android {
  namespace = BuildConfig.packageName

  defaultConfig {
    applicationId = BuildConfig.packageName
    vectorDrawables.useSupportLibrary = true
  }
  
  experimentalProperties["android.experimental.enableGlobalSynthetics"] = true

  // FIX: Resource extraction error rokne ke liye
  aaptOptions {
    cruncherEnabled = false
  }

  signingConfigs {
      create("custom") {
          val keyStorePath = "${rootProject.projectDir}/signing/signing-key.jks"
          val keyStoreFile = file(keyStorePath)
          val signing_storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
          val signing_keyPassword = System.getenv("SIGNING_KEY_PASSWORD") ?: ""
          storeFile = keyStoreFile
          storePassword = signing_storePassword
          keyAlias = "AndroidCS"
          keyPassword = signing_keyPassword
      }
  }

  androidResources { generateLocaleConfig = true }

  buildFeatures {
    aidl = true
    dataBinding = true
  }

  buildTypes {
    debug { signingConfig = signingConfigs.getByName("custom") }
    release {
      isShrinkResources = false
      signingConfig = signingConfigs.getByName("custom")
    }
  }
  
  lint {
    abortOnError = false
    disable.addAll(arrayOf("VectorPath", "NestedWeights", "ContentDescription", "SmallSp"))
  }

  // FIX: Packaging conflict ko solve karne ke liye
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      pickFirsts += "kotlin/**.kotlin_builtins"
      pickFirsts += "META-INF/LICENSE.md"
      pickFirsts += "META-INF/LICENSE-notice.md"
      pickFirsts += "META-INF/LICENSE"
      pickFirsts += "META-INF/NOTICE"
      pickFirsts += "THIRD-PARTY"
    }
  }

  applicationVariants.all {
    val variant = this
    variant.outputs.all {
      val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
      val versionName = variant.versionName ?: "unknown"
      val buildType = variant.buildType.name
      val archSuffix = "arm64-v8a" // Defaulting to arm64 for simplicity in server builds
      val appName = "android-code-studio"
      output.outputFileName = "${appName}-${archSuffix}-${buildType}-${versionName}.apk"
    }
  }
}

kapt { arguments { arg("eventBusIndex", "${BuildConfig.packageName}.events.AppEventsIndex") } }

desugaring {
  replacements {
    includePackage("org.eclipse.jgit")
    applyJavaIOReplacements()
  }
}

dependencies {
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
  implementation("org.tukaani:xz:1.9")
  implementation("org.apache.commons:commons-compress:1.21")
  implementation("com.github.Dimezis:BlurView:version-3.2.0")
  implementation("androidx.security:security-crypto:1.1.0-alpha06")
  implementation(projects.external.acsprovider)
  implementation(projects.external.atc) 
  implementation(libs.external.customizable.cardview)
  implementation(projects.external.logwire)
  implementation(libs.external.seasonal.effects)
  kapt(libs.common.glide.ap)
  kapt(libs.google.auto.service)
  kapt(projects.annotation.processors)
  implementation(libs.common.editor)
  implementation(libs.common.utilcode)
  implementation(libs.common.glide)
  implementation(libs.common.jsoup)
  implementation(libs.common.kotlin.coroutines.android)
  implementation(libs.common.retrofit)
  implementation(libs.common.retrofit.gson)
  implementation(libs.common.charts)
  implementation(libs.common.hiddenApiBypass)
  implementation(libs.aapt2.common)
  implementation(libs.google.auto.service.annotations)
  implementation(libs.google.gson)
  implementation(libs.google.guava)
  implementation("com.google.ai.client.generativeai:generativeai:0.9.0") {
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "org.slf4j", module = "slf4j-simple")
    exclude(group = "org.slf4j", module = "slf4j-nop")
  }
  implementation("com.github.MiyazKaori:SilentInstaller:1.0.0-alpha")
  implementation(libs.git.jgit)
  implementation(libs.androidx.splashscreen)
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.cardview)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.coordinatorlayout)
  implementation(libs.androidx.drawer)
  implementation(libs.androidx.grid)
  implementation(libs.androidx.nav.fragment)
  implementation(libs.androidx.nav.ui)
  implementation(libs.androidx.preference)
  implementation(libs.androidx.recyclerview)
  implementation(libs.androidx.transition)
  implementation(libs.androidx.vectors)
  implementation(libs.androidx.animated.vectors)
  implementation(libs.androidx.work)
  implementation(libs.androidx.work.ktx)
  implementation(libs.google.material)
  implementation(libs.google.flexbox)
  implementation(libs.androidx.core.ktx)
  implementation(libs.common.kotlin)
  implementation(libs.composite.appintro)
  implementation(libs.composite.desugaringCore)
  implementation(libs.composite.javapoet)
  implementation(projects.core.projectdata)
  implementation(projects.ideconfigurations)
  implementation(projects.core.actions)
  implementation(projects.core.common)
  implementation(projects.core.indexingApi)
  implementation(projects.core.indexingCore)
  implementation(projects.core.lspApi)
  implementation(projects.core.projects)
  implementation(projects.core.resources)
  implementation(projects.editor.impl)
  implementation(projects.editor.lexers)
  implementation(projects.event.eventbus)
  implementation(projects.event.eventbusAndroid)
  implementation(projects.event.eventbusEvents)
  implementation(projects.java.javacServices)
  implementation(projects.java.lspSetup)
  implementation(projects.java.lsp)
  implementation(projects.logging.idestats)
  implementation(projects.logging.logsender)
  implementation(projects.termux.application)
  implementation(projects.termux.view)
  implementation(projects.termux.emulator)
  implementation(projects.termux.shared)
  implementation(projects.tooling.api)
  implementation(projects.tooling.pluginConfig)
  implementation(projects.utilities.buildInfo)
  implementation(projects.utilities.lookup)
  implementation(projects.utilities.preferences)
  implementation(projects.utilities.templatesApi)
  implementation(projects.utilities.templatesImpl)
  implementation(projects.treeview)
  implementation(projects.utilities.uidesigner)
  implementation(projects.utilities.xmlInflater)
  implementation(projects.xml.aaptcompiler)
  implementation(projects.xml.lsp)
  implementation(projects.xml.utils)
  compileOnly(projects.tooling.impl)
}
