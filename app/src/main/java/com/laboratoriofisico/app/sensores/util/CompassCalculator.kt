package com.laboratoriofisico.app.sensores.util



import android.hardware.SensorManager



/**

 * Calcula azimuth fusionando acelerómetro + magnetómetro ([SensorManager.getRotationMatrix]

 * + [SensorManager.getOrientation]) con filtro low-pass para estabilizar la aguja.

 *

 * El remapeo AXIS_X / AXIS_Z asume el teléfono en vertical (pantalla hacia el usuario),

 * típico al caminar con la app abierta. Si [getRotationMatrix] falla (interferencia fuerte

 * de un imán cercano), se conserva el último azimut filtrado y [rotationMatrixValid] es false.

 */

class CompassCalculator {

    private val gravity = FloatArray(3)

    private val geomagnetic = FloatArray(3)

    private val rotationMatrix = FloatArray(9)

    private val remappedMatrix = FloatArray(9)

    private val orientation = FloatArray(3)



    private var hasGravity = false

    private var hasGeomagnetic = false

    private var filteredAzimuth: Double? = null

    private var lastRotationValid = false



    fun updateAccelerometer(x: Double, y: Double, z: Double) {

        gravity[0] = lowPass(x.toFloat(), gravity[0], hasGravity)

        gravity[1] = lowPass(y.toFloat(), gravity[1], hasGravity)

        gravity[2] = lowPass(z.toFloat(), gravity[2], hasGravity)

        hasGravity = true

    }



    fun updateMagnetometer(x: Double, y: Double, z: Double) {

        geomagnetic[0] = lowPass(x.toFloat(), geomagnetic[0], hasGeomagnetic)

        geomagnetic[1] = lowPass(y.toFloat(), geomagnetic[1], hasGeomagnetic)

        geomagnetic[2] = lowPass(z.toFloat(), geomagnetic[2], hasGeomagnetic)

        hasGeomagnetic = true

    }



    /** Último azimut filtrado en grados [0, 360), o null si aún no hay datos suficientes. */

    fun azimuthDegrees(): Double? {

        if (!hasGravity || !hasGeomagnetic) return filteredAzimuth



        lastRotationValid = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)

        if (!lastRotationValid) {

            return filteredAzimuth

        }



        SensorManager.remapCoordinateSystem(

            rotationMatrix,

            SensorManager.AXIS_X,

            SensorManager.AXIS_Z,

            remappedMatrix

        )

        SensorManager.getOrientation(remappedMatrix, orientation)



        var azimuth = Math.toDegrees(orientation[0].toDouble())

        if (azimuth < 0) azimuth += 360.0



        filteredAzimuth = filteredAzimuth?.let { smoothAzimuth(it, azimuth) } ?: azimuth

        return filteredAzimuth

    }



    fun rotationMatrixValid(): Boolean = lastRotationValid



    fun reset() {

        hasGravity = false

        hasGeomagnetic = false

        filteredAzimuth = null

        lastRotationValid = false

        gravity.fill(0f)

        geomagnetic.fill(0f)

    }



    private fun lowPass(input: Float, output: Float, hasPrevious: Boolean): Float =

        if (!hasPrevious) input else SENSOR_ALPHA * input + (1f - SENSOR_ALPHA) * output



    private fun smoothAzimuth(previous: Double, current: Double): Double {

        var delta = current - previous

        if (delta > 180) delta -= 360

        if (delta < -180) delta += 360

        var result = previous + AZIMUTH_ALPHA * delta

        if (result < 0) result += 360

        if (result >= 360) result -= 360

        return result

    }



    private companion object {

        const val SENSOR_ALPHA = 0.15f

        const val AZIMUTH_ALPHA = 0.08

    }

}


