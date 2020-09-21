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

class StartLiveEventTask(val context: Activity,
                         private val googleCredential: GoogleAccountCredential?,
                         private val broadcastId: String?,
                         private val listener: LiveEventTaskCallback) : AsyncTask<Void?, Void?, List<LiveEventsItem>?>() {

    private var progressDialog: Dialog? = null
    private val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    private val jsonFactory: JsonFactory = GsonFactory()

    override fun onPreExecute() {
        Log.d(OBJ_NAME, "StartLiveEventTask")
        progressDialog = ProgressDialog.create(context, R.string.startingEvent)
        progressDialog?.show()
    }

    override fun doInBackground(vararg params: Void?): List<LiveEventsItem>? {
        val youtube = YouTube.Builder(
            transport,
            jsonFactory,
            googleCredential
        ).setApplicationName(MainActivity.APP_NAME)
            .build()
        try {
            LiveEventsController.startEvent(youtube, broadcastId)
        } catch (e: UserRecoverableAuthIOException) {
            listener.onAuthException(e)
        } catch (e: IOException) {
            Log.e(OBJ_NAME, "Unexpected Exception", e)
        }
        return null
    }

    override fun onPostExecute(
        fetchedLiveEventsItems: List<LiveEventsItem>?
    ) {
        progressDialog?.dismiss()
    }

    companion object {
        const val OBJ_NAME = "StartLiveEventTask"
    }
}
