package com.tom.composepreview

import android.content.Intent
import android.os.Bundle
import dalvik.system.DexClassLoader
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

class ComposePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Accept optional extras: preview_apk_path, preview_class, preview_function
        val apkPath = intent.getStringExtra("preview_apk_path")
        val previewClass = intent.getStringExtra("preview_class")
        val previewFunction = intent.getStringExtra("preview_function")

        if (!apkPath.isNullOrEmpty() && !previewClass.isNullOrEmpty()) {
            // Try to load the class from provided apk/dex
            try {
                val optimizedDir = File(cacheDir, "dex")
                optimizedDir.mkdirs()
                val loader = DexClassLoader(apkPath, optimizedDir.absolutePath, null, classLoader)
                val cls = loader.loadClass(previewClass)
                // Look for a static composable wrapper function we agreed upon: previewFunction
                // Fallback: just show a message that class was loaded
                setContent {
                    ComposePreviewLoaded(previewClass, previewFunction ?: "")
                }
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        setContent {
            ComposePreviewApp()
        }
    }
}

@Composable
fun ComposePreviewApp() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Compose Preview", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = {}) {
                    Text("Button")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ComposePreviewAppPreview() {
    ComposePreviewApp()
}

@Composable
fun ComposePreviewLoaded(className: String, functionName: String) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Loaded: $className", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Function: $functionName")
            }
        }
    }
}
