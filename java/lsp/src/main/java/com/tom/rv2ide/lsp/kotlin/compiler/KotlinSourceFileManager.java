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

package com.tom.rv2ide.lsp.kotlin.compiler;

import static java.util.Collections.emptySet;

import androidx.annotation.NonNull;
import com.tom.rv2ide.javac.config.JavacConfigProvider;
import com.tom.rv2ide.javac.services.fs.AndroidFsProviderImpl;
import com.tom.rv2ide.projects.android.AndroidModule;
import com.tom.rv2ide.projects.ModuleProject;
import com.tom.rv2ide.utils.Environment;
import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tom.rv2ide.lsp.kotlin.KslLogs;

/**
 * Kotlin source file manager that configures classpaths for Android modules.
 * Similar to Java SourceFileManager but adapted for Kotlin language server.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
public class KotlinSourceFileManager {

  public static final KotlinSourceFileManager NO_MODULE;
  private static final Logger LOG;
  private static final Map<ModuleProject, KotlinSourceFileManager> cachedFileManagers =
      new ConcurrentHashMap<>();

  static {
    // Initialize LOG first to prevent NPE
    LOG = LoggerFactory.getLogger(KotlinSourceFileManager.class);
    NO_MODULE = new KotlinSourceFileManager(null);
  }

  private final ModuleProject module;
  private final Set<File> classPaths;
  private final Set<File> bootClassPaths;

  private KotlinSourceFileManager(final ModuleProject module) {
    this.module = module;

    AndroidFsProviderImpl.INSTANCE.init();

    if (module == null) {
      this.classPaths = emptySet();
      this.bootClassPaths = emptySet();
      return;
    }

    // Must be set before setting classpaths
    System.setProperty(JavacConfigProvider.PROP_ANDROIDIDE_JAVA_HOME,
        Environment.JAVA_HOME.getAbsolutePath());

    this.classPaths = configureClasspaths(module);
    this.bootClassPaths = configureBootClasspaths(module);
  }

  @NonNull
  private Set<File> configureClasspaths(final ModuleProject module) {
    if (module == null) {
      return emptySet();
    }

    return module.getCompileClasspaths();
  }

  @NonNull
  private Set<File> configureBootClasspaths(final ModuleProject module) {
    if (module == null) {
      return emptySet();
    }

    if (module instanceof AndroidModule) {
      final AndroidModule androidModule = (AndroidModule) module;
      return new java.util.HashSet<>(androidModule.getBootClassPaths());
    }

    return emptySet();
  }

  /**
   * Get the configured classpaths for this module.
   */
  public Set<File> getClassPaths() {
    return classPaths;
  }

  /**
   * Get the configured boot classpaths for this module.
   */
  public Set<File> getBootClassPaths() {
    return bootClassPaths;
  }

  /**
   * Get all classpaths including boot classpaths.
   */
  public Set<File> getAllClassPaths() {
    Set<File> allPaths = new java.util.HashSet<>();
    allPaths.addAll(classPaths);
    allPaths.addAll(bootClassPaths);
    return allPaths;
  }

  /**
   * Check if this file manager is for an Android module.
   */
  public boolean isAndroidModule() {
    return module instanceof AndroidModule;
  }

  /**
   * Get the module associated with this file manager.
   */
  public ModuleProject getModule() {
    return module;
  }

  /**
   * Destroy this file manager and clean up resources.
   */
  public void destroy() {
  }

  public static KotlinSourceFileManager forModule(@NonNull ModuleProject project) {
    Objects.requireNonNull(project);
    return cachedFileManagers.computeIfAbsent(project, KotlinSourceFileManager::createForModule);
  }

  private static KotlinSourceFileManager createForModule(@NonNull ModuleProject project) {
    return new KotlinSourceFileManager(project);
  }

  public static void clearCache() {
    for (final KotlinSourceFileManager fileManager : cachedFileManagers.values()) {
      fileManager.destroy();
    }

    cachedFileManagers.clear();
  }
}
