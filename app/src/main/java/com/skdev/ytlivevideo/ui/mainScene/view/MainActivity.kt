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
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.tabs.TabLayout
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.databinding.ActivityMainBinding
import com.skdev.ytlivevideo.model.enteties.AccountName
import com.skdev.ytlivevideo.model.googleAccount.GoogleAccountManager
import com.skdev.ytlivevideo.model.googleAccount.GoogleSignInManager
import com.skdev.ytlivevideo.model.network.loadImageFromUrl
import com.skdev.ytlivevideo.model.network.NetworkSingleton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.mainScene.adapter.SectionsPagerAdapter
import com.skdev.ytlivevideo.ui.mainScene.fragment.FragmentDelegate
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import com.skdev.ytlivevideo.ui.router.Router
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.ProgressDialog
import com.skdev.ytlivevideo.util.Utils
import com.skdev.ytlivevideo.util.fitContentToSystemBars
import kotlinx.coroutines.launch

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Main activity class which handles authorization and intents.
 */
class MainActivity : AppCompatActivity(), FragmentDelegate {

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: MainViewModel

    private var progressDialog: Dialog? = null

    private val mImageLoader: ImageLoader? by lazy {
        NetworkSingleton.getInstance(this)?.imageLoader
    }

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.signInManager.onSignInResult(result.data)
        }

    private val authorizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.handleActivitiesResults(Config.REQUEST_AUTHORIZATION, result.resultCode, result.data)
        }

    private val accountPickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.handleActivitiesResults(Config.REQUEST_ACCOUNT_PICKER, result.resultCode, result.data)
        }

    private val googlePlayServicesLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            viewModel.handleActivitiesResults(Config.REQUEST_GOOGLE_PLAY_SERVICES, result.resultCode, result.data)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fitContentToSystemBars()
        // The main screen is the root of the app: the Back gesture is ignored here.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Log.d(Config.APP_NAME, "onBackPressed")
            }
        })
        configureViewModel()
        configureTabBar()
        logInIfNeeded()
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        AccountName.saveName(this, AccountName.getName(this), outState)
    }

    override fun renderView() {
        binding.displayName.text = viewModel.getAccountName()
        val photoUri = viewModel.getPhotoUrl()
        if (photoUri == null) {
            binding.avatar.setImageDrawable(null)
        } else {
            lifecycleScope.launch { binding.avatar.loadImageFromUrl(photoUri.toString()) }
        }
    }

    override fun onGetImageLoader(): ImageLoader? {
        Log.d(Config.APP_NAME, "onGetImageLoader")
        return mImageLoader
    }

    override fun didUserSelectBroadcastItem(liveBroadcastItem: LiveBroadcastItem?) {
        Log.d(Config.APP_NAME, "didUserSelectBroadcastItem")
        if (liveBroadcastItem != null) {
            Router.StartActivity.EVENT_PREVIEW.run(liveBroadcastItem)
        }
    }

    fun startAuthorization(intent: Intent) {
        authorizationLauncher.launch(intent)
    }

    fun startAccountPicker(intent: Intent) {
        accountPickerLauncher.launch(intent)
    }

    /**
     * Show Error Dialog
     */
    private fun showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode: Int) {
        GoogleApiAvailability.getInstance().showErrorDialogFragment(
            this,
            connectionStatusCode,
            googlePlayServicesLauncher,
            null
        )
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

    private fun logInIfNeeded() {
        if (GoogleAccountManager.signIn(this)) {
            viewModel.signInManager = GoogleSignInManager(this)
            viewModel.signInManager.didUserSignIn.observe(this, { event ->
                event?.getContentIfNotHandledOrReturnNull()?.let {
                if (it) {
                    GoogleAccountManager.setUpGoogleAccount(viewModel.signInManager.account!!)
                    invalidateOptionsMenu()
                    renderView()
                }
            }})
            viewModel.signInManager.googleSignIn(signInLauncher)
        } else {
            Utils.showError(this, "OAUTH2 credentials are not presented")
        }
    }
}
