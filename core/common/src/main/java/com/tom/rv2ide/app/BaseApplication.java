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
package com.tom.rv2ide.app;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import com.blankj.utilcode.util.ThrowableUtils;
import com.tom.rv2ide.buildinfo.BuildInfo;
import com.tom.rv2ide.common.R;
import com.tom.rv2ide.managers.PreferenceManager;
import com.tom.rv2ide.managers.ToolsManager;
import com.tom.rv2ide.utils.Environment;
import com.tom.rv2ide.utils.FileUtil;
import com.tom.rv2ide.utils.FlashbarUtilsKt;
import com.tom.rv2ide.utils.JavaCharacter;
import com.tom.rv2ide.utils.VMUtils;
import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class BaseApplication extends Application {

  public static final String NOTIFICATION_GRADLE_BUILD_SERVICE = "17571";
  public static final String TELEGRAM_GROUP_URL = "https://t.me/acs_x";
  public static final String TELEGRAM_CHANNEL_URL = "https://t.me/rv2ide";  
  public static final String SPONSOR_URL = BuildInfo.REPO_URL;
  public static final String DOCS_URL = "https://docs.androidide.com";
  public static final String CONTRIBUTOR_GUIDE_URL =
      BuildInfo.REPO_URL + "/blob/dev/docs/en/CONTRIBUTING.md";
  public static final String EMAIL = "rv2.code.studio@gmail.com";
  
  private static BaseApplication instance;
  private PreferenceManager mPrefsManager;
  
  private Object kotlinProcessManager;
  private CountDownLatch serverStartLatch;
  private volatile boolean isStarting = false;

  public static BaseApplication getBaseInstance() {
    return instance;
  }

  @Override
  public void onCreate() {
    instance = this;
    Environment.init(this);
    super.onCreate();

    mPrefsManager = new PreferenceManager(this);
    JavaCharacter.initMap();

    if (!VMUtils.isJvm()) {
      ToolsManager.init(this, null);
    }
  }
  
  // public Object getKotlinProcessManager() {
  //   synchronized (this) {
  //       // Check if we need to start/restart the server
  //       if (kotlinProcessManager == null || !isServerAlive()) {
  //           if (isStarting) {
  //               android.util.Log.w("BaseApplication", "Server is already starting, waiting...");
  //           } else {
  //               android.util.Log.i("BaseApplication", "Starting/restarting Kotlin server...");
  //               serverStartLatch = new CountDownLatch(1);
  //               isStarting = true;
  //               // initKotlinServer();
  //           }
  //       } else {
  //           android.util.Log.i("BaseApplication", "Server already running and alive");
  //           return kotlinProcessManager;
  //       }
  //   }
    
  //   // Wait for server to be ready (max 10 seconds)
  //   try {
  //       if (!serverStartLatch.await(10, TimeUnit.SECONDS)) {
  //           android.util.Log.e("BaseApplication", "Timeout waiting for Kotlin server to start");
  //           isStarting = false;
  //           return null;
  //       }
  //   } catch (InterruptedException e) {
  //       android.util.Log.e("BaseApplication", "Interrupted while waiting for Kotlin server", e);
  //       Thread.currentThread().interrupt();
  //       isStarting = false;
  //       return null;
  //   }
    
  //   return kotlinProcessManager;
  // }
  
  private boolean isServerAlive() {
      if (kotlinProcessManager == null) {
          return false;
      }
      
      try {
          Class<?> managerClass = kotlinProcessManager.getClass();
          java.lang.reflect.Field processField = managerClass.getDeclaredField("process");
          processField.setAccessible(true);
          Object process = processField.get(kotlinProcessManager);
          
          if (process == null) {
              return false;
          }
          
          java.lang.reflect.Method isAliveMethod = process.getClass().getMethod("isAlive");
          Boolean isAlive = (Boolean) isAliveMethod.invoke(process);
          
          return isAlive != null && isAlive;
      } catch (Exception e) {
          android.util.Log.e("BaseApplication", "Error checking if server is alive", e);
          return false;
      }
  }
  
  private void initKotlinServer() {
    new Thread(() -> {
      try {
        android.util.Log.i("BaseApplication", "=== STARTING KOTLIN SERVER INITIALIZATION ===");
        
        // Use reflection to avoid circular dependency
        Class<?> managerClass = Class.forName("com.tom.rv2ide.lsp.kotlin.KotlinServerProcessManager");
        Class<?> providerClass = Class.forName("com.tom.rv2ide.lsp.kotlin.KotlinClasspathProvider");
        
        android.util.Log.i("BaseApplication", "Creating manager and provider instances...");
        Object manager = managerClass.getConstructor(android.content.Context.class).newInstance(this);
        Object provider = providerClass.getConstructor().newInstance();
        
        android.util.Log.i("BaseApplication", "Calling startServer method...");
        java.lang.reflect.Method startMethod = managerClass.getMethod("startServer", providerClass);
        startMethod.invoke(manager, provider);
        
        // Wait for server to initialize
        Thread.sleep(3000);
        
        kotlinProcessManager = manager;
        
        android.util.Log.i("BaseApplication", "=== KOTLIN SERVER STARTED SUCCESSFULLY ===");
      } catch (Exception e) {
        android.util.Log.e("BaseApplication", "FATAL: Failed to start Kotlin Language Server", e);
        e.printStackTrace();
      } finally {
        isStarting = false;
        if (serverStartLatch != null) {
            serverStartLatch.countDown();
        }
      }
    }).start();
  }

  public void writeException(Throwable th) {
    FileUtil.writeFile(new File(FileUtil.getExternalStorageDir(), "idelog.txt").getAbsolutePath(),
        ThrowableUtils.getFullStackTrace(th));
  }

  public PreferenceManager getPrefManager() {
    return mPrefsManager;
  }

  public File getProjectsDir() {
    return Environment.PROJECTS_DIR;
  }

  public void openTelegramGroup() {
    openTelegram(BaseApplication.TELEGRAM_GROUP_URL);
  }

  public void openTelegramChannel() {
    openTelegram(BaseApplication.TELEGRAM_CHANNEL_URL);
  }

  public void openGitHub() {
    openUrl(BuildInfo.REPO_URL);
  }

  public void openWebsite() {
    openUrl(BuildInfo.PROJECT_SITE);
  }

  public void openDonationsPage() {
    openUrl(SPONSOR_URL);
  }

  public void openDocs() {
    openUrl(DOCS_URL);
  }

  public void emailUs() {
    openUrl("mailto:" + EMAIL);
  }

  public void openUrl(String url) {
    openUrl(url, null);
  }

  public void openTelegram(String url) {
    openUrl(url, "org.telegram.messenger");
  }

  public void openUrl(String url, String pkg) {
    try {
      Intent open = new Intent();
      open.setAction(Intent.ACTION_VIEW);
      open.setData(Uri.parse(url));
      open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (pkg != null) {
        open.setPackage(pkg);
      }
      startActivity(open);
    } catch (Throwable th) {
      if (pkg != null) {
        openUrl(url);
      } else if (th instanceof ActivityNotFoundException) {
        FlashbarUtilsKt.flashError(R.string.msg_app_unavailable_for_intent);
      } else {
        FlashbarUtilsKt.flashError(th.getMessage());
      }
    }
  }
}
