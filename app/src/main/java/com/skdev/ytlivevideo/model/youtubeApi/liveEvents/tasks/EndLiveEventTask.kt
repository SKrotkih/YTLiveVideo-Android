package com.skdev.ytlivevideo.model.youtubeApi.liveEvents.tasks

import android.app.Activity
import android.app.Dialog
import android.os.AsyncTask
import android.util.Log
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.youtube.YouTube
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.youtubeApi.liveEvents.LiveEventsController
import com.skdev.ytlivevideo.model.youtubeApi.liveEvents.LiveEventsItem
import com.skdev.ytlivevideo.ui.MainActivity
import com.skdev.ytlivevideo.util.ProgressDialog
import java.io.IOException

class EndLiveEventTask(val context: Activity,
                       val broadcastId: String?,
                       val googleCredential: GoogleAccountCredential,
                       val onAuthException: () -> Void,
                       val completion: (List<LiveEventsItem>?) -> Void) : AsyncTask<Void?, Void?, List<LiveEventsItem>?>() {

    private var progressDialog: Dialog? = null
    private val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    private val jsonFactory: JsonFactory = GsonFactory()

    override fun onPreExecute() {
        Log.d(OBJ_NAME, "onPreExecute")
        progressDialog = ProgressDialog.create(context, R.string.endingEvent)
        progressDialog?.show()
    }

    override fun doInBackground(vararg params: Void?): List<LiveEventsItem>? {
        val youtube = YouTube.Builder(
            transport, jsonFactory,
            googleCredential
        ).setApplicationName(MainActivity.APP_NAME)
            .build()
        try {
            if (!broadcastId.isNullOrBlank()) {
                LiveEventsController.endEvent(youtube, broadcastId)
            }
        } catch (e: UserRecoverableAuthIOException) {
            onAuthException()
        } catch (e: IOException) {
            Log.e(OBJ_NAME, "", e)
        }
        return null
    }

    override fun onPostExecute(
        fetchedLiveEventsItems: List<LiveEventsItem>?
    ) {
        completion(fetchedLiveEventsItems)
        progressDialog?.dismiss()
    }

    companion object {
        const val OBJ_NAME = "EndLiveEventTask"
    }
}
