/*
 *  This file is part of AndroidCodeStudio.
 *
 *  AndroidCodeStudio is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  AndroidCodeStudio is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *   along with AndroidCodeStudio.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tom.rv2ide.fragments.sidebar

/** @Author Mohammed-baqer-null @ https://github.com/Mohammed-baqer-null */
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.ThreadUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.R
import com.tom.rv2ide.app.BaseApplication
import com.tom.rv2ide.fragments.sidebar.utils.AIResponseHandler
import com.tom.rv2ide.fragments.sidebar.utils.ResponseMode
import com.tom.rv2ide.fragments.sidebar.utils.ai_agents
import com.tom.rv2ide.fragments.sidebar.utils.showErrorDialog
import com.tom.rv2ide.managers.PreferenceManager
import com.tom.rv2ide.projects.ModuleProject
import com.tom.rv2ide.projects.android.AndroidModule
import com.tom.rv2ide.projects.internal.ProjectManagerImpl
import com.tom.rv2ide.services.ai.AnthropicService
import com.tom.rv2ide.services.ai.ContextAwareAIService
import com.tom.rv2ide.services.ai.DeepSeekAIService
import com.tom.rv2ide.services.ai.GeminiAIService
import com.tom.rv2ide.services.ai.OpenAIService
import com.tom.rv2ide.services.ai.ProjectContextManager
import com.tom.rv2ide.utils.ProjectHelper
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class AIAgentFragment : Fragment() {

  companion object {
    private val log = LoggerFactory.getLogger(AIAgentFragment::class.java)
  }

  // Context-aware AI services that maintain project and conversation context
  private val geminiService = ContextAwareAIService(GeminiAIService(), "Gemini")
  private var isGemini = false
  private val deepSeekService = ContextAwareAIService(DeepSeekAIService(), "DeepSeek")
  private var isDeepseek = false
  private val openAIService = ContextAwareAIService(OpenAIService(), "OpenAI")
  private var isOpenAI = false
  private val anthropicService = ContextAwareAIService(AnthropicService(), "Anthropic")
  private var isAnthropic = false

  // Project context manager for intelligent caching
  private val projectContextManager = ProjectContextManager.getInstance()

  private val prefManager: PreferenceManager
    get() = BaseApplication.getBaseInstance().prefManager

  private val projectManager: ProjectManagerImpl
    get() = ProjectManagerImpl.getInstance()

  private lateinit var aiResponseHandler: AIResponseHandler
  private var streamView: MaterialTextView? = null
  private var streamJob: Job? = null
  private lateinit var viewModel: AIAgentViewModel
  private var chatAdapter: ChatAdapter? = null
  private var streamingIndex: Int = -1

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View {
    return inflater.inflate(R.layout.fragment_ai_agent_simple, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    // Ensure keyboard doesn't cover input
    requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    // Get Activity-scoped ViewModel for persistence across fragments
    viewModel = ViewModelProvider(requireActivity())[AIAgentViewModel::class.java]

    val responseTextView = view.findViewById<MaterialTextView>(R.id.ai_agent_note)
    val messagesRecyclerView = view.findViewById<RecyclerView>(R.id.rv_messages)

    val responseContainer =
        LinearLayout(requireContext()).apply {
          orientation = LinearLayout.VERTICAL
          layoutParams =
              LinearLayout.LayoutParams(
                  LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT,
              )
          setPadding(8, 8, 8, 8)
        }
    aiResponseHandler =
        AIResponseHandler(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            responseContainer = responseContainer,
            onError = { message -> showError(message) },
            onSuccess = { message -> showSuccess(message) },
        )

    // Hide legacy notice; use chat list instead
    responseTextView?.visibility = View.GONE
    responseContainer.visibility = View.GONE

    // Setup chat list
    messagesRecyclerView?.layoutManager =
        LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
    chatAdapter = ChatAdapter { position -> viewModel.messages.value?.get(position) }
    messagesRecyclerView?.adapter = chatAdapter

    // Observe persisted messages
    viewModel.messages.observe(
        viewLifecycleOwner,
        Observer { list ->
          chatAdapter?.notifyDataSetChanged()
          messagesRecyclerView?.scrollToPosition(((list?.size) ?: 1) - 1)
        },
    )

    setupUI()
    initializeAIService()
  }

  private fun changeAIAgent(agent: String) {
    prefManager.putString("ai_agent_name", agent)

    // Reset all service flags immediately when changing agent
    isGemini = false
    isDeepseek = false
    isOpenAI = false

    log.debug("Changed AI agent to: $agent, reset all service flags")
  }

  private fun setupUI() {
    val view = requireView()
    val btnSend =
        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.btn_send
        )
    val etPrompt =
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_prompt)
    val tilPrompt =
        view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.til_prompt)
    val btnSettings =
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_settings)
    val progressIndicator =
        view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(
            R.id.progress_indicator
        )

    val noticeView =
        view.findViewById<com.google.android.material.textview.MaterialTextView>(R.id.ai_agent_note)

    // If using Kotlin
    val noticeText = getString(R.string.ai_agent_note)
    noticeView.text = Html.fromHtml(noticeText, Html.FROM_HTML_MODE_COMPACT)

    // agent options
    val agents = ai_agents

    // Set up the adapter for the dropdown
    val adapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, agents)
    val dropdown = view.findViewById<AutoCompleteTextView>(R.id.agent_dropdown)
    dropdown.setAdapter(adapter)

    // Set initial selection
    val currentAgent = getAgentModel()
    dropdown.setText(if (currentAgent.isNullOrEmpty()) agents.first() else currentAgent, false)

    // Handle selection changes
    dropdown.setOnItemClickListener { parent, view, position, id ->
      val selectedAgent = agents[position]
      log.debug("User selected agent: $selectedAgent")

      // Change the agent preference and reset flags
      changeAIAgent(selectedAgent)

      // Reinitialize the AI service with the new agent
      initializeAIService()
    }

    val btnMode = view.findViewById<MaterialButton>(R.id.btn_mode) // Add this button to your layout
    btnMode?.setOnClickListener { showModeSelector() }
    btnSend.setOnClickListener {
      manageAIContext(ContextResetType.SOFT)
      // Add user message to chat
      val userText = etPrompt.text?.toString()?.trim().orEmpty()
      if (userText.isNotEmpty()) {
        viewModel.addUserMessage(userText)
      }
      sendRequest()
    }

    // btnSettings.setOnClickListener { showSettingsDialog() }

    // Add long click for context management options
    btnSend.setOnLongClickListener {
      showContextManagementOptions()
      true
    }

    // Enable/disable send button based on text input
    etPrompt.addTextChangedListener(
        object : android.text.TextWatcher {
          override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

          override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

          override fun afterTextChanged(s: android.text.Editable?) {
            val hasText = !s.isNullOrBlank()
            btnSend.isEnabled = hasText
            btnSend.alpha = if (hasText) 1.0f else 0.5f
          }
        }
    )

    // Handle enter key to send
    etPrompt.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
        if (!etPrompt.text.isNullOrBlank()) {
          manageAIContext(ContextResetType.SOFT)
          sendRequest()
        }
        true
      } else {
        false
      }
    }
  }

  private fun setLoading(isLoading: Boolean) {
    // Check if fragment is still attached and view exists
    if (!isAdded || view == null) {
      log.warn("Fragment not attached or view is null, skipping UI update")
      return
    }

    try {
      val progressIndicator =
          requireView()
              .findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(
                  R.id.progress_indicator
              )
      val btnSend =
          requireView()
              .findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
                  R.id.btn_send
              )

      progressIndicator?.visibility = if (isLoading) View.VISIBLE else View.GONE
      btnSend?.isEnabled = !isLoading
    } catch (e: IllegalStateException) {
      log.warn("Failed to update loading state - view not available", e)
    }
  }

  private fun sendRequest() {
    val view =
        view
            ?: run {
              showError("Fragment view not available")
              return
            }

    val etPrompt =
        view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_prompt)
    val btnSend =
        view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(
            R.id.btn_send
        )
    val progressIndicator =
        view.findViewById<com.google.android.material.progressindicator.LinearProgressIndicator>(
            R.id.progress_indicator
        )

    val prompt = etPrompt.text.toString().trim()
    if (prompt.isBlank()) {
      Toast.makeText(requireContext(), "Please enter a prompt", Toast.LENGTH_SHORT).show()
      return
    }

    // Check validations
    val isEnabled = isAIAgentEnabled()
    if (!isEnabled) {
      showError("AI Agent is disabled. Please enable it in settings.")
      return
    }

    /**
     * Allow the AI agent to work even when the project isn't fully initialized This enables the AI
     * to help fix project errors during setup
     */
    // if (!projectManager.projectInitialized) {
    // showError("Project is not initialized yet. Please wait for project setup to complete.")
    // return
    // }

    // Validate that we have a properly initialized AI service
    val currentAgent = getAgentModel()
    if (currentAgent.isNullOrEmpty()) {
      showError("No AI model selected. Please select a model first.")
      return
    }

    // Check API keys based on current agent
    when {
      currentAgent.startsWith("gemini", ignoreCase = true) -> {
        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
          showError("Please configure your Gemini API key in settings.")
          return
        }
        if (!isGemini) {
          showError("Gemini service is not initialized. Please try selecting the model again.")
          return
        }
      }

      currentAgent.startsWith("deepseek", ignoreCase = true) -> {
        val deepseekApiKey = getDeepseekApiKey()
        if (deepseekApiKey.isNullOrBlank()) {
          showError("Please configure your DeepSeek API key in settings.")
          return
        }
        if (!isDeepseek) {
          showError("DeepSeek service is not initialized. Please try selecting the model again.")
          return
        }
      }

      currentAgent.startsWith("claude", ignoreCase = true) -> {
        val anthropicApiKey = getAnthropicApiKey()
        if (anthropicApiKey.isNullOrBlank()) {
          showError("Please configure your Anthropic API key in settings.")
          return
        }
        if (!isAnthropic) {
          showError("Anthropic service is not initialized. Please try selecting the model again.")
          return
        }
      }

      currentAgent.startsWith("gpt", ignoreCase = true) ||
          currentAgent.startsWith("o1", ignoreCase = true) ||
          currentAgent.startsWith("o3", ignoreCase = true) ||
          currentAgent.startsWith("o2", ignoreCase = true) -> {
        val openaiApiKey = getOpenAIApiKey()
        if (openaiApiKey.isNullOrBlank()) {
          showError("Please configure your OpenAI API key in settings.")
          return
        }
        if (!isOpenAI) {
          showError("OpenAI service is not initialized. Please try selecting the model again.")
          return
        }
      }

      else -> {
        showError("Unsupported AI model: $currentAgent")
        return
      }
    }

    btnSend?.isEnabled = false
    btnSend?.alpha = 0.5f
    etPrompt?.isEnabled = false
    progressIndicator?.visibility = android.view.View.VISIBLE
    setLoading(true)

    lifecycleScope.launch {
      try {
        log.debug("Processing request with agent: $currentAgent")

        // Get project context (cached and intelligent)
        val projectContext = getIProjectContext()

        // Get current file info (optional - works without open files)
        val currentFileContent = getCurrentEditorContent()
        val currentFile = getCurrentFile()
        val language = detectLanguage(currentFileContent)

        log.debug(
            "Current file: ${currentFile?.name ?: "none"}, Content length: ${currentFileContent?.length ?: 0}"
        )

        // Build prompt based on mode selection
        val isLegacyMode = prefManager.getBoolean("ai_legacy_mode", false)
        val AIPrompt =
            if (isLegacyMode) {
              // Use the original working prompt format
              buildLegacyPrompt(prompt, currentFileContent, currentFile)
            } else {
              // Use context-aware prompt
              buildContextAwarePrompt(prompt, projectContext, currentFileContent, currentFile)
            }

        // Select the correct context-aware AI service
        val aiService: ContextAwareAIService =
            when {
              isGemini -> {
                log.debug("Using context-aware Gemini service")
                geminiService
              }
              isDeepseek -> {
                log.debug("Using context-aware DeepSeek service")
                deepSeekService
              }
              isOpenAI -> {
                log.debug("Using context-aware OpenAI service")
                openAIService
              }
              isAnthropic -> {
                log.debug("Using context-aware Anthropic service")
                anthropicService
              }
              else -> {
                throw IllegalStateException("No AI service is initialized")
              }
            }

        // Generate code using context-aware service
        val result =
            aiService.generateCode(
                prompt = AIPrompt,
                context = currentFileContent,
                language = language,
                projectStructure = projectContext.projectStructure,
            )

        result.fold(
            onSuccess = { generatedResponse: String ->
              val mode = aiResponseHandler.getMode()
              if (mode == ResponseMode.CHAT) {
                // Live render then finalize into handler
                startLiveRender(generatedResponse)
                updateUIOnSuccess(generatedResponse, prompt, etPrompt, currentAgent)
              } else {
                // File modification mode: apply changes instead of chatting
                ThreadUtils.runOnUiThread {
                  try {
                    processMultipleFileModifications(generatedResponse)
                    etPrompt?.text?.clear()
                    setLoading(false)
                  } catch (e: Exception) {
                    log.error("Error applying file modifications", e)
                    showError("Failed to apply changes: ${e.message}")
                  }
                }
              }
            },
            onFailure = { error: Throwable ->
              // Use lifecycle-aware UI update
              updateUIOnFailure(error, currentAgent)
            },
        )
      } catch (e: Exception) {
        log.error("Error processing AI request", e)
        // Use lifecycle-aware UI update
        updateUIOnException(e, currentAgent)
      } finally {
        // Use lifecycle-aware UI update
        finalizeUI(btnSend, etPrompt, progressIndicator)
      }
    }
  }

  // Lifecycle-safe UI update methods
  private fun updateUIOnSuccess(
      generatedResponse: String,
      prompt: String,
      etPrompt: com.google.android.material.textfield.TextInputEditText?,
      currentAgent: String,
  ) {
    if (isAdded && view != null) {
      ThreadUtils.runOnUiThread {
        if (isAdded && view != null) {
          try {
            processAIResponse(generatedResponse, prompt)
            etPrompt?.text?.clear()
            showSuccess("AI task completed successfully with $currentAgent!")
            setLoading(false)
          } catch (e: Exception) {
            log.error("Error in success UI update", e)
          }
        }
      }
    }
  }

  private fun updateUIOnFailure(error: Throwable, currentAgent: String) {
    if (isAdded && view != null) {
      ThreadUtils.runOnUiThread {
        if (isAdded && view != null) {
          try {
            if (
                error.message?.contains("You exceeded your") == true ||
                    error.message?.contains("Insufficient Balance") == true
            ) {
              showErrorDialog(
                  ctx = requireContext(),
                  title = "Daily Limit Reached",
                  message =
                      """It looks like your request could not be processed.  
This may have happened for one of the following reasons:

1. Request limit reached
   - You have already used up all your available requests for the current billing cycle or quota period.  
   - Some plans and free tiers include daily or monthly request limits.

2. Attempt to access a paid model without proper access
   - The model you selected requires a paid subscription or an upgraded plan.  
   - If you do not have the required access, requests to this model will fail.

What you can do:
- Switch to a different model that is available to your account.  
- Check your usage and quota limits in your dashboard.  
- Upgrade your plan if you need access to premium models or more requests.  

Then, try again once the issue is resolved.
                            """,
                  negativeBtnTitle = "Close",
              )
            } else if (
                error.message?.contains("Your credit balance is too low to access the Anthropic") ==
                    true
            ) {
              showErrorDialog(
                  requireContext(),
                  title = "Low Credit Balance",
                  message =
                      "Your credit balance is too low to access the Anthropic API. Please go to Plans & Billing to upgrade or purchase credits.",
                  negativeBtnTitle = "Close",
              )
            } else {
              showError("Failed to process request with $currentAgent: ${error.message}")
            }
            setLoading(false)
          } catch (e: Exception) {
            log.error("Error in failure UI update", e)
          }
        }
      }
    }
  }

  private fun updateUIOnException(exception: Exception, currentAgent: String) {
    if (isAdded && view != null) {
      ThreadUtils.runOnUiThread {
        if (isAdded && view != null) {
          try {
            showError("Unexpected error with $currentAgent: ${exception.message}")
            setLoading(false)
          } catch (e: Exception) {
            log.error("Error in exception UI update", e)
          }
        }
      }
    }
  }

  private fun finalizeUI(
      btnSend: com.google.android.material.floatingactionbutton.FloatingActionButton?,
      etPrompt: com.google.android.material.textfield.TextInputEditText?,
      progressIndicator: com.google.android.material.progressindicator.LinearProgressIndicator?,
  ) {
    if (isAdded && view != null) {
      ThreadUtils.runOnUiThread {
        if (isAdded && view != null) {
          try {
            btnSend?.isEnabled = true
            btnSend?.alpha = 1.0f
            etPrompt?.isEnabled = true
            progressIndicator?.visibility = android.view.View.GONE
          } catch (e: Exception) {
            log.error("Error in finalize UI", e)
          }
        }
      }
    }
  }

  private fun startLiveRender(fullText: String) {
    // Cancel any existing stream job
    streamJob?.cancel()
    // Create new assistant streaming message in chat
    streamingIndex = viewModel.startAssistantStreaming()

    streamJob =
        viewLifecycleOwner.lifecycleScope.launch {
          val words = fullText.split(Regex("(\\n+|\\s+)"))
          var acc = StringBuilder()
          for ((i, w) in words.withIndex()) {
            if (!isAdded) break
            if (w.isEmpty()) continue
            acc.append(w)
            // Reconstruct spacing roughly
            acc.append(if (w.contains("\n")) "\n" else " ")
            if (streamingIndex >= 0) {
              viewModel.updateStreaming(streamingIndex, acc.toString())
            }
            // Small delay for streaming effect
            delay(8)
            // Occasionally yield longer delay at sentence ends
            if (w.endsWith(".") || w.endsWith("!")) delay(24)
            // Avoid UI jank on very long texts
            if (i % 200 == 0) delay(16)
          }
          // finalize streaming state
          if (streamingIndex >= 0) {
            viewModel.finalizeStreaming(streamingIndex)
          }
        }
  }

  data class ChatMessage(val text: String, val isUser: Boolean, var isStreaming: Boolean)

  inner class ChatAdapter(private val provider: (Int) -> AIAgentViewModel.ChatMessage?) :
      RecyclerView.Adapter<ChatViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
      // Root full-width container
      val root =
          LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams =
                RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            setPadding(12, 8, 12, 8)
          }

      // Bubble card
      val card =
          com.google.android.material.card.MaterialCardView(parent.context).apply {
            radius = 20f
            strokeWidth = 0
            cardElevation = 1f
            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
          }

      // Content container inside bubble
      val content =
          LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 12, 16, 12)
          }
      card.addView(content)
      root.addView(card)
      return ChatViewHolder(root, card, content)
    }

    override fun getItemCount(): Int = viewModel.messages.value?.size ?: 0

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
      val msg = provider(position) ?: return

      // Align bubble left/right using root gravity, adjust card margins safely
      (holder.root as LinearLayout).gravity =
          if (msg.isUser) android.view.Gravity.END else android.view.Gravity.START
      (holder.card.layoutParams as ViewGroup.MarginLayoutParams).apply {
        setMargins(if (msg.isUser) 48 else 8, 4, if (msg.isUser) 8 else 48, 4)
        width = ViewGroup.LayoutParams.WRAP_CONTENT
        height = ViewGroup.LayoutParams.WRAP_CONTENT
        holder.card.layoutParams = this
      }

      // Colors (Material3)
      val context = holder.card.context
      val surface =
          com.google.android.material.color.MaterialColors.getColor(
              holder.card,
              com.google.android.material.R.attr.colorSurface,
          )
      val surfaceVariant =
          com.google.android.material.color.MaterialColors.getColor(
              holder.card,
              com.google.android.material.R.attr.colorSurfaceVariant,
          )
      val onSurface =
          com.google.android.material.color.MaterialColors.getColor(
              holder.card,
              com.google.android.material.R.attr.colorOnSurface,
          )
      val primaryContainer =
          com.google.android.material.color.MaterialColors.getColor(
              holder.card,
              com.google.android.material.R.attr.colorPrimaryContainer,
          )
      val onPrimaryContainer =
          com.google.android.material.color.MaterialColors.getColor(
              holder.card,
              com.google.android.material.R.attr.colorOnPrimaryContainer,
          )

      holder.card.setCardBackgroundColor(if (msg.isUser) primaryContainer else surfaceVariant)
      holder.content.removeAllViews()

      fun addTextChunk(text: String) {
        if (text.isBlank()) return
        val tv =
            MaterialTextView(context).apply {
              this.text = text
              setTextColor(if (msg.isUser) onPrimaryContainer else onSurface)
            }
        holder.content.addView(tv)
      }

      fun addCodeBlock(code: String, language: String?) {
        val codeCard =
            com.google.android.material.card.MaterialCardView(context).apply {
              radius = 12f
              cardElevation = 0f
              setCardBackgroundColor(surface)
            }
        val wrap =
            LinearLayout(context).apply {
              orientation = LinearLayout.VERTICAL
              setPadding(12, 8, 12, 8)
            }
        // Top bar with language and copy button
        val top =
            LinearLayout(context).apply {
              orientation = LinearLayout.HORIZONTAL
              val lp =
                  LinearLayout.LayoutParams(
                      ViewGroup.LayoutParams.MATCH_PARENT,
                      ViewGroup.LayoutParams.WRAP_CONTENT,
                  )
              layoutParams = lp
            }
        val langView =
            MaterialTextView(context).apply {
              text = (language?.ifBlank { "code" } ?: "code").lowercase()
              setTextColor(onSurface)
              alpha = 0.7f
            }
        val copyBtn =
            com.google.android.material.button
                .MaterialButton(
                    context,
                    null,
                    com.google.android.material.R.attr.materialIconButtonStyle,
                )
                .apply {
                  icon =
                      androidx.core.content.ContextCompat.getDrawable(
                          context,
                          R.drawable.ic_content_copy,
                      )
                  iconPadding = 0
                  // Remove all extra inset to keep it compact
                  insetTop = 0
                  insetBottom = 0
                  minHeight = 0
                  minimumHeight = 0
                  minWidth = 0
                  minimumWidth = 0
                  setPadding(8, 8, 8, 8)
                  alpha = 0.8f
                  setOnClickListener {
                    val clipboard =
                        context.getSystemService(android.content.ClipboardManager::class.java)
                    val clip = android.content.ClipData.newPlainText("code", code)
                    clipboard?.setPrimaryClip(clip)
                    com.google.android.material.snackbar.Snackbar.make(
                            requireView(),
                            "Copied",
                            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
                        )
                        .show()
                  }
                }
        val spacer = View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) }
        top.addView(langView)
        top.addView(spacer)
        top.addView(copyBtn)
        wrap.addView(top)

        val codeTv =
            MaterialTextView(context).apply {
              text = beautifyInlineCodeIfNeeded(code, language).trimEnd()
              typeface = android.graphics.Typeface.MONOSPACE
              setTextIsSelectable(true)
              setHorizontallyScrolling(true)
              setTextColor(onSurface)
            }
        wrap.addView(codeTv)
        codeCard.addView(wrap)
        holder.content.addView(codeCard)
      }

      // Parse message content for code blocks in both multi-line and single-line forms:
      // ```lang\n...\n``` OR ```lang code``` OR ```code```
      val pattern =
          Regex("```(\\w+)?(?:\\n([\\s\\S]*?)|\\s+([^`\\n]+))```", setOf(RegexOption.MULTILINE))
      var lastIndex = 0
      val text = msg.text
      for (match in pattern.findAll(text)) {
        val range = match.range
        val before = text.substring(lastIndex, range.first)
        addTextChunk(before)
        val lang = match.groups[1]?.value
        val code = match.groups[2]?.value ?: match.groups[3]?.value ?: ""
        addCodeBlock(code, lang)
        lastIndex = range.last + 1
      }
      if (lastIndex < text.length) addTextChunk(text.substring(lastIndex))

      holder.itemView.alpha = if (msg.isStreaming) 0.95f else 1.0f
    }
  }

  inner class ChatViewHolder(
      val root: View,
      val card: com.google.android.material.card.MaterialCardView,
      val content: LinearLayout,
  ) : RecyclerView.ViewHolder(root)

  private fun beautifyInlineCodeIfNeeded(raw: String, language: String?): String {
    val code = raw.trim()
    if (code.contains('\n')) return code
    var pretty = code
    // Split chained statements and add newlines around braces
    pretty =
        pretty
            .replace(Regex("\\s*;\\s*"), ";\n")
            .replace(Regex("\\)\\s*\\{"), ") {\n")
            .replace(Regex("\\s*\\{\\s*"), " {\n")
            .replace(Regex("\\s*\\}\\s*"), "\n}\n")
            .replace(Regex("\\s*//"), "\n//")
    // Insert breaks before common declarations/keywords
    pretty =
        pretty
            .replace(Regex("\\s+(class|interface|enum)\\s+"), "\n$1 ")
            .replace(Regex("\\s+(fun)\\s+"), "\n$1 ")
            .replace(Regex("\\s+(public|private|protected|override|final|abstract)\\s+"), "\n$1 ")
            .replace(Regex("\\s+import\\s+"), "\nimport ")
            .replace(Regex("\\s+package\\s+"), "\npackage ")
    // Normalize multiple newlines
    pretty = pretty.replace(Regex("\\n{3,}"), "\n\n").trim()
    return pretty
  }

  private fun validateToken(token: String, model: String, block: (Boolean) -> Unit) {
    val isValid = !token.isNullOrBlank()
    if (!isValid) {
      showError("API Key for $model is not set")
    }
    block(isValid)
  }

  private fun initializeAIService() {
    try {
      log.debug("Initializing AI service...")

      val agent = getAgentModel()
      if (agent.isNullOrEmpty()) {
        showError("No AI model selected!")
        return
      }

      log.debug("Selected agent: $agent")

      when {
        agent.startsWith("gemini", ignoreCase = true) -> {
          log.debug("Initializing Gemini service")
          val apiKey = getApiKey()
          if (apiKey.isNullOrBlank()) {
            showError("Gemini API Key is not set")
            return
          }

          validateToken(apiKey, agent) { isValid: Boolean ->
            if (isValid) {
              geminiService.getUnderlyingService().let { service ->
                if (service is GeminiAIService) {
                  service.initialize(apiKey)
                }
              }
              isGemini = true
              isDeepseek = false
              isOpenAI = false
              isAnthropic = false
              log.info("Context-aware Gemini AI service initialized successfully")
            }
          }
        }

        agent.startsWith("deepseek", ignoreCase = true) -> {
          log.debug("Initializing DeepSeek service")
          val deepseekApiToken = getDeepseekApiKey()
          if (deepseekApiToken.isNullOrBlank()) {
            showError("DeepSeek API Key is not set")
            return
          }

          validateToken(deepseekApiToken, agent) { isValid: Boolean ->
            if (isValid) {
              deepSeekService.getUnderlyingService().let { service ->
                if (service is DeepSeekAIService) {
                  service.initialize(deepseekApiToken)
                }
              }
              isDeepseek = true
              isGemini = false
              isOpenAI = false
              isAnthropic = false
              log.info("Context-aware DeepSeek AI service initialized successfully")
            }
          }
        }

        agent.startsWith("gpt", ignoreCase = true) || agent.startsWith("o1", ignoreCase = true) -> {
          log.debug("Initializing OpenAI service for agent: $agent")
          val openAiApiToken = getOpenAIApiKey()
          if (openAiApiToken.isNullOrBlank()) {
            showError("OpenAI API Key is not set")
            return
          }

          validateToken(openAiApiToken, agent) { isValid: Boolean ->
            if (isValid) {
              openAIService.getUnderlyingService().let { service ->
                if (service is OpenAIService) {
                  service.initialize(openAiApiToken)
                }
              }
              isOpenAI = true
              isDeepseek = false
              isGemini = false
              isAnthropic = false
              log.info("Context-aware OpenAI service initialized successfully for $agent")
            }
          }
        }

        /* Anthropic Service */
        agent.startsWith("claude", ignoreCase = true) -> {
          log.debug("Initializing Anthropic service for agent: $agent")
          val anthropicApiToken = getAnthropicApiKey()
          if (anthropicApiToken.isNullOrBlank()) {
            showError("Anthropic API Key is not set")
            return
          }

          validateToken(anthropicApiToken, agent) { isValid: Boolean ->
            if (isValid) {
              anthropicService.getUnderlyingService().let { service ->
                if (service is AnthropicService) {
                  service.initialize(anthropicApiToken)
                }
              }
              isOpenAI = false
              isDeepseek = false
              isGemini = false
              isAnthropic = true
              log.info("Context-aware Anthropic service initialized successfully for $agent")
            }
          }
        }

        else -> {
          log.error("Unsupported AI model: $agent")
          showError("Unsupported AI model: $agent")
          // Reset all flags since no valid service was found
          isGemini = false
          isDeepseek = false
          isOpenAI = false
          isAnthropic = false
        }
      }

      log.debug(
          "Service flags after initialization - Gemini: $isGemini, DeepSeek: $isDeepseek, OpenAI: $isOpenAI"
      )
    } catch (e: Exception) {
      log.error("Failed to initialize AI service", e)
      showError("Failed to initialize AI service: ${e.message}")

      // Reset all flags on error
      isGemini = false
      isDeepseek = false
      isOpenAI = false
      isAnthropic = false
    }
  }

  private data class ProjectContext(
      val projectStructure: String,
      val androidModules: List<AndroidModule>,
      val resourceFiles: Map<String, File>,
      val sourceDirectories: List<File>,
      val currentModule: ModuleProject?,
      val fileMap: Map<String, File>, // Complete mapping of file names to actual file paths
  )

  /** Get project context with intelligent caching and analysis */
  private fun getIProjectContext(): ProjectContextManager.ProjectContextInfo {
    val workspace = projectManager.getWorkspace()
    val projectRoot = projectManager.projectDir
    val androidModules =
        workspace?.getSubProjects()?.filterIsInstance<AndroidModule>() ?: emptyList()

    return projectContextManager.getProjectContext(projectRoot, androidModules, false)
  }

  private fun getFullProjectContext(): ProjectContext {
    val workspace = projectManager.getWorkspace()
    val projectRoot = projectManager.projectDir

    // Get all Android modules
    val androidModules =
        workspace?.getSubProjects()?.filterIsInstance<AndroidModule>() ?: emptyList()

    // Get current module (the one containing current file)
    val currentFile = getCurrentFile()
    val currentModule = currentFile?.let { workspace?.findModuleForFile(it, false) }

    // Build complete file mapping by scanning entire project
    val fileMap = buildCompleteFileMap(projectRoot)

    // Find resource files using the file map
    // val resourceFiles = findResourceFiles(androidModules, fileMap)
    val resourceFiles = findResourceFiles(androidModules)
    // Get source directories
    val sourceDirectories = getSourceDirectories(androidModules)

    // Build detailed project structure
    val projectStructure = buildDetailedProjectStructure(projectRoot, androidModules, fileMap)

    return ProjectContext(
        projectStructure = projectStructure,
        androidModules = androidModules,
        resourceFiles = resourceFiles,
        sourceDirectories = sourceDirectories,
        currentModule = currentModule,
        fileMap = fileMap,
    )
  }

  /** Build complete mapping of all files in the project */
  private fun buildCompleteFileMap(rootDir: File): Map<String, File> {
    val fileMap = mutableMapOf<String, File>()

    fun scanDirectory(dir: File, relativePath: String = "") {
      try {
        if (!dir.exists() || !dir.isDirectory) return

        dir.listFiles()?.forEach { file ->
          val currentPath = if (relativePath.isEmpty()) file.name else "$relativePath/${file.name}"

          if (file.isFile) {
            // Map both just filename and relative path
            fileMap[file.name] = file
            fileMap[currentPath] = file

            // For source files, also map by class name
            if (file.name.endsWith(".kt") || file.name.endsWith(".java")) {
              val className = file.nameWithoutExtension
              if (!fileMap.containsKey(className)) {
                fileMap[className] = file
              }
            }
          } else if (file.isDirectory && shouldScanDirectory(file.name)) {
            scanDirectory(file, currentPath)
          }
        }
      } catch (e: Exception) {
        log.error("Error scanning directory: ${dir.absolutePath}", e)
      }
    }

    scanDirectory(rootDir)
    log.debug("Built file map with ${fileMap.size} entries")
    return fileMap
  }

  /** Dynamically resolve file path using AI-provided path and project file map */
  private fun resolveFilePath(aiProvidedPath: String, fileMap: Map<String, File>): File? {
    // Clean the AI provided path
    val cleanPath =
        aiProvidedPath
            .trim()
            .removePrefix("**")
            .removeSuffix("**") // Remove markdown
            .removePrefix("`")
            .removeSuffix("`") // Remove code blocks
            .trim()

    log.debug("Attempting to resolve file path: '$cleanPath'")

    // Strategy 1: Direct match in file map
    fileMap[cleanPath]?.let { file ->
      log.debug("Found direct match: ${file.absolutePath}")
      return file
    }

    // Strategy 2: Try just the filename
    val fileName = cleanPath.substringAfterLast('/')
    fileMap[fileName]?.let { file ->
      log.debug("Found by filename: ${file.absolutePath}")
      return file
    }

    // Strategy 3: Try class name for .kt/.java files
    if (fileName.endsWith(".kt") || fileName.endsWith(".java")) {
      val className = fileName.substringBeforeLast('.')
      fileMap[className]?.let { file ->
        log.debug("Found by class name: ${file.absolutePath}")
        return file
      }
    }

    // Strategy 4: Fuzzy matching - find files with similar names
    val possibleMatches =
        fileMap.entries.filter { (path, _) ->
          path.contains(fileName, ignoreCase = true) ||
              fileName.contains(path.substringAfterLast('/'), ignoreCase = true)
        }

    if (possibleMatches.isNotEmpty()) {
      val bestMatch = possibleMatches.first().value
      log.debug("Found fuzzy match: ${bestMatch.absolutePath}")
      return bestMatch
    }

    // Strategy 5: Try to create in appropriate location
    return createFileInAppropriateLocation(cleanPath, fileMap)
  }

  private fun findResourceFiles(androidModules: List<AndroidModule>): Map<String, File> {
    val resourceFiles = mutableMapOf<String, File>()

    androidModules.forEach { module ->
      module.mainSourceSet?.sourceProvider?.resDirectories?.forEach { resDir ->
        // Find values directories
        val valuesDir = File(resDir, "values")
        if (valuesDir.exists() && valuesDir.isDirectory) {
          valuesDir.listFiles()?.forEach { file ->
            when (file.name) {
              "strings.xml" -> resourceFiles["${module.path}/strings.xml"] = file
              "colors.xml" -> resourceFiles["${module.path}/colors.xml"] = file
              "dimens.xml" -> resourceFiles["${module.path}/dimens.xml"] = file
              "styles.xml" -> resourceFiles["${module.path}/styles.xml"] = file
              "themes.xml" -> resourceFiles["${module.path}/themes.xml"] = file
            }
          }
        }

        // Find layout directories
        val layoutDir = File(resDir, "layout")
        if (layoutDir.exists() && layoutDir.isDirectory) {
          layoutDir.listFiles()?.forEach { file ->
            if (file.name.endsWith(".xml")) {
              resourceFiles["${module.path}/layout/${file.name}"] = file
            }
          }
        }

        // Find drawable directories
        val drawableDir = File(resDir, "drawable")
        if (drawableDir.exists() && drawableDir.isDirectory) {
          drawableDir.listFiles()?.forEach { file ->
            resourceFiles["${module.path}/drawable/${file.name}"] = file
          }
        }
      }
    }

    return resourceFiles
  }

  /**
   * Create file in the most appropriate location based on file type and existing project structure
   */
  private fun createFileInAppropriateLocation(filePath: String, fileMap: Map<String, File>): File? {
    val fileName = filePath.substringAfterLast('/')
    val projectRoot = projectManager.projectDir

    try {
      when {
        // Source files (.kt, .java)
        fileName.endsWith(".kt") || fileName.endsWith(".java") -> {
          // Find existing source directories
          val existingSourceFiles =
              fileMap.values.filter { it.name.endsWith(".kt") || it.name.endsWith(".java") }

          if (existingSourceFiles.isNotEmpty()) {
            // Use the directory of an existing source file
            val targetDir = existingSourceFiles.first().parentFile
            val newFile = File(targetDir, fileName)
            log.debug("Creating source file at: ${newFile.absolutePath}")
            return newFile
          }

          // Fallback: create in app/src/main/java
          val fallbackDir = File(projectRoot, "app/src/main/java")
          fallbackDir.mkdirs()
          return File(fallbackDir, fileName)
        }

        // Layout files
        fileName.endsWith(".xml") &&
            (filePath.contains("layout") ||
                fileName.startsWith("activity_") ||
                fileName.startsWith("fragment_")) -> {
          val layoutDir = findOrCreateLayoutDir(fileMap, projectRoot)
          return File(layoutDir, fileName)
        }

        // Resource files (strings.xml, colors.xml, etc.)
        fileName.endsWith(".xml") &&
            (fileName == "strings.xml" ||
                fileName == "colors.xml" ||
                fileName == "themes.xml" ||
                fileName == "styles.xml") -> {
          val valuesDir = findOrCreateValuesDir(fileMap, projectRoot)
          return File(valuesDir, fileName)
        }

        // Gradle files
        fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts") -> {
          // Find existing gradle files to determine location
          val existingGradleFiles =
              fileMap.values.filter {
                it.name.endsWith(".gradle") || it.name.endsWith(".gradle.kts")
              }

          return if (existingGradleFiles.isNotEmpty()) {
            File(existingGradleFiles.first().parentFile, fileName)
          } else {
            File(projectRoot, fileName)
          }
        }

        // Default: create in project root
        else -> {
          return File(projectRoot, fileName)
        }
      }
    } catch (e: Exception) {
      log.error("Error creating file in appropriate location: $filePath", e)
      return null
    }
  }

  private fun findOrCreateLayoutDir(fileMap: Map<String, File>, projectRoot: File): File {
    // Find existing layout directory
    val existingLayoutFiles =
        fileMap.values.filter { it.parentFile?.name == "layout" && it.name.endsWith(".xml") }

    return if (existingLayoutFiles.isNotEmpty()) {
      existingLayoutFiles.first().parentFile
    } else {
      // Create layout directory
      val layoutDir = File(projectRoot, "app/src/main/res/layout")
      layoutDir.mkdirs()
      layoutDir
    }
  }

  private fun findOrCreateValuesDir(fileMap: Map<String, File>, projectRoot: File): File {
    // Find existing values directory
    val existingValueFiles =
        fileMap.values.filter { it.parentFile?.name == "values" && it.name.endsWith(".xml") }

    return if (existingValueFiles.isNotEmpty()) {
      existingValueFiles.first().parentFile
    } else {
      // Create values directory
      val valuesDir = File(projectRoot, "app/src/main/res/values")
      valuesDir.mkdirs()
      valuesDir
    }
  }

  /** writeToFile function using ProjectHelper */
  private fun writeToFile(aiProvidedPath: String, content: String): Boolean {
    return try {
      log.debug("Writing to file using ProjectHelper: $aiProvidedPath")

      // Validate content is not empty
      if (content.trim().isEmpty()) {
        log.error("Content is empty for file: $aiProvidedPath")
        return false
      }

      // Use ProjectHelper to write the file
      val success = ProjectHelper.writeFileToProject(aiProvidedPath, content)

      if (success) {
        log.info("Successfully wrote to file using ProjectHelper: $aiProvidedPath")

        // Notify project manager about file change if needed
        try {
          val resolvedFile = ProjectHelper.resolveProjectFilePath(aiProvidedPath)
          if (resolvedFile?.exists() == true) {
            projectManager.notifyFileCreated(resolvedFile)
          }
        } catch (e: Exception) {
          log.warn("Failed to notify project manager about file: $aiProvidedPath", e)
        }
      } else {
        log.error("ProjectHelper failed to write file: $aiProvidedPath")
        showError("Failed to write file: $aiProvidedPath")
      }

      success
    } catch (e: Exception) {
      log.error("Error writing to file using ProjectHelper: $aiProvidedPath", e)
      showError("Failed to write file: ${e.message}")
      false
    }
  }

  /** Project structure building with complete file mapping */
  private fun buildDetailedProjectStructure(
      projectRoot: File,
      androidModules: List<AndroidModule>,
      fileMap: Map<String, File>,
  ): String {
    val builder = StringBuilder()
    builder.append("=== ANDROID PROJECT STRUCTURE ===\n")
    builder.append("Root: ${projectRoot.absolutePath}\n\n")

    // Add modules information
    builder.append("=== MODULES ===\n")
    androidModules.forEach { module ->
      builder.append("Module: ${module.path}\n")
      builder.append("  Package: ${getModulePackageName(module) ?: "unknown"}\n")

      // Add source sets
      module.mainSourceSet?.let { sourceSet ->
        builder.append("  Source Directories:\n")
        sourceSet.sourceProvider.javaDirectories?.forEach { dir ->
          builder.append("    - ${dir.path}\n")
        }

        builder.append("  Resource Directories:\n")
        sourceSet.sourceProvider.resDirectories?.forEach { dir ->
          builder.append("    - ${dir.path}\n")
        }
      }

      builder.append("\n")
    }

    // Add key files with their actual locations
    builder.append("=== KEY FILES AND LOCATIONS ===\n")

    // Group files by type
    val sourceFiles = fileMap.values.filter { it.name.endsWith(".kt") || it.name.endsWith(".java") }
    val layoutFiles =
        fileMap.values.filter { it.name.endsWith(".xml") && it.parentFile?.name == "layout" }
    val valueFiles =
        fileMap.values.filter { it.name.endsWith(".xml") && it.parentFile?.name == "values" }
    val gradleFiles =
        fileMap.values.filter { it.name.endsWith(".gradle") || it.name.endsWith(".gradle.kts") }

    if (sourceFiles.isNotEmpty()) {
      builder.append("Source Files:\n")
      sourceFiles.take(10).forEach { file ->
        builder.append("  - ${file.name} → ${file.absolutePath}\n")
      }
      if (sourceFiles.size > 10) {
        builder.append("  ... and ${sourceFiles.size - 10} more\n")
      }
      builder.append("\n")
    }

    if (layoutFiles.isNotEmpty()) {
      builder.append("Layout Files:\n")
      layoutFiles.forEach { file -> builder.append("  - ${file.name} → ${file.absolutePath}\n") }
      builder.append("\n")
    }

    if (valueFiles.isNotEmpty()) {
      builder.append("Resource Files:\n")
      valueFiles.forEach { file -> builder.append("  - ${file.name} → ${file.absolutePath}\n") }
      builder.append("\n")
    }

    if (gradleFiles.isNotEmpty()) {
      builder.append("Build Files:\n")
      gradleFiles.forEach { file -> builder.append("  - ${file.name} → ${file.absolutePath}\n") }
      builder.append("\n")
    }

    // Add detailed file structure
    builder.append("=== PROJECT TREE ===\n")
    builder.append(buildProjectStructure(projectRoot))

    return builder.toString()
  }

  private fun shouldScanDirectory(dirName: String): Boolean {
    val skipDirs =
        setOf(
            ".git",
            ".gradle",
            ".idea",
            "build",
            "builds",
            ".externalNativeBuild",
            "node_modules",
            ".dart_tool",
        )
    return !skipDirs.contains(dirName) && !dirName.startsWith(".")
  }

  private fun getSourceDirectories(androidModules: List<AndroidModule>): List<File> {
    val sourceDirectories = mutableListOf<File>()

    androidModules.forEach { module ->
      module.mainSourceSet?.sourceProvider?.javaDirectories?.forEach { javaDir ->
        sourceDirectories.add(javaDir)
      }
    }

    return sourceDirectories
  }

  private fun buildDetailedProjectStructure(
      projectRoot: File,
      androidModules: List<AndroidModule>,
  ): String {
    val builder = StringBuilder()
    builder.append("=== ANDROID PROJECT STRUCTURE ===\n")
    builder.append("Root: ${projectRoot.absolutePath}\n\n")

    // Add modules information
    builder.append("=== MODULES ===\n")
    androidModules.forEach { module ->
      builder.append("Module: ${module.path}\n")
      builder.append("  Package: ${getModulePackageName(module) ?: "unknown"}\n")

      // Add source sets
      module.mainSourceSet?.let { sourceSet ->
        builder.append("  Source Directories:\n")
        sourceSet.sourceProvider.javaDirectories?.forEach { dir ->
          builder.append("    - ${dir.path}\n")
        }

        builder.append("  Resource Directories:\n")
        sourceSet.sourceProvider.resDirectories?.forEach { dir ->
          builder.append("    - ${dir.path}\n")
        }
      }

      builder.append("\n")
    }

    // Add detailed file structure
    builder.append("=== FILE STRUCTURE ===\n")
    builder.append(buildProjectStructure(projectRoot))

    return builder.toString()
  }

  private fun getModulePackageName(module: AndroidModule): String? {
    return try {
      // Try to get the package name from the module's main source set
      module.mainSourceSet?.sourceProvider?.manifestFile?.let { manifestFile ->
        if (manifestFile.exists()) {
          val manifestContent = manifestFile.readText()
          val packageRegex = Regex("package\\s*=\\s*[\"']([^\"']+)[\"']")
          packageRegex.find(manifestContent)?.groupValues?.get(1)
        } else null
      }
    } catch (e: Exception) {
      log.error("Error getting package name for module ${module.path}", e)
      null
    }
  }

  /** Build a simple legacy prompt */
  private fun buildLegacyPrompt(
      userPrompt: String,
      currentFileContent: String?,
      currentFile: File?,
  ): String {
    return buildString {
      append("=== PROJECT TREE ===\n")
      append(getFullProjectTree())
      append("\n\n")
      append(userPrompt)
      if (!currentFileContent.isNullOrBlank() && currentFile != null) {
        append("\n\nCurrent file content:\n")
        append(currentFileContent)
      }
    }
  }

  /**
   * Build a context-aware prompt that leverages cached project information Works with or without an
   * open file
   */
  private fun buildContextAwarePrompt(
      userPrompt: String,
      projectContext: ProjectContextManager.ProjectContextInfo,
      currentFileContent: String?,
      currentFile: File?,
  ): String {
    return buildString {
      append("=== PROJECT TREE ===\n")
      append(getFullProjectTree())
      append("\n\n")
      append("=== REQUEST ===\n")
      append(userPrompt)
      append("\n\n")
      if (!currentFileContent.isNullOrBlank() && currentFile != null) {
        val currentFilePath =
            currentFile.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
        append("=== CURRENT FILE (").append(currentFilePath).append(") ===\n")
        append(currentFileContent)
        append("\n\n")
      }
      append("=== PROJECT CONTEXT ===\n")
      append("Project Type: ").append(projectContext.projectType).append("\n")
      when (projectContext.projectType) {
        ProjectContextManager.ProjectType.COMPOSE_ONLY -> {
          append("UI FRAMEWORK: JETPACK COMPOSE ONLY\n")
          append(
              "⚠️ CRITICAL: This is a Compose-only project. DO NOT use XML layouts, findViewById, or View-based code.\n"
          )
          append("✅ Use ONLY: @Composable functions, Modifier, LazyColumn, etc.\n")
        }
        ProjectContextManager.ProjectType.VIEW_BASED_ONLY -> {
          append("UI FRAMEWORK: TRADITIONAL VIEWS ONLY\n")
          append(
              "⚠️ CRITICAL: This is a traditional Android project. DO NOT use @Composable, setContent, or Compose UI.\n"
          )
          append("✅ Use ONLY: XML layouts, findViewById, View-based code.\n")
        }
        ProjectContextManager.ProjectType.HYBRID_COMPOSE_VIEW -> {
          append("UI FRAMEWORK: HYBRID (Compose + Views)\n")
          append(
              "⚠️ CRITICAL: Match the existing pattern. Don't mix frameworks in the same component.\n"
          )
        }
        else -> {
          append(
              "UI FRAMEWORK: ${if (projectContext.usesCompose) "Jetpack Compose" else "Traditional Views"}\n"
          )
        }
      }

      if (projectContext.packageName != null) {
        append("Package: ${projectContext.packageName}\n")
      }
      append("Architecture: ${projectContext.architecturePattern}\n")
      if (
          projectContext.dependencyInjection !=
              ProjectContextManager.DependencyInjectionFramework.NONE
      ) {
        append("DI Framework: ${projectContext.dependencyInjection}\n")
      }

      append(
          "\nIMPORTANT: Maintain consistency with the project's UI framework. Do not introduce incompatible UI components."
      )
      append("\nIf creating new files, use appropriate locations based on the project structure.")
    }
  }

  private fun buildProjectAwarePrompt(
      userPrompt: String,
      projectContext: ProjectContext,
      currentFileContent: String?,
      currentFile: File?,
  ): String {

    // Check if this is an error fixing request
    val isErrorFix =
        userPrompt.lowercase().contains("error") ||
            userPrompt.lowercase().contains("fix") ||
            userPrompt.lowercase().contains("compile")

    return if (isErrorFix) {
      buildString {
        append("=== COMPILATION ERROR FIXING TASK ===\n")
        append("You are fixing compilation errors in an Android project.\n")
        append("ANALYZE the errors and provide CORRECTED code.\n\n")

        append("=== COMPILATION ERRORS ===\n")
        append(userPrompt)
        append("\n\n")

        append("=== PROJECT FILE LOCATIONS ===\n")
        append("Here are the EXACT file locations in this project:\n")

        // Show key file mappings so AI knows exactly where files are
        val sourceFiles =
            projectContext.fileMap.values
                .filter { it.name.endsWith(".kt") || it.name.endsWith(".java") }
                .sortedBy { it.name }

        if (sourceFiles.isNotEmpty()) {
          append("Source Files:\n")
          sourceFiles.forEach { file ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        val layoutFiles =
            projectContext.fileMap.values.filter {
              it.name.endsWith(".xml") && it.parentFile?.name == "layout"
            }
        if (layoutFiles.isNotEmpty()) {
          append("Layout Files:\n")
          layoutFiles.forEach { file ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        val resourceFiles =
            projectContext.fileMap.values.filter {
              it.name.endsWith(".xml") && it.parentFile?.name == "values"
            }
        if (resourceFiles.isNotEmpty()) {
          append("Resource Files:\n")
          resourceFiles.forEach { file ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        if (!currentFileContent.isNullOrBlank()) {
          val currentFilePath =
              currentFile?.absolutePath?.removePrefix(projectManager.projectDir.absolutePath + "/")
          append("=== CURRENT FILE ($currentFilePath) ===\n")
          append(currentFileContent)
          append("\n\n")
        }

        append("=== REQUIRED OUTPUT FORMAT ===\n")
        append("When providing fixed files, use the EXACT paths shown above.\n")
        append("Format your response exactly like this:\n\n")
        append("==== FILE: [use exact path from above] ====\n")
        append("[CORRECTED file content - fix the actual compilation errors]\n")
        append("==== END_FILE ====\n\n")

        append("CRITICAL REQUIREMENTS:\n")
        append("1. Use the EXACT file paths shown in 'PROJECT FILE LOCATIONS' section above\n")
        append("2. Fix compilation errors by removing duplicates and conflicts\n")
        append("3. Keep only ONE working version of each class/function\n")
        append("4. Ensure package declarations match the file locations\n")
        append("5. DO NOT add explanatory text after END_FILE markers\n")
        append("6. The response should end immediately after the last '==== END_FILE ===='\n")
      }
    } else {
      // Regular non-error requests
      buildString {
        append("=== ANDROID DEVELOPMENT TASK ===\n")
        append("You are working on an Android project. Here's your task:\n\n")

        append("=== REQUEST ===\n")
        append(userPrompt)
        append("\n\n")

        append("=== PROJECT FILE LOCATIONS ===\n")
        append("Here are the EXACT file locations in this project:\n")

        // Show comprehensive file mapping
        val allFiles = projectContext.fileMap.entries.sortedBy { it.key }

        // Group files by type for better organization
        val sourceFiles =
            allFiles.filter { (_, file) ->
              file.name.endsWith(".kt") || file.name.endsWith(".java")
            }
        val layoutFiles =
            allFiles.filter { (_, file) ->
              file.name.endsWith(".xml") && file.parentFile?.name == "layout"
            }
        val resourceFiles =
            allFiles.filter { (_, file) ->
              file.name.endsWith(".xml") && file.parentFile?.name == "values"
            }
        val gradleFiles =
            allFiles.filter { (_, file) ->
              file.name.endsWith(".gradle") || file.name.endsWith(".gradle.kts")
            }
        val manifestFiles = allFiles.filter { (_, file) -> file.name == "AndroidManifest.xml" }

        if (sourceFiles.isNotEmpty()) {
          append("Source Files (.kt/.java):\n")
          sourceFiles.take(15).forEach { (_, file) ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          if (sourceFiles.size > 15) {
            append("  ... and ${sourceFiles.size - 15} more source files\n")
          }
          append("\n")
        }

        if (layoutFiles.isNotEmpty()) {
          append("Layout Files:\n")
          layoutFiles.forEach { (_, file) ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        if (resourceFiles.isNotEmpty()) {
          append("Resource Files (strings, colors, etc.):\n")
          resourceFiles.forEach { (_, file) ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        if (manifestFiles.isNotEmpty()) {
          append("Manifest Files:\n")
          manifestFiles.forEach { (_, file) ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        if (gradleFiles.isNotEmpty()) {
          append("Build Files:\n")
          gradleFiles.forEach { (_, file) ->
            val relativePath =
                file.absolutePath.removePrefix(projectManager.projectDir.absolutePath + "/")
            append("  - ${file.name} → $relativePath\n")
          }
          append("\n")
        }

        if (!currentFileContent.isNullOrBlank()) {
          val currentFilePath =
              currentFile?.absolutePath?.removePrefix(projectManager.projectDir.absolutePath + "/")
          append("=== CURRENT FILE ($currentFilePath) ===\n")
          append(currentFileContent)
          append("\n\n")
        }

        append("=== REQUIRED OUTPUT FORMAT ===\n")
        append("When creating/modifying files, use the EXACT paths shown above.\n")
        append("If creating new files, choose appropriate locations based on existing structure.\n")
        append("Format your response exactly like this:\n\n")
        append("==== FILE: [use exact path from above or appropriate new location] ====\n")
        append("[complete file content]\n")
        append("==== END_FILE ====\n\n")

        append("CRITICAL REQUIREMENTS:\n")
        append("1. Use EXACT file paths from 'PROJECT FILE LOCATIONS' section\n")
        append(
            "2. For new files, place them in appropriate locations following Android conventions\n"
        )
        append("3. Ensure package declarations match directory structure\n")
        append("4. Follow Android best practices and coding standards\n")
        append("5. DO NOT add explanatory text after END_FILE markers\n")
        append("6. The response should end immediately after the last '==== END_FILE ===='\n")
      }
    }
  }

  /** Intelligent context management instead of hard reset */
  private fun manageAIContext(resetType: ContextResetType = ContextResetType.SOFT) {
    try {
      when (resetType) {
        ContextResetType.SOFT -> {
          log.info("Performing soft context reset (clears conversation, keeps project context)")
          when {
            isGemini -> {
              geminiService.softReset()
              geminiService.smartReset()
            }
            isDeepseek -> {
              deepSeekService.softReset()
              deepSeekService.smartReset()
            }
            isOpenAI -> {
              openAIService.softReset()
              openAIService.smartReset()
            }
            isAnthropic -> {
              anthropicService.softReset()
              anthropicService.smartReset()
            }
          }
        }

        ContextResetType.HARD -> {
          log.info("Performing hard context reset (clears everything)")
          when {
            isGemini -> geminiService.hardReset()
            isDeepseek -> deepSeekService.hardReset()
            isOpenAI -> openAIService.hardReset()
            isAnthropic -> anthropicService.hardReset()
          }
          // Clear project context cache as well
          projectContextManager.clearCache()
        }

        ContextResetType.PROJECT_REFRESH -> {
          log.info("Refreshing project context")
          projectContextManager.clearCache()
          // Force refresh project context on next request
          val workspace = projectManager.getWorkspace()
          val projectRoot = projectManager.projectDir
          val androidModules =
              workspace?.getSubProjects()?.filterIsInstance<AndroidModule>() ?: emptyList()
          projectContextManager.getProjectContext(projectRoot, androidModules, true)
        }
      }
    } catch (e: Exception) {
      log.error("Failed to manage AI context", e)
    }
  }

  enum class ContextResetType {
    SOFT, // Clear conversation history but keep project context
    HARD, // Clear everything (equivalent to old resetAIContext)
    PROJECT_REFRESH, // Refresh project analysis only
  }

  private fun processAIResponse(response: String, originalPrompt: String) {
    aiResponseHandler.processResponse(
        response = response,
        originalPrompt = originalPrompt,
        currentFileInserter = { code -> insertCodeIntoEditor(code) },
    )
  }

  private fun showModeSelector() {
    val modes = arrayOf("💬 Chat Mode (Display in UI)", "📝 File Modification Mode")

    val currentMode = aiResponseHandler.getMode()
    val currentIndex = if (currentMode == ResponseMode.CHAT) 0 else 1

    MaterialAlertDialogBuilder(requireContext())
        .setTitle("AI Response Mode")
        .setSingleChoiceItems(modes, currentIndex) { dialog, which ->
          val newMode =
              if (which == 0) {
                ResponseMode.CHAT
              } else {
                ResponseMode.FILE_MODIFICATION
              }
          aiResponseHandler.setMode(newMode)
          aiResponseHandler.clearResponse()
          dialog.dismiss()
          showSuccess("Mode changed to: ${modes[which]}")
        }
        .setNegativeButton("Cancel", null)
        .show()
  }

  private fun parseFileModifications(response: String): Map<String, String> {
    val modifications = mutableMapOf<String, String>()

    // First, clean the response by removing anything after END_FILE markers
    val cleanedResponse = cleanResponseFromExplanations(response)
    val lines = cleanedResponse.lines()

    var currentFile: String? = null
    val currentContent = StringBuilder()
    var inFileContent = false

    log.debug("Starting to parse response with ${lines.size} lines")

    for (i in lines.indices) {
      val line = lines[i]
      val trimmedLine = line.trim()

      when {
        // Match various file header formats the AI might use
        trimmedLine.matches(Regex("^====\\s*FILE:\\s*(.+?)\\s*====\$")) -> {
          // Save previous file if exists
          currentFile?.let { file ->
            val content = currentContent.toString().trimEnd()
            modifications[file] = content
            log.debug("Saved file: $file, content length: ${content.length}")
          }

          // Extract file path
          currentFile =
              trimmedLine
                  .replace(Regex("^====\\s*FILE:\\s*"), "")
                  .replace(Regex("\\s*====\$"), "")
                  .trim()
          currentContent.clear()
          inFileContent = true
          log.debug("Found file modification for: $currentFile")
        }

        trimmedLine.matches(Regex("^====\\s*END_FILE\\s*====\$")) -> {
          inFileContent = false
          log.debug("End of file content for: $currentFile")
          // Don't process any more lines for this file
        }

        // Also handle simpler formats the AI might use
        trimmedLine.startsWith("**") &&
            (trimmedLine.contains(".xml") ||
                trimmedLine.contains(".kt") ||
                trimmedLine.contains(".java")) -> {
          // Save previous file if exists
          currentFile?.let { file ->
            val content = currentContent.toString().trimEnd()
            modifications[file] = content
            log.debug("Saved file: $file, content length: ${content.length}")
          }

          // Extract file path from markdown-style header
          currentFile = trimmedLine.replace("**", "").replace(":", "").trim()
          currentContent.clear()
          inFileContent = true
          log.debug("Found markdown-style file: $currentFile")
        }

        inFileContent && currentFile != null -> {
          // Skip markdown code block markers and other formatting
          if (
              !trimmedLine.startsWith("```") &&
                  !trimmedLine.equals("\\```") &&
                  !trimmedLine.startsWith("\\```") &&
                  !trimmedLine.startsWith("---") &&
                  !trimmedLine.matches(Regex("^\\*\\*.*\\*\\*\$"))
          ) {
            currentContent.append(line).append("\n")
          }
        }
      }
    }

    // Save last file
    currentFile?.let { file ->
      val content = currentContent.toString().trimEnd()
      modifications[file] = content
      log.debug("Saved final file: $file, content length: ${content.length}")
    }

    log.debug("Parsed ${modifications.size} total file modifications")
    return modifications
  }

  private fun cleanResponseFromExplanations(response: String): String {
    // Split response into sections and only keep content before any explanatory text
    val lines = response.lines()
    val cleanedLines = mutableListOf<String>()
    var insideFileContent = false
    var skipRestOfResponse = false

    for (line in lines) {
      if (skipRestOfResponse) break

      val trimmedLine = line.trim()

      when {
        // Start of file content
        trimmedLine.matches(Regex("^====\\s*FILE:.*====\$")) -> {
          insideFileContent = true
          cleanedLines.add(line)
        }

        // End of file content - after this, ignore everything else
        trimmedLine.matches(Regex("^====\\s*END_FILE\\s*====\$")) -> {
          cleanedLines.add(line)
          insideFileContent = false
          // Check if there are more files coming
          val remainingLines = lines.drop(lines.indexOf(line) + 1)
          val hasMoreFiles =
              remainingLines.any { it.trim().matches(Regex("^====\\s*FILE:.*====\$")) }
          if (!hasMoreFiles) {
            // No more files, stop processing
            skipRestOfResponse = true
          }
        }

        // Inside file content or before first file
        else -> {
          // Only add the line if we haven't hit explanatory content
          if (!isExplanatoryContent(trimmedLine)) {
            cleanedLines.add(line)
          }
        }
      }
    }

    return cleanedLines.joinToString("\n")
  }

  private fun isExplanatoryContent(line: String): Boolean {
    val explanatoryPhrases =
        listOf(
            "the original code had",
            "the corrected code",
            "this fixes",
            "the issue was",
            "i removed",
            "i fixed",
            "the problem",
            "duplicate",
            "causing the compilation errors",
            "while compiling successfully",
            "maintains the functionality",
        )

    val lowerLine = line.lowercase()
    return explanatoryPhrases.any { phrase -> lowerLine.contains(phrase) }
  }

  private fun processMultipleFileModifications(response: String) {
    val fileModifications = parseFileModifications(response)

    if (fileModifications.isEmpty()) {
      log.warn("No file modifications found in response, falling back to single file")
      val cleanedResponse = cleanMarkdownBlocks(response)
      insertCodeIntoEditor(cleanedResponse)
      return
    }

    var modificationCount = 0
    var failureCount = 0

    fileModifications.forEach { (filePath, content) ->
      try {
        // Clean the content before writing
        val cleanedContent = cleanMarkdownBlocks(content)
        log.debug("Writing to file: $filePath")
        log.debug("Original content length: ${content.length}")
        log.debug("Cleaned content length: ${cleanedContent.length}")
        log.debug("Content preview: ${cleanedContent.take(200)}...")

        if (writeToFile(filePath, cleanedContent)) {
          modificationCount++
          log.info("Successfully wrote to file: $filePath")
        } else {
          failureCount++
          log.error("Failed to write to file: $filePath")
        }
      } catch (e: Exception) {
        failureCount++
        log.error("Failed to write to file: $filePath", e)
        showError("Failed to write to file $filePath: ${e.message}")
      }
    }

    if (modificationCount > 0) {
      showSuccess(
          "Successfully modified $modificationCount file(s)" +
              if (failureCount > 0) ", $failureCount failed" else ""
      )
      // Notify project manager about file changes
      refreshCurrentEditor()

      // notifyProjectUpdate()
    } else {
      showError("Failed to modify any files. Check logs for details.")
    }
  }

  private fun refreshCurrentEditor() {
    try {
      val activity = requireActivity()
      if (activity is com.tom.rv2ide.activities.editor.EditorHandlerActivity) {
        val currentEditor = activity.getCurrentEditor()
        if (currentEditor?.file != null) {
          val file = currentEditor.file
          val newContent = file?.readText()
          val editorText = currentEditor.editor?.text

          if (editorText != null && newContent != null) {
            editorText.replace(0, editorText.length, newContent)
          }
        }
      }
    } catch (e: Exception) {
      log.error("Failed to refresh current editor", e)
    }
  }

  private fun cleanMarkdownBlocks(content: String): String {
    // Aggressively remove all markdown and chat formatting
    var cleaned = content

    // Remove markdown code blocks
    cleaned = cleaned.replace(Regex("\\\\```\\w*\\n?"), "") // Remove escaped opening code blocks
    cleaned = cleaned.replace("\\\\```", "") // Remove escaped closing code blocks
    cleaned = cleaned.replace(Regex("```[\\w-]*\\n?"), "") // Remove opening code blocks
    cleaned = cleaned.replace("```", "") // Remove closing code blocks

    // Remove markdown formatting
    cleaned =
        cleaned.replace(
            Regex("^\\*\\*.*\\*\\*\$", RegexOption.MULTILINE),
            "",
        ) // Remove bold headers
    cleaned =
        cleaned.replace(Regex("^#+ .*\$", RegexOption.MULTILINE), "") // Remove markdown headers
    cleaned = cleaned.replace(Regex("^---+\$", RegexOption.MULTILINE), "") // Remove separator lines
    cleaned =
        cleaned.replace(Regex("^=+ .*\$", RegexOption.MULTILINE), "") // Remove section headers

    // Remove chat-like responses
    cleaned =
        cleaned.replace(Regex("^Here's.*:?\$", RegexOption.MULTILINE), "") // Remove "Here's..."
    cleaned = cleaned.replace(Regex("^I've.*:?\$", RegexOption.MULTILINE), "") // Remove "I've..."
    cleaned = cleaned.replace(Regex("^This.*:?\$", RegexOption.MULTILINE), "") // Remove "This..."
    cleaned =
        cleaned.replace(Regex("^Let me.*:?\$", RegexOption.MULTILINE), "") // Remove "Let me..."

    return cleaned
        .lines()
        .dropWhile { it.trim().isEmpty() } // Remove leading empty lines
        .dropLastWhile { it.trim().isEmpty() } // Remove trailing empty lines
        .joinToString("\n")
        .trim()
  }

  private fun notifyProjectUpdate() {
    try {
      projectManager.notifyProjectUpdate()
    } catch (e: Exception) {
      log.error("Error notifying project update", e)
    }
  }

  private fun getCurrentFile(): File? {
    return try {
      val activity = requireActivity()
      if (activity is com.tom.rv2ide.activities.editor.EditorHandlerActivity) {
        activity.getCurrentEditor()?.file
      } else null
    } catch (e: Exception) {
      log.error("Failed to get current file", e)
      null
    }
  }

  private fun getCurrentEditorContent(): String? {
    return try {
      val activity = requireActivity()
      if (activity is com.tom.rv2ide.activities.editor.EditorHandlerActivity) {
        val currentEditor = activity.getCurrentEditor()
        currentEditor?.editor?.text?.toString()
      } else {
        null
      }
    } catch (e: Exception) {
      log.error("Failed to get current editor content", e)
      null
    }
  }

  private fun detectLanguage(content: String?): String {
    // Prefer current editor file extension when available; fallback to kotlin
    val current = getCurrentFile()
    val ext = current?.extension?.lowercase()
    return when (ext) {
      "kt",
      "kts" -> "kotlin"
      "java" -> "java"
      "xml" -> "xml"
      else -> "kotlin"
    }
  }

  private fun buildProjectStructure(
      rootDir: File,
      maxDepth: Int = 3,
      currentDepth: Int = 0,
  ): String {
    if (currentDepth >= maxDepth) return ""

    val builder = StringBuilder()
    val files = rootDir.listFiles()?.sortedBy { it.name } ?: return ""

    files.forEachIndexed { index, file ->
      val isLast = index == files.size - 1
      val prefix =
          if (currentDepth == 0) ""
          else "│   ".repeat(currentDepth - 1) + if (isLast) "└── " else "├── "

      builder.append(prefix).append(file.name)

      builder.append("\n")

      // Recursively scan important directories
      if (file.isDirectory && shouldScanDirectory(file.name, currentDepth)) {
        val subStructure = buildProjectStructure(file, maxDepth, currentDepth + 1)
        if (subStructure.isNotEmpty()) {
          builder.append(subStructure)
        }
      }
    }

    return builder.toString()
  }

  private fun shouldScanDirectory(dirName: String, currentDepth: Int): Boolean {
    return when {
      currentDepth == 0 -> true // Always scan root level
      dirName in
          listOf(
              "src",
              "main",
              "java",
              "kotlin",
              "res",
              "drawable",
              "layout",
              "values",
              "mipmap",
          ) -> true
      dirName.startsWith("core") || dirName.startsWith("app") -> true
      currentDepth < 2 -> true // Scan first two levels
      else -> false
    }
  }

  private fun insertCodeIntoEditor(code: String) {
    try {
      val activity = requireActivity()
      if (activity is com.tom.rv2ide.activities.editor.EditorHandlerActivity) {
        val currentEditor = activity.getCurrentEditor()
        if (currentEditor != null) {
          val editor = currentEditor.editor
          if (editor != null) {
            val text = editor.text
            if (text != null) {
              log.debug("Replacing entire file content")
              log.debug("New code: ${code.take(200)}...")

              // Clean up the code - remove markdown formatting if present
              val cleanCode = cleanMarkdownBlocks(code)

              // Replace the entire file content
              text.replace(0, text.length, cleanCode)

              // Move cursor to end of file
              val lines = cleanCode.split("\n")
              editor.setSelection(lines.size - 1, lines.lastOrNull()?.length ?: 0)

              log.info("Successfully replaced file content")
            } else {
              showError("Editor text is null")
            }
          } else {
            showError("Editor is null")
          }
        } else {
          showError("No editor is currently open")
        }
      } else {
        showError("Cannot access editor from this activity")
      }
    } catch (e: Exception) {
      log.error("Failed to insert code into editor", e)
      showError("Failed to insert code: ${e.message}")
    }
  }

  private fun showApiKeyRequiredDialog() {
    // TODO: Show dialog to enter API key
    Toast.makeText(
            requireContext(),
            "Please configure your Gemini API key in settings",
            Toast.LENGTH_LONG,
        )
        .show()
  }

  private fun showSettingsDialog() {
    // TODO: Show settings dialog for API key configuration
    Toast.makeText(requireContext(), "Settings dialog not implemented yet", Toast.LENGTH_SHORT)
        .show()
  }

  private fun showContextManagementOptions() {
    val currentMode = aiResponseHandler.getMode()
    val modeText = if (currentMode == ResponseMode.CHAT) "💬 Chat" else "📝 Files"

    val options =
        arrayOf(
            "🔄 Switch Mode (Current: $modeText)",
            "🧠 Soft Reset (Keep project context)",
            "🔄 Hard Reset (Clear everything)",
            "📊 Refresh Project Analysis",
            "ℹ️ Show Context Status",
            "⚙️ Toggle Legacy Mode",
        )

    MaterialAlertDialogBuilder(requireContext())
        .setTitle("AI Context Management")
        .setItems(options) { dialog, which ->
          dialog.dismiss()
          when (which) {
            0 -> showModeSelector()
            1 -> {
              manageAIContext(ContextResetType.SOFT)
              showMaterialSuccess("Conversation context cleared")
            }
            2 -> showConfirmHardReset()
            3 -> {
              manageAIContext(ContextResetType.PROJECT_REFRESH)
              showMaterialSuccess("Project analysis refreshed")
            }
            4 -> showContextStatus()
            5 -> toggleLegacyMode()
          }
        }
        .setNegativeButton("Cancel", null)
        .show()
  }

  private fun toggleLegacyMode() {
    val isLegacyMode = prefManager.getBoolean("ai_legacy_mode", false)
    prefManager.putBoolean("ai_legacy_mode", !isLegacyMode)

    if (!isLegacyMode) {
      showMaterialSuccess("Legacy mode enabled - AI will use original prompt format")
    } else {
      showMaterialSuccess("Context-aware mode enabled - AI will use enhanced context")
    }
  }

  private fun showConfirmHardReset() {
    com.google.android.material.dialog
        .MaterialAlertDialogBuilder(requireContext())
        .setTitle("⚠️ Confirm Hard Reset")
        .setMessage("This will clear all conversation history and project context. Are you sure?")
        .setPositiveButton("Reset") { _, _ ->
          manageAIContext(ContextResetType.HARD)
          showMaterialSuccess("All context cleared")
        }
        .setNegativeButton("Cancel", null)
        .show()
  }

  private fun showContextStatus() {
    val activeService =
        when {
          isGemini -> geminiService
          isDeepseek -> deepSeekService
          isOpenAI -> openAIService
          isAnthropic -> anthropicService
          else -> null
        }

    val status = buildString {
      append("=== AI Context Status ===\n\n")

      if (activeService != null) {
        append("Active Service: ")
        append(
            when {
              isGemini -> "Gemini"
              isDeepseek -> "DeepSeek"
              isOpenAI -> "OpenAI"
              isAnthropic -> "Anthropic"
              else -> "Unknown"
            }
        )
        append("\n\n")

        append(activeService.getConversationSummary())
        append("\n")

        val projectSummary = projectContextManager.getContextSummary()
        if (projectSummary != null) {
          append("\n")
          append(projectSummary)
        } else {
          append("\nProject Context: Not analyzed yet")
        }
      } else {
        append("No active AI service")
      }
    }

    com.google.android.material.dialog
        .MaterialAlertDialogBuilder(requireContext())
        .setTitle("🤖 AI Context Status")
        .setMessage(status)
        .setPositiveButton("OK", null)
        .setNeutralButton("Copy to Clipboard") { _, _ ->
          copyToClipboard("AI Context Status", status)
        }
        .show()
  }

  private fun showError(message: String) {
    showMaterialError(message)
  }

  private fun showSuccess(message: String) {
    showMaterialSuccess(message)
  }

  private fun showMaterialSuccess(message: String) {
    com.google.android.material.snackbar.Snackbar.make(
            requireView(),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT,
        )
        .apply { show() }
  }

  private fun showMaterialError(message: String) {
    com.google.android.material.snackbar.Snackbar.make(
            requireView(),
            message,
            com.google.android.material.snackbar.Snackbar.LENGTH_LONG,
        )
        .apply {
          setAction("Dismiss") { dismiss() }
          show()
        }
  }

  private fun copyToClipboard(label: String, text: String) {
    val clipboard =
        requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    showMaterialSuccess("Copied to clipboard")
  }

  private fun isAIAgentEnabled(): Boolean {
    return prefManager.getBoolean("ai_agent_enabled", false)
  }

  private fun getApiKey(): String? {
    val apiKey = prefManager.getString("ai_agent_gemini_api_key", null)
    log.debug(
        "Retrieved API key: ${if (apiKey.isNullOrBlank()) "null/empty" else "present (${apiKey.length} chars)"}"
    )
    return apiKey
  }

  private fun getDeepseekApiKey(): String? {
    val apiKey = prefManager.getString("ai_agent_deepseek_api_key", null)
    log.debug(
        "Retrieved API key: ${if (apiKey.isNullOrBlank()) "null/empty" else "present (${apiKey.length} chars)"}"
    )
    return apiKey
  }

  private fun getOpenAIApiKey(): String? {
    val apiKey = prefManager.getString("ai_agent_openai_api_key", null)
    log.debug(
        "Retrieved API key: ${if (apiKey.isNullOrBlank()) "null/empty" else "present (${apiKey.length} chars)"}"
    )
    return apiKey
  }

  private fun getAnthropicApiKey(): String? {
    val apiKey = prefManager.getString("ai_agent_anthropic_api_key", null)
    log.debug(
        "Retrieved API key: ${if (apiKey.isNullOrBlank()) "null/empty" else "present (${apiKey.length} chars)"}"
    )
    return apiKey
  }

  private fun getAgentModel(): String? {
    val agentName = prefManager.getString("ai_agent_name", null)
    return agentName
  }

  private fun getFullProjectTree(): String {
    val projectRoot = projectManager.projectDir
    return buildProjectStructure(projectRoot, maxDepth = 10)
  }

  override fun onDestroyView() {
    super.onDestroyView()
  }
}
