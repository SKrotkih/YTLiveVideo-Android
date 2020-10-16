/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skdev.ytlivevideo.ui.youtubePlayer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import com.google.android.youtube.player.YouTubePlayer
import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener
import com.google.android.youtube.player.YouTubePlayerView
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.util.DeveloperKey
import com.skdev.ytlivevideo.youtubeapidemo.YouTubeFailureRecoveryActivity

/**
 * Sample activity showing how to properly enable custom fullscreen behavior.
 *
 *
 * This is the preferred way of handling fullscreen because the default fullscreen implementation
 * will cause re-buffering of the video.
 */
internal class YouTubePlayerActivity : YouTubeFailureRecoveryActivity(), View.OnClickListener,
    CompoundButton.OnCheckedChangeListener, OnFullscreenListener {
    private var baseLayout: LinearLayout? = null
    private var playerView: YouTubePlayerView? = null
    private var player: YouTubePlayer? = null
    private var fullscreenButton: Button? = null
    private var checkbox: CompoundButton? = null
    private var otherViews: View? = null
    private var fullscreen = false
    lateinit var broadcastId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_youtube_fullscreen_player)
        extractParams()
        baseLayout = findViewById<View>(R.id.layout) as LinearLayout
        playerView = findViewById<View>(R.id.player) as YouTubePlayerView
        fullscreenButton = findViewById<View>(R.id.fullscreen_button) as Button
        checkbox = findViewById<View>(R.id.landscape_fullscreen_checkbox) as CompoundButton
        otherViews = findViewById(R.id.other_views)
        checkbox!!.setOnCheckedChangeListener(this)
        // You can use your own button to switch to fullscreen too
        fullscreenButton!!.setOnClickListener(this)
        playerView!!.initialize(DeveloperKey.DEVELOPER_KEY, this)
        doLayout()
    }

    override fun onInitializationSuccess(
        provider: YouTubePlayer.Provider, player: YouTubePlayer,
        wasRestored: Boolean
    ) {
        this.player = player
        setControlsEnabled()
        // Specify that we want to handle fullscreen behavior ourselves.
        player.addFullscreenControlFlag(YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT)
        player.setOnFullscreenListener(this)
        if (!wasRestored) {
            player.cueVideo(broadcastId)
        }
    }

    private fun extractParams() {
        broadcastId = intent.getStringExtra("broadcastId").toString()
    }

    override fun getYouTubePlayerProvider(): YouTubePlayer.Provider {
        return playerView!!
    }

    override fun onClick(v: View) {
        player!!.setFullscreen(!fullscreen)
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        var controlFlags = player!!.fullscreenControlFlags
        if (isChecked) {
            // If you use the FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE, your activity's normal UI
            // should never be laid out in landscape mode (since the video will be fullscreen whenever the
            // activity is in landscape orientation). Therefore you should set the activity's requested
            // orientation to portrait. Typically you would do this in your AndroidManifest.xml, we do it
            // programmatically here since this activity demos fullscreen behavior both with and without
            // this flag).
            requestedOrientation = PORTRAIT_ORIENTATION
            controlFlags = controlFlags or YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR
            controlFlags = controlFlags and YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE.inv()
        }
        player!!.fullscreenControlFlags = controlFlags
    }

    private fun doLayout() {
        val playerParams = playerView!!.layoutParams as LinearLayout.LayoutParams
        if (fullscreen) {
            // When in fullscreen, the visibility of all other views than the player should be set to
            // GONE and the player should be laid out across the whole screen.
            playerParams.width = LinearLayout.LayoutParams.MATCH_PARENT
            playerParams.height = LinearLayout.LayoutParams.MATCH_PARENT
            otherViews!!.visibility = View.GONE
        } else {
            // This layout is up to you - this is just a simple example (vertically stacked boxes in
            // portrait, horizontally stacked in landscape).
            otherViews!!.visibility = View.VISIBLE
            val otherViewsParams = otherViews!!.layoutParams
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                otherViewsParams.width = 0
                playerParams.width = otherViewsParams.width
                playerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                otherViewsParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                playerParams.weight = 1f
                baseLayout!!.orientation = LinearLayout.HORIZONTAL
            } else {
                otherViewsParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                playerParams.width = otherViewsParams.width
                playerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                playerParams.weight = 0f
                otherViewsParams.height = 0
                baseLayout!!.orientation = LinearLayout.VERTICAL
            }
            setControlsEnabled()
        }
    }

    private fun setControlsEnabled() {
        checkbox!!.isEnabled = (player != null
                && resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT)
        fullscreenButton!!.isEnabled = player != null
    }

    override fun onFullscreen(isFullscreen: Boolean) {
        fullscreen = isFullscreen
        doLayout()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        doLayout()
    }

    companion object {
        private val PORTRAIT_ORIENTATION =
            if (Build.VERSION.SDK_INT < 9) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
    }
}