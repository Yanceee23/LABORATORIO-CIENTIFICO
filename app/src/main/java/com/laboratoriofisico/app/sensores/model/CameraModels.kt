package com.laboratoriofisico.app.sensores.model

enum class CameraHardwareState {
    UNAVAILABLE,
    NO_CAMERA,
    PERMISSION_REQUIRED,
    INITIALIZING,
    READY,
    RUNNING,
    BUSY,
    ERROR,
    CLOSED
}

enum class CameraLensFacing(val label: String) {
    BACK("Trasera"),
    FRONT("Frontal")
}
