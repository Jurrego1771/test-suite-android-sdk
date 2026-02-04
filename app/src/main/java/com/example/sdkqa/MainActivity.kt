package com.example.sdkqa

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sdkqa.audio.AudioAodSimpleActivity
import com.example.sdkqa.audio.AudioAodWithServiceActivity
import com.example.sdkqa.audio.AudioEpisodeActivity
import com.example.sdkqa.audio.AudioLiveActivity
import com.example.sdkqa.audio.AudioLocalActivity
import com.example.sdkqa.audio.AudioLocalWithServiceActivity
import com.example.sdkqa.audio.AudioLiveDvrActivity
import com.example.sdkqa.audio.AudioLiveWithServiceActivity
import com.example.sdkqa.audio.AudioMixedActivity
import com.example.sdkqa.audio.AudioMixedWithServiceActivity
import com.example.sdkqa.video.VideoEpisodeActivity
import com.example.sdkqa.video.VideoLiveActivity
import com.example.sdkqa.video.VideoLiveDvrActivity
import com.example.sdkqa.video.VideoLocalActivity
import com.example.sdkqa.video.VideoLocalWithServiceActivity
import com.example.sdkqa.video.VideoMixedActivity
import com.example.sdkqa.video.VideoMixedWithServiceActivity
import com.example.sdkqa.video.VideoVodSimpleActivity

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SDK-QA"
        private const val SDK_VERSION = "10.0.0-alpha.03"
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TestCaseAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupHeader()
        setupRecyclerView()
    }

    private fun setupHeader() {
        val tvVersion = findViewById<android.widget.TextView>(R.id.tvSdkVersion)
        tvVersion.text = "v$SDK_VERSION"
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerTestCases)

        val testCases = TestCase.getAllTestCases()

        adapter = TestCaseAdapter(testCases) { testCase ->
            onTestCaseClicked(testCase)
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun onTestCaseClicked(testCase: TestCase) {
        Log.d(TAG, "Selected test case: ${testCase.getDisplayTitle()} (${testCase.type})")

        // TODO: Launch corresponding activity based on test case type
        when (testCase.type) {
            TestCase.TestCaseType.AUDIO_AOD_SIMPLE -> {
                Log.d(TAG, "Launching Audio AOD Simple test...")
                startActivity(Intent(this, AudioAodSimpleActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_AOD_WITH_SERVICE -> {
                Log.d(TAG, "Launching Audio AOD with Service test...")
                startActivity(Intent(this, AudioAodWithServiceActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_EPISODE -> {
                Log.d(TAG, "Launching Audio Episode test...")
                startActivity(Intent(this, AudioEpisodeActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_LOCAL -> {
                Log.d(TAG, "Launching Audio Local test...")
                startActivity(Intent(this, AudioLocalActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_LOCAL_WITH_SERVICE -> {
                Log.d(TAG, "Launching Audio Local with Service test...")
                startActivity(Intent(this, AudioLocalWithServiceActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_LIVE -> {
                Log.d(TAG, "Launching Audio Live test...")
                startActivity(Intent(this, AudioLiveActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_LIVE_WITH_SERVICE -> {
                Log.d(TAG, "Launching Audio Live with Service test...")
                startActivity(Intent(this, AudioLiveWithServiceActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_LIVE_DVR -> {
                Log.d(TAG, "Launching Audio Live DVR test...")
                startActivity(Intent(this, AudioLiveDvrActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_MIXED -> {
                Log.d(TAG, "Launching Audio Mixed test...")
                startActivity(Intent(this, AudioMixedActivity::class.java))
            }
            TestCase.TestCaseType.AUDIO_MIXED_WITH_SERVICE -> {
                Log.d(TAG, "Launching Audio Mixed with Service test...")
                startActivity(Intent(this, AudioMixedWithServiceActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_VOD_SIMPLE -> {
                Log.d(TAG, "Launching Video VOD Simple test...")
                startActivity(Intent(this, VideoVodSimpleActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_VOD_PIP -> {
                Log.d(TAG, "Launching Video VOD PiP test...")
                startActivity(Intent(this, com.example.sdkqa.video.VideoVodPipActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_LOCAL -> {
                Log.d(TAG, "Launching Video Local test...")
                startActivity(Intent(this, VideoLocalActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_LOCAL_WITH_SERVICE -> {
                Log.d(TAG, "Launching Video Local with Service test...")
                startActivity(Intent(this, VideoLocalWithServiceActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_EPISODE -> {
                Log.d(TAG, "Launching Video Episode test...")
                startActivity(Intent(this, VideoEpisodeActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_LIVE -> {
                Log.d(TAG, "Launching Video Live test...")
                startActivity(Intent(this, VideoLiveActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_LIVE_DVR -> {
                Log.d(TAG, "Launching Video Live DVR test...")
                startActivity(Intent(this, VideoLiveDvrActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_MIXED -> {
                Log.d(TAG, "Launching Video Mixed test...")
                startActivity(Intent(this, VideoMixedActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_MIXED_WITH_SERVICE -> {
                Log.d(TAG, "Launching Video Mixed with Service test...")
                startActivity(Intent(this, VideoMixedWithServiceActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_LIVE_PIP -> {
                Log.d(TAG, "Launching Video Live with PiP test...")
                startActivity(Intent(this, com.example.sdkqa.video.VideoPipActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_REELS -> {
                Log.d(TAG, "Launching Video Reels test...")
                startActivity(Intent(this, com.example.sdkqa.video.VideoReelActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_LIVE_SCROLL -> {
                Log.d(TAG, "Launching Video Live Scroll test...")
                startActivity(Intent(this, com.example.sdkqa.video.VideoLiveWithScrollActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_EPISODE_MANUAL -> {
                Log.d(TAG, "Launching Video Episode Manual test...")
                startActivity(Intent(this, com.example.sdkqa.video.VideoEpisodeManualActivity::class.java))
            }
            TestCase.TestCaseType.VIDEO_EPISODE_AUTO -> {
                Log.d(TAG, "Launching Video Episode Auto test...")
                startActivity(Intent(this, com.example.sdkqa.video.VideoEpisodeAutomaticActivity::class.java))
            }
        }
    }
}
