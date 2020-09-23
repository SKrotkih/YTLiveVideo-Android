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
package com.skdev.ytlivevideo.media.video

import android.graphics.ImageFormat
import android.hardware.Camera
import com.skdev.ytlivevideo.ui.videoStreamingScene.VideoStreamingActivity

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * VideoFrameGrabber class which grabs video frames to buffer.
 */
class VideoFrameGrabber {
    // Member variables
    private var camera: Camera? = null
    private var frameCallback: FrameCallback? = null
    fun setFrameCallback(callback: FrameCallback?) {
        frameCallback = callback
    }

    /**
     * Starts camera recording to buffer.
     *
     * @param camera - Camera to be recorded.
     * @return preview size.
     */
    fun start(camera: Camera): Camera.Size {
        this.camera = camera
        val params = camera.parameters
        params.setPreviewSize(VideoStreamingActivity.CAMERA_WIDTH, VideoStreamingActivity.CAMERA_HEIGHT)
        camera.parameters = params
        val previewSize = params.previewSize
        val bufferSize = previewSize.width * previewSize.height * ImageFormat.getBitsPerPixel(
            params.previewFormat
        )
        camera.addCallbackBuffer(ByteArray(bufferSize))
        camera.setPreviewCallbackWithBuffer { yuv_image, camera ->
            if (frameCallback != null) {
                frameCallback!!.handleFrame(yuv_image)
            }
            camera.addCallbackBuffer(yuv_image)
        }
        return previewSize
    }

    fun stop() {
        camera!!.setPreviewCallbackWithBuffer(null)
        camera = null
    }

    interface FrameCallback {
        fun handleFrame(yuv_image: ByteArray?)
    }

    companion object {
        const val OBJ_NAME = "VideoGrabber"
    }
}