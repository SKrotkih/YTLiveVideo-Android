package com.skdev.ytlivevideo.model.googleAccount

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.services.youtube.YouTubeScopes
import com.skdev.ytlivevideo.util.Event

class GoogleSignInManager(val context: Activity) {
    var didUserSignIn: MutableLiveData<Event<Boolean>> = MutableLiveData()
    var googleApiReadyToUse: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private var mAccount: GoogleSignInAccount? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null

    val account: GoogleSignInAccount?
        get() {
            return mAccount
        }

    fun googleSignIn(launcher: ActivityResultLauncher<Intent>) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(YouTubeScopes.YOUTUBE))
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(context, gso)
        mGoogleSignInClient?.signInIntent?.let { launcher.launch(it) }
    }

    fun onSignInResult(data: Intent?) {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        handleSignInResult(task)
    }

    fun logOut() {
        mGoogleSignInClient?.signOut()
    }

    val accountName: String
        get() {
            return mAccount?.displayName ?: "Not signed in"
        }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            mAccount = completedTask.getResult(ApiException::class.java)
            if (mAccount != null) {
                Log.d(TAG,"The User is authenticated '${mAccount!!.displayName}' ${mAccount!!.grantedScopes} has done!")
                didUserSignIn.value = Event(true)
                googleApiReadyToUse.value = Event(true)
            }
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    companion object {
        private val TAG = GoogleSignInManager::class.java.name
    }
}
