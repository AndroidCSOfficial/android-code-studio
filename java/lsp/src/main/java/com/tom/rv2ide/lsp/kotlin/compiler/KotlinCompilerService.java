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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.tom.rv2ide.projects.ModuleProject;
import com.tom.rv2ide.projects.android.AndroidModule;
import com.tom.rv2ide.projects.util.BootClasspathProvider;
import com.tom.rv2ide.utils.Environment;
import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kotlin compiler service that handles Android classes and local dependencies.
 * Similar to JavaCompilerService but adapted for Kotlin language server.
 *
 * @author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null
 */
public class KotlinCompilerService {

  public static final KotlinCompilerService NO_MODULE_COMPILER = new KotlinCompilerService(null);
  private static final Logger LOG = LoggerFactory.getLogger(KotlinCompilerService.class);
  
  protected final Set<String> classPathClasses;
  protected final KotlinSourceFileManager fileManager;
  protected final ModuleProject module;
  protected Set<String> bootClasspathClasses;

  // The module project must not be null
  // It is marked as nullable just for some special cases like tests
  public KotlinCompilerService(@Nullable ModuleProject module) {
    this.module = module;
    if (module == null) {
      this.fileManager = KotlinSourceFileManager.NO_MODULE;
      this.classPathClasses = Collections.emptySet();
      this.bootClasspathClasses = Collections.emptySet();
    } else {
      this.fileManager = KotlinSourceFileManager.forModule(module);
      this.classPathClasses = Collections.unmodifiableSet(module.compileClasspathClasses.allClassNames());
      this.bootClasspathClasses = Collections.unmodifiableSet(getBootclasspathClasses());
    }
  }

  private Set<String> getBootclasspathClasses() {
    Set<String> bootClasspathClasses = BootClasspathProvider.getTopLevelClasses(
        Collections.singleton(Environment.ANDROID_JAR.getAbsolutePath()));
    
    if (module != null && module instanceof AndroidModule) {
      final AndroidModule androidModule = (AndroidModule) module;
      // Use the existing public API to get boot classpaths
      final List<String> classpaths =
          androidModule.getBootClassPaths().stream().map(File::getPath).collect(Collectors.toList());
      BootClasspathProvider.update(classpaths);
      bootClasspathClasses =
          Collections.unmodifiableSet(BootClasspathProvider.getTopLevelClasses(classpaths));
    }
    return bootClasspathClasses;
  }

  public ModuleProject getModule() {
    return module;
  }

  public void destroy() {
    if (fileManager != null) {
      fileManager.destroy();
    }
  }

  /**
   * Get all available top-level types including Android classes and local dependencies.
   */
  public Set<String> getAvailableTypes() {
    Set<String> all = new java.util.TreeSet<>();
    
    // Add classpath classes (local dependencies)
    all.addAll(classPathClasses);
    
    // Add boot classpath classes (Android framework classes)
    all.addAll(bootClasspathClasses);
    
    // Add source classes from the module
    if (module != null) {
      Set<String> sourceClasses = module.compileJavaSourceClasses.allClassNames();
      all.addAll(sourceClasses);
    }
    
    return all;
  }

  /**
   * Check if a class is available in the classpath or boot classpath.
   */
  public boolean isClassAvailable(String className) {
    return classPathClasses.contains(className) || bootClasspathClasses.contains(className);
  }

  /**
   * Get the file manager for this compiler service.
   */
  public KotlinSourceFileManager getFileManager() {
    return fileManager;
  }
}
