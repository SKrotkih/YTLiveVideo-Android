package com.skdev.ytlivevideo.util

import java.text.SimpleDateFormat
import java.util.*

/**
 * Convert date to string format date which used on server
 */
fun Date.formattedToServerString(): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSz", Locale.US)
    return dateFormat.format(this).replace("GMT", "")
}

fun Date.toString(format: String? = null): String {
    val dateFormatter = SimpleDateFormat(format ?: "dd/M/yyyy hh:mm:ss", Locale.US)
    return dateFormatter.format(this)
}

fun Date.currentDate() : String {
    return Date().toString()
}

/**
 * Usage
 *    val json = JSONObject();
 *    json.put("date", 1598435781)
 *    val date = json.getIntOrNull("date")?.asDate
 */
fun Int.toDate(): Date = Date(this.toLong() * 1000L)

val Int.asDate: Date
    get() = Date(this.toLong() * 1000L)
