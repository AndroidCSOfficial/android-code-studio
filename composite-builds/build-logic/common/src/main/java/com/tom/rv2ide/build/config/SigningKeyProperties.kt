package com.tom.rv2ide.build.config

import org.gradle.api.Project
import java.util.Properties

/**
 * 扩展函数，用于从 local.properties 中读取签名信息
 */
fun Project.loadSigningProperties(): SigningConfig? {
    val localFile = rootProject.file("local.properties")
    if (!localFile.exists()) return null

    val props = Properties().apply {
        localFile.inputStream().use { load(it) }
    }

    val storeFile = props.getProperty("signing.storeFile") ?: return null
    val storePassword = props.getProperty("signing.storePassword") ?: return null
    val keyAlias = props.getProperty("signing.keyAlias") ?: return null
    val keyPassword = props.getProperty("signing.keyPassword") ?: return null

    return SigningConfig(storeFile, storePassword, keyAlias, keyPassword)
}

/**
 * 签名配置数据类
 */
data class SigningConfig(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String
)