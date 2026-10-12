/* Copyright (c) 2014 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skdev.ytlivevideo.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Toast
import com.skdev.ytlivevideo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Class containing some static utility methods.
 */
object Utils {
    private var camera: Camera? = null

    fun getCamera(cameraType: Int): Camera? {
        if (camera == null) {
            val cameraInfo = CameraInfo()
            for (i in 0 until Camera.getNumberOfCameras()) {
                Camera.getCameraInfo(i, cameraInfo)
                if (cameraInfo.facing == cameraType) {
                    try {
                        camera = Camera.open(i)
                    } catch (e: RuntimeException) {
                        Log.e(Config.APP_NAME, String.format("Couldn't open camera type '%d'.", cameraType), e)
                    }
                }
            }
            if (camera == null) {
                camera = Camera.open()
            }
        }
        return camera
    }

    fun releaseCamera() {
        camera!!.release()
        camera = null
    }

    /**
     * Shows an error alert dialog with the given message.
     *
     * @param context activity
     * @param message  message to show or `null` for none
     */
    fun showError(context: Context, message: String?) {
        val errorMessage = getErrorMessage(context, message)
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun getErrorMessage(context: Context, message: String?): String {
        val resources = context.resources
        return if (message == null) {
            resources.getString(R.string.error)
        } else resources.getString(R.string.error_format, message)
    }

    class SafeClickListener(
        private var defaultInterval: Int = 1000,
        private val onSafeCLick: (View) -> Unit
    ) : View.OnClickListener {
        private var lastTimeClicked: Long = 0
        override fun onClick(v: View) {
            if (SystemClock.elapsedRealtime() - lastTimeClicked < defaultInterval) {
                return
            }
            lastTimeClicked = SystemClock.elapsedRealtime()
            onSafeCLick(v)
        }
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    /**
     * Check that all given permissions have been granted by verifying that each entry in the
     * given array is of the value [PackageManager.PERMISSION_GRANTED].
     *
     * @see Activity.onRequestPermissionsResult
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        // At least one result must be checked.
        if (grantResults.isEmpty()) {
            return false
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Global Application instance
     */
    class LaunchedApp: Application() {

        init {
            instance = this
        }

        companion object {
            private var instance: LaunchedApp? = null

            fun applicationContext() : Context {
                return instance!!.applicationContext
            }
        }
    }
}