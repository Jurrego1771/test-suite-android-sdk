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
import com.example.sdkqa.R
import com.example.sdkqa.testing.TestEventBus
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import org.json.JSONObject

class VideoVodPipActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA-VOD-PiP"
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
            id = "696bc8a832ce0ef08c6fa0ef"
            type = MediastreamPlayerConfig.VideoTypes.VOD
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
                    name = "VideoVodPip.playerViewReady",
                    data = mapOf("hasPlayerView" to ((msplayerView != null).toString()))
                )
            }

            override fun onReady() {
                Log.d(TAG, "onReady")
                TestEventBus.record(name = "VideoVodPip.onReady")
            }
            override fun onPlay() {
                Log.d(TAG, "onPlay")
                TestEventBus.record(name = "VideoVodPip.onPlay")
            }
            override fun onPause() {
                Log.d(TAG, "onPause")
                TestEventBus.record(name = "VideoVodPip.onPause")
            }
            override fun onEnd() { Log.d(TAG, "onEnd") }
            override fun onBuffering() {
                Log.d(TAG, "onBuffering")
                TestEventBus.record(name = "VideoVodPip.onBuffering")
            }
            override fun onError(error: String?) {
                Log.e(TAG, "onError: $error")
                TestEventBus.record(name = "VideoVodPip.onError", data = mapOf("error" to (error ?: "")))
            }
            override fun onDismissButton() { finish() }
            override fun onPlayerClosed() { finish() }
            
            override fun onNext() {}
            override fun onPrevious() {}
            override fun onFullscreen() {}
            override fun offFullscreen() {}
            override fun onNewSourceAdded(config: MediastreamPlayerConfig) {}
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
