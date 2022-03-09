package com.kkstream.imademo.repo

import android.content.Context
import android.os.Handler
import android.util.Log
import com.kkstream.imademo.app.BaseApp
import com.kkstream.imademo.data.ApiResponse
import com.kkstream.imademo.data.Content
import com.kkstream.imademo.data.PlaybackInfoData
import com.kkstream.imademo.data.StartSessionData
import com.kkstream.imademo.repo.api.*
import com.kkstream.imademo.repo.base.APICaller
import com.kkstream.imademo.repo.base.ApiHelper
import kotlinx.coroutines.*
import java.util.*

class ApiFlow(
        context: Context,
        private val handler: Handler
) {

    private val job = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + job)
    private val apiCaller = APICaller()
    private var pollingHeartbeatAction: Runnable? = null

    private lateinit var contentType: String
    private lateinit var playbackToken: String

    init {
        // Set Api info
        ApiHelper.hostUrl = HOST_URL
        ApiHelper.clearHeaders()
        ApiHelper.addHeader("Authorization", ACCESS_TOKEN)
        ApiHelper.deviceId = BaseApp.getDeviceId(context)
        PLAYBACK_CUSTOM_HEADERS.map {
            ApiHelper.addHeader(it.key, it.value)
        }
    }

    fun startFlow(callback : (playbackInfoData: PlaybackInfoData) -> Unit) {
        ioScope.launch {
            val videoInfo = requestVideoInfo(CONTENT_ID)
            contentType = videoInfo!!.type.toString().toLowerCase(Locale.ROOT)

            val startSessionData = startPlaybackSession(CONTENT_ID, contentType, "")!!
            playbackToken = startSessionData.token

            // Polling heartbeat if playback token is exist.
            pollingHeartbeat(CONTENT_ID, contentType, playbackToken)

            //get manifest
            val playbackInfoData = getPlaybackInfo(CONTENT_ID, contentType, playbackToken)!!
            withContext(Dispatchers.Main) {
                callback(playbackInfoData)
            }
        }
    }

    /**
     * Request the next content for self-linear
     */
    fun getNextContent(nextContentId: String, callback : (playbackInfoData: PlaybackInfoData) -> Unit) {
        ioScope.launch {
            val videoInfo = requestVideoInfo(nextContentId)
            contentType = videoInfo!!.type.toString().toLowerCase(Locale.ROOT)

            //get manifest
            val playbackInfoData = getPlaybackInfo(nextContentId, contentType, playbackToken)!!
            withContext(Dispatchers.Main) {
                callback(playbackInfoData)
            }
        }
    }

    private suspend fun requestVideoInfo(contentId: String): Content? {
        return when (val apiResponse: ApiResponse = apiCaller.execute(VideoInfoAPI(contentId))) {
            is ApiResponse.Success<*> -> {
                apiResponse.data as Content
            }
            is ApiResponse.Error -> {
                Log.e(TAG, "msg: ${apiResponse.errorEvent}")
                null
            }
        }
    }

    private suspend fun startPlaybackSession(
            contentId: String,
            contentType: String,
            licenseId: String?
    ): StartSessionData? {
        return when (val apiResponse: ApiResponse = apiCaller.execute(StartSessionAPI(contentId, contentType, licenseId))) {
            is ApiResponse.Success<*> -> {
                apiResponse.data as StartSessionData
            }
            is ApiResponse.Error -> {
                Log.e(TAG, "msg: ${apiResponse.errorEvent}")
                null
            }
        }
    }

    private suspend fun getPlaybackInfo(
            itemId: String,
            itemType: String,
            playbackToken: String
    ): PlaybackInfoData? {
        return when (val apiResponse: ApiResponse = apiCaller.execute(PlaybackInfoAPI(itemId, itemType, playbackToken))) {
            is ApiResponse.Success<*> -> {
                apiResponse.data as PlaybackInfoData
            }
            is ApiResponse.Error -> {
                Log.e(TAG, "msg: ${apiResponse.errorEvent}")
                null
            }
        }
    }

    /**
     * Heart beat
     */
    private fun pollingHeartbeat(
            contentId: String,
            contentType: String,
            playbackToken: String
    ) {
        pollingHeartbeatAction =
                providePollingHeartbeatAction(contentId, contentType, playbackToken)
        pollingHeartbeatAction?.run()
    }

    private fun providePollingHeartbeatAction(
            contentId: String,
            contentType: String,
            playbackToken: String
    ) = Runnable {
        ioScope.launch {
            postHeartbeat(contentId, contentType, playbackToken)
        }

        Log.d("APICaller", "Test")

        // Polling heartbeat api.
        pollingHeartbeatAction?.let {
            handler.postDelayed(it, HEARTBEAT_INTERVAL)
        }
    }

    private suspend fun postHeartbeat(
            contentId: String,
            contentType: String,
            playbackToken: String
    ): Boolean {
        return when (val apiResponse: ApiResponse = apiCaller.execute(HeartbeatAPI(contentId, contentType, playbackToken))) {
            is ApiResponse.Success<*> -> {
                apiResponse.data as Boolean
            }
            is ApiResponse.Error -> {
                Log.e(TAG, "msg: ${apiResponse.errorEvent}")
                false
            }
        }
    }

    fun endFlow() {
        ioScope.launch {
            // update position, if needed

            // stop heart beat
            pollingHeartbeatAction?.let {
                handler.removeCallbacks(it)
            }

            // end start session
            endStartSession(CONTENT_ID, contentType, playbackToken)
        }
    }

    private suspend fun endStartSession(
            contentId: String,
            contentType: String,
            playbackToken: String
    ): Boolean {
        return when (val apiResponse: ApiResponse = apiCaller.execute(EndSessionAPI(contentId, contentType, playbackToken))) {
            is ApiResponse.Success<*> -> {
                apiResponse.data as Boolean
            }
            is ApiResponse.Error -> {
                Log.e(TAG, "msg: ${apiResponse.errorEvent}")
                false
            }
        }
    }

    companion object {
        private val TAG = ApiFlow::class.java.simpleName.toString()

        // API Info
        private const val HOST_URL = ""             // https://xxxxxx.xxxx.xxxx
        private const val CONTENT_ID = ""           // ex: 1, 20, ...etc
        private const val ACCESS_TOKEN = ""         // token
        private val PLAYBACK_CUSTOM_HEADERS = mapOf(
                "PaaS-Sample-Platform-Type" to "androidtv",
                "X-Device-Type" to "androidtv"
        )
        private const val HEARTBEAT_INTERVAL = 10_000L
    }
}