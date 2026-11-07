package com.tom.rv2ide.artificial.dialogs

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tom.rv2ide.R
import com.tom.rv2ide.artificial.profiles.CustomProviderProfile
import com.tom.rv2ide.artificial.profiles.CustomProviderProfileStore

class CustomProviderConfigDialog(
    private val onSave: (baseUrl: String, modelName: String, apiKey: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var profileSelectorLayout: TextInputLayout
    private lateinit var profileSelector: AutoCompleteTextView
    private lateinit var profileNameInput: TextInputEditText
    private lateinit var baseUrlInput: TextInputEditText
    private lateinit var modelNameInput: TextInputEditText
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var saveButton: MaterialButton
    private lateinit var deleteButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var profiles: MutableList<CustomProviderProfile> = mutableListOf()
    private var activeId: String? = null
    private var selectedId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_custom_provider_config, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        profileSelectorLayout = view.findViewById(R.id.profileSelectorLayout)
        profileSelector = view.findViewById(R.id.profileSelector)
        profileNameInput = view.findViewById(R.id.profileNameInput)
        baseUrlInput = view.findViewById(R.id.baseUrlInput)
        modelNameInput = view.findViewById(R.id.modelNameInput)
        apiKeyInput = view.findViewById(R.id.apiKeyInput)
        saveButton = view.findViewById(R.id.saveButton)
        deleteButton = view.findViewById(R.id.deleteButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        reload()
        setupSelector()

        saveButton.setOnClickListener {
            val name = profileNameInput.text.toString().trim().ifBlank { "Custom" }
            val baseUrl = baseUrlInput.text.toString().trim()
            val modelName = modelNameInput.text.toString().trim()
            val apiKey = apiKeyInput.text.toString().trim()
            if (baseUrl.isEmpty() || modelName.isEmpty()) {
                Toast.makeText(requireContext(), "Base URL and model name are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val current = profiles.firstOrNull { it.id == selectedId }
            val profile = (current ?: CustomProviderProfile(name = name, baseUrl = baseUrl, modelName = modelName, apiKey = apiKey)).copy(
                name = name, baseUrl = baseUrl, modelName = modelName, apiKey = apiKey
            )
            val saved = CustomProviderProfileStore.upsert(profile, makeActive = true)
            selectedId = saved.id
            reload()
            setupSelector()
            onSave(baseUrl, modelName, apiKey)
            Toast.makeText(requireContext(), "Profile saved and activated", Toast.LENGTH_SHORT).show()
        }

        deleteButton.setOnClickListener {
            val id = selectedId ?: return@setOnClickListener
            if (profiles.size <= 1) {
                Toast.makeText(requireContext(), "Keep at least one profile", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CustomProviderProfileStore.delete(id)
            reload()
            setupSelector()
        }

        view.findViewById<MaterialButton?>(R.id.newProfileButton)?.setOnClickListener {
            selectedId = null
            profileNameInput.setText("New Profile")
            baseUrlInput.setText("https://api.openai.com")
            modelNameInput.setText("gpt-4o-mini")
            apiKeyInput.setText("")
            profileSelector.setText("(New Profile)", false)
        }

        cancelButton.setOnClickListener { dismiss() }
    }

    private fun reload() {
        val (aid, list) = CustomProviderProfileStore.load()
        activeId = aid
        profiles = list.toMutableList()
        if (profiles.isEmpty()) {
            val def = CustomProviderProfile(name = "Default", baseUrl = "https://api.openai.com", modelName = "gpt-4o-mini")
            CustomProviderProfileStore.save(def.id, listOf(def))
            profiles = mutableListOf(def)
            activeId = def.id
        }
        if (selectedId == null || profiles.none { it.id == selectedId }) selectedId = activeId ?: profiles.first().id
        bindSelected()
    }

    private fun setupSelector() {
        val names = profiles.map { "${it.name}${if (it.id == activeId) " (active)" else ""}" } + listOf("＋ New profile")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, names)
        profileSelector.setAdapter(adapter)
        val idx = profiles.indexOfFirst { it.id == selectedId }
        if (idx >= 0) profileSelector.setText(names[idx], false)
        profileSelector.setOnItemClickListener { _, _, pos, _ ->
            if (pos >= profiles.size) {
                selectedId = null
                profileNameInput.setText("New Profile")
                baseUrlInput.setText("https://api.openai.com")
                modelNameInput.setText("gpt-4o-mini")
                apiKeyInput.setText("")
                profileSelector.setText("(New Profile)", false)
            } else {
                selectedId = profiles[pos].id
                CustomProviderProfileStore.setActive(profiles[pos].id)
                activeId = profiles[pos].id
                bindSelected()
                setupSelector()
                val p = profiles[pos]
                onSave(p.baseUrl, p.modelName, p.apiKey)
            }
        }
    }

    private fun bindSelected() {
        val p = profiles.firstOrNull { it.id == selectedId } ?: profiles.firstOrNull() ?: return
        profileNameInput.setText(p.name)
        baseUrlInput.setText(p.baseUrl)
        modelNameInput.setText(p.modelName)
        apiKeyInput.setText(p.apiKey)
    }
}
