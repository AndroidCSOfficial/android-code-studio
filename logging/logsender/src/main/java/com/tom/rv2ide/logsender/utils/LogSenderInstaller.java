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

package com.tom.rv2ide.logsender.utils;

import static com.tom.rv2ide.logsender.LogSender.PACKAGE_ANDROIDIDE;

import android.app.Application;
import android.app.BackgroundServiceStartNotAllowedException;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.app.Activity;
import android.os.Bundle;
import com.tom.rv2ide.logsender.LogSender;
import com.tom.rv2ide.logsender.LogSenderService;

/**
 * Content providers are loaded before the application class is created. {@link LogSenderInstaller}
 * is used to install {@link LogSender} on application start.
 * <p>
 * {@link LogSenderInstaller} automatically sets up the LogSender code that runs in the main app
 * process.
 *
 * @author Akash Yadav
 * @see <a
 * href="https://github.com/square/leakcanary/blob/main/leakcanary-object-watcher-android/src/main/java/leakcanary/internal/MainProcessAppWatcherInstaller.kt">MainProcessAppWatcherInstaller</a>
 * Modifications by
 * Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
public class LogSenderInstaller extends ContentProvider {

  @Override
  public boolean onCreate() {
    final Application application = ((Application) getContext());
    if (PACKAGE_ANDROIDIDE.equals(application.getPackageName())) {
      // do not send logs to self
      return true;
    }

    // Register a lifecycle callback to restart service when app is restarted
    application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
      private boolean serviceStarted = false;

      @Override
      public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (!serviceStarted) {
          serviceStarted = true;
          startLogSenderService(application);
        }
      }

      @Override
      public void onActivityStarted(Activity activity) {}

      @Override
      public void onActivityResumed(Activity activity) {}

      @Override
      public void onActivityPaused(Activity activity) {}

      @Override
      public void onActivityStopped(Activity activity) {}

      @Override
      public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

      @Override
      public void onActivityDestroyed(Activity activity) {}
    });

    // Also start immediately
    startLogSenderService(application);

    return true;
  }

  private void startLogSenderService(Application application) {
    try {
      final Intent intent = new Intent(application, LogSenderService.class);
      intent.setAction(LogSenderService.ACTION_START_SERVICE);

      if (VERSION.SDK_INT >= VERSION_CODES.O) {
        application.startForegroundService(intent);
      } else {
        application.startService(intent);
      }
    } catch (Exception e) {
      // starting a background service is not allowed on Android 12+
      // ignore the BackgroundServiceStartNotAllowedException in such cases
      if (VERSION.SDK_INT < VERSION_CODES.S
          || !(e instanceof BackgroundServiceStartNotAllowedException)) {
        Logger.error("Failed to start LogSenderService", e);
      }
    }
  }


  @Override
  public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
      String sortOrder
  ) {
    return null;
  }


  @Override
  public String getType(Uri uri) {
    return null;
  }


  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs
  ) {
    return 0;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs
  ) {
    return 0;
  }
}
