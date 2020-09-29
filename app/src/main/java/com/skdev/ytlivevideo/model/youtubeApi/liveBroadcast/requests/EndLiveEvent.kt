package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.app.Activity
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.util.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.IOException

object EndLiveEvent {

    fun runAsync(credential: GoogleAccountCredential, broadcastId: String?) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                endLiveEvent(credential, broadcastId)
            } catch (e: IOException) {
                Log.e(TAG, "Error while finishing broadcast request:", e)
                val message = e.cause?.message ?: "Error while finishing broadcast request"
                throw IOException(message)
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
            throw IllegalArgumentException("The Broadcast ID is not presented")
        } else {
            YouTubeLiveBroadcastRequest.transitionToCompletedState(youtube, broadcastId)
        }
    }

    private val TAG: String = EndLiveEvent::class.java.name
}
