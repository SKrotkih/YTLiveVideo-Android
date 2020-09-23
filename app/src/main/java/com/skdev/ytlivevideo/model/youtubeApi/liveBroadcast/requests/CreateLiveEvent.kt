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
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.util.ProgressDialog
import java.io.IOException
import java.util.*

class CreateLiveEvent(val context: Activity,
                      private val credential: GoogleAccountCredential?,
                      private val callback: LiveBroadcastApiInterface) : AsyncTask<Void?, Void?, List<LiveBroadcastItem>?>() {

    private var progressDialog: Dialog? = null
    private val transport: HttpTransport = NetHttpTransport()
    private val jsonFactory: JsonFactory = GsonFactory()

    override fun onPreExecute() {
        Log.d(TAG, "Start")
        progressDialog = ProgressDialog.create(context, R.string.creatingEvent)
        progressDialog?.show()
    }

    override fun doInBackground(vararg params: Void?): List<LiveBroadcastItem>? {
        val youtube = YouTube.Builder(transport, jsonFactory, credential)
            .setApplicationName(MainActivity.APP_NAME)
            .build()
        try {
            val date = Date().toString()
            val description = "Event - $date"
            val name = "A live streaming event - $date"
            YouTubeLiveBroadcastRequest.createLiveEvent(youtube, description, name)
            return YouTubeLiveBroadcastRequest.getLiveEvents(youtube)
        } catch (e: UserRecoverableAuthIOException) {
            callback.onAuthException(e)
        } catch (e: IOException) {
            Log.e(TAG, "Unexpected Exception", e)
        }
        return null
    }

    override fun onPostExecute(fetchedLiveBroadcastItems: List<LiveBroadcastItem>?) {
        Log.d(TAG, "Finish")
        progressDialog?.dismiss()
    }

    companion object {
        val TAG: String = CreateLiveEvent::class.java.name
    }
}
