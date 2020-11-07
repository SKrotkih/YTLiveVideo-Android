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
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.googleAccount.GoogleSignInManager
import com.skdev.ytlivevideo.model.network.DownLoadImageTask
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.mainScene.adapter.SectionsPagerAdapter
import com.skdev.ytlivevideo.ui.mainScene.fragment.FragmentDelegate
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import com.skdev.ytlivevideo.ui.router.Router
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
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

    private lateinit var viewModel: MainViewModel

    private var progressDialog: Dialog? = null

    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        window.requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configureViewModel()
        configureTabBar()
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
    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
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
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        viewModel.needSendRequestAuthorization.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                startAccountPicker(GoogleAccountManager.credential!!.newChooseAccountIntent())
            }})
        viewModel.errorMessage.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                Utils.showError(this, it)
            }})
        viewModel.invalidateView.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                invalidateOptionsMenu()
                renderView()
            }})
        viewModel.accountName.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                AccountName.saveName(this, it)
            }})
        viewModel.needToCheckGooglePlayServicesAvailable.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
            if (it) {
                val googleAPI = GoogleApiAvailability.getInstance()
                val connectionStatusCode: Int = googleAPI.isGooglePlayServicesAvailable(this)
                if (googleAPI.isUserResolvableError(connectionStatusCode)) {
                    showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode)
                }
            }}
        })
        viewModel.errorAuthorization.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                startAuthorization(it.intent)
            }})
        viewModel.startProcessing.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
                progressDialog = ProgressDialog.create(this, it)
                progressDialog?.show()
            }})
        viewModel.stopProcessing.observe(this, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {
            progressDialog?.dismiss()
        }})
    }

    private fun logInIfNeeded(savedInstanceState: Bundle?) {
        if (GoogleAccountManager.signIn(this, savedInstanceState)) {
            viewModel.signInManager = GoogleSignInManager(this)
            viewModel.signInManager.didUserSignIn.observe(this, { event ->
                event?.getContentIfNotHandledOrReturnNull()?.let {
                if (it) {
                    GoogleAccountManager.setUpGoogleAccount(viewModel.signInManager.account!!)
                    invalidateOptionsMenu()
                    renderView()
                }
            }})
            viewModel.signInManager.googleSignIn()
        } else {
            Utils.showError(this, "OAUTH2 credentials are not presented")
        }
    }
}
