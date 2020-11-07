@file:Suppress("unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused", "unused")

package com.skdev.ytlivevideo.ui.mainScene.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.skdev.ytlivevideo.model.youtubeApi.liveBroadcasts.LiveBroadcastItem

/**
 * TODO: Move from Fragment
 */
class BroadcastsListAdapter(val context: Context, private val mLiveBroadcastItems: List<LiveBroadcastItem>) : BaseAdapter() {
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
            return View(context)
        }
}