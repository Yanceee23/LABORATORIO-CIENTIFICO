package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PlaceholderGray
import com.laboratoriofisico.app.core.theme.TealSuccess
import com.laboratoriofisico.app.experimentos.sensores.ProcessedSensorExperiment
import com.laboratoriofisico.app.experimentos.sensores.SensorPhysicsExperimentType
import com.laboratoriofisico.app.frontend.ui.components.AyudaControl
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigable
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigableTipo
import com.laboratoriofisico.app.frontend.ui.components.ExperimentoGuiaCard
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.MetricRowCard
import com.laboratoriofisico.app.frontend.ui.components.MiniMetricCard
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.clasificarMensaje
import com.laboratoriofisico.app.frontend.ui.components.guiaAceleracion
import com.laboratoriofisico.app.frontend.ui.components.guiaCaida
import com.laboratoriofisico.app.frontend.ui.components.guiaPara
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoSensorKind
import com.laboratoriofisico.app.frontend.ui.components.StatusChip
import com.laboratoriofisico.app.frontend.ui.components.StatusChipType
import com.laboratoriofisico.app.frontend.viewmodel.SensorPhysicsExperimentViewModel

@Composable
fun SensorPhysicsExperimentScreen(
    viewModel: SensorPhysicsExperimentViewModel,
    type: SensorPhysicsExperimentType,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendulumLengthText by remember { mutableStateOf("") }
    val pendulumLength = pendulumLengthText.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }

    DisposableEffect(viewModel) {
        onDispose {
            if (viewModel.uiState.value.isCapturing) {
                viewModel.cancelar()
            }
        }
    }

    val content = experimentContent(type)
    val guia = when (type) {
        SensorPhysicsExperimentType.OBJECT_ACCELERATION -> guiaAceleracion()
        SensorPhysicsExperimentType.FREE_FALL -> guiaCaida()
        SensorPhysicsExperimentType.PENDULUM -> guiaPara(ExperimentoSensorKind.PENDULO)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(title = content.title)
        ExperimentoGuiaCard(guia = guia)

        BotonAccionPrincipal(
            text = if (uiState.isCapturing) {
                stringResource(R.string.accion_detener_medicion)
            } else {
                guia.botonProbar
            },
            icon = if (uiState.isCapturing) Icons.Default.Stop else Icons.Default.PlayArrow,
            enabled = !uiState.isSaving,
            onClick = {
                if (uiState.isCapturing) {
                    viewModel.detener(pendulumLengthM = pendulumLength)
                } else {
                    viewModel.iniciar(type)
                }
            }
        )
        AyudaControl(
            texto = if (uiState.isCapturing) {
                stringResource(R.string.guia_ayuda_detener)
            } else {
                guia.ayudaProbar
            }
        )

        LabSurfaceCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    LabSectionLabel(text = stringResource(R.string.sensor_label))
                    Text(
                        text = stringResource(R.string.sensor_acelerometro_nombre),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Graphite
                    )
                    Text(
                        text = stringResource(R.string.sensor_physics_real_data_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteMedium
                    )
                }
                StatusChip(
                    text = experimentStateText(uiState.isCapturing, uiState.isSaving, uiState.savedResult != null),
                    type = experimentStateChip(uiState.isCapturing, uiState.isSaving, uiState.savedResult != null)
                )
            }
        }

        val lecturaEnVivo = type != SensorPhysicsExperimentType.PENDULUM
        PrimaryValueCard(
            value = if (lecturaEnVivo) {
                uiState.latestAccelerationMs2
            } else {
                uiState.latestSummary?.primaryValue
            },
            unit = if (lecturaEnVivo) {
                "m/s²"
            } else {
                uiState.latestSummary?.primaryUnit ?: content.primaryUnit
            },
            label = if (lecturaEnVivo) {
                stringResource(R.string.sensor_physics_lectura_acelerometro)
            } else {
                uiState.latestSummary?.primaryName ?: content.primaryLabel
            },
            isCapturing = uiState.isCapturing
        )

        if (lecturaEnVivo) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniMetricCard(
                    label = stringResource(R.string.sensor_eje_x),
                    value = uiState.latestAx?.let { "%.2f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    label = stringResource(R.string.sensor_eje_y),
                    value = uiState.latestAy?.let { "%.2f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
                MiniMetricCard(
                    label = stringResource(R.string.sensor_eje_z),
                    value = uiState.latestAz?.let { "%.2f".format(it) } ?: "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        uiState.latestSummary?.summary?.let { summary ->
            val significado = when (type) {
                SensorPhysicsExperimentType.OBJECT_ACCELERATION ->
                    stringResource(R.string.guia_aceleracion_significado)
                SensorPhysicsExperimentType.FREE_FALL ->
                    if (summary.contains("No se detectó")) {
                        stringResource(R.string.guia_caida_significado_no)
                    } else {
                        stringResource(R.string.guia_caida_significado_si)
                    }
                SensorPhysicsExperimentType.PENDULUM ->
                    stringResource(R.string.guia_pendulo_fisica_significado)
            }
            LabSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabSectionLabel(text = stringResource(R.string.guia_que_significa))
                    Text(text = significado, style = MaterialTheme.typography.bodyLarge, color = Graphite)
                    Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = GraphiteMedium)
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MiniMetricCard(
                label = stringResource(R.string.sensor_physics_tiempo),
                value = "%.1f s".format(uiState.elapsedSeconds),
                modifier = Modifier.weight(1f)
            )
            MiniMetricCard(
                label = stringResource(R.string.sensor_physics_muestras),
                value = uiState.sampleCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        if (type == SensorPhysicsExperimentType.PENDULUM) {
            MiniMetricCard(
                label = stringResource(R.string.sensor_physics_aceleracion_actual),
                value = uiState.latestAccelerationMs2?.let { "%.2f m/s²".format(it) } ?: "—"
            )
        }

        if (type == SensorPhysicsExperimentType.PENDULUM) {
            OutlinedTextField(
                value = pendulumLengthText,
                onValueChange = { pendulumLengthText = it },
                label = { Text(stringResource(R.string.sensor_physics_pendulo_longitud)) },
                placeholder = { Text(stringResource(R.string.guia_pendulo_largo_ejemplo)) },
                supportingText = { Text(stringResource(R.string.sensor_physics_pendulo_longitud_desc)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        uiState.latestSummary?.let { summary ->
            ExperimentMetrics(type = type, summary = summary)
        }

        uiState.savedResult?.let { saved ->
            LabSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(
                        text = stringResource(R.string.sensor_physics_guardado),
                        type = StatusChipType.Success
                    )
                    Text(
                        text = stringResource(
                            R.string.sensor_physics_guardado_detalle,
                            saved.rawReadingsSaved
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteMedium
                    )
                }
            }
        }

        if (uiState.isSaving) {
            EstadoAmigable(
                tipo = EstadoAmigableTipo.Cargando,
                texto = stringResource(R.string.estado_guardando)
            )
        }
        uiState.error?.let { error ->
            val (tipo, texto) = clasificarMensaje(error)
            EstadoAmigable(tipo = tipo, texto = texto)
        }

        SecondaryButton(
            text = stringResource(R.string.medicion_guardar),
            onClick = { viewModel.guardarResultado() },
            enabled = uiState.canSave && !uiState.isSaving
        )
        AyudaControl(texto = guia.ayudaGuardar)

        SecondaryButton(
            text = stringResource(R.string.sensor_physics_cancelar),
            onClick = { viewModel.cancelar() },
            enabled = uiState.isCapturing || uiState.sampleCount > 0 || uiState.savedResult != null
        )
        AyudaControl(texto = stringResource(R.string.guia_ayuda_descartar))
    }
}

@Composable
private fun PrimaryValueCard(
    value: Double?,
    unit: String,
    label: String,
    isCapturing: Boolean
) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = value?.let { "%.2f".format(it) } ?: "▬▬▬",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = if (value == null) PlaceholderGray else Graphite
            )
            Text(text = unit, style = MaterialTheme.typography.titleMedium, color = GraphiteMedium)
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = GraphiteMedium)
            Text(
                text = if (isCapturing) {
                    stringResource(R.string.sensor_physics_midiendo_en_vivo)
                } else {
                    stringResource(R.string.sensor_physics_pulsa_iniciar)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isCapturing) TealSuccess else GraphiteMedium
            )
        }
    }
}

@Composable
private fun ExperimentMetrics(
    type: SensorPhysicsExperimentType,
    summary: ProcessedSensorExperiment
) {
    LabSectionLabel(text = stringResource(R.string.sensor_physics_datos_clave))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (type) {
            SensorPhysicsExperimentType.OBJECT_ACCELERATION -> {
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_aceleracion_maxima),
                    value = summary.numericResults["aceleracion_dinamica_max_ms2"].formatUnit("m/s²")
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_aceleracion_promedio),
                    value = summary.numericResults["aceleracion_dinamica_promedio_ms2"].formatUnit("m/s²")
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_aceleracion_total_maxima),
                    value = summary.numericResults["aceleracion_total_max_ms2"].formatUnit("m/s²")
                )
            }
            SensorPhysicsExperimentType.FREE_FALL -> {
                val detected = summary.numericResults["caida_detectada"] == 1.0
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_caida_detectada),
                    value = if (detected) stringResource(R.string.si) else stringResource(R.string.no)
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_duracion_caida),
                    value = summary.numericResults["duracion_caida_s"].formatUnit("s")
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_aceleracion_minima),
                    value = summary.numericResults["aceleracion_min_ms2"].formatUnit("m/s²")
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_impacto_maximo),
                    value = summary.numericResults["impacto_max_ms2"].formatUnit("m/s²")
                )
            }
            SensorPhysicsExperimentType.PENDULUM -> {
                MetricRowCard(
                    label = stringResource(R.string.pendulo_periodo),
                    value = summary.numericResults["periodo_s"].formatUnit("s")
                )
                MetricRowCard(
                    label = stringResource(R.string.pendulo_frecuencia),
                    value = summary.numericResults["frecuencia_hz"].formatUnit("Hz")
                )
                MetricRowCard(
                    label = stringResource(R.string.pendulo_oscilaciones),
                    value = summary.numericResults["oscilaciones"]?.toInt()?.toString() ?: "0"
                )
                MetricRowCard(
                    label = stringResource(R.string.pendulo_amplitud),
                    value = summary.numericResults["amplitud_ms2"].formatUnit("m/s²")
                )
                summary.numericResults["gravedad_calculada_ms2"]?.let { gravity ->
                    MetricRowCard(
                        label = stringResource(R.string.pendulo_gravedad_calculada),
                        value = "%.2f m/s²".format(gravity)
                    )
                }
                MetricRowCard(
                    label = stringResource(R.string.pendulo_eje),
                    value = summary.textResults["eje_dominante"] ?: "—"
                )
            }
        }
    }
    HorizontalDivider(color = Color.Transparent)
}

private data class ExperimentContent(
    val title: String,
    val subtitle: String,
    val primaryLabel: String,
    val primaryUnit: String
)

@Composable
private fun experimentContent(type: SensorPhysicsExperimentType): ExperimentContent = when (type) {
    SensorPhysicsExperimentType.OBJECT_ACCELERATION -> ExperimentContent(
        title = stringResource(R.string.sensor_physics_aceleracion_title),
        subtitle = stringResource(R.string.sensor_physics_aceleracion_desc),
        primaryLabel = stringResource(R.string.sensor_physics_aceleracion_principal),
        primaryUnit = "m/s²"
    )
    SensorPhysicsExperimentType.FREE_FALL -> ExperimentContent(
        title = stringResource(R.string.sensor_physics_caida_title),
        subtitle = stringResource(R.string.sensor_physics_caida_desc),
        primaryLabel = stringResource(R.string.sensor_physics_caida_principal),
        primaryUnit = "m/s²"
    )
    SensorPhysicsExperimentType.PENDULUM -> ExperimentContent(
        title = stringResource(R.string.pendulo_title),
        subtitle = stringResource(R.string.laboratorio_pendulo_desc),
        primaryLabel = stringResource(R.string.descripcion_periodo_pendulo),
        primaryUnit = "s"
    )
}

private fun Double?.formatUnit(unit: String): String =
    this?.let { "%.2f %s".format(it, unit) } ?: "—"

private fun experimentStateText(capturing: Boolean, saving: Boolean, saved: Boolean): String = when {
    saving -> "Guardando…"
    capturing -> "Midiendo ahora"
    saved -> "Listo, guardado"
    else -> "Listo para probar"
}

private fun experimentStateChip(capturing: Boolean, saving: Boolean, saved: Boolean): StatusChipType = when {
    saving || capturing -> StatusChipType.Processing
    saved -> StatusChipType.Success
    else -> StatusChipType.Stub
}
