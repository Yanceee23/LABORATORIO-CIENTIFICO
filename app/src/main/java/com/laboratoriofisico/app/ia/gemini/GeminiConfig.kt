package com.laboratoriofisico.app.ia.gemini

import com.laboratoriofisico.app.BuildConfig

object GeminiConfig {
    const val MODEL_NAME = "gemini-3.6-flash"
    const val MIN_REQUEST_INTERVAL_MS = 1_500L
    const val MAX_OUTPUT_CHARS = 8_000
    const val MAX_CONTEXT_INPUT_CHARS = 18_000

    fun isApiKeyConfigured(): Boolean = BuildConfig.GEMINI_API_KEY.isNotBlank()
}
