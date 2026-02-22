package com.example.sdkqa.video

import am.mediastre.mediastreamplatformsdkandroid.MediastreamMiniPlayerConfig
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayer
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerCallback
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerConfig
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import com.example.sdkqa.testing.TestEventBus
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject

class VideoEpisodeManualActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA-NextEp"
    }

    private var player: MediastreamPlayer? = null
    private lateinit var container: FrameLayout

    // Simulated Playlist
    private val playlist = listOf(
        "696c5d9f76d96a30f6542a8a", // Video 1
        "696c5dbb76d96a30f6542ab7", // Video 2
        "69839661dabbf9ca486830c6"  // Video 3
    )
    private var currentIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programmatic Layout (Full Screen FrameLayout)
        container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            keepScreenOn = true
        }
        setContentView(container)

        setupPlayer(currentIndex)
    }

    private fun setupPlayer(index: Int) {
        if (index >= playlist.size) return

        val config = createConfigFor(index)
        
        // Initial setup
        player = MediastreamPlayer(
            this,
            config,
            container,
            container,
            supportFragmentManager
        )
        player?.addPlayerCallback(createPlayerCallback())
    }

    private fun createConfigFor(index: Int): MediastreamPlayerConfig {
        return MediastreamPlayerConfig().apply {
            id = playlist[index]
            type = MediastreamPlayerConfig.VideoTypes.EPISODE // Critical for overlay
            
            // Manual Mode Configuration
            // Check if there is a next video
            if (index + 1 < playlist.size) {
                nextEpisodeId = playlist[index + 1] // Set next ID
                nextEpisodeTime = 15 // Show 15s before end
            } else {
                nextEpisodeId = null // End of playlist
            }
            
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            
            override fun nextEpisodeIncoming(nextEpisodeId: String) {
                Log.d(TAG, "Next Episode Incoming! Prepare id: $nextEpisodeId")
                
                // Advance index
                val nextIndex = currentIndex + 1
                if (nextIndex < playlist.size) {
                    currentIndex = nextIndex
                    
                    // Create config for the incoming video
                    // IMPORTANT: We must also predict the "next-next" for the chain to continue
                    val newConfig = createConfigFor(currentIndex)
                    
                    // Confirm to SDK
                    Log.d(TAG, "Updating Next Episode with: ${newConfig.id} -> Next: ${newConfig.nextEpisodeId}")
                    player?.updateNextEpisode(newConfig)
                }
            }

            override fun onNewSourceAdded(config: MediastreamPlayerConfig) {
                Log.d(TAG, "New Source Added (Playing): ${config.id}")
                Toast.makeText(this@VideoEpisodeManualActivity, "Playing: ${config.id}", Toast.LENGTH_SHORT).show()
            }

            // Standard callbacks
            override fun playerViewReady(msplayerView: PlayerView?) {
                TestEventBus.record(
                    name = "VideoEpisodeManual.playerViewReady",
                    data = mapOf("hasPlayerView" to ((msplayerView != null).toString()))
                )
            }
            override fun onPlay() {
                Log.d(TAG, "onPlay")
                TestEventBus.record(name = "VideoEpisodeManual.onPlay")
            }
            override fun onPause() {
                Log.d(TAG, "onPause")
                TestEventBus.record(name = "VideoEpisodeManual.onPause")
            }
            override fun onReady() {
                Log.d(TAG, "onReady")
                TestEventBus.record(name = "VideoEpisodeManual.onReady")
            }
            override fun onEnd() { 
                Log.d(TAG, "onEnd")
                if (currentIndex >= playlist.size - 1) {
                    Toast.makeText(this@VideoEpisodeManualActivity, "Playlist Ended", Toast.LENGTH_LONG).show()
                }
            }
            override fun onBuffering() {
                Log.d(TAG, "onBuffering")
                TestEventBus.record(name = "VideoEpisodeManual.onBuffering")
            }
            override fun onError(error: String?) {
                Log.e(TAG, "onError: $error")
                TestEventBus.record(name = "VideoEpisodeManual.onError", data = mapOf("error" to (error ?: "")))
            }
            override fun onDismissButton() { finish() }
            override fun onPlayerClosed() { finish() }
            override fun onNext() {}
            override fun onPrevious() {}
            override fun onFullscreen() {}
            override fun offFullscreen() {}
            override fun onLocalSourceAdded() {}
            override fun onAdEvents(type: AdEvent.AdEventType) {}
            override fun onAdErrorEvent(error: AdError) {}
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
            override fun onPlaybackErrors(error: JSONObject?) {}
            override fun onEmbedErrors(error: JSONObject?) {}
            override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {}
            override fun onPlayerReload() {}
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.releasePlayer()
    }
}
