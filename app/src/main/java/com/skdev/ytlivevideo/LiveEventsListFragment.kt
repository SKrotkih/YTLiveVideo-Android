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
package com.skdev.ytlivevideo

import android.app.Activity
import android.app.Fragment
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.NetworkImageView
import com.skdev.ytlivevideo.model.youtubeApi.LiveEvent
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks
import com.google.android.gms.plus.Plus
import com.google.android.gms.plus.PlusOneButton
import com.google.android.gms.plus.model.people.Person

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Left side fragment showing user's uploaded YouTube videos.
 */
class LiveEventsListFragment : Fragment(), ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private var mCallbacks: Callbacks? = null
    private var mImageLoader: ImageLoader? = null
    private var mGoogleApiClient: GoogleApiClient? = null
    private var mGridView: GridView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGoogleApiClient = GoogleApiClient.Builder(activity)
            .addApi(Plus.API)
            .addScope(Plus.SCOPE_PLUS_PROFILE)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val listView: View = inflater.inflate(
            R.layout.list_fragment,
            container,
            false
        )
        mGridView = listView.findViewById<View>(R.id.grid_view) as GridView
        val emptyView = listView
            .findViewById<View>(R.id.empty) as TextView
        mGridView!!.emptyView = emptyView
        return listView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setProfileInfo()
    }

    fun setEvents(liveEvents: List<LiveEvent>) {
        if (!isAdded) {
            return
        }
        mGridView!!.adapter = LiveEventAdapter(liveEvents)
    }

    private fun setProfileInfo() {
        if (!mGoogleApiClient!!.isConnected
            || Plus.PeopleApi.getCurrentPerson(mGoogleApiClient) == null
        ) {
            (view!!.findViewById<View>(R.id.avatar) as ImageView)
                .setImageDrawable(null)
            (view!!.findViewById<View>(R.id.display_name) as TextView)
                .setText(R.string.not_signed_in)
        } else {
            val currentPerson: Person = Plus.PeopleApi.getCurrentPerson(mGoogleApiClient)
            if (currentPerson.hasImage()) {
                // Set the URL of the image that should be loaded into this view, and
                // specify the ImageLoader that will be used to make the request.
                (view!!.findViewById<View>(R.id.avatar) as NetworkImageView).setImageUrl(
                    currentPerson.image.url, mImageLoader
                )
            }
            if (currentPerson.hasDisplayName()) {
                (view!!.findViewById<View>(R.id.display_name) as TextView).text = currentPerson.displayName
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mGoogleApiClient?.connect()
    }

    override fun onPause() {
        super.onPause()
        mGoogleApiClient?.disconnect()
    }

    override fun onConnected(bundle: Bundle?) {
        if (mGridView?.adapter != null) {
            (mGridView!!.adapter as LiveEventAdapter)
                .notifyDataSetChanged()
        }
        setProfileInfo()
        mCallbacks?.onConnected(Plus.AccountApi.getAccountName(mGoogleApiClient))
    }

    override fun onConnectionSuspended(i: Int) {
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        if (connectionResult.hasResolution()) {
            Toast.makeText(
                activity,
                R.string.connection_to_google_play_failed,
                Toast.LENGTH_SHORT
            ).show()
            Log.e(
                TAG,
                java.lang.String.format(
                    "Connection to Play Services Failed, error: %d, reason: %s",
                    connectionResult.errorCode,
                    connectionResult.toString()
                )
            )
            try {
                connectionResult.startResolutionForResult(activity, 0)
            } catch (e: SendIntentException) {
                Log.e(TAG, e.toString(), e)
            }
        }
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
        fun onEventSelected(liveEvent: LiveEvent?)
        fun onConnected(connectedAccountName: String?)
    }

    private inner class LiveEventAdapter(private val mLiveEvents: List<LiveEvent>) : BaseAdapter() {
        override fun getCount(): Int {
            return mLiveEvents.size
        }

        override fun getItem(i: Int): Any {
            return mLiveEvents[i]
        }

        override fun getItemId(i: Int): Long {
            return mLiveEvents[i].id.hashCode().toLong()
        }

        override fun getView(
            position: Int, convertView: View,
            container: ViewGroup
        ): View {
            var convertView = convertView
            if (convertView == null) {
                convertView = LayoutInflater.from(activity).inflate(
                    R.layout.list_item, container, false
                )
            }
            val event = mLiveEvents[position]
            (convertView.findViewById<View>(R.id.text1) as TextView).text = event.title
            (convertView.findViewById<View>(R.id.thumbnail) as NetworkImageView).setImageUrl(
                event.thumbUri,
                mImageLoader
            )
            if (mGoogleApiClient!!.isConnected) {
                (convertView.findViewById<View>(R.id.plus_button) as PlusOneButton)
                    .initialize(event.watchUri, null)
            }
            convertView.findViewById<View>(R.id.main_target)
                .setOnClickListener {
                    mCallbacks!!.onEventSelected(mLiveEvents[position])
                }
            return convertView
        }
    }

    companion object {
        private val TAG = LiveEventsListFragment::class.java.name
    }
}