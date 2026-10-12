package com.laboratoriofisico.app.reportes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.reportes.model.ResultadoExportData
import java.io.File
import java.io.FileOutputStream

class ResultadoPdfGenerator {
    fun generar(data: ResultadoExportData, destino: File) {
        val document = PdfDocument()
        val pageWidth = PAGE_WIDTH
        val pageHeight = PAGE_HEIGHT
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN_TOP.toFloat()

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
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }

        fun newPageIfNeeded(extraHeight: Float): Canvas {
            if (y + extraHeight <= pageHeight - MARGIN_BOTTOM) return canvas
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page.canvas
            y = MARGIN_TOP.toFloat()
            return canvas
        }

        fun drawLine(text: String, paint: Paint = bodyPaint, indent: Float = 0f) {
            val maxWidth = pageWidth - MARGIN_HORIZONTAL * 2 - indent
            val lines = wrapText(text, paint, maxWidth)
            lines.forEach { line ->
                newPageIfNeeded(LINE_HEIGHT)
                canvas.drawText(line, MARGIN_HORIZONTAL + indent, y, paint)
                y += LINE_HEIGHT
            }
        }

        fun drawSection(title: String) {
            y += SECTION_GAP
            newPageIfNeeded(LINE_HEIGHT * 2)
            drawLine(title, sectionPaint)
            y += 4f
        }

        canvas.drawText("Laboratorio Científico", MARGIN_HORIZONTAL.toFloat(), y, titlePaint)
        y += LINE_HEIGHT + 4
        drawLine("Reporte de resultado individual")
        y += SECTION_GAP

        drawSection("Información general")
        drawLine("Experimento: ${data.experimento.titulo}")
        drawLine("Estudiante: ${data.usuarioNombre}")
        drawLine("Fecha y hora: ${DateTimeUtils.format(data.resultado.timestamp)}")
        drawLine("Estado del experimento: ${data.experimento.estado.name}")

        drawSection("Medición registrada")
        drawLine("Nombre: ${data.medicion.nombre}")
        drawLine("Valor medido: ${formatNumber(data.medicion.valor)} ${data.medicion.unidad}")
        drawLine("Fecha medición: ${DateTimeUtils.format(data.medicion.timestamp)}")
        data.sensor?.let { drawLine("Sensor: ${formatSensor(it)}") }

        drawSection("Resultado calculado")
        drawLine("Valor: ${formatNumber(data.resultado.valorCalculado)} ${data.resultado.unidad}")
        data.formulaId?.let { drawLine("Fórmula aplicada: ${formulaLabel(it)}") }

        val extras = buildList {
            data.valoresNumericos.forEach { (key, value) ->
                add("$key = ${formatNumber(value)}")
            }
            data.valoresTexto.forEach { (key, value) ->
                add("$key = $value")
            }
        }
        if (extras.isNotEmpty()) {
            drawSection("Valores adicionales")
            extras.forEach { drawLine("• $it") }
        }

        val chartValues = magnetismChartValues(data)
        if (chartValues.isNotEmpty()) {
            drawSection("Componentes magnéticas")
            y += drawSimpleBarChart(newPageIfNeeded(CHART_HEIGHT), MARGIN_HORIZONTAL.toFloat(), y, chartValues)
            y += CHART_HEIGHT
        } else {
            drawSection("Gráfica")
            drawLine(
                "No se incluyó gráfica de series temporales porque la app no guarda el historial " +
                    "completo del sensor en Room, solo los valores finales y metadatos."
            )
        }

        if (data.resultado.notas.isNotBlank() && data.resultado.notas != "medicion_sensor") {
            drawSection("Metadatos")
            drawLine(data.resultado.notas)
        }

        drawSection("Generado por")
        drawLine("Laboratorio Científico · ${DateTimeUtils.format(System.currentTimeMillis())}")

        document.finishPage(page)
        FileOutputStream(destino).use { output -> document.writeTo(output) }
        document.close()
    }

    private fun magnetismChartValues(data: ResultadoExportData): List<Pair<String, Double>> {
        val keys = listOf("Bx" to "Bx", "By" to "By", "Bz" to "Bz", "bx" to "Bx", "by" to "By", "bz" to "Bz")
        return keys.mapNotNull { (key, label) ->
            data.valoresNumericos[key]?.let { label to it }
        }.distinctBy { it.first }
    }

    private fun drawSimpleBarChart(
        canvas: Canvas,
        left: Float,
        top: Float,
        values: List<Pair<String, Double>>
    ): Float {
        if (values.isEmpty()) return 0f
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(0, 96, 100) }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            color = Color.BLACK
        }
        val chartTop = top + 8f
        val chartHeight = CHART_HEIGHT - 24f
        val maxValue = values.maxOf { kotlin.math.abs(it.second) }.coerceAtLeast(0.001)
        val barWidth = 48f
        val gap = 24f
        var x = left + 8f

        values.forEach { (label, value) ->
            val normalized = (kotlin.math.abs(value) / maxValue).toFloat().coerceIn(0.05f, 1f)
            val barHeight = chartHeight * normalized
            canvas.drawRect(x, chartTop + chartHeight - barHeight, x + barWidth, chartTop + chartHeight, barPaint)
            canvas.drawText(label, x, chartTop + chartHeight + 14f, textPaint)
            canvas.drawText(formatNumber(value), x, chartTop - 4f, textPaint)
            x += barWidth + gap
        }
        return CHART_HEIGHT
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
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

    private fun formatNumber(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else "%.4f".format(value).trimEnd('0').trimEnd('.')

    private fun formatSensor(sensor: String): String =
        sensor.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }

    private fun formulaLabel(formulaId: FormulaId): String = when (formulaId) {
        FormulaId.V_IGUAL_D_T -> "v = Δd / Δt"
        FormulaId.A_IGUAL_DELTA_V_DELTA_T -> "a = Δv / Δt"
        FormulaId.D_IGUAL_V_T -> "d = v · t"
        FormulaId.F_IGUAL_1_SOBRE_T -> "f = 1 / T"
        FormulaId.T_IGUAL_1_SOBRE_F -> "T = 1 / f"
        FormulaId.V_IGUAL_LAMBDA_F -> "v = λ · f"
        FormulaId.G_PENDULO -> "g = 4π²L / T²"
        FormulaId.B_INTENSIDAD -> "B = √(Bx² + By² + Bz²)"
        FormulaId.THETA_INCIDENCIA_REFLEXION -> "θ incidencia = θ reflexión"
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN_HORIZONTAL = 40
        private const val MARGIN_TOP = 48
        private const val MARGIN_BOTTOM = 48
        private const val LINE_HEIGHT = 18f
        private const val SECTION_GAP = 10f
        private const val CHART_HEIGHT = 120f
    }
}
