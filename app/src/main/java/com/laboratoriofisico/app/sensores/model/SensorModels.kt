package com.laboratoriofisico.app.sensores.model

enum class SensorType {
    ACCELEROMETER,
    GYROSCOPE,
    MAGNETOMETER,
    LIGHT,
    LOCATION,
    MICROPHONE,
    CAMERA;

    val unit: String
        get() = when (this) {
            ACCELEROMETER -> "m/s²"
            GYROSCOPE -> "rad/s"
            MAGNETOMETER -> "µT"
            LIGHT -> "lux"
            LOCATION -> "deg"
            MICROPHONE -> "RMS"
            CAMERA -> "frame"
        }
}

enum class SamplingRate {
    LOW,
    NORMAL,
    HIGH
}

enum class SensorState {
    IDLE,
    ACTIVE,
    UNAVAILABLE,
    PERMISSION_DENIED,
    DISABLED,
    ERROR
}

data class SensorInfo(
    val type: SensorType,
    val available: Boolean,
    val state: SensorState = if (available) SensorState.IDLE else SensorState.UNAVAILABLE,
    val unit: String = type.unit
)

data class SensorReading(
    val id: Long = 0,
    val sensorType: SensorType,
    val timestamp: Long,
    val valueX: Double? = null,
    val valueY: Double? = null,
    val valueZ: Double? = null,
    val valueScalar: Double? = null,
    val altitude: Double? = null,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val unit: String = sensorType.unit,
    val metadata: String? = null
)

data class AccelerometerReading(
    val x: Double,
    val y: Double,
    val z: Double,
    val timestamp: Long,
    /**
     * Instante del evento reportado por el hardware (nanosegundos monótonos desde el arranque).
     * [timestamp] es la hora de entrega en el hilo principal e incluye jitter de la UI, por lo
     * que no sirve para medir intervalos entre oscilaciones.
     */
    val eventTimestampNanos: Long? = null,
    val samplePeriodUs: Int? = null
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.ACCELEROMETER,
        timestamp = timestamp,
        valueX = x,
        valueY = y,
        valueZ = z,
        unit = SensorType.ACCELEROMETER.unit,
        metadata = buildSensorMetadata()
    )

    private fun buildSensorMetadata(): String? = buildString {
        eventTimestampNanos?.let { append("event_ts_ns=$it") }
        samplePeriodUs?.let {
            if (isNotEmpty()) append(';')
            append("sample_period_us=$it")
        }
    }.takeIf { it.isNotEmpty() }
}

data class GyroscopeReading(
    val x: Double,
    val y: Double,
    val z: Double,
    val timestamp: Long
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.GYROSCOPE,
        timestamp = timestamp,
        valueX = x,
        valueY = y,
        valueZ = z,
        unit = SensorType.GYROSCOPE.unit
    )
}

data class MagnetometerReading(
    val x: Double,
    val y: Double,
    val z: Double,
    val timestamp: Long,
    val eventTimestampNanos: Long? = null,
    val accuracy: Int? = null
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.MAGNETOMETER,
        timestamp = timestamp,
        valueX = x,
        valueY = y,
        valueZ = z,
        unit = SensorType.MAGNETOMETER.unit,
        metadata = buildList {
            eventTimestampNanos?.let { add("event_ts_ns=$it") }
            accuracy?.let { add("accuracy=$it") }
        }.takeIf { it.isNotEmpty() }?.joinToString(";")
    )
}

data class LightReading(
    val rawValue: Double,
    val timestamp: Long,
    val experimental: Boolean = false,
    val rawValuesSummary: String? = null,
    val sensorName: String? = null,
    val sensorVendor: String? = null,
    val sensorAndroidType: Int? = null,
    val sensorMaxRange: Double? = null
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.LIGHT,
        timestamp = timestamp,
        valueScalar = rawValue,
        unit = if (experimental) "raw" else SensorType.LIGHT.unit,
        metadata = buildSensorMetadata()
    )

    private fun buildSensorMetadata(): String? = buildString {
        sensorName?.let { append("sensor_name=$it") }
        sensorVendor?.let {
            if (isNotEmpty()) append(';')
            append("sensor_vendor=$it")
        }
        sensorAndroidType?.let {
            if (isNotEmpty()) append(';')
            append("sensor_type=$it")
        }
        sensorMaxRange?.let {
            if (isNotEmpty()) append(';')
            append("sensor_max_range=$it")
        }
        if (experimental) {
            if (isNotEmpty()) append(';')
            append("experimental_als=true")
        }
        rawValuesSummary?.let {
            if (isNotEmpty()) append(';')
            append("raw_values=$it")
        }
    }.takeIf { it.isNotEmpty() }
}

data class LocationReading(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float?,
    val speed: Float?,
    val timestamp: Long
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.LOCATION,
        timestamp = timestamp,
        valueX = latitude,
        valueY = longitude,
        valueZ = altitude,
        altitude = altitude,
        accuracy = accuracy,
        speed = speed,
        unit = SensorType.LOCATION.unit
    )
}

data class AudioReading(
    val amplitude: Double,
    val sampleCount: Int,
    val timestamp: Long
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.MICROPHONE,
        timestamp = timestamp,
        valueScalar = amplitude,
        unit = SensorType.MICROPHONE.unit,
        metadata = "samples=$sampleCount"
    )
}

data class CameraReading(
    val timestamp: Long,
    val hardwareState: CameraHardwareState,
    val lensFacing: CameraLensFacing? = null,
    val frameWidth: Int? = null,
    val frameHeight: Int? = null,
    val brightness: Double? = null,
    val fpsEstimate: Double? = null,
    val imageFormat: String? = null,
    val capturedImagePath: String? = null,
    val errorMessage: String? = null,
    val exposureTimeNs: Long? = null,
    val iso: Int? = null,
    val aperture: Double? = null
) {
    fun toSensorReading(): SensorReading = SensorReading(
        sensorType = SensorType.CAMERA,
        timestamp = timestamp,
        valueX = frameWidth?.toDouble(),
        valueY = frameHeight?.toDouble(),
        valueScalar = brightness,
        valueZ = fpsEstimate,
        unit = SensorType.CAMERA.unit,
        metadata = buildCameraMetadata()
    )

    private fun buildCameraMetadata(): String = buildString {
        append("state=${hardwareState.name}")
        lensFacing?.let { append(";lens=${it.name}") }
        imageFormat?.let { append(";format=$it") }
        capturedImagePath?.let { append(";capture=$it") }
        errorMessage?.let { append(";error=$it") }
        exposureTimeNs?.let { append(";exposure_ns=$it") }
        iso?.let { append(";iso=$it") }
        aperture?.let { append(";aperture=$it") }
    }
}
