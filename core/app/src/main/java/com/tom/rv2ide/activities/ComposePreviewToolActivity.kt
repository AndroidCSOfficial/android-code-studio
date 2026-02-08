package com.tom.rv2ide.activities

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.widget.Toast
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class ComposePreviewToolActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val previewFile = intent?.getStringExtra("preview_file")
        val previewFunction = intent?.getStringExtra("preview_function")
        setAppContext(this)
        setContent {
            ComposePreviewToolScreen(previewFile ?: "", previewFunction)
        }
    }
}

@Composable
fun ComposePreviewToolScreen(previewFile: String? = null, previewFunction: String? = null) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Android Code Studio — Compose Preview", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (!previewFile.isNullOrEmpty()) {
                    Text("File: $previewFile")
                }
                if (!previewFunction.isNullOrEmpty()) {
                    Text("Preview: $previewFunction")
                }
                Spacer(Modifier.height(12.dp))
                if (!previewFile.isNullOrEmpty()) {
                    var status by remember { mutableStateOf("Idle") }
                    Text("Status: $status")
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        status = "Building..."
                        GlobalScope.launch(Dispatchers.IO) {
                            try {
                                val result = com.tom.rv2ide.preview.PreviewPackager.packagePreview(previewFile, previewFunction)
                                if (result.success && result.artifactPath != null) {
                                    // notify main thread
                                    launch(Dispatchers.Main) {
                                        status = "Built: ${result.artifactPath}"
                                        Toast.makeText(getAppContext(), "Built: ${result.artifactPath}", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    launch(Dispatchers.Main) {
                                        status = "Build failed"
                                        Toast.makeText(getAppContext(), "Build failed: ${result.logs.take(200)}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    status = "Exception"
                                    Toast.makeText(getAppContext(), "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }) {
                        Text("Build & Convert to DEX")
                    }
                } else {
                    Button(onClick = {}) {
                        Text("Sample Button")
                    }
                }
            }
        }
    }
}

// Simple way to get a Context inside composables in this activity
private var appContext: Context? = null
fun setAppContext(ctx: Context) { appContext = ctx }
fun getAppContext(): Context = appContext ?: throw IllegalStateException("App context not set")

@Preview(showBackground = true)
@Composable
fun ComposePreviewToolScreenPreview() {
    ComposePreviewToolScreen()
}
