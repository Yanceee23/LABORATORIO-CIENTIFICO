package com.laboratoriofisico.app.frontend.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.reportes.ExportResult
import com.laboratoriofisico.app.reportes.ReporteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultadosListUiState(
    val resultados: List<com.laboratoriofisico.app.backend.model.Resultado> = emptyList(),
    val query: String = "",
    val exportando: Boolean = false,
    val error: String? = null,
    val shareUri: Uri? = null,
    val shareFileName: String? = null
)

class ResultadosListViewModel(
    resultadoRepository: ResultadoRepository,
    private val reporteService: ReporteService,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _exportState = MutableStateFlow(ExportUiSlice())

    val uiState: StateFlow<ResultadosListUiState> = combine(
        resultadoRepository.observeAll(),
        query,
        _exportState
    ) { resultados, q, export ->
        val filtrados = if (q.isBlank()) resultados
        else resultados.filter {
            it.notas.contains(q, true) ||
                it.unidad.contains(q, true) ||
                "%.2f".format(it.valorCalculado).contains(q, true)
        }
        ResultadosListUiState(
            resultados = filtrados,
            query = q,
            exportando = export.exportando,
            error = export.error,
            shareUri = export.shareUri,
            shareFileName = export.shareFileName
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ResultadosListUiState()
    )

    fun onQueryChange(value: String) {
        query.value = value
        _exportState.update { it.copy(error = null) }
    }

    fun exportarPdf() {
        if (_exportState.value.exportando) return
        viewModelScope.launch {
            val visibleIds = uiState.value.resultados.map { it.id }
            if (visibleIds.isEmpty()) {
                _exportState.update {
                    it.copy(error = "Todavía no hay resultados para armar el PDF.")
                }
                return@launch
            }

            val userId = sessionRepository.getLoggedInUserId()
            if (userId == null) {
                _exportState.update {
                    it.copy(error = "Inicia sesión para crear el PDF.")
                }
                return@launch
            }

            _exportState.update { it.copy(exportando = true, error = null, shareUri = null) }
            val filtro = uiState.value.query
            when (val result = reporteService.exportarResultadosPeriodoPdf(userId, visibleIds, filtro)) {
                is ExportResult.Success -> _exportState.update {
                    it.copy(
                        exportando = false,
                        shareUri = result.uri,
                        shareFileName = result.fileName,
                        error = null
                    )
                }
                is ExportResult.Error -> _exportState.update {
                    it.copy(exportando = false, error = result.message)
                }
            }
        }
    }

    fun onShareHandled() {
        _exportState.update { it.copy(shareUri = null, shareFileName = null) }
    }

    fun onShareError(message: String) {
        _exportState.update { it.copy(error = message) }
    }

    private data class ExportUiSlice(
        val exportando: Boolean = false,
        val error: String? = null,
        val shareUri: Uri? = null,
        val shareFileName: String? = null
    )
}
