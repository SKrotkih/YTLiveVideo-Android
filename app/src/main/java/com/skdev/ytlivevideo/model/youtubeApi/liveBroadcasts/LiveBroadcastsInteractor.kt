/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts

import android.util.Log
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.youtubeApi.liveStreams.LiveStreamsInteractor
import com.skdev.ytlivevideo.util.Config
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object LiveBroadcastsInteractor {
    const val TAG = "BroadcastIterator"
    const val RTMP_URL_KEY = "rtmpUrl"
    const val BROADCAST_ID_KEY = "broadcastId"
    private const val FUTURE_DATE_OFFSET_MILLIS = 5 * 1000

    private val youtube: YouTube
        get() {
            val transport: HttpTransport = NetHttpTransport()
            val jsonFactory: JsonFactory = GsonFactory()
            return YouTube.Builder(transport, jsonFactory, GoogleAccountManager.credential!!)
                .setApplicationName(Config.APP_NAME)
                .build()
        }

    fun createNewBroadcast(description: String?, name: String?) : String? {
        try {
            val liveBroadcast = liveBroadcastInsert(name, description)
            val liveStream = LiveStreamsInteractor.liveStreamInsert(name)
            // Create the bind request
            val liveBroadcastBind = youtube
                .liveBroadcasts()
                .bind(liveBroadcast.id,"id,contentDetails")
            // Set stream id to bind
            liveBroadcastBind.streamId = liveStream.id
            // Request is executed and bound broadcast is returned
            Log.d(TAG, "The request to create a new broadcast has been sent")
            liveBroadcastBind.execute()
            return liveBroadcast.id
        } catch (e: GoogleJsonResponseException) {
            System.err.println(
                "GoogleJsonResponseException code: "
                        + e.details.code + " : "
                        + e.details.message
            )
            e.printStackTrace()
        } catch (e: IOException) {
            System.err.println("IOException: " + e.message)
            e.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Throwable: " + t.stackTrace)
            t.printStackTrace()
        }
        return null
    }

    private fun liveBroadcastInsert(name: String?, description: String?) : LiveBroadcast {
        // We need a date that's in the proper ISO format and is in the future,
        // since the API won't
        // create events that start in the past.
        val dateFormat = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val futureDateMillis = (System.currentTimeMillis()
                + FUTURE_DATE_OFFSET_MILLIS)
        val futureDate = Date()
        futureDate.time = futureDateMillis
        val date = dateFormat.format(futureDate)

        Log.d(Config.APP_NAME, String.format("Creating event: name='%s', description='%s', date='%s'.", name, description, date))

        val broadcastSnippet = LiveBroadcastSnippet()
        broadcastSnippet.title = name
        broadcastSnippet.scheduledStartTime = DateTime(futureDate)
        val contentDetails = LiveBroadcastContentDetails()
        val monitorStream = MonitorStreamInfo()
        monitorStream.enableMonitorStream = false
        contentDetails.monitorStream = monitorStream

        // Create LiveBroadcastStatus with privacy status.
        val status = LiveBroadcastStatus()
        status.privacyStatus = "unlisted"
        val broadcast = LiveBroadcast()
        broadcast.kind = "youtube#liveBroadcast"
        broadcast.snippet = broadcastSnippet
        broadcast.status = status
        broadcast.contentDetails = contentDetails

        // Create the insert request
        val liveBroadcastInsert = youtube
            .liveBroadcasts()
            .insert(
                "id,snippet,contentDetails,status",
                broadcast
            )

        // Request is executed and inserted broadcast is returned
        return liveBroadcastInsert.execute()
    }

    fun getLiveBroadcastsList(state: BroadcastState?, broadcastId: String?): List<LiveBroadcastItem> {
        Log.d(Config.APP_NAME, "Requesting live events.")
        val liveBroadcastRequest = youtube.liveBroadcasts().list("id,snippet,contentDetails,status")
        //liveBroadcastRequest.setMine(true);
        if (state != null) liveBroadcastRequest.broadcastStatus = state.value()
        if (broadcastId != null) liveBroadcastRequest.id = broadcastId
        try {
            // List request is executed and list of broadcasts are returned
            val returnedListResponse = liveBroadcastRequest.execute()
            // Get the list of broadcasts associated with the user.
            val returnedList = returnedListResponse.items
            val resultList: MutableList<LiveBroadcastItem> = ArrayList(returnedList.size)
            var liveBroadcastItem: LiveBroadcastItem
            for (broadcast in returnedList) {
                liveBroadcastItem = LiveBroadcastItem()
                liveBroadcastItem.event = broadcast
                val streamId = broadcast.contentDetails.boundStreamId
                if (streamId != null) {
                    val ingestionAddress = LiveStreamsInteractor.getLiveStreamingIngestionAddress(streamId)
                    liveBroadcastItem.ingestionAddress = ingestionAddress
                }
                resultList.add(liveBroadcastItem)
            }
            return resultList
        } catch (e: IOException) {
            Log.e(Config.APP_NAME, "Failed getting broadcasts list (see 'Caused by'):", e)
            throw e
        }
    }

    fun deleteBroadcast(broadcastId: String?) {
        youtube.liveBroadcasts().delete(broadcastId)
    }

    /**
     * Transition State methods
     */
     fun transitionLiveBroadcastsToLive(broadcastId: String?) {
        try {
            transitionToStatus("live", broadcastId)
        } catch (e: IOException) {
            Log.d(TAG, "Trying to transition on Test state because error ${e.localizedMessage}")
            transitionLiveBroadcastsToTesting(broadcastId)
        }
    }

    private fun transitionLiveBroadcastsToTesting(broadcastId: String?) {
        transitionToStatus("testing", broadcastId)
    }

    fun transitionLiveBroadcastsToCompleted(broadcastId: String?) {
        transitionToStatus("completed", broadcastId)
    }

    private fun transitionToStatus(status: String, broadcastId: String?) {
        Log.d(TAG, "Transition broadcast $broadcastId to the $status status")
        youtube.liveBroadcasts().transition(status, broadcastId, "status").execute()
    }

}