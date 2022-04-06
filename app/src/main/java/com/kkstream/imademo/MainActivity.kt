package com.kkstream.imademo

import android.media.session.PlaybackState
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import com.google.ads.interactivemedia.v3.api.*
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.kkstream.imademo.data.*
import com.kkstream.imademo.databinding.ActivityMainBinding
import com.kkstream.imademo.extend.getStringForTime
import com.kkstream.imademo.extend.toMilliSecond
import com.kkstream.imademo.extend.toSecond
import com.kkstream.imademo.player.AdsStreamPlayer
import com.kkstream.imademo.repo.ApiFlow
import java.lang.Runnable

class MainActivity : AppCompatActivity(), View.OnClickListener, AdsLoader.AdsLoadedListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mediaSource: DashMediaSource
    private lateinit var player: SimpleExoPlayer
    private lateinit var adsStreamPlayer: AdsStreamPlayer

    private var adType = AdType.VOD
    private val sdkFactory = ImaSdkFactory.getInstance()
    private var adsLoader: AdsLoader? = null
    private var streamManager: StreamManager? = null

    private lateinit var apiFlow: ApiFlow

    private lateinit var playbackInfoData: PlaybackInfoData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playPauseBtn.setOnClickListener(this)
        binding.progressBar.addListener(timeBarListener)

        apiFlow = ApiFlow(this, handler)

        createPlayer()
    }

    private fun createPlayer() {
        player = SimpleExoPlayer.Builder(this).build()
        player.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                super.onPlayerStateChanged(playWhenReady, playbackState)

                if (playbackState == PlaybackState.STATE_PLAYING) {
                    startUpdateProgressDelayed()
                }
            }
        })
        binding.playerView.player = player
        adsStreamPlayer = AdsStreamPlayer(player)
        adsStreamPlayer.setAdsStreamPlayerCallback(adsStreamPlayerCallback)
    }

    private fun prepareMediaSource(url: String) {
        mediaSource = DashMediaSource.Factory(DefaultHttpDataSourceFactory("Test"))
            .createMediaSource(Uri.parse(url))

        player.prepare(mediaSource)
    }

    private fun createAdsLoader() {
        val setting = sdkFactory.createImaSdkSettings()
        setting.isDebugMode = true
        val displayContainer = ImaSdkFactory.createStreamDisplayContainer(binding.adsView, adsStreamPlayer)
        adsLoader = sdkFactory.createAdsLoader(this, setting, displayContainer)
        adsLoader?.addAdsLoadedListener(this)
        adsLoader?.addAdErrorListener(adErrorEventListener)
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
        createAdsLoader()

        apiFlow.startFlow { data ->
            playbackInfoData = data
//            binding.playPauseBtn.visibility = View.VISIBLE
            requestAdContent()
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
        apiFlow.endFlow()
        releaseAdsLoader()
    }

    override fun onResume() {
        super.onResume()
        player.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()

        player.playWhenReady = false
    }

    private fun releaseAdsLoader() {
        streamManager?.removeAdEventListener(adEventListener)
        streamManager?.removeAdErrorListener(adErrorEventListener)
        streamManager?.destroy()
        streamManager = null
        adsLoader?.removeAdsLoadedListener(this)
        adsLoader?.removeAdErrorListener(adErrorEventListener)
        adsLoader?.release()
        adsLoader = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (player.isPlaying) {
            player.stop(true)
        }
        player.release()
        binding.progressBar.removeListener(timeBarListener)
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName

        // progress
        private const val UPDATE_PROGRESS_INTERVAL = 1000L

        // VOD DASH content source and video IDs.
        private const val TEST_DASH_CONTENT_SOURCE_ID = "2559737"
        private const val TEST_DASH_VIDEO_ID = "tos-dash"

        // Live DASH stream asset key.
        private const val TEST_DASH_ASSET_KEY = "PSzZMzAkSXCmlJOWDmRj8Q"
    }

    /**
     * Progress
     */
    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressAction = Runnable {
        startUpdateProgressDelayed()
    }
    private fun startUpdateProgressDelayed() {
        updateProgressAction.let {
            handler.removeCallbacks(it)
        }
        updateProgress()
//        requestNextContent()
        handler.postDelayed(
                updateProgressAction,
                UPDATE_PROGRESS_INTERVAL
        )
    }
    private fun updateProgress() {
        binding.progressBar.apply {
            setDuration(getContentTime(player.duration))
            setBufferedPosition(getContentTime(player.bufferedPosition))
            setPosition(getContentTime(player.currentPosition))
        }

        binding.progressCurrentPosition.text = getContentTime(player.currentPosition).getStringForTime()
        binding.progressDuration.text = getContentTime(player.duration).getStringForTime()
    }
    private fun requestNextContent() {
        val duration = getContentTime(player.duration)
        val currentPosition = getContentTime(player.currentPosition)
        if (currentPosition >= duration) {
            apiFlow.getNextContent("NEXT_CONTENT_ID") { data ->
                playbackInfoData = data
            }
            // prepare the media source and send it to player
            requestAdContent()
        }
    }

    /**
     * Callback of AdsLoadedListener
     * It is the response of ad request
     */
    override fun onAdsManagerLoaded(event: AdsManagerLoadedEvent?) {
        streamManager = event?.streamManager?.apply {
            addAdEventListener(adEventListener)
            addAdErrorListener(adErrorEventListener)
            init()
        }
    }

    /**
     * OnClick event
     */
    override fun onClick(view: View?) {
        when (view?.id) {
            binding.playPauseBtn.id -> {
                requestAdContent()
                binding.playPauseBtn.visibility = View.INVISIBLE
            }
        }
    }

    private fun requestAdContent() {
        when (adType) {
            AdType.VOD -> {
                sdkFactory.createVodStreamRequest(
                        playbackInfoData.sources[0].manifests[1].ssai?.google_dai?.vod?.content_source_id,
                        playbackInfoData.sources[0].manifests[1].ssai?.google_dai?.vod?.video_id,
                        null
                ).apply {
                    format = StreamRequest.StreamFormat.DASH
                }
            }
            AdType.Live -> {
                sdkFactory.createLiveStreamRequest(TEST_DASH_ASSET_KEY, null)
            }
        }.also {
            adsLoader?.requestStream(it)
        }
    }

    /**
     * Scrub event of progress bar
     */
    private val timeBarListener = object : TimeBar.OnScrubListener {
        override fun onScrubStart(timeBar: TimeBar, position: Long) {
            player.playWhenReady = false
            updateProgressAction.let {
                handler.removeCallbacks(it)
            }
        }

        override fun onScrubMove(timeBar: TimeBar, position: Long) {
            binding.progressCurrentPosition.text = position.getStringForTime()
        }

        override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
            if (canceled.not()) {
                player.seekTo(getStreamTime(position))
            }
            player.playWhenReady = true
//            startUpdateProgressDelayed()
        }
    }

    /**
     * Ad error event listener
     */
    private val adErrorEventListener = AdErrorEvent.AdErrorListener { event ->
        Log.d(TAG, "Error: ${event.error}")
    }

    /**
     * Ad event listener
     */
    private val adEventListener = AdEvent.AdEventListener { event ->
        if (event?.type != AdEvent.AdEventType.AD_PROGRESS) {
            Log.d(TAG, "Event: ${event?.type}")
        }
    }

    /**
     * Callback from AdsStreamPlayer
     */
    private val adsStreamPlayerCallback: AdsStreamPlayer.Callback = object : AdsStreamPlayer.Callback {
        override fun onVideoUrlLoaded(url: String) {
            Log.d(TAG, "onVideoUrlLoaded")
            prepareMediaSource(url)

            // Get Cue Points
            val list = getAllAdCuePoints()
            updateCuePoint(list)
        }

        override fun onAdBreakStarted() {
            Log.d(TAG, "onAdBreakStarted")
            binding.progressRegion.visibility = View.INVISIBLE
        }

        override fun onAdBreakEnded() {
            Log.d(TAG, "onAdBreakEnded")
            binding.progressRegion.visibility = View.VISIBLE
        }

        override fun onAdPeriodStarted() {
            Log.d(TAG, "onAdPeriodStarted")
        }

        override fun onAdPeriodEnded() {
            Log.d(TAG, "onAdPeriodEnded")
        }
    }

    fun getAllAdCuePoints(): List<CuePoint> {
        return streamManager?.cuePoints ?: emptyList()
    }

    private fun updateCuePoint(cuePoints: List<CuePoint>) {
        val adGroupTimesMs = mutableListOf<Long>()
        val adIsPlayedGroup = mutableListOf<Boolean>()
        cuePoints.forEach {
            adGroupTimesMs.add(getContentTime(it.startTime.toMilliSecond()))
            adIsPlayedGroup.add(it.isPlayed)
        }

        binding.progressBar.setAdGroupTimesMs(
                adGroupTimesMs.toLongArray(),
                adIsPlayedGroup.toBooleanArray(),
                cuePoints.size
        )
    }

    /**
     * Timeline conversion
     *
     * streamTime: include ad interval
     * contentTime: exclude ad interval
     */
    private fun getStreamTime(contentTime: Long): Long {
        // getStreamTimeForContentTime => second in second out
        return streamManager?.getStreamTimeForContentTime(contentTime.toSecond())?.toMilliSecond() ?: contentTime
    }
    private fun getContentTime(streamTime: Long): Long {
        // getContentTimeForStreamTime => second in second out
        return streamManager?.getContentTimeForStreamTime(streamTime.toSecond())?.toMilliSecond() ?: streamTime
    }
}