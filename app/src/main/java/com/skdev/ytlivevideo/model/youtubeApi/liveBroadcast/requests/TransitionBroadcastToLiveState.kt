package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

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

object TransitionBroadcastToLiveState {

    fun runAsync(credential: GoogleAccountCredential, broadcastId: String?) : Deferred<Boolean> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                if (broadcastId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                // delay(10000)
                val result = transitionBroadcastToLiveState(credential, broadcastId)
                return@async result
            } catch (e: IOException) {
                Log.e(TAG, "Failed start broadcasting:", e)
                val message = e.localizedMessage
                throw IOException(message)
            }
        }

    private fun transitionBroadcastToLiveState(credential: GoogleAccountCredential, broadcastId: String) : Boolean {
        Log.d(TAG, "checkAndStartStreaming")
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(Config.APP_NAME)
            .build()
        try {
            YouTubeLiveBroadcastRequest.transitionToLiveState(youtube, broadcastId)
            return true
        } catch (e: IOException) {
            throw e
        }
    }

    private val TAG: String = TransitionBroadcastToLiveState::class.java.name
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
