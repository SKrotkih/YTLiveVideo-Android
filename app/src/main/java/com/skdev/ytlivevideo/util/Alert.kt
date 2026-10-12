package com.skdev.ytlivevideo.util

import android.app.AlertDialog
import android.content.Context

object Alert {

    /**
     * Shows a dialog with an OK button. [context] must be an Activity context.
     */
    fun showOK(context: Context, title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}
