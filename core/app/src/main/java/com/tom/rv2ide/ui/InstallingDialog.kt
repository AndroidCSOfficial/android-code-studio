package com.tom.rv2ide.ui

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class InstallingDialog
private constructor(
    private val dialog: androidx.appcompat.app.AlertDialog,
    private val tvMessage: TextView,
) {
  companion object {
    fun create(context: Context, message: String = "Installing..."): InstallingDialog {
      // 创建根布局
      val rootLayout =
          LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)

            layoutParams =
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
          }

      // 添加圆形进度条
      val progressBar =
          ProgressBar(context).apply {
            isIndeterminate = true
            layoutParams = LinearLayout.LayoutParams(120, 120).apply { gravity = Gravity.CENTER }
          }

      // 提示文本
      val tvMessage =
          TextView(context).apply {
            text = message
            textSize = 16f
            setTextColor(
                context.getColor(
                    com.google.android.material.R.color.material_on_surface_emphasis_high_type
                )
            )
            layoutParams =
                LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                    )
                    .apply {
                      topMargin = 32
                      gravity = Gravity.CENTER
                    }
          }

      rootLayout.addView(progressBar)
      rootLayout.addView(tvMessage)

      // 创建 Material 风格的 Dialog
      val dialog =
          MaterialAlertDialogBuilder(context)
              .setView(rootLayout)
              .setCancelable(false) // 禁止返回键关闭
              .create()

      // 背景透明 + 圆角
      dialog.window?.setBackgroundDrawableResource(
          com.google.android.material.R.color.m3_ref_palette_neutral10
      )

      return InstallingDialog(dialog, tvMessage)
    }
  }

  fun show() {
    dialog.show()
  }

  fun dismiss() {
    dialog.dismiss()
  }

  fun setMessage(message: String) {
    tvMessage.text = message
  }
}
