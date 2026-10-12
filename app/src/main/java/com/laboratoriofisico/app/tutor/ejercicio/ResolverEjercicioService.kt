package com.laboratoriofisico.app.tutor.ejercicio

import android.util.Log
import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.backend.repository.EjercicioResueltoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.ia.gemini.GeminiClient
import com.laboratoriofisico.app.ia.gemini.GeminiPrompts

sealed class ResolverEjercicioOutcome {
    data class Success(val ejercicio: EjercicioResuelto, val fromCacheOrNetwork: Boolean = true) :
        ResolverEjercicioOutcome()

    data class Error(val message: String, val canRetry: Boolean = true) : ResolverEjercicioOutcome()
    data object Busy : ResolverEjercicioOutcome()
}

/**
 * Orquesta UNA llamada Gemini (foto o texto), parseo, verificación local y guardado.
 * No se invoca desde LaunchedEffect ni con reintentos automáticos.
 */
class ResolverEjercicioService(
    private val geminiClient: GeminiClient,
    private val repository: EjercicioResueltoRepository,
    private val sessionRepository: SessionRepository,
    private val verifier: EjercicioLocalVerifier
) {
    /**
     * Contador de llamadas reales a Gemini (para tests).
     * No incluye lecturas desde Room.
     */
    @Volatile
    var geminiCallCount: Int = 0
        private set

    fun resetGeminiCallCountForTests() {
        geminiCallCount = 0
    }

    suspend fun resolverDesdeFoto(fotoPath: String): ResolverEjercicioOutcome {
        if (sessionRepository.getLoggedInUserId() == null) {
            return ResolverEjercicioOutcome.Error(AuthMessages.INVITADO_BLOQUEADO, canRetry = false)
        }
        Log.i(TAG, "resolverDesdeFoto start")
        geminiCallCount++
        val raw = geminiClient.generarConImagen(GeminiPrompts.resolverEjercicioDesdeFoto(), fotoPath)
        return procesarRespuestaGemini(raw = raw, fotoPath = fotoPath, enunciadoOverride = null)
    }

    suspend fun resolverDesdeTexto(enunciado: String, fotoPath: String): ResolverEjercicioOutcome {
        if (sessionRepository.getLoggedInUserId() == null) {
            return ResolverEjercicioOutcome.Error(AuthMessages.INVITADO_BLOQUEADO, canRetry = false)
        }
        val texto = enunciado.trim()
        if (texto.isBlank()) {
            return ResolverEjercicioOutcome.Error("Escribe el enunciado antes de resolver.", canRetry = true)
        }
        Log.i(TAG, "resolverDesdeTexto start")
        geminiCallCount++
        val raw = geminiClient.generar(GeminiPrompts.resolverEjercicioDesdeTexto(texto))
        return procesarRespuestaGemini(raw = raw, fotoPath = fotoPath, enunciadoOverride = texto)
    }

    /** Solo Room: abrir ejercicio guardado nunca llama a Gemini. */
    suspend fun cargarGuardado(id: Long): ResolverEjercicioOutcome {
        Log.i(TAG, "cargarGuardado id=$id (sin Gemini)")
        val ejercicio = repository.getByIdForCurrentUser(id)
            ?: return ResolverEjercicioOutcome.Error("No se encontró el ejercicio.", canRetry = false)
        return ResolverEjercicioOutcome.Success(ejercicio, fromCacheOrNetwork = false)
    }

    suspend fun guardarSiNuevo(ejercicio: EjercicioResuelto): EjercicioResuelto {
        if (ejercicio.id > 0L) return ejercicio
        val id = repository.insert(ejercicio)
        return ejercicio.copy(id = id)
    }

    private suspend fun procesarRespuestaGemini(
        raw: String,
        fotoPath: String,
        enunciadoOverride: String?
    ): ResolverEjercicioOutcome {
        if (raw.trim().startsWith("Esperá un momento") || raw.contains("ya hay una llamada")) {
            Log.i(TAG, "resolver ignored busy")
            return ResolverEjercicioOutcome.Busy
        }
        if (esErrorUsuario(raw)) {
            return ResolverEjercicioOutcome.Error(mapearError(raw), canRetry = true)
        }
        val parsed = EjercicioResueltoJsonParser.parse(raw).getOrElse { error ->
            Log.w(TAG, "parse failed: ${error.message}")
            return ResolverEjercicioOutcome.Error(
                "No se pudo leer la respuesta. Revisa la foto o inténtalo de nuevo.",
                canRetry = true
            )
        }
        if (parsed.enunciado.contains("borrosa", ignoreCase = true) ||
            parsed.enunciado.contains("ilegible", ignoreCase = true)
        ) {
            return ResolverEjercicioOutcome.Error(
                "La foto está borrosa o ilegible. Toma otra más clara.",
                canRetry = true
            )
        }
        val enunciado = enunciadoOverride?.takeIf { it.isNotBlank() } ?: parsed.enunciado
        val verificacion = verifier.verificar(
            materia = parsed.materia,
            formula = parsed.formula,
            datos = parsed.datos,
            resultadoGemini = parsed.resultado
        )
        val userId = sessionRepository.requireRegisteredUserId()
        val ejercicio = EjercicioResuelto(
            id = 0,
            usuarioId = userId,
            fotoPath = fotoPath,
            materia = parsed.materia,
            tema = parsed.tema,
            enunciado = enunciado,
            datos = parsed.datos,
            incognita = parsed.incognita,
            formula = parsed.formula,
            formulaLegible = parsed.formulaLegible,
            variables = parsed.variables,
            explicacionFormula = parsed.explicacionFormula,
            pasos = parsed.pasos,
            resultado = parsed.resultado,
            verificacion = verificacion,
            confianza = parsed.confianza,
            fechaCreacion = System.currentTimeMillis()
        ).paraMostrar()
        val saved = guardarSiNuevo(ejercicio)
        Log.i(TAG, "resolver finished savedId=${saved.id} verificacion=${verificacion.key}")
        return ResolverEjercicioOutcome.Success(saved)
    }

    private fun esErrorUsuario(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.startsWith("sin conexión") ||
            lower.startsWith("falta configurar") ||
            lower.startsWith("no se pudo") ||
            lower.startsWith("se alcanzó") ||
            lower.contains("api key") ||
            lower.startsWith("el servicio") ||
            lower.startsWith("la solicitud tardó") ||
            lower.startsWith("el modelo")
    }

    private fun mapearError(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.startsWith("sin conexión") -> "Sin internet. Revisa tu conexión e inténtalo de nuevo."
            lower.contains("api key") || lower.startsWith("falta configurar") ->
                "Error de la API. Revisa la configuración de Gemini."
            else -> raw.take(280)
        }
    }

    companion object {
        private const val TAG = "ResolverEjercicio"
    }
}
