/*
 * Copyright (c) 2015 Google Inc.
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
package com.skdev.ytlivevideo.util

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.android.volley.toolbox.ImageLoader.ImageCache

class LruBitmapCache(maxSize: Int) : LruCache<String?, Bitmap?>(maxSize), ImageCache {
    constructor(ctx: Context) : this(getCacheSize(ctx))

    fun sizeOf(key: String?, value: Bitmap): Int {
        return value.rowBytes * value.height
    }

    override fun getBitmap(url: String?): Bitmap? {
        return get(url)
    }

    override fun putBitmap(url: String?, bitmap: Bitmap?) {
        put(url, bitmap)
    }

    companion object {
        // Returns a cache size equal to approximately three screens worth of images.
        fun getCacheSize(ctx: Context): Int {
            val displayMetrics = ctx.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            // 4 bytes per pixel
            val screenBytes = screenWidth * screenHeight * 4
            return screenBytes * 3
        }
    }
}