package com.laboratoriofisico.app.sensores.provider

import android.Manifest
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.camera.view.PreviewView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.laboratoriofisico.app.debug.CameraTestActivity
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.CameraLensFacing
import com.laboratoriofisico.app.sensores.util.CompassCalculator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class CameraXSessionInstrumentedTest {

    @get:Rule
    val cameraPermission: GrantPermissionRule =
        GrantPermissionRule.grant(Manifest.permission.CAMERA)

    @Test
    fun prepare_succeedsWhenCameraExists() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val session = CameraXSession(context)
        assumeTrue("Dispositivo sin cámara", session.hasCameraHardware())

        val latch = CountDownLatch(1)
        val prepared = AtomicReference(false)
        instrumentation.runOnMainSync {
            session.prepare { ok ->
                prepared.set(ok)
                latch.countDown()
            }
        }
        assertTrue("Timeout preparando CameraX", latch.await(15, TimeUnit.SECONDS))
        assertTrue("CameraX no pudo prepararse", prepared.get())
        instrumentation.runOnMainSync { session.release() }
    }

    @Test
    fun imageAnalysis_receivesRealFrames() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val session = CameraXSession(context)
        assumeTrue(session.hasCameraHardware())

        val prepareLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            session.prepare { prepareLatch.countDown() }
        }
        assertTrue(prepareLatch.await(15, TimeUnit.SECONDS))

        val framesLatch = CountDownLatch(1)
        val lastState = AtomicReference<CameraHardwareState>()
        val lastWidth = AtomicReference<Int>()

        ActivityScenario.launch(CameraTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val previewView = PreviewView(activity)
                activity.setContentView(previewView)
                previewView.post {
                    session.bind(
                        lifecycleOwner = activity,
                        previewView = previewView,
                        facing = CameraLensFacing.BACK,
                        onReading = { reading ->
                            reading.valueX?.toInt()?.let { lastWidth.set(it) }
                            if (reading.valueX != null) framesLatch.countDown()
                        },
                        onStateChange = { state -> lastState.set(state) }
                    )
                }
            }

            assertTrue("Sin frames de ImageAnalysis", framesLatch.await(45, TimeUnit.SECONDS))
            assertTrue(lastWidth.get() != null && lastWidth.get()!! > 0)
            assertTrue(lastState.get() == CameraHardwareState.RUNNING)

            scenario.onActivity { session.unbind() }
        }
        instrumentation.runOnMainSync { session.release() }
    }

    @Test
    fun capture_createsReadableJpeg() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val session = CameraXSession(context)
        assumeTrue(session.hasCameraHardware())

        val prepareLatch = CountDownLatch(1)
        instrumentation.runOnMainSync {
            session.prepare { prepareLatch.countDown() }
        }
        assertTrue(prepareLatch.await(15, TimeUnit.SECONDS))

        val runningLatch = CountDownLatch(1)
        val captureLatch = CountDownLatch(1)
        val captureResult = AtomicReference<Result<String>>()

        ActivityScenario.launch(CameraTestActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val previewView = PreviewView(activity)
                activity.setContentView(previewView)
                previewView.post {
                    session.bind(
                        lifecycleOwner = activity,
                        previewView = previewView,
                        facing = CameraLensFacing.BACK,
                        onReading = { reading ->
                            if (reading.valueX != null) runningLatch.countDown()
                        },
                        onStateChange = { }
                    )
                }
            }

            assertTrue(runningLatch.await(45, TimeUnit.SECONDS))

            val output = File(context.cacheDir, "instrumented_capture_${System.currentTimeMillis()}.jpg")
            scenario.onActivity { activity ->
                session.capturePhoto(output) { result ->
                    captureResult.set(result)
                    captureLatch.countDown()
                }
            }
            assertTrue(captureLatch.await(15, TimeUnit.SECONDS))
            assertTrue(captureResult.get().isSuccess)
            assertTrue(output.exists() && output.length() > 1_000)
            val bitmap = BitmapFactory.decodeFile(output.absolutePath)
            assertNotNull(bitmap)
            assertTrue(bitmap.width > 0 && bitmap.height > 0)

            scenario.onActivity { session.unbind() }
        }
        instrumentation.runOnMainSync { session.release() }
    }
}

@RunWith(AndroidJUnit4::class)
class CompassCalculatorInstrumentedTest {

    @Test
    fun azimuth_computesOnDevice() {
        val calc = CompassCalculator()
        calc.updateAccelerometer(0.0, 0.0, 9.81)
        calc.updateMagnetometer(20.0, 0.0, 40.0)
        val azimuth = calc.azimuthDegrees()
        assertNotNull(azimuth)
        assertTrue(azimuth!! in 0.0..360.0)
    }
}

@RunWith(AndroidJUnit4::class)
class MotionSensorHardwareInstrumentedTest {

    @Test
    fun standardMotionSensors_reportAvailability() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sensorManager = context.getSystemService(SensorManager::class.java)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnet = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        assertNotNull("Sin acelerómetro en este dispositivo", accel)
        assertNotNull("Sin magnetómetro en este dispositivo", magnet)
        // Giroscopio puede faltar en gama baja; no fallar el test entero
        if (gyro == null) {
            println("NOTA: dispositivo sin giroscopio hardware")
        }
    }
}
