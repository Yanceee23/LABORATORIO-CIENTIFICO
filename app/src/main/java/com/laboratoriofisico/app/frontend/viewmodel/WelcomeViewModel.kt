package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WelcomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class WelcomeViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(WelcomeUiState())
    val uiState: StateFlow<WelcomeUiState> = _uiState.asStateFlow()

    fun enterAsGuest() {
        if (_uiState.value.isLoading) return
        _uiState.value = WelcomeUiState(isLoading = true)
        viewModelScope.launch {
            runCatching { authRepository.enterAsGuest() }
                .onFailure { error ->
                    _uiState.value = WelcomeUiState(error = error.message)
                }
                .onSuccess {
                    _uiState.value = WelcomeUiState()
                }
        }
    }
}
