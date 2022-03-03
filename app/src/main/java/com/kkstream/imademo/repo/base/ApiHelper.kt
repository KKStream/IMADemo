package com.kkstream.imademo.repo.base

object ApiHelper {

    const val API_VERSION: String = "v1"

    var hostUrl: String = ""

    val headers: MutableMap<String, String> = mutableMapOf()

    var deviceId = ""
    val queries: MutableMap<String, String> = mutableMapOf()

    /**
     * Add a single header.
     */
    fun addHeader(key: String, value: String) {
        headers[key] = value
    }

    /**
     * Remove all headers.
     */
    fun clearHeaders() {
        headers.clear()
    }
}