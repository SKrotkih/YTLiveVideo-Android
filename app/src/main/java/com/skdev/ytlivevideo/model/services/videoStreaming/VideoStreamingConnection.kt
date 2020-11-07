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

import android.hardware.Camera
import android.util.Log
import android.view.Surface
import com.skdev.ytlivevideo.media.audio.AudioFrameGrabber
import com.skdev.ytlivevideo.media.Ffmpeg.encodeAudioFrame
import com.skdev.ytlivevideo.media.Ffmpeg.encodeVideoFrame
import com.skdev.ytlivevideo.media.Ffmpeg.init
import com.skdev.ytlivevideo.media.Ffmpeg.shutdown
import com.skdev.ytlivevideo.media.video.VideoFrameGrabber
import com.skdev.ytlivevideo.util.Config

class VideoStreamingConnection : VideoStreamingInterface {
    // Member variables.
    private var videoFrameGrabber: VideoFrameGrabber? = null
    private var audioFrameGrabber: AudioFrameGrabber? = null
    private val frameMutex = Any()
    private var encoding = false

    override fun open(url: String?, camera: Camera?, previewSurface: Surface?) {
        Log.d(Config.APP_NAME, "open")
        videoFrameGrabber = VideoFrameGrabber()
        videoFrameGrabber!!.setFrameCallback(object : VideoFrameGrabber.FrameCallback {
            override fun handleFrame(yuv_image: ByteArray?) {
                if (encoding) {
                    synchronized(frameMutex) { encodeVideoFrame(yuv_image) }
                }
            }
        })
        audioFrameGrabber = AudioFrameGrabber()
        audioFrameGrabber!!.setFrameCallback(object : AudioFrameGrabber.FrameCallback {
            override fun handleFrame(audioData: ShortArray?, length: Int) {
                if (encoding) {
                    synchronized(frameMutex) { encodeAudioFrame(audioData, length) }
                }
            }
        })
        synchronized(frameMutex) {
            val previewSize = videoFrameGrabber!!.start(camera!!)
            audioFrameGrabber!!.start(AUDIO_SAMPLE_RATE)
            val width = previewSize.width
            val height = previewSize.height
            encoding = init(width, height, AUDIO_SAMPLE_RATE, url)
            Log.d(Config.APP_NAME, "Ffmpeg.init() returned $encoding")
        }
    }

    override fun close() {
        Log.d(Config.APP_NAME, "close")
        videoFrameGrabber?.stop()
        audioFrameGrabber?.stop()
        encoding = false
        if (encoding) {
            shutdown()
        }
    }

    companion object {
        // CONSTANTS.
        private const val AUDIO_SAMPLE_RATE = 44100
    }
}