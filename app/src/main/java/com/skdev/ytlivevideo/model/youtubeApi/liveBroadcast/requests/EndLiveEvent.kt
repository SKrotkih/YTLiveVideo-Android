package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.MainActivity
import com.skdev.ytlivevideo.util.ProgressDialog
import java.io.IOException

class EndLiveEvent(val context: Activity,
                   private val credential: GoogleAccountCredential?,
                   private val broadcastId: String?,
                   private val callback: LiveEventTaskCallback) : AsyncTask<Void?, Void?, List<LiveBroadcastItem>?>() {

    private var progressDialog: Dialog? = null
    private val transport: HttpTransport = NetHttpTransport()
    private val jsonFactory: JsonFactory = GsonFactory()

    override fun onPreExecute() {
        Log.d(TAG, "Start")
        progressDialog = ProgressDialog.create(context, R.string.endingEvent)
        progressDialog?.show()
    }

    override fun doInBackground(vararg params: Void?): List<LiveBroadcastItem>? {
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(MainActivity.APP_NAME)
            .build()
        try {
            if (!broadcastId.isNullOrBlank()) {
                YouTubeLiveBroadcastRequest.endEvent(youtube, broadcastId)
            }
        } catch (e: UserRecoverableAuthIOException) {
            callback.onAuthException(e)
        } catch (e: IOException) {
            Log.e(TAG, "", e)
        }
        return null
    }

    override fun onPostExecute(fetchedLiveBroadcastItems: List<LiveBroadcastItem>?) {
        Log.d(TAG, "Finish")
        progressDialog?.dismiss()
    }

    companion object {
        const val TAG = "EndLiveEventTask"
    }
}
