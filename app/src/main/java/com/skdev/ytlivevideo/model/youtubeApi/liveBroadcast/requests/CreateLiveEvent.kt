package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.util.Log
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveStreamingInteractor
import kotlinx.coroutines.*
import java.io.IOException

object CreateLiveEvent  {

    fun runAsync(name: String, description: String) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                LiveStreamingInteractor.liveBroadcastsInsert(description, name)
            } catch (e: IOException) {
                Log.e(TAG, "Error while creating a new event request:", e)
                val message = e.cause?.message ?: "Error while creating a new event request"
                throw IOException(message)
            }
        }

    private val TAG: String = CreateLiveEvent::class.java.name
}

/**
 * https://developers.google.com/youtube/v3/live/docs/liveBroadcasts/insert
 */
