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
import kotlinx.coroutines.*
import java.io.IOException

object CreateLiveEvent  {

    @Throws(IOException::class)
    fun runAsync(context: Activity, credential: GoogleAccountCredential, name: String, description: String) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                createLiveEventRequest(credential, name, description)
            } catch (e: IOException) {
                Log.e(TAG, "Error while creating a new event request:", e)
                val message = e.cause?.message ?: "Error while creating a new event request"
                throw IOException(message)
            }
        }

    private fun createLiveEventRequest(credential: GoogleAccountCredential, name: String, description: String) {
        Log.d(TAG, "createLiveEventRequest")
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(Config.APP_NAME)
            .build()
        YouTubeLiveBroadcastRequest.createLiveEvent(youtube, description, name)
    }

    private val TAG: String = CreateLiveEvent::class.java.name
}

/**
 * https://developers.google.com/youtube/v3/live/docs/liveBroadcasts/insert
 */
