package com.example.sdkqa

import android.Manifest
import android.os.SystemClock
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.example.sdkqa.testing.TestEventBus
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AudioLiveSmokeTest {

    @get:Rule
    val notificationsRule: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @Test
    fun audioLive_emits_expected_events() {
        TestEventBus.clear()

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            onView(withId(R.id.recyclerTestCases)).check(matches(isDisplayed()))
            clickOnTestCaseWithScroll(title = "Live Audio")

            val expected = listOf(
                "MainActivity.SelectedTestCase",
                "MainActivity.Launching",
                "AudioLive.onBuffering",
                "AudioLive.onReady",
                "AudioLive.onPlay",
                "AudioLive.playerViewReady",
            )

            val events = TestEventBus.awaitAll(expectedNames = expected, timeoutMs = 25_000, pollIntervalMs = 150)
            val names = events.map { it.name }.toSet()
            val missing = expected.filterNot { it in names }
            if (missing.isNotEmpty()) {
                fail(
                    buildString {
                        appendLine("No se encontraron todos los eventos esperados antes del timeout.")
                        appendLine("Faltantes (${missing.size}):")
                        missing.forEach { appendLine("- $it") }
                        appendLine()
                        appendLine("Eventos capturados (${events.size}):")
                        events.forEach { e ->
                            appendLine("- ${e.timestampMs} ${e.name} ${if (e.data.isEmpty()) "" else e.data}")
                        }
                    }
                )
            }
        } finally {
            scenario.close()
        }
    }

    private fun clickOnTestCaseWithScroll(title: String) {
        onView(withId(R.id.recyclerTestCases)).perform(
            RecyclerViewActions.actionOnItem<RecyclerView.ViewHolder>(
                hasDescendant(withText(title)),
                ViewActions.click()
            )
        )
        SystemClock.sleep(200)
    }
}

