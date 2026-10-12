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
package com.skdev.ytlivevideo.model.network

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import android.widget.Toast
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.skdev.ytlivevideo.util.LruBitmapCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

class NetworkSingleton private constructor(context: Context) {
    // getApplicationContext() is key, it keeps you from leaking the
    // Activity or BroadcastReceiver if someone passes one in.
    val imageLoader = ImageLoader(
        Volley.newRequestQueue(context.applicationContext),
        LruBitmapCache(context)
    )

    companion object {
        private var mInstance: NetworkSingleton? = null
        @Synchronized
        fun getInstance(context: Context): NetworkSingleton? {
            if (mInstance == null) {
                mInstance = NetworkSingleton(context)
            }
            return mInstance
        }
    }
}

/**
 * Downloads an image from [url] and displays it in this image view.
 */
suspend fun ImageView.loadImageFromUrl(url: String) {
    val bitmap = withContext(Dispatchers.IO) {
        try {
            URL(url).openStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { // Catch the download exception
            e.printStackTrace()
            null
        }
    }
    if (bitmap != null) {
        setImageBitmap(bitmap)
    } else {
        Toast.makeText(context, "Error downloading", Toast.LENGTH_SHORT).show()
    }
}
