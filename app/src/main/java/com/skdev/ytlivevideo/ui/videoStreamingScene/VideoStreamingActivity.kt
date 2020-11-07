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
package com.skdev.ytlivevideo.ui.videoStreamingScene

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.View
import android.widget.ToggleButton
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.services.videoStreaming.VideoStreamingService
import com.skdev.ytlivevideo.util.Utils
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastsInteractor
import java.util.*
import com.skdev.ytlivevideo.model.services.videoStreaming.VideoStreamingService.LocalBinder
import com.skdev.ytlivevideo.ui.mainScene.view.PreviewVideo
import com.skdev.ytlivevideo.util.Config

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * VideoStreamingActivity class which previews the camera and streams via VideoStreamingService.
 */
class VideoStreamingActivity : Activity() {
    // Member variables
    private var videoStreamingService: VideoStreamingService? = null
    private var wakeLock: WakeLock? = null
    private var previewVideo: PreviewVideo? = null
    private var rtmpUrl: String? = null

    private val streamerConnection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(Config.APP_NAME, "onServiceConnected")
            videoStreamingService = (service as LocalBinder).service
            restoreStateFromService()
            startStreaming()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.e(Config.APP_NAME, "onServiceDisconnected")

            // This should never happen, because our service runs in the same process.
            videoStreamingService = null
        }
    }
    private var broadcastId: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(Config.APP_NAME, "onCreate")
        super.onCreate(savedInstanceState)
        broadcastId = intent.getStringExtra(LiveBroadcastsInteractor.BROADCAST_ID_KEY)
        rtmpUrl = intent.getStringExtra(LiveBroadcastsInteractor.RTMP_URL_KEY)
        if (rtmpUrl == null) {
            Log.w(Config.APP_NAME, "No RTMP URL was passed in; bailing.")
            finish()
        }
        Log.d(Config.APP_NAME, String.format("Got RTMP URL '%s' from calling activity.", rtmpUrl))
        setContentView(R.layout.activity_video_streaming)
        previewVideo = findViewById<View>(R.id.surfaceViewPreview) as PreviewVideo
        if (!bindService(
                Intent(this, VideoStreamingService::class.java), streamerConnection!!,
                BIND_AUTO_CREATE or BIND_DEBUG_UNBIND
            )
        ) {
            Log.e(Config.APP_NAME, "Failed to bind VideoStreamingService!")
        }
        val toggleButton = findViewById<View>(R.id.toggleBroadcasting) as ToggleButton
        toggleButton.setOnClickListener {
            if (toggleButton.isChecked) {
                videoStreamingService!!.startStreaming(rtmpUrl)
            } else {
                videoStreamingService!!.stopStreaming()
            }
        }
    }

    override fun onResume() {
        Log.d(Config.APP_NAME, "onResume")
        super.onResume()
        if (videoStreamingService != null) {
            restoreStateFromService()
        }
    }

    override fun onPause() {
        Log.d(Config.APP_NAME, "onPause")
        super.onPause()
        if (previewVideo != null) {
            previewVideo!!.camera = null
        }
        if (videoStreamingService != null) {
            videoStreamingService!!.releaseCamera()
        }
    }

    override fun onDestroy() {
        Log.d(Config.APP_NAME, "onDestroy")
        super.onDestroy()
        streamerConnection?.let { unbindService(it) }
        stopStreaming()
        if (videoStreamingService != null) {
            videoStreamingService!!.releaseCamera()
        }
    }

    private fun restoreStateFromService() {
        previewVideo!!.camera = Utils.getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
    }

    private fun startStreaming() {
        Log.d(Config.APP_NAME, "startStreaming")
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.javaClass.name)
        wakeLock!!.acquire()
        if (!videoStreamingService!!.isStreaming) {
            val cameraPermission = Manifest.permission.CAMERA
            val microphonePermission = Manifest.permission.RECORD_AUDIO
            val hasCamPermission = checkSelfPermission(cameraPermission)
            val hasMicPermission = checkSelfPermission(microphonePermission)
            val permissions: MutableList<String> = ArrayList()
            if (hasCamPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(cameraPermission)
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                    )
                ) {
                    // Provide rationale in Snackbar to request permission
                    Snackbar.make(
                        previewVideo!!, R.string.permission_camera_rationale,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                } else {
                    // Explain in Snackbar to turn on permission in settings
                    Snackbar.make(
                        previewVideo!!, R.string.permission_camera_explain,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            }
            if (hasMicPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(microphonePermission)
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.RECORD_AUDIO
                    )
                ) {
                    // Provide rationale in Snackbar to request permission
                    Snackbar.make(
                        previewVideo!!, R.string.permission_microphone_rationale,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                } else {
                    // Explain in Snackbar to turn on permission in settings
                    Snackbar.make(
                        previewVideo!!, R.string.permission_microphone_explain,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            }
            if (permissions.isNotEmpty()) {
                val params = permissions.toTypedArray()
                ActivityCompat.requestPermissions(this, params, REQUEST_CAMERA_MICROPHONE)
            } else {
                // We already have permission, so handle as normal
                videoStreamingService!!.startStreaming(rtmpUrl)
            }
        }
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CAMERA_MICROPHONE -> {
                Log.d(Config.APP_NAME, "Received response for camera with mic permissions request.")

                // We have requested multiple permissions for contacts, so all of them need to be
                // checked.
                if (Utils.verifyPermissions(grantResults)) {
                    // permissions were granted, yay! do the
                    // activity_video_streaming task you need to do.
                    videoStreamingService!!.startStreaming(rtmpUrl)
                } else {
                    Log.d(Config.APP_NAME, "Camera with mic permissions were NOT granted.")
                    Snackbar.make(
                        previewVideo!!, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        return
    }

    private fun stopStreaming() {
        Log.d(Config.APP_NAME, "stopStreaming")
        if (wakeLock != null) {
            wakeLock!!.release()
            wakeLock = null
        }
        if (videoStreamingService!!.isStreaming) {
            videoStreamingService!!.stopStreaming()
        }
    }

    fun endEvent(view: View?) {
        val data = Intent()
        data.putExtra(LiveBroadcastsInteractor.BROADCAST_ID_KEY, broadcastId)
        if (parent == null) {
            setResult(RESULT_OK, data)
        } else {
            parent.setResult(RESULT_OK, data)
        }
        finish()
    }

    companion object {
        // CONSTANTS
        // TODO: Stop hardcoding this and read values from the camera's supported sizes.
        const val CAMERA_WIDTH = 640
        const val CAMERA_HEIGHT = 480
        private const val REQUEST_CAMERA_MICROPHONE = 0
    }
}