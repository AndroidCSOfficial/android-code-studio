package com.tom.rv2ide.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.TransitionManager
import com.tom.rv2ide.R
import com.tom.rv2ide.R.string
import com.tom.rv2ide.activities.MainActivity
import com.tom.rv2ide.adapters.TemplateWidgetsListAdapter
import com.tom.rv2ide.databinding.FragmentTemplateDetailsBinding
import com.tom.rv2ide.tasks.executeAsyncProvideError
import com.tom.rv2ide.templates.ProjectTemplateRecipeResult
import com.tom.rv2ide.templates.StringParameter
import com.tom.rv2ide.templates.Template
import com.tom.rv2ide.templates.base.FileBrowserCallback
import com.tom.rv2ide.templates.base.setFileBrowserCallback
import com.tom.rv2ide.templates.impl.ConstraintVerifier
import com.tom.rv2ide.utils.Environment
import com.tom.rv2ide.utils.TemplateRecipeExecutor
import com.tom.rv2ide.utils.flashError
import com.tom.rv2ide.utils.flashSuccess
import com.tom.rv2ide.viewmodel.MainViewModel
import org.slf4j.LoggerFactory

/**
 * A fragment which shows a wizard-like interface for creating templates.
 *
 * @author Akash Yadav
 */
class TemplateDetailsFragment :
    FragmentWithBinding<FragmentTemplateDetailsBinding>(
        R.layout.fragment_template_details,
        FragmentTemplateDetailsBinding::bind,
    ) {

  private val viewModel by viewModels<MainViewModel>(ownerProducer = { requireActivity() })

  private var currentSaveLocationParameter: StringParameter? = null

  companion object {
    internal const val ACSIDE_KEY_LAST_SAVE_LOCATION = "last_save_location"
    private val log = LoggerFactory.getLogger(TemplateDetailsFragment::class.java)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    // Set up the file browser callback
    setFileBrowserCallback(
        object : FileBrowserCallback {
          override fun openFileBrowser(currentPath: String, parameter: StringParameter) {
            currentSaveLocationParameter = parameter
            openFolderPicker(currentPath)
          }
        }
    )

    viewModel.template.observe(viewLifecycleOwner) {
      binding.widgets.adapter = null
      viewModel.postTransition(viewLifecycleOwner) { bindWithTemplate(it) }
    }

    viewModel.creatingProject.observe(viewLifecycleOwner) {
      TransitionManager.beginDelayedTransition(binding.root)
      binding.progress.isVisible = it
      binding.finish.isEnabled = !it
      binding.previous.isEnabled = !it
    }

    binding.previous.setOnClickListener { viewModel.setScreen(MainViewModel.SCREEN_TEMPLATE_LIST) }

    binding.finish.setOnClickListener {
      viewModel.creatingProject.value = true
      val template =
          viewModel.template.value
              ?: run {
                viewModel.setScreen(MainViewModel.SCREEN_MAIN)
                return@setOnClickListener
              }

      val isValid =
          template.parameters.fold(true) { isValid, param ->
            if (param is StringParameter) {
              return@fold isValid && ConstraintVerifier.isValid(param.value, param.constraints)
            } else isValid
          }

      if (!isValid) {
        viewModel.creatingProject.value = false
        flashError(string.msg_invalid_project_details)
        return@setOnClickListener
      }

      viewModel.creatingProject.value = true
      executeAsyncProvideError({ template.recipe.execute(TemplateRecipeExecutor()) }) { result, err
        ->
        viewModel.creatingProject.value = false
        if (result == null || err != null || result !is ProjectTemplateRecipeResult) {
          err?.printStackTrace()
          log.error("Failed to create project. result={}, err={}", result, err?.message)
          if (err != null) {
            flashError(err.cause?.message ?: err.message)
          } else {
            flashError(string.project_creation_failed)
          }
          return@executeAsyncProvideError
        }

        viewModel.setScreen(MainViewModel.SCREEN_MAIN)
        flashSuccess(string.project_created_successfully)

        viewModel.postTransition(viewLifecycleOwner) {
          // open the project
          (requireActivity() as MainActivity).openProject(result.data.projectDir)
        }
      }
    }

    binding.widgets.layoutManager = LinearLayoutManager(requireContext())
  }

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

  private fun openFolderPicker(currentPath: String) {
    // Use the existing pickDirectory method from BaseFragment
    pickDirectory { selectedDir ->
      currentSaveLocationParameter?.setValue(selectedDir.absolutePath, notify = true)

      // Save the selected location for next time
      saveLastSaveLocation(requireContext(), selectedDir.absolutePath)
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    setFileBrowserCallback(null)
    currentSaveLocationParameter = null
  }

  private fun bindWithTemplate(template: Template<*>?) {
    template ?: return

    binding.widgets.adapter = TemplateWidgetsListAdapter(template.widgets)
    binding.title.setText(template.templateName)
  }
}
