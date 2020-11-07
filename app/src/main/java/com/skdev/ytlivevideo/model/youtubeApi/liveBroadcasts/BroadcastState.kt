package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts

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


//            Valid values for this property are:
//            abandoned – This broadcast was never started.
//            complete – The broadcast is finished.
//            created – The broadcast has incomplete settings, so it is not ready to transition to a live or testing status, but it has been created and is otherwise valid.
//            live – The broadcast is active.
//            liveStarting – The broadcast is in the process of transitioning to live status.
//            ready – The broadcast settings are complete and the broadcast can transition to a live or testing status.
//            reclaimed – This broadcast has been reclaimed.
//            revoked – This broadcast was removed by an admin action.
//            testStarting – The broadcast is in the process of transitioning to testing status.
//            testing – The broadcast is only visible to the partner.
@Parcelize
enum class BroadcastLifeCycleStatus: Parcelable {
    Abandoned,
    Complete,
    Created,
    Live,
    LiveStarting,
    Ready,
    Reclaimed,
    Revoked,
    TestStarting,
    Testing;
}
