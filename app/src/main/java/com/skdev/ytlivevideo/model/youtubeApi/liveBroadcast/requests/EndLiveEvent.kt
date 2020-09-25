package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.ProgressDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.IOException

object EndLiveEvent {

    @Throws(IOException::class)
    fun runAsync(context: Activity, credential: GoogleAccountCredential, broadcastId: String?) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                endLiveEvent(credential, broadcastId)
            } catch (e: IOException) {
                Log.e(TAG, "Error while finishing broadcast request:", e)
                throw e
            }
        }

    private fun endLiveEvent(credential: GoogleAccountCredential, broadcastId: String?) {
        Log.d(TAG, "endLiveEvent")
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(Config.APP_NAME)
            .build()
        if (broadcastId.isNullOrBlank()) {
            throw IOException("The Broadcast ID is not presented")
        } else {
            YouTubeLiveBroadcastRequest.endEvent(youtube, broadcastId)
        }
    }

    private val TAG: String = EndLiveEvent::class.java.name
}
