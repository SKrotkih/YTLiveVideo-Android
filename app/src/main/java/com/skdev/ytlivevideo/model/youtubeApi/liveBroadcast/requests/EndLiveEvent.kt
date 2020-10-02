package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.util.Log
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveStreamingInteractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import java.io.IOException

object EndLiveEvent {

    fun runAsync(broadcastId: String?) : Deferred<Unit> =
        CoroutineScope(Dispatchers.IO).async() {
            try {
                if (broadcastId.isNullOrBlank()) {
                    throw IllegalArgumentException("The Broadcast ID is not presented")
                } else {
                    LiveStreamingInteractor.transitionLiveBroadcastsToCompleted(broadcastId)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error while finishing broadcast request:", e)
                val message = e.cause?.message ?: "Error while finishing broadcast request"
                throw IOException(message)
            }
        }

    private val TAG: String = EndLiveEvent::class.java.name
}
