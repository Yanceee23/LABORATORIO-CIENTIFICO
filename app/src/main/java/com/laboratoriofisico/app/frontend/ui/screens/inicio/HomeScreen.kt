package com.laboratoriofisico.app.frontend.ui.screens.inicio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.frontend.ui.components.HubNavigationCard
import com.laboratoriofisico.app.frontend.ui.components.IntroBloque
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton

@Composable
fun HomeScreen(
    onNavigateToExperimentos: () -> Unit,
    onNavigateToMedir: () -> Unit,
    onNavigateToLaboratorio: () -> Unit,
    onNavigateToMagnetico: () -> Unit,
    onNavigateToDocumentos: () -> Unit,
    onNavigateToEstudio: () -> Unit,
    onNavigateToTutor: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        LabScreenTitle(
            title = stringResource(R.string.home_title),
            subtitle = stringResource(R.string.home_subtitle)
        )
        IntroBloque(
            titulo = stringResource(R.string.guia_intro_titulo),
            cuerpo = stringResource(R.string.guia_intro_cuerpo)
        )
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            HubNavigationCard(
                title = stringResource(R.string.home_card_experimentos),
                description = stringResource(R.string.home_card_experimentos_desc),
                icon = Icons.Default.Science,
                onClick = onNavigateToExperimentos
            )
            HubNavigationCard(
                title = stringResource(R.string.home_card_medir),
                description = stringResource(R.string.home_card_medir_desc),
                icon = Icons.Default.Speed,
                onClick = onNavigateToMedir
            )
            HubNavigationCard(
                title = stringResource(R.string.home_card_laboratorio),
                description = stringResource(R.string.home_card_laboratorio_desc),
                icon = Icons.Default.Biotech,
                onClick = onNavigateToLaboratorio
            )
            HubNavigationCard(
                title = stringResource(R.string.home_card_magnetico),
                description = stringResource(R.string.home_card_magnetico_desc),
                icon = Icons.Outlined.Bolt,
                onClick = onNavigateToMagnetico
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabSectionLabel(text = stringResource(R.string.home_seccion_aprendizaje))
            HubNavigationCard(
                title = stringResource(R.string.home_card_documentos),
                description = stringResource(R.string.home_card_documentos_desc),
                icon = Icons.Default.PictureAsPdf,
                onClick = onNavigateToDocumentos
            )
            HubNavigationCard(
                title = stringResource(R.string.home_card_estudio),
                description = stringResource(R.string.home_card_estudio_desc),
                icon = Icons.Default.MenuBook,
                onClick = onNavigateToEstudio
            )
            HubNavigationCard(
                title = stringResource(R.string.home_card_tutor),
                description = stringResource(R.string.home_card_tutor_desc),
                icon = Icons.Default.SmartToy,
                onClick = onNavigateToTutor
            )
        }

        SecondaryButton(
            text = stringResource(R.string.cerrar_sesion),
            onClick = onLogout
        )
    }
}
