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
package com.skdev.ytlivevideo.ui.mainScene.view

import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import com.skdev.ytlivevideo.ui.videoStreamingScene.VideoStreamingActivity
import com.skdev.ytlivevideo.util.Config
import com.skdev.ytlivevideo.util.Utils
import java.io.IOException
import kotlin.math.abs

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * PreviewVideo class which previews the camera.
 */
internal class PreviewVideo(context: Context?, attributes: AttributeSet?) : ViewGroup(context, attributes),
    SurfaceHolder.Callback {

    private var surfaceView: SurfaceView = SurfaceView(context)

    var surfaceHolder: SurfaceHolder
    var previewSize: Camera.Size? = null
    var supportedPreviewSizes: List<Camera.Size>? = null

    var camera: Camera? = null
        set(value) {
            field = value
            if (camera != null) {
                supportedPreviewSizes = camera!!.parameters.supportedPreviewSizes
                for (s in supportedPreviewSizes!!) {
                    Log.d(Config.APP_NAME, String.format("Supported size: %dw x %dh", s.width, s.height))
                }
                requestLayout()
            }
        }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // TODO: This shouldn't be hardcoded.
        val width: Int =
            VideoStreamingActivity.CAMERA_WIDTH // = resolveSize(getSuggestedMinimumWidth(), VideoStreamingActivity.CAMERA_WIDTH);
        val height: Int =
            VideoStreamingActivity.CAMERA_HEIGHT // = resolveSize(getSuggestedMinimumHeight(), VideoStreamingActivity.CAMERA_HEIGHT);
        setMeasuredDimension(width, height)
        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (changed && childCount > 0) {
            val child = getChildAt(0)
            val width = r - l
            val height = b - t
            var previewWidth = width
            var previewHeight = height
            if (previewSize != null) {
                previewWidth = previewSize!!.width
                previewHeight = previewSize!!.height
            }

            // Center the child SurfaceView within the parent.
            if (width * previewHeight > height * previewWidth) {
                val scaledChildWidth = previewWidth * height / previewHeight
                child.layout(
                    (width - scaledChildWidth) / 2, 0,
                    (width + scaledChildWidth) / 2, height
                )
            } else {
                val scaledChildHeight = previewHeight * width / previewWidth
                child.layout(
                    0, (height - scaledChildHeight) / 2, width,
                    (height + scaledChildHeight) / 2
                )
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // The Surface has been created, acquire the camera and tell it where
        // to draw.
        try {
            camera = Utils.getCamera(Camera.CameraInfo.CAMERA_FACING_FRONT)
            if (camera != null) {
                camera!!.setPreviewDisplay(holder)
            }
        } catch (exception: IOException) {
            Log.e(Config.APP_NAME, "IOException caused by setPreviewDisplay()", exception)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // Surface will be destroyed when we return, so stop the preview.
        if (camera != null) {
            try {
                camera!!.setPreviewDisplay(null)
            } catch (e: IOException) {
                Log.e(Config.APP_NAME, "Caught IOException", e)
            }
        }
    }

    private fun getOptimalPreviewSize(sizes: List<Camera.Size>?, w: Int, h: Int): Camera.Size? {
        val aspectTolerance = 0.1
        val targetRatio = w.toDouble() / h
        if (sizes == null) return null
        var optimalSize: Camera.Size? = null
        var minDiff = Double.MAX_VALUE

        // Try to find an size match aspect ratio and size
        for (size in sizes) {
            val ratio = size.width.toDouble() / size.height
            if (abs(ratio - targetRatio) > aspectTolerance) continue
            if (abs(size.height - h) < minDiff) {
                optimalSize = size
                minDiff = abs(size.height - h).toDouble()
            }
        }

        // Cannot find the one match the aspect ratio, ignore the requirement
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE
            for (size in sizes) {
                if (abs(size.height - h) < minDiff) {
                    optimalSize = size
                    minDiff = abs(size.height - h).toDouble()
                }
            }
        }
        return optimalSize
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        if (camera != null) {
            // Now that the size is known, set up the camera parameters and begin
            // the preview.
            val parameters = camera!!.parameters
            parameters.setPreviewSize(VideoStreamingActivity.CAMERA_WIDTH, VideoStreamingActivity.CAMERA_HEIGHT)
            requestLayout()
            camera!!.parameters = parameters
            try {
                camera!!.setPreviewDisplay(holder)
            } catch (e: IOException) {
                Log.e(Config.APP_NAME, "", e)
            }
            camera!!.startPreview()
        }
    }

    init {
        addView(surfaceView)
        surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
}