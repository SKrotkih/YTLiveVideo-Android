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
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.Event
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel : ViewModel(), MainViewModelInterface {

    // Data Source
    var allBroadcastItems: MutableLiveData<Event<List<LiveBroadcastItem>>> = MutableLiveData()
    var upcomingBroadcastItems: MutableLiveData<Event<List<LiveBroadcastItem>>> = MutableLiveData()
    var activeBroadcastItems: MutableLiveData<Event<List<LiveBroadcastItem>>> = MutableLiveData()
    var completedBroadcastItems: MutableLiveData<Event<List<LiveBroadcastItem>>> = MutableLiveData()

    // State changes
    var needToCheckGooglePlayServicesAvailable: MutableLiveData<Event<Boolean>> = MutableLiveData()
    var needSendRequestAuthorization: MutableLiveData<Event<Boolean>> = MutableLiveData()
    var errorAuthorization: MutableLiveData<Event<UserRecoverableAuthIOException>> = MutableLiveData()
    var errorMessage: MutableLiveData<Event<String>> = MutableLiveData()
    var invalidateView: MutableLiveData<Event<Boolean>> = MutableLiveData()
    var startProcessing: MutableLiveData<Event<String>> = MutableLiveData()
    var stopProcessing: MutableLiveData<Event<Boolean>> = MutableLiveData()
    var accountName: MutableLiveData<Event<String>> = MutableLiveData()

    lateinit var signInManager: GoogleSignInManager

    override fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            Config.REQUEST_GMS_ERROR_DIALOG -> {
            }
            Config.REQUEST_GOOGLE_PLAY_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                didCheckGooglePlayServices()
            } else {
                needToCheckGooglePlayServicesAvailable.value = Event(true)
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
        needSendRequestAuthorization.value = Event(true)
    }

    override fun logOut() {
        signInManager.logOut()
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
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = LiveBroadcasts.getLiveBroadcastsAsync(state)
                launch(Dispatchers.Main) {
                    stopProcessing()
                    when (state) {
                        BroadcastState.ALL -> {
                            allBroadcastItems.value = Event(list)
                        }
                        BroadcastState.UPCOMING -> {
                            upcomingBroadcastItems.value = Event(list)
                        }
                        BroadcastState.ACTIVE -> {
                            activeBroadcastItems.value = Event(list)
                        }
                        BroadcastState.COMPLETED -> {
                            completedBroadcastItems.value = Event(list)
                        }
                    }
                }
            } catch (e: UserRecoverableAuthIOException) {
                launch(Dispatchers.Main) {
                    stopProcessing()
                    errorAuthorization.value = Event(e)
                }
            } catch (e: IOException) {
                launch(Dispatchers.Main) {
                    stopProcessing()
                    errorMessage.value = Event(e.localizedMessage)
                }
            }
        }
    }

    /**
     * Progress Indicator
     */
    private fun startProcessing(title: String) {
        startProcessing.value = Event(title)
    }

    private fun stopProcessing() {
        stopProcessing.value = Event(true)
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
        accountName.value = Event(selectedAccountName!!)
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
