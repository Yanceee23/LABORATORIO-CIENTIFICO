package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.repository.ExperimentoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ExperimentoListUiState(
    val experimentos: List<Experimento> = emptyList(),
    val mostrarDialogoNuevo: Boolean = false,
    val tituloNuevo: String = "",
    val descripcionNueva: String = ""
)

class ExperimentoListViewModel(
    private val experimentoRepository: ExperimentoRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val dialogoState = MutableStateFlow(
        Triple(false, "", "")
    )

    val uiState: StateFlow<ExperimentoListUiState> = combine(
        experimentoRepository.observeAll(),
        dialogoState
    ) { experimentos, (mostrar, titulo, descripcion) ->
        ExperimentoListUiState(
            experimentos = experimentos,
            mostrarDialogoNuevo = mostrar,
            tituloNuevo = titulo,
            descripcionNueva = descripcion
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExperimentoListUiState()
    )

    fun mostrarDialogoNuevo(mostrar: Boolean) {
        dialogoState.value = if (mostrar) {
            Triple(true, dialogoState.value.second, dialogoState.value.third)
        } else {
            Triple(false, "", "")
        }
    }

    fun onTituloChange(titulo: String) {
        dialogoState.value = dialogoState.value.copy(second = titulo)
    }

    fun onDescripcionChange(descripcion: String) {
        dialogoState.value = dialogoState.value.copy(third = descripcion)
    }

    fun crearExperimento() {
        val titulo = dialogoState.value.second.trim()
        if (titulo.isEmpty()) return

        viewModelScope.launch {
            val usuarioId = sessionRepository.getLoggedInUserId() ?: return@launch

            experimentoRepository.insert(
                Experimento(
                    titulo = titulo,
                    descripcion = dialogoState.value.third.trim(),
                    usuarioId = usuarioId,
                    ubicacionId = null,
                    estado = EstadoExperimento.BORRADOR,
                    fechaCreacion = System.currentTimeMillis()
                )
            )
            dialogoState.value = Triple(false, "", "")
        }
    }

    fun eliminarExperimento(experimento: Experimento) {
        viewModelScope.launch {
            experimentoRepository.delete(experimento)
        }
    }
}
