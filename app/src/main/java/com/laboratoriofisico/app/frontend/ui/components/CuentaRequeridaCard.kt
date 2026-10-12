package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium

@Composable
fun CuentaRequeridaCard(
    onCrearCuentaOIniciar: () -> Unit,
    modifier: Modifier = Modifier,
    mensaje: String = stringResource(R.string.rol_bloqueado_laboratorio)
) {
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.rol_acceso_restringido_titulo),
                style = MaterialTheme.typography.titleMedium,
                color = Graphite
            )
            Text(
                text = mensaje,
                style = MaterialTheme.typography.bodyMedium,
                color = GraphiteMedium
            )
            PrimaryButton(
                text = stringResource(R.string.crear_cuenta_o_iniciar),
                onClick = onCrearCuentaOIniciar,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
