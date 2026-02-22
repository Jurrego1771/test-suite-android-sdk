package com.example.sdkqa.video

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
import com.example.sdkqa.testing.TestEventBus
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class VideoLiveDvrActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA"
    }

    private lateinit var container: FrameLayout
    private lateinit var spinnerMode: Spinner
    private var player: MediastreamPlayer? = null

    private var currentMode = "Live"
    private val modes = arrayOf("Live", "DVR", "DVR Start", "DVR VOD")

    // Base configuration
    private val baseId = "5fd39e065d68477eaa1ccf5a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_live_dvr)

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
        val config = createConfigForMode("Live")
        player = MediastreamPlayer(this, config, container, container, supportFragmentManager)
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
            id = baseId
            type = MediastreamPlayerConfig.VideoTypes.LIVE
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }

        when (mode) {
            "Live" -> {
                // Default live configuration
                Log.d(TAG, "Config: Live mode")
            }
            "DVR" -> {
                config.dvr = true
                config.windowDvr = 60 // Request 60 minutes DVR window
                Log.d(TAG, "Config: DVR mode with 60min window")
            }
            "DVR Start" -> {
                config.dvr = true
                config.dvrStart = getDvrStartTime()
                Log.d(TAG, "Config: DVR Start mode - dvrStart: ${config.dvrStart}")
            }
            "DVR VOD" -> {
                config.dvr = true
                config.dvrStart = getDvrVodStartTime()
                config.dvrEnd = getDvrVodEndTime()
                Log.d(TAG, "Config: DVR VOD mode - dvrStart: ${config.dvrStart}, dvrEnd: ${config.dvrEnd}")
            }
        }

        return config
    }

    private fun getDvrStartTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        calendar.add(Calendar.MINUTE, -30)
        return formatDateToISO(calendar.time)
    }

    private fun getDvrVodStartTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.MINUTE, -30)
        return formatDateToISO(calendar.time)
    }

    private fun getDvrVodEndTime(): String {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.add(Calendar.MINUTE, -10)
        return formatDateToISO(calendar.time)
    }

    private fun formatDateToISO(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            override fun playerViewReady(msplayerView: PlayerView?) {
                Log.d(TAG, "playerViewReady")
                TestEventBus.record(
                    name = "VideoLiveDvr.playerViewReady",
                    data = mapOf("hasPlayerView" to ((msplayerView != null).toString()))
                )
            }

            override fun onPlay() {
                Log.d(TAG, "onPlay")
                TestEventBus.record(name = "VideoLiveDvr.onPlay")
            }

            override fun onPause() {
                Log.d(TAG, "onPause")
                TestEventBus.record(name = "VideoLiveDvr.onPause")
            }

            override fun onReady() {
                Log.d(TAG, "onReady")
                TestEventBus.record(name = "VideoLiveDvr.onReady")
            }

            override fun onEnd() {
                Log.d(TAG, "onEnd")
            }

            override fun onBuffering() {
                Log.d(TAG, "onBuffering")
                TestEventBus.record(name = "VideoLiveDvr.onBuffering")
            }

            override fun onError(error: String?) {
                Log.e(TAG, "onError: $error")
                TestEventBus.record(name = "VideoLiveDvr.onError", data = mapOf("error" to (error ?: "")))
            }

            override fun onDismissButton() {
                Log.d(TAG, "onDismissButton")
            }

            override fun onPlayerClosed() {
                Log.d(TAG, "onPlayerClosed")
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
            override fun onLocalSourceAdded() {}

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

            override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {}

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
