package com.laboratoriofisico.app.sensores.util



import org.junit.Assert.assertEquals

import org.junit.Assert.assertNotNull

import org.junit.Assert.assertNull

import org.junit.Assert.assertTrue

import org.junit.Test



class MagneticAnomalyDetectorTest {



    @Test

    fun calibratesThenDetectsAnomaly() {

        val detector = MagneticAnomalyDetector(baselineSampleCount = 3, anomalyThresholdUt = 10.0)

        repeat(3) {

            val result = detector.observe(45.0)

            assertEquals(MagneticAnomalyPhase.CALIBRATING, result.phase)

        }

        val normal = detector.observe(46.0)

        assertEquals(MagneticAnomalyPhase.NORMAL, normal.phase)

        assertEquals(45.0, normal.baselineUt!!, 0.01)



        val anomaly = detector.observe(70.0)

        assertEquals(MagneticAnomalyPhase.ANOMALY, anomaly.phase)

        assertEquals(25.0, anomaly.deltaUt!!, 0.01)

    }



    @Test

    fun resetClearsBaseline() {

        val detector = MagneticAnomalyDetector(baselineSampleCount = 2, anomalyThresholdUt = 5.0)

        detector.observe(10.0)

        detector.observe(10.0)

        detector.reset()

        val afterReset = detector.observe(50.0)

        assertEquals(MagneticAnomalyPhase.CALIBRATING, afterReset.phase)

        assertNull(afterReset.baselineUt)

    }



    @Test

    fun reportsStrongestCardinalWhenRotatingNearMagnet() {

        val detector = MagneticAnomalyDetector(baselineSampleCount = 3, anomalyThresholdUt = 10.0)

        repeat(3) { detector.observe(40.0, azimuthDegrees = 0.0) }



        // Campo normal hacia el este; pico hacia el norte (imán).

        detector.observe(42.0, azimuthDegrees = 90.0)

        val northPeak = detector.observe(80.0, azimuthDegrees = 5.0)

        assertEquals(MagneticAnomalyPhase.ANOMALY, northPeak.phase)

        assertEquals("N", northPeak.peakDirectionCardinal)

        assertNotNull(northPeak.hintMessage)

        assertTrue(northPeak.hintMessage!!.contains("norte", ignoreCase = true) ||

            northPeak.hintMessage!!.contains("N"))

    }



    @Test

    fun magneticCardinalsFromAzimuth() {

        assertEquals("N", MagneticCardinals.fromAzimuth(0.0))

        assertEquals("E", MagneticCardinals.fromAzimuth(90.0))

        assertEquals("S", MagneticCardinals.fromAzimuth(180.0))

        assertEquals("O", MagneticCardinals.fromAzimuth(270.0))

        assertEquals("N", MagneticCardinals.fromAzimuth(359.0))

    }



    @Test

    fun neverReportsKilometerDistance() {

        val detector = MagneticAnomalyDetector(baselineSampleCount = 2, anomalyThresholdUt = 5.0)

        detector.observe(30.0, 0.0)

        detector.observe(30.0, 0.0)

        val result = detector.observe(100.0, 45.0)

        assertNotNull(result.hintMessage)

        assertTrue(!result.hintMessage!!.contains("km", ignoreCase = true) ||

            result.hintMessage!!.contains("No es una distancia en km"))

    }

}


