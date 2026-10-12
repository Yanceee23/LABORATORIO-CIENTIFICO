package com.laboratoriofisico.app.sensores.util



import kotlin.math.abs

import kotlin.math.max



/**

 * Detecta anomalías del campo magnético local (µT) respecto a una línea base ambiental.

 *

 * No estima distancia al imán (imposible con el magnetómetro del celular a escala de km).

 * Tras calibrar, compara la intensidad mientras el usuario gira el teléfono y registra en qué

 * dirección cardinal la intensidad es mayor: eso indica la dirección aproximada de un imán

 * cercano (centímetros a ~1–2 m según la potencia del imán).

 */

class MagneticAnomalyDetector(

    private val baselineSampleCount: Int = 25,

    private val anomalyThresholdUt: Double = 12.0

) {

    private val baselineSamples = mutableListOf<Double>()

    private var baselineUt: Double? = null

    private val sectorPeakUt = DoubleArray(SECTOR_COUNT) { Double.NEGATIVE_INFINITY }

    private var peakIntensityUt: Double? = null

    private var peakAzimuthDegrees: Double? = null



    fun reset() {

        baselineSamples.clear()

        baselineUt = null

        sectorPeakUt.fill(Double.NEGATIVE_INFINITY)

        peakIntensityUt = null

        peakAzimuthDegrees = null

    }



    fun observe(intensityUt: Double, azimuthDegrees: Double? = null): MagneticAnomalyResult {

        val baseline = baselineUt

        if (baseline == null) {

            baselineSamples.add(intensityUt)

            if (baselineSamples.size >= baselineSampleCount) {

                baselineUt = baselineSamples.average()

            }

            return MagneticAnomalyResult(

                phase = MagneticAnomalyPhase.CALIBRATING,

                baselineUt = baselineUt,

                currentUt = intensityUt,

                deltaUt = null,

                samplesCollected = baselineSamples.size,

                samplesRequired = baselineSampleCount,

                peakDirectionCardinal = null,

                peakAzimuthDegrees = null,

                hintMessage = "Calibrando campo ambiental: mantén el teléfono quieto, lejos de imanes."

            )

        }



        if (azimuthDegrees != null) {

            val sector = MagneticCardinals.sectorIndex(azimuthDegrees)

            sectorPeakUt[sector] = max(sectorPeakUt[sector], intensityUt)

            val previousPeak = peakIntensityUt

            if (previousPeak == null || intensityUt > previousPeak) {

                peakIntensityUt = intensityUt

                peakAzimuthDegrees = azimuthDegrees

            }

        }



        val delta = intensityUt - baseline

        val absDelta = abs(delta)

        val phase = if (absDelta >= anomalyThresholdUt) {

            MagneticAnomalyPhase.ANOMALY

        } else {

            MagneticAnomalyPhase.NORMAL

        }



        val strongest = strongestAnomalousSector(baseline)

        val peakCardinal = strongest?.let { MagneticCardinals.SECTOR_NAMES[it] }

            ?: peakAzimuthDegrees?.let { MagneticCardinals.fromAzimuth(it) }

                ?.takeIf { phase == MagneticAnomalyPhase.ANOMALY }



        val hint = when {

            phase == MagneticAnomalyPhase.ANOMALY && peakCardinal != null ->

                "Imán detectado cerca, hacia el $peakCardinal (pocos cm a ~1–2 m). No es una distancia en km."

            phase == MagneticAnomalyPhase.ANOMALY ->

                "Anomalía magnética cerca del teléfono. Gira despacio para localizar la dirección."

            else ->

                "Campo normal. Acerca un imán y gira el teléfono para hallar la dirección de mayor intensidad."

        }



        return MagneticAnomalyResult(

            phase = phase,

            baselineUt = baseline,

            currentUt = intensityUt,

            deltaUt = delta,

            samplesCollected = baselineSampleCount,

            samplesRequired = baselineSampleCount,

            peakDirectionCardinal = peakCardinal,

            peakAzimuthDegrees = peakAzimuthDegrees,

            hintMessage = hint

        )

    }



    private fun strongestAnomalousSector(baseline: Double): Int? {

        var bestIndex: Int? = null

        var bestValue = Double.NEGATIVE_INFINITY

        for (i in sectorPeakUt.indices) {

            val value = sectorPeakUt[i]

            if (!value.isFinite()) continue

            if (value < baseline + anomalyThresholdUt) continue

            if (value > bestValue) {

                bestValue = value

                bestIndex = i

            }

        }

        return bestIndex

    }



    private companion object {

        const val SECTOR_COUNT = 8

    }

}



enum class MagneticAnomalyPhase {

    CALIBRATING,

    NORMAL,

    ANOMALY

}



data class MagneticAnomalyResult(

    val phase: MagneticAnomalyPhase,

    val baselineUt: Double?,

    val currentUt: Double,

    val deltaUt: Double?,

    val samplesCollected: Int,

    val samplesRequired: Int,

    val peakDirectionCardinal: String? = null,

    val peakAzimuthDegrees: Double? = null,

    val hintMessage: String? = null

)



/** Conversión de azimut (0–360°) a punto cardinal. */

object MagneticCardinals {

    val SECTOR_NAMES = arrayOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")



    fun fromAzimuth(azimuthDegrees: Double): String {

        val normalized = ((azimuthDegrees % 360.0) + 360.0) % 360.0

        return SECTOR_NAMES[sectorIndex(normalized)]

    }



    fun sectorIndex(azimuthDegrees: Double): Int {

        val normalized = ((azimuthDegrees % 360.0) + 360.0) % 360.0

        return ((normalized + 22.5) / 45.0).toInt() % 8

    }

}


