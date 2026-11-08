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
package com.tom.rv2ide.tooling.impl

/*
 * Rework by:
 * @Author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 * Original file: Main.java.orig
 */

import com.tom.rv2ide.logging.JvmStdErrAppender
import com.tom.rv2ide.tooling.api.IToolingApiClient
import com.tom.rv2ide.tooling.api.util.ToolingApiLauncher
import com.tom.rv2ide.tooling.impl.internal.ProjectImpl
import com.tom.rv2ide.tooling.impl.progress.ForwardingProgressListener
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import org.gradle.tooling.ConfigurableLauncher
import org.gradle.tooling.events.OperationType
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Main {

  private val LOG: Logger = LoggerFactory.getLogger(Main::class.java)

  @Volatile var client: IToolingApiClient? = null

  @Volatile var future: Future<Void?>? = null

  @JvmStatic
  fun main(args: Array<String>) {
    // disable the JVM std.err appender
    System.setProperty(JvmStdErrAppender.PROP_JVM_STDERR_APPENDER_ENABLED, "false")

    // Force encoding/locale as early as possible in this JVM
    System.setProperty("file.encoding", "UTF-8")
    System.setProperty("sun.jnu.encoding", "UTF-8")
    System.setProperty("user.language", "en")
    System.setProperty("user.country", "US")
    System.setProperty("user.region", "US")
    try {
      Locale.setDefault(Locale.forLanguageTag("en-US"))
    } catch (ignored: Throwable) {
      // ignore
    }

    LOG.debug("Starting Tooling API server...")
    val project = ProjectImpl()
    val server = ToolingApiServerImpl(project)
    val launcher = ToolingApiLauncher.newServerLauncher(server, project, System.`in`, System.out)
    future = launcher.startListening()
    client = launcher.remoteProxy as IToolingApiClient
    server.connect(client!!)

    LOG.debug("Server started. Will run until shutdown message is received...")
    LOG.debug("Running on Java version: {}", System.getProperty("java.version", "<unknown>"))

    try {
      future?.get()
    } catch (cancellation: CancellationException) {
      // ignored
    } catch (e: InterruptedException) {
      LOG.error("An error occurred while waiting for shutdown message", e)
      // set the interrupt flag back
      Thread.currentThread().interrupt()
    } catch (e: ExecutionException) {
      LOG.error("An error occurred while waiting for shutdown message", e)
    } finally {
      // Cleanup should be performed in ToolingApiServerImpl.shutdown()
      // this is to make sure that the daemons are stopped in case the client doesn't call
      // shutdown()
      try {
        if (server.isInitialized || server.isConnected) {
          LOG.warn("Connection to tooling server closed without shutting it down!")
          server.shutdown().get()
        }
      } catch (e: InterruptedException) {
        LOG.error("An error occurred while shutting down tooling API server", e)
        Thread.currentThread().interrupt()
      } catch (e: ExecutionException) {
        LOG.error("An error occurred while shutting down tooling API server", e)
      } finally {
        future = null
        client = null
        LOG.info("Tooling API server shutdown complete")
      }
    }
  }

  fun checkGradleWrapper() {
    client?.let {
      LOG.info("Checking gradle wrapper availability...")
      try {
        val availability = it.checkGradleWrapperAvailability().get()
        if (!availability.isAvailable) {
          LOG.warn(
              "Gradle wrapper is not available." +
                  " Client might have failed to ensure availability." +
                  " Build might fail."
          )
        } else {
          LOG.info("Gradle wrapper is available")
        }
      } catch (e: Throwable) {
        LOG.warn("Unable to get Gradle wrapper availability from client", e)
      }
    }
  }

  @Suppress("NewApi")
  fun finalizeLauncher(launcher: ConfigurableLauncher<*>) {
    val out = LoggingOutputStream()
    launcher.setStandardError(out)
    launcher.setStandardOutput(out)
    launcher.setStandardInput(ByteArrayInputStream("NoOp".toByteArray(StandardCharsets.UTF_8)))

    // Ensure JVM platform encoding and locale are initialized before Gradle starts
    launcher.setJvmArguments(
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Duser.language=en",
        "-Duser.country=US",
        "-Dsun.stdout.encoding=UTF-8",
        "-Dsun.stderr.encoding=UTF-8",
        "-Dnative.encoding=UTF-8",
        "-Dorg.gradle.daemon.idletimeout=10800000",  // 3 hours in milliseconds
        
    )

    // Also enforce via environment for any forked JVM
    val env: MutableMap<String, String> = HashMap()
    env["JAVA_TOOL_OPTIONS"] =
        "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Duser.language=en -Duser.country=US"
    env["GRADLE_OPTS"] = "-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
    env["LANG"] = "en_US.UTF-8"
    env["LC_ALL"] = "en_US.UTF-8"

    // Inject proxy if server started one and exposed its port
    var proxyPort: String? = System.getenv("ANDROIDIDE_PROXY_PORT")
    if (proxyPort == null || proxyPort.isBlank()) {
      proxyPort = System.getProperty("ANDROIDIDE_PROXY_PORT")
    }
    if (!proxyPort.isNullOrBlank()) {
      env["systemProp.http.proxyHost"] = "127.0.0.1"
      env["systemProp.http.proxyPort"] = proxyPort
      env["systemProp.https.proxyHost"] = "127.0.0.1"
      env["systemProp.https.proxyPort"] = proxyPort
      env["systemProp.java.net.useSystemProxies"] = "false"

      // Passing them as JVM args to ensure the daemon picks them up early
      launcher.addArguments(
          "-Dhttp.proxyHost=127.0.0.1",
          "-Dhttp.proxyPort=$proxyPort",
          "-Dhttps.proxyHost=127.0.0.1",
          "-Dhttps.proxyPort=$proxyPort",
          "-Djava.net.useSystemProxies=false",
      )
    }
    launcher.setEnvironmentVariables(env)

    // launcher.addArguments("--no-daemon")
    // launcher.setJvmArguments(
    //    "-Xmx4g",
    //    "-XX:+UseG1GC",
    //    "-Dfile.encoding=UTF-8",
    //    "-Dkotlin.daemon.jvm.options=-Xmx2g"
    // );

    launcher.addProgressListener(ForwardingProgressListener(), progressUpdateTypes())

    client?.let { c ->
      try {
        val args = c.getBuildArguments().get()
        // Filter out null and blank items
        val filteredArgs = args.filter { it != null && it.isNotBlank() }
        launcher.addArguments(filteredArgs)
      } catch (e: Throwable) {
        LOG.error("Unable to get build arguments from tooling client", e)
      }
    }
  }

  fun progressUpdateTypes(): Set<OperationType> {
    val types = HashSet<OperationType>()
    // AndroidIDE currently does not handle any other type of events
    types.add(OperationType.TASK)
    types.add(OperationType.PROJECT_CONFIGURATION)
    return types
  }
}
