package com.tom.rv2ide.uidesigner.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.tom.rv2ide.R
import com.tom.rv2ide.activities.ComposePreviewToolActivity

class ComposePreviewFragment : Fragment() {

  private var filePath: String? = null
  private var fileText: String? = null
  private var previewNamesJson: String? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    arguments?.let {
      filePath = it.getString(ARG_PATH)
      fileText = it.getString(ARG_TEXT)
      previewNamesJson = it.getString(ARG_PREVIEWS)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    val composeView = ComposeView(requireContext())
    val previews = if (!previewNamesJson.isNullOrEmpty()) {
      try {
        val gson = com.google.gson.Gson()
        gson.fromJson(previewNamesJson, Array<String>::class.java).toList()
      } catch (e: Exception) {
        parsePreviewFunctions(fileText ?: "")
      }
    } else parsePreviewFunctions(fileText ?: "")
    composeView.setContent {
      ComposePreviewContent(filePath ?: "", fileText ?: "", previews)
    }
    return composeView
  }

  private fun parsePreviewFunctions(text: String): List<String> {
    if (text.isEmpty()) return emptyList()
    val regex = Regex("@Preview[\\s\\S]*?fun\\s+(\\w+)\\s*\\(")
    return regex.findAll(text).map { it.groupValues[1] }.toList()
  }

  companion object {
    private const val ARG_PATH = "arg_path"
    private const val ARG_TEXT = "arg_text"
    private const val ARG_PREVIEWS = "arg_previews"

    fun newInstance(path: String, text: String): ComposePreviewFragment {
      val frag = ComposePreviewFragment()
      frag.arguments = Bundle().apply {
        putString(ARG_PATH, path)
        putString(ARG_TEXT, text)
      }
      return frag
    }

    fun newInstance(path: String, text: String, previewsJson: String): ComposePreviewFragment {
      val frag = ComposePreviewFragment()
      frag.arguments = Bundle().apply {
        putString(ARG_PATH, path)
        putString(ARG_TEXT, text)
        putString(ARG_PREVIEWS, previewsJson)
      }
      return frag
    }
  }
}

@Composable
fun ComposePreviewContent(path: String, text: String, previews: List<String>) {
  MaterialTheme {
    Surface(modifier = Modifier.fillMaxSize()) {
      Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Compose preview detected for:\n$path", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(12.dp))
        if (previews.isEmpty()) {
          Text("No @Preview functions found. Detected @Composable: ${text.contains("@Composable")}.")
        } else {
          Text("Previews:")
          Spacer(modifier = Modifier.height(8.dp))
          previews.forEach { name ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
              Text(name, modifier = Modifier.weight(1f))
              val ctx = androidx.compose.ui.platform.LocalContext.current
              Button(onClick = {
                ctx.startActivity(Intent(ctx, ComposePreviewToolActivity::class.java).apply {
                  putExtra("preview_file", path)
                  putExtra("preview_function", name)
                })
              }) {
                Text("Open")
              }
            }
          }
        }
      }
    }
  }
}
