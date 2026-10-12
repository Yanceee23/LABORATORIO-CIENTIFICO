package com.laboratoriofisico.app.frontend.ui.screens.estudio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.model.TipoResumen
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.frontend.ui.components.DifficultySelector
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioCard
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.ui.components.ExportShareLaunchedEffect
import com.laboratoriofisico.app.frontend.ui.components.PrimaryButton
import com.laboratoriofisico.app.frontend.ui.components.RichMarkdownText
import com.laboratoriofisico.app.frontend.ui.components.ProgressBarEstudio
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.viewmodel.EstudioViewModel

private val tabs = listOf(
    R.string.estudio_tab_resumen,
    R.string.estudio_tab_notas,
    R.string.estudio_tab_preguntas,
    R.string.estudio_tab_ejercicios,
    R.string.estudio_tab_examen,
    R.string.estudio_tab_tarjetas,
    R.string.estudio_tab_historial
)

@Composable
fun EstudioHubScreen(
    viewModel: EstudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val documentos by viewModel.documentos.collectAsState()

    ExportShareLaunchedEffect(
        shareUri = uiState.shareUri,
        shareFileName = uiState.shareFileName,
        mimeType = "application/pdf",
        chooserTitleRes = R.string.estudio_compartir_historial_pdf,
        onShareHandled = viewModel::onShareHandled,
        onShareError = viewModel::onShareError
    )

    LaboratorioScaffold(title = stringResource(R.string.estudio_title), modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = if (uiState.iaConfigurada) {
                    stringResource(R.string.estudio_ia_activa)
                } else {
                    stringResource(R.string.tutor_ia_sin_clave)
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (uiState.iaConfigurada) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    stringResource(R.string.estudio_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(stringResource(R.string.estudio_seleccionar_documento), style = MaterialTheme.typography.labelLarge)
                if (documentos.isEmpty()) {
                    Text(stringResource(R.string.estudio_sin_documento), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                        documentos.forEach { doc ->
                            FilterChip(
                                selected = uiState.documentoSeleccionadoId == doc.id,
                                onClick = { viewModel.seleccionarDocumento(doc.id) },
                                label = { Text(doc.nombre.take(20)) }
                            )
                        }
                    }
                }
            }
            TabRow(selectedTabIndex = uiState.tabIndex) {
                tabs.forEachIndexed { index, labelRes ->
                    Tab(
                        selected = uiState.tabIndex == index,
                        onClick = { viewModel.seleccionarTab(index) },
                        text = { Text(stringResource(labelRes), maxLines = 1) }
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.generando) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.estudio_generando))
                }
                when (uiState.tabIndex) {
                    0 -> ResumenTab(uiState, viewModel::seleccionarTipoResumen, viewModel::generarResumen)
                    1 -> NotasTab(uiState, viewModel::onNotasChange, viewModel::generarNotas, viewModel::guardarNotas)
                    2 -> PreguntasTab(uiState, viewModel::seleccionarDificultad, viewModel::generarPreguntas, viewModel::toggleMostrarRespuestas)
                    3 -> EjerciciosTab(uiState, viewModel::generarEjercicio, viewModel::onRespuestaEjercicioChange, viewModel::verificarEjercicio)
                    4 -> ExamenTab(uiState, viewModel::crearExamen, viewModel::responderExamen, viewModel::finalizarExamen)
                    5 -> TarjetasTab(uiState, viewModel::generarTarjetas, viewModel::toggleReverso, viewModel::anteriorTarjeta, viewModel::siguienteTarjeta)
                    6 -> HistorialTab(
                        historial = uiState.historial,
                        exportando = uiState.exportandoHistorial,
                        onExportar = viewModel::exportarHistorialPdf
                    )
                }
                uiState.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.tertiary) }
                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun ResumenTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.EstudioUiState,
    onTipo: (TipoResumen) -> Unit,
    onGenerar: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = uiState.tipoResumen == TipoResumen.AUTOMATICO, onClick = { onTipo(TipoResumen.AUTOMATICO) }, label = { Text(stringResource(R.string.estudio_resumen_automatico)) })
        FilterChip(selected = uiState.tipoResumen == TipoResumen.CORTO, onClick = { onTipo(TipoResumen.CORTO) }, label = { Text(stringResource(R.string.estudio_resumen_corto)) })
        FilterChip(selected = uiState.tipoResumen == TipoResumen.DETALLADO, onClick = { onTipo(TipoResumen.DETALLADO) }, label = { Text(stringResource(R.string.estudio_resumen_detallado)) })
    }
    PrimaryButton(
        text = stringResource(R.string.estudio_btn_resumen),
        onClick = onGenerar,
        enabled = !uiState.generando && uiState.iaConfigurada && uiState.documentoSeleccionadoId != null
    )
    if (uiState.resumen.isNotBlank()) {
        LaboratorioCard {
            Text(stringResource(R.string.estudio_tu_resumen), style = MaterialTheme.typography.titleSmall)
            RichMarkdownText(markdown = uiState.resumen, renderMath = false)
        }
    }
}

@Composable
private fun NotasTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.EstudioUiState,
    onChange: (String) -> Unit,
    onGenerar: () -> Unit,
    onGuardar: () -> Unit
) {
    PrimaryButton(
        text = stringResource(R.string.estudio_btn_notas),
        onClick = onGenerar,
        enabled = !uiState.generando && uiState.iaConfigurada && uiState.documentoSeleccionadoId != null
    )
    if (uiState.notas.isNotBlank()) {
        LaboratorioCard {
            RichMarkdownText(markdown = uiState.notas, renderMath = false)
        }
    }
    OutlinedTextField(value = uiState.notas, onValueChange = onChange, modifier = Modifier.fillMaxWidth(), minLines = 6)
    SecondaryButton(text = stringResource(R.string.estudio_guardar_notas), onClick = onGuardar)
}

@Composable
private fun PreguntasTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.EstudioUiState,
    onDificultad: (String) -> Unit,
    onGenerar: () -> Unit,
    onToggle: () -> Unit
) {
    DifficultySelector(selected = uiState.dificultad, onSelected = onDificultad)
    PrimaryButton(
        text = stringResource(R.string.estudio_btn_preguntas),
        onClick = onGenerar,
        enabled = !uiState.generando && uiState.iaConfigurada && uiState.documentoSeleccionadoId != null
    )
    SecondaryButton(text = stringResource(R.string.estudio_ver_respuesta), onClick = onToggle)
    uiState.preguntas.filter { it.dificultad == uiState.dificultad }.forEach { p ->
        LaboratorioCard {
            Text(p.pregunta, style = MaterialTheme.typography.titleSmall)
            if (uiState.mostrarRespuestas) {
                RichMarkdownText(markdown = p.respuesta, renderMath = false)
                if (p.explicacion.isNotBlank()) {
                    RichMarkdownText(markdown = p.explicacion, renderMath = false)
                }
            }
        }
    }
}

@Composable
private fun EjerciciosTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.EstudioUiState,
    onGenerar: () -> Unit,
    onRespuesta: (String) -> Unit,
    onVerificar: () -> Unit
) {
    PrimaryButton(
        text = stringResource(R.string.estudio_btn_ejercicio),
        onClick = onGenerar,
        enabled = !uiState.generando && uiState.iaConfigurada && uiState.documentoSeleccionadoId != null
    )
    uiState.ejercicio?.let { e ->
        LaboratorioCard {
            Text(e.enunciado, style = MaterialTheme.typography.titleSmall)
            if (e.datos.isNotBlank()) {
                Text(stringResource(R.string.estudio_datos_reales), style = MaterialTheme.typography.labelLarge)
                Text(e.datos)
            }
            if (e.formula.isNotBlank()) {
                Text(stringResource(R.string.estudio_formula), style = MaterialTheme.typography.labelLarge)
                Text(e.formula)
            }
        }
        OutlinedTextField(value = uiState.respuestaEjercicio, onValueChange = onRespuesta, label = { Text("Respuesta") }, modifier = Modifier.fillMaxWidth())
        PrimaryButton(text = stringResource(R.string.estudio_verificar), onClick = onVerificar)
        uiState.verificacionEjercicio?.let { RichMarkdownText(markdown = it, renderMath = false) }
    }
}

@Composable
private fun ExamenTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.EstudioUiState,
    onCrear: () -> Unit,
    onResponder: (Int, Int) -> Unit,
    onFinalizar: () -> Unit
) {
    PrimaryButton(
        text = stringResource(R.string.estudio_crear_examen),
        onClick = onCrear,
        enabled = !uiState.generando && uiState.iaConfigurada && uiState.documentoSeleccionadoId != null
    )
    uiState.examen?.preguntas?.forEach { p ->
        LaboratorioCard {
            Text(p.pregunta, style = MaterialTheme.typography.titleSmall)
            p.opciones.forEachIndexed { idx, op ->
                FilterChip(
                    selected = uiState.respuestasExamen[p.id] == idx,
                    onClick = { onResponder(p.id, idx) },
                    label = { Text(op) },
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
    if (uiState.examen != null) {
        PrimaryButton(text = stringResource(R.string.estudio_finalizar_examen), onClick = onFinalizar)
    }
    uiState.calificacionExamen?.let { Text(stringResource(R.string.estudio_calificacion, it)) }
}

@Composable
private fun TarjetasTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.EstudioUiState,
    onGenerar: () -> Unit,
    onToggle: () -> Unit,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    PrimaryButton(
        text = stringResource(R.string.estudio_btn_tarjetas),
        onClick = onGenerar,
        enabled = !uiState.generando && uiState.iaConfigurada && uiState.documentoSeleccionadoId != null
    )
    if (uiState.tarjetas.isNotEmpty()) {
        val tarjeta = uiState.tarjetas[uiState.tarjetaIndex]
        val progress = (uiState.tarjetaIndex + 1).toFloat() / uiState.tarjetas.size
        ProgressBarEstudio(progress = progress, label = "Tarjeta ${uiState.tarjetaIndex + 1}/${uiState.tarjetas.size}")
        LaboratorioCard {
            RichMarkdownText(
                markdown = if (uiState.mostrarReverso) tarjeta.reverso else tarjeta.frente,
                renderMath = false
            )
        }
        SecondaryButton(text = stringResource(R.string.estudio_mostrar_respuesta), onClick = onToggle)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(text = stringResource(R.string.estudio_anterior_tarjeta), onClick = onAnterior)
            PrimaryButton(text = stringResource(R.string.estudio_siguiente_tarjeta), onClick = onSiguiente)
        }
    }
}

@Composable
private fun HistorialTab(
    historial: List<com.laboratoriofisico.app.backend.model.HistorialEstudio>,
    exportando: Boolean,
    onExportar: () -> Unit
) {
    PrimaryButton(
        text = stringResource(R.string.estudio_exportar_historial_pdf),
        onClick = onExportar,
        enabled = !exportando
    )
    if (exportando) {
        CircularProgressIndicator()
        Text(stringResource(R.string.estudio_exportando_historial_pdf))
    }
    if (historial.isEmpty()) {
        Text(stringResource(R.string.estudio_historial_vacio))
    }
    historial.forEach { h ->
        LaboratorioCard {
            Text("${h.tipoActividad}: ${h.resultado}", style = MaterialTheme.typography.titleSmall)
            Text(DateTimeUtils.format(h.fecha), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(R.string.estudio_tiempo_sesion, DateTimeUtils.formatDuration(h.tiempoEstudio)),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
