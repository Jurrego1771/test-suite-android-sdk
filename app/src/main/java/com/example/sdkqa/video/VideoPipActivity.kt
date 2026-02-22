package com.example.sdkqa.video

import am.mediastre.mediastreamplatformsdkandroid.MediastreamMiniPlayerConfig
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayer
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerCallback
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerConfig
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.ui.PlayerView
import com.example.sdkqa.testing.TestEventBus
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject

class VideoPipActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA"
    }

    private var player: MediastreamPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainMediaFrame = FrameLayout(this).apply {
            id = View.generateViewId()
            setBackgroundColor(Color.BLACK)
            keepScreenOn = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val playerContainer = FrameLayout(this).apply {
            id = View.generateViewId()
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        mainMediaFrame.addView(playerContainer)
        setContentView(mainMediaFrame)

        setupPlayer(mainMediaFrame)
    }

    private fun setupPlayer(mainMediaFrame: FrameLayout) {
        val config = MediastreamPlayerConfig().apply {
            id = "5fd39e065d68477eaa1ccf5a"
            type = MediastreamPlayerConfig.VideoTypes.LIVE
            pip = MediastreamPlayerConfig.FlagStatus.ENABLE
            enablePlayerZoom = true
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }

        player = MediastreamPlayer(
            this,
            config,
            mainMediaFrame,
            mainMediaFrame,
            supportFragmentManager
        )

        player?.addPlayerCallback(createPlayerCallback())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onUserLeaveHint() {
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
        } else {
            Log.d(TAG, "Exited PiP Mode")
            supportActionBar?.show()
        }
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            override fun playerViewReady(msplayerView: PlayerView?) {
                Log.d(TAG, "playerViewReady")
                TestEventBus.record(
                    name = "VideoPip.playerViewReady",
                    data = mapOf("hasPlayerView" to ((msplayerView != null).toString()))
                )
            }

            override fun onPlay() {
                Log.d(TAG, "onPlay")
                TestEventBus.record(name = "VideoPip.onPlay")
            }

            override fun onPause() {
                Log.d(TAG, "onPause")
                TestEventBus.record(name = "VideoPip.onPause")
            }

            override fun onReady() {
                Log.d(TAG, "onReady")
                TestEventBus.record(name = "VideoPip.onReady")
            }

            override fun onEnd() {
                Log.d(TAG, "onEnd")
            }

            override fun onBuffering() {
                Log.d(TAG, "onBuffering")
                TestEventBus.record(name = "VideoPip.onBuffering")
            }

            override fun onError(error: String?) {
                Log.e(TAG, "onError: $error")
                TestEventBus.record(name = "VideoPip.onError", data = mapOf("error" to (error ?: "")))
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
