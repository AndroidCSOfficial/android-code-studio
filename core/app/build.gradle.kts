@file:Suppress("UnstableApiUsage")

import com.tom.rv2ide.build.config.BuildConfig
import com.tom.rv2ide.desugaring.utils.JavaIOReplacements.applyJavaIOReplacements
import com.tom.rv2ide.plugins.AndroidIDEAssetsPlugin
import com.android.build.gradle.internal.api.BaseVariantOutputImpl

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

apply<AndroidIDEAssetsPlugin>()

buildscript {
    dependencies {
        classpath(libs.logging.logback.core)
        classpath(libs.composite.desugaringCore)
    }
}

android {
    namespace = BuildConfig.packageName
    compileSdk = 34

    defaultConfig {
        applicationId = BuildConfig.packageName
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    // ABI Splits for optimized APK distribution
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("custom") {
            storeFile = file("${rootProject.projectDir}/signing/signing-key.jks")
            storePassword = System.getenv("SIGNING_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: "AndroidCS"
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildFeatures {
        aidl = true
        dataBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("custom")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as BaseVariantOutputImpl
            val abi = output.getFilter("ABI") ?: "universal"
            output.outputFileName = "android-code-studio-${abi}-${variant.versionName}.apk"
        }
    }
}

kapt {
    arguments {
        arg("eventBusIndex", "${BuildConfig.packageName}.events.AppEventsIndex")
    }
}

desugaring {
    replacements {
        includePackage("org.eclipse.jgit")
        applyJavaIOReplacements()
    }
}

dependencies {
    // Core External
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.tukaani:xz:1.9")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("com.github.Dimezis:BlurView:version-3.2.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.github.MiyazKaori:SilentInstaller:1.0.0-alpha")

    // Project modules (as per your structure)
    implementation(projects.external.acsprovider)
    implementation(projects.external.atc)
    implementation(projects.core.projectdata)
    implementation(projects.core.actions)
    implementation(projects.core.common)
    implementation(projects.core.indexingApi)
    implementation(projects.editor.impl)
    implementation(projects.java.javacServices)
    implementation(projects.termux.application)
    // ... (Keep your other project dependencies here)

    // Annotation processors
    kapt(libs.common.glide.ap)
    kapt(libs.google.auto.service)
    kapt(projects.annotation.processors)
    
    compileOnly(projects.tooling.impl)
}
