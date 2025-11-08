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

package com.tom.rv2ide.fragments.output

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.tom.rv2ide.R
import com.tom.rv2ide.logging.LifecycleAwareAppender
import com.tom.rv2ide.resources.R.string
import org.slf4j.LoggerFactory

/**
 * Fragment to show IDE logs.
 *
 * @author Akash Yadav
 */
class IDELogFragment : LogViewFragment() {

  private val lifecycleAwareAppender = LifecycleAwareAppender(Lifecycle.State.CREATED)
  private lateinit var sharedPreferences: SharedPreferences
  private val logsEnabledKey = "idepref_ide_logs_enabled"

  override fun isSimpleFormattingEnabled() = true

  override fun getFilename() = "ide_logs"

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Initialize SharedPreferences
    sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())

    val areLogsEnabled = sharedPreferences.getBoolean(logsEnabledKey, true) // Default to true

    if (areLogsEnabled) {
      setupLogging()
    } else {
      showLogsDisabledMessage()
    }
  }

  private fun setupLogging() {
    emptyStateViewModel.emptyMessage.value = getString(R.string.msg_emptyview_idelogs)

    lifecycleAwareAppender.consumer = this::appendLine
    lifecycleAwareAppender.attachTo(viewLifecycleOwner)

    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    lifecycleAwareAppender.context = loggerContext
    lifecycleAwareAppender.start()

    rootLogger.addAppender(lifecycleAwareAppender)
  }

  private fun showLogsDisabledMessage() {
    emptyStateViewModel.emptyMessage.value = getString(R.string.msg_ide_logs_disabled)
    // clearLogs()
  }

  override fun onDestroy() {
    super.onDestroy()
    lifecycleAwareAppender.stop()

    val logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger
    logger.detachAppender(lifecycleAwareAppender)
  }
}
