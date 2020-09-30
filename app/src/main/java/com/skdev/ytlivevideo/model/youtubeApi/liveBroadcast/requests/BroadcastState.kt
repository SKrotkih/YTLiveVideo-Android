package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

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
