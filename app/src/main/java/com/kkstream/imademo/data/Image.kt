package com.kkstream.imademo.data

data class Image(
    val url: String = ""
) {
    companion object {
        fun create(url: String) =
            Image(
                url = url
            )
    }
}
