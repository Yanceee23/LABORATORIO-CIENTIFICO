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

data class RegistroUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val nombreError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmarPasswordError: String? = null,
    val generalError: String? = null,
    val isLoading: Boolean = false,
    val registroExitoso: Boolean = false
)

class RegistroViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroUiState())
    val uiState: StateFlow<RegistroUiState> = _uiState.asStateFlow()

    fun onNombreChange(nombre: String) {
        _uiState.value = _uiState.value.copy(nombre = nombre, nombreError = null, generalError = null)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, emailError = null, generalError = null)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            passwordError = null,
            confirmarPasswordError = null,
            generalError = null
        )
    }

    fun onConfirmarPasswordChange(confirmarPassword: String) {
        _uiState.value = _uiState.value.copy(
            confirmarPassword = confirmarPassword,
            confirmarPasswordError = null,
            generalError = null
        )
    }

    fun registrar() {
        val state = _uiState.value
        if (state.isLoading) return
        val nombreError = AuthValidation.validateNombre(state.nombre)
        val emailError = AuthValidation.validateEmail(state.email.trim())
        val passwordError = AuthValidation.validatePassword(state.password)
        val confirmarError = AuthValidation.validateConfirmPassword(state.password, state.confirmarPassword)

        if (nombreError != null || emailError != null || passwordError != null || confirmarError != null) {
            _uiState.value = state.copy(
                nombreError = nombreError,
                emailError = emailError,
                passwordError = passwordError,
                confirmarPasswordError = confirmarError
            )
            return
        }

        _uiState.value = state.copy(isLoading = true, generalError = null)
        viewModelScope.launch {
            when (
                val result = authRepository.register(
                    nombre = state.nombre,
                    email = state.email,
                    password = state.password
                )
            ) {
                is AuthResult.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, registroExitoso = true)
                }
                is AuthResult.Error -> {
                    val message = result.message
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        nombreError = message.takeIf {
                            it == "El nombre es obligatorio"
                        },
                        emailError = when {
                            message == AuthMessages.CORREO_YA_REGISTRADO -> message
                            message.contains("correo", ignoreCase = true) -> message
                            else -> null
                        },
                        passwordError = message.takeIf {
                            it.contains("contraseña", ignoreCase = true) &&
                                it != AuthMessages.CORREO_YA_REGISTRADO
                        },
                        confirmarPasswordError = message.takeIf {
                            it.contains("coinciden", ignoreCase = true) ||
                                it.contains("Confirma", ignoreCase = true)
                        },
                        generalError = message.takeIf { msg ->
                            msg != AuthMessages.CORREO_YA_REGISTRADO &&
                                !msg.contains("correo", ignoreCase = true) &&
                                !msg.contains("contraseña", ignoreCase = true) &&
                                !msg.contains("nombre", ignoreCase = true) &&
                                !msg.contains("coinciden", ignoreCase = true) &&
                                !msg.contains("Confirma", ignoreCase = true)
                        }
                    )
                }
                is AuthResult.NeedsPasswordReset -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        generalError = AuthMessages.CUENTA_SIN_CONTRASENA
                    )
                }
            }
        }
    }
}
