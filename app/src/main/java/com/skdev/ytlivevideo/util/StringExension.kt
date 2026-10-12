package com.skdev.ytlivevideo.util

import android.net.Uri
import android.webkit.URLUtil
import androidx.core.net.toUri
import java.time.ZonedDateTime
import java.util.Date
import java.util.concurrent.TimeUnit

fun String.parseStringToDate(): Date {
    val zonedDate = ZonedDateTime.parse(this)
    return Date.from(zonedDate.toInstant())
}

fun String.timeAgo() : String {
    val eventDate = this.parseStringToDate()
    val date1 = Date().time
    val date2 = eventDate.time
    var diff = date1 - date2
    return if (diff > 0) {
        val diffInHours: Long = TimeUnit.MILLISECONDS.toHours(diff)
        val diffInMin: Long = TimeUnit.MILLISECONDS.toMinutes(diff) - diffInHours * 60
        "$diffInHours h $diffInMin m ago"
    } else {
        diff = date2 - date1
        val diffInHours: Long = TimeUnit.MILLISECONDS.toHours(diff)
        val diffInMin: Long = TimeUnit.MILLISECONDS.toMinutes(diff) - diffInHours * 60
        "after $diffInHours h $diffInMin m"
    }
}

/**
 * Returns the [Uri] for a valid URL or `null`.
 */
val String.asUri: Uri?
    get() = try {
        if (URLUtil.isValidUrl(this))
            toUri()
        else
            null
    } catch (e: Exception) {
        null
    }