package com.example.sdkqa

import android.Manifest
import android.os.SystemClock
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.swipeUp
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.example.sdkqa.testing.TestEventBus
import com.example.sdkqa.video.VideoReelActivity
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
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

            val reelsVisibilityButtonId =
                resolveFirstId(names = listOf("reels_visibility_button", "reels_visbility_button"))
            assertTrue(
                "No se encontró el id de reels visibility button. Probé: reels_visibility_button, reels_visbility_button",
                reelsVisibilityButtonId != 0
            )
            // Reels suele tener múltiples items pre-cargados con el mismo id. Tomamos el primer match visible.
            onView(withIndex(withId(reelsVisibilityButtonId), 0)).check(matches(isDisplayed()))

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

    private fun resolveFirstId(names: List<String>): Int {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return names.firstNotNullOfOrNull { name ->
            context.resources.getIdentifier(name, "id", context.packageName).takeIf { it != 0 }
        } ?: 0
    }

    private fun withIndex(matcher: Matcher<View>, index: Int): Matcher<View> {
        return object : TypeSafeMatcher<View>() {
            var currentIndex = 0

            override fun describeTo(description: org.hamcrest.Description) {
                description.appendText("withIndex($index): ")
                matcher.describeTo(description)
            }

            override fun matchesSafely(view: View): Boolean {
                return matcher.matches(view) && currentIndex++ == index
            }
        }
    }
}

