package com.laboratoriofisico.app.ia.gemini

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GeminiRateLimiter(
    private val minIntervalMs: Long = GeminiConfig.MIN_REQUEST_INTERVAL_MS
) {
    private val mutex = Mutex()
    private var lastRequestAt = 0L

    suspend fun awaitTurn() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val waitMs = minIntervalMs - (now - lastRequestAt)
            if (waitMs > 0) delay(waitMs)
            lastRequestAt = System.currentTimeMillis()
        }
    }
}
