package com.skdev.ytlivevideo.ui.createBroadcast

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.requests.LiveBroadcasts
import com.skdev.ytlivevideo.ui.liveStream.YouTubeStreamLauncher
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import kotlinx.android.synthetic.main.activity_create_broadcast.*
import kotlinx.coroutines.*
import java.io.IOException

class CreateNewBroadcast: AppCompatActivity() {

    private var job: Job? = null

    private val streamLauncher = YouTubeStreamLauncher()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_broadcast)

        create_broadcast.text = "Start Live Streaming"
    }

    fun onCreateBroadcast(view: View) {
        startYouTubeLive()
    }

    /**
     * Create Live Stream with using YouTube App
     */
    private fun startYouTubeLive() {
        val description = broadcast_description.text.toString().trim()
        if (description.isEmpty()) {
            Utils.showError(this, "Please enter new broadcast' description")
            return
        }
        streamLauncher.start(description)
    }

    /**
     * Create Broadcast with using YouTube API
     */
    private fun createBroadcastOnMyAccount() {
        val name = broadcast_name.text.toString().trim()
        val description = broadcast_description.text.toString().trim()
        if (name.isEmpty() || description.isEmpty()) {
            Utils.showError(this, "Please enter new broadcast' name and description")
            return
        }
        val progressDialog = ProgressDialog.create(this, R.string.creatingEvent)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            createNewBroadcast(name, description)
            launch(Dispatchers.Main) {
                progressDialog.dismiss()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }

    /**
     * Create a new Broadcast
     */
    private suspend fun createNewBroadcast(name: String, description: String) {
        Log.d(TAG, "createNewBroadcast")
        withContext(Dispatchers.IO) {
            try {
                LiveBroadcasts.createNewBroadcastAsync(name, description).await()
            } catch (e: UserRecoverableAuthIOException) {
                // In this cases we are using transfer to the auth screen
                val context = application.applicationContext
                Utils.showError(context, e.localizedMessage)
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    val context = application.applicationContext
                    Utils.showError(context, e.localizedMessage)
                }
            }
        }
    }

    companion object {
        private val TAG = CreateNewBroadcast::class.java.name
    }
}
