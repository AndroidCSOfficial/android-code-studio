package com.tom.rv2ide.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.tom.rv2ide.R
import com.tom.rv2ide.adapters.FileChangeAdapter
import com.tom.rv2ide.artificial.agents.AIAgentManager
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class ReviewChangesActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var summaryText: MaterialTextView
    private lateinit var fileChangesRecyclerView: RecyclerView
    private lateinit var codePreview: CodeEditor
    private lateinit var cancelBtn: MaterialButton
    private lateinit var applyBtn: MaterialButton
    private lateinit var fileChangeAdapter: FileChangeAdapter

    private lateinit var modifications: ArrayList<ModificationData>
    private var currentSelectedIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_review_changes)

        modifications = intent.getParcelableArrayListExtra("modifications") ?: arrayListOf()

        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupButtons()
        displaySummary()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        summaryText = findViewById(R.id.summaryText)
        fileChangesRecyclerView = findViewById(R.id.fileChangesRecyclerView)
        codePreview = findViewById(R.id.codePreview)
        cancelBtn = findViewById(R.id.cancelBtn)
        applyBtn = findViewById(R.id.applyBtn)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.review_changes)
        }
        toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun setupRecyclerView() {
        fileChangeAdapter = FileChangeAdapter(modifications) { position ->
            currentSelectedIndex = position
            displayFileContent(modifications[position])
        }

        fileChangesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ReviewChangesActivity)
            adapter = fileChangeAdapter
        }

        if (modifications.isNotEmpty()) {
            displayFileContent(modifications[0])
            fileChangeAdapter.setSelected(0)
        }
    }

    private fun setupButtons() {
        cancelBtn.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        applyBtn.setOnClickListener {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun displaySummary() {
        val totalFiles = modifications.size
        val newFiles = modifications.count { it.isNewFile }
        val modifiedFiles = totalFiles - newFiles

        val summary = buildString {
            append("📊 Total Changes: $totalFiles file(s)\n")
            if (newFiles > 0) append("🆕 New Files: $newFiles\n")
            if (modifiedFiles > 0) append("✏️ Modified Files: $modifiedFiles\n")
        }

        summaryText.text = summary
    }

    private fun displayFileContent(modification: ModificationData) {
        codePreview.setText(modification.content)
    }

    companion object {
        const val REQUEST_CODE_REVIEW = 1001
    }
}
