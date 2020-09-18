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
package com.skdev.ytlivevideo

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
import com.skdev.ytlivevideo.util.Utils
import com.skdev.ytlivevideo.util.YouTubeApi
import java.util.*
import com.skdev.ytlivevideo.StreamerService.LocalBinder

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * StreamerActivity class which previews the camera and streams via StreamerService.
 */
class StreamerActivity : Activity() {
    // Member variables
    private var streamerService: StreamerService? = null
    private var wakeLock: WakeLock? = null
    private var preview: Preview? = null
    private var rtmpUrl: String? = null

    private val streamerConnection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            Log.d(MainActivity.APP_NAME, "onServiceConnected")
            streamerService = (service as LocalBinder).service
            restoreStateFromService()
            startStreaming()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            Log.e(MainActivity.APP_NAME, "onServiceDisconnected")

            // This should never happen, because our service runs in the same process.
            streamerService = null
        }
    }
    private var broadcastId: String? = null
    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(MainActivity.APP_NAME, "onCreate")
        super.onCreate(savedInstanceState)
        broadcastId = intent.getStringExtra(YouTubeApi.BROADCAST_ID_KEY)
        rtmpUrl = intent.getStringExtra(YouTubeApi.RTMP_URL_KEY)
        if (rtmpUrl == null) {
            Log.w(MainActivity.APP_NAME, "No RTMP URL was passed in; bailing.")
            finish()
        }
        Log.i(MainActivity.APP_NAME, String.format("Got RTMP URL '%s' from calling activity.", rtmpUrl))
        setContentView(R.layout.streamer)
        preview = findViewById<View>(R.id.surfaceViewPreview) as Preview
        if (!bindService(
                Intent(this, StreamerService::class.java), streamerConnection!!,
                BIND_AUTO_CREATE or BIND_DEBUG_UNBIND
            )
        ) {
            Log.e(MainActivity.APP_NAME, "Failed to bind StreamerService!")
        }
        val toggleButton = findViewById<View>(R.id.toggleBroadcasting) as ToggleButton
        toggleButton.setOnClickListener {
            if (toggleButton.isChecked) {
                streamerService!!.startStreaming(rtmpUrl)
            } else {
                streamerService!!.stopStreaming()
            }
        }
    }

    override fun onResume() {
        Log.d(MainActivity.APP_NAME, "onResume")
        super.onResume()
        if (streamerService != null) {
            restoreStateFromService()
        }
    }

    override fun onPause() {
        Log.d(MainActivity.APP_NAME, "onPause")
        super.onPause()
        if (preview != null) {
            preview!!.camera = null
        }
        if (streamerService != null) {
            streamerService!!.releaseCamera()
        }
    }

    override fun onDestroy() {
        Log.d(MainActivity.APP_NAME, "onDestroy")
        super.onDestroy()
        streamerConnection?.let { unbindService(it) }
        stopStreaming()
        if (streamerService != null) {
            streamerService!!.releaseCamera()
        }
    }

    private fun restoreStateFromService() {
        preview!!.camera = Utils.getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
    }

    private fun startStreaming() {
        Log.d(MainActivity.APP_NAME, "startStreaming")
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, this.javaClass.name)
        wakeLock!!.acquire()
        if (!streamerService!!.isStreaming) {
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
                        preview!!, R.string.permission_camera_rationale,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                } else {
                    // Explain in Snackbar to turn on permission in settings
                    Snackbar.make(
                        preview!!, R.string.permission_camera_explain,
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
                        preview!!, R.string.permission_microphone_rationale,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                } else {
                    // Explain in Snackbar to turn on permission in settings
                    Snackbar.make(
                        preview!!, R.string.permission_microphone_explain,
                        Snackbar.LENGTH_INDEFINITE
                    ).show()
                }
            }
            if (!permissions.isEmpty()) {
                val params = permissions.toTypedArray()
                ActivityCompat.requestPermissions(this, params, REQUEST_CAMERA_MICROPHONE)
            } else {
                // We already have permission, so handle as normal
                streamerService!!.startStreaming(rtmpUrl)
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
                Log.i(MainActivity.APP_NAME, "Received response for camera with mic permissions request.")

                // We have requested multiple permissions for contacts, so all of them need to be
                // checked.
                if (Utils.verifyPermissions(grantResults)) {
                    // permissions were granted, yay! do the
                    // streamer task you need to do.
                    streamerService!!.startStreaming(rtmpUrl)
                } else {
                    Log.i(MainActivity.APP_NAME, "Camera with mic permissions were NOT granted.")
                    Snackbar.make(
                        preview!!, R.string.permissions_not_granted,
                        Snackbar.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
        return
    }

    private fun stopStreaming() {
        Log.d(MainActivity.APP_NAME, "stopStreaming")
        if (wakeLock != null) {
            wakeLock!!.release()
            wakeLock = null
        }
        if (streamerService!!.isStreaming) {
            streamerService!!.stopStreaming()
        }
    }

    fun endEvent(view: View?) {
        val data = Intent()
        data.putExtra(YouTubeApi.BROADCAST_ID_KEY, broadcastId)
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