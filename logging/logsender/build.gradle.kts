/*
 *  This file is part of AndroidIDE.
 */

import com.tom.rv2ide.plugins.NoDesugarPlugin
import com.tom.rv2ide.build.config.BuildConfig

plugins {
    id("com.android.library")
}

apply {
    plugin(NoDesugarPlugin::class.java)
}

description = "LogSender is used to read logs from applications built with AndroidIDE"

android {
    namespace = "${BuildConfig.packageName}.logsender"

    defaultConfig {
        minSdk = 16
        vectorDrawables.useSupportLibrary = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        aidl = true
        viewBinding = false
    }
}

dependencies {
    // your dependencies here
}

tasks.register("fixAarName") {
    doLast {
        val aarDir = file("$buildDir/outputs/aar")
        val files = aarDir.listFiles { f -> f.extension == "aar" } ?: return@doLast
        files.forEach { f ->
            if (f.name != "logger-runtime.aar") {
                val target = File(f.parentFile, "logger-runtime.aar")
                target.delete()
                if (f.renameTo(target)) {
                    println("✅ Renamed ${f.name} → ${target.name}")
                } else {
                    println("⚠️  Could not rename ${f.name}")
                }
            }
        }
    }
}