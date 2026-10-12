package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.ResultadoBusqueda
import com.laboratoriofisico.app.pdf.PdfProcessingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BuscarDocumentoUiState(
    val query: String = "",
    val buscando: Boolean = false,
    val busquedaRealizada: Boolean = false,
    val resultados: List<ResultadoBusqueda> = emptyList()
)

class BuscarDocumentoViewModel(
    private val documentoId: Long,
    private val pdfProcessingService: PdfProcessingService
) : ViewModel() {
    private val _uiState = MutableStateFlow(BuscarDocumentoUiState())
    val uiState: StateFlow<BuscarDocumentoUiState> = _uiState.asStateFlow()

    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query, busquedaRealizada = false) }
    }

    fun buscar() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(resultados = emptyList(), busquedaRealizada = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(buscando = true, busquedaRealizada = false) }
            val resultados = pdfProcessingService.buscarTexto(documentoId, query)
            _uiState.update {
                it.copy(
                    buscando = false,
                    busquedaRealizada = true,
                    resultados = resultados
                )
            }
        }
    }
}
