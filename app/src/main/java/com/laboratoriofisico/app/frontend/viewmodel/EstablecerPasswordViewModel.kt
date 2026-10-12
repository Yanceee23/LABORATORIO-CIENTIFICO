package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.auth.AuthValidation
import com.laboratoriofisico.app.backend.repository.AuthRepository
import com.laboratoriofisico.app.backend.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstablecerPasswordUiState(
    val password: String = "",
    val confirmar: String = "",
    val passwordError: String? = null,
    val confirmarError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val exitoso: Boolean = false
)

class EstablecerPasswordViewModel(
    private val authRepository: AuthRepository,
    private val userId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstablecerPasswordUiState())
    val uiState: StateFlow<EstablecerPasswordUiState> = _uiState.asStateFlow()

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password, passwordError = null, generalError = null)
    }

    fun onConfirmarChange(confirmar: String) {
        _uiState.value = _uiState.value.copy(confirmar = confirmar, confirmarError = null, generalError = null)
    }

    fun guardar() {
        val state = _uiState.value
        if (state.isLoading) return
        val passwordError = AuthValidation.validatePassword(state.password)
        val confirmarError = AuthValidation.validateConfirmPassword(state.password, state.confirmar)
        if (passwordError != null || confirmarError != null) {
            _uiState.value = state.copy(passwordError = passwordError, confirmarError = confirmarError)
            return
        }
        _uiState.value = state.copy(isLoading = true, generalError = null)
        viewModelScope.launch {
            when (val result = authRepository.setNewPassword(userId, state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, exitoso = true)
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, generalError = result.message)
                }
                is AuthResult.NeedsPasswordReset -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = result.email
                    )
                }
            }
        }
    }
}
