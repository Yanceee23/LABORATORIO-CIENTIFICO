package com.laboratoriofisico.app.frontend.viewmodel

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.ResultadoBusqueda
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.pdf.PdfProcessingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PdfViewerTab { Pdf, Resultados }

data class PdfViewerUiState(
    val documento: Documento? = null,
    val paginaActual: Int = 0,
    val totalPaginas: Int = 0,
    val bitmap: Bitmap? = null,
    val cargando: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val buscando: Boolean = false,
    val busquedaRealizada: Boolean = false,
    val resultados: List<ResultadoBusqueda> = emptyList(),
    val tabActiva: PdfViewerTab = PdfViewerTab.Pdf
)

class PdfViewerViewModel(
    private val documentoId: Long,
    private val documentoRepository: DocumentoRepository,
    private val pdfProcessingService: PdfProcessingService
) : ViewModel() {
    private val _uiState = MutableStateFlow(PdfViewerUiState(cargando = true))
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val doc = documentoRepository.observeById(documentoId).filterNotNull().first()
            _uiState.update {
                it.copy(
                    documento = doc,
                    totalPaginas = doc.numeroPaginas.coerceAtLeast(1),
                    cargando = false
                )
            }
            renderPagina(0)
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, busquedaRealizada = false) }
    }

    fun seleccionarTab(tab: PdfViewerTab) {
        _uiState.update { it.copy(tabActiva = tab) }
    }

    fun buscar() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    resultados = emptyList(),
                    busquedaRealizada = false,
                    tabActiva = PdfViewerTab.Pdf
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(buscando = true, busquedaRealizada = false) }
            val resultados = pdfProcessingService.buscarTexto(documentoId, query)
            _uiState.update {
                it.copy(
                    buscando = false,
                    busquedaRealizada = true,
                    resultados = resultados,
                    tabActiva = PdfViewerTab.Resultados
                )
            }
            // #region agent log
            com.laboratoriofisico.app.core.debug.DebugSessionLog.log(
                hypothesisId = "H1",
                location = "PdfViewerViewModel.buscar",
                message = "Resultados búsqueda PDF",
                data = mapOf(
                    "query" to query,
                    "count" to resultados.size,
                    "sampleTitulos" to resultados.take(3).map { it.titulo }
                )
            )
            // #endregion
        }
    }

    fun irAPagina(numeroPagina: Int) {
        val paginaIndex = (numeroPagina - 1).coerceAtLeast(0)
        _uiState.update { it.copy(tabActiva = PdfViewerTab.Pdf, paginaActual = paginaIndex) }
        renderPagina(paginaIndex)
    }

    fun paginaAnterior() {
        val pagina = _uiState.value.paginaActual
        if (pagina > 0) {
            _uiState.update { it.copy(paginaActual = pagina - 1, tabActiva = PdfViewerTab.Pdf) }
            renderPagina(pagina - 1)
        }
    }

    fun paginaSiguiente() {
        val pagina = _uiState.value.paginaActual
        val total = _uiState.value.totalPaginas
        if (pagina < total - 1) {
            _uiState.update { it.copy(paginaActual = pagina + 1, tabActiva = PdfViewerTab.Pdf) }
            renderPagina(pagina + 1)
        }
    }

    private fun renderPagina(pagina: Int) {
        val doc = _uiState.value.documento ?: return
        if (doc.rutaArchivo.isBlank()) {
            _uiState.update {
                it.copy(
                    cargando = false,
                    error = "Documento demo sin archivo PDF. Importa un PDF real para ver el visor.",
                    bitmap = null
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cargando = true, error = null) }
            withContext(Dispatchers.IO) {
                runCatching {
                    val file = pdfProcessingService.abrirArchivo(doc)
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                        PdfRenderer(fd).use { renderer ->
                            if (renderer.pageCount == 0) error("PDF vacío")
                            val pageIndex = pagina.coerceIn(0, renderer.pageCount - 1)
                            renderer.openPage(pageIndex).use { page ->
                                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap
                            }
                        }
                    }
                }.onSuccess { bitmap ->
                    _uiState.update {
                        it.copy(bitmap = bitmap, cargando = false, totalPaginas = doc.numeroPaginas.coerceAtLeast(1))
                    }
                }.onFailure { e ->
                    _uiState.update { it.copy(cargando = false, error = e.message, bitmap = null) }
                }
            }
        }
    }
}
