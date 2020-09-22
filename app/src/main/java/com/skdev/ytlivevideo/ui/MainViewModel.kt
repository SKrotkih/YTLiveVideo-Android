package com.skdev.ytlivevideo.ui

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.ExponentialBackOff
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.*
import com.skdev.ytlivevideo.util.Utils

class MainViewModel(val activity: MainActivity) {

    private var credential: GoogleAccountCredential? = null

    fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_GMS_ERROR_DIALOG -> {
            }
            REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                didCheckGooglePlayServices()
            } else {
                checkGooglePlayServicesAvailable()
            }
            REQUEST_AUTHORIZATION -> if (resultCode != Activity.RESULT_OK) {
                startSelectAccountActivity()
            }
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data.extras != null) {
                didSelectAccount(data)
            }
            REQUEST_STREAMER -> if (resultCode == Activity.RESULT_OK && data.extras != null) {
                didSelectBroadcast(data)
            }
        }
    }

    fun startSelectAccountActivity() {
        Log.i(MainActivity.APP_NAME, "startSelectAccountActivity")
        activity.startAccountPicker(credential!!.newChooseAccountIntent())
    }


    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private fun checkGooglePlayServicesAvailable(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val connectionStatusCode: Int = googleAPI.isGooglePlayServicesAvailable(activity)
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
            activity.showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
            return false
        }
        return true
    }

    private fun didCheckGooglePlayServices() {
        Log.i(MainActivity.APP_NAME, "didCheckGooglePlayServices")
        // check if there is already an account selected
        if (credential?.selectedAccountName == null) {
            // ask user to choose account
            startSelectAccountActivity()
        }
    }

    private fun didSelectAccount(intent: Intent) {
        val accountName = intent.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
        credential!!.selectedAccountName = accountName
        AccountName.saveName(activity, accountName)
    }

    private fun didSelectBroadcast(intent: Intent) {
        val broadcastId = intent.getStringExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY)
        if (broadcastId != null) {
            EndLiveEvent(activity, credential, broadcastId, object : LiveEventTaskCallback {
                override fun onAuthException(e: UserRecoverableAuthIOException) {
                    activity.startAuthorization(e.intent)
                }
                override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
                }
            }).execute()
        }
    }

    /**
     * Present User accounts dialog
     */
    fun sighIn(context: Context, savedInstanceState: Bundle?) {
        Log.i(MainActivity.APP_NAME, "sighIn")
        credential = GoogleAccountCredential.usingOAuth2(context, Utils.SCOPES)
        if (credential == null) {
            val message = activity.resources.getText(R.string.oauth2_credentials_are_empty).toString()
            Utils.showError(activity, message)
            return
        }
        credential!!.backOff = ExponentialBackOff()
        credential!!.selectedAccountName = AccountName.getName(activity, savedInstanceState)
        activity.invalidateOptionsMenu()
    }

    /**
     * Fetch all broadcasts
     */
    fun fetchLiveBroadcastItems() {
        if (AccountName.getName(activity) == null || credential == null) {
            return
        }
        Log.i(MainActivity.APP_NAME, "fetchLiveBroadcastItems")
        GetLiveEvent(activity, credential, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                activity.startAuthorization(e.intent)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
                activity.didFetchLiveBroadcastItems(fetchedLiveBroadcastItems)
            }
        }).execute()
    }

    /**
     * Create new Broadcast
     */
    fun createEvent(view: View?) {
        Log.i(MainActivity.APP_NAME, "createEvent")
        CreateLiveEvent(activity, credential, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                activity.startAuthorization(e.intent)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
            }
        }).execute()
    }

    /**
     * Start Broadcast live video
     */
    fun startStreaming(liveBroadcastItem: LiveBroadcastItem) {
        Log.i(MainActivity.APP_NAME, "startStreaming")
        val broadcastId: String = liveBroadcastItem.id
        StartLiveEvent(activity, credential, broadcastId, object : LiveEventTaskCallback {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                activity.startAuthorization(e.intent)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
            }
        }).execute()
        activity.startBroadcastStreaming(broadcastId, liveBroadcastItem.ingestionAddress!!)
    }

    companion object {
        const val REQUEST_GOOGLE_PLAY_SERVICES = 0
        const val REQUEST_GMS_ERROR_DIALOG = 1
        const val REQUEST_ACCOUNT_PICKER = 2
        const val REQUEST_AUTHORIZATION = 3
        const val REQUEST_STREAMER = 4
    }
}