package com.laboratoriofisico.app.ia.gemini

import android.graphics.BitmapFactory
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.laboratoriofisico.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class GeminiClient(
    private val systemInstruction: String = GeminiPrompts.TUTOR_SYSTEM
) {
    private val rateLimiter = GeminiRateLimiter()
    private val requestMutex = Mutex()
    private val imageCacheMutex = Mutex()
    private val imageResponseCache = mutableMapOf<String, String>()
    private var activeRequestLabel: String? = null
    private val model: GenerativeModel? by lazy {
        if (!GeminiConfig.isApiKeyConfigured()) return@lazy null
        GenerativeModel(
            modelName = GeminiConfig.MODEL_NAME,
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(systemInstruction) },
            safetySettings = GeminiErrorMapper.safetySettings
        )
    }

    suspend fun generar(prompt: String): String = withContext(Dispatchers.IO) {
        runSingleGeminiCall(label = "text:${prompt.fingerprint()}") {
            generarTextoSinGuard(prompt)
        }
    }

    suspend fun generarConImagen(prompt: String, imagenPath: String): String = withContext(Dispatchers.IO) {
        val cacheKey = imageCacheKey(prompt, imagenPath)
        imageCacheMutex.withLock {
            imageResponseCache[cacheKey]?.let { cached ->
                Log.i(TAG, "Gemini image cache hit key=${cacheKey.fingerprint()} path=${File(imagenPath).name}")
                return@withContext cached
            }
        }
        runSingleGeminiCall(label = "image:${cacheKey.fingerprint()}") {
            val response = generarImagenSinGuard(prompt, imagenPath)
            if (!response.isGeminiErrorMessage()) {
                imageCacheMutex.withLock {
                    imageResponseCache[cacheKey] = response
                    if (imageResponseCache.size > MAX_IMAGE_CACHE_ENTRIES) {
                        imageResponseCache.keys.firstOrNull()?.let(imageResponseCache::remove)
                    }
                }
            }
            response
        }
    }

    private suspend fun generarTextoSinGuard(prompt: String): String =
        runCatching {
            val generativeModel = model ?: throw IaApiKeyMissingException()
            rateLimiter.awaitTurn()
            val response = generativeModel.generateContent(prompt)
            val texto = response.text?.trim().orEmpty()
            if (texto.isBlank()) {
                "La IA no devolvió texto. Reformulá la pregunta e intentá de nuevo."
            } else {
                texto.take(GeminiConfig.MAX_OUTPUT_CHARS)
            }
        }.getOrElse { error ->
            GeminiErrorMapper.mensajeParaUsuario(error)
        }

    private suspend fun generarImagenSinGuard(prompt: String, imagenPath: String): String =
        runCatching {
            val generativeModel = model ?: throw IaApiKeyMissingException()
            val bitmap = BitmapFactory.decodeFile(imagenPath)
                ?: throw IllegalArgumentException("No se pudo leer la imagen capturada.")
            rateLimiter.awaitTurn()
            val response = generativeModel.generateContent(
                content {
                    text(prompt)
                    image(bitmap)
                }
            )
            val texto = response.text?.trim().orEmpty()
            if (texto.isBlank()) {
                "La IA no devolvió texto. Captura una imagen más clara e inténtalo de nuevo."
            } else {
                texto.take(GeminiConfig.MAX_OUTPUT_CHARS)
            }
        }.getOrElse { error ->
            GeminiErrorMapper.mensajeParaUsuario(error)
        }

    private suspend fun runSingleGeminiCall(label: String, block: suspend () -> String): String {
        val active = requestMutex.withLock {
            val current = activeRequestLabel
            if (current == null) {
                activeRequestLabel = label
                null
            } else {
                current
            }
        }
        if (active != null) {
            Log.w(TAG, "Gemini call ignored because another call is active. active=$active ignored=$label")
            return BUSY_MESSAGE
        }

        Log.i(TAG, "Gemini call started label=$label")
        return try {
            block()
        } finally {
            requestMutex.withLock {
                if (activeRequestLabel == label) activeRequestLabel = null
            }
            Log.i(TAG, "Gemini call finished label=$label")
        }
    }

    private fun imageCacheKey(prompt: String, imagenPath: String): String {
        val file = File(imagenPath)
        return listOf(
            file.absolutePath,
            file.lastModified().toString(),
            file.length().toString(),
            prompt.fingerprint()
        ).joinToString("|")
    }

    private fun String.fingerprint(): String = hashCode().toUInt().toString(16)

    private fun String.isGeminiErrorMessage(): Boolean {
        val lower = lowercase()
        return lower.startsWith("sin conexión") ||
            lower.startsWith("falta configurar") ||
            lower.startsWith("esperá un momento") ||
            lower.startsWith("no se pudo") ||
            lower.startsWith("se alcanzó") ||
            lower.contains("api key")
    }

    private companion object {
        const val TAG = "GeminiClient"
        const val MAX_IMAGE_CACHE_ENTRIES = 12
        const val BUSY_MESSAGE = "Esperá un momento: ya hay una llamada a Gemini en curso."
    }
}
