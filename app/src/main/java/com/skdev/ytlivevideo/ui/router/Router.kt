package com.skdev.ytlivevideo.ui.router

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.broadcastPreview.BroadcastPreview
import com.skdev.ytlivevideo.ui.createBroadcast.CreateNewBroadcast
import com.skdev.ytlivevideo.ui.youtubePlayer.YouTubePlayerActivity

@SuppressLint("StaticFieldLeak")
object Router {
    var startActivityType: StartActivity? = null
    var currentContext: Context? = null

    enum class StartActivity {
        EVENT_PREVIEW {
            override fun run() {}
            override fun run(params: Any) {
                openEventPreview(params)
            }
        },
        CREATE_BROADCAST {
            override fun run() {
                createBroadcast()
            }
            override fun run(params: Any) {
            }
        },
        YT_FULLSCREEN_PLAYER {
            override fun run() {
            }
            override fun run(params: Any) {
                playVideoOnYoutubePlayer(params)
            }
        };

        abstract fun run()
        abstract fun run(params: Any)
    }

    init {
        println("init completed")
    }

    fun startActivityIfNeeded() {
        if (startActivityType != null) {
            startActivityType!!.run()
            startActivityType = null
        }
    }

    // Actions
    private fun openEventPreview(params: Any) {
        if (currentContext == null) {
            return
        }
        val broadcastItem = params as? LiveBroadcastItem
        if (broadcastItem != null) {
            val intent = Intent(currentContext!!, BroadcastPreview::class.java)
            intent.putExtra("broadcastId", broadcastItem.id)
            intent.putExtra("state", broadcastItem.state!!.value())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            currentContext!!.startActivity(intent)
        }
    }

    private fun createBroadcast() {
        val intent = Intent(currentContext!!, CreateNewBroadcast::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        currentContext!!.startActivity(intent)
    }

    private fun playVideoOnYoutubePlayer(params: Any) {
        if (currentContext == null) {
            return
        }
        val broadcastId = params as? String
        if (broadcastId != null) {
            val intent = Intent(currentContext!!, YouTubePlayerActivity::class.java)
            intent.putExtra("broadcastId", broadcastId)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            currentContext!!.startActivity(intent)
        }
    }
}
