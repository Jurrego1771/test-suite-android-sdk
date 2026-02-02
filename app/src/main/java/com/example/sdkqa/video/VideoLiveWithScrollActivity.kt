package com.example.sdkqa.video

import am.mediastre.mediastreamplatformsdkandroid.MediastreamMiniPlayerConfig
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayer
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerCallback
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerConfig
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import com.example.sdkqa.R
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject

class VideoLiveWithScrollActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA-Scroll"
    }

    private var player: MediastreamPlayer? = null
    private lateinit var playerContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_live_scroll)

        playerContainer = findViewById(R.id.playerContainer)
        setupPlayer()
    }

    private fun setupPlayer() {
        val config = MediastreamPlayerConfig().apply {
            id = "5fd39e065d68477eaa1ccf5a" // Live ID
            type = MediastreamPlayerConfig.VideoTypes.LIVE
            pip = MediastreamPlayerConfig.FlagStatus.ENABLE // Critical for PiP
            
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }

        player = MediastreamPlayer(
            this,
            config,
            playerContainer,
            playerContainer,
            supportFragmentManager
        )

        player?.addPlayerCallback(createPlayerCallback())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
        // Trigger PiP when user goes Home/Recents
        player?.startPiP()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        player?.onPictureInPictureModeChanged(isInPictureInPictureMode)
        
        if (isInPictureInPictureMode) {
            Log.d(TAG, "Entered PiP Mode")
            supportActionBar?.hide()
            // Optional: Hide scroll view content if desired, 
            // but normally the system handles the window resize
        } else {
            Log.d(TAG, "Exited PiP Mode")
            supportActionBar?.show()
        }
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            override fun playerViewReady(msplayerView: PlayerView?) {
                Log.d(TAG, "playerViewReady")
            }

            override fun onReady() { Log.d(TAG, "onReady") }
            override fun onPlay() { Log.d(TAG, "onPlay") }
            override fun onPause() { Log.d(TAG, "onPause") }
            override fun onEnd() { Log.d(TAG, "onEnd") }
            override fun onBuffering() { Log.d(TAG, "onBuffering") }
            override fun onError(error: String?) { Log.e(TAG, "onError: $error") }
            override fun onDismissButton() { finish() }
            override fun onPlayerClosed() { finish() }
            
            // Boilerplate overrides
            override fun onNext() {}
            override fun onPrevious() {}
            override fun onFullscreen() { Log.d(TAG, "onFullscreen") }
            override fun offFullscreen() { Log.d(TAG, "offFullscreen") }
            override fun onNewSourceAdded(config: MediastreamPlayerConfig) {}
            override fun onLocalSourceAdded() {}
            override fun onAdEvents(type: AdEvent.AdEventType) { Log.d(TAG, "onAdEvents: ${type.name}") }
            override fun onAdErrorEvent(error: AdError) { Log.e(TAG, "onAdErrorEvent: ${error.message}") }
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
            override fun onPlayerReload() { Log.d(TAG, "onPlayerReload") }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player?.releasePlayer()
    }
}
