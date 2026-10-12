package com.laboratoriofisico.app.sensores.provider

import androidx.camera.core.ImageProxy
import com.laboratoriofisico.app.sensores.util.LumaFrame
import kotlin.math.max

/**
 * Convierte el plano Y de un frame real de CameraX en un [LumaFrame] reducido y ya rotado
 * a la orientación en que el usuario ve el preview, para que un toque en pantalla
 * corresponda al mismo punto que analiza el rastreador.
 *
 * No copia el frame completo: muestrea con paso fijo hasta un ancho objetivo pequeño,
 * suficiente para el seguimiento y barato de procesar en tiempo real.
 */
object LumaFrameExtractor {

    private const val TARGET_WIDTH = 160

    fun extract(image: ImageProxy, timestampMs: Long): LumaFrame? {
        val plane = image.planes.firstOrNull() ?: return null
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val srcWidth = image.width
        val srcHeight = image.height
        if (srcWidth <= 0 || srcHeight <= 0) return null

        val step = max(1, srcWidth / TARGET_WIDTH)
        val gridWidth = srcWidth / step
        val gridHeight = srcHeight / step
        if (gridWidth <= 0 || gridHeight <= 0) return null

        buffer.rewind()
        val grid = IntArray(gridWidth * gridHeight)
        for (gy in 0 until gridHeight) {
            val rowOffset = (gy * step) * rowStride
            val outRow = gy * gridWidth
            for (gx in 0 until gridWidth) {
                val index = rowOffset + (gx * step) * pixelStride
                grid[outRow + gx] = if (index < buffer.limit()) {
                    buffer.get(index).toInt() and 0xFF
                } else {
                    0
                }
            }
        }

        return rotate(
            grid = grid,
            gridWidth = gridWidth,
            gridHeight = gridHeight,
            srcWidth = srcWidth,
            srcHeight = srcHeight,
            rotationDegrees = image.imageInfo.rotationDegrees,
            timestampMs = timestampMs
        )
    }

    private fun rotate(
        grid: IntArray,
        gridWidth: Int,
        gridHeight: Int,
        srcWidth: Int,
        srcHeight: Int,
        rotationDegrees: Int,
        timestampMs: Long
    ): LumaFrame {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        if (normalized == 0) {
            return LumaFrame(grid, gridWidth, gridHeight, srcWidth, srcHeight, timestampMs)
        }

        val swapped = normalized == 90 || normalized == 270
        val outWidth = if (swapped) gridHeight else gridWidth
        val outHeight = if (swapped) gridWidth else gridHeight
        val out = IntArray(outWidth * outHeight)

        for (y in 0 until outHeight) {
            for (x in 0 until outWidth) {
                val sourceIndex = when (normalized) {
                    90 -> (gridHeight - 1 - x) * gridWidth + y
                    180 -> (gridHeight - 1 - y) * gridWidth + (gridWidth - 1 - x)
                    else -> x * gridWidth + (gridWidth - 1 - y)
                }
                out[y * outWidth + x] = grid[sourceIndex]
            }
        }

        return LumaFrame(
            luma = out,
            width = outWidth,
            height = outHeight,
            sourceWidth = if (swapped) srcHeight else srcWidth,
            sourceHeight = if (swapped) srcWidth else srcHeight,
            timestampMs = timestampMs
        )
    }
}
