package com.kkstream.imademo.repo.base

import com.kkstream.android.ottfs.module.api.APIError
import com.kkstream.android.ottfs.module.api.restful.APIBase
import com.kkstream.android.ottfs.module.api.restful.HTTPMethod

abstract class APIBase<ResultType>(
    url: String,
    method: HTTPMethod,
    playbackToken: String? = null,
    licenseId: String? = null
) : APIBase<ResultType>(ApiHelper.hostUrl + "/${ApiHelper.API_VERSION}/" + url, method) {

    companion object {
        private const val LICENSE_ID = "license_id"
        private const val PLAYBACK_TOKEN = "playback_token"
    }

    init {
        ApiHelper.headers.forEach {
            this.addHeader(it.key, it.value)
        }
        ApiHelper.queries.forEach {
            this.addQuery(it.key, it.value)
        }
        playbackToken?.let {
            addQuery(PLAYBACK_TOKEN, playbackToken)
        }
        licenseId?.let {
            if (it.isNotEmpty()) {
                this.addQuery(LICENSE_ID, licenseId)
            }
        }
    }

    // TODO: Wait for backend implement response code
    override fun parseAPIError(error: APIError) = Unit
}