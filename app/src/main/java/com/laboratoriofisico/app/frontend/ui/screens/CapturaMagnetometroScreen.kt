package com.laboratoriofisico.app.frontend.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.frontend.ui.components.AyudaControl
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigable
import com.laboratoriofisico.app.frontend.ui.components.ExperimentoGuiaCard
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.ui.components.clasificarMensaje
import com.laboratoriofisico.app.frontend.ui.components.guiaCapturaMagnetica
import com.laboratoriofisico.app.frontend.viewmodel.CapturaMagnetometroViewModel
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.displayLabel

@Composable
fun CapturaMagnetometroScreen(
    viewModel: CapturaMagnetometroViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onPause()
        }
    }

    LaboratorioScaffold(
        title = stringResource(R.string.captura_magnetometro_title),
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
            Text(
                text = stringResource(R.string.captura_magnetometro_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExperimentoGuiaCard(guia = guiaCapturaMagnetica())

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "Estado: ${uiState.state.displayLabel()}")
                    Text(text = "Sensor listo: ${if (uiState.available) "Sí" else "No"}")
                    Text(text = "Lecturas hechas: ${uiState.capturedCount}")

                    uiState.latestReading?.let { reading ->
                        reading.valueX?.let { Text(text = "Lado a lado (X): $it µT") }
                        reading.valueY?.let { Text(text = "Adelante (Y): $it µT") }
                        reading.valueZ?.let { Text(text = "Arriba (Z): $it µT") }
                        Text(text = "Hora: ${DateTimeUtils.format(reading.timestamp)}")
                    } ?: Text(text = stringResource(R.string.sin_lecturas_recientes))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = viewModel::startSensor,
                            enabled = uiState.available && uiState.state != SensorState.ACTIVE
                        ) {
                            Text(stringResource(R.string.guia_captura_boton_iniciar))
                        }
                        Button(
                            onClick = viewModel::stopSensor,
                            enabled = uiState.state == SensorState.ACTIVE
                        ) {
                            Text(stringResource(R.string.accion_detener_medicion))
                        }
                    }
                    AyudaControl(texto = stringResource(R.string.guia_captura_ayuda_iniciar))
                    AyudaControl(texto = stringResource(R.string.guia_captura_ayuda_detener))

                    Button(
                        onClick = viewModel::guardarCaptura,
                        enabled = !uiState.isSaving && uiState.capturedCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (uiState.isSaving) {
                                stringResource(R.string.cargando)
                            } else {
                                stringResource(R.string.captura_guardar_resultado)
                            }
                        )
                    }
                    AyudaControl(texto = stringResource(R.string.guia_ayuda_guardar))

                    if (uiState.isSaving) {
                        EstadoAmigable(
                            tipo = com.laboratoriofisico.app.frontend.ui.components.EstadoAmigableTipo.Cargando,
                            texto = stringResource(R.string.estado_guardando)
                        )
                    }
                    uiState.saveMessage?.let { message ->
                        val (tipo, texto) = clasificarMensaje(message)
                        EstadoAmigable(tipo = tipo, texto = texto)
                    }
                }
            }
        }
    }
}
