package com.skdev.ytlivevideo.model.googleAccount

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.GoogleSignInDelegate

class GoogleSignInManager(val context: Activity, private val delegate: GoogleSignInDelegate) {
    private var mAccount: GoogleSignInAccount? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null

    val account: GoogleSignInAccount?
        get() {
            return mAccount
        }

    fun googleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
        context.startActivityForResult(mGoogleSignInClient?.signInIntent, RC_SIGN_IN)
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    fun logOut() {
        mGoogleSignInClient?.signOut()
    }

    fun revokeAccess() {
        mGoogleSignInClient?.revokeAccess()
    }

    val isConnected: Boolean
        get() {
            return mAccount != null
        }

    val lastSignedInAccount: GoogleSignInAccount?
        get() {
            return GoogleSignIn.getLastSignedInAccount(context)
        }

    val accountName: String
        get() {
            return mAccount?.displayName ?: "Not signed in"
        }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            mAccount = completedTask.getResult(ApiException::class.java)
            if (mAccount != null) {
                Log.d(TAG,"The User is authenticated '${mAccount!!.getDisplayName()}' ${mAccount!!.grantedScopes} has done!")
                delegate.didUserGoogleSignIn()
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    companion object {
        private val TAG = GoogleSignInManager::class.java.name
        private const val RC_SIGN_IN = 9001
    }
}
