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

android {
    namespace = BuildConfig.packageName

    defaultConfig {
        applicationId = BuildConfig.packageName
        vectorDrawables.useSupportLibrary = true
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // FIX: Configuration error rokne ke liye
    variantFilter {
        if (buildType.name == "debug") {
            setIgnore(true)
        }
    }

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

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("custom")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("custom")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            val archSuffix = output.getFilter(com.android.build.OutputFile.ABI) ?: "universal"
            output.outputFileName = "android-code-studio-${archSuffix}-${variant.versionName}.apk"
        }
    }

    buildFeatures {
        aidl = true
        dataBinding = true
    }

    lint { abortOnError = false }
}



dependencies {
    // Basic Dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.apache.commons:commons-compress:1.21")
    
    // External & Composite
    implementation(projects.external.acsprovider)
    implementation(projects.external.atc)
    implementation(projects.external.logwire)
    implementation(libs.composite.appintro)
    implementation(libs.composite.desugaringCore)
    
    // Core Modules
    implementation(projects.core.projectdata)
    implementation(projects.core.actions)
    implementation(projects.core.common)
    implementation(projects.core.projects)
    
    // Editor & UI
    implementation(libs.common.editor)
    implementation(libs.common.glide)
    implementation(libs.google.material)
    
    // Git & Tooling
    implementation(libs.git.jgit)
    implementation(projects.tooling.api)
    
    // All other legacy deps
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.work.ktx)
    implementation(libs.google.gson)
    implementation(libs.google.guava)
    
    // AI Integration
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
}
