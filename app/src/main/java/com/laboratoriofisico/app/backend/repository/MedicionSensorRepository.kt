package com.laboratoriofisico.app.backend.repository

import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.local.dao.ExperimentoDao
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.core.debug.DebugSessionLog
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.fisica.FisicaEngine
import com.laboratoriofisico.app.fisica.ResultadoMetadata
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoSensorKind
import com.laboratoriofisico.app.frontend.viewmodel.LightHistoryEntry
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorType

data class SensorMedicionSnapshot(
    val valorPrincipal: Double,
    val unidad: String,
    val lectura: SensorReading?,
    val historial: List<Double>,
    val extras: Map<String, String>,
    val penduloLongitudM: Double? = null,
    val luzExperimental: Boolean = false,
    val lightHistoryEntries: List<LightHistoryEntry> = emptyList()
)

sealed class GuardarMedicionResult {
    data class Success(val resultado: Resultado, val experimentoId: Long) : GuardarMedicionResult()
    data class Error(val message: String) : GuardarMedicionResult()
}

/**
 * Persiste mediciones del catálogo de experimentos → [Medicion] → [FisicaEngine] → [Resultado].
 * Mismo patrón que [CapturaMagnetometroRepository].
 */
class MedicionSensorRepository(
    private val sessionRepository: SessionRepository,
    private val experimentoDao: ExperimentoDao,
    private val experimentoRepository: ExperimentoRepository,
    private val medicionRepository: MedicionRepository,
    private val resultadoRepository: ResultadoRepository,
    private val fisicaEngine: FisicaEngine
) {
    suspend fun guardar(
        kind: ExperimentoSensorKind,
        snapshot: SensorMedicionSnapshot
    ): GuardarMedicionResult {
        val usuarioId = sessionRepository.getLoggedInUserId()
            ?: return GuardarMedicionResult.Error(AuthMessages.INVITADO_BLOQUEADO)

        if (snapshot.historial.isEmpty() && (snapshot.valorPrincipal.isNaN() || snapshot.valorPrincipal == 0.0 && snapshot.lectura == null)) {
            return GuardarMedicionResult.Error("No hay lecturas para guardar")
        }

        val valorBase = when (kind) {
            ExperimentoSensorKind.DISTANCIA,
            ExperimentoSensorKind.PENDULO -> snapshot.historial.lastOrNull() ?: snapshot.valorPrincipal
            else -> snapshot.historial.average().takeIf { snapshot.historial.isNotEmpty() }
                ?: snapshot.valorPrincipal
        }
        val timestamp = snapshot.lectura?.timestamp ?: System.currentTimeMillis()

        return try {
            val experimentoId = findOrCreateExperimento(usuarioId, kind)
            val payload = buildPayload(kind, snapshot, valorBase, timestamp, usuarioId)
                ?: return GuardarMedicionResult.Error("No se pudo calcular el resultado")

            val medicionId = medicionRepository.insert(payload.medicion.copy(experimentoId = experimentoId))
            val resultado = payload.resultado.copy(medicionId = medicionId)
            resultadoRepository.insert(resultado)

            // #region agent log
            DebugSessionLog.log(
                hypothesisId = "H-SAVE",
                location = "MedicionSensorRepository.guardar",
                message = "Resultado guardado en Room",
                data = mapOf(
                    "kind" to kind.name,
                    "experimentoId" to experimentoId,
                    "medicionId" to medicionId,
                    "valorCalculado" to resultado.valorCalculado,
                    "unidad" to resultado.unidad,
                    "viaFisicaEngine" to payload.viaFisicaEngine
                ),
                runId = "post-fix"
            )
            LogEngine(kind, payload.viaFisicaEngine, resultado.valorCalculado)
            // #endregion

            GuardarMedicionResult.Success(resultado, experimentoId)
        } catch (e: Exception) {
            GuardarMedicionResult.Error("Error al guardar: ${e.message ?: "desconocido"}")
        }
    }

    private suspend fun findOrCreateExperimento(usuarioId: Long, kind: ExperimentoSensorKind): Long {
        val titulo = kind.catalogTitulo
        experimentoDao.findByUsuarioAndTitulo(usuarioId, titulo)?.let { return it.id }
        return experimentoRepository.insert(
            Experimento(
                titulo = titulo,
                descripcion = "Experimento de catálogo · ${kind.name}",
                usuarioId = usuarioId,
                ubicacionId = null,
                estado = EstadoExperimento.EN_PROGRESO,
                fechaCreacion = System.currentTimeMillis()
            )
        )
    }

    private data class Payload(
        val medicion: Medicion,
        val resultado: Resultado,
        val viaFisicaEngine: Boolean
    )

    private fun buildPayload(
        kind: ExperimentoSensorKind,
        snapshot: SensorMedicionSnapshot,
        valorBase: Double,
        timestamp: Long,
        usuarioId: Long
    ): Payload? = when (kind) {
        ExperimentoSensorKind.LUZ -> directPayload(
            nombre = "Intensidad luminosa",
            valor = valorBase,
            unidad = if (snapshot.luzExperimental) "raw" else "lux",
            sensor = SensorType.LIGHT,
            timestamp = timestamp,
            usuarioId = usuarioId,
            textExtras = lightTextExtras(snapshot)
        )
        ExperimentoSensorKind.REFLEXION -> directPayload(
            nombre = "Luz reflejada",
            valor = valorBase,
            unidad = if (snapshot.luzExperimental) "raw" else "lux",
            sensor = SensorType.LIGHT,
            timestamp = timestamp,
            usuarioId = usuarioId,
            textExtras = lightTextExtras(snapshot, includeAngles = true)
        )
        ExperimentoSensorKind.SONIDO,
        ExperimentoSensorKind.ONDAS -> {
            val amplitude = snapshot.lectura?.valueScalar ?: return null
            Payload(
                medicion = Medicion(
                    experimentoId = 0,
                    usuarioId = usuarioId,
                    nombre = "Amplitud sonora (RMS)",
                    valor = amplitude,
                    unidad = "amplitude",
                    timestamp = timestamp
                ),
                resultado = Resultado(
                    medicionId = 0,
                    usuarioId = usuarioId,
                    valorCalculado = valorBase,
                    unidad = "dB",
                    notas = ResultadoMetadata.buildExtended(
                        sensor = SensorType.MICROPHONE,
                        textExtras = snapshot.extras.filterKeys {
                            it in setOf("frecuencia_hz", "longitud_onda_m", "clasificacion", "amplitud_rms")
                        }
                    ),
                    timestamp = timestamp + 1
                ),
                viaFisicaEngine = false
            )
        }
        ExperimentoSensorKind.FRECUENCIA -> {
            val freq = snapshot.lectura?.valueY?.takeIf { it > 0 }
                ?: snapshot.valorPrincipal.takeIf { it > 0 }
                ?: return null
            val periodo = fisicaEngine.calcular(FormulaId.T_IGUAL_1_SOBRE_F.id, mapOf("f" to freq))
            Payload(
                medicion = Medicion(
                    experimentoId = 0,
                    usuarioId = usuarioId,
                    nombre = "Frecuencia dominante",
                    valor = freq,
                    unidad = "Hz",
                    timestamp = timestamp
                ),
                resultado = Resultado(
                    medicionId = 0,
                    usuarioId = usuarioId,
                    valorCalculado = periodo ?: freq,
                    unidad = if (periodo != null) "s" else "Hz",
                    notas = ResultadoMetadata.buildExtended(
                        formulaId = periodo?.let { FormulaId.T_IGUAL_1_SOBRE_F },
                        sensor = SensorType.MICROPHONE,
                        numericExtras = mapOf("f" to freq),
                        textExtras = snapshot.extras.filterKeys { it == "frecuencia_hz" }
                    ),
                    timestamp = timestamp + 1
                ),
                viaFisicaEngine = periodo != null
            )
        }
        ExperimentoSensorKind.MAGNETISMO -> {
            val reading = snapshot.lectura ?: return null
            val bx = reading.valueX ?: return null
            val by = reading.valueY ?: return null
            val bz = reading.valueZ ?: return null
            val intensidad = fisicaEngine.calcular(
                FormulaId.B_INTENSIDAD.id,
                mapOf("Bx" to bx, "By" to by, "Bz" to bz)
            ) ?: return null
            Payload(
                medicion = Medicion(
                    experimentoId = 0,
                    usuarioId = usuarioId,
                    nombre = "Magnetómetro (Bx=$bx, By=$by, Bz=$bz)",
                    valor = bx,
                    unidad = SensorType.MAGNETOMETER.unit,
                    timestamp = timestamp
                ),
                resultado = Resultado(
                    medicionId = 0,
                    usuarioId = usuarioId,
                    valorCalculado = intensidad,
                    unidad = SensorType.MAGNETOMETER.unit,
                    notas = ResultadoMetadata.buildExtended(
                        formulaId = FormulaId.B_INTENSIDAD,
                        sensor = SensorType.MAGNETOMETER,
                        numericExtras = mapOf("Bx" to bx, "By" to by, "Bz" to bz),
                        textExtras = snapshot.extras.filterKeys {
                            it in setOf("azimuth", "direccion", "bx", "by", "bz")
                        }
                    ),
                    timestamp = timestamp + 1
                ),
                viaFisicaEngine = true
            )
        }
        ExperimentoSensorKind.DISTANCIA -> directPayload(
            nombre = "Distancia recorrida",
            valor = valorBase,
            unidad = "m",
            sensor = SensorType.LOCATION,
            timestamp = timestamp,
            usuarioId = usuarioId,
            textExtras = snapshot.extras.filterKeys {
                it in setOf("velocidad", "precision", "tiempo")
            }
        )
        ExperimentoSensorKind.MOVIMIENTO -> directPayload(
            nombre = "Aceleración dinámica",
            valor = valorBase,
            unidad = "m/s²",
            sensor = SensorType.ACCELEROMETER,
            timestamp = timestamp,
            usuarioId = usuarioId,
            textExtras = snapshot.extras.filterKeys {
                it in setOf("estado_movimiento", "velocidad", "tiempo")
            }
        )
        ExperimentoSensorKind.PENDULO -> {
            val periodo = snapshot.valorPrincipal.takeIf { it > 0 } ?: return null
            val longitud = snapshot.penduloLongitudM
            val gravedad = longitud?.let { l ->
                fisicaEngine.calcular(FormulaId.G_PENDULO.id, mapOf("L" to l, "T" to periodo))
            }
            Payload(
                medicion = Medicion(
                    experimentoId = 0,
                    usuarioId = usuarioId,
                    nombre = "Periodo del péndulo",
                    valor = periodo,
                    unidad = "s",
                    timestamp = timestamp
                ),
                resultado = Resultado(
                    medicionId = 0,
                    usuarioId = usuarioId,
                    valorCalculado = gravedad ?: periodo,
                    unidad = if (gravedad != null) "m/s²" else "s",
                    notas = ResultadoMetadata.buildExtended(
                        formulaId = gravedad?.let { FormulaId.G_PENDULO },
                        sensor = SensorType.ACCELEROMETER,
                        numericExtras = buildMap {
                            put("T", periodo)
                            longitud?.let { put("L", it) }
                            gravedad?.let { put("g", it) }
                        },
                        textExtras = snapshot.extras.filterKeys {
                            it in setOf("periodo_teorico", "gravedad_calculada")
                        }
                    ),
                    timestamp = timestamp + 1
                ),
                viaFisicaEngine = gravedad != null
            )
        }
    }

    private fun lightTextExtras(
        snapshot: SensorMedicionSnapshot,
        includeAngles: Boolean = false
    ): Map<String, String> = buildMap {
        if (snapshot.luzExperimental) {
            put("experimental_als", "true")
        }
        snapshot.extras["nivel_iluminacion"]?.let { put("nivel_iluminacion", it) }
        snapshot.extras["descripcion_iluminacion"]?.let { put("descripcion_iluminacion", it) }
        if (snapshot.lightHistoryEntries.isNotEmpty()) {
            put(
                "historial_iluminacion",
                snapshot.lightHistoryEntries.takeLast(20).joinToString("\n") { it.formatLine() }
            )
        }
        if (includeAngles) {
            snapshot.extras["angulo_inclinacion"]?.let { put("angulo_inclinacion", it) }
            snapshot.extras["angulo_lateral"]?.let { put("angulo_lateral", it) }
        }
    }

    private fun directPayload(
        nombre: String,
        valor: Double,
        unidad: String,
        sensor: SensorType,
        timestamp: Long,
        usuarioId: Long,
        textExtras: Map<String, String> = emptyMap()
    ): Payload = Payload(
        medicion = Medicion(
            experimentoId = 0,
            usuarioId = usuarioId,
            nombre = nombre,
            valor = valor,
            unidad = unidad,
            timestamp = timestamp
        ),
        resultado = Resultado(
            medicionId = 0,
            usuarioId = usuarioId,
            valorCalculado = valor,
            unidad = unidad,
            notas = ResultadoMetadata.buildExtended(sensor = sensor, textExtras = textExtras),
            timestamp = timestamp + 1
        ),
        viaFisicaEngine = false
    )

    private fun LogEngine(kind: ExperimentoSensorKind, viaEngine: Boolean, valor: Double) {
        if (viaEngine) {
            android.util.Log.i("H-ENGINE", "kind=${kind.name} valor=$valor viaFisicaEngine=true")
        }
    }
}
