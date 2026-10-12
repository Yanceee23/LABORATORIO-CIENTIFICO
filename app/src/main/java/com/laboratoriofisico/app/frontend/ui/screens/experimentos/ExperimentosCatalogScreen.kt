package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Waves
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.frontend.ui.components.HubNavigationCard
import com.laboratoriofisico.app.frontend.ui.components.IntroBloque
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle

data class ExperimentoCatalogItem(
    val title: String,
    val description: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun ExperimentosCatalogScreen(
    onNavigateBack: () -> Unit,
    onExperimentoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        ExperimentoCatalogItem(
            title = stringResource(R.string.experimento_medir_sonido),
            description = stringResource(R.string.experimento_medir_sonido_desc),
            route = "sonido",
            icon = Icons.Default.VolumeUp
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.experimento_medir_distancia),
            description = stringResource(R.string.experimento_medir_distancia_desc),
            route = "distancia",
            icon = Icons.Default.Straighten
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.experimento_movimiento),
            description = stringResource(R.string.experimento_movimiento_desc),
            route = "movimiento",
            icon = Icons.Default.DirectionsWalk
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.sensor_physics_aceleracion_title),
            description = stringResource(R.string.sensor_physics_aceleracion_desc),
            route = "aceleracion-objeto",
            icon = Icons.Default.Speed
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.sensor_physics_caida_title),
            description = stringResource(R.string.sensor_physics_caida_desc),
            route = "caida",
            icon = Icons.Default.Timer
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.experimento_camara_title),
            description = stringResource(R.string.experimento_camara_subtitle),
            route = "camara",
            icon = Icons.Default.PhotoCamera
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.objeto_title),
            description = stringResource(R.string.objeto_subtitle),
            route = "objetos",
            icon = Icons.Default.CenterFocusStrong
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.laboratorio_frecuencia_title),
            description = stringResource(R.string.laboratorio_frecuencia_desc),
            route = "frecuencia",
            icon = Icons.Default.MusicNote
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.analizar_ondas_title),
            description = stringResource(R.string.laboratorio_ondas_desc),
            route = "ondas",
            icon = Icons.Default.Waves
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.pendulo_title),
            description = stringResource(R.string.laboratorio_pendulo_desc),
            route = "pendulo",
            icon = Icons.Default.Sensors
        ),
        ExperimentoCatalogItem(
            title = stringResource(R.string.analisis_luz_title),
            description = stringResource(R.string.guia_luz_para_que),
            route = "luz",
            icon = Icons.Default.WbSunny
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.experimentos_title),
            subtitle = stringResource(R.string.experimentos_subtitle)
        )
        IntroBloque(
            titulo = stringResource(R.string.guia_catalogo_titulo),
            cuerpo = stringResource(R.string.guia_catalogo_cuerpo)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                HubNavigationCard(
                    title = item.title,
                    description = item.description,
                    icon = item.icon,
                    onClick = { onExperimentoClick(item.route) }
                )
            }
        }
    }
}
