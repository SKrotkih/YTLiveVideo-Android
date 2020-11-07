package com.skdev.ytlivevideo.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import android.net.Uri
import android.webkit.URLUtil

fun String.parseStringToDate(): Date {
    val zonedDate = ZonedDateTime.parse(this)
    return Date.from(zonedDate.toInstant())
}

fun String.parseStringToLocalDate(): LocalDate? {
    // 2019-01-14T06:22:23.365Z
    val serverDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
    val dateFormatter = DateTimeFormatter.ofPattern(serverDateFormat)
    return LocalDate.parse(this, dateFormatter)
}

fun String.toDate(format: String): Date? {
    val dateFormatter = SimpleDateFormat(format, Locale.US)
    return try {
        dateFormatter.parse(this)
    } catch (e: ParseException) {
        null
    }
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

val String.containsDigit: Boolean
    get() = matches(Regex(".*[0-9].*"))

val String.isAlphanumeric: Boolean
    get() = matches(Regex("[a-zA-Z0-9]*"))

/**
 *  val uri = "invalid_uri".asUri
 *  val uri2 = "https://medium.com/@alex_nekrasov".asUri
 */
val String.asUri: Uri?
    get() = try {
        if (URLUtil.isValidUrl(this))
            Uri.parse(this)
        else
            null
    } catch (e: Exception) {
        null
    }