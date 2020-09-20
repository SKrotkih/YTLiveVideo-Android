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
package com.skdev.ytlivevideo.media

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * FFmpeg class which loads ffmpeg library and exposes its methods.
 */
object Ffmpeg {
    @JvmStatic
    external fun init(width: Int, height: Int, audio_sample_rate: Int, rtmpUrl: String?): Boolean
    @JvmStatic
    external fun shutdown()

    // Returns the size of the encoded frame.
    @JvmStatic
    external fun encodeVideoFrame(yuv_image: ByteArray?): Int
    @JvmStatic
    external fun encodeAudioFrame(audio_data: ShortArray?, length: Int): Int

    init {
        System.loadLibrary("ffmpeg")
    }
}