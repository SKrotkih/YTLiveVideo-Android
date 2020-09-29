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
import java.lang.Thread.sleep

object StartLiveEvent {

    fun runAsync(credential: GoogleAccountCredential, streamId: String?, broadcastId: String?) : Deferred<Boolean> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                if (streamId.isNullOrBlank() || broadcastId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                // delay(10000)
                val result = checkAndStartStreaming(credential, streamId, broadcastId)
                return@async result
            } catch (e: IOException) {
                Log.e(TAG, "Failed start broadcasting:", e)
                val message = e.localizedMessage
                throw IOException(message)
            }
        }

    private fun checkAndStartStreaming(credential: GoogleAccountCredential, streamId: String, broadcastId: String) : Boolean {
        Log.d(TAG, "startLiveEvent")
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(Config.APP_NAME)
            .build()
        val livestreamRequest = youtube.liveStreams().list("status")
        livestreamRequest.id = streamId
        val returnedListResponse = livestreamRequest.execute()
        val returnedList = returnedListResponse.items
        if (returnedList.size == 1) {
            val stream = returnedList[0]
            Log.v(TAG, "the current stream status is : " + stream.status.streamStatus)
            if (stream.status.streamStatus == "active") {
                Log.v(TAG, "start broadcasting now")
                YouTubeLiveBroadcastRequest.transitionToLiveState(youtube, broadcastId)
                return true
            } else {
                throw IOException("The Stream is not in Active state. It's in ${stream.status.streamStatus} state.")
            }
        }
        return false
    }

    private val TAG: String = StartLiveEvent::class.java.name
}

/**
 *  API Request LiveBroadcasts: transition
 *  https://developers.google.com/youtube/v3/live/docs/liveBroadcasts/transition
 *
403 Forbidden
POST https://www.googleapis.com/youtube/v3/liveBroadcasts/transition?broadcastStatus=live&id=8bqMrWvCgqg&part=status
{
"code": 403,
"errors": [
{
"domain": "youtube.liveBroadcast",
"message": "Stream is inactive",
"reason": "errorStreamInactive",
"extendedHelp": "https://developers.google.com/youtube/v3/live/docs/liveBroadcasts/transition"
}
],
"message": "Stream is inactive"
}

https://www.googleapis.com/youtube/v3/liveBroadcasts?part=status&broadcastStatus=all&broadcastType=all&key=[YOUR_API_KEY]

"items": [
{
"kind": "youtube#liveBroadcast",
"etag": "R6Hc0SWbiE-XWRNqFTw1O5VB6XY",
"id": "Am_kib7rYzc",
"status": {
"lifeCycleStatus": "live",     // "ready", "complete"
"privacyStatus": "unlisted",
"recordingStatus": "recording",
"madeForKids": false,
"selfDeclaredMadeForKids": false
}
},


POST https://www.googleapis.com/youtube/v3/liveBroadcasts/transition?broadcastStatus=live&id=AKkkPpmWwpY&part=status
{
"code": 403,
"errors": [
{
"domain": "youtube.liveBroadcast",
"message": "Invalid transition",
"reason": "invalidTransition",
"extendedHelp": "https://developers.google.com/youtube/v3/live/docs/liveBroadcasts/transition#params"
}
],
"message": "Invalid transition"
}

 */
