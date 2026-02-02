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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject

/**
 * Test Activity for Next Episode AUTOMATIC Mode (API Driven).
 * 
 * In this mode, the SDK fetches the "next" episode ID directly from the backend API.
 * The app does NOT need to call updateNextEpisode().
 * 
 * Preconditions:
 * - The video ID used must have a configured "Next Episode" in the Mediastream backend.
 */
class VideoEpisodeAutomaticActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA-AutoEp"
    }

    private var player: MediastreamPlayer? = null
    private lateinit var container: FrameLayout
    private lateinit var infoText: TextView

    // ID known to have next episode data (or intended to be tested for it)
    private val TEST_VIDEO_ID = "696c5d9f76d96a30f6542a8a"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Programmatic Layout
        container = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            keepScreenOn = true
        }

        // Add an info overlay to explain what's happening
        infoText = TextView(this).apply {
            text = "AUTOMATIC MODE\nWaiting for API metadata..."
            setTextColor(Color.YELLOW)
            setPadding(30, 100, 30, 30)
            textSize = 14f
        }
        container.addView(infoText)
        
        setContentView(container)

        setupPlayer()
    }

    private fun setupPlayer() {
        val config = MediastreamPlayerConfig().apply {
            id = TEST_VIDEO_ID
            type = MediastreamPlayerConfig.VideoTypes.EPISODE
            loadNextAutomatically = true // Ensure this is enabled (default for EPISODE)
            
            // IMPORTANT: We do NOT set nextEpisodeId manually here.
            // We rely 100% on the API response.
            
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }

        player = MediastreamPlayer(
            this,
            config,
            container,
            container,
            supportFragmentManager
        )
        player?.addPlayerCallback(createPlayerCallback())
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            
            override fun nextEpisodeIncoming(nextEpisodeId: String) {
                // This callback confirms that the API successfully returned a "next" ID
                Log.d(TAG, "API SUCCESS: Received next episode ID from backend: $nextEpisodeId")
                
                runOnUiThread {
                    infoText.text = "AUTOMATIC MODE\nSUCCESS! Next ID found: $nextEpisodeId\nOverlay will appear automatically."
                    infoText.setTextColor(Color.GREEN)
                    Toast.makeText(this@VideoEpisodeAutomaticActivity, "API Found Next: $nextEpisodeId", Toast.LENGTH_LONG).show()
                }
                
                // NO ACTION REQUIRED (No updateNextEpisode needed)
            }

            override fun onNewSourceAdded(config: MediastreamPlayerConfig) {
                Log.d(TAG, "New Source Added (Playing): ${config.id}")
                runOnUiThread {
                    infoText.text = "Now Playing: ${config.id}\nWait for end..."
                }
                Toast.makeText(this@VideoEpisodeAutomaticActivity, "Playing: ${config.id}", Toast.LENGTH_SHORT).show()
            }

            // Standard callbacks
            override fun playerViewReady(msplayerView: PlayerView?) {}
            override fun onPlay() { Log.d(TAG, "onPlay") }
            override fun onPause() { Log.d(TAG, "onPause") }
            override fun onReady() { Log.d(TAG, "onReady") }
            override fun onEnd() { Log.d(TAG, "onEnd") }
            override fun onBuffering() { Log.d(TAG, "onBuffering") }
            override fun onError(error: String?) { 
                Log.e(TAG, "onError: $error")
                runOnUiThread { infoText.text = "Error: $error"; infoText.setTextColor(Color.RED) }
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
