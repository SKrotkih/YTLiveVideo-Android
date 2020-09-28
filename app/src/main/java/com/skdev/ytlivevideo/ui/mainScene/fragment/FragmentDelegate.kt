package com.skdev.ytlivevideo.ui.mainScene.fragment

import com.android.volley.toolbox.ImageLoader
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem

interface FragmentDelegate {
    fun onGetImageLoader(): ImageLoader?
    fun onEventSelected(liveBroadcastItem: LiveBroadcastItem?)
    fun onConnected(connectedAccountName: String?)
}
