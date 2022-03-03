package com.kkstream.imademo.data

data class StartSessionData(
    val token: String,
    val license_id: String?,
    val widevine_blacklist: List<String>,
    val drm_portal_url: String?
)
