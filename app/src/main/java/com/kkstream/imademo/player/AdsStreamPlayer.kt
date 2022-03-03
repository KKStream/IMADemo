package com.kkstream.imademo.player

import android.util.Log
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate
import com.google.ads.interactivemedia.v3.api.player.VideoStreamPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.metadata.emsg.EventMessage
import com.google.android.exoplayer2.metadata.id3.TextInformationFrame
import java.util.HashMap

class AdsStreamPlayer(private val player: SimpleExoPlayer) : VideoStreamPlayer {

    interface Callback {
        fun onVideoUrlLoaded(url: String)

        fun onAdBreakStarted()

        fun onAdBreakEnded()

        fun onAdPeriodStarted()

        fun onAdPeriodEnded()
    }

    private val videoStreamPlayerCallback: MutableList<VideoStreamPlayer.VideoStreamPlayerCallback> =
        mutableListOf()

    private var callback: Callback? = null

    fun setAdsStreamPlayerCallback(callback: Callback) {
        this.callback = callback
    }

    private fun setupMetaOutputCallback() {
        player.addMetadataOutput { metadata ->
            for (i in 0 until metadata.length()) {
                val entry: Metadata.Entry = metadata.get(i)
                if (entry is TextInformationFrame) {
                    if ("TXXX" == entry.id) {
                        Log.d(TAG, "Received user text: " + entry.value)
                        for (callback in videoStreamPlayerCallback) {
                            callback.onUserTextReceived(entry.value)
                        }
                    }
                } else if (entry is EventMessage) {
                    val eventMessageValue = String(entry.messageData)
                    Log.d(TAG, "Received user text: $eventMessageValue")
                    for (callback in videoStreamPlayerCallback) {
                        callback.onUserTextReceived(eventMessageValue)
                    }
                }
            }
        }
    }

    override fun getContentProgress(): VideoProgressUpdate {
        return VideoProgressUpdate(player.currentPosition, player.duration)
    }

    override fun getVolume(): Int {
        return 100
    }

    override fun addCallback(callback: VideoStreamPlayer.VideoStreamPlayerCallback?) {
        videoStreamPlayerCallback.add(callback ?: return)
    }

    override fun loadUrl(url: String?, subtitle: MutableList<HashMap<String, String>>?) {
        callback?.onVideoUrlLoaded(url ?: "")
        setupMetaOutputCallback()
    }

    override fun onAdBreakEnded() {
        callback?.onAdBreakEnded()
    }

    override fun onAdBreakStarted() {
        callback?.onAdBreakStarted()
    }

    override fun onAdPeriodEnded() {
        callback?.onAdPeriodEnded()
    }

    override fun onAdPeriodStarted() {
        callback?.onAdPeriodStarted()
    }

    override fun pause() {
        player.playWhenReady = false
    }

    override fun removeCallback(callback: VideoStreamPlayer.VideoStreamPlayerCallback?) {
        videoStreamPlayerCallback.remove(callback ?: return)
    }

    override fun resume() {
        player.playWhenReady = true
    }

    override fun seek(position: Long) {
        player.seekTo(position)
    }

    companion object {
        private val TAG = AdsStreamPlayer::class.java.simpleName
    }

}