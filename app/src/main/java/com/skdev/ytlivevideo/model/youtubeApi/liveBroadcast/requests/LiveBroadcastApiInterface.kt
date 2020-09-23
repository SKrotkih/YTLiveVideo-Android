package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.requests
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem

interface LiveBroadcastApiInterface {
    fun onAuthException(e: UserRecoverableAuthIOException)
    fun onCompletion(items: List<LiveBroadcastItem>)
}
