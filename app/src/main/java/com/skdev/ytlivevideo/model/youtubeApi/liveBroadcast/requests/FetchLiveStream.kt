package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.util.Log
import com.google.api.services.youtube.model.LiveStream
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveStreamingInteractor
import kotlinx.coroutines.*
import java.io.IOException

object FetchLiveStream {
     suspend fun runAsync(streamId: String?) : LiveStream? =
        withContext(Dispatchers.IO) {
            try {
                if (streamId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                val list = LiveStreamingInteractor.getLiveStreamsListItem(streamId)
                Log.d(TAG, list.toString())
                return@withContext list
            } catch (e: IOException) {
                Log.e(TAG, "Failed fetch live stream:", e)
                val message = e.cause?.message ?: "Error while fetching live stream $streamId"
                throw IOException(message)
            }
        }

    private val TAG: String = FetchBroadcasts::class.java.name
}
