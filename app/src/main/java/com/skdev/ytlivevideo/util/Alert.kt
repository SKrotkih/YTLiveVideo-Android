package com.skdev.ytlivevideo.util

import android.app.AlertDialog

object Alert {

    fun showOK(title: String, message: String) {
        val context = Utils.LaunchedApp.applicationContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, which ->
            dialog.dismiss()
        }

//        // Display a negative button on alert dialog
//        builder.setNegativeButton("No") { dialog, which ->
//            dialog.dismiss()
//        }
//
//        // Display a neutral button on alert dialog
//        builder.setNeutralButton("Cancel") { _, _ ->
//            //Toast.makeText(context,"You cancelled the dialog.", Toast.LENGTH_SHORT).show()
//        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }
}
