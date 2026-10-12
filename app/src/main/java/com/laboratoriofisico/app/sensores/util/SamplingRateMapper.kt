package com.laboratoriofisico.app.sensores.util

import android.hardware.SensorManager as AndroidSensorManager
import com.laboratoriofisico.app.sensores.model.SamplingRate

fun SamplingRate.toAndroidSensorDelay(): Int = when (this) {
    SamplingRate.LOW -> AndroidSensorManager.SENSOR_DELAY_NORMAL
    SamplingRate.NORMAL -> AndroidSensorManager.SENSOR_DELAY_GAME
    // HIGH: no usar SENSOR_DELAY_FASTEST (0 µs). En SM-G780G provoca registerListener=false
    // en varios sensores; GAME (~50 Hz) es suficiente para péndulo/movimiento.
    SamplingRate.HIGH -> AndroidSensorManager.SENSOR_DELAY_GAME
}

fun SamplingRate.toLocationIntervalMs(): Long = when (this) {
    SamplingRate.LOW -> 5_000L
    SamplingRate.NORMAL -> 2_000L
    SamplingRate.HIGH -> 1_000L
}

fun SamplingRate.toAudioSampleRate(): Int = when (this) {
    SamplingRate.LOW -> 8_000
    SamplingRate.NORMAL -> 16_000
    SamplingRate.HIGH -> 44_100
}

fun SamplingRate.toAudioPollingDelayMs(): Long = when (this) {
    SamplingRate.LOW -> 500L
    SamplingRate.NORMAL -> 200L
    SamplingRate.HIGH -> 100L
}
