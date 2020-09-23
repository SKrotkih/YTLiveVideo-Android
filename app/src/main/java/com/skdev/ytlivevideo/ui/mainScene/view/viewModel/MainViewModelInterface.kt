package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem

interface MainViewModelInterface {
    fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent)
    fun startSelectAccountActivity()
    fun sighIn(context: Context, savedInstanceState: Bundle?)
    fun fetchLiveBroadcastItems()
    fun createEvent()
    fun startStreaming(liveBroadcastItem: LiveBroadcastItem)
}