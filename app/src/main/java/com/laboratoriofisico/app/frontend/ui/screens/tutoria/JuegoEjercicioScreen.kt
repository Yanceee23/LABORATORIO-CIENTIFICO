package com.laboratoriofisico.app.frontend.ui.screens.tutoria

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.PrimaryButton
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.viewmodel.JuegoEjercicioUiState
import com.laboratoriofisico.app.frontend.viewmodel.JuegoEjercicioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.JuegoPantalla
import com.laboratoriofisico.app.frontend.viewmodel.NivelPista

private val SoftGreen = Color(0xFF2A9D8F)
private val SoftRed = Color(0xFFC75B5B)

@Composable
fun JuegoEjercicioScreen(
    viewModel: JuegoEjercicioViewModel,
    onNavigateBack: () -> Unit,
    onSiguienteEjercicio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ui by viewModel.uiState.collectAsState()
    val scale by animateFloatAsState(
        targetValue = if (ui.animacionAcierto) 1.04f else 1f,
        animationSpec = tween(280),
        label = "acierto"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .scale(scale),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.tutor_juego_title),
            subtitle = stringResource(R.string.tutor_juego_subtitle)
        )

        when (ui.pantalla) {
            JuegoPantalla.CARGANDO -> Text(stringResource(R.string.tutor_juego_cargando))
            JuegoPantalla.INTENTALO -> IntentaloTuSection(ui, viewModel)
            JuegoPantalla.RETO_DATOS -> RetoDatosSection(ui, viewModel)
            JuegoPantalla.RETO_FORMULA -> RetoFormulaSection(ui, viewModel)
            JuegoPantalla.RETO_SUSTITUYE -> RetoSustituyeSection(ui, viewModel)
            JuegoPantalla.RETO_CALCULA -> RetoCalculaSection(ui, viewModel)
            JuegoPantalla.FINAL -> FinalSection(ui, viewModel, onNavigateBack, onSiguienteEjercicio)
        }

        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun IntentaloTuSection(ui: JuegoEjercicioUiState, vm: JuegoEjercicioViewModel) {
    val ej = ui.ejercicio ?: return
    val queEs = listOf(ej.formulaLegible, ej.explicacionFormula)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .joinToString("\n")
        .ifBlank { ui.ideaFisica.ifBlank { stringResource(R.string.tutor_juego_subtitle) } }
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.tutor_juego_que_es),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            Text(queEs, color = Graphite)
        }
    }
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.tutor_juego_ejercicio),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            Text(ej.enunciado, color = Graphite)
            OutlinedTextField(
                value = ui.respuestaTexto,
                onValueChange = vm::onRespuestaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tutor_juego_tu_respuesta)) }
            )
            OutlinedTextField(
                value = ui.unidadTexto,
                onValueChange = vm::onUnidadChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tutor_juego_unidad)) }
            )
            FeedbackBox(ui)
            Button(
                onClick = vm::comprobarIntento,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PetrolDark, contentColor = Color.White)
            ) {
                Text(stringResource(R.string.tutor_juego_comprobar))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = vm::pedirPista,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = ui.nivelPista != NivelPista.CLARA && ui.nivelPista != NivelPista.RESPUESTA
                ) {
                    Text(stringResource(R.string.tutor_juego_pista))
                }
                OutlinedButton(
                    onClick = vm::verSolucion,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = ui.nivelPista != NivelPista.RESPUESTA
                ) {
                    Text(stringResource(R.string.tutor_juego_ver_solucion))
                }
            }
            if (ui.textoPista.isNotBlank()) {
                Text(
                    text = if (ui.nivelPista == NivelPista.RESPUESTA) {
                        stringResource(R.string.tutor_juego_solucion)
                    } else {
                        stringResource(R.string.tutor_juego_pista)
                    },
                    fontWeight = FontWeight.Bold,
                    color = PetrolDark
                )
                Text(ui.textoPista, color = Graphite)
            }
        }
    }
}

@Composable
private fun RetoDatosSection(ui: JuegoEjercicioUiState, vm: JuegoEjercicioViewModel) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.tutor_juego_paso_datos),
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            Text(stringResource(R.string.tutor_juego_paso_datos_ayuda), color = GraphiteMedium)
            ui.opcionesDatos.forEachIndexed { index, opcion ->
                val selected = index in ui.seleccionDatos
                Text(
                    opcion.etiqueta,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (selected) SoftGreen else GraphiteMedium,
                            RoundedCornerShape(10.dp)
                        )
                        .background(if (selected) SoftGreen.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { vm.toggleDato(index) }
                        .padding(12.dp),
                    color = Graphite
                )
            }
            FeedbackBox(ui)
            PrimaryButton(stringResource(R.string.tutor_juego_comprobar), onClick = vm::comprobarDatos)
            PistaRow(ui, vm)
        }
    }
}

@Composable
private fun RetoFormulaSection(ui: JuegoEjercicioUiState, vm: JuegoEjercicioViewModel) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.tutor_juego_paso_formula),
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            ui.opcionesFormula.forEachIndexed { index, opcion ->
                val selected = ui.formulaElegida == index
                Text(
                    opcion.texto,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (selected) SoftGreen else GraphiteMedium,
                            RoundedCornerShape(10.dp)
                        )
                        .background(if (selected) SoftGreen.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { vm.elegirFormula(index) }
                        .padding(14.dp),
                    color = Graphite,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            FeedbackBox(ui)
            PrimaryButton(stringResource(R.string.tutor_juego_comprobar), onClick = vm::comprobarFormula)
            PistaRow(ui, vm)
        }
    }
}

@Composable
private fun RetoSustituyeSection(ui: JuegoEjercicioUiState, vm: JuegoEjercicioViewModel) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.tutor_juego_paso_sustituye),
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            ui.ejercicio?.formulaLegible?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.titleLarge, color = PetrolDark)
            }
            ui.slots.forEach { slot ->
                OutlinedTextField(
                    value = ui.valoresSlots[slot.id].orEmpty(),
                    onValueChange = { vm.onSlotChange(slot.id, it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${slot.simbolo} (${slot.unidad})") }
                )
            }
            FeedbackBox(ui)
            PrimaryButton(stringResource(R.string.tutor_juego_comprobar), onClick = vm::comprobarSustitucion)
            PistaRow(ui, vm)
        }
    }
}

@Composable
private fun RetoCalculaSection(ui: JuegoEjercicioUiState, vm: JuegoEjercicioViewModel) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(R.string.tutor_juego_paso_calcula),
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            OutlinedTextField(
                value = ui.respuestaTexto,
                onValueChange = vm::onRespuestaChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tutor_juego_tu_respuesta)) }
            )
            OutlinedTextField(
                value = ui.unidadTexto,
                onValueChange = vm::onUnidadChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.tutor_juego_unidad)) }
            )
            FeedbackBox(ui)
            PrimaryButton(stringResource(R.string.tutor_juego_comprobar), onClick = vm::comprobarCalculoReto)
            PistaRow(ui, vm)
        }
    }
}

@Composable
private fun FinalSection(
    ui: JuegoEjercicioUiState,
    vm: JuegoEjercicioViewModel,
    onVolver: () -> Unit,
    onSiguiente: () -> Unit
) {
    LabSurfaceCard {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(R.string.tutor_juego_estrellas, "★".repeat(ui.estrellas.coerceIn(0, 3))),
                style = MaterialTheme.typography.headlineMedium,
                color = PetrolDark
            )
            Text(ui.mensajeAnimo, color = Graphite)
            Text(ui.ideaFisica, color = GraphiteMedium, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            PrimaryButton(stringResource(R.string.tutor_juego_reintentar), onClick = vm::reintentar)
            SecondaryButton(stringResource(R.string.tutor_juego_siguiente), onClick = onSiguiente)
            SecondaryButton(stringResource(R.string.tutor_juego_volver), onClick = onVolver)
        }
    }
}

@Composable
private fun FeedbackBox(ui: JuegoEjercicioUiState) {
    ui.feedback?.let { fb ->
        Text(
            fb.mensaje,
            color = if (fb.correcto) SoftGreen else SoftRed,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun PistaRow(ui: JuegoEjercicioUiState, vm: JuegoEjercicioViewModel) {
    val label = when (ui.nivelPista) {
        NivelPista.NINGUNA -> stringResource(R.string.tutor_juego_pista_suave)
        NivelPista.SUAVE -> stringResource(R.string.tutor_juego_pista_clara)
        NivelPista.CLARA, NivelPista.RESPUESTA -> stringResource(R.string.tutor_juego_pista_respuesta)
    }
    SecondaryButton(text = label, onClick = vm::pedirPista)
    if (ui.textoPista.isNotBlank()) {
        Text(ui.textoPista, color = GraphiteMedium, style = MaterialTheme.typography.bodySmall)
    }
}
