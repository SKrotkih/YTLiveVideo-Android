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
package com.skdev.ytlivevideo.ui

import android.accounts.AccountManager
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.Button
import androidx.preference.PreferenceManager
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTube
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.ui.LiveEventsListFragment.Callbacks
import com.skdev.ytlivevideo.model.youtubeApi.liveEvents.LiveEventsItem
import com.skdev.ytlivevideo.model.youtubeApi.liveEvents.LiveEventsController
import com.skdev.ytlivevideo.model.youtubeApi.liveEvents.tasks.*
import com.skdev.ytlivevideo.util.*
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
    private var mLiveEventsListFragment: LiveEventsListFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureLoader()
        credential = GoogleAccountCredential.usingOAuth2(applicationContext, Utils.SCOPES)
        if (credential != null) {
            // set exponential backoff policy
            credential!!.backOff = ExponentialBackOff()
            if (savedInstanceState != null) {
                mChosenAccountName = savedInstanceState.getString(ACCOUNT_KEY)
            } else {
                loadAccount()
            }
            credential!!.selectedAccountName = mChosenAccountName
            mLiveEventsListFragment = fragmentManager
                .findFragmentById(R.id.list_fragment) as LiveEventsListFragment
        } else {
            val message = resources.getText(R.string.oauth2_credentials_are_empty).toString()
            Utils.showError(this@MainActivity, message)
        }
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
            REQUEST_ACCOUNT_PICKER -> if (resultCode == RESULT_OK && data.extras != null) {
                val accountName = data.extras!!.getString(
                        AccountManager.KEY_ACCOUNT_NAME
                )
                if (accountName != null) {
                    mChosenAccountName = accountName
                    credential!!.selectedAccountName = accountName
                    saveAccount()
                }
            }
            REQUEST_STREAMER -> if (resultCode == RESULT_OK && data.extras != null) {
                val broadcastId = data.getStringExtra(LiveEventsController.BROADCAST_ID_KEY)
                if (broadcastId != null) {
                    EndLiveEventTask(this, credential, broadcastId, object : LiveEventTaskCallback {
                        override fun onAuthException(e: UserRecoverableAuthIOException) {
                            startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                        }
                        override fun onCompletion(fetchedLiveEventsItems: List<LiveEventsItem>) {
                        }
                    }).execute()
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
            val googleAPI = GoogleApiAvailability.getInstance()
            val dialog: Dialog = googleAPI.getErrorDialog(
                    this@MainActivity,
                    connectionStatusCode,
                    REQUEST_GOOGLE_PLAY_SERVICES
            )
            dialog.show()
        }
    }

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private fun checkGooglePlayServicesAvailable(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val connectionStatusCode: Int = googleAPI.isGooglePlayServicesAvailable(this@MainActivity)
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
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

    override fun onEventSelected(liveBroadcast: LiveEventsItem?) {
        if (liveBroadcast != null) {
            startStreaming(liveBroadcast)
        }
    }

    private val liveEvents: Unit
        get() {
            if (mChosenAccountName == null || credential == null) {
                return
            }
            GetLiveEventTask(this, credential, object : LiveEventTaskCallback {
                override fun onAuthException(e: UserRecoverableAuthIOException) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }
                override fun onCompletion(fetchedLiveEventsItems: List<LiveEventsItem>) {
                    mLiveEventsListFragment?.setEvents(fetchedLiveEventsItems)
                }
            }).execute()
        }

    fun createEvent(view: View?) {
        CreateLiveEventTask(this, credential, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            }
            override fun onCompletion(fetchedLiveEventsItems: List<LiveEventsItem>) {
            }
        }).execute()
    }

    private fun startStreaming(liveEventsItem: LiveEventsItem) {
        val broadcastId: String = liveEventsItem.id
        StartLiveEventTask(this, credential, broadcastId, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            }
            override fun onCompletion(fetchedLiveEventsItems: List<LiveEventsItem>) {
            }
        }).execute()
        val intent = Intent(
            applicationContext,
            VideoStreamingActivity::class.java
        )
        intent.putExtra(LiveEventsController.RTMP_URL_KEY, liveEventsItem.ingestionAddress)
        intent.putExtra(LiveEventsController.BROADCAST_ID_KEY, broadcastId)
        startActivityForResult(intent, REQUEST_STREAMER)
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