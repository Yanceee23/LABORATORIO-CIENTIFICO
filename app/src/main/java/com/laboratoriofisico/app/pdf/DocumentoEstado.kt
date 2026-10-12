package com.laboratoriofisico.app.pdf

object DocumentoEstado {
    const val IMPORTANDO = "importando"
    const val PROCESANDO = "procesando"
    const val LISTO = "listo"
    const val SIN_TEXTO = "sin_texto"
    const val ERROR = "error"

    fun etiqueta(estado: String): String = when (estado) {
        IMPORTANDO -> "Importando…"
        PROCESANDO -> "Procesando…"
        LISTO -> "Listo"
        SIN_TEXTO -> "Sin texto extraíble"
        ERROR -> "Error"
        else -> estado.replaceFirstChar { it.uppercase() }
    }
}
