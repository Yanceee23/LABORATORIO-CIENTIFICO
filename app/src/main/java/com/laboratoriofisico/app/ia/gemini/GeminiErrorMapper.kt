package com.laboratoriofisico.app.ia.gemini

import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object GeminiErrorMapper {
    fun mensajeParaUsuario(error: Throwable): String = when (error) {
        is IaRateLimitException -> "Esperá un momento antes de enviar otra pregunta."
        is IaApiKeyMissingException -> "PON TU CLAVE DE GEMINI AQUI en local.properties."
        is UnknownHostException, is IOException ->
            "Sin conexión a internet. Verificá tu red e intentá de nuevo."
        else -> mensajeDesdeTexto(extraerMensajeUtil(error))
    }

    private fun mensajeDesdeTexto(msg: String): String {
        val lower = msg.lowercase()
        return when {
            lower.contains("429") ||
                lower.contains("quota") ||
                lower.contains("rate limit") ->
                "Se alcanzó el límite de uso de la API. Intentá más tarde."
            lower.contains("403") ||
                lower.contains("api key") ||
                lower.contains("permission") ->
                "PON TU CLAVE DE GEMINI AQUI en local.properties."
            lower.contains("404") ||
                lower.contains("not_found") ||
                lower.contains("no longer available") ||
                lower.contains("is not found") ->
                "El modelo de IA ya no está disponible. Actualizá la app e intentá de nuevo."
            lower.contains("503") || lower.contains("500") ->
                "El servicio de IA no está disponible ahora. Intentá en unos minutos."
            lower.contains("timeout") ->
                "La solicitud tardó demasiado. Intentá de nuevo."
            msg.isNotBlank() -> msg.take(280)
            else -> "No se pudo obtener respuesta de la IA. Intentá de nuevo."
        }
    }

    private fun extraerMensajeUtil(error: Throwable): String {
        val raw = buildString {
            var current: Throwable? = error
            while (current != null) {
                current.message?.let { append(it).append(' ') }
                current = current.cause
            }
        }.trim()

        extractJsonMessage(raw)?.let { return it }
        return raw
    }

    private fun extractJsonMessage(text: String): String? {
        val match = Regex(""""message"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(text)
            ?: return null
        return match.groupValues[1]
            .replace("\\\"", "\"")
            .replace("\\n", "\n")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    val safetySettings: List<SafetySetting> = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.ONLY_HIGH),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.ONLY_HIGH)
    )
}

class IaApiKeyMissingException : Exception("PON TU CLAVE DE GEMINI AQUI")
class IaRateLimitException : Exception("Rate limit local")
