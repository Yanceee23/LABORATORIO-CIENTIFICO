package com.laboratoriofisico.app.frontend.ui.screens.tutoria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.PetrolDark
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.tutor.juego.LeccionBasica
import com.laboratoriofisico.app.tutor.juego.LeccionesBasicas
import com.laboratoriofisico.app.tutor.juego.MateriaAprender

private val SoftGreen = Color(0xFF2A9D8F)
private val SoftRed = Color(0xFFC75B5B)

@Composable
fun AprenderMateriaScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var materiaNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var leccionId by rememberSaveable { mutableStateOf<String?>(null) }
    var completadas by rememberSaveable { mutableStateOf("") }
    val materia = materiaNombre?.let { runCatching { MateriaAprender.valueOf(it) }.getOrNull() }
    val leccion = LeccionesBasicas.todas.firstOrNull { it.id == leccionId }
    val hechas = completadas.split(',').filter { it.isNotBlank() }.toSet()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LabBackLink(onClick = {
            when {
                leccion != null -> leccionId = null
                materia != null -> materiaNombre = null
                else -> onNavigateBack()
            }
        })
        when {
            materia == null -> ElegirMateria(
                onElegir = { elegida ->
                    materiaNombre = elegida.name
                    leccionId = null
                }
            )
            leccion == null -> ListaConceptos(
                materia = materia,
                hechas = hechas,
                onAbrir = { leccionId = it.id }
            )
            else -> LeccionConcepto(
                leccion = leccion,
                onAcierto = {
                    if (leccion.id !in hechas) {
                        completadas = (hechas + leccion.id).joinToString(",")
                    }
                },
                onSiguiente = {
                    val lista = LeccionesBasicas.de(materia)
                    val index = lista.indexOfFirst { it.id == leccion.id }
                    val siguiente = lista.getOrNull(index + 1)
                    leccionId = siguiente?.id
                }
            )
        }
    }
}

@Composable
private fun ElegirMateria(onElegir: (MateriaAprender) -> Unit) {
    LabScreenTitle(
        title = stringResource(R.string.tutor_juego_title),
        subtitle = stringResource(R.string.tutor_aprender_pregunta)
    )
    Text(stringResource(R.string.tutor_aprender_elige), color = Graphite)
    MateriaAprender.entries.forEach { materia ->
        LabSurfaceCard(onClick = { onElegir(materia) }) {
            Text(
                text = nombreMateria(materia),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
        }
    }
}

@Composable
private fun ListaConceptos(
    materia: MateriaAprender,
    hechas: Set<String>,
    onAbrir: (LeccionBasica) -> Unit
) {
    LabScreenTitle(
        title = nombreMateria(materia),
        subtitle = stringResource(R.string.tutor_juego_subtitle)
    )
    LeccionesBasicas.de(materia).forEach { leccion ->
        LabSurfaceCard(onClick = { onAbrir(leccion) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    leccion.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PetrolDark
                )
                if (leccion.id in hechas) {
                    Text("✓", color = SoftGreen, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LeccionConcepto(
    leccion: LeccionBasica,
    onAcierto: () -> Unit,
    onSiguiente: () -> Unit
) {
    var respuesta by rememberSaveable(leccion.id) { mutableStateOf("") }
    var pistaVisible by rememberSaveable(leccion.id) { mutableStateOf(false) }
    var solucionVisible by rememberSaveable(leccion.id) { mutableStateOf(false) }
    var feedbackOk by rememberSaveable(leccion.id) { mutableStateOf<Boolean?>(null) }

    LabScreenTitle(
        title = leccion.titulo,
        subtitle = nombreMateria(leccion.materia)
    )
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.tutor_juego_que_es),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = PetrolDark
            )
            Text(leccion.queEs, color = Graphite)
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
            Text(leccion.enunciado, color = Graphite)
            OutlinedTextField(
                value = respuesta,
                onValueChange = {
                    respuesta = it
                    feedbackOk = null
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        if (leccion.valor != null) {
                            stringResource(R.string.tutor_juego_tu_respuesta)
                        } else {
                            stringResource(R.string.tutor_aprender_tu_respuesta)
                        }
                    )
                },
                supportingText = if (leccion.unidad.isNotBlank()) {
                    { Text(leccion.unidad) }
                } else {
                    null
                }
            )
            feedbackOk?.let { ok ->
                Text(
                    text = stringResource(
                        if (ok) R.string.tutor_aprender_correcto else R.string.tutor_aprender_incorrecto
                    ),
                    color = if (ok) SoftGreen else SoftRed,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = {
                    val ok = LeccionesBasicas.acierto(leccion, respuesta)
                    feedbackOk = ok
                    if (ok) onAcierto()
                },
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
                    onClick = { pistaVisible = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !pistaVisible
                ) {
                    Text(stringResource(R.string.tutor_juego_pista))
                }
                OutlinedButton(
                    onClick = { solucionVisible = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !solucionVisible
                ) {
                    Text(stringResource(R.string.tutor_juego_ver_solucion))
                }
            }
            if (pistaVisible) {
                Text(stringResource(R.string.tutor_juego_pista), fontWeight = FontWeight.Bold, color = PetrolDark)
                Text(leccion.pista, color = Graphite)
            }
            if (solucionVisible) {
                Text(stringResource(R.string.tutor_juego_solucion), fontWeight = FontWeight.Bold, color = PetrolDark)
                Text(leccion.solucion, color = Graphite)
            }
            if (feedbackOk == true) {
                OutlinedButton(
                    onClick = onSiguiente,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.tutor_aprender_siguiente))
                }
            }
        }
    }
}

@Composable
private fun nombreMateria(materia: MateriaAprender): String = when (materia) {
    MateriaAprender.FISICA -> stringResource(R.string.tutor_materia_fisica)
    MateriaAprender.QUIMICA -> stringResource(R.string.tutor_materia_quimica)
    MateriaAprender.BIOLOGIA -> stringResource(R.string.tutor_materia_biologia)
}
