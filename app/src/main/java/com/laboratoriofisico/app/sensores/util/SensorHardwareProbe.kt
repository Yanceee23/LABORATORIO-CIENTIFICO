package com.laboratoriofisico.app.sensores.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import com.laboratoriofisico.app.sensores.model.SensorType

/**
 * Detección centralizada de hardware de sensores.
 * Fallback experimental para ALS propietarios Samsung (TCS3407, Camera Light, etc.).
 */
object SensorHardwareProbe {
    private const val TAG = "SensorHardwareProbe"

    /** TCS3407 Rear ALS — visible en TYPE_ALL pero a menudo no entrega eventos a terceros. */
    const val SAMSUNG_TCS3407_ALS_TYPE = 65_577

    /** Camera Light Sensor — suele ser el que realmente emite lecturas en Samsung. */
    const val SAMSUNG_CAMERA_LIGHT_TYPE = 65_604

    private var loggedAllSensors = false
    private var cachedLightResolution: LightSensorResolution? = null
    private var loggedExperimentalSelection = false

    data class LightSensorResolution(
        val sensor: Sensor,
        val isExperimental: Boolean
    )

    fun logAllSensorsOnce(context: Context) {
        if (loggedAllSensors) return
        loggedAllSensors = true
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val all = sensorManager.getSensorList(Sensor.TYPE_ALL)
        Log.i(TAG, "=== SensorManager.getSensorList(TYPE_ALL): ${all.size} sensores ===")
        all.forEach { sensor ->
            Log.i(
                TAG,
                "  type=${sensor.type} (${sensorTypeLabel(sensor.type)}) " +
                    "name=${sensor.name} vendor=${sensor.vendor}"
            )
        }
        val lightDefault = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        val lightList = sensorManager.getSensorList(Sensor.TYPE_LIGHT)
        val candidates = listLightCandidates(sensorManager)
        val resolved = resolveLightSensor(sensorManager)
        Log.i(
            TAG,
            "TYPE_LIGHT → default=${lightDefault?.name ?: "null"} " +
                "getSensorList(TYPE_LIGHT).size=${lightList.size} " +
                "candidates=${candidates.size} " +
                "resolved=${resolved?.sensor?.name ?: candidates.firstOrNull()?.name ?: "null"}"
        )
        candidates.forEachIndexed { index, sensor ->
            Log.i(TAG, "  light candidate[$index]: ${sensor.name} type=${sensor.type}")
        }
    }

    fun resolveMotionSensor(sensorManager: SensorManager, type: SensorType): Sensor? = when (type) {
        SensorType.ACCELEROMETER -> findStandardSensor(sensorManager, Sensor.TYPE_ACCELEROMETER)
        SensorType.GYROSCOPE -> findStandardSensor(sensorManager, Sensor.TYPE_GYROSCOPE)
        SensorType.MAGNETOMETER -> findStandardSensor(sensorManager, Sensor.TYPE_MAGNETIC_FIELD)
        SensorType.LIGHT -> resolveLightSensor(sensorManager)?.sensor
        else -> null
    }

    /**
     * Candidatos ordenados por probabilidad de entregar eventos.
     * Camera Light Sensor va primero porque el TCS3407 Rear ALS suele no emitir a terceros.
     */
    fun listLightCandidates(sensorManager: SensorManager): List<Sensor> {
        val all = sensorManager.getSensorList(Sensor.TYPE_ALL)
        val byType = all.associateBy { it.type }
        val ordered = buildList {
            findStandardLightSensor(sensorManager)?.let { add(it) }
            byType[SAMSUNG_CAMERA_LIGHT_TYPE]?.let { add(it) }
            all.filter { it.name.contains("Camera Light", ignoreCase = true) }
                .forEach { if (none { s -> s.type == it.type }) add(it) }
            byType[SAMSUNG_TCS3407_ALS_TYPE]?.let { add(it) }
            all.filter {
                it.name.contains("ALS", ignoreCase = true) ||
                    it.name.contains("Light", ignoreCase = true)
            }.forEach { if (none { s -> s.type == it.type }) add(it) }
        }
        return ordered.distinctBy { it.type }
    }

    fun resolveLightSensor(sensorManager: SensorManager): LightSensorResolution? {
        cachedLightResolution?.let { return it }

        listLightCandidates(sensorManager).firstOrNull()?.let { sensor ->
            val experimental = sensor.type != Sensor.TYPE_LIGHT
            if (experimental && !loggedExperimentalSelection) {
                loggedExperimentalSelection = true
                Log.i(
                    TAG,
                    "EXPERIMENTAL light sensor selected: name=${sensor.name} " +
                        "type=${sensor.type} vendor=${sensor.vendor} " +
                        "minDelay=${sensor.minDelay}us maxRange=${sensor.maximumRange} " +
                        "wakeUp=${sensor.isWakeUpSensor}"
                )
            }
            cachedLightResolution = LightSensorResolution(sensor = sensor, isExperimental = experimental)
            return cachedLightResolution
        }

        cachedLightResolution = null
        return null
    }

    fun updateActiveLightSensor(sensor: Sensor) {
        val experimental = sensor.type != Sensor.TYPE_LIGHT
        cachedLightResolution = LightSensorResolution(sensor = sensor, isExperimental = experimental)
        Log.i(
            TAG,
            "Light sensor activo cambiado a: name=${sensor.name} type=${sensor.type} " +
                "experimental=$experimental"
        )
    }

    fun isMotionSensorAvailable(sensorManager: SensorManager, type: SensorType): Boolean =
        resolveMotionSensor(sensorManager, type) != null

    fun isExperimentalLightSensor(sensorManager: SensorManager): Boolean =
        resolveLightSensor(sensorManager)?.isExperimental == true

    private fun findStandardLightSensor(sensorManager: SensorManager): Sensor? {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let { return it }
        sensorManager.getSensorList(Sensor.TYPE_LIGHT).firstOrNull()?.let { return it }
        return sensorManager.getSensorList(Sensor.TYPE_ALL)
            .firstOrNull { it.type == Sensor.TYPE_LIGHT }
    }

    private fun findStandardSensor(sensorManager: SensorManager, androidType: Int): Sensor? =
        sensorManager.getDefaultSensor(androidType)
            ?: sensorManager.getSensorList(androidType).firstOrNull()

    private fun sensorTypeLabel(type: Int): String = when (type) {
        Sensor.TYPE_ACCELEROMETER -> "ACCELEROMETER"
        Sensor.TYPE_GYROSCOPE -> "GYROSCOPE"
        Sensor.TYPE_MAGNETIC_FIELD -> "MAGNETIC_FIELD"
        Sensor.TYPE_LIGHT -> "LIGHT"
        SAMSUNG_TCS3407_ALS_TYPE -> "SAMSUNG_ALS_EXPERIMENTAL"
        SAMSUNG_CAMERA_LIGHT_TYPE -> "SAMSUNG_CAMERA_LIGHT"
        else -> type.toString()
    }
}
