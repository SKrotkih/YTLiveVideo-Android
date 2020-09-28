/*
 * Copyright (c) 2014 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.skdev.ytlivevideo.ui.mainScene.view

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelStoreOwner
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GoogleApiAvailability
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.ui.mainScene.fragment.BroadcastsListFragment
import com.skdev.ytlivevideo.ui.mainScene.fragment.FragmentDelegate
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import com.skdev.ytlivevideo.ui.videoStreamingScene.VideoStreamingActivity
import com.skdev.ytlivevideo.util.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Main activity class which handles authorization and intents.
 */
class MainActivity : AppCompatActivity(), FragmentDelegate, ViewModelStoreOwner {
    private lateinit var mBroadcastsListFragment: BroadcastsListFragment

    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            mBroadcastsListFragment = BroadcastsListFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, mBroadcastsListFragment)
                .commitNow()
        }
        configureViewModel()
        logInIfNeeded(savedInstanceState)
    }

    /**
        Menu
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.activity_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val viewModel: MainViewModel by viewModels()
        when (item.itemId) {
            R.id.menu_refresh -> viewModel.fetchOfAllBroadcasts()
            R.id.menu_accounts -> {
                viewModel.startSelectAccountActivity()
                return true
            }
            R.id.menu_logout -> {
                viewModel.logOut()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
        Parse Activity Results
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val viewModel: MainViewModel by viewModels()
        viewModel.handleActivitiesResults(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AccountName.saveName(this, AccountName.getName(this), outState)
    }

    override fun onConnected(connectedAccountName: String?) {
        val viewModel: MainViewModel by viewModels()
        viewModel.fetchOfAllBroadcasts()
    }

    override fun onBackPressed() {
        Log.d(Config.APP_NAME, "onBackPressed")
    }

    override fun onGetImageLoader(): ImageLoader? {
        Log.d(Config.APP_NAME, "onGetImageLoader")
        return mImageLoader
    }

    override fun onEventSelected(liveBroadcast: LiveBroadcastItem?) {
        Log.d(Config.APP_NAME, "onEventSelected")
        if (liveBroadcast != null) {
            val viewModel: MainViewModel by viewModels()
            viewModel.startStreaming(liveBroadcast)
        }
    }

    fun createEvent(context: View?) {
        val viewModel: MainViewModel by viewModels()
        viewModel.createNewBroadcast()
    }

    fun didfetchOfAllBroadcasts(fetchedLiveBroadcastItems: List<LiveBroadcastItem>?) {
        if (fetchedLiveBroadcastItems == null) return
        Log.i(Config.APP_NAME, "didfetchOfAllBroadcasts=$fetchedLiveBroadcastItems")
        mBroadcastsListFragment.setEvents(fetchedLiveBroadcastItems)
    }

    fun startBroadcastStreaming(broadcastId: String, ingestionAddress: String) {
        val intent = Intent(
            applicationContext,
            VideoStreamingActivity::class.java
        )
        intent.putExtra(YouTubeLiveBroadcastRequest.RTMP_URL_KEY, ingestionAddress)
        intent.putExtra(YouTubeLiveBroadcastRequest.BROADCAST_ID_KEY, broadcastId)
        startActivityForResult(intent, MainViewModel.REQUEST_STREAMER)
    }

    fun startAuthorization(intent: Intent) {
        startActivityForResult(intent, MainViewModel.REQUEST_AUTHORIZATION)
    }

    fun startAccountPicker(intent: Intent) {
        startActivityForResult(intent, MainViewModel.REQUEST_ACCOUNT_PICKER)
    }

    /**
     * Show Error Dialog
     */
    fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val googleAPI = GoogleApiAvailability.getInstance()
            val dialog: Dialog = googleAPI.getErrorDialog(
                this@MainActivity,
                connectionStatusCode,
                MainViewModel.REQUEST_GOOGLE_PLAY_SERVICES
            )
            dialog.show()
        }
    }

    private fun configureViewModel() {
        val viewModel: MainViewModel by viewModels()
        viewModel.signInConnectDelegate = mBroadcastsListFragment
        viewModel.viewDelegate = this
    }

    private fun logInIfNeeded(savedInstanceState: Bundle?) {
        val viewModel: MainViewModel by viewModels()
        viewModel.signIn(applicationContext, savedInstanceState)
    }
}
