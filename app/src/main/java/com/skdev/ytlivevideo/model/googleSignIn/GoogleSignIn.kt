package com.skdev.ytlivevideo.model.googleSignIn

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

/**
 * Activity to demonstrate basic retrieval of the Google user's ID, email address, and basic
 * profile.
 */

/**
 * Source from there:
 * https://developers.google.com/identity/sign-in/android/sign-in
 * https://developers.google.com/identity/sign-in/android/start-integrating
 */

class GoogleSignIn(context: Activity) {
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mContext: Activity

    init {
        // [START configure_signin]
        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        // [END configure_signin]

        // [START build_client]
        // Build a GoogleSignInClient with the options specified by gso.
        mContext = context
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso)
        // [END build_client]
    }

    fun getLastSignedInAccount(): GoogleSignInAccount? {
        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        return GoogleSignIn.getLastSignedInAccount(mContext)
    }

    // [START onActivityResult]
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    // [END onActivityResult]
    // [START handleSignInResult]
    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)

            // Signed in successfully, show authenticated UI.
            if (account != null) {
                Log.d(TAG, "Log in as '$account.getDisplayName()' has done!")
            }
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    // [END handleSignInResult]
    // [START signIn]
    //  startActivityForResult(signIn(), RC_SIGN_IN)
    fun signIn(): Intent? {
        return mGoogleSignInClient?.signInIntent
    }

    // [END signIn]
    // [START signOut]
    fun signOut() {
        mGoogleSignInClient?.signOut()
//            .addOnCompleteListener(this, OnCompleteListener<Void?> {
//                // [START_EXCLUDE]
//                Log.d(TAG, "Log out has done!")
//                // [END_EXCLUDE]
//            })
    }

    // [END signOut]
    // [START revokeAccess]
    private fun revokeAccess() {
        mGoogleSignInClient?.revokeAccess()
//            .addOnCompleteListener(this, OnCompleteListener<Void?> {
//                // [START_EXCLUDE]
//                Log.d(TAG, "Revoke has done!")
//                // [END_EXCLUDE]
//            })
    }

    companion object {
        private const val TAG = "SignInActivity"
        private const val RC_SIGN_IN = 9001
    }
}