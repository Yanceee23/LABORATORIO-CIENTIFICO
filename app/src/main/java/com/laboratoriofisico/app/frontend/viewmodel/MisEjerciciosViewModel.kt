package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.JuegoEjercicioProgreso
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.backend.repository.EjercicioResueltoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MisEjerciciosUiState(
    val filtroMateria: String? = null,
    val pendienteEliminar: EjercicioResuelto? = null,
    val error: String? = null
)

data class EjercicioConProgreso(
    val ejercicio: EjercicioResuelto,
    val estrellas: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class MisEjerciciosViewModel(
    private val repository: EjercicioResueltoRepository
) : ViewModel() {

    private val filtro = MutableStateFlow<String?>(null)
    private val _uiState = MutableStateFlow(MisEjerciciosUiState())
    val uiState: StateFlow<MisEjerciciosUiState> = _uiState.asStateFlow()

    val ejercicios: StateFlow<List<EjercicioConProgreso>> = combine(
        filtro.flatMapLatest { materia -> repository.observeMisEjercicios(materia) },
        repository.observeProgresoActual()
    ) { lista, progreso ->
        lista.map { ej ->
            EjercicioConProgreso(
                ejercicio = ej,
                estrellas = progreso[ej.id]?.estrellas ?: 0
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFiltro(materia: MateriaEjercicio?) {
        filtro.value = materia?.key
        _uiState.update { it.copy(filtroMateria = materia?.key) }
    }

    fun pedirEliminar(ejercicio: EjercicioResuelto) {
        _uiState.update { it.copy(pendienteEliminar = ejercicio) }
    }

    fun cancelarEliminar() {
        _uiState.update { it.copy(pendienteEliminar = null) }
    }

    fun confirmarEliminar() {
        val item = _uiState.value.pendienteEliminar ?: return
        viewModelScope.launch {
            runCatching { repository.deleteById(item.id) }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(error = e.message, pendienteEliminar = null)
                    }
                }
                .onSuccess {
                    _uiState.update { it.copy(pendienteEliminar = null) }
                }
        }
    }
}
