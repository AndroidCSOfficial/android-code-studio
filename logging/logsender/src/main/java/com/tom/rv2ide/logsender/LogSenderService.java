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

package com.tom.rv2ide.logsender;

import android.app.Notification;
import android.app.Notification.BigTextStyle;
import android.app.Notification.Builder;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;
import com.tom.rv2ide.logsender.utils.Logger;

/**
 * A {@link Service} which runs in the background and sends logs to AndroidIDE.
 *
 * @author Akash Yadav
 * Modifications by
 * Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
 
public class LogSenderService extends Service {

  private final LogSender logSender = new LogSender();
  private static final int NOTIFICATION_ID = 644;
  private static final String NOTIFICATION_CHANNEL_NAME = "LogSender Service";
  private static final String NOTIFICATION_TITLE = "LogSender Service";
  private static final String NOTIFICATION_TEXT = "Connected to AndroidIDE";
  private static final String NOTIFICATION_CHANNEL_ID = "ide.logsender.service";
  public static final String ACTION_START_SERVICE = "ide.logsender.service.start";
  public static final String ACTION_STOP_SERVICE = "ide.logsender.service.stop";

  private boolean hasCleanedUp = false;
  private boolean wasConnected = false;
  
  private Handler reconnectHandler;
  private static final int RECONNECT_DELAY_MS = 5000;
  private int reconnectAttempts = 0;
  private static final int MAX_RECONNECT_ATTEMPTS = 10;

  @Override
  public void onCreate() {
    Logger.debug("[LogSenderService] onCreate()");
    super.onCreate();
    setupNotificationChannel();
    startForeground(NOTIFICATION_ID, buildNotification());
    reconnectHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  public IBinder onBind(Intent intent) {
    Logger.debug("Unexpected request to bind.", intent);
    return null;
  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    Logger.debug("onStartCommand", intent, flags, startId);

    if (intent == null) {
      Logger.info("Service restarted by system, attempting to reconnect...");
      actionStartService();
      return START_STICKY;
    }

    switch (intent.getAction()) {
      case ACTION_START_SERVICE:
        actionStartService();
        break;
      case ACTION_STOP_SERVICE:
        actionStopService();
        break;
      default:
        Logger.error("Unknown service action:", intent.getAction());
        break;
    }

    return START_STICKY;
  }

  private void actionStartService() {
    Logger.info("Starting log sender service...");

    if (logSender.isConnected() || logSender.isBinding()) {
      Logger.debug("Already connected or binding. Skipping.");
      reconnectAttempts = 0;
      return;
    }

    boolean result = false;
    try {
      result = logSender.bind(getApplicationContext());
      Logger.debug("Bind to AndroidIDE:", result);
      if (result) {
        wasConnected = true;
        reconnectAttempts = 0;
      }
    } catch (Exception err) {
      Logger.error(getString(R.string.msg_bind_service_failed), err);
    }

    if (!result) {
      if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
        reconnectAttempts++;
        Logger.info("Connection failed, scheduling reconnect attempt " + reconnectAttempts);
        scheduleReconnect();
      } else {
        Logger.error("Max reconnection attempts reached, stopping service");
        Toast.makeText(this, getString(R.string.msg_bind_service_failed), Toast.LENGTH_SHORT).show();
        actionStopService();
      }
    }
  }

  private void scheduleReconnect() {
    reconnectHandler.postDelayed(new Runnable() {
      @Override
      public void run() {
        Logger.info("Attempting automatic reconnection...");
        actionStartService();
      }
    }, RECONNECT_DELAY_MS);
  }

  private void actionStopService() {
    Logger.info("Stopping log sender service...");
    wasConnected = false;
    stopSelf();
  }

@Override
public void onTaskRemoved(Intent rootIntent) {
    Logger.debug("[LogSenderService] [onTaskRemoved]", rootIntent);
    
    // Cancel any pending reconnection attempts
    if (reconnectHandler != null) {
        reconnectHandler.removeCallbacksAndMessages(null);
    }
    
    // Reset reconnect attempts since task was removed
    reconnectAttempts = 0;
    
    // If we were connected, try to restart the service
    if (wasConnected) {
        Logger.info("Task removed but was connected - scheduling restart");
        Intent restartIntent = new Intent(getApplicationContext(), LogSenderService.class);
        restartIntent.setAction(ACTION_START_SERVICE);
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            startForegroundService(restartIntent);
        } else {
            startService(restartIntent);
        }
    } else {
        Logger.info("Task removed, service will stop");
    }
}

@Override
public void onDestroy() {
    Logger.debug("[LogSenderService] [onDestroy]");
    
    if (reconnectHandler != null) {
        reconnectHandler.removeCallbacksAndMessages(null);
    }
    
    if (hasCleanedUp) {
        Logger.debug("Already cleaned up. Ignored.");
        super.onDestroy();
        return;
    }

    // Remove the early exit check - always try to clean up
    Logger.warn("Service is being destroyed. Destroying log sender...");
    logSender.destroy(getApplicationContext());
    hasCleanedUp = true;
    super.onDestroy();
}

  private void setupNotificationChannel() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }

    NotificationChannel channel = new NotificationChannel(
        NOTIFICATION_CHANNEL_ID,
        NOTIFICATION_CHANNEL_NAME,
        NotificationManager.IMPORTANCE_LOW
    );

    NotificationManager notificationManager = getSystemService(NotificationManager.class);
    if (notificationManager != null) {
      notificationManager.createNotificationChannel(channel);
    }
  }

  private Notification buildNotification() {
    Resources res = getResources();

    int priority = Notification.PRIORITY_LOW;

    final Builder builder = new Builder(this);
    builder.setContentTitle(NOTIFICATION_TITLE);
    builder.setContentText(NOTIFICATION_TEXT);
    builder.setStyle(new BigTextStyle().bigText(NOTIFICATION_TEXT));
    builder.setPriority(priority);

    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      builder.setChannelId(NOTIFICATION_CHANNEL_ID);
    }

    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
      builder.setShowWhen(false);
    }

    builder.setSmallIcon(R.drawable.ic_androidide_log);

    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      builder.setColor(0xFF607D8B);
    }

    builder.setOngoing(true);

    Intent exitIntent = new Intent(this, LogSenderService.class).setAction(ACTION_STOP_SERVICE);
    builder.addAction(android.R.drawable.ic_delete,
        res.getString(R.string.notification_action_exit),
        PendingIntent.getService(this, 0, exitIntent, PendingIntent.FLAG_IMMUTABLE));

    return builder.build();
  }
}
