package com.laboratoriofisico.app.sensores.permissions

import android.Manifest
import com.laboratoriofisico.app.sensores.model.SensorType

object SensorPermissions {
    fun requiredPermissions(type: SensorType): Array<String> = when (type) {
        SensorType.LOCATION -> arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        SensorType.MICROPHONE -> arrayOf(Manifest.permission.RECORD_AUDIO)
        SensorType.CAMERA -> arrayOf(Manifest.permission.CAMERA)
        else -> emptyArray()
    }

    fun requiresRuntimePermission(type: SensorType): Boolean =
        requiredPermissions(type).isNotEmpty()
}
