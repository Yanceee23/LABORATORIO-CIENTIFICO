package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.Usuario
import com.laboratoriofisico.app.backend.repository.AuthRepository
import com.laboratoriofisico.app.backend.repository.UsuarioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UsuarioUiState(
    val usuario: Usuario? = null,
    val nombre: String = "",
    val email: String = "",
    val rol: String = "",
    val guardado: Boolean = false,
    val logoutCompletado: Boolean = false
)

class UsuarioViewModel(
    private val usuarioRepository: UsuarioRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(UsuarioUiState())
    val uiState: StateFlow<UsuarioUiState> = _uiState.asStateFlow()

    val usuario: StateFlow<Usuario?> = usuarioRepository.observeSesion()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        viewModelScope.launch {
            usuario.collect { u ->
                if (u != null && !_uiState.value.guardado) {
                    _uiState.value = UsuarioUiState(
                        usuario = u,
                        nombre = u.nombre,
                        email = u.email,
                        rol = u.rol
                    )
                } else if (u != null && _uiState.value.guardado) {
                    _uiState.value = _uiState.value.copy(usuario = u, guardado = false)
                }
            }
        }
    }

    fun onNombreChange(nombre: String) {
        _uiState.value = _uiState.value.copy(nombre = nombre)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onRolChange(rol: String) {
        _uiState.value = _uiState.value.copy(rol = rol)
    }

    fun guardar() {
        val actual = _uiState.value.usuario ?: return
        viewModelScope.launch {
            usuarioRepository.update(
                actual.copy(
                    nombre = _uiState.value.nombre.trim(),
                    email = _uiState.value.email.trim(),
                    rol = _uiState.value.rol.trim()
                )
            )
            _uiState.value = _uiState.value.copy(guardado = true)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = _uiState.value.copy(logoutCompletado = true)
        }
    }
}
