package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.util.Log
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveStreamingInteractor
import kotlinx.coroutines.*
import java.io.IOException

object TransitionBroadcastToLiveState {

    fun runAsync(broadcastId: String?) : Deferred<Boolean> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                if (broadcastId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                // delay(10000)
                LiveStreamingInteractor.transitionLiveBroadcastsToLive(broadcastId)
                return@async true
            } catch (e: IOException) {
                Log.e(TAG, "Failed start broadcasting:", e)
                val message = e.localizedMessage
                throw IOException(message)
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
