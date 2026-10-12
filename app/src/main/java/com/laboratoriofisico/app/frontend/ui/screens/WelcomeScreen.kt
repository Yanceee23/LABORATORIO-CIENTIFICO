package com.laboratoriofisico.app.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.ui.components.PrimaryButton
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    onGuest: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaboratorioScaffold(
        title = stringResource(R.string.welcome_title),
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = GraphiteMedium,
                textAlign = TextAlign.Center
            )
            PrimaryButton(
                text = stringResource(R.string.iniciar_sesion),
                onClick = onLogin,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton(
                text = stringResource(R.string.crear_cuenta),
                onClick = onRegister,
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryButton(
                text = stringResource(R.string.entrar_como_invitado),
                onClick = onGuest,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(R.string.welcome_invitado_nota),
                style = MaterialTheme.typography.bodySmall,
                color = GraphiteMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
