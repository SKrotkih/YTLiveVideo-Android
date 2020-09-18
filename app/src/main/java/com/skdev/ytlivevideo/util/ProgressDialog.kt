package com.skdev.ytlivevideo.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.skdev.ytlivevideo.R
import kotlinx.android.synthetic.main.activity_progress_dialog.*

class ProgressDialog {
    companion object {
        fun create(context: Context, resId: Int): Dialog {
            val dialog = Dialog(context)
            val inflate = LayoutInflater.from(context).inflate(R.layout.activity_progress_dialog, null)
            dialog.setContentView(inflate)
            dialog.setCancelable(false)
            dialog.title.text = context.getText(resId).toString()
            dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
            return dialog
        }
    }
}
