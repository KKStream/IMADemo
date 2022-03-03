package com.kkstream.imademo.repo.api

import com.google.gson.Gson
import com.kkstream.android.ottfs.module.api.restful.HTTPMethod
import com.kkstream.imademo.data.Content
import com.kkstream.imademo.data.VideoInfoData
import com.kkstream.imademo.repo.base.APIBase

class VideoInfoAPI(contentId: String) : APIBase<Content.Video>("videos/$contentId", HTTPMethod.GET) {
    override fun parse(response: String): Content.Video = Content.Video.create((Gson().fromJson(response, VideoInfoData::class.java)))
}