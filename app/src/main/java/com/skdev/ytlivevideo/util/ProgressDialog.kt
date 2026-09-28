package com.skdev.ytlivevideo.util

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import com.skdev.ytlivevideo.databinding.ActivityProgressDialogBinding

class ProgressDialog {
    companion object {
        fun create(context: Context, resId: Int): Dialog {
            return create(context, context.getText(resId).toString())
        }

        fun create(context: Context, title: String): Dialog {
            val dialog = Dialog(context)
            val binding = ActivityProgressDialogBinding.inflate(LayoutInflater.from(context))
            dialog.setContentView(binding.root)
            dialog.setCancelable(false)
            binding.title.text = title
            dialog.window!!.setBackgroundDrawable(
                ColorDrawable(Color.TRANSPARENT)
            )
            return dialog
        }
    }
}
