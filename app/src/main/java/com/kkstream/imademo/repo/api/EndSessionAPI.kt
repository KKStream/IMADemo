package com.kkstream.imademo.repo.api

import com.kkstream.android.ottfs.module.api.restful.HTTPMethod
import com.kkstream.imademo.repo.base.APIBase
import com.kkstream.imademo.repo.base.ApiHelper

class EndSessionAPI(
        contentId: String,
        contentType: String,
        playbackToken: String
) : APIBase<Boolean>(
        "sessions/${contentType}/$contentId/playback/${ApiHelper.deviceId}/end",
        HTTPMethod.POST,
        playbackToken
) {
    override fun parse(response: String): Boolean? = response.isEmpty()
}