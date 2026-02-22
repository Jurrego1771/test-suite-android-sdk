package com.example.sdkqa.testing

import android.os.SystemClock
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Simple in-memory event recorder for instrumentation smoke tests.
 *
 * - Intended for observing SDK callbacks without relying on logcat.
 */
object TestEventBus {

    data class Event(
        val name: String,
        val data: Map<String, String> = emptyMap(),
        val timestampMs: Long = SystemClock.elapsedRealtime(),
    )

    private val events = ConcurrentLinkedQueue<Event>()

    fun clear() {
        events.clear()
    }

    fun record(name: String, data: Map<String, String> = emptyMap()) {
        events.add(Event(name = name, data = data))
    }

    fun snapshot(): List<Event> {
        return events.toList()
    }

    fun awaitAll(
        expectedNames: List<String>,
        timeoutMs: Long,
        pollIntervalMs: Long = 100,
    ): List<Event> {
        val deadline = SystemClock.uptimeMillis() + timeoutMs
        while (SystemClock.uptimeMillis() < deadline) {
            val snap = snapshot()
            val names = snap.map { it.name }.toSet()
            if (expectedNames.all { it in names }) return snap
            SystemClock.sleep(pollIntervalMs)
        }
        return snapshot()
    }
}

