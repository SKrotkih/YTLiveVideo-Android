package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.ui.mainScene.fragment.BroadcastState

interface MainViewModelInterface {
    fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent?)
    fun startSelectAccountActivity()
    fun signIn(context: Context, savedInstanceState: Bundle?)
    fun logOut()
    fun fetchBroadcasts(state: BroadcastState)
    fun createNewBroadcast()
    fun startStreaming(liveBroadcastItem: LiveBroadcastItem)
}