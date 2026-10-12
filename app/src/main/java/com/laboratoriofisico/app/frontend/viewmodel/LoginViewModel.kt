package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.auth.AuthMessages
import com.laboratoriofisico.app.backend.auth.AuthValidation
import com.laboratoriofisico.app.backend.repository.AuthRepository
import com.laboratoriofisico.app.backend.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val loginExitoso: Boolean = false,
    val needsPasswordReset: Boolean = false,
    val resetUserId: Long? = null,
    val resetEmail: String? = null
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = null,
            generalError = null
        )
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            generalError = null
        )
    }

    fun clearPasswordReset() {
        _uiState.value = _uiState.value.copy(
            needsPasswordReset = false,
            resetUserId = null,
            resetEmail = null
        )
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return
        val emailError = AuthValidation.validateEmail(state.email.trim())
        val passwordError = if (state.password.isBlank()) "La contraseña es obligatoria" else null

        if (emailError != null || passwordError != null) {
            _uiState.value = state.copy(
                emailError = emailError,
                passwordError = passwordError
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, generalError = null)
        viewModelScope.launch {
            when (val result = authRepository.login(state.email, state.password)) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, loginExitoso = true)
                }
                is AuthResult.NeedsPasswordReset -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        needsPasswordReset = true,
                        resetUserId = result.userId,
                        resetEmail = result.email,
                        generalError = AuthMessages.CUENTA_SIN_CONTRASENA
                    )
                }
                is AuthResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = result.message,
                        emailError = null,
                        passwordError = null
                    )
                }
            }
        }
    }
}
