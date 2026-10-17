package com.tom.rv2ide.artificial.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.tom.rv2ide.R
import com.tom.rv2ide.app.BaseApplication

class CustomEndpointConfigDialog(
    private val onSave: (baseUrl: String, apiKey: String, modelName: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var baseUrlInput: TextInputEditText
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var modelNameInput: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_custom_endpoint_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        baseUrlInput = view.findViewById(R.id.baseUrlInput)
        apiKeyInput = view.findViewById(R.id.customApiKeyInput)
        modelNameInput = view.findViewById(R.id.customModelNameInput)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        loadSavedConfig()

        saveButton.setOnClickListener {
            val baseUrl = baseUrlInput.text.toString().trim().trimEnd('/')
            val apiKey = apiKeyInput.text.toString().trim()
            val modelName = modelNameInput.text.toString().trim()

            if (baseUrl.isNotEmpty() && apiKey.isNotEmpty() && modelName.isNotEmpty()) {
                saveConfig(baseUrl, apiKey, modelName)
                onSave(baseUrl, apiKey, modelName)
                dismiss()
            }
        }

        cancelButton.setOnClickListener { dismiss() }
    }

    private fun loadSavedConfig() {
        val prefs = BaseApplication.getBaseInstance().prefManager
        baseUrlInput.setText(prefs.getString("ai_agent_custom_base_url", "https://ai2.bytellm.web.id/v1"))
        apiKeyInput.setText(prefs.getString("ai_agent_custom_api_key", ""))
        modelNameInput.setText(prefs.getString("ai_agent_custom_model", "cbai/deepseek-v4.1-flash"))
    }

    private fun saveConfig(baseUrl: String, apiKey: String, modelName: String) {
        val prefs = BaseApplication.getBaseInstance().prefManager
        prefs.putString("ai_agent_custom_base_url", baseUrl)
        prefs.putString("ai_agent_custom_api_key", apiKey)
        prefs.putString("ai_agent_custom_model", modelName)
    }
}
