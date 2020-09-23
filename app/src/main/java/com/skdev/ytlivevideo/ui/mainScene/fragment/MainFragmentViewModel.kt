package com.skdev.ytlivevideo.ui.mainScene.fragment

import android.app.Activity
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.plus.Plus
import com.google.android.gms.plus.model.people.Person
import com.skdev.ytlivevideo.R

class MainFragmentViewModel(val activity: Activity, private val delegate: ApiClientDelegate) : GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener,
    ApiClientInterface {

    private lateinit var mGoogleApiClient: GoogleApiClient

    override fun build() {
        mGoogleApiClient = GoogleApiClient.Builder(activity)
            .addApi(Plus.API)
            .addScope(Plus.SCOPE_PLUS_PROFILE)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
    }

    override fun isConnected() : Boolean {
        return mGoogleApiClient.isConnected
    }

    override fun getCurrentPerson() : Person? {
       return Plus.PeopleApi.getCurrentPerson(mGoogleApiClient)
    }

    override fun getAccountName() : String {
        return Plus.AccountApi.getAccountName(mGoogleApiClient)
    }

    override fun connect() {
        mGoogleApiClient.connect()
    }

    override fun disconnect() {
        mGoogleApiClient.disconnect()
    }

    override fun onConnected(bundle: Bundle?) {
        delegate.onApiClientConnected()
    }

    override fun onConnectionSuspended(i: Int) {
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (connectionResult.hasResolution()) {
            Toast.makeText(
                activity,
                R.string.connection_to_google_play_failed,
                Toast.LENGTH_SHORT
            ).show()
            Log.e(
                TAG,
                java.lang.String.format(
                    "Connection to Play Services Failed, error: %d, reason: %s",
                    connectionResult.errorCode,
                    connectionResult.toString()
                )
            )
            try {
                connectionResult.startResolutionForResult(activity, 0)
            } catch (e: IntentSender.SendIntentException) {
                Log.e(TAG, e.toString(), e)
            }
        }
    }

    companion object {
        private const val TAG = "MainFragmentViewModel"
    }
}

interface ApiClientInterface {
    fun build()
    fun connect()
    fun disconnect()
    fun isConnected() : Boolean
    fun getCurrentPerson() : Person?
    fun getAccountName() : String
}

