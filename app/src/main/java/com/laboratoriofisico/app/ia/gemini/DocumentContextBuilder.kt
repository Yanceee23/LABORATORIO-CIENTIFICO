package com.laboratoriofisico.app.ia.gemini

import com.laboratoriofisico.app.backend.model.SeccionDocumento

data class DocumentContext(
    val texto: String,
    val seccionesUsadas: List<SeccionReferencia>
)

data class SeccionReferencia(
    val titulo: String,
    val numeroPagina: Int
)

object DocumentContextBuilder {
    private const val MAX_SECCIONES = 12
    private const val MIN_KEYWORD_LENGTH = 3

    fun build(secciones: List<SeccionDocumento>, consulta: String? = null): Result<DocumentContext> {
        val conTexto = secciones
            .filter { it.contenido.isNotBlank() }
            .sortedBy { it.orden }

        if (conTexto.isEmpty()) {
            return Result.failure(DocumentoSinContenidoException())
        }

        val seleccionadas = if (consulta.isNullOrBlank()) {
            conTexto.take(MAX_SECCIONES)
        } else {
            seleccionarRelevantes(conTexto, consulta)
        }

        val referencias = mutableListOf<SeccionReferencia>()
        val builder = StringBuilder()
        var totalChars = 0

        for (seccion in seleccionadas) {
            val bloque = formatearSeccion(seccion)
            if (totalChars + bloque.length > GeminiConfig.MAX_CONTEXT_INPUT_CHARS) break
            builder.append(bloque)
            totalChars += bloque.length
            referencias += SeccionReferencia(
                titulo = seccion.titulo.ifBlank { "Sección ${seccion.orden + 1}" },
                numeroPagina = seccion.numeroPagina
            )
        }

        val texto = builder.toString().trim()
        if (texto.isBlank()) {
            return Result.failure(DocumentoSinContenidoException())
        }

        return Result.success(DocumentContext(texto = texto, seccionesUsadas = referencias))
    }

    private fun seleccionarRelevantes(
        secciones: List<SeccionDocumento>,
        consulta: String
    ): List<SeccionDocumento> {
        val keywords = consulta
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .distinct()

        if (keywords.isEmpty()) return secciones.take(MAX_SECCIONES)

        val puntuadas = secciones.map { seccion ->
            val texto = "${seccion.titulo} ${seccion.contenido}".lowercase()
            val score = keywords.sumOf { keyword ->
                Regex("\\b${Regex.escape(keyword)}\\b").findAll(texto).count()
            }
            seccion to score
        }

        val relevantes = puntuadas
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (seccion, _) -> seccion }

        return if (relevantes.isEmpty()) {
            secciones.take(MAX_SECCIONES)
        } else {
            (relevantes + secciones.filterNot { it in relevantes })
                .distinctBy { it.id }
                .take(MAX_SECCIONES)
        }
    }

    private fun formatearSeccion(seccion: SeccionDocumento): String =
        buildString {
            append("[Pág. ${seccion.numeroPagina}")
            if (seccion.titulo.isNotBlank()) append(" — ${seccion.titulo}")
            append("]\n")
            append(seccion.contenido.trim())
            append("\n\n")
        }
}
