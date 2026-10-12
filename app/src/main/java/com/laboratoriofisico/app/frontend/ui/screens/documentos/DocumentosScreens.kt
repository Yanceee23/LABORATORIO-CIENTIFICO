package com.laboratoriofisico.app.frontend.ui.screens.documentos

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.model.Documento
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.pdf.DocumentoEstado
import com.laboratoriofisico.app.frontend.ui.components.EmptyStateView
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioCard
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.ui.components.PrimaryButton
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.StatusChip
import com.laboratoriofisico.app.frontend.ui.components.StatusChipType
import com.laboratoriofisico.app.frontend.viewmodel.BuscarDocumentoViewModel
import com.laboratoriofisico.app.frontend.viewmodel.DocumentosListViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ImportarPdfViewModel
import com.laboratoriofisico.app.frontend.viewmodel.PdfViewerTab
import com.laboratoriofisico.app.frontend.viewmodel.PdfViewerViewModel

@Composable
fun DocumentosListScreen(
    viewModel: DocumentosListViewModel,
    onNavigateToImportar: () -> Unit,
    onDocumentoClick: (Long) -> Unit,
    onNavigateToBuscar: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val documentos by viewModel.documentos.collectAsState()

    LaboratorioScaffold(
        title = stringResource(R.string.documentos_title),
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToImportar) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.documentos_importar))
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (documentos.isEmpty()) {
                EmptyStateView(message = stringResource(R.string.documentos_vacio))
            } else {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documentos, key = { it.id }) { doc ->
                        DocumentoListItem(
                            documento = doc,
                            onClick = { onDocumentoClick(doc.id) },
                            onSearch = { onNavigateToBuscar(doc.id) },
                            onDelete = { viewModel.eliminar(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentoListItem(
    documento: Documento,
    onClick: () -> Unit,
    onSearch: () -> Unit,
    onDelete: () -> Unit
) {
    val busquedaDisponible = documento.estadoProcesamiento == DocumentoEstado.LISTO
    LaboratorioCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(documento.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${documento.numeroPaginas} págs · ${DateTimeUtils.format(documento.fechaImportacion)}",
                    style = MaterialTheme.typography.bodySmall
                )
                StatusChip(
                    text = DocumentoEstado.etiqueta(documento.estadoProcesamiento),
                    type = estadoChipType(documento.estadoProcesamiento),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (busquedaDisponible) {
                IconButton(onClick = onSearch) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = stringResource(R.string.pdf_buscar_icon)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.eliminar))
            }
        }
    }
}

@Composable
fun ImportarPdfScreen(
    viewModel: ImportarPdfViewModel,
    onNavigateBack: () -> Unit,
    onImportComplete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importar(it) }
    }

    LaunchedEffect(uiState.documentoImportadoId) {
        uiState.documentoImportadoId?.let { id ->
            onImportComplete(id)
            viewModel.limpiarEstado()
        }
    }

    LaboratorioScaffold(
        title = stringResource(R.string.documentos_importar),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PrimaryButton(
                text = stringResource(R.string.documentos_seleccionar),
                onClick = { launcher.launch(arrayOf("application/pdf")) }
            )
            if (uiState.procesando) {
                LinearProgressIndicator(progress = { uiState.progreso }, modifier = Modifier.fillMaxWidth())
            }
            uiState.mensaje?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun estadoChipType(estado: String): StatusChipType = when (estado) {
    DocumentoEstado.IMPORTANDO, DocumentoEstado.PROCESANDO -> StatusChipType.Processing
    DocumentoEstado.LISTO -> StatusChipType.Success
    DocumentoEstado.SIN_TEXTO -> StatusChipType.Warning
    DocumentoEstado.ERROR -> StatusChipType.Error
    else -> StatusChipType.Processing
}

@Composable
fun PdfViewerScreen(
    viewModel: PdfViewerViewModel,
    onNavigateBack: () -> Unit,
    abrirBusqueda: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(abrirBusqueda) {
        if (abrirBusqueda) {
            viewModel.seleccionarTab(PdfViewerTab.Resultados)
        }
    }

    LaboratorioScaffold(
        title = uiState.documento?.nombre ?: stringResource(R.string.pdf_visor_title),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.pdf_buscar_campo)) },
                placeholder = { Text(stringResource(R.string.pdf_buscar_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.pdf_buscar_icon))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.buscar() }),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(
                text = stringResource(R.string.pdf_buscar),
                onClick = viewModel::buscar,
                enabled = !uiState.buscando
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = uiState.tabActiva == PdfViewerTab.Pdf,
                    onClick = { viewModel.seleccionarTab(PdfViewerTab.Pdf) },
                    label = { Text(stringResource(R.string.pdf_tab_pdf)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                FilterChip(
                    selected = uiState.tabActiva == PdfViewerTab.Resultados,
                    onClick = { viewModel.seleccionarTab(PdfViewerTab.Resultados) },
                    label = {
                        Text(
                            if (uiState.resultados.isNotEmpty()) {
                                "${stringResource(R.string.pdf_tab_resultados)} (${uiState.resultados.size})"
                            } else {
                                stringResource(R.string.pdf_tab_resultados)
                            }
                        )
                    },
                    enabled = uiState.busquedaRealizada,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (uiState.tabActiva) {
                    PdfViewerTab.Pdf -> PdfContenidoTab(uiState = uiState, viewModel = viewModel)
                    PdfViewerTab.Resultados -> PdfResultadosTab(uiState = uiState, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun PdfContenidoTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.PdfViewerUiState,
    viewModel: PdfViewerViewModel
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.pdf_pagina, uiState.paginaActual + 1, uiState.totalPaginas.coerceAtLeast(1))
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                uiState.cargando -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                uiState.error != null -> Text(
                    uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                uiState.bitmap != null -> PdfBitmapView(uiState.bitmap!!)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                text = stringResource(R.string.pdf_anterior),
                onClick = viewModel::paginaAnterior,
                modifier = Modifier.weight(1f)
            )
            SecondaryButton(
                text = stringResource(R.string.pdf_siguiente),
                onClick = viewModel::paginaSiguiente,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PdfResultadosTab(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.PdfViewerUiState,
    viewModel: PdfViewerViewModel
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.buscando) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        if (uiState.resultados.isNotEmpty()) {
            Text(
                stringResource(R.string.pdf_resultados_count, uiState.resultados.size),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                stringResource(R.string.pdf_tocar_resultado),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        if (uiState.resultados.isEmpty() && uiState.query.isNotBlank() && !uiState.buscando && uiState.busquedaRealizada) {
            Text(stringResource(R.string.pdf_sin_resultados, uiState.query))
        }
        if (uiState.resultados.isEmpty() && !uiState.busquedaRealizada && uiState.query.isBlank()) {
            Text(
                stringResource(R.string.pdf_buscar_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f, fill = true),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.resultados, key = { "${it.seccionId}_${it.numeroPagina}_${it.titulo.hashCode()}" }) { r ->
                LaboratorioCard(onClick = { viewModel.irAPagina(r.numeroPagina) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(r.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Pág. ${r.numeroPagina}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextoConResaltado(
                            texto = r.fragmento,
                            query = uiState.query,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfBitmapView(bitmap: Bitmap) {
    val scrollState = rememberScrollState()
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
    )
}

@Composable
fun BuscarEnDocumentoScreen(
    viewModel: BuscarDocumentoViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaboratorioScaffold(
        title = stringResource(R.string.pdf_buscar),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(stringResource(R.string.pdf_buscar_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.buscar() }),
                modifier = Modifier.fillMaxWidth()
            )
            PrimaryButton(text = stringResource(R.string.pdf_buscar), onClick = viewModel::buscar)
            if (uiState.buscando) CircularProgressIndicator()
            if (uiState.resultados.isNotEmpty()) {
                Text(
                    stringResource(R.string.pdf_resultados_count, uiState.resultados.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (uiState.resultados.isEmpty() && uiState.query.isNotBlank() && !uiState.buscando && uiState.busquedaRealizada) {
                Text(stringResource(R.string.pdf_sin_resultados, uiState.query))
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.resultados, key = { it.seccionId }) { r ->
                    LaboratorioCard {
                        Text(r.titulo, style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Pág. ${r.numeroPagina}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextoConResaltado(texto = r.fragmento, query = uiState.query)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextoConResaltado(
    texto: String,
    query: String,
    modifier: Modifier = Modifier
) {
    if (query.isBlank()) {
        Text(texto, style = MaterialTheme.typography.bodySmall, modifier = modifier)
        return
    }
    val resaltado = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
    val annotated = buildAnnotatedString {
        var start = 0
        while (start < texto.length) {
            val idx = texto.indexOf(query, start, ignoreCase = true)
            if (idx < 0) {
                append(texto.substring(start))
                break
            }
            append(texto.substring(start, idx))
            withStyle(SpanStyle(background = resaltado, fontWeight = FontWeight.Bold)) {
                append(texto.substring(idx, idx + query.length))
            }
            start = idx + query.length
        }
    }
    Text(annotated, style = MaterialTheme.typography.bodySmall, modifier = modifier)
}
