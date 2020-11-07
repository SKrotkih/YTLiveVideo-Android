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
package com.skdev.ytlivevideo.media.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.skdev.ytlivevideo.util.Config

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * AudioFrameGrabber class which records audio.
 */
class AudioFrameGrabber {
    private var thread: Thread? = null
    private var cancel = false
    private var frequency = 0
    private var frameCallback: FrameCallback? = null
    fun setFrameCallback(callback: FrameCallback?) {
        frameCallback = callback
    }

    /**
     * Starts recording.
     *
     * @param frequency - Recording frequency.
     */
    fun start(frequency: Int) {
        this.frequency = frequency
        cancel = false
        thread = Thread { recordThread() }
        thread!!.start()
    }

    /**
     * Records audio and pushes to buffer.
     */
    private fun recordThread() {
        Log.d(Config.APP_NAME, "recordThread")
        val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
        val channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_STEREO
        var bufferSize = AudioRecord.getMinBufferSize(frequency, channelConfiguration, audioEncoding)
        Log.d(Config.APP_NAME, "AudioRecord buffer size: $bufferSize")

        // 16 bit PCM stereo recording was chosen as example.
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.CAMCORDER, frequency, channelConfiguration,
            audioEncoding, bufferSize
        )
        recorder.startRecording()

        // Make bufferSize be in samples instead of bytes.
        bufferSize /= 2
        val buffer = ShortArray(bufferSize)
        while (!cancel) {
            val bufferReadResult = recorder.read(buffer, 0, bufferSize)
            // Utils.Debug("bufferReadResult: " + bufferReadResult);
            if (bufferReadResult > 0) {
                frameCallback!!.handleFrame(buffer, bufferReadResult)
            } else if (bufferReadResult < 0) {
                Log.w(OBJ_NAME, "Error calling recorder.read: $bufferReadResult")
            }
        }
        recorder.stop()
    }

    /**
     * Stops recording.
     */
    fun stop() {
        cancel = true
        try {
            thread!!.join()
        } catch (e: InterruptedException) {
            Log.e(OBJ_NAME, "", e)
        }
    }

    interface FrameCallback {
        fun handleFrame(audio_data: ShortArray?, length: Int)
    }

    companion object {
        const val OBJ_NAME = "AudioGrabber"
    }
}