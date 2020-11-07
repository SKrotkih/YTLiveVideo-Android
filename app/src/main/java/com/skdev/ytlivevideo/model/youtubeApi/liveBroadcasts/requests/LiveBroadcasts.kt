package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.requests

import android.util.Log
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.BroadcastPreviewData
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.BroadcastState
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastsInteractor
import com.skdev.ytlivevideo.model.youtubeApi.liveStreams.LiveStreams
import kotlinx.coroutines.*
import java.io.IOException

object LiveBroadcasts  {

    fun createNewBroadcastAsync(name: String, description: String) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async {
            try {
                val broadcastId = LiveBroadcastsInteractor.createNewBroadcast(description, name)

                delay(5000)
                val data: BroadcastPreviewData? = getBroadcastPreviewData(broadcastId!!)
                Log.d(TAG, "The new stream has '${data?.streamStatus ?: "-"}' status")

            } catch (e: IOException) {
                Log.e(TAG, "Error while creating a new event request:", e)
                val message = e.cause?.message ?: "Error while creating a new event request"
                throw IOException(message)
            }
        }

    suspend fun getBroadcastPreviewData(broadcastId: String) : BroadcastPreviewData? =
        CoroutineScope(Dispatchers.IO).async {
            val list = getLiveBroadcastsAsync(null, broadcastId)
            if (list.count() > 0) {
                val broadcast = list[0]
                val streamId = broadcast.streamId
                if (streamId == null) {
                    return@async null
                } else {
                    val stream = LiveStreams.getLiveStreamsListItemAsync(streamId)
                    return@async BroadcastPreviewData(
                        broadcastId,
                        streamId,
                        broadcast.title,
                        broadcast.description,
                        broadcast.publishedAt,
                        broadcast.publishedAt,
                        broadcast.lifeCycleStatus,
                        stream?.status?.streamStatus ?: "-",
                        broadcast.thumbUri,
                        broadcast.watchUri,
                        broadcast.ingestionAddress ?: ""
                    )
                }
            } else {
                return@async null
            }
        }.await()

    suspend fun getLiveBroadcastsAsync(state: BroadcastState?, broadcastId: String? = null) : List<LiveBroadcastItem> =
        withContext(Dispatchers.IO) {
            try {
                val list = LiveBroadcastsInteractor.getLiveBroadcastsList(state, broadcastId)
                list.forEach{it.state = state}
                Log.d(TAG, list.toString())
                return@withContext list
            } catch (e: IOException) {
                Log.e(TAG, "Failed fetch all live events:", e)
                val message = e.cause?.message ?: "Error while fetching live events with '${state.toString()}' state"
                throw IOException(message)
            }
        }

    fun deleteBroadcasts(broadcastIDs: List<String>): Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async {
            try {
                broadcastIDs.forEach {
                    LiveBroadcastsInteractor.deleteBroadcast(it)
                }
                return@async
            } catch (e: IOException) {
                Log.e(TAG, "Error while deleting broadcasts:", e)
                val message = e.cause?.message ?: "Error while deleting broadcasts"
                throw IOException(message)
            }
        }

    /**
     * Transitions
     */

    /**
     * Transit to Live status
     */
    fun transitionLiveBroadcastsToLiveAsync(broadcastId: String?) : Deferred<Boolean> =
        CoroutineScope(Dispatchers.IO).async {
            try {
                if (broadcastId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                // delay(10000)
                LiveBroadcastsInteractor.transitionLiveBroadcastsToLive(broadcastId)
                return@async true
            } catch (e: IOException) {
                Log.e(TAG, "Failed start broadcasting:", e)
                val message = e.localizedMessage
                throw IOException(message)
            }
        }

    /**
     * Transit to Completed status
     */
    fun transitionLiveBroadcastsToCompletedAsync(broadcastId: String?) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async {
            try {
                if (broadcastId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Broadcast ID is not presented")
                } else {
                    LiveBroadcastsInteractor.transitionLiveBroadcastsToCompleted(broadcastId)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error while finishing broadcast request:", e)
                val message = e.cause?.message ?: "Error while finishing broadcast request"
                throw IOException(message)
            }
        }

    private val TAG: String = LiveBroadcasts::class.java.name
}

/**
 * https://developers.google.com/youtube/v3/live/docs/liveBroadcasts/insert
 */

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


