package com.skdev.ytlivevideo.ui.mainScene.view.viewModel

import android.content.Intent
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.BroadcastState

interface MainViewModelInterface {
    fun handleActivitiesResults(requestCode: Int, resultCode: Int, data: Intent?)
    fun startSelectAccountActivity()
    fun logOut()
    fun fetchBroadcasts(state: BroadcastState)
}