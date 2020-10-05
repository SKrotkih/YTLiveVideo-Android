package com.skdev.ytlivevideo.ui.broadcastPreview

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.android.volley.toolbox.ImageLoader
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.services.youtube.model.LiveStream
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.BroadcastPreviewData
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastsInteractor
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.requests.*
import com.skdev.ytlivevideo.ui.router.Router
import com.skdev.ytlivevideo.ui.videoStreamingScene.VideoStreamingActivity
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import kotlinx.android.synthetic.main.activity_broadcast_preview.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class BroadcastPreview: AppCompatActivity() {
    private var broadcastItem: LiveBroadcastItem? = null
    private var streamItem: LiveStream? = null

    private var state: String? = null
    private var broadcastId: String? = null
    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_preview)
        extractParams()
        renderView(null)
        downloadEventData()
    }

    private fun extractParams() {
        state = intent.getStringExtra("state")
        broadcastId = intent.getStringExtra("broadcastId")
    }

    private fun renderView(data: BroadcastPreviewData?) {
        broadcast_title.text = "$state broadcast"
        broadcast_name.text = data?.name ?: ""
        broadcast_description.text = data?.description ?: ""
        broadcast_created.text = Utils.timeAgo(data?.created)
        broadcast_scheduled.text = Utils.timeAgo(data?.scheduled)
        broadcast_lifeCycleStatus.text = data?.lifeCycleStatus ?: "-"
        broadcast_streamStatus.text = data?.streamStatus ?: "-"
        thumbnail.setImageUrl(data?.thumbUri, mImageLoader)
        start_streaming.isVisible = true
        when {
            canWatchVideo -> {
                broadcast_streamStatus.setTextColor(Color.BLACK)
                start_streaming.text = "Watch video"
            }
            streamItem?.status?.streamStatus == "active" -> {
                broadcast_streamStatus.setTextColor(Color.GREEN)
                start_streaming.text = "Start Streaming"
            }
            else -> {
                start_streaming.isVisible = false
            }
        }
    }

    private fun downloadEventData() {
        if (broadcastId == null) {
            return
        }
        val progressDialog = ProgressDialog.create(this, "Downloading broadcast data...")
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            val data: BroadcastPreviewData? = LiveBroadcasts.getBroadcastPreviewData(broadcastId!!).await()
            launch(Dispatchers.Main) {
                progressDialog.dismiss()
                renderView(data)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Router.currentContext = this
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        handleActivitiesResults(requestCode, resultCode, data)
    }

    private fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Config.REQUEST_STREAMER -> if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                finalizeStreaming(data)
            }
        }
    }

    private val canWatchVideo: Boolean
        get() {
            if (broadcastItem == null) {
                return false
            }
            return broadcastItem!!.lifeCycleStatus == "complete" &&  !broadcastItem!!.watchUri.isNullOrEmpty()
        }

    fun onButtonPress(view: View) {
        when {
            broadcastItem == null -> return
            canWatchVideo -> {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(broadcastItem!!.watchUri))
                startActivity(browserIntent)
            }
            streamItem?.status?.streamStatus == "active" -> startStreaming()
            else -> return
        }
    }

    private fun startStreaming() {
        Log.d(TAG, "startStreaming")
        val broadcastId = broadcastItem!!.id
        val streamId = broadcastItem!!.streamId
        val progressDialog = ProgressDialog.create(this, R.string.startStreaming)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isBroadcastingStarted = LiveBroadcasts.transitionLiveBroadcastsToLiveAsync(broadcastId).await()
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    if (isBroadcastingStarted) startBroadcastStreaming()
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    startAuthorization(e.intent)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    val context = application.applicationContext
                    Utils.showError(context, e.localizedMessage)
                }
            }
        }
    }

    private fun startBroadcastStreaming() {
        val broadcastId = broadcastItem!!.id
        val ingestionAddress = broadcastItem!!.ingestionAddress!!
        val intent = Intent(
            applicationContext,
            VideoStreamingActivity::class.java
        )
        intent.putExtra(LiveBroadcastsInteractor.RTMP_URL_KEY, ingestionAddress)
        intent.putExtra(LiveBroadcastsInteractor.BROADCAST_ID_KEY, broadcastId)
        startActivityForResult(intent, Config.REQUEST_STREAMER)
    }

    /**
     * Transit broadcast to completed state
     */
    private fun finalizeStreaming(intent: Intent) {
        val broadcastId = intent.getStringExtra(LiveBroadcastsInteractor.BROADCAST_ID_KEY)
        val progressDialog = ProgressDialog.create(this, R.string.startStreaming)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                LiveBroadcasts.transitionLiveBroadcastsToCompletedAsync(broadcastId).await()
                Log.d(TAG, "The Broadcast is finished")
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    startAuthorization(e.intent)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    val context = application.applicationContext
                    Utils.showError(context, e.localizedMessage)
                }
            }
        }
    }

    private fun startAuthorization(intent: Intent) {
        startActivityForResult(intent, Config.REQUEST_AUTHORIZATION)
    }

    companion object {
        private val TAG = BroadcastPreview::class.java.name
    }
}