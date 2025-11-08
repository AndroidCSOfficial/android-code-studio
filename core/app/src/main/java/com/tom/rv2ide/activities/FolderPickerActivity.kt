package com.tom.rv2ide.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

class FolderPickerActivity : Activity() {

  companion object {
    @Volatile var onFolderPicked: ((String) -> Unit)? = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
          addFlags(
              Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                  Intent.FLAG_GRANT_READ_URI_PERMISSION or
                  Intent.FLAG_GRANT_WRITE_URI_PERMISSION
          )
        }
    startActivityForResult(intent, 1001)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 1001) {
      if (resultCode == RESULT_OK) {
        val treeUri: Uri? = data?.data
        if (treeUri != null) {
          try {
            contentResolver.takePersistableUriPermission(
                treeUri,
                (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
            )
          } catch (_: Exception) {}

          onFolderPicked?.invoke(treeUri.toString())
        }
      }
      finish()
    }
  }
}
