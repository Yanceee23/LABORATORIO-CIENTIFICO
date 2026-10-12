package com.laboratoriofisico.app.pdf

import com.laboratoriofisico.app.backend.local.entity.SeccionDocumentoEntity

object PdfSectionSplitter {

    private val headingRegex = Regex(
        """^(\d+(\.\d+)*[\.\)]?\s+.+|[A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ0-9\s\-:]{2,})$"""
    )

    fun dividirEnSecciones(documentoId: Long, paginas: List<PaginaTexto>): List<SeccionDocumentoEntity> {
        val paginasConTexto = paginas.filter { it.contenido.isNotBlank() }
        if (paginasConTexto.isEmpty()) return emptyList()

        val seccionesPorTitulo = mutableListOf<SeccionDocumentoEntity>()
        paginasConTexto.forEach { pagina ->
            val subsecciones = dividirPagina(documentoId, pagina)
            if (subsecciones.isEmpty()) {
                seccionesPorTitulo += seccionPorPagina(documentoId, pagina, seccionesPorTitulo.size + 1)
            } else {
                subsecciones.forEachIndexed { index, seccion ->
                    seccionesPorTitulo += seccion.copy(orden = seccionesPorTitulo.size + index + 1)
                }
            }
        }
        return seccionesPorTitulo
    }

    private fun dividirPagina(documentoId: Long, pagina: PaginaTexto): List<SeccionDocumentoEntity> {
        val lineas = pagina.contenido.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lineas.size < 2) return emptyList()

        val encabezados = lineas.withIndex().filter { (_, linea) -> esEncabezado(linea) }
        if (encabezados.size < 2) return emptyList()

        val secciones = mutableListOf<SeccionDocumentoEntity>()
        encabezados.forEachIndexed { index, (lineIndex, titulo) ->
            val fin = encabezados.getOrNull(index + 1)?.index ?: lineas.size
            val contenido = lineas.subList(lineIndex + 1, fin).joinToString("\n").trim()
            if (contenido.isNotBlank()) {
                secciones += SeccionDocumentoEntity(
                    documentoId = documentoId,
                    numeroPagina = pagina.numeroPagina,
                    titulo = titulo.take(120),
                    contenido = contenido,
                    orden = 0
                )
            }
        }
        return secciones
    }

    private fun seccionPorPagina(
        documentoId: Long,
        pagina: PaginaTexto,
        orden: Int
    ): SeccionDocumentoEntity {
        val primeraLinea = pagina.contenido.lineSequence().firstOrNull()?.trim().orEmpty()
        val titulo = if (primeraLinea.isNotBlank() && esEncabezado(primeraLinea)) {
            primeraLinea.take(120)
        } else {
            "Página ${pagina.numeroPagina}"
        }
        return SeccionDocumentoEntity(
            documentoId = documentoId,
            numeroPagina = pagina.numeroPagina,
            titulo = titulo,
            contenido = pagina.contenido,
            orden = orden
        )
    }

    private fun esEncabezado(linea: String): Boolean {
        if (linea.length > 80) return false
        return headingRegex.matches(linea)
    }
}
