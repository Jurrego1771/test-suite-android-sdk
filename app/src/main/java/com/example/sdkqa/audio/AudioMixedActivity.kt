package com.example.sdkqa.audio

import am.mediastre.mediastreamplatformsdkandroid.MediastreamMiniPlayerConfig
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayer
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerCallback
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerConfig
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import com.example.sdkqa.R
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject

class AudioMixedActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA"

        // Content IDs
        private const val AOD_ID = "696c625d32ce0ef08ca5ef9d"
        private const val LIVE_ID = "6994f6128e4ea871f11730b9"
        private const val EPISODE_ID = "696c6c6f6aaa3609e0741f3e"
    }

    private lateinit var container: FrameLayout
    private lateinit var spinnerMode: Spinner
    private var player: MediastreamPlayer? = null

    private var currentMode = "Local"
    private val modes = arrayOf("Local", "Audio Simple", "Episode", "Live")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_live_dvr)

        initializeViews()
        setupSpinner()
        initializePlayer()
    }

    private fun initializeViews() {
        container = findViewById(R.id.main_media_frame)
        spinnerMode = findViewById(R.id.spinnerMode)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_item, modes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerMode.adapter = adapter

        spinnerMode.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedMode = modes[position]
                if (selectedMode != currentMode) {
                    switchToMode(selectedMode)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initializePlayer() {
        val config = createConfigForMode("Local")

        player = MediastreamPlayer(
            this,
            config,
            container,
            container,
            supportFragmentManager
        )

        player?.addPlayerCallback(createPlayerCallback())
    }

    private fun switchToMode(mode: String) {
        Log.d(TAG, "Switching to mode: $mode")
        currentMode = mode
        val newConfig = createConfigForMode(mode)
        player?.reloadPlayer(newConfig)
    }

    private fun createConfigForMode(mode: String): MediastreamPlayerConfig {
        val config = MediastreamPlayerConfig().apply {
            playerType = MediastreamPlayerConfig.PlayerType.AUDIO
            showControls = true
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }

        when (mode) {
            "Local" -> {
                config.src = "android.resource://${packageName}/raw/sample_audio"
                config.id = "local-audio"
                config.type = MediastreamPlayerConfig.VideoTypes.VOD
                Log.d(TAG, "Config: Local mode")
            }
            "Audio Simple" -> {
                config.id = AOD_ID
                config.type = MediastreamPlayerConfig.VideoTypes.VOD
                config.videoFormat = MediastreamPlayerConfig.AudioVideoFormat.MP3
                Log.d(TAG, "Config: Audio Simple (AOD) mode - id: $AOD_ID")
            }
            "Episode" -> {
                config.id = EPISODE_ID
                config.type = MediastreamPlayerConfig.VideoTypes.EPISODE
                config.videoFormat = MediastreamPlayerConfig.AudioVideoFormat.MP3
                config.loadNextAutomatically = true
                Log.d(TAG, "Config: Episode mode - id: $EPISODE_ID")
            }
            "Live" -> {
                config.id = LIVE_ID
                config.type = MediastreamPlayerConfig.VideoTypes.LIVE
                Log.d(TAG, "Config: Live mode - id: $LIVE_ID")
            }
        }

        return config
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            override fun playerViewReady(msplayerView: PlayerView?) {
                Log.d(TAG, "playerViewReady")
            }

            override fun onPlay() {
                Log.d(TAG, "onPlay")
            }

            override fun onPause() {
                Log.d(TAG, "onPause")
            }

            override fun onReady() {
                Log.d(TAG, "onReady")
            }

            override fun onEnd() {
                Log.d(TAG, "onEnd")
            }

            override fun onBuffering() {
                Log.d(TAG, "onBuffering")
            }

            override fun onError(error: String?) {
                Log.e(TAG, "onError: $error")
            }

            override fun onDismissButton() {
                Log.d(TAG, "onDismissButton")
            }

            override fun onPlayerClosed() {
                Log.d(TAG, "onPlayerClosed")
                finish()
            }

            override fun onNext() {}
            override fun onPrevious() {}
            override fun onFullscreen() {
                Log.d(TAG, "onFullscreen")
            }

            override fun offFullscreen() {
                Log.d(TAG, "offFullscreen")
            }

            override fun onNewSourceAdded(config: MediastreamPlayerConfig) {}
            override fun onLocalSourceAdded() {
                Log.d(TAG, "onLocalSourceAdded")
            }

            override fun onAdEvents(type: AdEvent.AdEventType) {
                Log.d(TAG, "onAdEvents: ${type.name}")
            }

            override fun onAdErrorEvent(error: AdError) {
                Log.e(TAG, "onAdErrorEvent: ${error.message}")
            }

            override fun onConfigChange(config: MediastreamMiniPlayerConfig?) {}
            override fun onCastAvailable(state: Boolean?) {}
            override fun onCastSessionStarting() {}
            override fun onCastSessionStarted() {}
            override fun onCastSessionStartFailed() {}
            override fun onCastSessionEnding() {}
            override fun onCastSessionEnded() {}
            override fun onCastSessionResuming() {}
            override fun onCastSessionResumed() {}
            override fun onCastSessionResumeFailed() {}
            override fun onCastSessionSuspended() {}
            override fun onPlaybackErrors(error: JSONObject?) {
                Log.e(TAG, "onPlaybackErrors: $error")
            }

            override fun onEmbedErrors(error: JSONObject?) {
                Log.e(TAG, "onEmbedErrors: $error")
            }

            override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {
                Log.d(TAG, "onLiveAudioCurrentSongChanged: $data")
            }

            override fun onPlayerReload() {
                Log.d(TAG, "onPlayerReload")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.releasePlayer()
    }
}
