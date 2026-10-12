package com.laboratoriofisico.app.pdf

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

data class PaginaTexto(
    val numeroPagina: Int,
    val contenido: String
)

class PdfTextExtractor(context: Context) {

    init {
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    fun extraerPorPagina(archivo: File): List<PaginaTexto> {
        PDDocument.load(archivo).use { document ->
            val totalPaginas = document.numberOfPages
            if (totalPaginas <= 0) return emptyList()

            val stripper = PDFTextStripper()
            return (1..totalPaginas).map { pagina ->
                stripper.startPage = pagina
                stripper.endPage = pagina
                PaginaTexto(
                    numeroPagina = pagina,
                    contenido = stripper.getText(document).trim()
                )
            }
        }
    }
}
