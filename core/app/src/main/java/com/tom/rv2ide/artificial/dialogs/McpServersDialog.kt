package com.tom.rv2ide.artificial.dialogs

import android.content.Context
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tom.rv2ide.artificial.mcp.McpPresets
import com.tom.rv2ide.artificial.mcp.McpServerConfig
import com.tom.rv2ide.artificial.mcp.McpServerStore

object McpServersDialog {
    fun show(context: Context, onChanged: () -> Unit = {}) {
        val servers = McpServerStore.load()
        val names = servers.map { "${if (it.enabled) "✅" else "⏸️"} ${it.name}" }.toTypedArray() + arrayOf("＋ Add MCP server", "＋ Add prebuilt MCP preset")
        MaterialAlertDialogBuilder(context)
            .setTitle("MCP Servers")
            .setMessage("HTTP MCP servers for agent tools. Prebuilt presets (GitHub, Firebase, Stitch, Vercel, Cloudflare, Supabase) can be added and configured.")
            .setItems(names) { d, which ->
                d.dismiss()
                when {
                    which < servers.size -> manage(context, servers[which], onChanged)
                    which == servers.size -> edit(context, null, onChanged)
                    else -> showPresets(context, onChanged)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showPresets(context: Context, onChanged: () -> Unit) {
        val presets = McpPresets.all()
        MaterialAlertDialogBuilder(context)
            .setTitle("Prebuilt MCP Presets")
            .setItems(presets.map { it.name }.toTypedArray()) { d, which ->
                d.dismiss()
                McpServerStore.addPreset(presets[which])
                Toast.makeText(context, "Preset added (edit to configure)", Toast.LENGTH_SHORT).show()
                onChanged(); show(context, onChanged)
            }
            .setNegativeButton("Back") { _, _ -> show(context, onChanged) }
            .show()
    }

    private fun manage(context: Context, server: McpServerConfig, onChanged: () -> Unit) {
        MaterialAlertDialogBuilder(context)
            .setTitle(server.name)
            .setMessage(server.url)
            .setItems(arrayOf(if (server.enabled) "Disable" else "Enable", "Edit", "Delete")) { d, which ->
                d.dismiss()
                when (which) {
                    0 -> { McpServerStore.upsert(server.copy(enabled = !server.enabled)); onChanged(); show(context, onChanged) }
                    1 -> edit(context, server, onChanged)
                    2 -> { McpServerStore.delete(server.id); onChanged(); show(context, onChanged) }
                }
            }
            .setNegativeButton("Back") { _, _ -> show(context, onChanged) }
            .show()
    }

    private fun edit(context: Context, existing: McpServerConfig?, onChanged: () -> Unit) {
        val container = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 24, 48, 0) }
        fun field(hint: String, value: String): TextInputEditText {
            val e = TextInputEditText(context).apply { setText(value) }
            container.addView(TextInputLayout(context).apply { this.hint = hint; addView(e); setPadding(0, 8, 0, 0) })
            return e
        }
        val name = field("Name", existing?.name ?: "")
        val url = field("URL (HTTP MCP endpoint)", existing?.url ?: "https://")
        val headers = field("Headers (JSON, optional)", if (existing?.headers.isNullOrEmpty()) "" else org.json.JSONObject(existing!!.headers).toString())
        val scroll = ScrollView(context).apply { addView(container) }
        MaterialAlertDialogBuilder(context)
            .setTitle(if (existing == null) "Add MCP Server" else "Edit MCP Server")
            .setView(scroll)
            .setPositiveButton("Save") { _, _ ->
                val n = name.text?.toString()?.trim().orEmpty().ifBlank { "MCP Server" }
                val u = url.text?.toString()?.trim().orEmpty()
                val h = mutableMapOf<String, String>()
                try {
                    val ht = headers.text?.toString()?.trim().orEmpty()
                    if (ht.isNotBlank()) { val obj = org.json.JSONObject(ht); obj.keys().forEach { k -> h[k] = obj.optString(k) } }
                } catch (_: Exception) { Toast.makeText(context, "Headers JSON ignored (invalid)", Toast.LENGTH_SHORT).show() }
                McpServerStore.upsert((existing ?: McpServerConfig(name = n, url = u)).copy(name = n, url = u, headers = h))
                onChanged(); show(context, onChanged)
            }
            .setNegativeButton("Cancel") { _, _ -> show(context, onChanged) }
            .show()
    }
}
