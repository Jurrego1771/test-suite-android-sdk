package com.example.sdkqa

import android.Manifest
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.sdkqa.testing.TestEventBus
import com.example.sdkqa.video.VideoReelActivity
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoReelsGestureSmokeTest {

    @get:Rule
    val notificationsRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun reels_swipe_up_three_times_emits_pause_then_play() {
        TestEventBus.clear()

        val scenario = ActivityScenario.launch(VideoReelActivity::class.java)
        try {
            // Startup: en Reels esperamos solo onReady + onPlay (no siempre llega playerViewReady).
            SmokeTestUtils.assertEvents(
                expectedEventNames = listOf(
                    "VideoReel.onReady",
                    "VideoReel.onPlay",
                ),
                timeoutMs = 30_000,
                context = "Startup Reels"
            )

            repeat(3) { idx ->
                TestEventBus.clear()

                // Gesture: swipe up to go to next reel.
                onView(withId(android.R.id.content)).perform(swipeUp())

                // Give UI a moment to settle; events will be awaited below.
                SystemClock.sleep(200)

                val expected = listOf("VideoReel.onPause", "VideoReel.onPlay")
                val events = TestEventBus.awaitAll(expectedNames = expected, timeoutMs = 25_000, pollIntervalMs = 150)
                val names = events.map { it.name }

                val pauseIndex = names.indexOfFirst { it == "VideoReel.onPause" }
                val playIndex = names.indexOfFirst { it == "VideoReel.onPlay" }

                if (pauseIndex == -1 || playIndex == -1) {
                    fail(
                        buildString {
                            appendLine("Swipe ${idx + 1}: faltan eventos esperados.")
                            appendLine("Se esperaba: onPause -> onPlay")
                            appendLine("Eventos capturados (${events.size}):")
                            events.forEach { e ->
                                appendLine("- ${e.timestampMs} ${e.name} ${if (e.data.isEmpty()) "" else e.data}")
                            }
                        }
                    )
                }

                assertTrue(
                    "Swipe ${idx + 1}: se esperaba onPause antes de onPlay, pero el orden fue: pauseIndex=$pauseIndex playIndex=$playIndex",
                    pauseIndex < playIndex
                )
            }
        } finally {
            scenario.close()
        }
    }
}

