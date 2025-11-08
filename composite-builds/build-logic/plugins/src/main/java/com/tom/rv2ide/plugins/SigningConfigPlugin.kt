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

package com.tom.rv2ide.plugins

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import com.tom.rv2ide.build.config.SigningConfig
import com.tom.rv2ide.build.config.loadSigningProperties
import java.io.File

/**
 * Configures the signing keys to application modules.
 *
 * @author Akash Yadav
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class SigningConfigPlugin : Plugin<Project> {

  override fun apply(target: Project) {
    target.run {
        val signing = findSigningConfiguration()
        if (signing == null) {
            return
        }
        
        val signingKey = file(signing.storeFile)
        if (!signingKey.exists()) {
            return
        }

        // Create and apply the signing config
        extensions.getByType(BaseExtension::class.java).let { extension ->
            val config = extension.signingConfigs.create("common") {
                storeFile = signingKey
                keyAlias = signing.keyAlias
                storePassword = signing.storePassword
                keyPassword = signing.keyPassword
            }
            
            extension.buildTypes.forEach { buildType ->
                buildType.signingConfig = config
            }
            
            logger.lifecycle("Signing configured successfully for all build types")
            logger.lifecycle("Key store: ${signingKey.absolutePath}")
            logger.lifecycle("Key alias: ${signing.keyAlias}")
        }
    }
  }
  
  private fun Project.findSigningConfiguration(): SigningConfig? {
      val envStoreFile = System.getenv("SIGNING_STORE_FILE") ?: "signing/signing-key.jks"
      val envStorePassword = System.getenv("SIGNING_STORE_PASSWORD")
      val envKeyPassword = System.getenv("SIGNING_KEY_PASSWORD")
      val envKeyAlias = System.getenv("SIGNING_KEY_ALIAS") ?: "AndroidCS"
      
      if (envStorePassword != null && envKeyPassword != null) {
          return SigningConfig(
              storeFile = envStoreFile,
              storePassword = envStorePassword,
              keyPassword = envKeyPassword,
              keyAlias = envKeyAlias
          )
      }
      
      val gradleStoreFile = findProperty("signingStoreFile") as? String ?: "signing/signing-key.jks"
      val gradleStorePassword = findProperty("signingStorePassword") as? String
      val gradleKeyPassword = findProperty("signingKeyPassword") as? String
      val gradleKeyAlias = findProperty("signingKeyAlias") as? String ?: "AndroidCS"
      
      if (gradleStorePassword != null && gradleKeyPassword != null) {
          return SigningConfig(
              storeFile = gradleStoreFile,
              storePassword = gradleStorePassword,
              keyPassword = gradleKeyPassword,
              keyAlias = gradleKeyAlias
          )
      }
      
      val localSigning = loadSigningProperties()
      if (localSigning != null) {
          logger.lifecycle("Using signing configuration from local.properties")
      }
      
      return localSigning
  }
}