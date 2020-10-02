package com.skdev.ytlivevideo.ui.mainScene.fragment

import com.android.volley.toolbox.ImageLoader
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem

interface FragmentDelegate {
    fun onGetImageLoader(): ImageLoader?
    fun didUserSelectBroadcastItem(liveBroadcastItem: LiveBroadcastItem?)
    fun renderView()
}
