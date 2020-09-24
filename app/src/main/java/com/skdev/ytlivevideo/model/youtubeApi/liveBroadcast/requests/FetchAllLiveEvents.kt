package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.app.Activity
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.util.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.IOException

object FetchAllLiveEvents {

    @Throws(IOException::class)
    fun runAsync(context: Activity, credential: GoogleAccountCredential) : Deferred<List<LiveBroadcastItem>?> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                val list = fetchAllLiveEvents(credential)
                return@async list
            } catch (e: IOException) {
                Log.e(TAG, "Error while fetching all live events request:", e)
                throw e
            }
        }

    private fun fetchAllLiveEvents(credential: GoogleAccountCredential) : List<LiveBroadcastItem>? {
        Log.d(TAG, "getLiveEventRequest")
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(Config.APP_NAME)
            .build()
        val listItems = YouTubeLiveBroadcastRequest.getLiveEvents(youtube)
        Log.d(TAG, "Current my list broadcasts: $listItems")
        return listItems
    }

    private val TAG: String = FetchAllLiveEvents::class.java.name
}
