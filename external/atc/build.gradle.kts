/*
 * Android Template Creator - Free and open-source project
 * Android Template Creator is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for complete details.
 *
 * A copy of the LGPL license can be found at <https://www.gnu.org/licenses/lgpl-3.0.html>.
 */

/*
 ** @Author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

plugins {
    alias(libs.plugins.android.library)
    // alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.tom.androidcodestudio.atc"
    compileSdk = 37

    defaultConfig {
        minSdk = 21
    }

    buildFeatures {
        buildConfig = false
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)

    implementation(libs.common.datastore)
    implementation(libs.common.kotlin.coroutines.android)
}
