package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.model.Experimento
import com.laboratoriofisico.app.backend.model.ResumenDashboard
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.ExperimentoRepository
import com.laboratoriofisico.app.backend.repository.UsuarioRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val nombreUsuario: String = "",
    val resumen: ResumenDashboard = ResumenDashboard(0, 0, 0),
    val experimentosRecientes: List<Experimento> = emptyList(),
    val documentosRecientes: List<Documento> = emptyList(),
    val progresoEstudio: Float = 0f
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    experimentoRepository: ExperimentoRepository,
    documentoRepository: DocumentoRepository,
    usuarioRepository: UsuarioRepository,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val documentosRecientes = sessionRepository.sessionUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList())
        else documentoRepository.observeByUsuario(userId)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        usuarioRepository.observeSesion(),
        experimentoRepository.observeResumen(),
        experimentoRepository.observeAll(),
        documentosRecientes
    ) { usuario, resumen, experimentos, documentos ->
        HomeUiState(
            nombreUsuario = usuario?.nombre ?: "Estudiante",
            resumen = resumen,
            experimentosRecientes = experimentos.take(3),
            documentosRecientes = documentos.take(3),
            progresoEstudio = 0f
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}
