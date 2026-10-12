package com.laboratoriofisico.app.sensores.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorValueExtractorsTest {

    @Test
    fun tiltDegrees_fromGravityVector() {
        val (pitch, roll) = SensorValueExtractors.tiltDegrees(0.0, 9.81, 0.0)
        assertEquals(0.0, pitch, 1.0)
        assertEquals(90.0, roll, 1.0)
    }

    @Test
    fun gyroRadiansToDegreesPerSecond() {
        val deg = SensorValueExtractors.radiansToDegreesPerSecond(Math.PI)
        assertEquals(180.0, deg, 0.01)
    }

    @Test
    fun magnetMagnitude_fromComponents() {
        val b = SensorValueExtractors.magnetMagnitude(3.0, 4.0, 0.0)
        assertEquals(5.0, b, 0.001)
    }

    @Test
    fun lightPrimaryValue_usesRawWhenExperimental() {
        val reading = com.laboratoriofisico.app.sensores.model.LightReading(
            rawValue = 120.0,
            timestamp = 1L,
            experimental = true
        ).toSensorReading()
        assertEquals(120.0, SensorValueExtractors.lightPrimaryValue(reading)!!, 0.001)
    }

    @Test
    fun lightPrimaryValue_usesLuxWhenStandard() {
        val reading = com.laboratoriofisico.app.sensores.model.LightReading(
            rawValue = 250.0,
            timestamp = 1L,
            experimental = false
        ).toSensorReading()
        assertEquals(250.0, SensorValueExtractors.lightPrimaryValue(reading)!!, 0.001)
    }

    @Test
    fun lux_nullForRawUnit() {
        val reading = com.laboratoriofisico.app.sensores.model.LightReading(
            rawValue = 120.0,
            timestamp = 1L,
            experimental = true
        ).toSensorReading()
        assertNull(SensorValueExtractors.lux(reading))
        assertNotNull(SensorValueExtractors.rawLight(reading))
    }

    @Test
    fun relativeDb_notSpl() {
        val db = SensorValueExtractors.amplitudeToRelativeDb(0.1)
        assertTrue(db < 0)
    }

    @Test
    fun microphoneLevel_risesWithRealAmplitude() {
        val quiet = SensorValueExtractors.microphoneLevelPercent(0.001)
        val speech = SensorValueExtractors.microphoneLevelPercent(0.05)
        val loud = SensorValueExtractors.microphoneLevelPercent(0.5)
        assertTrue(quiet < speech)
        assertTrue(speech < loud)
        assertTrue(loud <= 100.0)
    }
}
