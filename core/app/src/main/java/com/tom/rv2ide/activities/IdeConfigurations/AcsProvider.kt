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

package com.tom.rv2ide.activities.IdeConfigurations

import com.tom.androidcodestudio.acsprovider.ACSProvider as ACSLibProvider
import com.tom.androidcodestudio.acsprovider.models.ACSConfig
import com.tom.androidcodestudio.acsprovider.utils.DownloadCallback
import com.tom.rv2ide.utils.Environment
import java.io.File
import kotlinx.coroutines.runBlocking

/**
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 *
 * Interface reflection for ACS (Android Code Studio Build System Provider) Powered by the ACS
 * library
 */
object AcsCommandInterface {

  /** ACS download directory */
  private val ACS_DOWNLOAD_DIR = File("${Environment.HOME}/acs")
  private val ACS_TEMP_DIR = File("${Environment.TMP_DIR}")

  /** Lazy initialization of ACS library provider */
  private val acsLibProvider: ACSLibProvider by lazy {
    ACSLibProvider(downloadDir = ACS_DOWNLOAD_DIR, tempDir = ACS_TEMP_DIR, enableLogging = false)
  }

  /** Supported architectures for ACS packages */
  enum class Architecture(val value: String) {
    ARM64_V8A("arm64-v8a"),
    X86_64("x86_64"),
    ARM_V7A("armeabi-v7a"),
  }

  /** Available fields that can be retrieved from manifest */
  enum class ManifestField(val value: String) {
    VERSION("version"),
    URL("url"),
    FILENAME("filename"),
    SHA256("sha256"),
  }

  /** Result wrapper for ACS command execution */
  data class AcsResult(
      val success: Boolean,
      val output: String,
      val errorOutput: String,
      val exitCode: Int,
  )

  /** Package information from manifest */
  data class PackageInfo(
      val id: String,
      val architecture: String,
      val version: String,
      val filename: String,
      val url: String,
      val sha256: String,
  )

  /** ACS command builder for fluent API */
  class AcsCommandBuilder {
    private var manifestUrl: String? = null
    private var architecture: Architecture? = null
    private var packageId: String? = null
    private var version: String? = null
    private var field: ManifestField? = null
    private var directUrl: String? = null
    private var shouldDownload: Boolean = false
    private var shouldListVersions: Boolean = false
    private var shouldShowHelp: Boolean = false

    fun readFrom(manifestUrl: String): AcsCommandBuilder {
      this.manifestUrl = manifestUrl
      return this
    }

    fun getForArch(architecture: Architecture): AcsCommandBuilder {
      this.architecture = architecture
      return this
    }

    fun getField(field: ManifestField): AcsCommandBuilder {
      this.field = field
      return this
    }

    fun withPackageId(packageId: String): AcsCommandBuilder {
      this.packageId = packageId
      return this
    }

    fun withVersion(version: String): AcsCommandBuilder {
      this.version = version
      return this
    }

    fun listVersions(): AcsCommandBuilder {
      this.shouldListVersions = true
      return this
    }

    fun downloadFromUrl(url: String): AcsCommandBuilder {
      this.directUrl = url
      return this
    }

    fun enableDownload(): AcsCommandBuilder {
      this.shouldDownload = true
      return this
    }

    fun showHelp(): AcsCommandBuilder {
      this.shouldShowHelp = true
      return this
    }

    /** Execute the built command */
    fun execute(): AcsResult {
      return try {
        if (shouldShowHelp) {
          return AcsResult(success = true, output = getHelpText(), errorOutput = "", exitCode = 0)
        }

        if (shouldListVersions) {
          return executeListVersions()
        }

        if (shouldDownload) {
          return executeDownload()
        }

        if (field != null) {
          return executeGetField()
        }

        if (directUrl != null) {
          return AcsResult(success = true, output = directUrl!!, errorOutput = "", exitCode = 0)
        }

        AcsResult(
            success = false,
            output = "",
            errorOutput = "No valid operation specified",
            exitCode = 1,
        )
      } catch (e: Exception) {
        AcsResult(success = false, output = "", errorOutput = "Error: ${e.message}", exitCode = 1)
      }
    }

    private fun executeListVersions(): AcsResult {
      return runBlocking {
        try {
          val jsonContent = acsLibProvider.downloadJson(manifestUrl!!, silent = true)
          val versions =
              acsLibProvider.listAvailableVersions(jsonContent, architecture!!.value, packageId)

          val output =
              if (versions.isEmpty()) {
                "No versions found for the specified criteria"
              } else {
                "Available versions:\n" + versions.joinToString("\n") { "  $it" }
              }

          AcsResult(success = true, output = output, errorOutput = "", exitCode = 0)
        } catch (e: Exception) {
          AcsResult(success = false, output = "", errorOutput = "Error: ${e.message}", exitCode = 1)
        }
      }
    }

    private fun executeDownload(): AcsResult {
      return runBlocking {
        try {
          val config =
              ACSConfig(
                  jsonUrl = manifestUrl,
                  architecture = architecture!!.value,
                  packageId = packageId,
                  version = version,
                  directUrl = directUrl,
                  shouldDownload = true,
              )

          val file =
              acsLibProvider.downloadPackage(
                  config,
                  object : DownloadCallback {
                    override fun onProgress(
                        bytesDownloaded: Long,
                        totalBytes: Long,
                        percentage: Int,
                    ) {
                      // Progress callback - could be used to update UI
                    }

                    override fun onComplete(file: File) {
                      // Download complete
                    }

                    override fun onError(error: Throwable) {
                      // Error during download
                    }
                  },
              )

          AcsResult(
              success = true,
              output = "Successfully downloaded: ${file.absolutePath}",
              errorOutput = "",
              exitCode = 0,
          )
        } catch (e: Exception) {
          AcsResult(success = false, output = "", errorOutput = "Error: ${e.message}", exitCode = 1)
        }
      }
    }

    private fun executeGetField(): AcsResult {
      return runBlocking {
        try {
          val config =
              ACSConfig(
                  jsonUrl = manifestUrl!!,
                  architecture = architecture!!.value,
                  packageId = packageId,
                  version = version,
                  getField = field!!.value,
              )

          val fieldValue = acsLibProvider.getField(config)

          AcsResult(success = true, output = fieldValue, errorOutput = "", exitCode = 0)
        } catch (e: Exception) {
          AcsResult(success = false, output = "", errorOutput = "Error: ${e.message}", exitCode = 1)
        }
      }
    }

    private fun getHelpText(): String {
      return """
               Android Code Studio Build System Provider - Download and manage build system packages

               Usage: acs [OPTIONS]

               Options:
                 -r, --read-from <url>     JSON manifest URL to read from
                 -a, --get-for <arch>      Architecture to get (e.g., arm64-v8a, x86_64)
                 -f, --get <field>         Field to retrieve (e.g., version, url, filename)
                 -i, --id <package_id>     Package ID to filter by (e.g., android-native-kit)
                 -v, --version <version>   Specific version to get (e.g., 28.2.13676358)
                 -l, --list-versions       List all available versions for given architecture and ID
                     --get <url>           Direct URL to download
                 -d, --download            Download the file and verify checksum
                 -h, --help                Show this help message

               Examples:
                 # List available versions for android-native-kit on arm64-v8a
                 acs -r https://example.com/manifest.json --get-for arm64-v8a -i android-native-kit --list-versions

                 # Get version for specific package and version
                 acs -r https://example.com/manifest.json --get-for arm64-v8a -i android-native-kit -v 28.2.13676358 -f version

                 # Download specific version of package
                 acs -r https://example.com/manifest.json --get-for arm64-v8a -i android-native-kit -v 29.0.14033849 --download
             """
                 .trimIndent()
    }

    /** Get the command arguments as array (for compatibility) */
    fun buildArgs(): Array<String> {
      val args = mutableListOf<String>()
      manifestUrl?.let { args.addAll(listOf("-r", it)) }
      architecture?.let { args.addAll(listOf("-a", it.value)) }
      field?.let { args.addAll(listOf("-f", it.value)) }
      packageId?.let { args.addAll(listOf("-i", it)) }
      version?.let { args.addAll(listOf("-v", it)) }
      if (shouldListVersions) args.add("--list-versions")
      directUrl?.let { args.addAll(listOf("--get", it)) }
      if (shouldDownload) args.add("-d")
      if (shouldShowHelp) args.add("-h")
      return args.toTypedArray()
    }
  }

  /** Create a new ACS command builder */
  fun newCommand(): AcsCommandBuilder = AcsCommandBuilder()

  /** List available versions for a package */
  fun listVersions(manifestUrl: String, architecture: Architecture, packageId: String): AcsResult {
    return newCommand()
        .readFrom(manifestUrl)
        .getForArch(architecture)
        .withPackageId(packageId)
        .listVersions()
        .execute()
  }

  /** Get specific field value for a package */
  fun getPackageField(
      manifestUrl: String,
      architecture: Architecture,
      packageId: String,
      field: ManifestField,
      version: String? = null,
  ): AcsResult {
    val command =
        newCommand()
            .readFrom(manifestUrl)
            .getForArch(architecture)
            .withPackageId(packageId)
            .getField(field)

    version?.let { command.withVersion(it) }

    return command.execute()
  }

  /** Download a package with verification */
  fun downloadPackage(
      manifestUrl: String,
      architecture: Architecture,
      packageId: String,
      version: String? = null,
  ): AcsResult {
    val command =
        newCommand()
            .readFrom(manifestUrl)
            .getForArch(architecture)
            .withPackageId(packageId)
            .enableDownload()

    version?.let { command.withVersion(it) }

    return command.execute()
  }

  /** Download directly from URL */
  fun downloadFromUrl(url: String): AcsResult {
    return newCommand().downloadFromUrl(url).enableDownload().execute()
  }

  /** Check if ACS is available (library is always available) */
  fun isAcsAvailable(): Boolean {
    return try {
      ACS_DOWNLOAD_DIR.exists() || ACS_DOWNLOAD_DIR.mkdirs()
    } catch (e: Exception) {
      false
    }
  }

  /** Get ACS help output */
  fun getHelp(): AcsResult {
    return newCommand().showHelp().execute()
  }
}

/** Updated AcsProvider using the new command interface */
object AcsProvider {

  const val REPO_HOST = "github.com"
  const val REPO_OWNER = "AndroidCSOfficial"
  const val REPO_NAME = "android-code-studio"
  const val ACS_BUILD_SYSTEM_REPONAME = "acs-build-system"
  const val ACS_BUILD_SYSTEM_REPOURL = "https://$REPO_HOST/$REPO_OWNER/$ACS_BUILD_SYSTEM_REPONAME"

  /** Manifest url getter function */
  val getManifestUrl: String = "${ACS_BUILD_SYSTEM_REPOURL}/raw/refs/heads/main/acs-manifest.json"

  /** Enhanced ACS runner with proper command interface */
  fun acsRunner(
      packageId: String,
      artifactVersion: String? = null,
      arch: AcsCommandInterface.Architecture,
  ): AcsCommandInterface.AcsResult {
    return if (artifactVersion != null) {
      AcsCommandInterface.downloadPackage(
          manifestUrl = getManifestUrl,
          architecture = arch,
          packageId = packageId,
          version = artifactVersion,
      )
    } else {
      AcsCommandInterface.downloadPackage(
          manifestUrl = getManifestUrl,
          architecture = arch,
          packageId = packageId,
      )
    }
  }

  /** List available versions for a package */
  fun listAvailableVersions(
      packageId: String,
      arch: AcsCommandInterface.Architecture,
  ): AcsCommandInterface.AcsResult {
    return AcsCommandInterface.listVersions(
        manifestUrl = getManifestUrl,
        architecture = arch,
        packageId = packageId,
    )
  }

  /** Get package version information */
  fun getPackageVersion(
      packageId: String,
      arch: AcsCommandInterface.Architecture,
      version: String? = null,
  ): AcsCommandInterface.AcsResult {
    return AcsCommandInterface.getPackageField(
        manifestUrl = getManifestUrl,
        architecture = arch,
        packageId = packageId,
        field = AcsCommandInterface.ManifestField.VERSION,
        version = version,
    )
  }

  /** Get package URL */
  fun getPackageUrl(
      packageId: String,
      arch: AcsCommandInterface.Architecture,
      version: String? = null,
  ): AcsCommandInterface.AcsResult {
    return AcsCommandInterface.getPackageField(
        manifestUrl = getManifestUrl,
        architecture = arch,
        packageId = packageId,
        field = AcsCommandInterface.ManifestField.URL,
        version = version,
    )
  }
}
