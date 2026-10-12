package com.laboratoriofisico.app.sensores.provider

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager as AndroidSensorManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.laboratoriofisico.app.core.debug.DebugSessionLog
import com.laboratoriofisico.app.sensores.model.AccelerometerReading
import com.laboratoriofisico.app.sensores.model.GyroscopeReading
import com.laboratoriofisico.app.sensores.model.LightReading
import com.laboratoriofisico.app.sensores.model.MagnetometerReading
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.util.SensorHardwareProbe
import com.laboratoriofisico.app.sensores.util.toAndroidSensorDelay

class MotionSensorProvider(context: Context) {
    private val appContext = context.applicationContext
    private val sensorManager =
        appContext.getSystemService(Context.SENSOR_SERVICE) as AndroidSensorManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = mutableMapOf<SensorType, SensorEventListener>()
    private val sensors = mutableMapOf<SensorType, Sensor>()
    private val readingCallbacks = mutableMapOf<SensorType, (SensorReading) -> Unit>()
    private var lightResolution: SensorHardwareProbe.LightSensorResolution? = null
    private var lightCandidates: List<Sensor> = emptyList()
    private var lightCandidateIndex = 0
    private var experimentalLightEventCount = 0
    private var lightFailoverWatchdog: Runnable? = null
    private var lightNudgeRunnable: Runnable? = null
    private var activeLightListener: SensorEventListener? = null
    private var lastLightRawValue: Float? = null
    private val registeredSamplePeriodUs = mutableMapOf<SensorType, Int>()
    private val magnetAccuracyBySensor = mutableMapOf<Int, Int>()
    private var magnetDiagCount = 0
    private var lightUiDiagCount = 0

    fun registeredSamplePeriodUs(type: SensorType): Int? = registeredSamplePeriodUs[type]

    init {
        SensorHardwareProbe.logAllSensorsOnce(appContext)
    }

    fun isAvailable(type: SensorType): Boolean {
        if (type == SensorType.LIGHT) {
            val candidates = lightCandidates.ifEmpty {
                filterAccessibleLightSensors(
                    SensorHardwareProbe.listLightCandidates(sensorManager)
                ).also { lightCandidates = it }
            }
            val available = candidates.isNotEmpty()
            DebugSessionLog.log(
                hypothesisId = "H3",
                location = "MotionSensorProvider.isAvailable",
                message = "Detección sensor LIGHT",
                data = mapOf(
                    "available" to available,
                    "candidateCount" to candidates.size,
                    "activeSensor" to sensors[SensorType.LIGHT]?.name,
                    "eventCount" to experimentalLightEventCount
                )
            )
            return available
        }
        return SensorHardwareProbe.isMotionSensorAvailable(sensorManager, type)
    }

    fun isLightExperimental(): Boolean = lightResolution()?.isExperimental == true

    fun start(
        type: SensorType,
        rate: SamplingRate,
        onReading: (SensorReading) -> Unit
    ): Boolean {
        if (type == SensorType.LIGHT) {
            return startLight(onReading)
        }

        stop(type)
        if (type == SensorType.MAGNETOMETER) {
            magnetDiagCount = 0
        }
        val sensor = SensorHardwareProbe.resolveMotionSensor(sensorManager, type) ?: run {
            Log.e(LOG_TAG, "start($type): sensor null")
            return false
        }
        readingCallbacks[type] = onReading
        val listener = buildMotionListener(type)
        if (!registerWithBestStrategy(listener, sensor, rate, type)) {
            Log.e(LOG_TAG, "start($type): registerListener falló para ${sensor.name}")
            readingCallbacks.remove(type)
            return false
        }
        listeners[type] = listener
        sensors[type] = sensor
        registeredSamplePeriodUs[type]?.let { delayUs ->
            Log.i(LOG_TAG, "start($type): cadencia efectiva=${delayUs}us (${1_000_000.0 / delayUs}Hz) sensor=${sensor.name}")
        }
        return true
    }

    fun stop(type: SensorType) {
        if (type == SensorType.LIGHT) {
            cancelLightFailover()
            cancelLightNudge()
            activeLightListener?.let { sensorManager.unregisterListener(it) }
            activeLightListener = null
            lightCandidateIndex = 0
            experimentalLightEventCount = 0
            lastLightRawValue = null
            Log.i(LUZ_TAG, "stop(LIGHT): listener desregistrado")
        } else {
            listeners.remove(type)?.let { sensorManager.unregisterListener(it) }
            sensors.remove(type)
        }
        readingCallbacks.remove(type)
    }

    fun stopAll() {
        SensorType.entries.filter { it in listeners.keys || it == SensorType.LIGHT && activeLightListener != null }
            .toList()
            .forEach { stop(it) }
    }

    private fun startLight(onReading: (SensorReading) -> Unit): Boolean {
        stop(SensorType.LIGHT)
        readingCallbacks[SensorType.LIGHT] = onReading
        lightCandidates = filterAccessibleLightSensors(
            SensorHardwareProbe.listLightCandidates(sensorManager)
        )
        lightCandidateIndex = 0
        experimentalLightEventCount = 0
        lastLightRawValue = null
        lightUiDiagCount = 0

        // #region agent log
        val typeLight = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        Log.d(
            LUZ_TAG,
            "Sensor TYPE_LIGHT encontrado: ${typeLight != null}, " +
                "nombre: ${typeLight?.name}, maxRange: ${typeLight?.maximumRange}"
        )
        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
            hypothesisId = "H-LUZ-A",
            location = "MotionSensorProvider.startLight.typeLightProbe",
            message = "Probe TYPE_LIGHT vs candidatos",
            data = mapOf(
                "typeLightNull" to (typeLight == null),
                "typeLightName" to typeLight?.name,
                "candidateCount" to lightCandidates.size,
                "candidates" to lightCandidates.joinToString { "${it.name}:${it.type}" }
            ),
            runId = "post-fix"
        )
        // #endregion

        if (lightCandidates.isEmpty()) {
            Log.e(LUZ_TAG, "start(LIGHT): sin candidatos de hardware")
            readingCallbacks.remove(SensorType.LIGHT)
            return false
        }

        Log.i(LUZ_TAG, "start(LIGHT): ${lightCandidates.size} candidatos → ${lightCandidates.map { it.name }}")
        // #region agent log
        com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
            hypothesisId = "H-LUZ-A",
            location = "MotionSensorProvider.startLight",
            message = "Registro luz iniciado",
            data = mapOf(
                "candidateCount" to lightCandidates.size,
                "candidates" to lightCandidates.joinToString { "${it.name}:${it.type}" },
                "hasCallback" to (readingCallbacks[SensorType.LIGHT] != null)
            ),
            runId = "post-fix"
        )
        // #endregion
        val ok = tryLightCandidate(atIndex = 0)
        if (!ok) {
            readingCallbacks.remove(SensorType.LIGHT)
        }
        return ok
    }

    private fun tryLightCandidate(atIndex: Int): Boolean {
        if (atIndex >= lightCandidates.size) {
            Log.e(LUZ_TAG, "start(LIGHT): ningún candidato entregó eventos")
            return false
        }

        cancelLightNudge()
        activeLightListener?.let { sensorManager.unregisterListener(it) }
        cancelLightFailover()

        lightCandidateIndex = atIndex
        val sensor = lightCandidates[atIndex]
        lightResolution = SensorHardwareProbe.LightSensorResolution(
            sensor = sensor,
            isExperimental = sensor.type != Sensor.TYPE_LIGHT
        )
        SensorHardwareProbe.updateActiveLightSensor(sensor)

        Log.i(
            LUZ_TAG,
            "tryLightCandidate[$atIndex/${lightCandidates.size}]: ${sensor.name} " +
                "type=${sensor.type} maxRange=${sensor.maximumRange} reporting=${sensor.reportingMode}"
        )

        val listener = buildLightListener()
        if (!registerLightListener(listener, sensor)) {
            Log.w(LUZ_TAG, "registerListener falló para ${sensor.name}, siguiente candidato…")
            return tryLightCandidate(atIndex + 1)
        }

        activeLightListener = listener
        sensors[SensorType.LIGHT] = sensor
        scheduleLightFailover(sensor, atIndex)
        // Re-registro suave a 200ms: Samsung on-change a veces no emite el valor inicial.
        scheduleLightNudge(sensor, listener)
        return true
    }

    private fun buildLightListener(): SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            cancelLightFailover()
            // Un re-registro continuo pisa la lectura: el primer evento real basta.
            cancelLightNudge()
            experimentalLightEventCount++
            val raw = event.values[0]
            lastLightRawValue = raw
            logLightReading(event)

            val reading = LightReading(
                rawValue = raw.toDouble(),
                timestamp = System.currentTimeMillis(),
                experimental = lightResolution()?.isExperimental == true,
                rawValuesSummary = event.values.joinToString(",") { "%.4f".format(it) },
                sensorName = event.sensor.name,
                sensorVendor = event.sensor.vendor,
                sensorAndroidType = event.sensor.type,
                sensorMaxRange = event.sensor.maximumRange.toDouble()
            ).toSensorReading()

            readingCallbacks[SensorType.LIGHT]?.invoke(reading)
                ?: Log.w(LUZ_TAG, "evento luz sin callback registrado raw=$raw")
            // #region agent log
            if (experimentalLightEventCount <= 12) {
                com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                    hypothesisId = "H-LUZ-B",
                    location = "MotionSensorProvider.onSensorChanged",
                    message = "Evento luz hardware",
                    data = mapOf(
                        "raw" to raw.toDouble(),
                        "eventCount" to experimentalLightEventCount,
                        "sensor" to event.sensor.name,
                        "maxRange" to event.sensor.maximumRange.toDouble(),
                        "callbackOk" to (readingCallbacks[SensorType.LIGHT] != null)
                    ),
                    runId = "post-fix"
                )
            }
            // #endregion
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.i(LUZ_TAG, "onAccuracyChanged: ${sensor?.name} accuracy=$accuracy")
        }
    }

    private fun buildMotionListener(type: SensorType): SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            val timestamp = System.currentTimeMillis()
            val reading = when (type) {
                SensorType.ACCELEROMETER -> AccelerometerReading(
                    x = event.values[0].toDouble(),
                    y = event.values[1].toDouble(),
                    z = event.values[2].toDouble(),
                    timestamp = timestamp,
                    eventTimestampNanos = event.timestamp,
                    samplePeriodUs = registeredSamplePeriodUs[SensorType.ACCELEROMETER]
                ).toSensorReading()
                SensorType.GYROSCOPE -> GyroscopeReading(
                    x = event.values[0].toDouble(),
                    y = event.values[1].toDouble(),
                    z = event.values[2].toDouble(),
                    timestamp = timestamp
                ).toSensorReading()
                SensorType.MAGNETOMETER -> {
                    val accuracy = magnetAccuracyBySensor[event.sensor.hashCode()]
                    logMagnetReading(event, accuracy)
                    MagnetometerReading(
                        x = event.values[0].toDouble(),
                        y = event.values[1].toDouble(),
                        z = event.values[2].toDouble(),
                        timestamp = timestamp,
                        eventTimestampNanos = event.timestamp,
                        accuracy = accuracy
                    ).toSensorReading()
                }
                else -> return
            }
            readingCallbacks[type]?.invoke(reading)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (sensor == null) return
            if (type == SensorType.MAGNETOMETER) {
                magnetAccuracyBySensor[sensor.hashCode()] = accuracy
                Log.i(
                    LOG_TAG,
                    "MagnetometroDiag accuracyChanged name=${sensor.name} accuracy=$accuracy " +
                        "(3=HIGH 2=MED 1=LOW 0=UNRELIABLE; si es baja, traza un 8 en el aire)"
                )
            }
        }
    }

    private fun logMagnetReading(event: SensorEvent, accuracy: Int?) {
        magnetDiagCount++
        if (magnetDiagCount > 8) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val intensity = kotlin.math.sqrt(x * x + y * y + z * z)
        Log.i(
            "MagnetometroDiag",
            "#$magnetDiagCount rawX=$x rawY=$y rawZ=$z " +
                "B=${"%.2f".format(intensity)}µT accuracy=${accuracy ?: "?"} " +
                "name=${event.sensor.name} type=${event.sensor.type} maxRange=${event.sensor.maximumRange}"
        )
    }

    private fun logLightReading(event: SensorEvent) {
        if (experimentalLightEventCount > 20) return
        Log.i(
            LUZ_TAG,
            "evento #$experimentalLightEventCount raw=${event.values[0]} " +
                "maxRange=${event.sensor.maximumRange} name=${event.sensor.name} type=${event.sensor.type}"
        )
    }

    private fun filterAccessibleLightSensors(candidates: List<Sensor>): List<Sensor> =
        candidates.filter { sensor ->
            when {
                sensor.type == SensorHardwareProbe.SAMSUNG_TCS3407_ALS_TYPE -> {
                    Log.w(LUZ_TAG, "Omitiendo ${sensor.name}: requiere permiso HRM_EXT")
                    false
                }
                // Evidencia SM-G780G: registerListener siempre false — no está en allowlist OEM.
                // Marcarlo "disponible" provoca SensorState.ERROR falso en Análisis de luz.
                sensor.type == SensorHardwareProbe.SAMSUNG_CAMERA_LIGHT_TYPE ||
                    sensor.name.contains("Camera Light", ignoreCase = true) -> {
                    Log.w(LUZ_TAG, "Omitiendo ${sensor.name}: bloqueado por allowlist OEM (terceros)")
                    false
                }
                else -> true
            }
        }

    private fun registerLightListener(listener: SensorEventListener, sensor: Sensor): Boolean {
        sensorManager.unregisterListener(listener)
        // Evidencia SM-G780G: SENSOR_DELAY_FASTEST (0us) hace que registerListener
        // devuelva false. Camera Light maxRate=5Hz → periodo válido ~200000us (NORMAL).
        val attempts = listOf(
            AndroidSensorManager.SENSOR_DELAY_NORMAL to "NORMAL/200000us",
            200_000 to "200000us",
            AndroidSensorManager.SENSOR_DELAY_UI to "UI/60000us",
            AndroidSensorManager.SENSOR_DELAY_GAME to "GAME/20000us"
        )
        for ((delayUs, label) in attempts) {
            Log.i(LUZ_TAG, "registerListener ANTES delay=$label sensor=${sensor.name} type=${sensor.type}")
            val ok = sensorManager.registerListener(listener, sensor, delayUs, mainHandler)
            Log.i(LUZ_TAG, "registerListener DESPUÉS ok=$ok delay=$label → ${sensor.name}")
            // #region agent log
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = "H-LUZ-REG",
                location = "MotionSensorProvider.registerLightListener",
                message = "Resultado registerListener",
                data = mapOf(
                    "ok" to ok,
                    "delayLabel" to label,
                    "delayUs" to delayUs,
                    "sensor" to sensor.name,
                    "type" to sensor.type
                ),
                runId = "post-fix"
            )
            // #endregion
            if (ok) {
                registeredSamplePeriodUs[SensorType.LIGHT] = delayUs
                return true
            }
        }
        Log.e(LUZ_TAG, "registerListener FALLÓ todos los delays → ${sensor.name}")
        return false
    }

    private fun registerWithBestStrategy(
        listener: SensorEventListener,
        sensor: Sensor,
        rate: SamplingRate,
        type: SensorType
    ): Boolean {
        val attempts = buildList {
            add(rate.toAndroidSensorDelay() to "RATE=${rate.name}")
            if (sensor.minDelay in 1..1_000_000) {
                add(sensor.minDelay to "minDelay=${sensor.minDelay}")
            }
            add(AndroidSensorManager.SENSOR_DELAY_GAME to "GAME")
            add(AndroidSensorManager.SENSOR_DELAY_UI to "UI")
            add(AndroidSensorManager.SENSOR_DELAY_NORMAL to "NORMAL")
        }.distinctBy { it.first }

        for ((delayUs, label) in attempts) {
            sensorManager.unregisterListener(listener)
            val ok = sensorManager.registerListener(listener, sensor, delayUs, mainHandler)
            if (ok) {
                registeredSamplePeriodUs[type] = delayUs
                Log.i(LOG_TAG, "registerListener OK $type ($label, ${delayUs}us) → ${sensor.name}")
                return true
            }
        }
        return false
    }

    /**
     * Re-registro periódico con el mismo delay válido (200000us). No usar FASTEST:
     * en SM-G780G provoca registerListener=false.
     */
    private fun scheduleLightNudge(sensor: Sensor, listener: SensorEventListener) {
        cancelLightNudge()
        val sampleUs = registeredSamplePeriodUs[SensorType.LIGHT]
            ?: AndroidSensorManager.SENSOR_DELAY_NORMAL
        val nudge = object : Runnable {
            override fun run() {
                if (activeLightListener !== listener) return
                if (readingCallbacks[SensorType.LIGHT] == null) return
                if (experimentalLightEventCount > 0) return
                sensorManager.unregisterListener(listener)
                val ok = sensorManager.registerListener(listener, sensor, sampleUs, mainHandler)
                if (!ok) {
                    Log.w(LUZ_TAG, "nudge: re-registro falló delayUs=$sampleUs")
                    return
                }
                if (experimentalLightEventCount > 0) return
                mainHandler.postDelayed(this, 400L)
            }
        }
        lightNudgeRunnable = nudge
        mainHandler.postDelayed(nudge, 300L)
    }

    private fun cancelLightNudge() {
        lightNudgeRunnable?.let { mainHandler.removeCallbacks(it) }
        lightNudgeRunnable = null
    }

    private fun scheduleLightFailover(sensor: Sensor, currentIndex: Int) {
        val watchdog = Runnable {
            if (experimentalLightEventCount == 0) {
                Log.w(
                    LUZ_TAG,
                    "Sin eventos en ${FAILOVER_MS}ms desde ${sensor.name}, siguiente candidato…"
                )
                // #region agent log
                com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                    hypothesisId = "H-LUZ-B",
                    location = "MotionSensorProvider.failover",
                    message = "Sin eventos luz — failover",
                    data = mapOf(
                        "sensor" to sensor.name,
                        "type" to sensor.type,
                        "nextIndex" to (currentIndex + 1),
                        "eventCount" to experimentalLightEventCount
                    )
                )
                // #endregion
                cancelLightNudge()
                tryLightCandidate(currentIndex + 1)
            }
        }
        lightFailoverWatchdog = watchdog
        mainHandler.postDelayed(watchdog, FAILOVER_MS)
    }

    private fun cancelLightFailover() {
        lightFailoverWatchdog?.let { mainHandler.removeCallbacks(it) }
        lightFailoverWatchdog = null
    }

    private fun lightResolution(): SensorHardwareProbe.LightSensorResolution? {
        if (lightResolution == null) {
            lightResolution = SensorHardwareProbe.resolveLightSensor(sensorManager)
        }
        return lightResolution
    }

    private companion object {
        const val LOG_TAG = "MotionSensorProvider"
        const val LUZ_TAG = "LuzDiag"
        const val FAILOVER_MS = 3_500L
        const val LIGHT_NUDGE_MS = 450L
    }
}
