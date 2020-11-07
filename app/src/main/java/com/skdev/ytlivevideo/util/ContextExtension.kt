@file:Suppress("unused")

package com.skdev.ytlivevideo.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Build
import android.view.WindowManager
import java.util.*

/**
 * Usage:
 *   val vn = versionName ?: "Unknown"
 *   val vc = versionCode?.toString() ?: "Unknown"
 *   val appVersion = "App Version: $vn ($vc)"
 */
val Context.versionName: String?
    get() = try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        pInfo?.versionName
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }

val Context.versionCode: Long?
    get() = try {
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo?.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo?.versionCode?.toLong()
        }
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        null
    }

/**
 * Log.d(TAG, "User's screen size: ${screenSize.x}x${screenSize.y}")
 */
@Suppress("DEPRECATION")
val Context.screenSize: Point
    get() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size
    }

/**
 * Log.d(TAG, "User's device: $deviceName")
 */
val Any.deviceName: String
    get() {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer))
            model.capitalize(Locale.getDefault())
        else
            manufacturer.capitalize(Locale.getDefault()) + " " + model
    }
