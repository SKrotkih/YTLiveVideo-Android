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
package com.skdev.ytlivevideo

import android.accounts.AccountManager
import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import com.android.volley.toolbox.ImageLoader
import com.skdev.ytlivevideo.util.EventData
import com.skdev.ytlivevideo.util.NetworkSingleton
import com.skdev.ytlivevideo.util.Utils
import com.skdev.ytlivevideo.util.YouTubeApi
import com.skdev.ytlivevideo.EventsListFragment.Callbacks
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import java.io.IOException
import java.util.*


/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Main activity class which handles authorization and intents.
 */
class MainActivity : Activity(), Callbacks {
    val transport: HttpTransport = AndroidHttp.newCompatibleTransport()
    val jsonFactory: JsonFactory = GsonFactory()
    var credential: GoogleAccountCredential? = null
    private var mChosenAccountName: String? = null
    private var mImageLoader: ImageLoader? = null
    private var mEventsListFragment: EventsListFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureLoader()
        credential = GoogleAccountCredential.usingOAuth2(applicationContext, Utils.SCOPES.toList())
        // set exponential backoff policy
        credential!!.backOff = ExponentialBackOff()
        if (savedInstanceState != null) {
            mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY)
        } else {
            loadAccount()
        }
        credential!!.selectedAccountName = mChosenAccountName
        mEventsListFragment = fragmentManager
            .findFragmentById(R.id.list_fragment) as EventsListFragment
    }

    private fun startStreaming(event: EventData) {
        val broadcastId: String = event.id
        StartEventTask().execute(broadcastId)
        val intent = Intent(
            applicationContext,
            StreamerActivity::class.java
        )
        intent.putExtra(YouTubeApi.RTMP_URL_KEY, event.ingestionAddress)
        intent.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId)
        startActivityForResult(intent, REQUEST_STREAMER)
    }

    private val liveEvents: Unit
        private get() {
            if (mChosenAccountName == null) {
                return
            }
            GetLiveEventsTask().execute()
        }

    fun createEvent(view: View?) {
        CreateLiveEventTask().execute()
    }

    private fun ensureLoader() {
        if (mImageLoader == null) {
            // Get the ImageLoader through your singleton class.
            mImageLoader = NetworkSingleton.getInstance(this)?.imageLoader
        }
    }

    private fun loadAccount() {
        val sp = PreferenceManager
            .getDefaultSharedPreferences(this)
        mChosenAccountName = sp.getString(ACCOUNT_KEY, null)
        invalidateOptionsMenu()
    }

    private fun saveAccount() {
        val sp = PreferenceManager
            .getDefaultSharedPreferences(this)
        sp.edit().putString(ACCOUNT_KEY, mChosenAccountName).apply()
    }

    private fun loadData() {
        if (mChosenAccountName == null) {
            return
        }
        liveEvents
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> loadData()
            R.id.menu_accounts -> {
                chooseAccount()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_GMS_ERROR_DIALOG -> {
            }
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == RESULT_OK) {
                haveGooglePlayServices()
            } else {
                checkGooglePlayServicesAvailable()
            }
            REQUEST_AUTHORIZATION -> if (resultCode != RESULT_OK) {
                chooseAccount()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == RESULT_OK && data != null && data.extras != null) {
                val accountName = data.extras!!.getString(
                    AccountManager.KEY_ACCOUNT_NAME
                )
                if (accountName != null) {
                    mChosenAccountName = accountName
                    credential!!.selectedAccountName = accountName
                    saveAccount()
                }
            }
            REQUEST_STREAMER -> if (resultCode == RESULT_OK && data != null && data.extras != null) {
                val broadcastId = data.getStringExtra(YouTubeApi.BROADCAST_ID_KEY)
                if (broadcastId != null) {
                    EndEventTask().execute(broadcastId)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(ACCOUNT_KEY, mChosenAccountName)
    }

    override fun onConnected(connectedAccountName: String?) {
        // Make API requests only when the user has successfully signed in.
        loadData()
    }

    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        runOnUiThread {
            val dialog: Dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode, this@MainActivity,
                REQUEST_GOOGLE_PLAY_SERVICES
            )
            dialog.show()
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private fun checkGooglePlayServicesAvailable(): Boolean {
        val connectionStatusCode: Int = GooglePlayServicesUtil
            .isGooglePlayServicesAvailable(this)
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
            return false
        }
        return true
    }

    private fun haveGooglePlayServices() {
        // check if there is already an account selected
        if (credential?.selectedAccountName == null) {
            // ask user to choose account
            chooseAccount()
        }
    }

    private fun chooseAccount() {
        startActivityForResult(
            credential!!.newChooseAccountIntent(),
            REQUEST_ACCOUNT_PICKER
        )
    }

    override fun onBackPressed() {

    }

    override fun onGetImageLoader(): ImageLoader? {
        ensureLoader()
        return mImageLoader
    }

    override fun onEventSelected(liveBroadcast: EventData?) {
        if (liveBroadcast != null) {
            startStreaming(liveBroadcast)
        }
    }

    private inner class GetLiveEventsTask : AsyncTask<Void?, Void?, List<EventData>?>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog.show(
                this@MainActivity, null,
                resources.getText(R.string.loadingEvents), true
            )
        }

        override fun doInBackground(vararg params: Void?): List<EventData>? {
            val youtube = YouTube.Builder(
                transport, jsonFactory,
                credential
            ).setApplicationName(APP_NAME)
                .build()
            try {
                return YouTubeApi.getLiveEvents(youtube)
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: IOException) {
                Log.e(APP_NAME, "", e)
            }
            return null
        }

        override fun onPostExecute(
            fetchedEvents: List<EventData>?
        ) {
            if (fetchedEvents == null) {
                progressDialog!!.dismiss()
                return
            }
            mEventsListFragment?.setEvents(fetchedEvents)
            progressDialog!!.dismiss()
        }
    }

    private inner class CreateLiveEventTask : AsyncTask<Void?, Void?, List<EventData>?>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog.show(
                this@MainActivity, null,
                resources.getText(R.string.creatingEvent), true
            )
        }

        override fun doInBackground(vararg params: Void?): List<EventData>? {
            val youtube = YouTube.Builder(
                transport, jsonFactory,
                credential
            ).setApplicationName(APP_NAME)
                .build()
            try {
                val date = Date().toString()
                YouTubeApi.createLiveEvent(
                    youtube, "Event - $date",
                    "A live streaming event - $date"
                )
                return YouTubeApi.getLiveEvents(youtube)
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION)
            } catch (e: IOException) {
                Log.e(APP_NAME, "", e)
            }
            return null
        }

        protected override fun onPostExecute(
            fetchedEvents: List<EventData>?
        ) {
            val buttonCreateEvent = findViewById<View>(R.id.create_button) as Button
            buttonCreateEvent.isEnabled = true
            progressDialog!!.dismiss()
        }
    }

    private inner class StartEventTask : AsyncTask<String?, Void?, Void?>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog.show(
                this@MainActivity, null,
                resources.getText(R.string.startingEvent), true
            )
        }

        override fun doInBackground(vararg params: String?): Void? {
            val youtube = YouTube.Builder(
                transport, jsonFactory,
                credential
            ).setApplicationName(APP_NAME)
                .build()
            try {
                YouTubeApi.startEvent(youtube, params[0])
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION)
            } catch (e: IOException) {
                Log.e(APP_NAME, "", e)
            }
            return null
        }

        override fun onPostExecute(param: Void?) {
            progressDialog!!.dismiss()
        }
    }

    private inner class EndEventTask : AsyncTask<String?, Void?, Void?>() {

        private var progressDialog: ProgressDialog? = null

        override fun onPreExecute() {
            progressDialog = ProgressDialog.show(
                this@MainActivity, null,
                resources.getText(R.string.endingEvent), true
            )
        }

        override fun doInBackground(vararg params: String?): Void? {
            val youtube = YouTube.Builder(
                transport, jsonFactory,
                credential
            ).setApplicationName(APP_NAME)
                .build()
            try {
                if (params.isNotEmpty()) {
                    YouTubeApi.endEvent(youtube, params[0])
                }
            } catch (e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            } catch (e: IOException) {
                Log.e(APP_NAME, "", e)
            }
            return null
        }

        override fun onPostExecute(param: Void?) {
            progressDialog!!.dismiss()
        }
    }

    companion object {
        const val ACCOUNT_KEY = "accountName"
        const val APP_NAME = "LiveVideo"
        private const val REQUEST_GOOGLE_PLAY_SERVICES = 0
        private const val REQUEST_GMS_ERROR_DIALOG = 1
        private const val REQUEST_ACCOUNT_PICKER = 2
        private const val REQUEST_AUTHORIZATION = 3
        private const val REQUEST_STREAMER = 4
    }
}