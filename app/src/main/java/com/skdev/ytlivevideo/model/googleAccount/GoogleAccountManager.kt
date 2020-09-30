package com.skdev.ytlivevideo.model.googleAccount

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.Scopes
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.youtube.YouTubeScopes

object GoogleAccountManager {

    private var mCredential: GoogleAccountCredential? = null

    val credential: GoogleAccountCredential?
        get() {
            return mCredential
        }

    fun setUpGoogleAccount(googleAccount: GoogleSignInAccount) {
        mCredential!!.selectedAccount = googleAccount.account
    }

    fun signIn(context: Context, savedInstanceState: Bundle?): Boolean {
        Log.d(TAG, "signIn")
        val scopes = listOf(Scopes.PROFILE, YouTubeScopes.YOUTUBE)
        mCredential = GoogleAccountCredential.usingOAuth2(context, scopes)
        return if (mCredential == null) {
            false
        } else {
            Log.d(TAG, "$mCredential")
            mCredential!!.backOff = ExponentialBackOff()
            // credential!!.selectedAccountName = AccountName.getName(view, savedInstanceState)
            true
        }
    }

    fun setUpSignInAccount(signInAccount: GoogleSignInAccount) {
        if (mCredential != null) {
            mCredential!!.selectedAccount = signInAccount.account
        }
    }

    private val TAG = GoogleAccountManager::class.java.name
}