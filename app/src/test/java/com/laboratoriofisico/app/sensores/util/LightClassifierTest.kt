package com.laboratoriofisico.app.sensores.util

import com.laboratoriofisico.app.sensores.model.LightReading
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LightClassifierTest {

    @Test
    fun relativeLuma_autoCalibratesFirstSecond() {
        val calibrator = RelativeLightCalibrator()

        val first = calibrator.update(luma = 100.0, timestampMs = 0L)
        val second = calibrator.update(luma = 102.0, timestampMs = 500L)
        val calibrated = calibrator.update(luma = 101.0, timestampMs = 1_000L)

        assertEquals(false, first.calibrated)
        assertEquals(false, second.calibrated)
        assertEquals(true, calibrated.calibrated)
        assertEquals(101.0, calibrated.baseline!!, 0.0001)
        assertEquals(LightLevel.MEDIUM, calibrated.level)
    }

    @Test
    fun relativeLuma_classifiesAgainstAutomaticBaseline() {
        val calibrator = RelativeLightCalibrator()
        calibrator.update(100.0, 0L)
        calibrator.update(100.0, 500L)
        calibrator.update(100.0, 1_000L)

        assertEquals(LightLevel.LOW, calibrator.update(80.0, 1_100L).level)
        assertEquals(LightLevel.MEDIUM, calibrator.update(110.0, 1_200L).level)
        assertEquals(LightLevel.HIGH, calibrator.update(130.0, 1_300L).level)
    }

    @Test
    fun relativeLuma_rawEquivalentUsesFullUiRange() {
        val calibrator = RelativeLightCalibrator()

        assertEquals(0.0, calibrator.update(0.0, 0L).rawEquivalent, 0.0001)
        calibrator.reset()
        assertEquals(60_000.0, calibrator.update(255.0, 0L).rawEquivalent, 0.0001)
    }

    @Test
    fun relativeLuma_progressTracksRelativeChange() {
        val calibrator = RelativeLightCalibrator()
        calibrator.update(100.0, 0L)
        calibrator.update(100.0, 500L)
        val calibrated = calibrator.update(100.0, 1_000L)

        assertEquals(0.5f, calibrator.progressFor(0.0, 100.0, true), 0.0001f)
        assertTrue(calibrator.progressFor(0.30, 130.0, true) > 0.5f)
        assertTrue(calibrator.progressFor(-0.30, 70.0, true) < 0.5f)
        assertEquals(true, calibrated.calibrated)
    }

    @Test
    fun classifyLight_raw_lowMediumHigh() {
        assertEquals(LightLevel.LOW, LightClassifier.classifyLight(1_500.0, LightMeasurementUnit.RAW))
        assertEquals(LightLevel.MEDIUM, LightClassifier.classifyLight(8_000.0, LightMeasurementUnit.RAW))
        assertEquals(LightLevel.HIGH, LightClassifier.classifyLight(20_000.0, LightMeasurementUnit.RAW))
    }

    @Test
    fun classifyLight_lux_lowMediumHigh() {
        assertEquals(LightLevel.LOW, LightClassifier.classifyLight(50.0, LightMeasurementUnit.LUX))
        assertEquals(LightLevel.MEDIUM, LightClassifier.classifyLight(250.0, LightMeasurementUnit.LUX))
        assertEquals(LightLevel.HIGH, LightClassifier.classifyLight(600.0, LightMeasurementUnit.LUX))
    }

    @Test
    fun classifyFromReading_experimentalRaw() {
        val reading = LightReading(
            rawValue = 8_000.0,
            timestamp = 1L,
            experimental = true,
            sensorMaxRange = 60_000.0
        ).toSensorReading()
        assertEquals(LightLevel.MEDIUM, LightClassifier.classifyFromReading(reading))
    }

    @Test
    fun lightProgress_usesHardwareMaxRangeLinearly() {
        val progress = LightClassifier.lightProgress(15_000.0, LightMeasurementUnit.RAW, 60_000.0)
        assertEquals(0.25f, progress, 0.001f)
    }

    @Test
    fun lightProgress_isMonotonicAndBounded() {
        var previous = -1f
        listOf(0.0, 500.0, 3_000.0, 8_000.0, 15_000.0, 30_000.0, 60_000.0).forEach { value ->
            val progress = LightClassifier.lightProgress(value, LightMeasurementUnit.RAW, 60_000.0)
            assertTrue("progreso fuera de rango en $value", progress in 0f..1f)
            assertTrue("progreso no monótono en $value", progress >= previous)
            previous = progress
        }
        assertEquals(1f, LightClassifier.lightProgress(60_000.0, LightMeasurementUnit.RAW, 60_000.0), 0.0001f)
    }

    @Test
    fun levelLabels_areSafeWording() {
        assertEquals("BAJA", LightClassifier.levelDisplayName(LightLevel.LOW))
        assertEquals("MEDIA", LightClassifier.levelDisplayName(LightLevel.MEDIUM))
        assertEquals("ALTA", LightClassifier.levelDisplayName(LightLevel.HIGH))
        assertEquals("Poca luz — zona verde", LightClassifier.levelDescription(LightLevel.LOW))
        assertTrue(LightClassifier.safetyDisclaimer().contains("No determina por sí sola riesgo ocular"))
    }
}
