package com.tom.rv2ide.artificial.dialogs

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tom.rv2ide.artificial.profiles.AgentPromptProfile
import com.tom.rv2ide.artificial.profiles.AgentPromptProfileStore
import com.tom.rv2ide.artificial.profiles.SkillPresets

object AgentProfilesDialog {
    fun show(context: Context, onChanged: () -> Unit = {}) {
        val (activeId, profiles) = AgentPromptProfileStore.load()
        val names = profiles.map { "${it.name}${if (it.id == activeId) " (active)" else ""}" }.toTypedArray() + arrayOf("＋ Create new profile", "＋ Add prebuilt skill")
        MaterialAlertDialogBuilder(context)
            .setTitle("Agent Profiles & Skills")
            .setItems(names) { d, which ->
                d.dismiss()
                when {
                    which < profiles.size -> selectOrEdit(context, profiles[which], onChanged)
                    which == profiles.size -> edit(context, null, onChanged)
                    else -> showPresets(context, onChanged)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPresets(context: Context, onChanged: () -> Unit) {
        val presets = SkillPresets.all()
        MaterialAlertDialogBuilder(context)
            .setTitle("Prebuilt Skills")
            .setItems(presets.map { it.name }.toTypedArray()) { d, which ->
                d.dismiss()
                AgentPromptProfileStore.addPreset(presets[which])
                Toast.makeText(context, "Skill added", Toast.LENGTH_SHORT).show()
                onChanged(); show(context, onChanged)
            }
            .setNegativeButton("Back") { _, _ -> show(context, onChanged) }
            .show()
    }

    private fun selectOrEdit(context: Context, profile: AgentPromptProfile, onChanged: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(profile.name)
            .setItems(arrayOf("Activate", "Edit", "Duplicate", "Delete")) { d, which ->
                d.dismiss()
                when (which) {
                    0 -> { AgentPromptProfileStore.setActive(profile.id); Toast.makeText(context, "Activated", Toast.LENGTH_SHORT).show(); onChanged() }
                    1 -> edit(context, profile, onChanged)
                    2 -> { AgentPromptProfileStore.upsert(profile.copy(id = java.util.UUID.randomUUID().toString(), name = profile.name + " (copy)")); onChanged(); show(context, onChanged) }
                    3 -> { AgentPromptProfileStore.delete(profile.id); onChanged(); show(context, onChanged) }
                }
            }
            .setNegativeButton("Back") { _, _ -> show(context, onChanged) }
            .show()
    }

    private fun edit(context: Context, existing: AgentPromptProfile?, onChanged: () -> Unit) {
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 0) }
        val nameInput = TextInputEditText(context).apply { setText(existing?.name ?: "New Profile") }
        val promptInput = TextInputEditText(context).apply { setText(existing?.systemPrompt ?: ""); minLines = 4; maxLines = 10; hint = "Extra system prompt / instructions" }
        val toolsSwitch = MaterialSwitch(context).apply { text = "Enable built-in tools/skills"; isChecked = existing?.enableTools ?: true }
        val mcpSwitch = MaterialSwitch(context).apply { text = "Enable MCP tools"; isChecked = existing?.enableMcpTools ?: true }
        val cmdSwitch = MaterialSwitch(context).apply { text = "Allow command execution (risky, opt-in)"; isChecked = existing?.allowCommandExecution ?: false }
        container.addView(TextInputLayout(context).apply { hint = "Profile name"; addView(nameInput) })
        container.addView(TextInputLayout(context).apply { hint = "System prompt"; addView(promptInput); setPadding(0, 12, 0, 0) })
        container.addView(toolsSwitch); container.addView(mcpSwitch); container.addView(cmdSwitch)
        val scroll = ScrollView(context).apply { addView(container) }
        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) "Create Agent Profile" else "Edit Agent Profile")
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                val p = (existing ?: AgentPromptProfile(name = "Profile")).copy(
                    name = nameInput.text?.toString()?.trim().orEmpty().ifBlank { "Profile" },
                    systemPrompt = promptInput.text?.toString().orEmpty(),
                    enableTools = toolsSwitch.isChecked,
                    enableMcpTools = mcpSwitch.isChecked,
                    allowCommandExecution = cmdSwitch.isChecked,
                )
                AgentPromptProfileStore.upsert(p, makeActive = existing == null)
                onChanged(); show(context, onChanged)
            }
            .setNegativeButton("Cancel") { _, _ -> show(context, onChanged) }
            .show()
    }
}
