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
import android.content.pm.PackageManager
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.util.Log
import android.widget.Toast
import com.skdev.ytlivevideo.ui.mainScene.view.MainActivity
import com.skdev.ytlivevideo.R
import com.google.api.client.googleapis.json.GoogleJsonResponseException

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
     * Logs the given throwable and shows an error alert dialog with its message.
     *
     * @param activity activity
     * @param tag      log tag to use
     * @param t        throwable to log and show
     */
    fun logAndShow(activity: Activity, tag: String?, t: Throwable) {
        Log.e(tag, "Error", t)
        var message = t.message
        if (t is GoogleJsonResponseException) {
            val details = t.details
            if (details != null) {
                message = details.message
            }
        } else if (t.cause?.message != null) {
            message = t.cause!!.message
        }
        showError(activity, message)
    }

    /**
     * Logs the given message and shows an error alert dialog with it.
     *
     * @param activity activity
     * @param tag      log tag to use
     * @param message  message to log and show or `null` for none
     */
    fun logAndShowError(activity: Activity, tag: String?, message: String?) {
        val errorMessage = getErrorMessage(activity, message)
        Log.e(tag, errorMessage)
        showErrorInternal(activity, errorMessage)
    }

    /**
     * Shows an error alert dialog with the given message.
     *
     * @param activity activity
     * @param message  message to show or `null` for none
     */
    fun showError(activity: Activity, message: String?) {
        val errorMessage = getErrorMessage(activity, message)
        showErrorInternal(activity, errorMessage)
    }

    private fun showErrorInternal(activity: Activity, errorMessage: String) {
        activity.runOnUiThread { Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show() }
    }

    private fun getErrorMessage(activity: Activity, message: String?): String {
        val resources = activity.resources
        return if (message == null) {
            resources.getString(R.string.error)
        } else resources.getString(R.string.error_format, message)
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
}