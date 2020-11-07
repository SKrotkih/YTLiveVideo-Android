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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.toolbox.ImageLoader
import com.google.android.gms.plus.PlusOneButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem
import com.skdev.ytlivevideo.R
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.BroadcastState
import com.skdev.ytlivevideo.ui.mainScene.view.viewModel.MainViewModel
import com.skdev.ytlivevideo.ui.router.Router
import com.skdev.ytlivevideo.util.Utils.setSafeOnClickListener
import com.skdev.ytlivevideo.util.timeAgo
import kotlinx.android.synthetic.main.live_events_list_item.view.*

/**
 * Left side fragment showing user's uploaded YouTube videos.
 */
class BroadcastsListFragment(val state: BroadcastState) : Fragment() {

    var selected = false

    private var mImageLoader: ImageLoader? = null
    private var mGridView: GridView? = null
    private var mSwipeRefresh: SwipeRefreshLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val fragmentView = inflater.inflate(R.layout.fragment_live_events_list, container, false)
        configureGrid(fragmentView)
        subscribeOnSignIn()
        configureRefreshController(fragmentView)
        configureActionButton(fragmentView)
        return fragmentView
    }

    private fun subscribeOnSignIn() {
        val viewModel: MainViewModel by activityViewModels()
        viewModel.signInManager.googleApiReadyToUse.observe(viewLifecycleOwner, { event ->
            event?.getContentIfNotHandledOrReturnNull()?.let {fetchData()}
        })
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        if (activity !is FragmentDelegate) {
            throw ClassCastException("Activity must implement callbacks.")
        }
        mImageLoader = (activity as FragmentDelegate).onGetImageLoader()
        subscribeOnChangeData()
    }

    private fun fetchData() {
        if (mGridView?.adapter != null) {
            (mGridView!!.adapter as LiveEventAdapter)
                .notifyDataSetChanged()
        }
        if (selected) {
            val viewModel: MainViewModel by activityViewModels()
            viewModel.fetchBroadcasts(state)
        }
    }

    private fun subscribeOnChangeData() {
        val viewModel: MainViewModel by activityViewModels()
        when (state) {
            BroadcastState.ALL -> {
                viewModel.allBroadcastItems.observe(this, { event ->
                    event?.getContentIfNotHandledOrReturnNull()?.let {
                        setEvents(it)
                    }})
            }
            BroadcastState.UPCOMING -> {
                viewModel.upcomingBroadcastItems.observe(this, { event ->
                    event?.getContentIfNotHandledOrReturnNull()?.let {
                        setEvents(it)
                    }})
            }
            BroadcastState.ACTIVE -> {
                viewModel.activeBroadcastItems.observe(this, { event ->
                    event?.getContentIfNotHandledOrReturnNull()?.let {
                        setEvents(it)
                    }})
            }
            BroadcastState.COMPLETED -> {
                viewModel.completedBroadcastItems.observe(this, { event ->
                    event?.getContentIfNotHandledOrReturnNull()?.let {
                        setEvents(it)
                    }})
            }
        }
    }

    override fun onDetach() {
        super.onDetach()
        mImageLoader = null
    }

    private fun configureRefreshController(context: View) {
        mSwipeRefresh = context.findViewById<View>(R.id.swipeRefresh) as SwipeRefreshLayout
        mSwipeRefresh?.setColorSchemeResources(R.color.colorPrimary)
        mSwipeRefresh?.setOnRefreshListener {
            val viewModel: MainViewModel by activityViewModels()
            viewModel.fetchBroadcasts(state)
        }
    }

    private fun configureActionButton(context: View) {
        val addNewBroadcast = context.findViewById<View>(R.id.addNewBroadcast) as FloatingActionButton
        addNewBroadcast.setSafeOnClickListener {
            Router.StartActivity.CREATE_BROADCAST.run()
        }
    }

    private fun setEvents(liveBroadcastItems: List<LiveBroadcastItem>) {
        mSwipeRefresh?.isRefreshing = false
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
            view.createdAt.text = "Created: ${broadcastItem.publishedAt.timeAgo()}"
            view.scheduledAt.text = "Scheduled: ${broadcastItem.publishedAt.timeAgo()}"
            view.thumbnail.setImageUrl(broadcastItem.thumbUri, mImageLoader)
            val viewModel: MainViewModel by activityViewModels()
            if (viewModel.isConnected()) {
                (view.plus_button as PlusOneButton).initialize(broadcastItem.watchUri, null)
            }
            view.main_target.setOnClickListener {
                (activity as FragmentDelegate).didUserSelectBroadcastItem(broadcastItem)
            }
        }
    }

    /**
     * Private presenter's methods
     */
    private fun configureGrid(context: View) {
        mGridView = context.findViewById<View>(R.id.grid_view) as GridView
        val emptyView = context.findViewById<View>(R.id.empty) as TextView
        mGridView!!.emptyView = emptyView
    }

    companion object {
        private val TAG = BroadcastsListFragment::class.java.name
    }
}
