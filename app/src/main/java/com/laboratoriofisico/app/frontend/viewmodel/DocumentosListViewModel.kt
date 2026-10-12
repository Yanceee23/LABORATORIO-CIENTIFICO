package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentosListViewModel(
    private val documentoRepository: DocumentoRepository,
    sessionRepository: SessionRepository
) : ViewModel() {
    val documentos: StateFlow<List<Documento>> = sessionRepository.sessionUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList())
            else documentoRepository.observeByUsuario(userId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun eliminar(documento: Documento) {
        viewModelScope.launch {
            documentoRepository.delete(documento)
        }
    }
}
