package com.skdev.ytlivevideo.model.youtubeApi.liveStreams

import android.util.Log
import com.google.api.services.youtube.model.LiveStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object LiveStreams {

    suspend fun getLiveStreamsListItemAsync(streamId: String?) : LiveStream? =
        withContext(Dispatchers.IO) {
            try {
                if (streamId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Stream ID is not presented")
                }
                val list = LiveStreamsInteractor.getLiveStreamsListItem(streamId)
                Log.d(TAG, list.toString())
                return@withContext list
            } catch (e: IOException) {
                Log.e(TAG, "Failed fetch live stream:", e)
                val message = e.cause?.message ?: "Error while fetching live stream $streamId"
                throw IOException(message)
            }
        }

    private val TAG: String = LiveStreams::class.java.name
}