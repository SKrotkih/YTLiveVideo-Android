package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager.widget.ViewPager
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
import com.skdev.ytlivevideo.ui.mainScene.adapter.SectionsPagerAdapter
import com.skdev.ytlivevideo.ui.mainScene.fragment.BroadcastState
import com.skdev.ytlivevideo.ui.mainScene.fragment.BroadcastsListFragment
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class MainViewModel : ViewModel(), MainViewModelInterface {

    var allBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()
    var upcomingBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()
    var activeBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()
    var completedBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()

    private val accountManager = GoogleAccountManager()
    lateinit var viewDelegate: MainActivity

    val signInManager: GoogleSignInManager by lazy {
        val fld = GoogleSignInManager(viewDelegate)
        fld.didUserSignIn.observe(viewDelegate, {
            if (it) didUserGoogleSignIn()
        })
        return@lazy fld
    }

    override fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent?) {
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
            REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                didSelectAccount(data)
            }
            REQUEST_STREAMER -> if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                didSelectBroadcast(data)
            }
        }
        signInManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun startSelectAccountActivity() {
        Log.d(TAG, "startSelectAccountActivity")
        viewDelegate.startAccountPicker(accountManager.credential!!.newChooseAccountIntent())
    }

    override fun signIn(context: Context, savedInstanceState: Bundle?) {
        if (accountManager.signIn(context, savedInstanceState)) {
            signInManager.googleSignIn()
        } else {
            val message = viewDelegate.resources.getText(R.string.oauth2_credentials_are_empty).toString()
            Utils.showError(viewDelegate, message)
        }
    }

    private fun didUserGoogleSignIn() {
        accountManager.setUpGoogleAccount(signInManager.account!!)
        viewDelegate.invalidateOptionsMenu()
        viewDelegate.renderView()
    }

    override fun logOut() {
        signInManager.logOut()
    }

    /**
     * When the owner activity is finished, the framework calls the ViewModel objects's onCleared() method so that it can clean up resources
     */
    override fun onCleared() {
        super.onCleared()
    }

    /**
     *  Tab Bar
     */
    fun setupViewPager(viewPager: ViewPager, adapter: SectionsPagerAdapter) {
        adapter.addFragment(BroadcastsListFragment(BroadcastState.ALL), "All")
        adapter.addFragment(BroadcastsListFragment(BroadcastState.UPCOMING), "Upcoming")
        adapter.addFragment(BroadcastsListFragment(BroadcastState.ACTIVE), "Active")
        adapter.addFragment(BroadcastsListFragment(BroadcastState.COMPLETED), "Completed")
        viewPager.adapter = adapter
        (adapter.getItem(0) as BroadcastsListFragment).selected = true
        viewPager.addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
            override fun onPageSelected(position: Int) {
                for (i in 0 until adapter.count) {
                    val fragment = adapter.getItem(i) as BroadcastsListFragment
                    if (i == position) {
                        fragment.selected = true
                        fetchBroadcasts(fragment.state)
                    } else {
                        fragment.selected = false
                    }
                }
            }
        })
    }

    /**
     * Fetch broadcasts by State
     */
    override fun fetchBroadcasts(state: BroadcastState) {
        val progressDialog = ProgressDialog.create(viewDelegate, R.string.loadingEvents)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch() {
            try {
                val list = FetchAllLiveEvents.runAsync(accountManager.credential!!, state)
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    when (state) {
                        BroadcastState.ALL -> {
                            allBroadcastItems.value = list
                        }
                        BroadcastState.UPCOMING -> {
                            upcomingBroadcastItems.value = list
                        }
                        BroadcastState.ACTIVE -> {
                            activeBroadcastItems.value = list
                        }
                        BroadcastState.COMPLETED -> {
                            completedBroadcastItems.value = list
                        }
                    }
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    viewDelegate.startAuthorization(e.intent)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(viewDelegate, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
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

        val progressDialog = ProgressDialog.create(viewDelegate, R.string.creatingEvent)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                CreateLiveEvent.runAsync(viewDelegate, accountManager.credential!!, name, description).await()
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    viewDelegate.startAuthorization(e.intent)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(viewDelegate, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Transit broadcast to live (or testing) state from active state
     */
    override fun startStreaming(liveBroadcastItem: LiveBroadcastItem) {
        Log.d(TAG, "startStreaming")
        val broadcastId: String = liveBroadcastItem.id
        val streamId = liveBroadcastItem.streamId
        val progressDialog = ProgressDialog.create(viewDelegate, R.string.startStreaming)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isBroadcastingStarted = StartLiveEvent.runAsync(accountManager.credential!!, streamId, broadcastId).await()
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    if (isBroadcastingStarted) viewDelegate.startBroadcastStreaming(broadcastId, liveBroadcastItem.ingestionAddress!!)
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    viewDelegate.startAuthorization(e.intent)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(viewDelegate, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    /**
     * Transit broadcast to completed state
     */
    private fun didSelectBroadcast(intent: Intent) {
        val broadcastId = intent.getStringExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY)
        val progressDialog = ProgressDialog.create(viewDelegate, R.string.startStreaming)
        progressDialog.show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EndLiveEvent.runAsync(accountManager.credential!!, broadcastId).await()
                Log.d(TAG, "The Broadcast is finished")
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    viewDelegate.startAuthorization(e.intent)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Toast.makeText(viewDelegate, e.localizedMessage, Toast.LENGTH_LONG).show()
                }
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
        val connectionStatusCode: Int = googleAPI.isGooglePlayServicesAvailable(viewDelegate)
        if (googleAPI.isUserResolvableError(connectionStatusCode)) {
            viewDelegate.showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
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
        AccountName.saveName(viewDelegate, accountName)
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
