package com.laboratoriofisico.app.frontend.ui.screens.resultados

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.ExportShareLaunchedEffect
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.ValorGrandeCard
import com.laboratoriofisico.app.frontend.ui.components.unidadVisible
import com.laboratoriofisico.app.frontend.viewmodel.ResultadoDetailViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResultadosListViewModel

@Composable
fun ResultadosListScreen(
    viewModel: ResultadosListViewModel,
    onNavigateBack: () -> Unit,
    onResultadoClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    ExportShareLaunchedEffect(
        shareUri = uiState.shareUri,
        shareFileName = uiState.shareFileName,
        mimeType = "application/pdf",
        chooserTitleRes = R.string.resultados_compartir_pdf,
        onShareHandled = viewModel::onShareHandled,
        onShareError = viewModel::onShareError
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.resultados_title),
            subtitle = stringResource(R.string.resultados_subtitle)
        )
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChange,
            label = { Text(stringResource(R.string.resultados_buscar)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        if (uiState.exportando) {
            CircularProgressIndicator()
            Text(stringResource(R.string.resultados_exportando_pdf))
        }
        uiState.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }
        BotonAccionPrincipal(
            text = stringResource(R.string.resultados_exportar_pdf),
            icon = Icons.Default.PictureAsPdf,
            enabled = !uiState.exportando && uiState.resultados.isNotEmpty(),
            onClick = viewModel::exportarPdf
        )
    }
}

@Composable
fun ResultadoDetailScreen(
    viewModel: ResultadoDetailViewModel,
    onNavigateBack: () -> Unit,
    onAnalizarConIa: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val resultado by viewModel.resultado.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    ExportShareLaunchedEffect(
        shareUri = uiState.shareUri,
        shareFileName = uiState.shareFileName,
        mimeType = "application/pdf",
        chooserTitleRes = R.string.resultado_compartir_pdf,
        onShareHandled = viewModel::onShareHandled,
        onShareError = viewModel::onShareError
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(title = stringResource(R.string.resultado_detalle_title))

        if (resultado == null) {
            Text(stringResource(R.string.cargando), color = GraphiteMedium)
        } else {
            val r = resultado!!
            ValorGrandeCard(
                valor = "%.2f".format(r.valorCalculado),
                unidad = unidadVisible(r.unidad),
                descripcion = stringResource(R.string.resultado_valor_calculado)
            )
            LabSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.resultado_que_significa),
                        style = MaterialTheme.typography.labelMedium,
                        color = GraphiteMedium
                    )
                    Text(
                        text = notaVisible(r.notas),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Graphite
                    )
                    Text(
                        text = DateTimeUtils.format(r.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (uiState.exportando) {
                CircularProgressIndicator()
                Text(stringResource(R.string.resultado_exportando_pdf))
            }
            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            BotonAccionPrincipal(
                text = stringResource(R.string.resultado_exportar_pdf),
                icon = Icons.Default.Share,
                enabled = !uiState.exportando,
                onClick = viewModel::exportarPdf
            )
            BotonAccionPrincipal(
                text = stringResource(R.string.analizar_con_ia),
                icon = Icons.Default.SmartToy,
                enabled = !uiState.exportando,
                onClick = { onAnalizarConIa(r.id) }
            )
        }
    }
}

private fun notaVisible(notas: String): String {
    if (!notas.contains('=')) return notas.ifBlank { "Medición guardada del teléfono." }
    val parsed = com.laboratoriofisico.app.reportes.ResultadoNotasParser.parse(notas)
    return parsed.valoresTexto["lectura_usuario"]?.takeIf { it.isNotBlank() }
        ?: parsed.valoresTexto["clasificacion"]?.takeIf { it.isNotBlank() }?.let { "Nivel: $it" }
        ?: "Medición guardada del teléfono."
}
