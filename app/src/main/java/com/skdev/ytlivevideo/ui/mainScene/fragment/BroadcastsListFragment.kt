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
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.plus.PlusOneButton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcast.LiveBroadcastItem
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.fragment_live_events_list.*
import kotlinx.android.synthetic.main.live_events_list_item.view.*

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 * Left side fragment showing user's uploaded YouTube videos.
 */
class BroadcastsListFragment : Fragment(), SignInConnectDelegate {

    companion object {
        fun newInstance() = BroadcastsListFragment()
//        Pass parameters into Fragment
//        fun newInstance(tutorial: Tutorial): BroadcastsListFragment {
//            val fragment = BroadcastsListFragment()
//            val args = Bundle()
//            args.putParcelable(TUTORIAL_KEY, tutorial)
//            fragment.arguments = args
//            return fragmentHome
//        }
//        Tutorial.kt:
//        @Parcelize
//        data class Tutorial(
//            val name: String,
//            val url: String,
//            val description: String) : Parcelable
//       Somewhere in BroadcastsListFragment:
//       val tutorial = arguments?.getParcelable(TUTORIAL_KEY) as Tutorial

        private val TAG = BroadcastsListFragment::class.java.name
    }

    private var mFragmentDelegate: FragmentDelegate? = null
    private var mImageLoader: ImageLoader? = null
    private var mGridView: GridView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val fragmentView = inflater.inflate(R.layout.fragment_live_events_list, container, false)
        configureGrid(fragmentView)
        return fragmentView
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity !is FragmentDelegate) {
            throw ClassCastException("Activity must implement callbacks.")
        }
        mFragmentDelegate = activity
        mImageLoader = mFragmentDelegate!!.onGetImageLoader()
    }

    override fun onDetach() {
        super.onDetach()
        mFragmentDelegate = null
        mImageLoader = null
    }

    fun setEvents(liveBroadcastItems: List<LiveBroadcastItem>) {
        mGridView!!.adapter = LiveEventAdapter(liveBroadcastItems)
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

        override fun getView(position: Int, convertView: View?, container: ViewGroup): View {
            val broadcastItem = mLiveBroadcastItems[position]
            val view: View = convertView ?: LayoutInflater.from(activity).inflate(R.layout.live_events_list_item, container, false)
            renderGridItem(view, broadcastItem)
            return view
        }

        private fun renderGridItem(view: View, broadcastItem: LiveBroadcastItem) {
            view.title.text = broadcastItem.title
            view.createdAt.text = "Created: ${broadcastItem.publishedAt}"
            view.scheduledAt.text = "Scheduled: ${broadcastItem.publishedAt}"
            view.thumbnail.setImageUrl(broadcastItem.thumbUri, mImageLoader)
            val viewModel: MainViewModel by activityViewModels()
            if (viewModel.isConnected()) {
                (view.plus_button as PlusOneButton).initialize(broadcastItem.watchUri, null)
            }
            view.main_target.setOnClickListener {
                mFragmentDelegate!!.onEventSelected(broadcastItem)
            }
        }
    }

    /**
     *  SignInConnectDelegate
     */
    override fun signedIn() {
        if (mGridView?.adapter != null) {
            (mGridView!!.adapter as LiveEventAdapter)
                .notifyDataSetChanged()
        }
        setProfileInfo()
        val viewModel: MainViewModel by activityViewModels()
        mFragmentDelegate?.onConnected(viewModel.getAccountName())
    }

    /**
     * Private presenter's methods
     */
    private fun configureGrid(context: View) {
        mGridView = context.findViewById<View>(R.id.grid_view) as GridView
        val emptyView = context.findViewById<View>(R.id.empty) as TextView
        mGridView!!.emptyView = emptyView
    }

    private fun setProfileInfo() {
        val viewModel: MainViewModel by activityViewModels()
        display_name.text = viewModel.getAccountName()
        avatar.setImageDrawable(null)
//            if (currentPerson.hasImage()) {
//                // Set the URL of the image that should be loaded into this view, and
//                // specify the ImageLoader that will be used to make the request.
//                (view!!.findViewById<View>(R.id.avatar) as NetworkImageView).setImageUrl(
//                    currentPerson.image.url, mImageLoader
//                )
//            }
    }
}
