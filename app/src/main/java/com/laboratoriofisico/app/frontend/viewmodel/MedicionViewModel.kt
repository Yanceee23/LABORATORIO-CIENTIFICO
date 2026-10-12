package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.MedicionConResultados
import com.laboratoriofisico.app.backend.repository.MedicionRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MedicionUiState(
    val mediciones: List<MedicionConResultados> = emptyList(),
    val mostrarDialogoNuevo: Boolean = false,
    val nombreNuevo: String = "",
    val valorNuevo: String = "",
    val unidadNueva: String = "m",
    val error: String? = null
)

class MedicionViewModel(
    private val experimentoId: Long,
    private val medicionRepository: MedicionRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MedicionUiState())
    val uiState: StateFlow<MedicionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            medicionRepository.observeConResultados(experimentoId).collect { lista ->
                _uiState.value = _uiState.value.copy(mediciones = lista)
            }
        }
    }

    fun mostrarDialogoNuevo(mostrar: Boolean) {
        _uiState.value = _uiState.value.copy(mostrarDialogoNuevo = mostrar, error = null)
    }

    fun onNombreChange(nombre: String) {
        _uiState.value = _uiState.value.copy(nombreNuevo = nombre)
    }

    fun onValorChange(valor: String) {
        _uiState.value = _uiState.value.copy(valorNuevo = valor)
    }

    fun onUnidadChange(unidad: String) {
        _uiState.value = _uiState.value.copy(unidadNueva = unidad)
    }

    fun crearMedicion() {
        val nombre = _uiState.value.nombreNuevo.trim()
        val valor = _uiState.value.valorNuevo.toDoubleOrNull()
        if (nombre.isEmpty() || valor == null) return

        viewModelScope.launch {
            val usuarioId = sessionRepository.getLoggedInUserId()
            if (usuarioId == null) {
                _uiState.value = _uiState.value.copy(error = AuthMessages.INVITADO_BLOQUEADO)
                return@launch
            }
            medicionRepository.insert(
                Medicion(
                    experimentoId = experimentoId,
                    usuarioId = usuarioId,
                    nombre = nombre,
                    valor = valor,
                    unidad = _uiState.value.unidadNueva.trim().ifEmpty { "u" },
                    timestamp = System.currentTimeMillis()
                )
            )
            _uiState.value = _uiState.value.copy(
                mostrarDialogoNuevo = false,
                nombreNuevo = "",
                valorNuevo = "",
                unidadNueva = "m",
                error = null
            )
        }
    }

    fun eliminarMedicion(medicion: Medicion) {
        viewModelScope.launch {
            medicionRepository.delete(medicion)
        }
    }
}
