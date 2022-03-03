package com.kkstream.imademo.data

/**
 * Content represent the media content, and including following types of content:
 * Video, Linear, Program, StartOver, Live, Trailer and Offline.
 */
sealed class Content {
    abstract val id: String
    abstract val type: ContentType
    abstract val title: String
    abstract val subtitle: String?
    abstract val startTime: Int?
    abstract val endTime: Int?
    abstract val endStartTime: Int?
    abstract val images: List<Image>

    /**
     * Video content is a playback with specific dubbing or subtitle, and allows following operations:
     *
     * Play, Pause, Next, Previous, Rewind, Fast Forward, Seek.
     */
    data class Video(
        override val id: String,
        override val type: ContentType = ContentType.Videos,
        override val title: String = "",
        override val subtitle: String? = null,
        override val startTime: Int? = null,
        override val endTime: Int? = null,
        override val endStartTime: Int? = null,
        override val images: List<Image> = listOf(),
        val time: VideoTime = VideoTime(),
        val nextVideo: Video? = null,
        val previousVideo: Video? = null
    ) : Content() {
        companion object {
            fun create(videoInfoData: VideoInfoData): Video =
                Video(
                    id = videoInfoData.id,
                    title = videoInfoData.title,
                    time = VideoTime(
                        lastPlayed = videoInfoData.time.last_position,
                        endStartTime = videoInfoData.time.end_start_position
                    ),
                    images = videoInfoData.image_url?.let {
                        listOf(Image.create(it))
                    } ?: listOf(),
                    startTime = videoInfoData.time.last_position,
                    endStartTime = videoInfoData.time.end_start_position,
                    nextVideo = videoInfoData.next_video?.let {
                        createRecommendItem(
                            it
                        )
                    },
                    previousVideo = videoInfoData.prev_video?.let {
                        createRecommendItem(
                            it
                        )
                    }
                )

            private fun createRecommendItem(video: VideoInfoOtherVideo) =
                Video(
                    id = video.id,
                    title = video.title,
                    images = video.image_url?.let {
                        listOf(Image.create(it))
                    } ?: listOf()
                )
        }
    }

    data class Live(
        override val id: String,
        override val type: ContentType = ContentType.Lives,
        override val title: String,
        override val subtitle: String? = null,
        override val startTime: Int?,
        override val endTime: Int?,
        override val endStartTime: Int? = null,
        val end: Boolean,
        override val images: List<Image> = listOf(),
        val seekable: Boolean = false,
        val sectionId: String? = null
    ) : Content()
}