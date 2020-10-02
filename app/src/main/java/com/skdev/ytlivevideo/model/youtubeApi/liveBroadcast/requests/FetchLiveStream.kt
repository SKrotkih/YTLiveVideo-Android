package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.LiveStream
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.util.Config
import kotlinx.coroutines.*
import java.io.IOException

object FetchLiveStream {
     suspend fun runAsync(credential: GoogleAccountCredential, streamId: String?) : LiveStream? =
        withContext(Dispatchers.IO) {
            try {
                if (streamId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                val list = fetchLiveStream(credential, streamId)
                Log.d(TAG, list.toString())
                return@withContext list
            } catch (e: IOException) {
                Log.e(TAG, "Failed fetch live stream:", e)
                val message = e.cause?.message ?: "Error while fetching live stream $streamId"
                throw IOException(message)
            }
        }

    private fun fetchLiveStream(credential: GoogleAccountCredential, streamId: String) : LiveStream? {
        Log.d(TAG, "fetchLiveStream")
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(Config.APP_NAME)
            .build()
        val liveStream = YouTubeLiveBroadcastRequest.getLiveStream(youtube, streamId)
        Log.d(TAG, "Stream (ID=$streamId) = $liveStream")
        return liveStream
    }

    private val TAG: String = FetchBroadcasts::class.java.name
}
