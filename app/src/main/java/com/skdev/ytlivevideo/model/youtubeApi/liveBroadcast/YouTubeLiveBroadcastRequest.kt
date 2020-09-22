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
package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast

import android.util.Log
import com.skdev.ytlivevideo.ui.MainActivity
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.util.DateTime
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object YouTubeLiveBroadcastRequest {
    const val RTMP_URL_KEY = "rtmpUrl"
    const val BROADCAST_ID_KEY = "broadcastId"
    private const val FUTURE_DATE_OFFSET_MILLIS = 5 * 1000

    fun createLiveEvent(youtube: YouTube, description: String?, name: String?) {
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
        Log.i(
            MainActivity.APP_NAME, String.format(
                "Creating event: name='%s', description='%s', date='%s'.",
                name, description, date
            )
        )
        try {
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
                    "snippet,status,contentDetails",
                    broadcast
                )

            // Request is executed and inserted broadcast is returned
            val returnedBroadcast = liveBroadcastInsert.execute()

            // Create a snippet with title.
            val streamSnippet = LiveStreamSnippet()
            streamSnippet.title = name

            // Create content distribution network with format and ingestion
            // type.
            val cdn = CdnSettings()
            cdn.format = "240p"
            cdn.ingestionType = "rtmp"
            val stream = LiveStream()
            stream.kind = "youtube#liveStream"
            stream.snippet = streamSnippet
            stream.cdn = cdn

            // Create the insert request
            val liveStreamInsert = youtube
                .liveStreams()
                .insert("snippet,cdn", stream)

            // Request is executed and inserted stream is returned
            val returnedStream = liveStreamInsert.execute()

            // Create the bind request
            val liveBroadcastBind = youtube
                .liveBroadcasts()
                .bind(
                    returnedBroadcast.id,
                    "id,contentDetails"
                )

            // Set stream id to bind
            liveBroadcastBind.streamId = returnedStream.id

            // Request is executed and bound broadcast is returned
            liveBroadcastBind.execute()
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
    }

    // TODO: Catch those exceptions and handle them here.
    @Throws(IOException::class)
    fun getLiveEvents(youtube: YouTube): List<LiveBroadcastItem> {
        Log.i(MainActivity.APP_NAME, "Requesting live events.")
        val liveBroadcastRequest = youtube
            .liveBroadcasts().list("id,snippet,contentDetails")
        // liveBroadcastRequest.setMine(true);
        liveBroadcastRequest.broadcastStatus = "upcoming"

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
                val ingestionAddress = getIngestionAddress(youtube, streamId)
                liveBroadcastItem.ingestionAddress = ingestionAddress
            }
            resultList.add(liveBroadcastItem)
        }
        return resultList
    }

    @Throws(IOException::class)
    fun startEvent(youtube: YouTube, broadcastId: String?) {
        try {
            Thread.sleep(10000)
        } catch (e: InterruptedException) {
            Log.e(MainActivity.APP_NAME, "", e)
        }
        val transitionRequest = youtube.liveBroadcasts().transition(
            "live", broadcastId, "status"
        )
        transitionRequest.execute()
    }

    @Throws(IOException::class)
    fun endEvent(youtube: YouTube, broadcastId: String?) {
        val transitionRequest = youtube.liveBroadcasts().transition(
            "completed", broadcastId, "status"
        )
        transitionRequest.execute()
    }

    @Throws(IOException::class)
    fun getIngestionAddress(youtube: YouTube, streamId: String?): String {
        val liveStreamRequest = youtube
            .liveStreams()
            .list("cdn")
        liveStreamRequest.id = streamId
        val returnedStream = liveStreamRequest.execute()
        val streamList = returnedStream.items
        if (streamList.isEmpty()) {
            return ""
        }
        val ingestionInfo = streamList[0].cdn.ingestionInfo
        return (ingestionInfo.ingestionAddress + "/"
                + ingestionInfo.streamName)
    }
}