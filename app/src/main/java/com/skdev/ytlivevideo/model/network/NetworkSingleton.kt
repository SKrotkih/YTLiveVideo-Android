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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.widget.ImageView
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.Volley
import com.skdev.ytlivevideo.util.LruBitmapCache
import java.net.URL

class NetworkSingleton private constructor(private var mCtx: Context) {
    private var mRequestQueue: RequestQueue?
    private val mImageLoader: ImageLoader

    // getApplicationContext() is key, it keeps you from leaking the
    // Activity or BroadcastReceiver if someone passes one in.
    private val requestQueue: RequestQueue?
        get() {
            if (mRequestQueue == null) {
                // getApplicationContext() is key, it keeps you from leaking the
                // Activity or BroadcastReceiver if someone passes one in.
                mRequestQueue = Volley.newRequestQueue(mCtx.applicationContext)
            }
            return mRequestQueue
        }

    fun <T> addToRequestQueue(req: Request<T>?) {
        requestQueue?.add(req)
    }

    val imageLoader: ImageLoader
        get() = mImageLoader

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

    init {
        mRequestQueue = requestQueue
        mImageLoader = ImageLoader(
            mRequestQueue,
            LruBitmapCache(mCtx)
        )
    }
}

// Class to download an image from url and display it into an image view
class DownLoadImageTask(private val imageView: ImageView) : AsyncTask<String, Void, Bitmap?>() {
    override fun doInBackground(vararg urls: String): Bitmap? {
        val urlOfImage = urls[0]
        return try {
            val inputStream = URL(urlOfImage).openStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) { // Catch the download exception
            e.printStackTrace()
            null
        }
    }
    override fun onPostExecute(result: Bitmap?) {
        if(result!=null){
            // Display the downloaded image into image view
            imageView.setImageBitmap(result)
        }else{
            Toast.makeText(imageView.context,"Error downloading",Toast.LENGTH_SHORT).show()
        }
    }
}
