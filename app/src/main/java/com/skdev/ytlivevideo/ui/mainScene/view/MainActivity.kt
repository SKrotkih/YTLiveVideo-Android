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

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GoogleApiAvailability
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.ui.mainScene.fragment.LiveEventsListFragment.Callbacks
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.YouTubeLiveBroadcastRequest
import com.skdev.ytlivevideo.ui.mainScene.fragment.LiveEventsListFragment
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
class MainActivity : Activity(), Callbacks {
    private val viewModel = MainViewModel(this)
    private val mLiveEventsListFragment: LiveEventsListFragment by lazy {
        fragmentManager.findFragmentById(R.id.list_fragment) as LiveEventsListFragment
    }
    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mLiveEventsListFragment.viewModel = viewModel
        viewModel.sighIn(applicationContext, savedInstanceState)
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
        when (item.itemId) {
            R.id.menu_refresh -> viewModel.fetchLiveBroadcastItems()
            R.id.menu_accounts -> {
                viewModel.startSelectAccountActivity()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    /**
        Parse Activity Results
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleActivitiesResults(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AccountName.saveName(this, AccountName.getName(this), outState)
    }

    override fun onConnected(connectedAccountName: String?) {
        viewModel.fetchLiveBroadcastItems()
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
            viewModel.startStreaming(liveBroadcast)
        }
    }

    fun createEvent(context: View?) {
        viewModel.createEvent()
    }

    fun didFetchLiveBroadcastItems(fetchedLiveBroadcastItems: List<LiveBroadcastItem>) {
        Log.i(Config.APP_NAME, "didFetchLiveBroadcastItems=$fetchedLiveBroadcastItems")
        mLiveEventsListFragment.setEvents(fetchedLiveBroadcastItems)
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
     * Show GooglePlay Services Availability Error Dialog
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
}
