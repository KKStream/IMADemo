package com.kkstream.imademo.repo.api

import com.google.gson.Gson
import com.kkstream.android.ottfs.module.api.restful.HTTPMethod
import com.kkstream.imademo.data.PlaybackInfoData
import com.kkstream.imademo.repo.base.APIBase
import com.kkstream.imademo.repo.base.ApiHelper

class PlaybackInfoAPI(
    contentId: String,
    contentType: String,
    playbackToken: String
) : APIBase<PlaybackInfoData>(
    "sessions/$contentType/$contentId/playback/${ApiHelper.deviceId}/info",
    HTTPMethod.GET,
    playbackToken
) {
    override fun parse(response: String): PlaybackInfoData? {
        return Gson().fromJson(response, PlaybackInfoData::class.java)
    }
}