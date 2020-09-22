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
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.ExponentialBackOff
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.ui.LiveEventsListFragment.Callbacks
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.*
import com.skdev.ytlivevideo.util.*

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Main activity class which handles authorization and intents.
 */
class MainActivity : Activity(), Callbacks {
    private var credential: GoogleAccountCredential? = null
    private val mLiveEventsListFragment: LiveEventsListFragment by lazy {
        fragmentManager.findFragmentById(R.id.list_fragment) as LiveEventsListFragment
    }
    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sighIn(savedInstanceState)
    }

    /**
        Menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> fetchLiveBroadcastItems()
            R.id.menu_accounts -> {
                startSelectAccountActivity()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
        Parse Activity Results
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)


        when (requestCode) {
            REQUEST_GMS_ERROR_DIALOG -> {
            }
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == RESULT_OK) {
                didCheckGooglePlayServices()
            } else {
                checkGooglePlayServicesAvailable()
            }
            REQUEST_AUTHORIZATION -> if (resultCode != RESULT_OK) {
                startSelectAccountActivity()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == RESULT_OK && data.extras != null) {
                didSelectAccount(data)
            }
            REQUEST_STREAMER -> if (resultCode == RESULT_OK && data.extras != null) {
                didSelectBroadcast(data)
            }
        }
    }

    private fun didSelectAccount(data: Intent) {
        val accountName = data.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
        credential!!.selectedAccountName = accountName
        AccountName.saveName(this, accountName)
    }

    private fun didSelectBroadcast(data: Intent) {
        val broadcastId = data.getStringExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY)
        if (broadcastId != null) {
            EndLiveEvent(this, credential, broadcastId, object : LiveEventTaskCallback {
                override fun onAuthException(e: UserRecoverableAuthIOException) {
                    startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
                }
                override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
                }
            }).execute()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AccountName.saveName(this, AccountName.getName(this), outState)
    }

    override fun onConnected(connectedAccountName: String?) {
        // Make API requests only when the user has successfully signed in.
        fetchLiveBroadcastItems()
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

    private fun didCheckGooglePlayServices() {
        Log.i(APP_NAME, "didCheckGooglePlayServices")
        // check if there is already an account selected
        if (credential?.selectedAccountName == null) {
            // ask user to choose account
            startSelectAccountActivity()
        }
    }

    private fun startSelectAccountActivity() {
        Log.i(APP_NAME, "startSelectAccountActivity")
        startActivityForResult(credential!!.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER)
    }

    override fun onBackPressed() {
        Log.i(APP_NAME, "onBackPressed")
    }

    override fun onGetImageLoader(): ImageLoader? {
        Log.i(APP_NAME, "onGetImageLoader")
        return mImageLoader
    }

    override fun onEventSelected(liveBroadcast: LiveBroadcastItem?) {
        Log.i(APP_NAME, "onEventSelected")
        if (liveBroadcast != null) {
            startStreaming(liveBroadcast)
        }
    }

    /**
     * Show GooglePlay Services Availability Error Dialog
     */
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
     * Present User accounts dialog
     */
    private fun sighIn(savedInstanceState: Bundle?) {
        Log.i(APP_NAME, "sighIn")
        credential = GoogleAccountCredential.usingOAuth2(applicationContext, Utils.SCOPES)
        if (credential == null) {
            val message = resources.getText(R.string.oauth2_credentials_are_empty).toString()
            Utils.showError(this@MainActivity, message)
            return
        }
        credential!!.backOff = ExponentialBackOff()
        credential!!.selectedAccountName = AccountName.getName(this, savedInstanceState)
        invalidateOptionsMenu()
    }

    /**
     * Fetch all broadcasts
     */
    private fun fetchLiveBroadcastItems() {
        if (AccountName.getName(this) == null || credential == null) {
            return
        }
        Log.i(APP_NAME, "fetchLiveBroadcastItems")
        GetLiveEvent(this, credential, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
                Log.i(APP_NAME, "fetchedLiveBroadcastItems=$fetchedLiveBroadcastItems")
                mLiveEventsListFragment.setEvents(fetchedLiveBroadcastItems)
            }
        }).execute()
    }

    /**
     * Create new Broadcast
     */
    fun createEvent(view: View?) {
        Log.i(APP_NAME, "createEvent")
        CreateLiveEvent(this, credential, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
            }
        }).execute()
    }

    /**
     * Start Broadcast live video
     */
    private fun startStreaming(liveBroadcastItem: LiveBroadcastItem) {
        Log.i(APP_NAME, "startStreaming")
        val broadcastId: String = liveBroadcastItem.id
        StartLiveEvent(this, credential, broadcastId, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                startActivityForResult(e.intent, REQUEST_AUTHORIZATION)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
            }
        }).execute()
        val intent = Intent(
            applicationContext,
            VideoStreamingActivity::class.java
        )
        intent.putExtra(YouTubeLiveBroadcastRequest.RTMP_URL_KEY, liveBroadcastItem.ingestionAddress)
        intent.putExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY, broadcastId)
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
