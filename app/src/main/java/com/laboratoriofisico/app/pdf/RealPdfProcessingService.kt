package com.laboratoriofisico.app.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.laboratoriofisico.app.backend.mapper.toDomain
import com.laboratoriofisico.app.backend.mapper.toEntity
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.ResultadoBusqueda
import com.laboratoriofisico.app.backend.model.SeccionDocumento
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.SeccionDocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream

class RealPdfProcessingService(
    private val context: Context,
    private val documentoRepository: DocumentoRepository,
    private val seccionDocumentoRepository: SeccionDocumentoRepository
) : PdfProcessingService {

    private val textExtractor = PdfTextExtractor(context)

    override suspend fun importarPdf(
        uri: Uri,
        usuarioId: Long,
        onProgress: (ImportacionProgreso) -> Unit
    ): Result<Documento> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(ImportacionProgreso.Importando)

            val nombreArchivo = resolverNombreArchivo(uri)
            val nombre = nombreArchivo.removeSuffix(".pdf").removeSuffix(".PDF").replace('_', ' ')
            val dir = File(context.filesDir, "documentos").apply { mkdirs() }
            val destino = File(dir, "${System.currentTimeMillis()}_$nombreArchivo")

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destino).use { output -> input.copyTo(output) }
            } ?: error("No se pudo abrir el archivo seleccionado")

            val paginas = contarPaginas(destino)
            var documento = Documento(
                usuarioId = usuarioId,
                nombre = nombre,
                nombreArchivo = nombreArchivo,
                rutaArchivo = destino.absolutePath,
                extension = "pdf",
                tamano = destino.length(),
                numeroPaginas = paginas,
                fechaImportacion = System.currentTimeMillis(),
                estadoProcesamiento = DocumentoEstado.IMPORTANDO
            )
            val id = documentoRepository.insert(documento.toEntity())
            documento = documento.copy(id = id)

            documento = actualizarEstado(documento, DocumentoEstado.PROCESANDO)
            onProgress(ImportacionProgreso.Procesando)

            try {
                val paginasTexto = extraerTextoConProgreso(destino, paginas, onProgress)
                val secciones = PdfSectionSplitter.dividirEnSecciones(id, paginasTexto)

                documento = if (secciones.isEmpty()) {
                    actualizarEstado(documento, DocumentoEstado.SIN_TEXTO)
                } else {
                    seccionDocumentoRepository.insertAll(secciones)
                    actualizarEstado(documento, DocumentoEstado.LISTO)
                }
            } catch (processingError: Exception) {
                documento = actualizarEstado(documento, DocumentoEstado.ERROR)
                throw processingError
            }

            onProgress(ImportacionProgreso.Listo)
            documento
        }.onFailure { error ->
            onProgress(ImportacionProgreso.Error(error.message ?: "Error al importar"))
        }
    }

    override suspend fun extraerSecciones(documentoId: Long): List<SeccionDocumento> {
        return seccionDocumentoRepository.observeByDocumento(documentoId)
            .first()
            .map { it.toDomain() }
    }

    override suspend fun buscarTexto(documentoId: Long, query: String): List<ResultadoBusqueda> {
        if (query.isBlank()) return emptyList()
        val secciones = extraerSecciones(documentoId)
        return secciones.filter {
            it.contenido.contains(query, ignoreCase = true) ||
                it.titulo.contains(query, ignoreCase = true)
        }.map { seccion ->
            ResultadoBusqueda(
                seccionId = seccion.id,
                titulo = seccion.titulo,
                fragmento = resaltarFragmento(seccion.contenido, query),
                numeroPagina = seccion.numeroPagina
            )
        }
    }

    override fun abrirArchivo(documento: Documento): File = File(documento.rutaArchivo)

    private suspend fun extraerTextoConProgreso(
        archivo: File,
        totalPaginas: Int,
        onProgress: (ImportacionProgreso) -> Unit
    ): List<PaginaTexto> {
        val paginas = mutableListOf<PaginaTexto>()
        textExtractor.extraerPorPagina(archivo).forEach { pagina ->
            paginas += pagina
            onProgress(ImportacionProgreso.ExtrayendoPagina(pagina.numeroPagina, totalPaginas))
            yield()
        }
        return paginas
    }

    private suspend fun actualizarEstado(documento: Documento, estado: String): Documento {
        val actualizado = documento.copy(estadoProcesamiento = estado)
        documentoRepository.update(actualizado)
        return actualizado
    }

    private fun resolverNombreArchivo(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)?.takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "documento.pdf"
    }

    private fun contarPaginas(file: File): Int = runCatching {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { it.pageCount }
        }
    }.getOrDefault(1)

    private fun resaltarFragmento(texto: String, query: String): String {
        val idx = texto.indexOf(query, ignoreCase = true)
        if (idx < 0) return texto.take(120)
        val start = (idx - 30).coerceAtLeast(0)
        val end = (idx + query.length + 30).coerceAtMost(texto.length)
        return "...${texto.substring(start, end)}..."
    }
}
