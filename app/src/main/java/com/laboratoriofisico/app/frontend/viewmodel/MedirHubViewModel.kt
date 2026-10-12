package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.auth.RegisteredUserGuard
import com.laboratoriofisico.app.backend.local.entity.MedicionHistorialRow
import com.laboratoriofisico.app.backend.repository.MedicionRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.sensores.SensorManager
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.util.LiveDisplayFilter
import com.laboratoriofisico.app.sensores.util.SensorValueExtractors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MedicionDatoUi(
    val nombre: String,
    val valor: String,
    val unidad: String
)

data class MedicionHistorialUi(
    val id: Long,
    val tipo: String,
    val nombre: String,
    val fechaMillis: Long,
    val fechaTexto: String,
    val resumen: List<MedicionDatoUi>,
    val datos: List<MedicionDatoUi>,
    val notas: String?
)

data class MedirHubUiState(
    val ubicacionDisponible: Boolean = false,
    val latitud: String = "N/A",
    val longitud: String = "N/A",
    val altitud: String = "N/A",
    val precisionGps: String = "N/A",
    val movimientoDisponible: Boolean = false,
    val velocidad: String = "N/A",
    val distancia: String = "N/A",
    val tiempo: String = "00:00",
    val accelDisponible: Boolean = false,
    val accelX: String = "N/A",
    val accelY: String = "N/A",
    val accelZ: String = "N/A",
    val accelMagnitud: String = "N/A",
    val gyroDisponible: Boolean = false,
    val gyroXRad: String = "N/A",
    val gyroYRad: String = "N/A",
    val gyroZRad: String = "N/A",
    val gyroMagnitudRad: String = "N/A",
    val gyroXDeg: String = "N/A",
    val gyroYDeg: String = "N/A",
    val gyroZDeg: String = "N/A",
    val inclinacionDisponible: Boolean = false,
    val inclinacionPitch: String = "N/A",
    val inclinacionRoll: String = "N/A",
    val inclinacionGrados: String = "N/A",
    val midiendo: Boolean = false,
    val historial: List<MedicionHistorialUi> = emptyList(),
    val filtros: List<String> = emptyList(),
    val filtroSeleccionado: String? = null,
    val medicionSeleccionada: MedicionHistorialUi? = null,
    val medicionPendienteEliminar: MedicionHistorialUi? = null,
    val puedeVerHistorial: Boolean = false,
    val esInvitado: Boolean = false
)

class MedirHubViewModel(
    private val sensorManager: SensorManager,
    private val medicionRepository: MedicionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedirHubUiState())
    val uiState: StateFlow<MedirHubUiState> = _uiState.asStateFlow()

    private val liveFilter = LiveDisplayFilter()
    private var startMs: Long? = null
    private var lastLocation: Pair<Double, Double>? = null
    private var accumulatedDistance = 0.0

    init {
        viewModelScope.launch {
            sessionRepository.session.collect { session ->
                val puedeHistorial = RegisteredUserGuard.canSaveOrViewHistory(session)
                _uiState.update {
                    it.copy(
                        puedeVerHistorial = puedeHistorial,
                        esInvitado = session is com.laboratoriofisico.app.backend.session.AppSession.Guest,
                        historial = if (puedeHistorial) it.historial else emptyList()
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                sensorManager.observeLatestReading(SensorType.LOCATION),
                sensorManager.observeState(SensorType.LOCATION)
            ) { reading, state -> reading to state }
                .collect { (reading, state) -> updateLocation(reading, state) }
        }
        viewModelScope.launch {
            combine(
                sensorManager.observeLatestReading(SensorType.ACCELEROMETER),
                sensorManager.observeState(SensorType.ACCELEROMETER)
            ) { reading, state -> reading to state }
                .collect { (reading, state) -> updateAccelerometer(reading, state) }
        }
        viewModelScope.launch {
            combine(
                sensorManager.observeLatestReading(SensorType.GYROSCOPE),
                sensorManager.observeState(SensorType.GYROSCOPE)
            ) { reading, state -> reading to state }
                .collect { (reading, state) -> updateGyroscope(reading, state) }
        }
        viewModelScope.launch {
            medicionRepository.observeHistorial().collect { rows ->
                if (!_uiState.value.puedeVerHistorial) {
                    _uiState.update { it.copy(historial = emptyList(), filtros = emptyList()) }
                    return@collect
                }
                val historial = rows.toHistorialUi()
                _uiState.update { state ->
                    val filtros = historial.map { it.tipo }.distinct()
                    val filtro = state.filtroSeleccionado?.takeIf { it in filtros }
                    state.copy(
                        historial = historial,
                        filtros = filtros,
                        filtroSeleccionado = filtro,
                        medicionSeleccionada = state.medicionSeleccionada?.let { selected ->
                            historial.firstOrNull { it.id == selected.id }
                        },
                        medicionPendienteEliminar = state.medicionPendienteEliminar?.let { pending ->
                            historial.firstOrNull { it.id == pending.id }
                        }
                    )
                }
            }
        }
    }

    fun actualizar() {
        startMs = System.currentTimeMillis()
        lastLocation = null
        accumulatedDistance = 0.0
        _uiState.update { it.copy(midiendo = true) }
        sensorManager.startReading(SensorType.LOCATION)
        sensorManager.startReading(SensorType.ACCELEROMETER)
        sensorManager.startReading(SensorType.GYROSCOPE)
    }

    fun detener() {
        sensorManager.stopReading(SensorType.LOCATION)
        sensorManager.stopReading(SensorType.ACCELEROMETER)
        sensorManager.stopReading(SensorType.GYROSCOPE)
        _uiState.update { it.copy(midiendo = false) }
    }

    fun onPermissionResult(type: SensorType, granted: Boolean) {
        sensorManager.setPermissionGranted(type, granted)
    }

    fun onResume() {
        sensorManager.onResume()
        if (sensorManager.isSensorAvailable(SensorType.ACCELEROMETER)) {
            sensorManager.startReading(SensorType.ACCELEROMETER)
        }
        if (sensorManager.isSensorAvailable(SensorType.GYROSCOPE)) {
            sensorManager.startReading(SensorType.GYROSCOPE)
        }
    }

    fun onPause() {
        detener()
        sensorManager.onPause()
    }

    fun seleccionarFiltro(tipo: String?) {
        _uiState.update { it.copy(filtroSeleccionado = tipo) }
    }

    fun seleccionarMedicion(medicion: MedicionHistorialUi?) {
        _uiState.update { it.copy(medicionSeleccionada = medicion) }
    }

    fun pedirEliminar(medicion: MedicionHistorialUi) {
        _uiState.update { it.copy(medicionPendienteEliminar = medicion) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(medicionPendienteEliminar = null) }
    }

    fun eliminarPendiente() {
        val id = _uiState.value.medicionPendienteEliminar?.id ?: return
        viewModelScope.launch {
            medicionRepository.deleteById(id)
            _uiState.update {
                it.copy(
                    medicionPendienteEliminar = null,
                    medicionSeleccionada = it.medicionSeleccionada?.takeIf { selected -> selected.id != id }
                )
            }
        }
    }

    override fun onCleared() {
        detener()
        super.onCleared()
    }

    private fun updateLocation(reading: SensorReading?, state: SensorState) {
        val disponible = sensorManager.isSensorAvailable(SensorType.LOCATION) &&
            state != SensorState.PERMISSION_DENIED &&
            state != SensorState.UNAVAILABLE &&
            state != SensorState.DISABLED
        if (reading != null && _uiState.value.midiendo) {
            val lat = reading.valueX
            val lon = reading.valueY
            if (lat != null && lon != null) {
                val prev = lastLocation
                if (prev != null) {
                    accumulatedDistance += SensorValueExtractors.haversineMeters(
                        prev.first, prev.second, lat, lon
                    )
                }
                lastLocation = lat to lon
            }
        }
        val elapsed = startMs?.let {
            ((System.currentTimeMillis() - it) / 1000).coerceAtLeast(0)
        } ?: 0L
        _uiState.update {
            it.copy(
                ubicacionDisponible = disponible,
                latitud = reading?.valueX?.let { v -> "%.6f°".format(v) } ?: "N/A",
                longitud = reading?.valueY?.let { v -> "%.6f°".format(v) } ?: "N/A",
                altitud = reading?.valueZ?.let { v -> "%.1f m".format(v) } ?: "N/A",
                precisionGps = reading?.accuracy?.let { v -> "±%.0f m".format(v) } ?: "N/A",
                distancia = if (disponible) "%.1f m".format(accumulatedDistance) else "N/A",
                tiempo = formatDuration(elapsed),
                velocidad = reading?.speed?.let { v -> "%.2f m/s".format(v) }
                    ?: if (disponible) it.velocidad else "N/A",
                movimientoDisponible = disponible
            )
        }
    }

    private fun updateAccelerometer(reading: SensorReading?, state: SensorState) {
        val disponible = sensorManager.isSensorAvailable(SensorType.ACCELEROMETER) &&
            state != SensorState.UNAVAILABLE
        val magnitud = reading?.let { SensorValueExtractors.accelMagnitude(it) }
        val tilt = reading?.let { SensorValueExtractors.tiltDegrees(it) }
        val accelX = reading?.valueX?.let { liveFilter.stable("ax", it, 0.1) }
        val accelY = reading?.valueY?.let { liveFilter.stable("ay", it, 0.1) }
        val accelZ = reading?.valueZ?.let { liveFilter.stable("az", it, 0.1) }
        val accelMag = magnitud?.let { liveFilter.stable("amag", it, 0.1) }
        val pitch = tilt?.first?.let { liveFilter.stable("pitch", it, 1.0) }
        val roll = tilt?.second?.let { liveFilter.stable("roll", it, 1.0) }
        _uiState.update {
            it.copy(
                accelDisponible = disponible,
                accelX = SensorValueExtractors.formatAxis(accelX, "m/s²"),
                accelY = SensorValueExtractors.formatAxis(accelY, "m/s²"),
                accelZ = SensorValueExtractors.formatAxis(accelZ, "m/s²"),
                accelMagnitud = accelMag?.let { m -> "%.1f m/s²".format(m) } ?: "N/A",
                inclinacionDisponible = disponible && tilt != null,
                inclinacionPitch = pitch?.let { p -> "%.0f°".format(p) } ?: "N/A",
                inclinacionRoll = roll?.let { r -> "%.0f°".format(r) } ?: "N/A",
                inclinacionGrados = pitch?.let { p -> "%.0f".format(p) } ?: "N/A"
            )
        }
    }

    private fun updateGyroscope(reading: SensorReading?, state: SensorState) {
        val disponible = sensorManager.isSensorAvailable(SensorType.GYROSCOPE) &&
            state != SensorState.UNAVAILABLE
        val x = reading?.valueX?.let { liveFilter.stable("gx", quietGyro(it), 0.05) }
        val y = reading?.valueY?.let { liveFilter.stable("gy", quietGyro(it), 0.05) }
        val z = reading?.valueZ?.let { liveFilter.stable("gz", quietGyro(it), 0.05) }
        val magnitud = if (x != null && y != null && z != null) {
            kotlin.math.sqrt(x * x + y * y + z * z)
        } else {
            null
        }
        _uiState.update {
            it.copy(
                gyroDisponible = disponible,
                gyroXRad = SensorValueExtractors.formatAxis(x, "rad/s"),
                gyroYRad = SensorValueExtractors.formatAxis(y, "rad/s"),
                gyroZRad = SensorValueExtractors.formatAxis(z, "rad/s"),
                gyroMagnitudRad = magnitud?.let { m -> "%.3f rad/s".format(m) } ?: "N/A",
                gyroXDeg = x?.let { v ->
                    "%.1f °/s".format(SensorValueExtractors.radiansToDegreesPerSecond(v))
                } ?: "N/A",
                gyroYDeg = y?.let { v ->
                    "%.1f °/s".format(SensorValueExtractors.radiansToDegreesPerSecond(v))
                } ?: "N/A",
                gyroZDeg = z?.let { v ->
                    "%.1f °/s".format(SensorValueExtractors.radiansToDegreesPerSecond(v))
                } ?: "N/A"
            )
        }
    }

    private fun quietGyro(radiansPerSecond: Double): Double =
        if (kotlin.math.abs(radiansPerSecond) < 0.06) 0.0 else radiansPerSecond

    private fun formatDuration(totalSeconds: Long): String {
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    private fun List<MedicionHistorialRow>.toHistorialUi(): List<MedicionHistorialUi> =
        groupBy { it.medicionId }.values.mapNotNull { group ->
            val first = group.firstOrNull() ?: return@mapNotNull null
            val resultRows = group.filter { it.resultadoId != null }
            val primary = MedicionDatoUi(
                nombre = first.medicionNombre,
                valor = "%.2f".format(first.medicionValor),
                unidad = first.medicionUnidad
            )
            val resultData = resultRows.flatMap { row ->
                buildList {
                    val value = row.resultadoValor
                    val unit = row.resultadoUnidad
                    if (value != null && unit != null) {
                        add(MedicionDatoUi("Resultado calculado", "%.2f".format(value), unit))
                    }
                    addAll(parseNotas(row.resultadoNotas))
                }
            }.distinctBy { "${it.nombre}|${it.valor}|${it.unidad}" }
            val allData = (listOf(primary) + resultData).distinctBy { "${it.nombre}|${it.valor}|${it.unidad}" }
            MedicionHistorialUi(
                id = first.medicionId,
                tipo = first.experimentoTitulo,
                nombre = first.medicionNombre,
                fechaMillis = first.medicionTimestamp,
                fechaTexto = dateFormatter.format(Date(first.medicionTimestamp)),
                resumen = allData.take(3),
                datos = allData,
                notas = resultRows.mapNotNull { it.resultadoNotas }.firstOrNull()
            )
        }.sortedByDescending { it.fechaMillis }

    private fun parseNotas(notas: String?): List<MedicionDatoUi> {
        if (notas.isNullOrBlank()) return emptyList()
        return notas.split(';')
            .mapNotNull { part ->
                val key = part.substringBefore('=', missingDelimiterValue = "").trim()
                val value = part.substringAfter('=', missingDelimiterValue = "").trim()
                if (key.isBlank() || value.isBlank()) return@mapNotNull null
                if (key in hiddenMetadataKeys) return@mapNotNull null
                val unit = inferUnit(key, value)
                MedicionDatoUi(
                    nombre = labelForKey(key),
                    valor = value,
                    unidad = unit
                )
            }
    }

    private fun labelForKey(key: String): String = when (key) {
        "duration_s", "duracion_s" -> "Duración"
        "muestras" -> "Muestras"
        "frecuencia_hz", "f" -> "Frecuencia"
        "periodo_s", "T" -> "Período"
        "oscilaciones" -> "Oscilaciones"
        "amplitud_ms2" -> "Amplitud"
        "longitud_m", "L" -> "Longitud"
        "gravedad_calculada_ms2", "g" -> "Gravedad calculada"
        "aceleracion_dinamica_max_ms2" -> "Aceleración máxima"
        "aceleracion_dinamica_promedio_ms2" -> "Aceleración promedio"
        "aceleracion_total_max_ms2" -> "Aceleración total máxima"
        "aceleracion_min_ms2" -> "Aceleración mínima"
        "impacto_max_ms2" -> "Impacto máximo"
        "duracion_caida_s" -> "Duración de caída"
        "caida_detectada" -> "Caída detectada"
        "tiempo" -> "Tiempo"
        "velocidad" -> "Velocidad"
        "precision" -> "Precisión"
        "longitud_onda_m" -> "Longitud de onda"
        "amplitud_rms" -> "Amplitud RMS"
        else -> key.replace('_', ' ').replaceFirstChar { it.titlecase(Locale.getDefault()) }
    }

    private fun inferUnit(key: String, value: String): String {
        if (value.any { it.isLetter() } || value.contains("°") || value.contains("µ")) return ""
        return when {
            key.endsWith("_s") || key == "T" -> "s"
            key.endsWith("_hz") || key == "f" -> "Hz"
            key.endsWith("_ms2") || key == "g" -> "m/s²"
            key.endsWith("_m") || key == "L" -> "m"
            key.endsWith("_cm") -> "cm"
            key.endsWith("_px") -> "px"
            key.startsWith("B") -> "µT"
            else -> ""
        }
    }

    private companion object {
        val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val hiddenMetadataKeys = setOf(
            "formula",
            "sensor",
            "experimento_sensor",
            "fuente_datos",
            "lectura_usuario"
        )
    }
}
