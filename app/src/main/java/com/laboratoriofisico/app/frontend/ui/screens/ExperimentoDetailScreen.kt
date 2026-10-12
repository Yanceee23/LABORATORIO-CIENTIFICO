package com.laboratoriofisico.app.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.model.EstadoExperimento
import com.laboratoriofisico.app.frontend.ui.components.EmptyStateView
import com.laboratoriofisico.app.frontend.ui.components.ExportShareLaunchedEffect
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.ui.components.LoadingView
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentoDetailScreen(
    viewModel: ExperimentoDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMediciones: (Long) -> Unit,
    onNavigateToCapturaMagnetometro: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    ExportShareLaunchedEffect(
        shareUri = uiState.shareUri,
        shareFileName = uiState.shareFileName,
        mimeType = "application/pdf",
        chooserTitleRes = R.string.experimento_compartir_pdf,
        onShareHandled = viewModel::onShareHandled,
        onShareError = viewModel::onShareError
    )

    LaboratorioScaffold(
        title = stringResource(R.string.experimento_detalle),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    ) { padding ->
        when {
            !uiState.cargado -> {
                LoadingView(modifier = Modifier.padding(padding))
            }
            uiState.experimento == null -> {
                EmptyStateView(
                    message = stringResource(R.string.experimento_no_encontrado),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.titulo,
                        onValueChange = viewModel::onTituloChange,
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.descripcion,
                        onValueChange = viewModel::onDescripcionChange,
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = uiState.estado.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            EstadoExperimento.entries.forEach { estado ->
                                DropdownMenuItem(
                                    text = { Text(estado.name) },
                                    onClick = {
                                        viewModel.onEstadoChange(estado)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    uiState.ubicacion?.let { ubicacion ->
                        Text("Ubicación: ${ubicacion.nombre}")
                    }

                    uiState.ultimoResultado?.let { resultado ->
                        Text(
                            text = stringResource(
                                R.string.experimento_ultimo_resultado,
                                resultado.valorCalculado,
                                resultado.unidad
                            )
                        )
                    }

                    Button(onClick = { viewModel.guardar() }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.guardar))
                    }

                    if (uiState.exportando) {
                        CircularProgressIndicator()
                        Text(stringResource(R.string.experimento_exportando_pdf))
                    }
                    uiState.error?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error)
                    }
                    Button(
                        onClick = viewModel::exportarPdf,
                        enabled = !uiState.exportando,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.experimento_exportar_pdf))
                    }

                    uiState.experimento?.let { experimento ->
                        Button(
                            onClick = { onNavigateToCapturaMagnetometro(experimento.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.captura_magnetometro_btn))
                        }
                        Button(
                            onClick = { onNavigateToMediciones(experimento.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.mediciones_title))
                        }
                    }
                }
            }
        }
    }
}
