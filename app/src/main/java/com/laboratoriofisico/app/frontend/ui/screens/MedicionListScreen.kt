package com.laboratoriofisico.app.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.frontend.ui.components.EmptyStateView
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.viewmodel.MedicionViewModel

@Composable
fun MedicionListScreen(
    viewModel: MedicionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaboratorioScaffold(
        title = stringResource(R.string.mediciones_title),
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.mostrarDialogoNuevo(true) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.medicion_nueva))
            }
        }
    ) { padding ->
        if (uiState.mediciones.isEmpty()) {
            EmptyStateView(
                message = stringResource(R.string.empty_mediciones),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(uiState.mediciones, key = { it.medicion.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = item.medicion.nombre)
                            Text(text = "${item.medicion.valor} ${item.medicion.unidad}")
                            Text(
                                text = DateTimeUtils.format(item.medicion.timestamp),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            if (item.resultados.isNotEmpty()) {
                                Text(
                                    text = "Resultados: ${item.resultados.size}",
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                item.resultados.forEach { resultado ->
                                    Text(text = "· ${resultado.valorCalculado} ${resultado.unidad}")
                                }
                            }
                            IconButton(onClick = { viewModel.eliminarMedicion(item.medicion) }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.eliminar))
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.mostrarDialogoNuevo) {
        AlertDialog(
            onDismissRequest = { viewModel.mostrarDialogoNuevo(false) },
            title = { Text(stringResource(R.string.medicion_nueva)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.nombreNuevo,
                        onValueChange = viewModel::onNombreChange,
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.valorNuevo,
                        onValueChange = viewModel::onValorChange,
                        label = { Text("Valor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.unidadNueva,
                        onValueChange = viewModel::onUnidadChange,
                        label = { Text("Unidad") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.crearMedicion() }) {
                    Text(stringResource(R.string.crear))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.mostrarDialogoNuevo(false) }) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}
