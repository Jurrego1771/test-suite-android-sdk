package com.example.sdkqa.audio

import am.mediastre.mediastreamplatformsdkandroid.MediastreamMiniPlayerConfig
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerCallback
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerConfig
import am.mediastre.mediastreamplatformsdkandroid.MediastreamPlayerServiceWithSync
import am.mediastre.mediastreamplatformsdkandroid.MessageEvent
import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerView
import com.example.sdkqa.R
import com.example.sdkqa.testing.TestEventBus
import com.google.ads.interactivemedia.v3.api.AdError
import com.google.ads.interactivemedia.v3.api.AdEvent
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject

class AudioEpisodeActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA"
    }

    private lateinit var container: FrameLayout
    private lateinit var playerView: PlayerView
    private lateinit var miniPlayerConfig: MediastreamMiniPlayerConfig
    private lateinit var controllerFuture: ListenableFuture<MediaController>

    private val controller: MediaController?
        get() = if (controllerFuture.isDone && !controllerFuture.isCancelled) controllerFuture.get() else null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_with_service)

        container = findViewById(R.id.main_media_frame)
        playerView = findViewById(R.id.player_view)

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        startService(createConfig())
    }

    private fun createConfig(): MediastreamPlayerConfig {
        return MediastreamPlayerConfig().apply {
            id = "696c6c6f6aaa3609e0741f3e"
            type = MediastreamPlayerConfig.VideoTypes.EPISODE
            showControls = true
            videoFormat = MediastreamPlayerConfig.AudioVideoFormat.MP3
            loadNextAutomatically = true
            //Uncomment to use development environment
            //environment = MediastreamPlayerConfig.Environment.DEV
        }
    }

    private fun startService(config: MediastreamPlayerConfig) {
        miniPlayerConfig = MediastreamMiniPlayerConfig()

        MediastreamPlayerServiceWithSync.initializeService(
            this,
            this@AudioEpisodeActivity,
            config,
            container,
            playerView,
            miniPlayerConfig,
            false,
            config.accountID ?: "",
            createPlayerCallback()
        )

        try {
            controllerFuture = MediaController.Builder(
                this,
                SessionToken(this, ComponentName(this, MediastreamPlayerServiceWithSync::class.java)),
            ).buildAsync()

            controllerFuture.addListener({ setController(config) }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting service: $e")
        }
    }

    private fun setController(config: MediastreamPlayerConfig) {
        val controller = this.controller ?: return
        playerView.player = controller
        playerView.useController = true
        EventBus.getDefault().post(MessageEvent(controller, config))
    }

    private fun createPlayerCallback(): MediastreamPlayerCallback {
        return object : MediastreamPlayerCallback {
            override fun playerViewReady(msplayerView: PlayerView?) {
                Log.d(TAG, "playerViewReady")
                TestEventBus.record(
                    name = "AudioEpisode.playerViewReady",
                    data = mapOf("hasPlayerView" to ((msplayerView != null).toString()))
                )
            }

            override fun onPlay() {
                Log.d(TAG, "onPlay")
                TestEventBus.record(name = "AudioEpisode.onPlay")
            }

            override fun onPause() {
                Log.d(TAG, "onPause")
                TestEventBus.record(name = "AudioEpisode.onPause")
            }

            override fun onReady() {
                Log.d(TAG, "onReady")
                TestEventBus.record(name = "AudioEpisode.onReady")
            }

            override fun onEnd() {
                Log.d(TAG, "onEnd")
            }

            override fun onBuffering() {
                Log.d(TAG, "onBuffering")
                TestEventBus.record(name = "AudioEpisode.onBuffering")
            }

            override fun onError(error: String?) {
                Log.e(TAG, "onError: $error")
                TestEventBus.record(name = "AudioEpisode.onError", data = mapOf("error" to (error ?: "")))
                Toast.makeText(this@AudioEpisodeActivity, error, Toast.LENGTH_LONG).show()
            }

            override fun onDismissButton() {
                Log.d(TAG, "onDismissButton")
            }

            override fun onPlayerClosed() {
                Log.d(TAG, "onPlayerClosed")
                finish()
            }

            override fun onNext() {
                Log.d(TAG, "onNext")
            }

            override fun onPrevious() {
                Log.d(TAG, "onPrevious")
            }

            override fun onFullscreen() {
                Log.d(TAG, "onFullscreen")
            }

            override fun offFullscreen() {
                Log.d(TAG, "offFullscreen")
            }

            override fun onNewSourceAdded(config: MediastreamPlayerConfig) {
                Log.d(TAG, "onNewSourceAdded")
            }

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

            override fun onLiveAudioCurrentSongChanged(data: JSONObject?) {
                Log.d(TAG, "onLiveAudioCurrentSongChanged: $data")
            }

            override fun onPlayerReload() {
                Log.d(TAG, "onPlayerReload")
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        releaseService()
    }

    private fun releaseService() {
        Log.d(TAG, "Release service called")
        playerView.player?.release()
        playerView.player = null
        releaseController()
        val stopIntent = Intent(this, MediastreamPlayerServiceWithSync::class.java)
        stopIntent.action = "STOP_SERVICE"
        startService(stopIntent)
    }

    private fun releaseController() {
        if (::controllerFuture.isInitialized) {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(applicationContext, "Notification permission denied", Toast.LENGTH_LONG).show()
        }
    }
}
