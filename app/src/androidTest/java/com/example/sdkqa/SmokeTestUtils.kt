package com.example.sdkqa

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import com.example.sdkqa.testing.TestEventBus
import org.junit.Assert.fail

object SmokeTestUtils {

    fun <T : Activity> launchAndAssertBasicEvents(
        activityClass: Class<T>,
        prefix: String,
        timeoutMs: Long = 30_000,
    ) {
        TestEventBus.clear()
        val scenario = ActivityScenario.launch(activityClass)
        try {
            assertBasicEvents(prefix = prefix, timeoutMs = timeoutMs)
        } finally {
            scenario.close()
        }
    }

    fun <T : Activity> launchAndAssertEvents(
        activityClass: Class<T>,
        expectedEventNames: List<String>,
        timeoutMs: Long = 30_000,
    ) {
        TestEventBus.clear()
        val scenario = ActivityScenario.launch(activityClass)
        try {
            assertEvents(expectedEventNames = expectedEventNames, timeoutMs = timeoutMs)
        } finally {
            scenario.close()
        }
    }

    fun assertBasicEvents(prefix: String, timeoutMs: Long = 30_000) {
        val expected = listOf(
            "$prefix.onBuffering",
            "$prefix.onReady",
            "$prefix.onPlay",
            "$prefix.playerViewReady",
        )

        assertEvents(expectedEventNames = expected, timeoutMs = timeoutMs, context = "Prefix: $prefix")
    }

    fun assertEvents(
        expectedEventNames: List<String>,
        timeoutMs: Long = 30_000,
        context: String? = null,
    ) {
        val events = TestEventBus.awaitAll(expectedNames = expectedEventNames, timeoutMs = timeoutMs, pollIntervalMs = 150)
        val names = events.map { it.name }.toSet()
        val missing = expectedEventNames.filterNot { it in names }

        if (missing.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("No se encontraron todos los eventos esperados antes del timeout.")
                    if (!context.isNullOrBlank()) appendLine(context)
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
    }
}

