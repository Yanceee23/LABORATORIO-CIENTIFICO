package com.laboratoriofisico.app.reportes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.laboratoriofisico.app.core.util.DateTimeUtils
import java.io.File
import java.io.FileOutputStream

internal class PdfReportLayout(
    private val pageWidth: Int = PAGE_WIDTH,
    private val pageHeight: Int = PAGE_HEIGHT
) {
    private val document = PdfDocument()
    private var pageNumber = 1
    private var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
    private var canvas: Canvas = page.canvas
    private var y = MARGIN_TOP.toFloat()

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f
        color = Color.DKGRAY
    }

    fun drawTitle(text: String) {
        canvas.drawText(text, MARGIN_HORIZONTAL.toFloat(), y, titlePaint)
        y += LINE_HEIGHT + 4f
    }

    fun drawSection(title: String) {
        y += SECTION_GAP
        newPageIfNeeded(LINE_HEIGHT * 2)
        drawLine(title, sectionPaint)
        y += 4f
    }

    fun drawLine(text: String, paint: Paint = bodyPaint, indent: Float = 0f) {
        val maxWidth = pageWidth - MARGIN_HORIZONTAL * 2 - indent
        wrapText(text, paint, maxWidth).forEach { line ->
            newPageIfNeeded(LINE_HEIGHT)
            canvas.drawText(line, MARGIN_HORIZONTAL + indent, y, paint)
            y += LINE_HEIGHT
        }
    }

    fun gap() {
        y += SECTION_GAP
    }

    fun writeTo(destino: File) {
        document.finishPage(page)
        FileOutputStream(destino).use { output -> document.writeTo(output) }
        document.close()
    }

    private fun newPageIfNeeded(extraHeight: Float): Canvas {
        if (y + extraHeight <= pageHeight - MARGIN_BOTTOM) return canvas
        document.finishPage(page)
        pageNumber++
        page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page.canvas
        y = MARGIN_TOP.toFloat()
        return canvas
    }

    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN_HORIZONTAL = 40
        const val MARGIN_TOP = 48
        const val MARGIN_BOTTOM = 48
        const val LINE_HEIGHT = 18f
        const val SECTION_GAP = 10f

        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            if (text.isBlank()) return listOf("")
            val words = text.split(' ')
            val lines = mutableListOf<String>()
            var current = StringBuilder()
            words.forEach { word ->
                val candidate = if (current.isEmpty()) word else "${current} $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    current = StringBuilder(candidate)
                } else {
                    if (current.isNotEmpty()) lines.add(current.toString())
                    current = StringBuilder(word)
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
            return lines.ifEmpty { listOf(text) }
        }

        fun formatNumber(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else "%.4f".format(value).trimEnd('0').trimEnd('.')

        fun formatDuration(ms: Long): String = DateTimeUtils.formatDuration(ms)
    }
}
