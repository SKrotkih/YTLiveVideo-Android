package com.skdev.ytlivevideo.model.youtubeApi.liveStreams

import android.util.Log
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.*
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.util.Config
import java.io.IOException

object LiveStreamsInteractor {

    private fun buildYoutube() : YouTube {
        val transport: HttpTransport = NetHttpTransport()
        val jsonFactory: JsonFactory = GsonFactory()
        return YouTube.Builder(transport, jsonFactory, GoogleAccountManager.credential!!)
            .setApplicationName(Config.APP_NAME)
            .build()
    }

    fun getLiveStreamingIngestionAddress(streamId: String?): String {
        val youtube = buildYoutube()
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

    fun getLiveStreamsListItem(streamId: String): LiveStream? {
        Log.d(Config.APP_NAME, "Requesting stream $streamId...")
        val youtube = buildYoutube()
        val livestreamRequest = youtube.liveStreams().list("status")
        livestreamRequest.id = streamId
        try {
            val liveStreamsResponse = livestreamRequest.execute()
            val liveStreams = liveStreamsResponse.items
            if (liveStreams.size == 1) {
                val stream = liveStreams[0]
                return stream
            }
        } catch (e: IOException) {
            Log.e(Config.APP_NAME, "Failed getting live streams list (see 'Caused by'):", e)
            throw e
        }
        return null
    }
}
