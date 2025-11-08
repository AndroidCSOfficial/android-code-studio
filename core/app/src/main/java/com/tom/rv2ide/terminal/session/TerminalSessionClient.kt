package com.tom.rv2ide.terminal.session

/** Interface for handling terminal session events */
interface TerminalSessionClient {
  /**
   * Called when text output is received from the terminal session
   *
   * @param session The terminal session that produced the output
   * @param text The text output received
   */
  fun onTextChanged(session: TerminalSession, text: String)

  /**
   * Called when the terminal session finishes
   *
   * @param session The terminal session that finished
   */
  fun onSessionFinished(session: TerminalSession)
}
