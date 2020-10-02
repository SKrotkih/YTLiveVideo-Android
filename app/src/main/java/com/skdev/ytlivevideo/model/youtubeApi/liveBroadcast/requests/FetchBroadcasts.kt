package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.util.Log
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveStreamingInteractor
import kotlinx.coroutines.*
import java.io.IOException

object FetchBroadcasts {

     suspend fun runAsync(state: BroadcastState?, broadcastId: String? = null) : List<LiveBroadcastItem>? =
        withContext(Dispatchers.IO) {
            try {
                val list = LiveStreamingInteractor.getLiveBroadcastsList(state, broadcastId)
                list?.forEach{it.state = state}
                Log.d(TAG, list.toString())
                return@withContext list
            } catch (e: IOException) {
                Log.e(TAG, "Failed fetch all live events:", e)
                val message = e.cause?.message ?: "Error while fetching live events with '${state.toString()}' state"
                throw IOException(message)
            }
        }

    private val TAG: String = FetchBroadcasts::class.java.name
}
