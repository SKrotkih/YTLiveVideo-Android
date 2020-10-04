package com.skdev.ytlivevideo.ui.broadcastPreview
import androidx.lifecycle.ViewModel

class ViewModel : ViewModel() {

    var broadcastId: String? = null

    companion object {
        private val TAG = ViewModel::class.java.name
    }
}
