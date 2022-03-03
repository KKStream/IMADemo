package com.kkstream.imademo.repo.api

import com.google.gson.Gson
import com.kkstream.android.ottfs.module.api.restful.HTTPMethod
import com.kkstream.imademo.data.StartSessionData
import com.kkstream.imademo.repo.base.APIBase
import com.kkstream.imademo.repo.base.ApiHelper

class StartSessionAPI(
    contentId: String,
    contentType: String,
    licenseId: String?
) : APIBase<StartSessionData>(
    "sessions/${contentType}/$contentId/playback/${ApiHelper.deviceId}/start",
    HTTPMethod.POST,
    licenseId = licenseId
) {
    override fun parse(response: String): StartSessionData? = Gson().fromJson(response, StartSessionData::class.java)
}