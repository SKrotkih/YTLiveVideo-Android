package com.skdev.ytlivevideo.ui.mainScene.fragment

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task

class MainFragmentViewModel(val context: Activity, private val delegate: ApiClientDelegate) : ApiClientInterface {
    private var account: GoogleSignInAccount? = null
    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mContext: Activity

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        mContext = context
        mGoogleSignInClient = GoogleSignIn.getClient(mContext, gso)
    }

    override fun signIn() {
        if (isConnected()) {
            delegate.onApiClientConnected()
        } else {
            mContext.startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun getLastSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(mContext)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                Log.d(TAG,"The User is authenticated '${account!!.getDisplayName()}' ${account!!.grantedScopes} has done!")
            }
            delegate.onApiClientConnected()
        } catch (e: ApiException) {
            Log.w(TAG, "signInResult:failed code=" + e.statusCode)
        }
    }

    private var signInIntent: Intent? = null
        get() {
            return mGoogleSignInClient?.signInIntent
        }

    override fun signOut() {
        mGoogleSignInClient?.signOut()
    }

    override fun revokeAccess() {
        mGoogleSignInClient?.revokeAccess()
    }

    override fun isConnected() : Boolean {
        return account != null
    }

    override fun getAccountName() : String {
        return account?.displayName ?: "Not signed in"
    }

    companion object {
        private val TAG = MainFragmentViewModel::class.java.name
        private const val RC_SIGN_IN = 9001
    }
}

interface ApiClientInterface {
    fun signIn()
    fun signOut()
    fun revokeAccess()
    fun getLastSignedInAccount(): GoogleSignInAccount?
    fun isConnected() : Boolean
    fun getAccountName() : String
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
}
