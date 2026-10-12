package com.laboratoriofisico.app.ia.gemini

import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.SeccionDocumentoRepository
import com.laboratoriofisico.app.pdf.DocumentoEstado

class DocumentoContextoHelper(
    private val seccionDocumentoRepository: SeccionDocumentoRepository,
    private val documentoRepository: DocumentoRepository
) {
    suspend fun cargarContexto(documentoId: Long, consulta: String? = null): DocumentContext {
        validarDocumento(documentoId)
        val secciones = seccionDocumentoRepository.getByDocumento(documentoId)
        return DocumentContextBuilder.build(secciones, consulta).getOrThrow()
    }

    private suspend fun validarDocumento(documentoId: Long) {
        val documento = documentoRepository.getById(documentoId)
            ?: throw IllegalArgumentException("No se encontró el documento seleccionado.")
        when (documento.estadoProcesamiento) {
            DocumentoEstado.SIN_TEXTO -> throw DocumentoSinContenidoException()
            DocumentoEstado.ERROR ->
                throw IllegalArgumentException("El documento tuvo un error al procesarse. Reimportalo.")
            DocumentoEstado.IMPORTANDO, DocumentoEstado.PROCESANDO ->
                throw IllegalArgumentException("El documento aún se está procesando. Esperá un momento.")
        }
    }
}
