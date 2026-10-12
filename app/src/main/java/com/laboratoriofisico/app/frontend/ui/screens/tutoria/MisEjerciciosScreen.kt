package com.laboratoriofisico.app.frontend.ui.screens.tutoria

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.viewmodel.EjercicioConProgreso
import com.laboratoriofisico.app.frontend.viewmodel.MisEjerciciosViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResolverEjercicioViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MisEjerciciosScreen(
    viewModel: MisEjerciciosViewModel,
    onNavigateBack: () -> Unit,
    onAbrirDetalle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val ejercicios by viewModel.ejercicios.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.tutor_mis_ejercicios_title),
            subtitle = stringResource(R.string.tutor_mis_ejercicios_subtitle)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.filtroMateria == null,
                onClick = { viewModel.setFiltro(null) },
                label = { Text(stringResource(R.string.tutor_filtro_todas)) }
            )
            MateriaEjercicio.entries.forEach { materia ->
                FilterChip(
                    selected = uiState.filtroMateria == materia.key,
                    onClick = { viewModel.setFiltro(materia) },
                    label = { Text(materia.key.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        if (ejercicios.isEmpty()) {
            Text(stringResource(R.string.tutor_mis_ejercicios_vacio), color = GraphiteMedium)
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ejercicios, key = { it.ejercicio.id }) { item ->
                    EjercicioListItem(
                        item = item,
                        fecha = dateFormat.format(Date(item.ejercicio.fechaCreacion)),
                        onClick = { onAbrirDetalle(item.ejercicio.id) },
                        onEliminar = { viewModel.pedirEliminar(item.ejercicio) }
                    )
                }
            }
        }
    }

    uiState.pendienteEliminar?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = { Text(stringResource(R.string.tutor_eliminar_titulo)) },
            text = { Text(stringResource(R.string.tutor_eliminar_mensaje)) },
            confirmButton = {
                TextButton(onClick = viewModel::confirmarEliminar) {
                    Text(stringResource(R.string.eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEliminar) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@Composable
private fun EjercicioListItem(
    item: EjercicioConProgreso,
    fecha: String,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    val ejercicio = item.ejercicio
    LabSurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val bmp = remember(ejercicio.fotoPath) {
                ResolverEjercicioViewModel.decodeFoto(ejercicio.fotoPath)
            }
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    contentScale = ContentScale.Crop
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "${ejercicio.materia.key} · ${ejercicio.tema}",
                    color = Graphite
                )
                Text(
                    "${ejercicio.resultado.valor} ${ejercicio.resultado.unidad}",
                    color = GraphiteMedium
                )
                if (item.estrellas > 0) {
                    Text(
                        stringResource(R.string.tutor_ejercicio_estrellas, item.estrellas),
                        color = PetrolDark
                    )
                }
                Text(fecha, color = GraphiteMedium, style = androidx.compose.material3.MaterialTheme.typography.labelSmall)
            }
            TextButton(onClick = onEliminar) {
                Text(stringResource(R.string.eliminar))
            }
        }
    }
}
