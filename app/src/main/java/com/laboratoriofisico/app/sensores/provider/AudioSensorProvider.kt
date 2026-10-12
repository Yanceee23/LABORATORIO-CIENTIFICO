package com.laboratoriofisico.app.sensores.provider

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.laboratoriofisico.app.sensores.model.AudioReading
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.util.toAudioPollingDelayMs
import com.laboratoriofisico.app.sensores.util.toAudioSampleRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioSensorProvider {
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null

    fun isAvailable(): Boolean {
        val sampleRate = SamplingRate.NORMAL.toAudioSampleRate()
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        return bufferSize > 0
    }

    fun start(
        scope: CoroutineScope,
        rate: SamplingRate,
        onReading: (SensorReading) -> Unit
    ): Boolean {
        stop()
        val sampleRate = rate.toAudioSampleRate()
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (bufferSize <= 0) return false

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }

        audioRecord = record
        try {
            record.startRecording()
        } catch (exception: IllegalStateException) {
            record.release()
            audioRecord = null
            return false
        }
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            record.release()
            audioRecord = null
            return false
        }

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ShortArray(bufferSize)
            while (isActive) {
                val read = record.read(buffer, 0, buffer.size)
                if (read > 0) {
                    val amplitude = calculateRms(buffer, read)
                    val frequency = if (amplitude >= TONE_RMS_MIN) {
                        estimateZeroCrossingFrequencyHz(buffer, read, sampleRate)
                    } else {
                        0.0
                    }
                    val reading = AudioReading(
                        amplitude = amplitude,
                        sampleCount = read,
                        timestamp = System.currentTimeMillis()
                    ).toSensorReading()
                    onReading(
                        reading.copy(
                            valueY = frequency.takeIf { it > 0 },
                            metadata = "${reading.metadata};sample_rate_hz=$sampleRate;freq=${"%.1f".format(frequency)}"
                        )
                    )
                }
                delay(rate.toAudioPollingDelayMs())
            }
        }
        return true
    }

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        audioRecord?.let { record ->
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            record.release()
        }
        audioRecord = null
    }

    private fun calculateRms(buffer: ShortArray, read: Int): Double {
        if (read == 0) return 0.0
        var sum = 0.0
        for (i in 0 until read) {
            val normalized = buffer[i] / 32768.0
            sum += normalized * normalized
        }
        return sqrt(sum / read)
    }

    private fun estimateZeroCrossingFrequencyHz(buffer: ShortArray, read: Int, sampleRate: Int): Double {
        if (read < 2) return 0.0
        var crossings = 0
        for (i in 1 until read) {
            if ((buffer[i - 1] >= 0 && buffer[i] < 0) || (buffer[i - 1] < 0 && buffer[i] >= 0)) {
                crossings++
            }
        }
        val durationSec = read.toDouble() / sampleRate
        return if (durationSec > 0) crossings / (2.0 * durationSec) else 0.0
    }

    private companion object {
        /** Por debajo de esto el cruce por cero es ruido, no un tono. */
        const val TONE_RMS_MIN = 0.02
    }
}
