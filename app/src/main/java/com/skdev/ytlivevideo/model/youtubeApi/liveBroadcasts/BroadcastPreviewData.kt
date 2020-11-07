package com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts

class BroadcastPreviewData(
    var broadcastId: String,
    var streamId: String,
    var name: String,
    var description : String,
    var created : String,
    var scheduled : String,
    /**
    Valid values for this property are:
    abandoned – This broadcast was never started.
    complete – The broadcast is finished.
    created – The broadcast has incomplete settings, so it is not ready to transition to a live or testing status, but it has been created and is otherwise valid.
    live – The broadcast is active.
    liveStarting – The broadcast is in the process of transitioning to live status.
    ready – The broadcast settings are complete and the broadcast can transition to a live or testing status.
    reclaimed – This broadcast has been reclaimed.
    revoked – This broadcast was removed by an admin action.
    testStarting – The broadcast is in the process of transitioning to testing status.
    testing – The broadcast is only visible to the partner.
     */
    var lifeCycleStatus : String,

    /**
    Valid values for this property are:
    active – The stream is in active state which means the user is receiving data via the stream.
    created – The stream has been created but does not have valid CDN settings.
    error – An error condition exists on the stream.
    inactive – The stream is in inactive state which means the user is not receiving data via the stream.
    ready – The stream has valid CDN settings.
     */
    var streamStatus : String,

    var thumbUri : String,
    var watchUri : String,
    var ingestionAddress : String)
