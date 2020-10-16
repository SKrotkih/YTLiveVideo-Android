package com.skdev.ytlivevideo.ui.liveStream

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.text.TextUtils
import com.skdev.ytlivevideo.util.Alert
import com.skdev.ytlivevideo.util.Utils

/**
 * https://developers.google.com/youtube/android/live
 */
/**
 *  Launching the live streaming flow
 */
class YouTubeStreamLauncher {

  /**
   * Step 1: Check for support Mobile Live Intent
   */

  /**
   * The canResolveMobileLiveIntent method verifies that the device supports the Mobile Live Intent.
   */
  private fun canResolveMobileLiveIntent(context: Context): Boolean {
    val intent = Intent("com.google.android.youtube.intent.action.CREATE_LIVE_STREAM")
      .setPackage("com.google.android.youtube")
    val pm: PackageManager = context.packageManager
    val resolveInfo: List<*> = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    return resolveInfo.isNotEmpty()
  }

  /**
   * The validateMobileLiveIntent calls the canResolveMobileLiveIntent method in the context of an if-else statement.
      If the device supports the Intent, then the device could launch the live stream flow.
      If the device does not support the Intent, then the device could prompt the user to install or upgrade the YouTube app.
   */
  fun start(description: String) {
    val context = Utils.LaunchedApp.applicationContext()
    if (canResolveMobileLiveIntent(context)) {
      /**
       * Step 2: Launch the live stream activity
       */
      val intent = createMobileLiveIntent(context, description)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(intent)
    } else {
      Alert.showOK("Warning", "To use this ability you should install YouTube app on your device")
    }
  }

  private fun createMobileLiveIntent(context: Context, description: String): Intent {
    val intent = Intent("com.google.android.youtube.intent.action.CREATE_LIVE_STREAM")
      .setPackage("com.google.android.youtube")
    val referrer: Uri = Uri.Builder()
      .scheme("android-app")
      .appendPath(context.packageName)
      .build()
    intent.putExtra(Intent.EXTRA_REFERRER, referrer)
    if (!TextUtils.isEmpty(description)) {
      intent.putExtra(Intent.EXTRA_SUBJECT, description)
    }
    return intent
  }
}

