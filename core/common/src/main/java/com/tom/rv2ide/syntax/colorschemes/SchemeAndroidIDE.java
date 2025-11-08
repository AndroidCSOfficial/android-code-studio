/*
 * This file is part of AndroidIDE.
 *
 * AndroidIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AndroidIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AndroidIDE.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package com.tom.rv2ide.syntax.colorschemes;

import android.content.Context;
import android.graphics.Color;
import io.github.rosemoe.sora.lang.styling.TextStyle;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

/**
 * Base class for editor color schemes in AndroidIDE. If you're implementing a color scheme for the
 * IDE, this should be the base class instead of {@link EditorColorScheme}.
 *
 * @author Akash Yadav
 */
public class SchemeAndroidIDE extends EditorColorScheme {

  protected SchemeAndroidIDE() {}

  protected static int endColorId = EditorColorScheme.END_COLOR_ID;
  
  public static final int COMPLETION_WND_BG_CURRENT_ITEM = ++endColorId;
  public static final int COMPLETION_WND_TEXT_LABEL = ++endColorId;
  public static final int COMPLETION_WND_TEXT_TYPE = ++endColorId;
  public static final int COMPLETION_WND_TEXT_API = ++endColorId;
  public static final int COMPLETION_WND_TEXT_DETAIL = ++endColorId;
  
  public static final int LOG_TEXT_INFO = ++endColorId;
  public static final int LOG_TEXT_DEBUG = ++endColorId;
  public static final int LOG_TEXT_VERBOSE = ++endColorId;
  public static final int LOG_TEXT_ERROR = ++endColorId;
  public static final int LOG_TEXT_WARNING = ++endColorId;
  public static final int LOG_PRIORITY_FG_INFO = ++endColorId;
  public static final int LOG_PRIORITY_FG_DEBUG = ++endColorId;
  public static final int LOG_PRIORITY_FG_VERBOSE = ++endColorId;
  public static final int LOG_PRIORITY_FG_ERROR = ++endColorId;
  public static final int LOG_PRIORITY_FG_WARNING = ++endColorId;
  public static final int LOG_PRIORITY_BG_INFO = ++endColorId;
  public static final int LOG_PRIORITY_BG_DEBUG = ++endColorId;
  public static final int LOG_PRIORITY_BG_VERBOSE = ++endColorId;
  public static final int LOG_PRIORITY_BG_ERROR = ++endColorId;
  public static final int LOG_PRIORITY_BG_WARNING = ++endColorId;
  public static final int XML_TAG = ++endColorId;
  public static final int FIELD = ++endColorId;
  public static final int TYPE_NAME = ++endColorId;
  public static final int TODO_COMMENT = ++endColorId;
  public static final int FIXME_COMMENT = ++endColorId;

  /**
   * Delegates to {@link TextStyle#makeStyle(int)}
   *
   * @param id The color id.
   * @return The style flags.
   */
  public static long get(int id) {
    return TextStyle.makeStyle(id);
  }

  /**
   * Create style for keywords. Convenient method to avoid calling {@link TextStyle#makeStyle(int,
   * int, boolean, boolean, boolean)}
   *
   * @return The default style for keywords.
   */
  public static long forKeyword() {
    return TextStyle.makeStyle(KEYWORD, 0, true, false, false);
  }

  /**
   * Create style for string literals. The returned style sets the {@link
   * TextStyle#NO_COMPLETION_BIT}.
   *
   * @return The style for string literals.
   */
  public static long forString() {
    return TextStyle.makeStyle(LITERAL, true);
  }

  /**
   * Create color style for default comments.
   *
   * @return The style for {@link #COMMENT}.
   */
  public static long forComment() {
    return TextStyle.makeStyle(COMMENT, true);
  }

  /**
   * Create style for the given id without completions.
   *
   * @param id The id to create style for.
   * @return The style for the id.
   */
  public static long withoutCompletion(int id) {
    return TextStyle.makeStyle(id, true);
  }

  public static SchemeAndroidIDE newInstance(Context context) {
    if (context == null) {
      return new SchemeAndroidIDE();
    }
    final var scheme = new DynamicColorScheme();
    scheme.apply(context);
    return scheme;
  }

  @Override
  public void applyDefault() {
    // Apply default colors
    super.applyDefault();

    // Apply Dracula theme colors
    setColor(WHOLE_BACKGROUND, 0xff282a36);
    setColor(LINE_NUMBER_BACKGROUND, 0xff282a36);
    setColor(MATCHED_TEXT_BACKGROUND, 0xffff79c6);
    setColor(SELECTED_TEXT_BACKGROUND, 0xff44475a);
    setColor(LINE_DIVIDER, Color.TRANSPARENT);
    setColor(LINE_NUMBER, 0xff6272a4);
    setColor(LINE_NUMBER_CURRENT, 0xfff8f8f2);
    setColor(LINE_NUMBER_PANEL, 0xff21222c);
    setColor(LINE_NUMBER_PANEL_TEXT, 0xfff8f8f2);
    setColor(TEXT_NORMAL, 0xfff8f8f2);
    setColor(TEXT_SELECTED, 0xfff8f8f2);
    setColor(SELECTION_INSERT, 0xffff79c6);
    setColor(SELECTION_HANDLE, 0xffff79c6);
    setColor(CURRENT_LINE, 0xff44475a);
    setColor(UNDERLINE, 0xfff8f8f2);
    setColor(SCROLL_BAR_THUMB, 0xff6272a4);
    setColor(SCROLL_BAR_THUMB_PRESSED, 0xffff79c6);
    setColor(SCROLL_BAR_TRACK, 0xff44475a);
    setColor(BLOCK_LINE, 0xff6272a4);
    setColor(BLOCK_LINE_CURRENT, 0xfff8f8f2);
    setColor(COMPLETION_WND_BACKGROUND, 0xff44475a);
    setColor(COMPLETION_WND_CORNER, 0xff6272a4);
    setColor(NON_PRINTABLE_CHAR, 0xff6272a4);
    
    setColor(KEYWORD, 0xffff79c6);
    setColor(OPERATOR, 0xffff79c6);
    setColor(LITERAL, 0xfff1fa8c);
    setColor(TYPE_NAME, 0xff8be9fd);
    setColor(ANNOTATION, 0xffff79c6);
    setColor(FIELD, 0xff50fa7b);

    setColor(XML_TAG, 0xffff79c6);

    setColor(LOG_TEXT_ERROR, 0xffff5555);
    setColor(LOG_TEXT_WARNING, 0xfff1fa8c);
    setColor(LOG_TEXT_INFO, 0xff50fa7b);
    setColor(LOG_TEXT_DEBUG, 0xff8be9fd);
    setColor(LOG_TEXT_VERBOSE, 0xffbd93f9);
    setColor(LOG_PRIORITY_FG_ERROR, 0xfff8f8f2);
    setColor(LOG_PRIORITY_FG_WARNING, 0xff282a36);
    setColor(LOG_PRIORITY_FG_INFO, 0xff282a36);
    setColor(LOG_PRIORITY_FG_DEBUG, 0xff282a36);
    setColor(LOG_PRIORITY_FG_VERBOSE, 0xff282a36);
    setColor(LOG_PRIORITY_BG_ERROR, 0xffff5555);
    setColor(LOG_PRIORITY_BG_WARNING, 0xfff1fa8c);
    setColor(LOG_PRIORITY_BG_INFO, 0xff50fa7b);
    setColor(LOG_PRIORITY_BG_DEBUG, 0xff8be9fd);
    setColor(LOG_PRIORITY_BG_VERBOSE, 0xffbd93f9);

    setColor(TODO_COMMENT, 0xfff1fa8c);
    setColor(FIXME_COMMENT, 0xffff5555);
    setColor(COMMENT, 0xff6272a4);
  }

  @Override
  public boolean isDark() {
    return true;
  }
}