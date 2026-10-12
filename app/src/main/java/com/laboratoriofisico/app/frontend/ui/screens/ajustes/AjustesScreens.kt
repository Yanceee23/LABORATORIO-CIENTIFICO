package com.laboratoriofisico.app.frontend.ui.screens.ajustes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.HubNavigationCard
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.StubBanner
import com.laboratoriofisico.app.frontend.viewmodel.ConfiguracionViewModel

@Composable
fun AjustesScreen(
    viewModel: ConfiguracionViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAcercaDe: () -> Unit,
    onNavigateToSensores: () -> Unit,
    onNavigateToUsuario: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.ajustes_title),
            subtitle = stringResource(R.string.ajustes_subtitle)
        )

        HubNavigationCard(
            title = stringResource(R.string.usuario_title),
            description = stringResource(R.string.ajustes_perfil_desc),
            icon = Icons.Default.Person,
            onClick = onNavigateToUsuario
        )
        HubNavigationCard(
            title = stringResource(R.string.sensores_diagnostico_title),
            description = stringResource(R.string.sensores_diagnostico_subtitle),
            icon = Icons.Default.Sensors,
            onClick = onNavigateToSensores
        )
        HubNavigationCard(
            title = stringResource(R.string.acerca_de_title),
            description = stringResource(R.string.acerca_de_descripcion),
            icon = Icons.Default.Info,
            onClick = onNavigateToAcercaDe
        )

        LabSectionLabel(text = stringResource(R.string.ajustes_seccion_preferencias))
        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.ajustes_tema_oscuro), color = Graphite)
                    Switch(checked = uiState.temaOscuro, onCheckedChange = viewModel::onTemaOscuroChange)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.ajustes_unidades_metricas), color = Graphite)
                    Switch(checked = uiState.unidadesMetricas, onCheckedChange = viewModel::onUnidadesMetricasChange)
                }
                OutlinedTextField(
                    value = uiState.idioma,
                    onValueChange = viewModel::onIdiomaChange,
                    label = { Text(stringResource(R.string.ajustes_idioma)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.ajustes_ia_titulo), fontWeight = FontWeight.SemiBold, color = Graphite)
                StubBanner(message = stringResource(R.string.tutor_stub_banner))
            }
        }

        BotonAccionPrincipal(
            text = stringResource(R.string.guardar),
            onClick = { viewModel.guardar() }
        )

        SecondaryButton(
            text = stringResource(R.string.cerrar_sesion),
            onClick = onLogout
        )
    }
}

@Composable
fun AcercaDeScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(title = stringResource(R.string.acerca_de_title))
        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Graphite
                )
                Text(stringResource(R.string.acerca_de_version, "1.0.0"), color = GraphiteMedium)
                Text(
                    text = stringResource(R.string.acerca_de_descripcion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GraphiteMedium
                )
            }
        }
    }
}
