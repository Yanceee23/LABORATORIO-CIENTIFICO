package com.laboratoriofisico.app.pdf

import android.net.Uri
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.ResultadoBusqueda
import com.laboratoriofisico.app.backend.model.SeccionDocumento
import java.io.File

interface PdfProcessingService {
    suspend fun importarPdf(
        uri: Uri,
        usuarioId: Long,
        onProgress: (ImportacionProgreso) -> Unit = {}
    ): Result<Documento>
    suspend fun extraerSecciones(documentoId: Long): List<SeccionDocumento>
    suspend fun buscarTexto(documentoId: Long, query: String): List<ResultadoBusqueda>
    fun abrirArchivo(documento: Documento): File
}
