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

class CustomProviderConfigDialog(
    private val onSave: (baseUrl: String, modelName: String, apiKey: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var baseUrlInput: TextInputEditText
    private lateinit var modelNameInput: TextInputEditText
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_custom_provider_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        baseUrlInput = view.findViewById(R.id.baseUrlInput)
        modelNameInput = view.findViewById(R.id.modelNameInput)
        apiKeyInput = view.findViewById(R.id.apiKeyInput)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        loadSavedConfig()

        saveButton.setOnClickListener {
            val baseUrl = baseUrlInput.text.toString().trim()
            val modelName = modelNameInput.text.toString().trim()
            val apiKey = apiKeyInput.text.toString().trim()

            if (baseUrl.isNotEmpty() && modelName.isNotEmpty()) {
                saveConfig(baseUrl, modelName, apiKey)
                onSave(baseUrl, modelName, apiKey)
                dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dismiss()
        }
    }

    private fun loadSavedConfig() {
        val prefs = BaseApplication.getBaseInstance().prefManager
        baseUrlInput.setText(prefs.getString("ai_agent_custom_base_url", "https://api.openai.com"))
        modelNameInput.setText(prefs.getString("ai_agent_custom_model_name", "gpt-4o-mini"))
        apiKeyInput.setText(prefs.getString("ai_agent_custom_api_key", ""))
    }

    private fun saveConfig(baseUrl: String, modelName: String, apiKey: String) {
        val prefs = BaseApplication.getBaseInstance().prefManager
        prefs.putString("ai_agent_custom_base_url", baseUrl)
        prefs.putString("ai_agent_custom_model_name", modelName)
        prefs.putString("ai_agent_custom_api_key", apiKey)
    }
}
