package com.laboratoriofisico.app.frontend.ui.screens.tutoria

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.frontend.ui.components.ChatBubble
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.ui.components.PrimaryButton
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.viewmodel.TutorViewModel

@Composable
fun TutorHubScreen(
    viewModel: TutorViewModel,
    onAprenderJugando: () -> Unit,
    onResolverConFoto: () -> Unit,
    onMisEjercicios: () -> Unit,
    resultadoIdInicial: Long? = null,
    modifier: Modifier = Modifier
) {
    var tabIndex by remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(resultadoIdInicial) {
        resultadoIdInicial?.let { viewModel.analizarResultadoInicial(it) }
    }

    LaboratorioScaffold(title = stringResource(R.string.tutor_title), modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = if (uiState.iaConfigurada) {
                    stringResource(R.string.tutor_ia_activa)
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
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PrimaryButton(
                    text = stringResource(R.string.tutor_aprender_jugando),
                    onClick = onAprenderJugando
                )
                SecondaryButton(
                    text = stringResource(R.string.tutor_resolver_con_foto),
                    onClick = onResolverConFoto
                )
                SecondaryButton(
                    text = stringResource(R.string.tutor_mis_ejercicios),
                    onClick = onMisEjercicios
                )
            }
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Chat") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("PDF") })
            }
            when (tabIndex) {
                0 -> TutorChatTab(viewModel, uiState)
                1 -> TutorPdfTab(viewModel, uiState)
            }
        }
    }
}

@Composable
private fun TutorChatTab(
    viewModel: TutorViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.TutorUiState
) {
    val listState = rememberLazyListState()
    var showFormulaDialog by remember { mutableStateOf(false) }
    var showConceptoDialog by remember { mutableStateOf(false) }
    var showEjercicioDialog by remember { mutableStateOf(false) }
    var formulaInput by remember { mutableStateOf("F = ma") }
    var conceptoInput by remember { mutableStateOf("Velocidad") }
    var enunciadoInput by remember { mutableStateOf("") }
    var datosInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty()) {
            listState.animateScrollToItem(uiState.mensajes.size - 1)
        }
    }

    if (showFormulaDialog) {
        AlertDialog(
            onDismissRequest = { showFormulaDialog = false },
            title = { Text(stringResource(R.string.tutor_formula_dialog)) },
            text = {
                OutlinedTextField(
                    value = formulaInput,
                    onValueChange = { formulaInput = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showFormulaDialog = false
                    viewModel.explicarFormula(formulaInput.trim())
                }) { Text(stringResource(R.string.tutor_enviar)) }
            },
            dismissButton = {
                TextButton(onClick = { showFormulaDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showConceptoDialog) {
        AlertDialog(
            onDismissRequest = { showConceptoDialog = false },
            title = { Text(stringResource(R.string.tutor_concepto_dialog)) },
            text = {
                OutlinedTextField(
                    value = conceptoInput,
                    onValueChange = { conceptoInput = it },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showConceptoDialog = false
                    viewModel.explicarConcepto(conceptoInput.trim())
                }) { Text(stringResource(R.string.tutor_enviar)) }
            },
            dismissButton = {
                TextButton(onClick = { showConceptoDialog = false }) { Text("Cancelar") }
            }
        )
    }

    if (showEjercicioDialog) {
        AlertDialog(
            onDismissRequest = { showEjercicioDialog = false },
            title = { Text(stringResource(R.string.tutor_ejercicio_dialog)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = enunciadoInput,
                        onValueChange = { enunciadoInput = it },
                        label = { Text(stringResource(R.string.tutor_ejercicio_dialog)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = datosInput,
                        onValueChange = { datosInput = it },
                        label = { Text(stringResource(R.string.tutor_datos_dialog)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showEjercicioDialog = false
                    viewModel.resolverEjercicio(enunciadoInput.trim(), datosInput.trim())
                }) { Text(stringResource(R.string.tutor_enviar)) }
            },
            dismissButton = {
                TextButton(onClick = { showEjercicioDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            FilterChip(
                selected = false,
                onClick = { showFormulaDialog = true },
                enabled = !uiState.escribiendo && uiState.iaConfigurada,
                label = { Text(stringResource(R.string.tutor_explicar_formula), maxLines = 1) }
            )
            FilterChip(
                selected = false,
                onClick = { showConceptoDialog = true },
                enabled = !uiState.escribiendo && uiState.iaConfigurada,
                label = { Text(stringResource(R.string.tutor_explicar_concepto), maxLines = 1) }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(
                selected = false,
                onClick = { showEjercicioDialog = true },
                enabled = !uiState.escribiendo && uiState.iaConfigurada,
                label = { Text(stringResource(R.string.tutor_resolver_ejercicio), maxLines = 1) }
            )
            FilterChip(
                selected = false,
                onClick = { viewModel.analizarUltimoResultado() },
                enabled = !uiState.escribiendo && uiState.iaConfigurada && uiState.ultimoResultadoId != null,
                label = { Text(stringResource(R.string.tutor_analizar_resultado), maxLines = 1) }
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.mensajes.isEmpty()) {
                item { Text(stringResource(R.string.tutor_sin_mensajes), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(uiState.mensajes, key = { it.id }) { msg ->
                ChatBubble(text = msg.texto, isUser = msg.esUsuario)
            }
            if (uiState.escribiendo) {
                item { Text(stringResource(R.string.tutor_escribiendo), style = MaterialTheme.typography.bodySmall) }
            }
        }
        uiState.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        OutlinedTextField(
            value = uiState.pregunta,
            onValueChange = viewModel::onPreguntaChange,
            label = { Text(stringResource(R.string.tutor_chat_hint)) },
            enabled = !uiState.escribiendo && uiState.iaConfigurada,
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = stringResource(R.string.tutor_enviar),
            onClick = { viewModel.enviarMensaje() },
            enabled = !uiState.escribiendo && uiState.iaConfigurada,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun TutorPdfTab(
    viewModel: TutorViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.TutorUiState
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.historialPdf.size, uiState.escribiendo) {
        if (uiState.historialPdf.isNotEmpty()) {
            listState.animateScrollToItem(uiState.historialPdf.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.tutor_pdf_title), style = MaterialTheme.typography.titleMedium)
        if (uiState.documentos.isEmpty()) {
            Text(stringResource(R.string.estudio_sin_documento))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.documentos.forEach { doc ->
                    FilterChip(
                        selected = uiState.documentoSeleccionadoId == doc.id,
                        onClick = { viewModel.seleccionarDocumento(doc.id) },
                        label = { Text(doc.nombre.take(15)) }
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (uiState.historialPdf.isEmpty() && !uiState.escribiendo) {
                item {
                    Text(
                        stringResource(R.string.tutor_pdf_sin_historial),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(uiState.historialPdf, key = { it.id }) { msg ->
                ChatBubble(text = msg.texto, isUser = msg.esUsuario)
            }
            if (uiState.escribiendo) {
                item { Text(stringResource(R.string.tutor_escribiendo), style = MaterialTheme.typography.bodySmall) }
            }
        }
        uiState.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error)
        }
        OutlinedTextField(
            value = uiState.preguntaPdf,
            onValueChange = viewModel::onPreguntaPdfChange,
            label = { Text(stringResource(R.string.tutor_chat_hint)) },
            enabled = !uiState.escribiendo && uiState.iaConfigurada,
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryButton(
            text = stringResource(R.string.tutor_enviar),
            onClick = viewModel::preguntarSobrePdf,
            enabled = !uiState.escribiendo && uiState.iaConfigurada &&
                uiState.documentoSeleccionadoId != null
        )
    }
}
