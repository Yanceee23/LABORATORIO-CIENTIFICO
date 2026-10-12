package com.laboratoriofisico.app.frontend.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.Resultado
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.reportes.ExportResult
import com.laboratoriofisico.app.reportes.ReporteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResultadoDetailUiState(
    val exportando: Boolean = false,
    val error: String? = null,
    val shareUri: Uri? = null,
    val shareFileName: String? = null
)

class ResultadoDetailViewModel(
    private val resultadoId: Long,
    resultadoRepository: ResultadoRepository,
    private val reporteService: ReporteService,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    val resultado: StateFlow<Resultado?> = resultadoRepository.observeById(resultadoId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    private val _uiState = MutableStateFlow(ResultadoDetailUiState())
    val uiState: StateFlow<ResultadoDetailUiState> = _uiState.asStateFlow()

    fun exportarPdf() {
        if (_uiState.value.exportando) return
        viewModelScope.launch {
            _uiState.update { it.copy(exportando = true, error = null, shareUri = null) }
            val userId = sessionRepository.getLoggedInUserId()
            if (userId == null) {
                _uiState.update {
                    it.copy(exportando = false, error = "Inicia sesión para exportar resultados.")
                }
                return@launch
            }
            when (val result = reporteService.exportarResultadoPdf(resultadoId, userId)) {
                is ExportResult.Success -> _uiState.update {
                    it.copy(
                        exportando = false,
                        shareUri = result.uri,
                        shareFileName = result.fileName,
                        error = null
                    )
                }
                is ExportResult.Error -> _uiState.update {
                    it.copy(exportando = false, error = result.message)
                }
            }
        }
    }

    fun onShareHandled() {
        _uiState.update { it.copy(shareUri = null, shareFileName = null) }
    }

    fun onShareError(message: String) {
        _uiState.update { it.copy(error = message) }
    }
}
