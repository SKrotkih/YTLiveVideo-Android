package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.viewpager.widget.ViewPager
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.googleAccount.GoogleSignInManager
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.BroadcastState
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.requests.*
import com.skdev.ytlivevideo.ui.mainScene.adapter.SectionsPagerAdapter
import com.skdev.ytlivevideo.ui.mainScene.fragment.BroadcastsListFragment
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.util.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel : ViewModel(), MainViewModelInterface {

    // Data Source
    var allBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()
    var upcomingBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()
    var activeBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()
    var completedBroadcastItems: MutableLiveData<List<LiveBroadcastItem>> = MutableLiveData()

    // State changes
    var needToCheckGooglePlayServicesAvailable: MutableLiveData<Boolean> = MutableLiveData()
    var needSendRequestAuthorization: MutableLiveData<Boolean> = MutableLiveData()
    var errorAuthorization: MutableLiveData<UserRecoverableAuthIOException> = MutableLiveData()
    var errorMessage: MutableLiveData<String> = MutableLiveData()
    var invalidateView: MutableLiveData<Boolean> = MutableLiveData()
    var startProcessing: MutableLiveData<String> = MutableLiveData()
    var stopProcessing: MutableLiveData<Boolean> = MutableLiveData()
    var accountName: MutableLiveData<String> = MutableLiveData()

    lateinit var signInManager: GoogleSignInManager

    override fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Config.REQUEST_GMS_ERROR_DIALOG -> {
            }
            Config.REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                didCheckGooglePlayServices()
            } else {
                needToCheckGooglePlayServicesAvailable.value = true
            }
            Config.REQUEST_AUTHORIZATION -> if (resultCode != Activity.RESULT_OK) {
                startSelectAccountActivity()
            }
            Config.REQUEST_ACCOUNT_PICKER -> if (resultCode == Activity.RESULT_OK && data?.extras != null) {
                didSelectAccount(data)
            }
        }
        signInManager.onActivityResult(requestCode, resultCode, data)
    }

    override fun startSelectAccountActivity() {
        Log.d(TAG, "startSelectAccountActivity")
        needSendRequestAuthorization.value = true
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
        startProcessing(getProgressBarTitle(state))
        CoroutineScope(Dispatchers.IO).launch() {
            try {
                val list = LiveBroadcasts.getLiveBroadcastsAsync(state)
                launch(Dispatchers.Main) {
                    stopProcessing()
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
                    stopProcessing()
                    errorAuthorization.value = e
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    stopProcessing()
                    errorMessage.value = e.localizedMessage
                }
            }
        }
    }

    /**
     * Progress Indicator
     */
    private fun startProcessing(title: String) {
        startProcessing.value = title
    }

    private fun stopProcessing() {
        stopProcessing.value = true
    }

    private fun getProgressBarTitle(state: BroadcastState): String {
        return "Downloading ${
            when(state) {
                BroadcastState.ALL -> "all"
                BroadcastState.UPCOMING -> "all upcoming"
                BroadcastState.ACTIVE -> "all active"
                BroadcastState.COMPLETED -> "all completed"                
            }
        } broadcasts…"
    }

    /**
     * Private Methods
     */
    private fun didCheckGooglePlayServices() {
        Log.d(TAG, "didCheckGooglePlayServices")
        // check if there is already an account selected
        if (GoogleAccountManager.credential?.selectedAccountName == null) {
            // ask user to choose account
            startSelectAccountActivity()
        }
    }

    private fun didSelectAccount(intent: Intent) {
        val selectedAccountName = intent.extras!!.getString(AccountManager.KEY_ACCOUNT_NAME)
        GoogleAccountManager.credential!!.selectedAccountName = selectedAccountName
        accountName.value = selectedAccountName
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

    fun getPhotoUrl() : Uri? {
        return signInManager.account?.photoUrl
    }

    companion object {
        private val TAG = MainViewModel::class.java.name
    }
}
