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

package com.tom.rv2ide.lsp.kotlin;

import androidx.annotation.NonNull;
import com.tom.rv2ide.lsp.kotlin.compiler.KotlinCompilerService;
import com.tom.rv2ide.projects.ModuleProject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides {@link KotlinCompilerService} instances for different {@link ModuleProject}s.
 * Similar to JavaCompilerProvider but for Kotlin language server.
 *
 * @author Tom
 */
public class KotlinCompilerProvider {

  private static KotlinCompilerProvider sInstance;
  private final Map<ModuleProject, KotlinCompilerService> mCompilers = new ConcurrentHashMap<>();

  private KotlinCompilerProvider() {}

  @NonNull
  public static KotlinCompilerService get(ModuleProject module) {
    return KotlinCompilerProvider.getInstance().forModule(module);
  }

  public static KotlinCompilerProvider getInstance() {
    if (sInstance == null) {
      sInstance = new KotlinCompilerProvider();
    }

    return sInstance;
  }

  @NonNull
  public synchronized KotlinCompilerService forModule(ModuleProject module) {
    // A module instance is set to the compiler only in case the project is initialized or
    // this method was called with other module instance.
    final KotlinCompilerService cached = mCompilers.get(module);
    if (cached != null && cached.getModule() != null) {
      return cached;
    }

    final KotlinCompilerService newInstance = new KotlinCompilerService(module);
    mCompilers.put(module, newInstance);

    return newInstance;
  }

  // TODO This currently destroys all the compiler instances
  //  We must have a method to destroy only the required instance in
  //  KotlinLanguageServer.handleFailure(LSPFailure)
  public synchronized void destroy() {
    for (final KotlinCompilerService compiler : mCompilers.values()) {
      compiler.destroy();
    }
    mCompilers.clear();
  }
}
