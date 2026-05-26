import com.tom.rv2ide.plugins.NoDesugarPlugin
import com.tom.rv2ide.build.config.BuildConfig
import java.io.File

apply { plugin(NoDesugarPlugin::class.java) }

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "io.github.mohammedbaqernull.logger.logwire"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig { minSdk = 21 }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures { aidl = true }

    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
}

tasks.register("fixAarName") {
    doLast {
        val aarDir = layout.buildDirectory.dir("outputs/aar").get().asFile
        val outDir = File(projectDir, "../../core/app/src/main/assets").canonicalFile
        outDir.mkdirs()
        val files = aarDir.listFiles { f -> f.extension == "aar" } ?: return@doLast
        val finalName = "logger-runtime.aar"
        val aar = files.maxByOrNull { it.lastModified() } ?: return@doLast
        val renamed = File(aar.parentFile, finalName)
        if (aar.name != finalName) {
            renamed.delete()
            aar.renameTo(renamed)
        }
        renamed.copyTo(File(outDir, finalName), overwrite = true)
    }
}

plugins.withId("com.android.library") {
    afterEvaluate {
        tasks.named("bundleReleaseAar").configure {
            finalizedBy("fixAarName")
        }
    }
}

gradle.projectsEvaluated {
    tasks.matching { it.path == ":core:app:preDebugBuild" }.configureEach {
        dependsOn(":external:logwire:assembleRelease")
        dependsOn(":external:logwire:fixAarName")
    }
}