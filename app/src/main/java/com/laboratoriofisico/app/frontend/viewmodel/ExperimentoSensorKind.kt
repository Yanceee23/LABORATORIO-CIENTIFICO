package com.laboratoriofisico.app.frontend.viewmodel

import com.laboratoriofisico.app.sensores.model.SensorType

enum class ExperimentoSensorKind(
    val sensorType: SensorType,
    val unidadDisplay: String,
    val extraSensorType: SensorType? = null,
    val catalogTitulo: String
) {
    LUZ(SensorType.LIGHT, "lux", catalogTitulo = "Análisis de luz"),
    SONIDO(SensorType.MICROPHONE, "dB rel", catalogTitulo = "Medir sonido"),
    MAGNETISMO(SensorType.MAGNETOMETER, "µT", SensorType.ACCELEROMETER, catalogTitulo = "Campo magnético"),
    DISTANCIA(SensorType.LOCATION, "m", catalogTitulo = "Medir distancia"),
    MOVIMIENTO(SensorType.ACCELEROMETER, "m/s²", SensorType.LOCATION, catalogTitulo = "Movimiento"),
    FRECUENCIA(SensorType.MICROPHONE, "Hz", catalogTitulo = "Medir frecuencia"),
    ONDAS(SensorType.MICROPHONE, "dB rel", catalogTitulo = "Analizar ondas"),
    PENDULO(SensorType.ACCELEROMETER, "s", catalogTitulo = "Péndulo"),
    REFLEXION(SensorType.CAMERA, "raw", SensorType.ACCELEROMETER, catalogTitulo = "Reflexión de luz")
}
