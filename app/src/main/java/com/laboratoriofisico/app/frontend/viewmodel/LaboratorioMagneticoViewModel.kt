package com.laboratoriofisico.app.frontend.viewmodel



import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.laboratoriofisico.app.fisica.FormulaId

import com.laboratoriofisico.app.fisica.FisicaEngine

import com.laboratoriofisico.app.sensores.SensorManager

import com.laboratoriofisico.app.sensores.model.SamplingRate

import com.laboratoriofisico.app.sensores.model.SensorReading

import com.laboratoriofisico.app.sensores.model.SensorState

import com.laboratoriofisico.app.sensores.model.SensorType

import com.laboratoriofisico.app.sensores.util.CompassCalculator

import com.laboratoriofisico.app.sensores.util.MagneticAnomalyDetector

import com.laboratoriofisico.app.sensores.util.MagneticAnomalyPhase

import com.laboratoriofisico.app.sensores.util.MagneticAnomalyResult

import com.laboratoriofisico.app.sensores.util.MagneticCardinals
import com.laboratoriofisico.app.sensores.util.LiveDisplayFilter
import com.laboratoriofisico.app.sensores.util.NearbyMagnetism

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class LaboratorioMagneticoUiState(

    val disponible: Boolean = false,

    val sensorState: SensorState = SensorState.IDLE,

    val permisoRequerido: Boolean = false,

    val intensidadUt: Double? = null,

    val azimuth: Double? = null,

    val direccion: String = "N/A",

    val bx: String = "N/A",

    val by: String = "N/A",

    val bz: String = "N/A",

    val campoBase: String = "N/A",

    val campoActual: String = "N/A",

    val deltaCampo: String = "N/A",

    val anomaliaEstado: String = "N/A",

    val imanHint: String = "N/A",

    val imanDireccion: String = "N/A",

    val brujulaValida: Boolean = true,

    val historial: List<Double> = emptyList(),

    val midiendo: Boolean = false

)



class LaboratorioMagneticoViewModel(

    private val sensorManager: SensorManager,

    private val fisicaEngine: FisicaEngine

) : ViewModel() {



    private val _uiState = MutableStateFlow(LaboratorioMagneticoUiState())

    val uiState: StateFlow<LaboratorioMagneticoUiState> = _uiState.asStateFlow()

    private val compassCalculator = CompassCalculator()

    private val magneticAnomalyDetector = MagneticAnomalyDetector()
    private val liveFilter = LiveDisplayFilter(alpha = 0.2)

    private var lastMagnetTimestamp: Long? = null

    private var lastAnomaly: MagneticAnomalyResult? = null

    private var magnetDiagUiCount = 0



    init {

        viewModelScope.launch {

            combine(

                sensorManager.observeState(SensorType.MAGNETOMETER),

                sensorManager.observeLatestReading(SensorType.MAGNETOMETER),

                sensorManager.observeLatestReading(SensorType.ACCELEROMETER)

            ) { state, magnetReading, accelReading ->

                Triple(state, magnetReading, accelReading)

            }.collect { (state, magnetReading, accelReading) ->

                updateFromReading(magnetReading, accelReading, state)

            }

        }

    }



    fun iniciar() {

        compassCalculator.reset()

        magneticAnomalyDetector.reset()
        liveFilter.reset()

        lastMagnetTimestamp = null

        lastAnomaly = null

        magnetDiagUiCount = 0

        sensorManager.setSamplingRate(SamplingRate.NORMAL)

        _uiState.update { it.copy(midiendo = true, historial = emptyList()) }

        sensorManager.startReading(SensorType.ACCELEROMETER)

        sensorManager.startReading(SensorType.MAGNETOMETER)

    }



    fun detener() {

        sensorManager.stopReading(SensorType.MAGNETOMETER)

        sensorManager.stopReading(SensorType.ACCELEROMETER)

        _uiState.update { it.copy(midiendo = false) }

    }



    fun recalibrarCampoBase() {

        magneticAnomalyDetector.reset()
        liveFilter.reset()

        lastAnomaly = null

        lastMagnetTimestamp = null

    }



    fun onPermissionResult(granted: Boolean) {

        sensorManager.setPermissionGranted(SensorType.MAGNETOMETER, granted)

    }



    fun onResume() = sensorManager.onResume()



    fun onPause() {

        detener()

        sensorManager.onPause()

    }



    override fun onCleared() {

        detener()

        super.onCleared()

    }



    private fun nearbyExcess(anomaly: MagneticAnomalyResult?): Double? {
        if (anomaly == null || anomaly.phase == MagneticAnomalyPhase.CALIBRATING) return null
        return NearbyMagnetism.excessUt(anomaly.deltaUt)
    }

    private fun readingIntensity(reading: SensorReading): Double? {

        val x = reading.valueX ?: return null

        val y = reading.valueY ?: return null

        val z = reading.valueZ ?: return null

        return fisicaEngine.calcular(FormulaId.B_INTENSIDAD.id, mapOf("Bx" to x, "By" to y, "Bz" to z))

    }



    private fun updateFromReading(

        reading: SensorReading?,

        accelReading: SensorReading?,

        state: SensorState

    ) {

        val disponible = sensorManager.isSensorAvailable(SensorType.MAGNETOMETER) &&

            sensorManager.isSensorAvailable(SensorType.ACCELEROMETER)

        val magnitude = reading?.let { readingIntensity(it) }

        accelReading?.let { accel ->

            val x = accel.valueX ?: return@let

            val y = accel.valueY ?: return@let

            val z = accel.valueZ ?: return@let

            compassCalculator.updateAccelerometer(x, y, z)

        }

        reading?.let { magnet ->

            val x = magnet.valueX ?: return@let

            val y = magnet.valueY ?: return@let

            val z = magnet.valueZ ?: return@let

            compassCalculator.updateMagnetometer(x, y, z)

        }

        val azimuth = compassCalculator.azimuthDegrees()



        // Solo avanzar el detector con lecturas nuevas del magnetómetro (no en cada tick del accel).

        val nuevaLectura = reading != null && magnitude != null && reading.timestamp != lastMagnetTimestamp

        val anomaly = if (nuevaLectura) {

            lastMagnetTimestamp = reading.timestamp

            magneticAnomalyDetector.observe(magnitude, azimuth).also {

                lastAnomaly = it

                logMagnetUi(reading, magnitude, azimuth, it)

            }

        } else {

            lastAnomaly

        }



        val resolvedState = if (

            disponible &&

            (state == SensorState.UNAVAILABLE || state == SensorState.DISABLED)

        ) {

            SensorState.IDLE

        } else {

            state

        }

        val excesoVisible = nearbyExcess(anomaly)?.let { excess -> liveFilter.stable("b", excess, 1.0) }

        _uiState.update {

            it.copy(

                disponible = disponible,

                sensorState = resolvedState,

                permisoRequerido = state == SensorState.PERMISSION_DENIED,

                intensidadUt = excesoVisible,
                historial = if (it.midiendo && nuevaLectura && excesoVisible != null) {
                    if (it.historial.lastOrNull() == excesoVisible) it.historial
                    else (it.historial + excesoVisible).takeLast(120)
                } else {
                    it.historial
                },

                azimuth = azimuth,

                direccion = azimuth?.let { a -> MagneticCardinals.fromAzimuth(a) } ?: "N/A",

                bx = reading?.valueX?.let { v -> "%.0f µT".format(liveFilter.stable("bx", v, 1.0)) } ?: "N/A",

                by = reading?.valueY?.let { v -> "%.0f µT".format(liveFilter.stable("by", v, 1.0)) } ?: "N/A",

                bz = reading?.valueZ?.let { v -> "%.0f µT".format(liveFilter.stable("bz", v, 1.0)) } ?: "N/A",

                campoBase = anomaly?.baselineUt?.let { v -> "%.1f µT".format(v) }

                    ?: if (anomaly?.phase == MagneticAnomalyPhase.CALIBRATING) {

                        "Calibrando (${anomaly.samplesCollected}/${anomaly.samplesRequired})"

                    } else {

                        "N/A"

                    },

                campoActual = magnitude?.let { v -> "%.1f µT".format(v) } ?: "N/A",

                deltaCampo = anomaly?.deltaUt?.let { v -> "%.1f µT".format(v) } ?: "N/A",

                anomaliaEstado = when (anomaly?.phase) {

                    MagneticAnomalyPhase.CALIBRATING ->

                        "Calibrando (${anomaly.samplesCollected}/${anomaly.samplesRequired})"

                    MagneticAnomalyPhase.NORMAL -> "NORMAL"

                    MagneticAnomalyPhase.ANOMALY -> anomaly.peakDirectionCardinal?.let { dir ->

                        "IMÁN CERCA → $dir"

                    } ?: "ANOMALÍA DETECTADA"

                    null -> "N/A"

                },

                imanHint = anomaly?.hintMessage ?: "N/A",

                imanDireccion = anomaly?.peakDirectionCardinal ?: "N/A",

                brujulaValida = compassCalculator.rotationMatrixValid()

            )

        }

    }



    private fun logMagnetUi(

        reading: SensorReading,

        intensity: Double,

        azimuth: Double?,

        anomaly: MagneticAnomalyResult

    ) {

        magnetDiagUiCount++

        if (magnetDiagUiCount > 12) return

        if (magnetDiagUiCount > 4 && magnetDiagUiCount % 3 != 0) return

        android.util.Log.i(

            "MagnetometroDiag",

            "LabUI #$magnetDiagUiCount Bx=${reading.valueX} By=${reading.valueY} Bz=${reading.valueZ} " +

                "B=${"%.2f".format(intensity)}µT azimut=${azimuth?.let { "%.1f°".format(it) } ?: "n/a"} " +

                "cardinal=${azimuth?.let { MagneticCardinals.fromAzimuth(it) } ?: "n/a"} " +

                "fase=${anomaly.phase} delta=${anomaly.deltaUt?.let { "%.1f".format(it) } ?: "n/a"} " +

                "imanDir=${anomaly.peakDirectionCardinal ?: "n/a"} " +

                "rotOk=${compassCalculator.rotationMatrixValid()}"

        )

    }

}


