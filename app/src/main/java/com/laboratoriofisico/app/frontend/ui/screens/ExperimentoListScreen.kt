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
import com.laboratoriofisico.app.frontend.ui.components.EntityListItem
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoListViewModel

@Composable
fun ExperimentoListScreen(
    viewModel: ExperimentoListViewModel,
    onExperimentoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaboratorioScaffold(
        title = stringResource(R.string.experimentos_title),
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.mostrarDialogoNuevo(true) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.experimento_nuevo))
            }
        }
    ) { padding ->
        if (uiState.experimentos.isEmpty()) {
            EmptyStateView(
                message = stringResource(R.string.empty_experimentos),
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
                items(uiState.experimentos, key = { it.id }) { experimento ->
                    EntityListItem(
                        title = experimento.titulo,
                        subtitle = "${experimento.estado.name} · ${DateTimeUtils.format(experimento.fechaCreacion)}",
                        onClick = { onExperimentoClick(experimento.id) }
                    )
                    IconButton(
                        onClick = { viewModel.eliminarExperimento(experimento) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.eliminar))
                    }
                }
            }
        }
    }

    if (uiState.mostrarDialogoNuevo) {
        AlertDialog(
            onDismissRequest = { viewModel.mostrarDialogoNuevo(false) },
            title = { Text(stringResource(R.string.experimento_nuevo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.tituloNuevo,
                        onValueChange = viewModel::onTituloChange,
                        label = { Text("Título") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uiState.descripcionNueva,
                        onValueChange = viewModel::onDescripcionChange,
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.crearExperimento() }) {
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
