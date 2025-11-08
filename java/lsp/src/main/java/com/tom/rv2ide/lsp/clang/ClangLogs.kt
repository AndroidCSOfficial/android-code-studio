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
package com.tom.rv2ide.lsp.clang

import org.slf4j.LoggerFactory

/*
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */

object ClangLogs {
  private const val ENABLE_LSP_LOGS = true

  private val log = LoggerFactory.getLogger(ClangLogs::class.java)

  fun error(message: String) {
    if (ENABLE_LSP_LOGS) log.error(message)
  }

  fun error(message: String, throwable: Throwable) {
    if (ENABLE_LSP_LOGS) log.error(message, throwable)
  }

  fun warn(message: String) {
    if (ENABLE_LSP_LOGS) log.warn(message)
  }

  fun warn(message: String, throwable: Throwable) {
    if (ENABLE_LSP_LOGS) log.warn(message, throwable)
  }

  fun info(message: String) {
    if (ENABLE_LSP_LOGS) log.info(message)
  }

  fun debug(message: String) {
    if (ENABLE_LSP_LOGS) log.debug(message)
  }

  fun trace(message: String) {
    if (ENABLE_LSP_LOGS) log.trace(message)
  }

  fun error(format: String, vararg args: Any?) {
    if (ENABLE_LSP_LOGS) {
      val safeArgs = args.map { it ?: "null" }.toTypedArray()
      log.error(format, *safeArgs)
    }
  }

  fun warn(format: String, vararg args: Any?) {
    if (ENABLE_LSP_LOGS) {
      val safeArgs = args.map { it ?: "null" }.toTypedArray()
      log.warn(format, *safeArgs)
    }
  }

  fun info(format: String, vararg args: Any?) {
    if (ENABLE_LSP_LOGS) {
      val safeArgs = args.map { it ?: "null" }.toTypedArray()
      log.info(format, *safeArgs)
    }
  }

  fun debug(format: String, vararg args: Any?) {
    if (ENABLE_LSP_LOGS) {
      val safeArgs = args.map { it ?: "null" }.toTypedArray()
      log.debug(format, *safeArgs)
    }
  }

  fun trace(format: String, vararg args: Any?) {
    if (ENABLE_LSP_LOGS) {
      val safeArgs = args.map { it ?: "null" }.toTypedArray()
      log.trace(format, *safeArgs)
    }
  }
}
