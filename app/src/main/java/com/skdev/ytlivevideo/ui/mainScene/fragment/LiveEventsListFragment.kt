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
package com.skdev.ytlivevideo.ui.mainScene.fragment

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.google.android.gms.plus.PlusOneButton
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Left side fragment showing user's uploaded YouTube videos.
 */
class LiveEventsListFragment : Fragment(), SignInConnectDelegate {

    var viewModel: MainViewModel? = null
        set(value) {
            field = value
            viewModel!!.signInConnectDelegate = this
        }

    private var mCallbacks: Callbacks? = null
    private var mImageLoader: ImageLoader? = null
    private var mGridView: GridView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val listView: View = inflater.inflate(
            R.layout.fragment_live_events_list,
            container,
            false
        )
        mGridView = listView.findViewById<View>(R.id.grid_view) as GridView
        val emptyView = listView
            .findViewById<View>(R.id.empty) as TextView
        mGridView!!.emptyView = emptyView
        return listView
    }

    fun setEvents(liveBroadcastItems: List<LiveBroadcastItem>) {
        if (!isAdded) {
            return
        }
        mGridView!!.adapter = LiveEventAdapter(liveBroadcastItems)
    }

    private fun setProfileInfo() {
        val accountName: String? = viewModel!!.getAccountName()
        (view!!.findViewById<View>(R.id.avatar) as ImageView)
            .setImageDrawable(null)
//            if (currentPerson.hasImage()) {
//                // Set the URL of the image that should be loaded into this view, and
//                // specify the ImageLoader that will be used to make the request.
//                (view!!.findViewById<View>(R.id.avatar) as NetworkImageView).setImageUrl(
//                    currentPerson.image.url, mImageLoader
//                )
//            }
        (view!!.findViewById<View>(R.id.display_name) as TextView).text = accountName
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity !is Callbacks) {
            throw ClassCastException("Activity must implement callbacks.")
        }
        mCallbacks = activity
        mImageLoader = mCallbacks!!.onGetImageLoader()
    }

    override fun onDetach() {
        super.onDetach()
        mCallbacks = null
        mImageLoader = null
    }

    interface Callbacks {
        fun onGetImageLoader(): ImageLoader?
        fun onEventSelected(liveBroadcastItem: LiveBroadcastItem?)
        fun onConnected(connectedAccountName: String?)
    }

    private inner class LiveEventAdapter(private val mLiveBroadcastItems: List<LiveBroadcastItem>) : BaseAdapter() {
        override fun getCount(): Int {
            return mLiveBroadcastItems.size
        }

        override fun getItem(i: Int): Any {
            return mLiveBroadcastItems[i]
        }

        override fun getItemId(i: Int): Long {
            return mLiveBroadcastItems[i].id.hashCode().toLong()
        }

        override fun getView(
            position: Int, convertView: View,
            container: ViewGroup
        ): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(
                    R.layout.live_events_list_item, container, false
                )
            }
            val event = mLiveBroadcastItems[position]
            (convertView.findViewById<View>(R.id.text1) as TextView).text = event.title
            (convertView.findViewById<View>(R.id.thumbnail) as NetworkImageView).setImageUrl(
                event.thumbUri,
                mImageLoader
            )
            if (viewModel!!.isConnected()) {
                (convertView.findViewById<View>(R.id.plus_button) as PlusOneButton)
                    .initialize(event.watchUri, null)
            }
            convertView.findViewById<View>(R.id.main_target)
                .setOnClickListener {
                    mCallbacks!!.onEventSelected(mLiveBroadcastItems[position])
                }
            return convertView
        }
    }

    /**
     *  SignInConnectDelegate
     */
    override fun signedIn() {
        setProfileInfo()
        if (mGridView?.adapter != null) {
            (mGridView!!.adapter as LiveEventAdapter)
                .notifyDataSetChanged()
        }
        setProfileInfo()
        mCallbacks?.onConnected(viewModel!!.getAccountName())
    }
    /**
     *
     */

    companion object {
        private val TAG = LiveEventsListFragment::class.java.name
    }
}

interface SignInConnectDelegate {
    fun signedIn()
}