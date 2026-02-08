package com.tom.rv2ide.uidesigner

import android.os.Handler
import android.os.Looper
import androidx.core.view.isVisible
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.tom.rv2ide.eventbus.events.editor.DocumentOpenEvent
import com.tom.rv2ide.uidesigner.fragments.ComposePreviewFragment
import com.tom.rv2ide.lsp.api.ILanguageServerRegistry
import com.tom.rv2ide.lsp.kotlin.KotlinLanguageServer
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ComposePreviewManager(private val activity: UIDesignerActivity) {

  private val mainHandler = Handler(Looper.getMainLooper())

  init {
    EventBus.getDefault().register(this)
  }

  fun dispose() {
    try {
      EventBus.getDefault().unregister(this)
    } catch (e: Exception) {
      // ignore
    }
  }

  @Subscribe(threadMode = ThreadMode.ASYNC)
  fun onDocumentOpen(event: DocumentOpenEvent) {
    val path = event.openedFile.toString()
    if (!path.endsWith(".kt") && !path.endsWith(".kts")) return

    val text = event.text
    if (!text.contains("@Composable") && !text.contains("@Preview")) return

    // Try to use Kotlin LSP to get document symbols and inspect only function regions for @Preview
    val server = ILanguageServerRegistry.getDefault().getServer(KotlinLanguageServer.SERVER_ID) as? KotlinLanguageServer

    if (server != null) {
      val uri = event.openedFile.toUri().toString()
      server.requestDocumentSymbols(uri) { result ->
        val previews = mutableListOf<String>()

        try {
          val symbolsJson = if (result != null) result.toString() else null
          val detected = ComposePreviewDetector.detect(text, symbolsJson)
          previews.addAll(detected)
        } catch (e: Exception) {
          // fall back handled below
        }

        mainHandler.post {
          try {
            activity.openHierarchyView()
            val fm = activity.supportFragmentManager
            val frag = if (previews.isNotEmpty()) {
              ComposePreviewFragment.newInstance(path, text, com.google.gson.Gson().toJson(previews))
            } else {
              ComposePreviewFragment.newInstance(path, text)
            }

            fm.beginTransaction()
              .replace(com.tom.rv2ide.uidesigner.R.id.compose_preview_container, frag)
              .commitAllowingStateLoss()

            // Make container visible
            val binding = activity.binding
            binding?.root?.findViewById<android.view.View>(com.tom.rv2ide.uidesigner.R.id.compose_preview_container)?.isVisible = true
          } catch (e: Exception) {
            // ignore
          }
        }
      }
    } else {
      // Fallback: existing simple behavior on main thread
      mainHandler.post {
        try {
          activity.openHierarchyView()
          val fm = activity.supportFragmentManager
          val frag = ComposePreviewFragment.newInstance(path, text)
          fm.beginTransaction()
            .replace(com.tom.rv2ide.uidesigner.R.id.compose_preview_container, frag)
            .commitAllowingStateLoss()

          val binding = activity.binding
          binding?.root?.findViewById<android.view.View>(com.tom.rv2ide.uidesigner.R.id.compose_preview_container)?.isVisible = true
        } catch (e: Exception) {
          // ignore
        }
      }
    }
  }

  private fun checkPreviewAbove(text: String, startLine: Int): Boolean {
    val lines = text.split('\n')
    val from = kotlin.math.max(0, startLine - 6)
    for (i in startLine - 1 downTo from) {
      val l = lines.getOrNull(i) ?: continue
      if (l.contains("@Preview")) return true
      // stop if we reach another top-level declaration line (heuristic)
      if (l.trim().startsWith("fun ") || l.trim().startsWith("class ") || l.trim().startsWith("object ")) return false
    }
    return false
  }
}
