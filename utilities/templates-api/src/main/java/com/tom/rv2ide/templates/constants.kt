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

package com.tom.rv2ide.templates

/** @author Akash Yadav */
const val ANDROID_GRADLE_PLUGIN_VERSION = "8.13.0"
const val GRADLE_DISTRIBUTION_VERSION = "8.13.0"
const val GRADLE_WRAPPER_DISTRIBUTION_VERSION = "8.13"
const val KOTLIN_VERSION = "2.1.0"

// Template creation has been migrated to ATC as of v1.0.0+gh.r2.
// The following dependency versions are recommended for use in sample templates.
// These versions have been thoroughly tested and perform optimally in ACS.

// AndroidX
const val ANDROIDX_CORE_KTEXT_VERSION = "1.17.0"
const val ANDROIDX_APPCOMPAT_VERSION = "1.7.1"
const val ANDROIDX_CONSTRAINTLAYOUT_VERSION = "2.2.1"
const val ANDROIDX_LIFECYCLE_KOTLIN_EXTENSIONS = "2.9.2"
const val ANDROIDX_ACTIVITY_COMPOSE = "1.11.0"
const val ANDROIDX_JETPACK_COMPOSE_LIBRARIES_BOM = "2025.10.01"

const val ANDROIDX_NAVIGATION_FRAGMENT_KTX = "2.9.5"
const val ANDROIDX_VECTORDRAWABLE = "1.2.0"
const val ANDROIDX_LIFECYCLE_LIVE_DATA_KTX = "2.9.4"
const val ANDROIDX_LIFE_CYCLE_VIEWMODEL_KTX = ANDROIDX_LIFECYCLE_LIVE_DATA_KTX
const val ANDROIDX_NAVIGATION_UI_KTX = ANDROIDX_NAVIGATION_FRAGMENT_KTX

const val ANDROIDX_GAMES_ACTIVITY = "4.0.0"

const val GOOGLE_MATERIAL_COMPONENTS_VERSION = "1.13.0"
val PROJECTS_COMPILE_SDK_VERSION = Sdk.BakLava.api

val TARGET_SDK_VERSION = Sdk.VanillaIceCream
val COMPILE_SDK_VERSION = Sdk.BakLava

const val JAVA_SOURCE_VERSION = "17"
const val JAVA_TARGET_VERSION = "17"
