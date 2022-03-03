package com.kkstream.imademo.data

data class PlaybackInfoData(
    val sources: List<Source>
)

data class Source(
    val manifests: List<ManifestData>,
    val thumbnail_seeking_url: String,
    val type: String?
)

data class ManifestData(
    val protocol: String,
    val url: String,
    val resolutions: List<Resolution>,
    val ssai: SSSAIItem?
)

data class Resolution(
    val height: Int
)

data class SSSAIItem(
    val media_tailor: MediaTailor?,
    val google_dai: GoogleDai?
)

data class MediaTailor(
    val client_side_reporting_url: String,
    val server_side_reporting_url: String
)

data class GoogleDai(
    val vod: GoogleDaiVod?,
    val live: GoogleDaiLive?
)

data class GoogleDaiVod(
    val api_key: String?,
    val content_source_id: String,
    val video_id: String
)

data class GoogleDaiLive(
    val api_key: String?,
    val asset_key: String
)