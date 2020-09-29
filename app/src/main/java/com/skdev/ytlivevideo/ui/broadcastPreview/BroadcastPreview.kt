package com.skdev.ytlivevideo.ui.broadcastPreview

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.ui.router.Router

class BroadcastPreview: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_preview)
    }


    override fun onResume() {
        super.onResume()
        Router.currentContext = this
    }

//    val viewModel: MainViewModel by viewModels()
//    viewModel.startStreaming(liveBroadcast)



}