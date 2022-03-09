# IMADemo
1. [Google DAI implements sample code and integrates with Playcraft API](#google-dai)
2. [For self-linear, show the timing of request next content](#self-linear)

## Adding dependency
- API corresponding
    - ${projectRoot}\app\libs\kks-network.aar
    - com.squareup.okhttp3
    - com.google.code.gson:gson
- Player
    - ExoPlayer
- Google IMA DAI SDK
    - com.google.ads.interactivemedia.v3

#### Turn on Java 8 support
If not enabled already, you need to turn on Java 8 support in all build.gradle files depending on ExoPlayer, by adding the following to the android section:
```
compileOptions {
  targetCompatibility JavaVersion.VERSION_1_8
}
```
#### Enable multidex
If your Gradle minSdkVersion is 20 or lower, you should enable multidex in order to prevent build errors.

## Google DAI

### Step1. Create player and adLoader
- create the exoplayer and wrap into AdsStreamPlayer which implement the IMA interface (VideoStreamPlayer)
```kotlin
val player = SimpleExoPlayer.Builder(this).build()
binding.playerView.player = player
val adsStreamPlayer = AdsStreamPlayer(player)
```
- create the adsLoader
```kotlin
val setting = sdkFactory.createImaSdkSettings()
val displayContainer = ImaSdkFactory.createStreamDisplayContainer(binding.adsView, adsStreamPlayer)
val adsLoader = sdkFactory.createAdsLoader(this, setting, displayContainer)
adsLoader.addAdsLoadedListener(this)
```
#### For more details, please check `createPlayer()` and `createAdsLoader()` methods.

### Step2. Start playback API flow
- The following information must be initialized to request the backend server. (Please search for "API info" in this demo.
- **☆☆☆ It is currently set to an empty string, so if the correct information is not filled in, this demo will not function properly.**
```kotlin
// API Info
private const val HOST_URL = ""             // https://xxxxxx.xxxx.xxxx
private const val CONTENT_ID = ""           // ex: 1, 20, ...etc
private const val ACCESS_TOKEN = ""         // token
```
- Get videoInfo, start playback session and playback information. The playback information includes "CONTENT_SOURCE_ID" and "VIDEO_ID", which will be used when sending DAI requests for VOD streams.
```kotlin
ioScope.launch {
    val videoInfo = requestVideoInfo(CONTENT_ID)
    contentType = videoInfo!!.type.toString().toLowerCase(Locale.ROOT)

    val startSessionData = startPlaybackSession(CONTENT_ID, contentType, "")!!
    playbackToken = startSessionData.token

    // Polling heartbeat if playback token is exist.
    pollingHeartbeat(CONTENT_ID, contentType, playbackToken)

    //get manifest
    val playbackInfoData = getPlaybackInfo(CONTENT_ID, contentType, startSessionData.token)!!
    withContext(Dispatchers.Main) {
        callback(playbackInfoData)
    }
}
```
#### For more details, please check `APIFlow` class.

### Step3. Request the streaming which inserted ads
- request the streaming with PlaybackInfo
```kotlin
val request = sdkFactory.createVodStreamRequest(
        playbackInfoData.sources[0].manifests[1].ssai?.google_dai?.vod?.content_source_id,
        playbackInfoData.sources[0].manifests[1].ssai?.google_dai?.vod?.video_id,
        null
).apply {
    format = StreamRequest.StreamFormat.DASH
}
adsLoader.requestStream(request)
```
- Since the `AdsLoadedListener` and  is registered in adsLoader, the response can be obtained in the `onAdsManagerLoaded(event: AdsManagerLoadedEvent?)` method. Then, initialize `streamManager`
```kotlin
override fun onAdsManagerLoaded(event: AdsManagerLoadedEvent?) {
    streamManager = event?.streamManager?.apply {
        addAdEventListener(adEventListener)
        addAdErrorListener(adErrorEventListener)
        init()
    }
}
```
- On the other hand, adsLoader owns `displayContainer` which owned `adsStreamPlayer` which implemented `VideoStreamPlayer` interface. So, the callback method `loadUrl` will be trigger.
```kotlin
override fun loadUrl(url: String?, subtitle: MutableList<HashMap<String, String>>?) {
    // here you can get the manifest with ads inserted
    callback?.onVideoUrlLoaded(url ?: "")
    setupMetaOutputCallback() // for Live streaming
}
```

### Step4. Prepare MediaSource and get ads cue points
```kotlin
override fun onVideoUrlLoaded(url: String) {
    prepareMediaSource(url)

    // Get Cue Points
    val list = getAllAdCuePoints()
    updateCuePoint(list)
}
```

### Step5. Progress bar and timeline conversion
- ExoPlayer provides a wedgit called `DefaultTimeBar` which implements cue point markers. It's easy to style with its properties.
```kotlin
binding.progressBar.apply {
    setDuration(getContentTime(player.duration))
    setBufferedPosition(getContentTime(player.bufferedPosition))
    setPosition(getContentTime(player.currentPosition))
}
```
- Because player runs on stream timeline. It include ads duration. Please use below methods processing the conversion. For more details, please search `Timeline conversion` in this demo
```kotlin
private fun getStreamTime(contentTime: Long): Long {
    // getStreamTimeForContentTime => second in second out
    return streamManager?.getStreamTimeForContentTime(contentTime.toSecond())?.toMilliSecond() ?: contentTime
}
private fun getContentTime(streamTime: Long): Long {
    // getContentTimeForStreamTime => second in second out
    return streamManager?.getContentTimeForStreamTime(streamTime.toSecond())?.toMilliSecond() ?: streamTime
}
```
- Seeking
    - Because we set the duration of the progress bar to the content timeline, we need to convert the seeking position to the stream timeline, and then let the player seek to the correct position.
    - For more details, please search `Scrub event of progress bar` in this demo.
```kotlin
override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {
    if (canceled.not()) {
        player.seekTo(getStreamTime(position))
    }
}
```

### Step6. End playback API flow
- For more details, please search `endFlow()` in this demo.
```kotlin
ioScope.launch {
    // update last position, if needed

    // stop heart beat
    pollingHeartbeatAction?.let {
        handler.removeCallbacks(it)
    }

    // end start session
    endStartSession(CONTENT_ID, contentType, playbackToken)
}
```

## Self linear
- By monitoring the current position, if the total content duration is reached, request the next content and set the returned data to the player.
- Please reference the `requestNextContent()` and `apiFlow.getNextContent()` method for more details.
```kotlin
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
```