package com.skdev.ytlivevideo.ui.broadcastPreview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.toolbox.ImageLoader
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.EndLiveEvent
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.FetchAllLiveEvents
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.StartLiveEvent
import com.skdev.ytlivevideo.ui.mainScene.fragment.FragmentDelegate
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import com.skdev.ytlivevideo.ui.router.Router
import com.skdev.ytlivevideo.ui.videoStreamingScene.VideoStreamingActivity
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import kotlinx.android.synthetic.main.activity_broadcast_preview.*
import kotlinx.android.synthetic.main.live_events_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class BroadcastPreview: AppCompatActivity() {

    private var broadcastItem: LiveBroadcastItem? = null
    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_preview)
        configureViewModel()
        val state = intent.getStringExtra("state")
        val broadcastId = intent.getStringExtra("broadcastId")
        downloadEventData(broadcastId)
        broadcast_title.text = "$state broadcast"
        renderView()
    }

    private fun renderView() {
        if (broadcastItem == null) {
            broadcast_name.text = ""
            broadcast_description.text  = ""
            broadcast_created.text = ""
            broadcast_scheduled.text = ""
        } else {
            broadcast_name.text = broadcastItem!!.title
            broadcast_description.text = broadcastItem!!.description
            broadcast_created.text = broadcastItem!!.publishedAt
            broadcast_scheduled.text = broadcastItem!!.publishedAt
            thumbnail.setImageUrl(broadcastItem!!.thumbUri, mImageLoader)
        }
    }

    private fun configureViewModel() {
        val viewModel: ViewModel by viewModels()
    }

    private fun downloadEventData(broadcastId: String?) {
        val progressDialog = ProgressDialog.create(this, "Downloading broadcast data...")
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch() {
            try {
                val list = FetchAllLiveEvents.runAsync(GoogleAccountManager.credential!!, null, broadcastId)
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    if (list!!.count() > 0) {
                        broadcastItem = list[0]
                        renderView()
                    }
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
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
            MainViewModel.REQUEST_STREAMER -> if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                finalizeStreaming(data)
            }
        }
    }

    fun onStartStreaming(view: View) {
        if (broadcastItem == null) {
            return
        }
        Log.d(TAG, "startStreaming")
        val broadcastId = broadcastItem!!.id
        val streamId = broadcastItem!!.streamId
        val progressDialog = ProgressDialog.create(this, R.string.startStreaming)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isBroadcastingStarted = StartLiveEvent.runAsync(GoogleAccountManager.credential!!, streamId, broadcastId).await()
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

    /**
     * Transit broadcast to completed state
     */
    private fun finalizeStreaming(intent: Intent) {
        val broadcastId = intent.getStringExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY)
        val progressDialog = ProgressDialog.create(this, R.string.startStreaming)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EndLiveEvent.runAsync(GoogleAccountManager.credential!!, broadcastId).await()
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

    private fun startBroadcastStreaming() {
        val broadcastId = broadcastItem!!.id
        val ingestionAddress = broadcastItem!!.ingestionAddress!!
        val intent = Intent(
            applicationContext,
            VideoStreamingActivity::class.java
        )
        intent.putExtra(YouTubeLiveBroadcastRequest.RTMP_URL_KEY, ingestionAddress)
        intent.putExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY, broadcastId)
        startActivityForResult(intent, MainViewModel.REQUEST_STREAMER)
    }

    private fun startAuthorization(intent: Intent) {
        startActivityForResult(intent, MainViewModel.REQUEST_AUTHORIZATION)
    }

    companion object {
        private val TAG = BroadcastPreview::class.java.name
    }
}