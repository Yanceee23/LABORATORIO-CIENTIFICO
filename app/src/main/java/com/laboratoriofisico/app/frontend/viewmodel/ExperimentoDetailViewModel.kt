package com.laboratoriofisico.app.frontend.viewmodel



import android.net.Uri

import androidx.lifecycle.ViewModel

import androidx.lifecycle.viewModelScope

import com.laboratoriofisico.app.backend.model.EstadoExperimento

import com.laboratoriofisico.app.backend.model.Experimento

import com.laboratoriofisico.app.backend.model.Resultado

import com.laboratoriofisico.app.backend.model.Ubicacion

import com.laboratoriofisico.app.backend.repository.ExperimentoRepository

import com.laboratoriofisico.app.backend.repository.ResultadoRepository

import com.laboratoriofisico.app.backend.repository.UbicacionRepository

import com.laboratoriofisico.app.backend.session.SessionRepository

import com.laboratoriofisico.app.reportes.ExportResult

import com.laboratoriofisico.app.reportes.ReporteService

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.combine

import kotlinx.coroutines.flow.update

import kotlinx.coroutines.launch



data class ExperimentoDetailUiState(

    val experimento: Experimento? = null,

    val ubicacion: Ubicacion? = null,

    val titulo: String = "",

    val descripcion: String = "",

    val estado: EstadoExperimento = EstadoExperimento.BORRADOR,

    val ultimoResultado: Resultado? = null,

    val cargado: Boolean = false,

    val guardado: Boolean = false,

    val exportando: Boolean = false,

    val error: String? = null,

    val shareUri: Uri? = null,

    val shareFileName: String? = null

)



class ExperimentoDetailViewModel(

    private val experimentoId: Long,

    private val experimentoRepository: ExperimentoRepository,

    private val ubicacionRepository: UbicacionRepository,

    private val resultadoRepository: ResultadoRepository,

    private val reporteService: ReporteService,

    private val sessionRepository: SessionRepository

) : ViewModel() {



    private val _uiState = MutableStateFlow(ExperimentoDetailUiState())

    val uiState: StateFlow<ExperimentoDetailUiState> = _uiState.asStateFlow()



    init {

        viewModelScope.launch {

            combine(

                experimentoRepository.observeById(experimentoId),

                ubicacionRepository.observeAll(),

                resultadoRepository.observeLatestByExperimento(experimentoId)

            ) { experimento, ubicaciones, ultimoResultado ->

                val ubicacion = experimento?.ubicacionId?.let { id ->

                    ubicaciones.firstOrNull { it.id == id }

                }

                if (experimento != null) {

                    ExperimentoDetailUiState(

                        experimento = experimento,

                        ubicacion = ubicacion,

                        titulo = experimento.titulo,

                        descripcion = experimento.descripcion,

                        estado = experimento.estado,

                        ultimoResultado = ultimoResultado,

                        cargado = true

                    )

                } else {

                    ExperimentoDetailUiState(cargado = true)

                }

            }.collect { state ->

                if (!_uiState.value.guardado) {

                    _uiState.value = _uiState.value.copy(

                        experimento = state.experimento,

                        ubicacion = state.ubicacion,

                        titulo = state.titulo,

                        descripcion = state.descripcion,

                        estado = state.estado,

                        ultimoResultado = state.ultimoResultado,

                        cargado = state.cargado

                    )

                } else {

                    _uiState.value = _uiState.value.copy(

                        experimento = state.experimento,

                        ubicacion = state.ubicacion,

                        titulo = state.titulo,

                        descripcion = state.descripcion,

                        estado = state.estado,

                        ultimoResultado = state.ultimoResultado,

                        cargado = state.cargado,

                        guardado = false

                    )

                }

            }

        }

    }



    fun onTituloChange(titulo: String) {

        _uiState.value = _uiState.value.copy(titulo = titulo)

    }



    fun onDescripcionChange(descripcion: String) {

        _uiState.value = _uiState.value.copy(descripcion = descripcion)

    }



    fun onEstadoChange(estado: EstadoExperimento) {

        _uiState.value = _uiState.value.copy(estado = estado)

    }



    fun guardar() {

        val actual = _uiState.value.experimento ?: return

        viewModelScope.launch {

            experimentoRepository.update(

                actual.copy(

                    titulo = _uiState.value.titulo.trim(),

                    descripcion = _uiState.value.descripcion.trim(),

                    estado = _uiState.value.estado

                )

            )

            _uiState.value = _uiState.value.copy(guardado = true)

        }

    }



    fun exportarPdf() {

        if (_uiState.value.exportando) return

        viewModelScope.launch {

            _uiState.update { it.copy(exportando = true, error = null, shareUri = null) }

            val userId = sessionRepository.getLoggedInUserId()

            if (userId == null) {

                _uiState.update {

                    it.copy(exportando = false, error = "Inicia sesión para exportar reportes.")

                }

                return@launch

            }

            when (val result = reporteService.exportarExperimentoPdf(experimentoId, userId)) {

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


