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
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.viewpager.widget.ViewPager
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.tabs.TabLayout
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.network.DownLoadImageTask
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.mainScene.adapter.SectionsPagerAdapter
import com.skdev.ytlivevideo.ui.mainScene.fragment.FragmentDelegate
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import com.skdev.ytlivevideo.ui.router.Router
import com.skdev.ytlivevideo.util.Config
import kotlinx.android.synthetic.main.activity_main.*
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

    lateinit var viewModel: MainViewModel

    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        setContentView(R.layout.activity_main)
        configureTabBar()
        configureViewModel()
        logInIfNeeded(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        Router.currentContext = this
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
            R.id.create_event -> {
                Router.StartActivity.CREATE_BROADCAST.run()
            }
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
     * Tab Bar
     */
    private fun configureTabBar() {
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        val adapter = SectionsPagerAdapter(supportFragmentManager)
        viewModel.setupViewPager(viewPager, adapter)
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)
    }

    /**
        Parse Activity Results
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        viewModel.handleActivitiesResults(requestCode, resultCode, data)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AccountName.saveName(this, AccountName.getName(this), outState)
    }

    override fun renderView() {
        display_name.text = viewModel.getAccountName()
        val photoUri = viewModel.getPhotoUrl()
        if (photoUri == null) {
            avatar.setImageDrawable(null)
        } else {
            DownLoadImageTask(avatar).execute(photoUri.toString())
        }
    }

    override fun onBackPressed() {
        Log.d(Config.APP_NAME, "onBackPressed")
    }

    override fun onGetImageLoader(): ImageLoader? {
        Log.d(Config.APP_NAME, "onGetImageLoader")
        return mImageLoader
    }

    override fun didUserSelectBroadcastItem(liveBroadcast: LiveBroadcastItem?) {
        Log.d(Config.APP_NAME, "didUserSelectBroadcastItem")
        if (liveBroadcast != null) {
            Router.StartActivity.EVENT_PREVIEW.run(liveBroadcast)
        }
    }

    fun startAuthorization(intent: Intent) {
        startActivityForResult(intent, Config.REQUEST_AUTHORIZATION)
    }

    fun startAccountPicker(intent: Intent) {
        startActivityForResult(intent, Config.REQUEST_ACCOUNT_PICKER)
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
                Config.REQUEST_GOOGLE_PLAY_SERVICES
            )
            dialog.show()
        }
    }

    private fun configureViewModel() {
        viewModel.viewDelegate = this
    }

    private fun logInIfNeeded(savedInstanceState: Bundle?) {
        viewModel.signIn(applicationContext, savedInstanceState)
    }
}
