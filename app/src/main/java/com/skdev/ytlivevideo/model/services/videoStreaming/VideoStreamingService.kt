/*
 * Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.skdev.ytlivevideo.model.services.videoStreaming

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.Binder
import android.os.IBinder
import android.util.Log
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.Utils

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * VideoStreamingService class which streams the video from camera.
 */
class VideoStreamingService : Service() {
    private val binder: IBinder = LocalBinder()

    // Member variables.
    private var connection: VideoStreamingConnection? = null

    private var camera: Camera? = null

    override fun onCreate() {
        Log.d(Config.APP_NAME, "onCreate")
    }

    override fun onDestroy() {
        Log.d(Config.APP_NAME, "onDestroy")
    }

    override fun onBind(intent: Intent): IBinder? {
        camera = Utils.getCamera(CameraInfo.CAMERA_FACING_FRONT)
        Log.d(Config.APP_NAME, "onBind")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(Config.APP_NAME, "onUnbind")
        return false
    }

    fun startStreaming(streamUrl: String?) {
        Log.d(Config.APP_NAME, "startStreaming")
        showForegroundNotification()
        connection = VideoStreamingConnection()
        // TODO Pass an actual preview surface.
        connection!!.open(streamUrl, camera, null)
    }

    fun stopStreaming() {
        Log.d(Config.APP_NAME, "stopStreaming")
        connection?.close()
        connection = null
        stopForeground(true)
    }

    val isStreaming: Boolean
        get() = connection != null

    fun releaseCamera() {
        Log.d(Config.APP_NAME, "releaseCamera")
        if (!isStreaming && camera != null) {
            Utils.releaseCamera()
            Log.d(Config.APP_NAME, "Camera was released.")
            camera = null
        } else {
            Log.d(Config.APP_NAME, "Camera was not released.")
        }
    }

    private fun showForegroundNotification() {
        val notifyManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Intent to call our activity from background.
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Intent.ACTION_MAIN
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER)

        // The PendingIntent to launch our activity if the user selects this notification.
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT
        )
        val notification: Notification = Notification.Builder(applicationContext)
            .setContentTitle(getText(R.string.activeStreamingLabel))
            .setContentText(getText(R.string.activeStreamingStatus))
            .setContentIntent(contentIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setWhen(System.currentTimeMillis())
            .build()
        notifyManager.notify(STREAMER_NOTIFICATION_ID, notification)
    }

    inner class LocalBinder : Binder() {
        val service: VideoStreamingService
            get() = this@VideoStreamingService
    }

    companion object {
        private const val STREAMER_NOTIFICATION_ID = 1001
    }
}