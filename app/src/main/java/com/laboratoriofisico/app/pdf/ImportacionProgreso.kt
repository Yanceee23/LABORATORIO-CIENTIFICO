package com.laboratoriofisico.app.pdf

sealed class ImportacionProgreso {
    data object Importando : ImportacionProgreso()
    data object Procesando : ImportacionProgreso()
    data class ExtrayendoPagina(val pagina: Int, val totalPaginas: Int) : ImportacionProgreso()
    data object Listo : ImportacionProgreso()
    data class Error(val mensaje: String) : ImportacionProgreso()
}
