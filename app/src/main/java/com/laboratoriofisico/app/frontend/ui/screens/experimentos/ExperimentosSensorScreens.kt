package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.frontend.sensor.MedicionEstado
import com.laboratoriofisico.app.frontend.ui.components.LightHistoryPanel
import com.laboratoriofisico.app.frontend.ui.components.MagnetismDirectionCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismGaugeCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismHelpCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismStatusCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismTechnicalDataCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismTechnicalItem
import com.laboratoriofisico.app.frontend.ui.components.MiniMetricCard
import com.laboratoriofisico.app.frontend.ui.components.MetricRowCard
import com.laboratoriofisico.app.frontend.ui.components.SensorExperimentoLayout
import com.laboratoriofisico.app.frontend.ui.components.SensorExperimentoLayoutConfig
import com.laboratoriofisico.app.frontend.ui.components.SensorMedicionLifecycleEffect
import com.laboratoriofisico.app.frontend.ui.components.SensorMedicionPermissionEffect
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoSensorKind
import com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionViewModel
import com.laboratoriofisico.app.sensores.util.SensorValueExtractors

@Composable
fun SensorMedicionScreen(
    viewModel: SensorMedicionViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SensorMedicionLifecycleEffect(viewModel)
    SensorMedicionPermissionEffect(viewModel)
    val uiState by viewModel.uiState.collectAsState()

    when (viewModel.kind) {
        ExperimentoSensorKind.LUZ -> LuzScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.SONIDO -> SonidoScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.MAGNETISMO -> MagnetismoScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.DISTANCIA -> DistanciaScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.MOVIMIENTO -> MovimientoScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.FRECUENCIA -> FrecuenciaScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.ONDAS -> OndasScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.PENDULO -> PenduloScreenContent(viewModel, uiState, onNavigateBack, modifier)
        ExperimentoSensorKind.REFLEXION -> ReflexionScreenContent(viewModel, uiState, onNavigateBack, modifier)
    }
}

@Composable
private fun LuzScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val unidadLuz = if (uiState.lecturaActual?.unit == "raw") {
        stringResource(R.string.unidad_crudo)
    } else {
        stringResource(R.string.unidad_lux)
    }
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.analisis_luz_title),
            sensorName = if (uiState.luzExperimental) {
                stringResource(R.string.sensor_luz_camara_nombre)
            } else {
                stringResource(R.string.sensor_luz_nombre)
            },
            unidad = unidadLuz,
            valorDescripcion = stringResource(R.string.descripcion_intensidad_luminosa),
            botonIcon = Icons.Default.Lightbulb,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_variacion_iluminacion),
            graficaUnidad = unidadLuz,
            usarBarraIntensidadLuz = true,
            contenidoExtra = {
                LightHistoryPanel(entries = uiState.lightHistoryEntries)
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = {
            if (uiState.medicionEstado == MedicionEstado.ACTIVO) {
                viewModel.detener()
            } else {
                viewModel.iniciar(lifecycleOwner)
            }
        },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun SonidoScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.experimento_medir_sonido),
            sensorName = stringResource(R.string.sensor_microfono_nombre),
            unidad = stringResource(R.string.unidad_nivel_microfono),
            valorDescripcion = stringResource(R.string.descripcion_nivel_sonido_relativo),
            botonIcon = Icons.Default.VolumeUp,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionClasificacion = stringResource(R.string.seccion_nivel_sonoro),
            opcionesClasificacion = listOf("Bajo", "Moderado", "Alto"),
            clasificacionSeleccionada = uiState.extras["clasificacion"],
            seccionGrafica = stringResource(R.string.seccion_onda_sonora),
            graficaUnidad = stringResource(R.string.unidad_nivel_microfono),
            contenidoExtra = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.sonido_label_db),
                        value = uiState.extras["nivel_db"] ?: "—",
                        modifier = Modifier.weight(1f),
                        ayuda = stringResource(R.string.sonido_ayuda_db)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.ondas_label_tono),
                        value = uiState.extras["frecuencia_hz"]?.let { "$it Hz" } ?: "—",
                        modifier = Modifier.weight(1f),
                        ayuda = stringResource(R.string.ondas_ayuda_tono)
                    )
                }
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_tiempo),
                    value = uiState.extras["tiempo"] ?: "00:00",
                    ayuda = stringResource(R.string.ondas_ayuda_tiempo)
                )
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun MagnetismoScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.experimento_campo_magnetico),
            sensorName = stringResource(R.string.sensor_magnetometro_nombre),
            unidad = "µT",
            valorDescripcion = stringResource(R.string.magnetismo_valor_principal_desc),
            botonIcon = Icons.Outlined.Bolt,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_variacion_campo),
            graficaUnidad = "µT",
            contenidoExtra = {
                val compassStable = uiState.extras["brujula_valida"] != "interferencia"
                val calibrando = uiState.extras["anomalia_estado"]?.startsWith("Calibrando") == true
                MagnetismStatusCard(
                    intensityUt = uiState.valorPrincipal,
                    compassStable = compassStable,
                    calibrating = calibrando
                )
                MagnetismGaugeCard(intensityUt = if (calibrando) null else uiState.valorPrincipal)
                MagnetismDirectionCard(
                    azimuthDegrees = uiState.extras["azimuth"]?.toDoubleOrNull(),
                    direction = uiState.extras["direccion"] ?: "N/A",
                    magnetDirection = uiState.extras["iman_direccion"] ?: "N/A"
                )
                MagnetismHelpCard()
                MagnetismTechnicalDataCard(
                    items = magnetismoTechnicalItems(uiState.extras)
                )
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun magnetismoTechnicalItems(extras: Map<String, String>): List<MagnetismTechnicalItem> = listOf(
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_bx),
        value = extras["bx"]?.let { "$it µT" } ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_eje_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_by),
        value = extras["by"]?.let { "$it µT" } ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_eje_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_bz),
        value = extras["bz"]?.let { "$it µT" } ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_eje_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_azimuth),
        value = extras["azimuth"]?.let { "$it°" } ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_azimuth_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.campo_base),
        value = extras["campo_base"] ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_campo_base_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.campo_actual),
        value = extras["campo_actual"] ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_campo_actual_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.delta_campo),
        value = extras["delta_campo"] ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_delta_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.anomalia_estado),
        value = extras["anomalia_estado"] ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_estado_desc)
    )
)

@Composable
private fun DistanciaScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.experimento_medir_distancia),
            sensorName = stringResource(R.string.sensor_gps_nombre),
            unidad = "M",
            valorDescripcion = stringResource(R.string.descripcion_distancia_recorrida),
            botonIcon = Icons.Default.Straighten,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_variacion_distancia),
            graficaUnidad = "m",
            contenidoExtra = {
                MetricRowCard(
                    label = stringResource(R.string.distancia_label_velocidad),
                    value = uiState.extras["velocidad"] ?: "—",
                    ayuda = stringResource(R.string.distancia_ayuda_velocidad)
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_physics_tiempo),
                    value = uiState.extras["tiempo"] ?: "00:00",
                    ayuda = stringResource(R.string.ondas_ayuda_tiempo)
                )
                MetricRowCard(
                    label = stringResource(R.string.distancia_label_precision),
                    value = uiState.extras["precision"] ?: "—",
                    ayuda = stringResource(R.string.distancia_ayuda_precision)
                )
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun MovimientoScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.experimento_movimiento),
            sensorName = stringResource(R.string.sensor_acelerometro_nombre),
            unidad = "M/S²",
            valorDescripcion = stringResource(R.string.descripcion_aceleracion_dinamica),
            botonIcon = Icons.AutoMirrored.Filled.DirectionsWalk,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_variacion_movimiento),
            graficaUnidad = "m/s²",
            contenidoExtra = {
                MetricRowCard(
                    label = stringResource(R.string.sensor_eje_x),
                    value = uiState.extras["ax"]?.let { "$it m/s²" } ?: "—",
                    ayuda = stringResource(R.string.sensor_eje_ayuda)
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_eje_y),
                    value = uiState.extras["ay"]?.let { "$it m/s²" } ?: "—",
                    ayuda = stringResource(R.string.sensor_eje_ayuda)
                )
                MetricRowCard(
                    label = stringResource(R.string.sensor_eje_z),
                    value = uiState.extras["az"]?.let { "$it m/s²" } ?: "—",
                    ayuda = stringResource(R.string.sensor_eje_ayuda)
                )
                MetricRowCard(
                    label = stringResource(R.string.movimiento_label_estado),
                    value = uiState.extras["estado_movimiento"] ?: "—",
                    ayuda = stringResource(R.string.movimiento_ayuda_estado)
                )
                MetricRowCard(
                    label = stringResource(R.string.movimiento_label_velocidad),
                    value = uiState.extras["velocidad"] ?: "—",
                    ayuda = stringResource(R.string.distancia_ayuda_velocidad)
                )
                MetricRowCard(
                    label = stringResource(R.string.movimiento_label_tiempo),
                    value = uiState.extras["tiempo"] ?: "00:00",
                    ayuda = stringResource(R.string.ondas_ayuda_tiempo)
                )
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun FrecuenciaScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.laboratorio_frecuencia_title),
            sensorName = stringResource(R.string.sensor_microfono_nombre),
            unidad = "HZ",
            valorDescripcion = stringResource(R.string.descripcion_frecuencia_dominante),
            botonIcon = Icons.Default.VolumeUp,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_variacion_frecuencia),
            graficaUnidad = "Hz"
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun OndasScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.analizar_ondas_title),
            sensorName = stringResource(R.string.sensor_microfono_nombre),
            unidad = stringResource(R.string.unidad_nivel_microfono),
            valorDescripcion = stringResource(R.string.descripcion_nivel_sonido_relativo),
            botonIcon = Icons.Default.VolumeUp,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_onda_sonora),
            graficaUnidad = stringResource(R.string.unidad_nivel_microfono),
            contenidoExtra = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.ondas_label_tono),
                        value = uiState.extras["frecuencia_hz"]?.let { "$it Hz" } ?: "—",
                        modifier = Modifier.weight(1f),
                        ayuda = stringResource(R.string.ondas_ayuda_tono)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.ondas_label_largo),
                        value = uiState.extras["longitud_onda_m"]?.let { "$it m" } ?: "—",
                        modifier = Modifier.weight(1f),
                        ayuda = stringResource(R.string.ondas_ayuda_largo)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.sensor_physics_tiempo),
                        value = uiState.extras["tiempo"] ?: "00:00",
                        modifier = Modifier.weight(1f),
                        ayuda = stringResource(R.string.ondas_ayuda_tiempo)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.ondas_label_fuerza),
                        value = uiState.extras["amplitud_rms"] ?: "—",
                        modifier = Modifier.weight(1f),
                        ayuda = stringResource(R.string.ondas_ayuda_fuerza)
                    )
                }
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun PenduloScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.pendulo_title),
            sensorName = stringResource(R.string.sensor_acelerometro_nombre),
            unidad = "s",
            valorDescripcion = stringResource(R.string.descripcion_periodo_pendulo),
            botonIcon = Icons.Default.Sensors,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionGrafica = stringResource(R.string.seccion_oscilacion_pendulo),
            graficaUnidad = "m/s²",
            graficaDatos = uiState.pendulumSignal,
            contenidoExtra = {
                MetricRowCard(
                    label = stringResource(R.string.pendulo_estado),
                    value = uiState.extras["estado_senal"] ?: "—"
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.sensor_eje_x),
                        value = uiState.extras["ax"]?.let { "$it" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.sensor_eje_y),
                        value = uiState.extras["ay"]?.let { "$it" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.sensor_eje_z),
                        value = uiState.extras["az"]?.let { "$it" } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.pendulo_periodo),
                        value = uiState.pendulum.periodSeconds?.let { "%.2f s".format(it) } ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.pendulo_frecuencia),
                        value = uiState.extras["frecuencia_hz"] ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.pendulo_oscilaciones),
                        value = uiState.extras["oscilaciones"] ?: "0",
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.pendulo_amplitud),
                        value = uiState.extras["amplitud"] ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniMetricCard(
                        label = stringResource(R.string.pendulo_tiempo),
                        value = uiState.extras["tiempo_medicion"] ?: "0.0 s",
                        modifier = Modifier.weight(1f)
                    )
                    MiniMetricCard(
                        label = stringResource(R.string.pendulo_eje),
                        value = uiState.extras["eje_dominante"] ?: "—",
                        modifier = Modifier.weight(1f)
                    )
                }
                var longitudText by remember(uiState.penduloLongitudM) {
                    mutableStateOf(uiState.penduloLongitudM?.toString() ?: "")
                }
                OutlinedTextField(
                    value = longitudText,
                    onValueChange = { text ->
                        longitudText = text
                        viewModel.setPenduloLongitud(text.replace(',', '.').toDoubleOrNull())
                    },
                    label = { androidx.compose.material3.Text(stringResource(R.string.guia_pendulo_largo_label)) },
                    placeholder = { androidx.compose.material3.Text(stringResource(R.string.guia_pendulo_largo_ejemplo)) },
                    supportingText = { androidx.compose.material3.Text(stringResource(R.string.guia_pendulo_largo_ayuda)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                MetricRowCard(
                    label = stringResource(R.string.pendulo_periodo_teorico),
                    value = uiState.extras["periodo_teorico"] ?: "—"
                )
                MetricRowCard(
                    label = stringResource(R.string.pendulo_gravedad_calculada),
                    value = uiState.extras["gravedad_calculada"] ?: "—"
                )
            }
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = { toggleMedicion(viewModel, uiState.medicionEstado) },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

@Composable
private fun ReflexionScreenContent(
    viewModel: SensorMedicionViewModel,
    uiState: com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState,
    onNavigateBack: () -> Unit,
    modifier: Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val unidadLuz = if (uiState.luzExperimental) {
        stringResource(R.string.unidad_crudo)
    } else {
        stringResource(R.string.unidad_lux)
    }
    SensorExperimentoLayout(
        uiState = uiState,
        config = SensorExperimentoLayoutConfig(
            titulo = stringResource(R.string.reflexion_luz_title),
            sensorName = stringResource(R.string.sensor_luz_camara_nombre),
            unidad = unidadLuz,
            valorDescripcion = stringResource(R.string.descripcion_luz_reflejada),
            botonIcon = Icons.Default.Lightbulb,
            botonIniciarText = stringResource(R.string.accion_iniciar_medicion),
            botonDetenerText = stringResource(R.string.accion_detener_medicion),
            seccionClasificacion = stringResource(R.string.seccion_condicion_iluminacion),
            opcionesClasificacion = listOf("Baja", "Moderada", "Alta"),
            clasificacionSeleccionada = uiState.extras["clasificacion"],
            seccionGrafica = stringResource(R.string.seccion_variacion_reflexion),
            graficaUnidad = unidadLuz,
            usarBarraIntensidadLuz = true,
            contenidoExtra = {
                MetricRowCard(
                    label = stringResource(R.string.reflexion_label_inclinacion),
                    value = uiState.extras["angulo_inclinacion"] ?: "—",
                    ayuda = stringResource(R.string.reflexion_ayuda_angulo)
                )
                MetricRowCard(
                    label = stringResource(R.string.reflexion_label_lateral),
                    value = uiState.extras["angulo_lateral"] ?: "—",
                    ayuda = stringResource(R.string.reflexion_ayuda_angulo)
                )
            },
            avisoLimitacion = stringResource(R.string.reflexion_luz_limitacion)
        ),
        onNavigateBack = onNavigateBack,
        onToggleMedicion = {
            if (uiState.medicionEstado == MedicionEstado.ACTIVO) {
                viewModel.detener()
            } else {
                viewModel.iniciar(lifecycleOwner)
            }
        },
        onGuardarResultado = { viewModel.guardarResultado() },
        modifier = modifier
    )
}

private fun toggleMedicion(viewModel: SensorMedicionViewModel, estado: MedicionEstado) {
    if (estado == MedicionEstado.ACTIVO) viewModel.detener() else viewModel.iniciar()
}
