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

package com.tom.rv2ide.services.log

import android.util.Log
import com.tom.rv2ide.logsender.ILogReceiver
import com.tom.rv2ide.logsender.ILogSender
import com.tom.rv2ide.models.LogLine
import com.tom.rv2ide.tasks.executeAsyncProvideError
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import org.slf4j.LoggerFactory

/**
 * Handles IPC connections from other proceses.
 *
 * @author Akash Yadav
 */
class LogReceiverImpl(consumer: ((LogLine) -> Unit)? = null) : ILogReceiver.Stub(), AutoCloseable {

  private val senderHandler = MultiLogSenderHandler()
  private val senders = LogSendersRegistry()
  private val consumerLock = ReentrantLock(true)
  private val shouldStartReaders = AtomicBoolean(false)

  internal var connectionObserver: ((ConnectionObserverParams) -> Unit)? = null

  internal var consumer: ((LogLine) -> Unit)? = consumer
    set(value) {
      field = value
      senderHandler.consumer = value?.let { synchronizeConsumer(value) }
    }

  companion object {

    private val log = LoggerFactory.getLogger(LogReceiverImpl::class.java)
  }

  private fun synchronizeConsumer(consumer: (LogLine) -> Unit): (LogLine) -> Unit {
    return { line -> consumerLock.withLock { consumer(line) } }
  }

  fun acceptSenders() {
    if (senderHandler.isAlive()) {
      return
    }

    log.info("Starting log sender handler..")
    senderHandler.start()
  }

  override fun ping() {
    doAsync("ping") { Log.d("LogRecevier", "ping: Received a ping request") }
  }

  override fun connect(sender: ILogSender?) {
    doAsync("connect") {
      val port = senderHandler.getPort()
      if (port == -1) {
        log.error("A log sender is trying to connect, but log receiver is not started")
        return@doAsync
      }

      val caching = sender?.let { CachingLogSender(it, port, false) } ?: return@doAsync

      val existingSender = senders.getByPackage(caching.packageName)

      if (existingSender != null) {
        log.warn(
            "Client '${existingSender.packageName}' is reconnecting with process ID '${caching.pid}'. " +
                "Previous connection with process ID '${existingSender.pid}' will be replaced..."
        )

        // Remove the old client from the handler
        senderHandler.removeClient(existingSender.id)

        // Try to disconnect the old sender if it's still alive
        try {
          if (existingSender.isAlive()) {
            existingSender.onDisconnect()
          }
        } catch (e: Exception) {
          log.debug("Failed to disconnect old sender (probably already dead)", e)
        }

        // Remove the old sender from the registry
        senders.remove(existingSender.packageName)
      }

      connectSender(caching, port)
    }
  }

  private fun connectSender(caching: CachingLogSender, port: Int) {
    // logging this also makes sure that the package name, pid and sender ID are
    // cached when the sender binds to the service
    // these fields are then used on disconnectAll()
    log.info("Connecting to client {}:{}:{}", caching.packageName, caching.pid, caching.id)

    this.senders.put(caching)

    if (shouldStartReaders.get()) {
      caching.startReader(port)
      caching.isStarted = true
    }

    logTotalConnected()

    notifyConnectionObserver(caching.id)
  }

  internal fun startReaders() {
    this.shouldStartReaders.set(true)

    doAsync("startReaders") {
      senders.getPendingSenders().forEach { sender ->
        log.info("Notifying sender '{}' to start reading logs...", sender.packageName)
        sender.startReader(sender.port)
      }
    }
  }

  override fun disconnect(packageName: String, senderId: String) {
    doAsync("disconnect") {
      val port = senderHandler.getPort()
      if (port == -1) {
        return@doAsync
      }

      if (!senders.containsKey(packageName)) {
        log.warn(
            "Received disconnect request from a log sender which is not connected: '${packageName}'"
        )
        return@doAsync
      }

      disconnectSender(packageName, senderId)
    }
  }

  internal fun disconnectAll() {
    log.debug("Disconnecting from all senders...")
    this.senders.forEach { sender ->
      try {
        sender.onDisconnect()
        disconnectSender(sender.packageName, sender.id)
      } catch (e: Exception) {
        log.error("Failed to disconnect from sender", e)
      }
    }
  }

  private fun disconnectSender(packageName: String, senderId: String) {
    log.info("Disconnecting from client: '{}'", packageName)
    this.senderHandler.removeClient(senderId)
    this.senders.remove(packageName)
    logTotalConnected()

    notifyConnectionObserver(senderId)
  }

  override fun close() {
    // TODO : Send close request to clients
    senderHandler.close()
    consumer = null
    connectionObserver = null
    senders.clear()
  }

  private fun doAsync(actionName: String, action: () -> Unit) {
    executeAsyncProvideError(action::invoke) { _, error ->
      if (error != null) {
        log.error("Failed to perform action '{}'", actionName, error)
      }
    }
  }

  private fun notifyConnectionObserver(senderId: String) {
    connectionObserver?.invoke(ConnectionObserverParams(senderId, this.senders.size))
  }

  private fun logTotalConnected() {
    log.info("Total clients connected: {}", senders.size)
  }
}
