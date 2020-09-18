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
package com.skdev.ytlivevideo.util

import com.google.api.services.youtube.model.LiveBroadcast

/**
 * @author Ibrahim Ulukaya <ulukaya></ulukaya>@google.com>
 *
 *
 * Helper class to handle YouTube videos.
 */
class EventData {
    var event: LiveBroadcast? = null

    var ingestionAddress: String? = null

    val id: String
        get() = event!!.id

    val title: String
        get() = event!!.snippet.title

    // if protocol is not defined, pick https
    val thumbUri: String
        get() {
            var url = event!!.snippet.thumbnails.default.url
            // if protocol is not defined, pick https
            if (url.startsWith("//")) {
                url = "https:$url"
            }
            return url
        }

    val watchUri: String
        get() = "http://www.youtube.com/watch?v=$id"
}