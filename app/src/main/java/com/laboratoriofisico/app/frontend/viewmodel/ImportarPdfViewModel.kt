package com.laboratoriofisico.app.frontend.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.pdf.ImportacionProgreso
import com.laboratoriofisico.app.pdf.PdfProcessingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportarPdfUiState(
    val procesando: Boolean = false,
    val progreso: Float = 0f,
    val mensaje: String? = null,
    val error: String? = null,
    val documentoImportadoId: Long? = null
)

class ImportarPdfViewModel(
    private val pdfProcessingService: PdfProcessingService,
    private val sessionRepository: SessionRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ImportarPdfUiState())
    val uiState: StateFlow<ImportarPdfUiState> = _uiState.asStateFlow()

    fun importar(uri: Uri) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    procesando = true,
                    progreso = 0.05f,
                    error = null,
                    mensaje = "Importando…",
                    documentoImportadoId = null
                )
            }
            val userId = sessionRepository.getLoggedInUserId() ?: run {
                _uiState.update { it.copy(procesando = false, error = "Sesión no válida") }
                return@launch
            }

            pdfProcessingService.importarPdf(uri, userId) { progreso ->
                _uiState.update { state ->
                    when (progreso) {
                        ImportacionProgreso.Importando -> state.copy(
                            progreso = 0.15f,
                            mensaje = "Importando…"
                        )
                        ImportacionProgreso.Procesando -> state.copy(
                            progreso = 0.35f,
                            mensaje = "Procesando…"
                        )
                        is ImportacionProgreso.ExtrayendoPagina -> {
                            val fraccion = 0.35f + (0.55f * progreso.pagina / progreso.totalPaginas.coerceAtLeast(1))
                            state.copy(
                                progreso = fraccion,
                                mensaje = "Extrayendo texto (${progreso.pagina}/${progreso.totalPaginas})…"
                            )
                        }
                        ImportacionProgreso.Listo -> state.copy(
                            progreso = 1f,
                            mensaje = "Listo"
                        )
                        is ImportacionProgreso.Error -> state.copy(
                            procesando = false,
                            progreso = 0f,
                            error = progreso.mensaje
                        )
                    }
                }
            }.onSuccess { doc ->
                _uiState.update {
                    it.copy(
                        procesando = false,
                        progreso = 1f,
                        mensaje = when (doc.estadoProcesamiento) {
                            "sin_texto" -> "Importado, pero no se pudo extraer texto"
                            "error" -> "Error al procesar el documento"
                            else -> "Documento listo: ${doc.nombre}"
                        },
                        documentoImportadoId = doc.id
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        procesando = false,
                        progreso = 0f,
                        error = e.message ?: "Error al importar"
                    )
                }
            }
        }
    }

    fun limpiarEstado() {
        _uiState.value = ImportarPdfUiState()
    }
}
