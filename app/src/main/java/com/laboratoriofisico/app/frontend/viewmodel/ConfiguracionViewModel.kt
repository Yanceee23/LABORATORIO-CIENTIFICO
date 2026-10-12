package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.Configuracion
import com.laboratoriofisico.app.backend.repository.ConfiguracionRepository
import com.laboratoriofisico.app.configuracion.ConfiguracionDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ConfiguracionUiState(
    val configuracion: Configuracion? = null,
    val temaOscuro: Boolean = false,
    val idioma: String = "es",
    val unidadesMetricas: Boolean = true,
    val guardado: Boolean = false
)

class ConfiguracionViewModel(
    private val configuracionRepository: ConfiguracionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState: StateFlow<ConfiguracionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            configuracionRepository.observe().collect { config ->
                if (!_uiState.value.guardado) {
                    _uiState.value = ConfiguracionUiState(
                        configuracion = config,
                        temaOscuro = config.temaOscuro,
                        idioma = config.idioma,
                        unidadesMetricas = config.unidadesMetricas
                    )
                } else {
                    _uiState.value = _uiState.value.copy(configuracion = config, guardado = false)
                }
            }
        }
    }

    fun onTemaOscuroChange(temaOscuro: Boolean) {
        _uiState.value = _uiState.value.copy(temaOscuro = temaOscuro)
        val actual = _uiState.value.configuracion ?: ConfiguracionDefaults.crear()
        viewModelScope.launch {
            configuracionRepository.guardar(actual.copy(temaOscuro = temaOscuro))
        }
    }

    fun onIdiomaChange(idioma: String) {
        _uiState.value = _uiState.value.copy(idioma = idioma)
    }

    fun onUnidadesMetricasChange(unidadesMetricas: Boolean) {
        _uiState.value = _uiState.value.copy(unidadesMetricas = unidadesMetricas)
    }

    fun guardar() {
        val actual = _uiState.value.configuracion ?: return
        viewModelScope.launch {
            configuracionRepository.guardar(
                actual.copy(
                    temaOscuro = _uiState.value.temaOscuro,
                    idioma = _uiState.value.idioma,
                    unidadesMetricas = _uiState.value.unidadesMetricas
                )
            )
            _uiState.value = _uiState.value.copy(guardado = true)
        }
    }
}
