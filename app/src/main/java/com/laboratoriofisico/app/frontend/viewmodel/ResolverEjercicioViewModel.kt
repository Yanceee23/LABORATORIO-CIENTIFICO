package com.laboratoriofisico.app.frontend.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.tutor.ejercicio.ResolverEjercicioOutcome
import com.laboratoriofisico.app.tutor.ejercicio.ResolverEjercicioService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class ResolverFase {
    CAPTURA,
    ENUNCIADO,
    SOLUCION
}

data class ResolverEjercicioUiState(
    val fase: ResolverFase = ResolverFase.CAPTURA,
    val fotoPath: String? = null,
    val enunciadoEditable: String = "",
    val ejercicio: EjercicioResuelto? = null,
    val pasosVisibles: Int = 1,
    val analizando: Boolean = false,
    val error: String? = null,
    val canRetry: Boolean = false,
    val avisoVerificacion: String? = null
)

class ResolverEjercicioViewModel(
    private val appContext: Context,
    private val service: ResolverEjercicioService,
    private val ejercicioIdInicial: Long? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResolverEjercicioUiState())
    val uiState: StateFlow<ResolverEjercicioUiState> = _uiState.asStateFlow()

    init {
        // Carga desde Room solo si hay id; nunca dispara Gemini.
        if (ejercicioIdInicial != null && ejercicioIdInicial > 0L) {
            viewModelScope.launch {
                when (val outcome = service.cargarGuardado(ejercicioIdInicial)) {
                    is ResolverEjercicioOutcome.Success -> mostrarSolucion(outcome.ejercicio)
                    is ResolverEjercicioOutcome.Error -> {
                        _uiState.update {
                            it.copy(error = outcome.message, canRetry = false)
                        }
                    }
                    ResolverEjercicioOutcome.Busy -> Unit
                }
            }
        }
    }

    fun onFotoCapturada(path: String) {
        if (_uiState.value.analizando) return
        _uiState.update {
            it.copy(
                fotoPath = path,
                fase = ResolverFase.CAPTURA,
                error = null,
                ejercicio = null
            )
        }
    }

    fun onFotoDesdeUri(uri: Uri) {
        if (_uiState.value.analizando) return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { copiarUriAArchivo(uri) }
            if (path == null) {
                _uiState.update {
                    it.copy(error = "No se pudo abrir la imagen.", canRetry = true)
                }
            } else {
                onFotoCapturada(path)
            }
        }
    }

    fun guardarRecorte(bitmap: Bitmap) {
        if (_uiState.value.analizando) return
        viewModelScope.launch {
            val path = withContext(Dispatchers.IO) { guardarBitmap(bitmap) }
            _uiState.update {
                it.copy(fotoPath = path, error = null)
            }
        }
    }

    /** Única acción del usuario que dispara Gemini con foto. */
    fun analizarFoto() {
        val path = _uiState.value.fotoPath ?: return
        if (_uiState.value.analizando) return
        _uiState.update { it.copy(analizando = true, error = null, canRetry = false) }
        viewModelScope.launch {
            when (val outcome = service.resolverDesdeFoto(path)) {
                is ResolverEjercicioOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            analizando = false,
                            fase = ResolverFase.ENUNCIADO,
                            enunciadoEditable = outcome.ejercicio.enunciado,
                            ejercicio = outcome.ejercicio,
                            avisoVerificacion = avisoDe(outcome.ejercicio.verificacion)
                        )
                    }
                }
                is ResolverEjercicioOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            analizando = false,
                            error = outcome.message,
                            canRetry = outcome.canRetry
                        )
                    }
                }
                ResolverEjercicioOutcome.Busy -> {
                    _uiState.update {
                        it.copy(
                            analizando = false,
                            error = "Ya hay un análisis en curso. Espera un momento."
                        )
                    }
                }
            }
        }
    }

    fun onEnunciadoChange(texto: String) {
        _uiState.update { it.copy(enunciadoEditable = texto, error = null) }
    }

    fun aceptarEnunciadoYVerSolucion() {
        val ejercicio = _uiState.value.ejercicio ?: return
        mostrarSolucion(ejercicio.copy(enunciado = _uiState.value.enunciadoEditable.trim().ifBlank {
            ejercicio.enunciado
        }))
    }

    /** Acción manual del usuario: nueva llamada solo texto. */
    fun resolverConEnunciadoCorregido() {
        val path = _uiState.value.fotoPath ?: return
        if (_uiState.value.analizando) return
        val enunciado = _uiState.value.enunciadoEditable.trim()
        if (enunciado.isBlank()) return
        _uiState.update { it.copy(analizando = true, error = null) }
        viewModelScope.launch {
            when (val outcome = service.resolverDesdeTexto(enunciado, path)) {
                is ResolverEjercicioOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            analizando = false,
                            enunciadoEditable = outcome.ejercicio.enunciado,
                            ejercicio = outcome.ejercicio,
                            avisoVerificacion = avisoDe(outcome.ejercicio.verificacion)
                        )
                    }
                    mostrarSolucion(outcome.ejercicio)
                }
                is ResolverEjercicioOutcome.Error -> {
                    _uiState.update {
                        it.copy(
                            analizando = false,
                            error = outcome.message,
                            canRetry = outcome.canRetry
                        )
                    }
                }
                ResolverEjercicioOutcome.Busy -> {
                    _uiState.update {
                        it.copy(
                            analizando = false,
                            error = "Ya hay un análisis en curso. Espera un momento."
                        )
                    }
                }
            }
        }
    }

    fun siguientePaso() {
        val total = _uiState.value.ejercicio?.pasos?.size ?: return
        _uiState.update {
            it.copy(pasosVisibles = (it.pasosVisibles + 1).coerceAtMost(total))
        }
    }

    fun verTodo() {
        val total = _uiState.value.ejercicio?.pasos?.size ?: return
        _uiState.update { it.copy(pasosVisibles = total) }
    }

    fun reintentarManual() {
        if (_uiState.value.analizando) return
        if (_uiState.value.fase == ResolverFase.ENUNCIADO ||
            _uiState.value.ejercicio != null && _uiState.value.fotoPath != null
        ) {
            // Solo reintento explícito del usuario
            analizarFoto()
        } else {
            analizarFoto()
        }
    }

    private fun mostrarSolucion(ejercicio: EjercicioResuelto) {
        _uiState.update {
            it.copy(
                fase = ResolverFase.SOLUCION,
                ejercicio = ejercicio,
                fotoPath = ejercicio.fotoPath.ifBlank { it.fotoPath },
                enunciadoEditable = ejercicio.enunciado,
                pasosVisibles = 1,
                avisoVerificacion = avisoDe(ejercicio.verificacion),
                error = null,
                analizando = false
            )
        }
    }

    private fun avisoDe(v: VerificacionEjercicio): String? = when (v) {
        VerificacionEjercicio.NO_COINCIDE ->
            "El resultado no coincide con el cálculo de la app, revísalo"
        VerificacionEjercicio.SIN_VERIFICACION ->
            "Sin verificación automática"
        VerificacionEjercicio.COINCIDE -> null
    }

    private fun copiarUriAArchivo(uri: Uri): String? = runCatching {
        val dir = File(appContext.filesDir, "ejercicios_foto").also { it.mkdirs() }
        val out = File(dir, "galeria_${System.currentTimeMillis()}.jpg")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(out).use { output -> input.copyTo(output) }
        } ?: return null
        out.absolutePath
    }.getOrNull()

    private fun guardarBitmap(bitmap: Bitmap): String {
        val dir = File(appContext.filesDir, "ejercicios_foto").also { it.mkdirs() }
        val out = File(dir, "crop_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        }
        return out.absolutePath
    }

    companion object {
        fun decodeFoto(path: String?): Bitmap? {
            if (path.isNullOrBlank()) return null
            return BitmapFactory.decodeFile(path)
        }
    }
}
