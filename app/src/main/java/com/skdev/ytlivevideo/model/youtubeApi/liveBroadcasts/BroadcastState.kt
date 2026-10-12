package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class BroadcastState : Parcelable {
    ALL {
        override fun value() = "all"
    }, UPCOMING {
        override fun value() = "upcoming"
    }, ACTIVE {
        override fun value() = "active"
    }, COMPLETED {
        override fun value() = "completed"
    };
    abstract fun value(): String
}

