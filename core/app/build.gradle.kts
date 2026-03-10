@file:Suppress("UnstableApiUsage")

import com.tom.rv2ide.build.config.BuildConfig
import com.tom.rv2ide.plugins.AndroidIDEAssetsPlugin
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
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

apply { plugin(AndroidIDEAssetsPlugin::class.java) }

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

    // Modern variant filtering for Gradle 8.x+
    val androidComponents = extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
    androidComponents.beforeVariants { variantBuilder ->
        if (variantBuilder.buildType == "debug") {
            variantBuilder.enable = false
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

    buildFeatures {
        aidl = true
        dataBinding = true
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    // Kotlin & Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.apache.commons:commons-compress:1.21")
    implementation("org.tukaani:xz:1.9")
    
    // Core IDE & Tooling
    implementation(projects.core.projectdata)
    implementation(projects.core.actions)
    implementation(projects.core.common)
    implementation(projects.core.projects)
    implementation(projects.tooling.api)
    implementation(projects.tooling.impl)
    
    // External & UI
    implementation("com.github.Dimezis:BlurView:version-3.2.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation(libs.common.editor)
    implementation(libs.common.glide)
    implementation(libs.google.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.work.ktx)
    
    // AI & Git
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation(libs.git.jgit)

    // Annotation Processors
    kapt(libs.common.glide.ap)
    kapt(libs.google.auto.service)
    kapt(projects.annotation.processors)
}
