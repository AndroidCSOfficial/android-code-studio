package com.tom.rv2ide.experimental.depsupdater

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.io.File
import android.util.Log
import com.tom.rv2ide.R

class DependencyUpdaterDialog(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val buildGradleFile: File,
    private val libsVersionsTomlFile: File? = null,
    private val onDependenciesUpdated: (() -> Unit)? = null
) {
    
    private val dependencyParser = DependencyParser()
    private val updateChecker = DependencyUpdateChecker()
    private lateinit var adapter: DependencyAdapter
    private var dialog: Dialog? = null
    private var skipUntil: Long = 0
    
    fun checkForUpdates() {
        if (System.currentTimeMillis() < skipUntil) {
            Log.d("DependencyUpdaterDialog", "Skipping check, skip active until ${skipUntil}")
            scheduleNextCheck()
            return
        }
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                val content = if (buildGradleFile.exists()) {
                    buildGradleFile.readText()
                } else {
                    Log.e("DependencyUpdaterDialog", "Build file not found: ${buildGradleFile.absolutePath}")
                    return@launch
                }
                
                val dependencies = dependencyParser.parseDependencies(content)
                Log.d("DependencyUpdaterDialog", "Parsed ${dependencies.size} dependencies")
                
                val versionCatalog = if (libsVersionsTomlFile?.exists() == true) {
                    dependencyParser.parseVersionCatalog(libsVersionsTomlFile.readText())
                } else {
                    emptyMap()
                }
                
                val updatedDeps = updateChecker.checkForUpdates(dependencies, versionCatalog)
                val validDeps = updatedDeps.filter { it.group.isNotEmpty() && it.name.isNotEmpty() }
                val updatesAvailable = validDeps.filter { it.hasUpdate }
                
                Log.d("DependencyUpdaterDialog", "Found ${updatesAvailable.size} updates available")
                
                if (updatesAvailable.isNotEmpty()) {
                    showUpdateDialog(validDeps)
                }
                
            } catch (e: Exception) {
                Log.e("DependencyUpdaterDialog", "Error checking updates", e)
            }
        }
    }
    
    private fun scheduleNextCheck() {
        lifecycleOwner.lifecycleScope.launch {
            val remainingTime = skipUntil - System.currentTimeMillis()
            if (remainingTime > 0) {
                delay(remainingTime)
                checkForUpdates()
            }
        }
    }
    
    private fun showUpdateDialog(dependencies: List<Dependency>) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_dependency_updates, null)
        
        val tvDialogTitle = dialogView.findViewById<MaterialTextView>(R.id.tvDialogTitle)
        val rvDependencies = dialogView.findViewById<RecyclerView>(R.id.rvDependencies)
        val btnSkip = dialogView.findViewById<MaterialButton>(R.id.btnSkip)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)
        val btnUpdateAll = dialogView.findViewById<MaterialButton>(R.id.btnUpdateAll)
        
        val updatesCount = dependencies.count { it.hasUpdate }
        tvDialogTitle.text = context.getString(R.string.updates_available, "$updatesCount")
        
        rvDependencies.layoutManager = LinearLayoutManager(context)
        adapter = DependencyAdapter { dependency ->
            showWarningDialog(dependency)
        }
        rvDependencies.adapter = adapter
        adapter.submitList(dependencies)
        
        dialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnSkip.setOnClickListener {
            skipUntil = System.currentTimeMillis() + 60000
            dialog?.dismiss()
            scheduleNextCheck()
        }
        
        btnClose.setOnClickListener {
            dialog?.dismiss()
        }
        
        btnUpdateAll.setOnClickListener {
            showWarningDialogForAll(dependencies.filter { it.hasUpdate })
        }
        
        dialog?.show()
    }
    
    private fun showWarningDialog(dependency: Dependency) {
        val warningView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
        
        val tvTitle = warningView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
        val tvMessage = warningView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
        val btnCancel = warningView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnUpdate = warningView.findViewById<MaterialButton>(R.id.btnUpdate)
        
        tvTitle.text = "⚠️ Warning"
        tvMessage.text = context.getString(R.string.dep_update_single_message, dependency.group, dependency.name, dependency.currentVersion, dependency.latestVersion)
        
        val warningDialog = MaterialAlertDialogBuilder(context)
            .setView(warningView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener {
            warningDialog.dismiss()
        }
        
        btnUpdate.setOnClickListener {
            warningDialog.dismiss()
            showUpdateConfirmDialog(dependency)
        }
        
        warningDialog.show()
    }
    
    private fun showWarningDialogForAll(dependencies: List<Dependency>) {
        val warningView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
        
        val tvTitle = warningView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
        val tvMessage = warningView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
        val btnCancel = warningView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnUpdate = warningView.findViewById<MaterialButton>(R.id.btnUpdate)
        
        tvTitle.text = "⚠️ Warning"
        tvMessage.text = context.getString(R.string.dep_update_all_message, dependencies.size)
        
        val warningDialog = MaterialAlertDialogBuilder(context)
            .setView(warningView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener {
            warningDialog.dismiss()
        }
        
        btnUpdate.setOnClickListener {
            warningDialog.dismiss()
            dialog?.dismiss()
            updateAllDependencies(dependencies)
        }
        
        warningDialog.show()
    }
    
    private fun showUpdateConfirmDialog(dependency: Dependency) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
        
        val tvConfirmTitle = dialogView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
        val tvConfirmMessage = dialogView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdate)
        
        tvConfirmTitle.text = context.getString(R.string.dep_confirm_update)
        tvConfirmMessage.text = "${dependency.group}:${dependency.name}\n${dependency.currentVersion} → ${dependency.latestVersion}\n\nProceed with update?"
        
        val confirmDialog = MaterialAlertDialogBuilder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()
        
        btnCancel.setOnClickListener {
            confirmDialog.dismiss()
        }
        
        btnUpdate.setOnClickListener {
            confirmDialog.dismiss()
            updateDependency(dependency)
        }
        
        confirmDialog.show()
    }
    
    private fun updateDependency(dependency: Dependency) {
        try {
            when {
                dependency.catalogReference != null -> {
                    updateInVersionCatalog(dependency)
                }
                dependency.variableReference != null -> {
                    updateVariableInBuildGradle(dependency)
                }
                else -> {
                    updateDirectInBuildGradle(dependency)
                }
            }
            
            val successView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
            val tvTitle = successView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
            val tvMessage = successView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
            val btnCancel = successView.findViewById<MaterialButton>(R.id.btnCancel)
            val btnUpdate = successView.findViewById<MaterialButton>(R.id.btnUpdate)
            
            tvTitle.text = context.getString(R.string.dep_success)
            tvMessage.text = "${dependency.group}:${dependency.name} updated to ${dependency.latestVersion}"
            btnCancel.visibility = View.GONE
            btnUpdate.text = "OK"
            
            val successDialog = MaterialAlertDialogBuilder(context)
                .setView(successView)
                .setCancelable(true)
                .create()
            
            btnUpdate.setOnClickListener {
                successDialog.dismiss()
                onDependenciesUpdated?.invoke()
                dialog?.dismiss()
                checkForUpdates()
            }
            
            successDialog.show()
                
        } catch (e: Exception) {
            Log.e("DependencyUpdaterDialog", "Error updating dependency", e)
            
            val errorView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
            val tvTitle = errorView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
            val tvMessage = errorView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
            val btnCancel = errorView.findViewById<MaterialButton>(R.id.btnCancel)
            val btnUpdate = errorView.findViewById<MaterialButton>(R.id.btnUpdate)
            
            tvTitle.text = context.getString(R.string.dep_error)
            tvMessage.text = context.getString(R.string.dep_update_failed, e.message)
            btnCancel.visibility = View.GONE
            btnUpdate.text = "OK"
            
            val errorDialog = MaterialAlertDialogBuilder(context)
                .setView(errorView)
                .setCancelable(true)
                .create()
            
            btnUpdate.setOnClickListener {
                errorDialog.dismiss()
            }
            
            errorDialog.show()
        }
    }
    
    private fun updateAllDependencies(dependencies: List<Dependency>) {
        val progressView = LayoutInflater.from(context).inflate(R.layout.dialog_deps_progress, null)
        
        val progressDialog = MaterialAlertDialogBuilder(context)
            .setView(progressView)
            .setCancelable(false)
            .create()
        
        progressDialog.show()
        
        lifecycleOwner.lifecycleScope.launch {
            try {
                var successCount = 0
                var failCount = 0
                
                dependencies.forEach { dependency ->
                    try {
                        when {
                            dependency.catalogReference != null -> {
                                updateInVersionCatalog(dependency)
                            }
                            dependency.variableReference != null -> {
                                updateVariableInBuildGradle(dependency)
                            }
                            else -> {
                                updateDirectInBuildGradle(dependency)
                            }
                        }
                        successCount++
                    } catch (e: Exception) {
                        Log.e("DependencyUpdaterDialog", "Error updating ${dependency.name}", e)
                        failCount++
                    }
                }
                
                progressDialog.dismiss()
                
                val resultView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
                val tvTitle = resultView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
                val tvMessage = resultView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
                val btnCancel = resultView.findViewById<MaterialButton>(R.id.btnCancel)
                val btnUpdate = resultView.findViewById<MaterialButton>(R.id.btnUpdate)
                
                tvTitle.text = context.getString(R.string.dep_update_complete)
                tvMessage.text = context.getString(R.string.dep_update_result, successCount, failCount)
                btnCancel.visibility = View.GONE
                btnUpdate.text = "OK"
                
                val resultDialog = MaterialAlertDialogBuilder(context)
                    .setView(resultView)
                    .setCancelable(true)
                    .create()
                
                btnUpdate.setOnClickListener {
                    resultDialog.dismiss()
                    onDependenciesUpdated?.invoke()
                    dialog?.dismiss()
                }
                
                resultDialog.show()
                    
            } catch (e: Exception) {
                progressDialog.dismiss()
                
                val errorView = LayoutInflater.from(context).inflate(R.layout.dialog_update_confirm, null)
                val tvTitle = errorView.findViewById<MaterialTextView>(R.id.tvConfirmTitle)
                val tvMessage = errorView.findViewById<MaterialTextView>(R.id.tvConfirmMessage)
                val btnCancel = errorView.findViewById<MaterialButton>(R.id.btnCancel)
                val btnUpdate = errorView.findViewById<MaterialButton>(R.id.btnUpdate)
                
                tvTitle.text = context.getString(R.string.dep_error)
                tvMessage.text = context.getString(R.string.dep_update_all_failed, e.message)
                btnCancel.visibility = View.GONE
                btnUpdate.text = "OK"
                
                val errorDialog = MaterialAlertDialogBuilder(context)
                    .setView(errorView)
                    .setCancelable(true)
                    .create()
                
                btnUpdate.setOnClickListener {
                    errorDialog.dismiss()
                }
                
                errorDialog.show()
            }
        }
    }
    
    private fun updateInVersionCatalog(dependency: Dependency) {
        if (libsVersionsTomlFile == null || !libsVersionsTomlFile.exists()) {
            throw Exception("libs.versions.toml not found")
        }
        
        var tomlContent = libsVersionsTomlFile.readText()
        val normalizedRef = dependency.catalogReference?.replace(".", "-") ?: return
        
        val versionRefPattern = """([a-zA-Z0-9\-_]+)\s*=\s*\{\s*group\s*=\s*"${dependency.group}"\s*,\s*name\s*=\s*"${dependency.name}"\s*,\s*version\.ref\s*=\s*"([^"]+)"""".toRegex()
        val versionRefMatch = versionRefPattern.find(tomlContent)
        
        if (versionRefMatch != null) {
            val versionKey = versionRefMatch.groupValues[2]
            val versionPattern = """($versionKey\s*=\s*")([^"]+)(")""".toRegex()
            tomlContent = versionPattern.replace(tomlContent) {
                "${it.groupValues[1]}${dependency.latestVersion}${it.groupValues[3]}"
            }
            Log.d("DependencyUpdaterDialog", "Updated version key: $versionKey to ${dependency.latestVersion}")
        } else {
            val directPattern = """($normalizedRef\s*=\s*")([^:]+):([^:]+):([^"]+)(")""".toRegex()
            tomlContent = directPattern.replace(tomlContent) {
                "${it.groupValues[1]}${it.groupValues[2]}:${it.groupValues[3]}:${dependency.latestVersion}${it.groupValues[5]}"
            }
            Log.d("DependencyUpdaterDialog", "Updated direct library: $normalizedRef to ${dependency.latestVersion}")
        }
        
        libsVersionsTomlFile.writeText(tomlContent)
        Log.d("DependencyUpdaterDialog", "Updated libs.versions.toml")
    }
    
    private fun updateVariableInBuildGradle(dependency: Dependency) {
        var text = buildGradleFile.readText()
        
        val varPattern = """(val|var)\s+${dependency.variableReference}\s*=\s*["']([^"']+)["']""".toRegex()
        text = varPattern.replace(text) { matchResult ->
            "${matchResult.groupValues[1]} ${dependency.variableReference} = \"${dependency.latestVersion}\""
        }
        
        buildGradleFile.writeText(text)
        Log.d("DependencyUpdaterDialog", "Updated variable ${dependency.variableReference} in build.gradle.kts")
    }
    
    private fun updateDirectInBuildGradle(dependency: Dependency) {
        var text = buildGradleFile.readText()
        
        val oldPattern = "${dependency.group}:${dependency.name}:${dependency.currentVersion}"
        val newPattern = "${dependency.group}:${dependency.name}:${dependency.latestVersion}"
        text = text.replace(oldPattern, newPattern)
        
        buildGradleFile.writeText(text)
        Log.d("DependencyUpdaterDialog", "Updated direct dependency in build.gradle.kts")
    }
}