package com.laboratoriofisico.app.frontend.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.JuegoEjercicioProgreso
import com.laboratoriofisico.app.backend.repository.EjercicioResueltoRepository
import com.laboratoriofisico.app.tutor.ejercicio.EjercicioLocalVerifier
import com.laboratoriofisico.app.tutor.ejercicio.TutorNumberFormat
import com.laboratoriofisico.app.tutor.ejercicio.paraMostrar
import com.laboratoriofisico.app.tutor.juego.DatoOpcion
import com.laboratoriofisico.app.tutor.juego.FormulaOpcion
import com.laboratoriofisico.app.tutor.juego.JuegoEjercicioEngine
import com.laboratoriofisico.app.tutor.juego.SustitucionSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class JuegoPantalla {
    CARGANDO,
    INTENTALO,
    RETO_DATOS,
    RETO_FORMULA,
    RETO_SUSTITUYE,
    RETO_CALCULA,
    FINAL
}

enum class NivelPista { NINGUNA, SUAVE, CLARA, RESPUESTA }

data class JuegoFeedback(
    val correcto: Boolean,
    val mensaje: String
)

data class JuegoEjercicioUiState(
    val pantalla: JuegoPantalla = JuegoPantalla.CARGANDO,
    val ejercicio: EjercicioResuelto? = null,
    val error: String? = null,
    val retoDisponible: Boolean = false,
    val respuestaTexto: String = "",
    val unidadTexto: String = "",
    val feedback: JuegoFeedback? = null,
    val animacionAcierto: Boolean = false,
    val intentosDirectos: Int = 0,
    val fallosPrevios: Int = 0,
    val pistasUsadas: Int = 0,
    val nivelPista: NivelPista = NivelPista.NINGUNA,
    val textoPista: String = "",
    val mostroRespuesta: Boolean = false,
    val opcionesDatos: List<DatoOpcion> = emptyList(),
    val seleccionDatos: Set<Int> = emptySet(),
    val opcionesFormula: List<FormulaOpcion> = emptyList(),
    val formulaElegida: Int? = null,
    val slots: List<SustitucionSlot> = emptyList(),
    val valoresSlots: Map<Int, String> = emptyMap(),
    val estrellas: Int = 0,
    val mensajeAnimo: String = "",
    val ideaFisica: String = ""
)

/**
 * Juego sobre ejercicio guardado. Cero llamadas a Gemini.
 */
class JuegoEjercicioViewModel(
    private val ejercicioId: Long,
    private val repository: EjercicioResueltoRepository,
    private val verifier: EjercicioLocalVerifier
) : ViewModel() {

    private val _uiState = MutableStateFlow(JuegoEjercicioUiState())
    val uiState: StateFlow<JuegoEjercicioUiState> = _uiState.asStateFlow()

    /** Expuesto para tests: el juego no debe incrementar esto. */
    val geminiCallsFromGame: Int = 0

    init {
        cargar()
    }

    private fun cargar() {
        viewModelScope.launch {
            val raw = repository.getByIdForCurrentUser(ejercicioId)
            if (raw == null) {
                _uiState.update {
                    it.copy(pantalla = JuegoPantalla.INTENTALO, error = "No se encontró el ejercicio.")
                }
                return@launch
            }
            val ejercicio = raw.paraMostrar()
            val reto = JuegoEjercicioEngine.puedeRetoGuiado(ejercicio, verifier)
            val progreso = repository.getProgreso(ejercicioId)
            _uiState.update {
                it.copy(
                    pantalla = JuegoPantalla.INTENTALO,
                    ejercicio = ejercicio,
                    retoDisponible = reto,
                    unidadTexto = ejercicio.resultado.unidad,
                    ideaFisica = ejercicio.explicacionFormula.ifBlank {
                        "Relaciona los datos con la fórmula para hallar lo que se pide."
                    },
                    pistasUsadas = progreso?.pistasUsadas ?: 0,
                    estrellas = progreso?.estrellas ?: 0
                )
            }
        }
    }

    fun onRespuestaChange(value: String) {
        _uiState.update { it.copy(respuestaTexto = value, feedback = null, animacionAcierto = false) }
    }

    fun onUnidadChange(value: String) {
        _uiState.update { it.copy(unidadTexto = value, feedback = null) }
    }

    fun comprobarIntento() {
        val state = _uiState.value
        val ej = state.ejercicio ?: return
        val ok = JuegoEjercicioEngine.validarRespuestaNumerica(
            respuestaTexto = state.respuestaTexto,
            unidadTexto = state.unidadTexto,
            valorEsperado = ej.resultado.valor,
            unidadEsperada = ej.resultado.unidad
        )
        if (ok) {
            val estrellas = JuegoEjercicioEngine.calcularEstrellas(
                aciertoPrimerIntento = state.intentosDirectos == 0 && state.fallosPrevios == 0,
                pistasUsadas = state.pistasUsadas,
                fallosPrevios = state.fallosPrevios,
                mostroRespuesta = state.mostroRespuesta
            )
            finalizar(estrellas, animoPorEstrellas(estrellas))
        } else {
            _uiState.update {
                it.copy(
                    intentosDirectos = it.intentosDirectos + 1,
                    fallosPrevios = it.fallosPrevios + 1,
                    feedback = JuegoFeedback(
                        correcto = false,
                        mensaje = "Aún no. Revisa el número y la unidad, o entra al reto guiado."
                    )
                )
            }
            if (state.retoDisponible) {
                // Tras fallar, ofrecer entrar al reto (el usuario puede pulsar "No sé"/reto)
            }
        }
    }

    fun noSe() {
        val state = _uiState.value
        if (state.retoDisponible) {
            entrarRetoGuiado()
        } else {
            _uiState.update {
                it.copy(
                    fallosPrevios = it.fallosPrevios + 1,
                    feedback = JuegoFeedback(
                        correcto = false,
                        mensaje = "Este ejercicio no tiene reto guiado (falta fórmula reconocida o datos). Usa las pistas."
                    )
                )
            }
        }
    }

    fun entrarRetoGuiado() {
        val ej = _uiState.value.ejercicio ?: return
        if (!_uiState.value.retoDisponible) return
        _uiState.update {
            it.copy(
                pantalla = JuegoPantalla.RETO_DATOS,
                feedback = null,
                opcionesDatos = JuegoEjercicioEngine.generarOpcionesDatos(ej.datos),
                seleccionDatos = emptySet(),
                nivelPista = NivelPista.NINGUNA,
                textoPista = ""
            )
        }
    }

    fun toggleDato(index: Int) {
        _uiState.update { st ->
            val next = st.seleccionDatos.toMutableSet()
            if (!next.add(index)) next.remove(index)
            st.copy(seleccionDatos = next, feedback = null)
        }
    }

    fun comprobarDatos() {
        val st = _uiState.value
        val correctas = st.opcionesDatos.mapIndexedNotNull { i, o -> if (o.correcto) i else null }.toSet()
        if (st.seleccionDatos == correctas) {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(true, "Bien: esos son los datos del enunciado."),
                    animacionAcierto = true,
                    pantalla = JuegoPantalla.RETO_FORMULA,
                    opcionesFormula = JuegoEjercicioEngine.generarOpcionesFormula(
                        it.ejercicio!!,
                        verifier
                    ),
                    formulaElegida = null,
                    nivelPista = NivelPista.NINGUNA,
                    textoPista = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(
                        false,
                        "Hay datos de más o faltan algunos. Lee otra vez el enunciado: solo lo que se da."
                    )
                )
            }
        }
    }

    fun elegirFormula(index: Int) {
        _uiState.update { it.copy(formulaElegida = index, feedback = null) }
    }

    fun comprobarFormula() {
        val st = _uiState.value
        val idx = st.formulaElegida ?: return
        val opcion = st.opcionesFormula.getOrNull(idx) ?: return
        if (opcion.correcta) {
            val ej = st.ejercicio!!
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(true, "Correcto: esa es la fórmula."),
                    animacionAcierto = true,
                    pantalla = JuegoPantalla.RETO_SUSTITUYE,
                    slots = JuegoEjercicioEngine.slotsSustitucion(ej),
                    valoresSlots = emptyMap(),
                    nivelPista = NivelPista.NINGUNA,
                    textoPista = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(
                        false,
                        "Esa fórmula relaciona otras magnitudes. Busca la que usa los datos de este problema."
                    )
                )
            }
        }
    }

    fun onSlotChange(slotId: Int, value: String) {
        _uiState.update {
            it.copy(
                valoresSlots = it.valoresSlots + (slotId to value),
                feedback = null
            )
        }
    }

    fun comprobarSustitucion() {
        val st = _uiState.value
        val ok = st.slots.all { slot ->
            val texto = st.valoresSlots[slot.id].orEmpty()
            val valor = JuegoEjercicioEngine.parseNumero(texto)
            valor != null && absRel(valor, slot.valorCorrecto)
        }
        if (ok) {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(true, "Bien: cada valor está en su lugar."),
                    animacionAcierto = true,
                    pantalla = JuegoPantalla.RETO_CALCULA,
                    respuestaTexto = "",
                    nivelPista = NivelPista.NINGUNA,
                    textoPista = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(
                        false,
                        "Revisa qué símbolo corresponde a cada dato. El número debe coincidir con el enunciado."
                    )
                )
            }
        }
    }

    fun comprobarCalculoReto() {
        val st = _uiState.value
        val ej = st.ejercicio ?: return
        val ok = JuegoEjercicioEngine.validarRespuestaNumerica(
            respuestaTexto = st.respuestaTexto,
            unidadTexto = st.unidadTexto.ifBlank { ej.resultado.unidad },
            valorEsperado = ej.resultado.valor,
            unidadEsperada = ej.resultado.unidad
        )
        if (ok) {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(true, "¡Calculaste bien! Ahora inténtalo tú de nuevo."),
                    animacionAcierto = true,
                    pantalla = JuegoPantalla.INTENTALO,
                    respuestaTexto = "",
                    nivelPista = NivelPista.NINGUNA,
                    textoPista = ""
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    feedback = JuegoFeedback(
                        false,
                        "El número no cuadra. Sustituye otra vez y opera con cuidado; revisa la unidad."
                    )
                )
            }
        }
    }

    fun pedirPista() {
        val st = _uiState.value
        val ej = st.ejercicio ?: return
        val siguiente = when (st.nivelPista) {
            NivelPista.NINGUNA -> NivelPista.SUAVE
            NivelPista.SUAVE -> NivelPista.CLARA
            NivelPista.CLARA, NivelPista.RESPUESTA -> st.nivelPista
        }
        val texto = textoPistaPara(siguiente, st.pantalla, ej)
        _uiState.update {
            it.copy(
                nivelPista = siguiente,
                textoPista = texto,
                pistasUsadas = it.pistasUsadas + if (siguiente != st.nivelPista) 1 else 0,
                mostroRespuesta = it.mostroRespuesta || siguiente == NivelPista.RESPUESTA
            )
        }
    }

    fun verSolucion() {
        val st = _uiState.value
        val ej = st.ejercicio ?: return
        if (st.nivelPista == NivelPista.RESPUESTA) return
        _uiState.update {
            it.copy(
                nivelPista = NivelPista.RESPUESTA,
                textoPista = textoPistaPara(NivelPista.RESPUESTA, st.pantalla, ej),
                mostroRespuesta = true,
                pistasUsadas = it.pistasUsadas + 1
            )
        }
    }

    fun reintentar() {
        val ej = _uiState.value.ejercicio ?: return
        _uiState.update {
            JuegoEjercicioUiState(
                pantalla = JuegoPantalla.INTENTALO,
                ejercicio = ej,
                retoDisponible = it.retoDisponible,
                unidadTexto = ej.resultado.unidad,
                ideaFisica = it.ideaFisica
            )
        }
    }

    private fun finalizar(estrellas: Int, animo: String) {
        val st = _uiState.value
        val ej = st.ejercicio ?: return
        viewModelScope.launch {
            repository.guardarProgreso(
                JuegoEjercicioProgreso(
                    usuarioId = ej.usuarioId,
                    ejercicioId = ej.id,
                    intentos = st.intentosDirectos + st.fallosPrevios + 1,
                    estrellas = maxOf(estrellas, st.estrellas),
                    pistasUsadas = st.pistasUsadas,
                    completo = true
                )
            )
            _uiState.update {
                it.copy(
                    pantalla = JuegoPantalla.FINAL,
                    estrellas = maxOf(estrellas, it.estrellas),
                    mensajeAnimo = animo,
                    animacionAcierto = true,
                    feedback = JuegoFeedback(true, "¡Lo lograste!")
                )
            }
        }
    }

    private fun textoPistaPara(nivel: NivelPista, pantalla: JuegoPantalla, ej: EjercicioResuelto): String {
        val resultadoFmt = TutorNumberFormat.withUnit(ej.resultado.valor, ej.resultado.unidad)
        return when (pantalla) {
            JuegoPantalla.INTENTALO, JuegoPantalla.RETO_CALCULA -> when (nivel) {
                NivelPista.SUAVE -> "Usa la fórmula y los datos del enunciado. No olvides la unidad."
                NivelPista.CLARA -> "La idea: ${ej.explicacionFormula.ifBlank { ej.formulaLegible }}"
                NivelPista.RESPUESTA -> "La respuesta es $resultadoFmt"
                else -> ""
            }
            JuegoPantalla.RETO_DATOS -> when (nivel) {
                NivelPista.SUAVE -> "Marca solo los números que aparecen en el enunciado."
                NivelPista.CLARA -> "Hay datos inventados de más; quítalos."
                NivelPista.RESPUESTA -> "Datos correctos: " + ej.datos.joinToString { "${it.nombre}=${it.valor} ${it.unidad}" }
                else -> ""
            }
            JuegoPantalla.RETO_FORMULA -> when (nivel) {
                NivelPista.SUAVE -> "Mira qué magnitudes tienes y cuál buscas."
                NivelPista.CLARA -> "La fórmula correcta usa: ${ej.formulaLegible.ifBlank { ej.formula }}"
                NivelPista.RESPUESTA -> "Elige: ${ej.formulaLegible.ifBlank { ej.formula }}"
                else -> ""
            }
            JuegoPantalla.RETO_SUSTITUYE -> when (nivel) {
                NivelPista.SUAVE -> "Cada letra de la fórmula corresponde a un dato."
                NivelPista.CLARA -> "Coloca el valor junto al símbolo que tiene el mismo nombre."
                NivelPista.RESPUESTA -> ej.datos.joinToString { "${it.nombre} → ${it.valor}" }
                else -> ""
            }
            else -> ""
        }
    }

    private fun animoPorEstrellas(estrellas: Int): String = when (estrellas) {
        3 -> "Excelente: lo resolviste a la primera."
        2 -> "Muy bien: las pistas te ayudaron y lo lograste."
        else -> "Lo importante es que lo entendiste. ¡Sigue practicando!"
    }

    private fun absRel(a: Double, b: Double): Boolean {
        val diff = kotlin.math.abs(a - b)
        val scale = maxOf(kotlin.math.abs(b), 1.0)
        return diff <= 1e-3 || diff / scale <= 0.01
    }
}
