package com.kkstream.imademo.data

data class VideoInfoData(
    val id: String,
    val image_url: String?,
    val next_video: VideoInfoOtherVideo?,
    val prev_video: VideoInfoOtherVideo?,
    val time: VideoInfoTime,
    val title: String
)

data class VideoInfoOtherVideo(
    val id: String,
    val image_url: String?,
    val title: String
)

data class VideoInfoTime(
    val end_start_position: Int? = null,
    val last_position: Int? = null
)
