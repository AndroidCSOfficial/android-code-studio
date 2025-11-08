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

package com.tom.rv2ide.templates.base

import android.content.Context
import android.view.View
import com.tom.rv2ide.templates.BooleanParameter
import com.tom.rv2ide.templates.CheckBoxWidget
import com.tom.rv2ide.templates.EnumParameter
import com.tom.rv2ide.templates.FileTemplate
import com.tom.rv2ide.templates.FileTemplateRecipeResult
import com.tom.rv2ide.templates.Language
import com.tom.rv2ide.templates.ModuleTemplate
import com.tom.rv2ide.templates.ModuleTemplateData
import com.tom.rv2ide.templates.ModuleType
import com.tom.rv2ide.templates.ModuleType.AndroidApp
import com.tom.rv2ide.templates.ModuleType.AndroidLibrary
import com.tom.rv2ide.templates.NdkVersion
import com.tom.rv2ide.templates.ParameterConstraint.DIRECTORY
import com.tom.rv2ide.templates.ParameterConstraint.EXISTS
import com.tom.rv2ide.templates.ParameterConstraint.MODULE_NAME
import com.tom.rv2ide.templates.ParameterConstraint.NONEMPTY
import com.tom.rv2ide.templates.ProjectTemplate
import com.tom.rv2ide.templates.ProjectTemplateData
import com.tom.rv2ide.templates.ProjectVersionData
import com.tom.rv2ide.templates.R
import com.tom.rv2ide.templates.Sdk
import com.tom.rv2ide.templates.SpinnerWidget
import com.tom.rv2ide.templates.StringParameter
import com.tom.rv2ide.templates.TextFieldWidget
import com.tom.rv2ide.templates.base.util.getNewProjectName
import com.tom.rv2ide.templates.base.util.moduleNameToDir
import com.tom.rv2ide.templates.enumParameter
import com.tom.rv2ide.templates.minSdkParameter
import com.tom.rv2ide.templates.packageNameParameter
import com.tom.rv2ide.templates.projectLanguageParameter
import com.tom.rv2ide.templates.projectNameParameter
import com.tom.rv2ide.templates.projectNdkVersionParameter
import com.tom.rv2ide.templates.stringParameter
import com.tom.rv2ide.templates.useKtsParameter
import com.tom.rv2ide.templates.useNdkParameter
import com.tom.rv2ide.utils.AndroidUtils
import com.tom.rv2ide.utils.Environment
import java.io.File

typealias AndroidModuleTemplateConfigurator = AndroidModuleTemplateBuilder.() -> Unit

internal const val PREFS_NAME = "project_template_prefs"
internal const val ACSIDE_KEY_LAST_SAVE_LOCATION = "last_save_location"

/** Get the last saved location from acside.properties */
@PublishedApi
internal fun getLastSaveLocation(context: Context?): String {
  val acsideFile = Environment.ACSIDE
  if (acsideFile?.exists() == true) {
    try {
      val props = java.util.Properties()
      acsideFile.inputStream().use { props.load(it) }
      val savedLocation = props.getProperty(ACSIDE_KEY_LAST_SAVE_LOCATION)
      if (savedLocation != null && File(savedLocation).exists()) {
        return savedLocation
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
  return Environment.PROJECTS_DIR.absolutePath
}

/** Save the last save location to acside.properties */
@PublishedApi
internal fun saveLastSaveLocation(context: Context?, location: String) {
  val acsideFile = Environment.ACSIDE ?: return
  try {
    val props = java.util.Properties()

    // Load existing properties if file exists
    if (acsideFile.exists()) {
      acsideFile.inputStream().use { props.load(it) }
    }

    // Update the location
    props.setProperty(ACSIDE_KEY_LAST_SAVE_LOCATION, location)

    // Save back to file
    acsideFile.outputStream().use { props.store(it, "AndroidCS Configuration") }
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

/** File browser callback interface that should be implemented by the UI layer */
interface FileBrowserCallback {
  fun openFileBrowser(currentPath: String, parameter: StringParameter)
}

@PublishedApi internal var _fileBrowserCallback: FileBrowserCallback? = null

@PublishedApi internal var _currentBrowseParameter: StringParameter? = null

/** Set the file browser callback. This should be called from the UI layer. */
fun setFileBrowserCallback(callback: FileBrowserCallback?) {
  _fileBrowserCallback = callback
}

/** Create a click listener for the browse button */
@PublishedApi
internal fun createBrowseClickListener(): View.OnClickListener {
  return View.OnClickListener {
    _currentBrowseParameter?.let { param ->
      _fileBrowserCallback?.openFileBrowser(param.value, param)
    }
  }
}

/** Update the browse click listener with the parameter reference */
@PublishedApi
internal fun updateBrowseClickListener(parameter: StringParameter) {
  _currentBrowseParameter = parameter
}

/**
 * Setup base files for project templates.
 *
 * @param block Function to configure the template.
 */
inline fun baseProject(
    projectName: StringParameter = projectNameParameter(),
    packageName: StringParameter = packageNameParameter(),
    useKts: BooleanParameter = useKtsParameter(),
    useNdk: BooleanParameter = useNdkParameter(),
    minSdk: EnumParameter<Sdk> = minSdkParameter(),
    language: EnumParameter<Language> = projectLanguageParameter(),
    ndkVersion: EnumParameter<NdkVersion> = projectNdkVersionParameter(),
    projectVersionData: ProjectVersionData = ProjectVersionData(),
    context: Context? = null,
    crossinline block: ProjectTemplateBuilder.() -> Unit,
): ProjectTemplate {
  return ProjectTemplateBuilder()
      .apply {

        // When project name is changed, change the package name accordingly
        projectName.observe { name ->
          val newPackage = AndroidUtils.appNameToPackageName(name.value, packageName.value)
          packageName.setValue(newPackage)
        }

        Environment.mkdirIfNotExits(Environment.PROJECTS_DIR)

        // Create the click listener that will be used
        val browseClickListener =
            View.OnClickListener {
              _currentBrowseParameter?.let { param ->
                _fileBrowserCallback?.openFileBrowser(param.value, param)
              }
            }

        val saveLocation = stringParameter {
          name = R.string.wizard_save_location
          default = getLastSaveLocation(context)
          constraints = listOf(NONEMPTY, DIRECTORY, EXISTS)
          endIcon = { R.drawable.ic_folder }
          onEndIconClick = browseClickListener
        }

        // Store reference to saveLocation for the click listener
        saveLocation.doAfterCreateView { param ->
          updateBrowseClickListener(param as StringParameter)
        }

        projectName.doBeforeCreateView {
          it.setValue(getNewProjectName(saveLocation.value, projectName.value))
        }

        widgets(
            TextFieldWidget(projectName),
            TextFieldWidget(packageName),
            TextFieldWidget(saveLocation),
            SpinnerWidget(language),
            SpinnerWidget(minSdk),
            CheckBoxWidget(useKts),
            CheckBoxWidget(useNdk),
        )

        // Setup the required properties before executing the recipe
        preRecipe = {
          this@apply._executor = this

          // Save the selected location for future use
          saveLastSaveLocation(context, saveLocation.value)

          this@apply._data =
              ProjectTemplateData(
                  projectName.value,
                  File(saveLocation.value, projectName.value),
                  projectVersionData,
                  language = language.value,
                  useKts = useKts.value,
                  useNdk = useNdk.value,
              )

          if (data.projectDir.exists() && data.projectDir.listFiles()?.isNotEmpty() == true) {
            throw IllegalArgumentException("Project directory already exists")
          }

          setDefaultModuleData(
              ModuleTemplateData(
                  ":app",
                  appName = data.name,
                  packageName.value,
                  data.moduleNameToDir(":app"),
                  type = AndroidApp,
                  language = language.value,
                  minSdk = minSdk.value,
                  useKts = data.useKts,
                  useNdk = data.useNdk,
              )
          )
        }

        // After the recipe is executed, finalize the project creation
        // In this phase, we write the build scripts as they may need additional data based on the
        // previous recipe
        // For example, writing settings.gradle[.kts] needs to know the name of the modules so that
        // those can be included
        postRecipe = {
          // settings.gradle[.kts]
          settingsGradle()

          // gradle.properties
          gradleProps()

          // gradlew
          // gradlew.bat
          // gradle/wrapper/gradle-wrapper.jar
          // gradle/wrapper/gradle-wrapper.properties
          gradleWrapper()

          // .gitignore
          gitignore()

          // build.gradle[.kts] - Call this LAST after modules have been processed
          buildGradle()
        }

        block()
      }
      .build() as ProjectTemplate
}

/**
 * Create a new module project in this project.
 *
 * @param block The module configurator.
 */
inline fun baseAndroidModule(
    isLibrary: Boolean = false,
    context: Context? = null,
    projectBuilder: ProjectTemplateBuilder? = null,
    crossinline block: AndroidModuleTemplateConfigurator,
): ModuleTemplate {
  return AndroidModuleTemplateBuilder()
      .apply {
        this.context = context
        this.projectBuilder = projectBuilder

        val appName = if (isLibrary) null else projectNameParameter()
        val language = projectLanguageParameter()
        val ndkVersion = projectNdkVersionParameter()
        val minSdk = minSdkParameter()
        val packageName = packageNameParameter()
        val useKts = useKtsParameter()
        val useNdk = useNdkParameter()

        val moduleName = stringParameter {
          name = R.string.wizard_module_name
          default = ":app"
          constraints = listOf(NONEMPTY, MODULE_NAME)
        }

        val type =
            enumParameter<ModuleType> {
              name = R.string.wizard_module_type
              default = AndroidLibrary
              startIcon = { R.drawable.ic_android }
              displayName = ModuleType::typeName
            }

        widgets(TextFieldWidget(moduleName))

        appName?.let { widgets(TextFieldWidget(it)) }

        widgets(
            TextFieldWidget(packageName),
            SpinnerWidget(minSdk),
            SpinnerWidget(type),
            SpinnerWidget(language),
            CheckBoxWidget(useKts),
            CheckBoxWidget(useNdk),
        )

        preRecipe = commonPreRecipe {
          ModuleTemplateData(
              name = moduleName.value,
              appName = appName?.value,
              packageName = packageName.value,
              projectDir = requireProjectData().moduleNameToDir(moduleName.value),
              type = type.value,
              language = language.value,
              minSdk = minSdk.value,
              useKts = useKts.value,
              useNdk = useNdk.value,
          )
        }
        postRecipe = commonPostRecipe()

        block()
      }
      .build() as ModuleTemplate
}

/**
 * Creates a template for a file.
 *
 * @param dir The directory in which the file will be created.
 * @param configurator The configurator to configure the template.
 * @return The [FileTemplate].
 */
inline fun <R : FileTemplateRecipeResult> baseFile(
    dir: File,
    crossinline configurator: FileTemplateBuilder<R>.() -> Unit,
): FileTemplate<R> {
  return FileTemplateBuilder<R>(dir).apply(configurator).build() as FileTemplate<R>
}
