package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.Scopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.*
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.util.Utils

class MainViewModel(val view: MainActivity) : MainViewModelInterface {

    private var credential: GoogleAccountCredential? = null

    override fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent) {
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

    override fun startSelectAccountActivity() {
        Log.d(TAG, "startSelectAccountActivity")
        view.startAccountPicker(credential!!.newChooseAccountIntent())
    }

    /**
     * Present User accounts dialog
     */
    override fun sighIn(context: Context, savedInstanceState: Bundle?) {
        Log.d(TAG, "sighIn")
        val scopes = listOf(Scopes.PROFILE, YouTubeScopes.YOUTUBE)
        credential = GoogleAccountCredential.usingOAuth2(context, scopes)
        if (credential == null) {
            val message = view.resources.getText(R.string.oauth2_credentials_are_empty).toString()
            Utils.showError(view, message)
            return
        }
        credential!!.backOff = ExponentialBackOff()
        credential!!.selectedAccountName = AccountName.getName(view, savedInstanceState)
        view.invalidateOptionsMenu()
    }

    /**
     * Fetch all broadcasts
     */
    override fun fetchLiveBroadcastItems() {
        if (AccountName.getName(view) == null || credential == null) {
            return
        }
        Log.d(TAG, "fetchLiveBroadcastItems")
        GetLiveEvent(view, credential, object : LiveBroadcastApiInterface {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                view.startAuthorization(e.intent)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
                view.didFetchLiveBroadcastItems(fetchedLiveBroadcastItems)
            }
        }).execute()
    }

    /**
     * Create new Broadcast
     */
    override fun createEvent() {
        Log.d(TAG, "createEvent")
        CreateLiveEvent(view, credential, object : LiveBroadcastApiInterface {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                view.startAuthorization(e.intent)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
            }
        }).execute()
    }

    /**
     * Start Broadcast live video
     */
    override fun startStreaming(liveBroadcastItem: LiveBroadcastItem) {
        Log.d(TAG, "startStreaming")
        val broadcastId: String = liveBroadcastItem.id
        StartLiveEvent(view, credential, broadcastId, object : LiveBroadcastApiInterface {
            override fun onAuthException(e: UserRecoverableAuthIOException) {
                view.startAuthorization(e.intent)
            }
            override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
            }
        }).execute()
        view.startBroadcastStreaming(broadcastId, liveBroadcastItem.ingestionAddress!!)
    }

    /**
     * Private Methods
     */

    /**
     * Check that Google Play services APK is installed and up to date.
     */
    private fun checkGooglePlayServicesAvailable(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val connectionStatusCode: Int = googleAPI.isGooglePlayServicesAvailable(view)
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
            view.showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
            return false
        }
        return true
    }

    private fun didCheckGooglePlayServices() {
        Log.d(TAG, "didCheckGooglePlayServices")
        // check if there is already an account selected
        if (credential?.selectedAccountName == null) {
            // ask user to choose account
            startSelectAccountActivity()
        }
    }

    private fun didSelectAccount(intent: Intent) {
        val accountName = intent.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
        credential!!.selectedAccountName = accountName
        AccountName.saveName(view, accountName)
    }

    private fun didSelectBroadcast(intent: Intent) {
        val broadcastId = intent.getStringExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY)
        if (broadcastId != null) {
            EndLiveEvent(view, credential, broadcastId, object : LiveBroadcastApiInterface {
                override fun onAuthException(e: UserRecoverableAuthIOException) {
                    view.startAuthorization(e.intent)
                }
                override fun onCompletion(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
                }
            }).execute()
        }
    }

    companion object {
        private val TAG = MainViewModel::class.java.name
        const val REQUEST_GOOGLE_PLAY_SERVICES = 0
        const val REQUEST_GMS_ERROR_DIALOG = 1
        const val REQUEST_ACCOUNT_PICKER = 2
        const val REQUEST_AUTHORIZATION = 3
        const val REQUEST_STREAMER = 4
    }
}