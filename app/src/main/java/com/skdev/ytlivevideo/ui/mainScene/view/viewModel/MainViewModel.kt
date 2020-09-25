package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.GoogleApiAvailability
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.googleAccount.GoogleSignInManager
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests.*
import com.skdev.ytlivevideo.ui.mainScene.fragment.SignInConnectDelegate
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class MainViewModel(val view: MainActivity) : MainViewModelInterface, GoogleSignInDelegate {

    private val accountManager = GoogleAccountManager()
    private val signInManager = GoogleSignInManager(view, this)
    lateinit var signInConnectDelegate: SignInConnectDelegate

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
        signInManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun startSelectAccountActivity() {
        Log.d(TAG, "startSelectAccountActivity")
        view.startAccountPicker(accountManager.credential!!.newChooseAccountIntent())
    }

    override fun didUserGoogleSignIn() {
        accountManager.setUpGoogleAccount(signInManager.account!!)
        view.invalidateOptionsMenu()
        signInConnectDelegate.signedIn()
        fetchOfAllBroadcasts()
    }

    override fun sighIn(context: Context, savedInstanceState: Bundle?) {
        if (accountManager.sighIn(context, savedInstanceState)) {
            signInManager.googleSignIn()
        } else {
            val message = view.resources.getText(R.string.oauth2_credentials_are_empty).toString()
            Utils.showError(view, message)
        }
    }

    /**
     * Fetch all broadcasts
     */
    override fun fetchOfAllBroadcasts() {
        Log.d(TAG, "fetchOfAllBroadcasts")
        val progressDialog = ProgressDialog.create(view, R.string.loadingEvents)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = FetchAllLiveEvents.runAsync(view, accountManager.credential!!).await()
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    view.didfetchOfAllBroadcasts(list)
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) { progressDialog.dismiss() }
                view.startAuthorization(e.intent)
            } catch (e: IOException) {
                launch(Dispatchers.Main) { progressDialog.dismiss() }
                Toast.makeText(view, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Create a new Broadcast
     */
    override fun createNewBroadcast() {
        Log.d(TAG, "createEvent")

        val date = Date().toString()
        val description = "Event - $date"
        val name = "A live streaming event - $date"

        val progressDialog = ProgressDialog.create(view, R.string.creatingEvent)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CreateLiveEvent.runAsync(view, accountManager.credential!!, name, description).await()
                launch(Dispatchers.Main) { progressDialog.dismiss() }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) { progressDialog.dismiss() }
                view.startAuthorization(e.intent)
            } catch (e: IOException) {
                launch(Dispatchers.Main) { progressDialog.dismiss() }
                Toast.makeText(view, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Start live video
     */
    override fun startStreaming(liveBroadcastItem: LiveBroadcastItem) {
        Log.d(TAG, "startStreaming")
        val broadcastId: String = liveBroadcastItem.id
        CoroutineScope(Dispatchers.IO).launch {
            try {
                StartLiveEvent.runAsync(view, accountManager.credential!!, broadcastId).await()
                view.startBroadcastStreaming(broadcastId, liveBroadcastItem.ingestionAddress!!)
            } catch (e: UserRecoverableAuthIOException) {
                view.startAuthorization(e.intent)
            } catch (e: IOException) {
                Toast.makeText(view, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
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
        if (accountManager.credential?.selectedAccountName == null) {
            // ask user to choose account
            startSelectAccountActivity()
        }
    }

    private fun didSelectAccount(intent: Intent) {
        val accountName = intent.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
        accountManager.credential!!.selectedAccountName = accountName
        AccountName.saveName(view, accountName)
    }

    /**
     * Finish Live Video
     */
    private fun didSelectBroadcast(intent: Intent) {
        val broadcastId = intent.getStringExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EndLiveEvent.runAsync(view, accountManager.credential!!, broadcastId).await()
                Log.d(TAG, "The Broadcast is finished")
            } catch (e: UserRecoverableAuthIOException) {
                view.startAuthorization(e.intent)
            } catch (e: IOException) {
                Toast.makeText(view, e.localizedMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        return signInManager.lastSignedInAccount
    }

    fun isConnected() : Boolean {
        return signInManager.isConnected
    }

    fun getAccountName() : String {
        return signInManager.accountName
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

interface GoogleSignInDelegate {
    fun didUserGoogleSignIn()
}