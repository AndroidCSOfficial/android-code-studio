/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.tom.androidcodestudio.acsprovider

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.tom.androidcodestudio.acsprovider.models.ACSConfig
import com.tom.androidcodestudio.acsprovider.models.PackageEntry
import com.tom.androidcodestudio.acsprovider.utils.DownloadCallback
import com.tom.androidcodestudio.acsprovider.utils.HashUtils
import com.tom.rv2ide.utils.Environment.TMP_DIR as TMPDIR
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.slf4j.LoggerFactory

/**
 * Android Code Studio Build System Provider Download and manage build system packages
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
class ACSProvider(
    private val downloadDir: File,
    private val tempDir: File = TMPDIR,
    enableLogging: Boolean = false,
) {

  private val logger = LoggerFactory.getLogger(ACSProvider::class.java)
  private val gson = Gson()

  private val client: OkHttpClient =
      OkHttpClient.Builder()
          .connectTimeout(30, TimeUnit.SECONDS)
          .readTimeout(120, TimeUnit.SECONDS)
          .writeTimeout(120, TimeUnit.SECONDS)
          .followRedirects(true)
          .followSslRedirects(true)
          .retryOnConnectionFailure(true)
          .apply {
            if (enableLogging) {
              val logging =
                  HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
              addInterceptor(logging)
            }
          }
          .build()

  init {
    if (!downloadDir.exists()) {
      downloadDir.mkdirs()
    }
    if (!tempDir.exists()) {
      tempDir.mkdirs()
    }
  }

  /** Download JSON manifest from URL */
  suspend fun downloadJson(url: String, silent: Boolean = false): String =
      withContext(Dispatchers.IO) {
        if (!silent) {
          logger.info("Downloading JSON from: {}", url)
        }

        val request = Request.Builder().url(url).get().build()

        client.newCall(request).execute().use { response ->
          if (!response.isSuccessful) {
            throw IOException("Failed to download JSON from: $url. HTTP ${response.code}")
          }

          val body =
              response.body?.string() ?: throw IOException("Downloaded JSON content is empty")

          if (body.isEmpty()) {
            throw IOException("Downloaded JSON content is empty")
          }

          body
        }
      }

  /** Parse JSON and find package entry matching criteria */
  fun findPackageEntry(jsonContent: String, config: ACSConfig): PackageEntry {
    val jsonObject = JsonParser.parseString(jsonContent).asJsonObject

    if (!jsonObject.has("packages") || !jsonObject.get("packages").isJsonArray) {
      throw IllegalArgumentException("JSON data does not contain 'packages' array")
    }

    val packages = jsonObject.getAsJsonArray("packages")
    val matchingPackages = mutableListOf<PackageEntry>()

    // First pass: find packages matching architecture and ID
    for (i in 0 until packages.size()) {
      val entry = packages[i].asJsonObject

      val archMatch =
          entry.has("architecture") && entry.get("architecture").asString == config.architecture

      val idMatch =
          config.packageId.isNullOrEmpty() ||
              (entry.has("id") && entry.get("id").asString == config.packageId)

      if (archMatch && idMatch) {
        matchingPackages.add(parsePackageEntry(entry))
      }
    }

    if (matchingPackages.isEmpty()) {
      var errorMsg = "Architecture '${config.architecture}'"
      if (!config.packageId.isNullOrEmpty()) {
        errorMsg += " with ID '${config.packageId}'"
      }
      errorMsg += " not found in packages"
      throw IllegalArgumentException(errorMsg)
    }

    // Second pass: filter by version if specified
    if (!config.version.isNullOrEmpty()) {
      matchingPackages
          .find { it.version == config.version }
          ?.let {
            return it
          }

      var errorMsg =
          "Version '${config.version}' not found for architecture '${config.architecture}'"
      if (!config.packageId.isNullOrEmpty()) {
        errorMsg += " with ID '${config.packageId}'"
      }
      throw IllegalArgumentException(errorMsg)
    }

    // Return first match if no version specified
    return matchingPackages[0]
  }

  private fun parsePackageEntry(jsonObject: JsonObject): PackageEntry {
    return PackageEntry(
        id = jsonObject.get("id").asString,
        architecture = jsonObject.get("architecture").asString,
        version = jsonObject.get("version").asString,
        filename = jsonObject.get("filename").asString,
        url = jsonObject.get("url").asString,
        sha256 = if (jsonObject.has("sha256")) jsonObject.get("sha256").asString else null,
    )
  }

  /** List available versions for given architecture and ID */
  fun listAvailableVersions(
      jsonContent: String,
      architecture: String,
      packageId: String? = null,
  ): List<String> {
    val jsonObject = JsonParser.parseString(jsonContent).asJsonObject

    if (!jsonObject.has("packages") || !jsonObject.get("packages").isJsonArray) {
      throw IllegalArgumentException("JSON data does not contain 'packages' array")
    }

    val packages = jsonObject.getAsJsonArray("packages")
    val versions = mutableSetOf<String>()

    for (i in 0 until packages.size()) {
      val entry = packages[i].asJsonObject

      val archMatch =
          entry.has("architecture") && entry.get("architecture").asString == architecture

      val idMatch =
          packageId.isNullOrEmpty() || (entry.has("id") && entry.get("id").asString == packageId)

      if (archMatch && idMatch && entry.has("version")) {
        versions.add(entry.get("version").asString)
      }
    }

    return versions.sorted()
  }

  /** Get specific field from package entry */
  suspend fun getField(config: ACSConfig): String =
      withContext(Dispatchers.IO) {
        if (config.jsonUrl.isNullOrEmpty()) {
          throw IllegalArgumentException("JSON URL is required")
        }

        if (config.getField.isNullOrEmpty()) {
          throw IllegalArgumentException("Field name is required")
        }

        val jsonContent = downloadJson(config.jsonUrl, silent = true)
        val entry = findPackageEntry(jsonContent, config)

        when (config.getField) {
          "id" -> entry.id
          "architecture" -> entry.architecture
          "version" -> entry.version
          "filename" -> entry.filename
          "url" -> entry.url
          "sha256" -> entry.sha256 ?: ""
          else -> throw IllegalArgumentException("Unknown field: ${config.getField}")
        }
      }

  /** Download file from URL with progress tracking */
  suspend fun downloadFile(
      url: String,
      outputFile: File,
      callback: DownloadCallback? = null,
  ): Boolean =
      withContext(Dispatchers.IO) {
        try {
          logger.info("Downloading: {}", url)
          logger.info("Output: {}", outputFile.absolutePath)

          val request = Request.Builder().url(url).get().build()

          client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              throw IOException("Failed to download file. HTTP ${response.code}")
            }

            val body = response.body ?: throw IOException("Response body is null")
            val contentLength = body.contentLength()

            FileOutputStream(outputFile).use { output ->
              val buffer = ByteArray(8192)
              var bytesRead: Int
              var totalBytesRead: Long = 0

              body.byteStream().use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                  output.write(buffer, 0, bytesRead)
                  totalBytesRead += bytesRead

                  if (contentLength > 0) {
                    val percentage = ((totalBytesRead * 100) / contentLength).toInt()
                    callback?.onProgress(totalBytesRead, contentLength, percentage)
                  }
                }
              }
            }

            if (!outputFile.exists() || outputFile.length() == 0L) {
              outputFile.delete()
              throw IOException("Downloaded file is empty or does not exist")
            }

            callback?.onComplete(outputFile)
            true
          }
        } catch (e: Exception) {
          callback?.onError(e)
          if (outputFile.exists()) {
            outputFile.delete()
          }
          throw e
        }
      }

  /** Download package based on config */
  suspend fun downloadPackage(config: ACSConfig, callback: DownloadCallback? = null): File =
      withContext(Dispatchers.IO) {
        if (config.directUrl != null) {
          // Direct URL download
          val filename = config.directUrl.substringAfterLast('/').ifEmpty { "downloaded_file" }
          val outputFile = File(downloadDir, filename)

          downloadFile(config.directUrl, outputFile, callback)
          return@withContext outputFile
        }

        // JSON manifest download
        if (config.jsonUrl.isNullOrEmpty()) {
          throw IllegalArgumentException("JSON URL is required")
        }

        val jsonContent = downloadJson(config.jsonUrl, silent = false)
        val entry = findPackageEntry(jsonContent, config)

        val outputFile = File(downloadDir, entry.filename)
        downloadFile(entry.url, outputFile, callback)

        // Verify SHA256 if provided
        if (entry.sha256 != null) {
          logger.info("Expected SHA256: {}", entry.sha256)
          val actualHash = HashUtils.calculateSHA256(outputFile)
          logger.info("Actual SHA256:   {}", actualHash)

          if (HashUtils.verifySHA256(outputFile, entry.sha256)) {
            logger.info("SHA256 verification: PASSED")
          } else {
            outputFile.delete()
            throw IOException("SHA256 verification: FAILED. Downloaded file may be corrupted")
          }
        } else {
          logger.warn("No SHA256 checksum available for verification")
        }

        outputFile
      }

  /** Execute operation based on config */
  suspend fun execute(config: ACSConfig, callback: DownloadCallback? = null): Any =
      withContext(Dispatchers.IO) {
        if (config.shouldDownload) {
          return@withContext downloadPackage(config, callback)
        }

        if (!config.getField.isNullOrEmpty()) {
          return@withContext getField(config)
        }

        if (config.directUrl != null) {
          return@withContext config.directUrl
        }

        throw IllegalArgumentException(
            "Invalid configuration. Specify either download, getField, or directUrl"
        )
      }
}
