package com.laboratoriofisico.app.reportes

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream

class ResultadosPeriodoPdfGenerator {
    fun generar(
        items: List<ResultadoPeriodoItem>,
        destino: File,
        estudiante: String,
        filtro: String? = null,
        generadoEn: String
    ) {
        val dias = ResultadosPeriodo.agrupar(items) { it.timestamp }
        val document = PdfDocument()
        var pageNumber = 1
        var page = start(document, pageNumber)
        var canvas = page.canvas
        var y = CONTENT_TOP

        val title = paint(22f, true, Color.WHITE)
        val subtitle = paint(11f, false, Color.rgb(214, 228, 232))
        val dayPaint = paint(13f, true, PETROL)
        val hourPaint = paint(11f, true, Color.rgb(61, 90, 110))
        val cardTitle = paint(12f, true, INK)
        val valuePaint = paint(18f, true, PETROL)
        val body = paint(11f, false, MUTED)
        val meta = paint(10f, false, MUTED)
        val footerPaint = paint(9f, false, MUTED)

        fun footer() {
            canvas.drawText(
                "Página $pageNumber  ·  Solo lo que guardaste en el teléfono",
                MARGIN,
                PAGE_HEIGHT - 28f,
                footerPaint
            )
        }

        fun newPage() {
            footer()
            document.finishPage(page)
            pageNumber++
            page = start(document, pageNumber)
            canvas = page.canvas
            y = CONTENT_TOP
            drawHeader(canvas, title, subtitle)
        }

        fun ensure(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN_BOTTOM) newPage()
        }

        drawHeader(canvas, title, subtitle)
        y = drawIntro(canvas, y, body, estudiante, items.size, dias.size, filtro, generadoEn)

        dias.forEach { dia ->
            ensure(86f)
            y = drawDayBanner(canvas, y, dayPaint, dia.etiqueta, dia.horas.sumOf { it.items.size })
            dia.horas.forEach { hora ->
                ensure(36f)
                canvas.drawText(hora.etiqueta, MARGIN, y, hourPaint)
                y += 16f
                hora.items.forEach { item ->
                    val height = cardHeight(item, body)
                    ensure(height + 8f)
                    y = drawCard(canvas, y, height, item, cardTitle, valuePaint, body, meta)
                }
            }
            y += 8f
        }

        footer()
        document.finishPage(page)
        FileOutputStream(destino).use { document.writeTo(it) }
        document.close()
    }

    private fun start(document: PdfDocument, number: Int): PdfDocument.Page =
        document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())

    private fun drawHeader(canvas: Canvas, title: Paint, subtitle: Paint) {
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 78f, Paint().apply { color = PETROL })
        canvas.drawText("Laboratorio Científico", MARGIN, 36f, title)
        canvas.drawText("Tus resultados, ordenados por día y por hora", MARGIN, 58f, subtitle)
    }

    private fun drawIntro(
        canvas: Canvas,
        startY: Float,
        body: Paint,
        estudiante: String,
        total: Int,
        dias: Int,
        filtro: String?,
        generadoEn: String
    ): Float {
        var y = startY
        val intro = buildString {
            append("Este PDF junta las mediciones que guardaste. ")
            append("Cada día va junto, y dentro del día las horas van en orden. ")
            append("Los números salen del teléfono. No hay ejemplos mezclados.")
        }
        wrap(intro, body, CONTENT_WIDTH).forEach { line ->
            canvas.drawText(line, MARGIN, y, body)
            y += 15f
        }
        y += 6f
        val resumen = "$total mediciones en $dias ${if (dias == 1) "día" else "días"}."
        canvas.drawText(resumen, MARGIN, y, body)
        y += 15f
        if (estudiante.isNotBlank()) {
            canvas.drawText("Cuenta: $estudiante", MARGIN, y, body)
            y += 15f
        }
        if (!filtro.isNullOrBlank()) {
            wrap("Solo entran los que coinciden con «$filtro».", body, CONTENT_WIDTH).forEach { line ->
                canvas.drawText(line, MARGIN, y, body)
                y += 15f
            }
        }
        canvas.drawText("Hecho el $generadoEn", MARGIN, y, body)
        return y + 22f
    }

    private fun drawDayBanner(canvas: Canvas, startY: Float, paint: Paint, etiqueta: String, cantidad: Int): Float {
        val rect = RectF(MARGIN - 4f, startY, PAGE_WIDTH - MARGIN + 4f, startY + 28f)
        canvas.drawRoundRect(rect, 8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = PETROL_SOFT })
        canvas.drawText(etiqueta, MARGIN + 8f, startY + 19f, paint)
        val count = if (cantidad == 1) "1 medición" else "$cantidad mediciones"
        val countPaint = paint(11f, false, PETROL)
        val width = countPaint.measureText(count)
        canvas.drawText(count, PAGE_WIDTH - MARGIN - width - 4f, startY + 19f, countPaint)
        return startY + 42f
    }

    private fun cardHeight(item: ResultadoPeriodoItem, body: Paint): Float {
        val lines = wrap(item.detalle, body, CONTENT_WIDTH - 28f).size.coerceAtLeast(1)
        return 78f + lines * 14f
    }

    private fun drawCard(
        canvas: Canvas,
        startY: Float,
        height: Float,
        item: ResultadoPeriodoItem,
        title: Paint,
        valuePaint: Paint,
        body: Paint,
        meta: Paint
    ): Float {
        val top = startY
        val rect = RectF(MARGIN, top, PAGE_WIDTH - MARGIN, top + height)
        canvas.drawRoundRect(rect, 10f, 10f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(248, 250, 250) })
        canvas.drawRoundRect(
            RectF(MARGIN, top, MARGIN + 5f, top + height),
            2f,
            2f,
            Paint().apply { color = PETROL }
        )
        canvas.drawText(fit(item.titulo, title, CONTENT_WIDTH - 28f), MARGIN + 16f, top + 20f, title)
        val valor = "${item.valor} ${item.unidad}".trim()
        canvas.drawText(valor, MARGIN + 16f, top + 42f, valuePaint)
        var textY = top + 62f
        wrap(item.detalle, body, CONTENT_WIDTH - 28f).forEach { line ->
            canvas.drawText(line, MARGIN + 16f, textY, body)
            textY += 14f
        }
        val hora = horaExacta(item.timestamp)
        canvas.drawText("Guardado a las $hora", MARGIN + 16f, top + height - 12f, meta)
        return top + height + 10f
    }

    private fun horaExacta(timestamp: Long): String {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        return "%02d:%02d".format(calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))
    }

    private fun fit(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var cut = text
        while (cut.length > 1 && paint.measureText("$cut…") > maxWidth) {
            cut = cut.dropLast(1)
        }
        return "$cut…"
    }

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        text.split(' ').forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
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

    private fun paint(size: Float, bold: Boolean, color: Int): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val CONTENT_TOP = 102f
        const val MARGIN_BOTTOM = 48f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
        val PETROL = Color.rgb(27, 58, 75)
        val PETROL_SOFT = Color.rgb(226, 238, 242)
        val INK = Color.rgb(44, 44, 44)
        val MUTED = Color.rgb(90, 104, 114)
    }
}
