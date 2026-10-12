package com.laboratoriofisico.app.sensores.model

fun SensorState.displayLabel(): String = when (this) {
    SensorState.IDLE -> "Listo para medir"
    SensorState.ACTIVE -> "Midiendo ahora"
    SensorState.UNAVAILABLE -> "Este teléfono no tiene el sensor"
    SensorState.PERMISSION_DENIED -> "Falta el permiso"
    SensorState.DISABLED -> "El sensor está apagado"
    SensorState.ERROR -> "No se pudo leer el sensor"
}
