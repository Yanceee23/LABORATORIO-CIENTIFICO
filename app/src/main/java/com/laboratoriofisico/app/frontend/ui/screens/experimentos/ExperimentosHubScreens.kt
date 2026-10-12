package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.CelesteLight
import com.laboratoriofisico.app.core.theme.EstadoChipBg
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark
import com.laboratoriofisico.app.core.theme.SensorAvailableBg
import com.laboratoriofisico.app.core.theme.SensorAvailableText
import com.laboratoriofisico.app.frontend.ui.components.AyudaControl
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.ExperimentoGuiaCard
import com.laboratoriofisico.app.frontend.ui.components.IntroBloque
import com.laboratoriofisico.app.frontend.ui.components.guiaMagneticoLab
import com.laboratoriofisico.app.frontend.ui.components.EstadoChip
import com.laboratoriofisico.app.frontend.ui.components.GraficaLineaCard
import com.laboratoriofisico.app.frontend.ui.components.GraficaPlaceholder
import com.laboratoriofisico.app.frontend.ui.components.HubNavigationCard
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismDirectionCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismGaugeCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismHelpCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismStatusCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismTechnicalDataCard
import com.laboratoriofisico.app.frontend.ui.components.MagnetismTechnicalItem
import com.laboratoriofisico.app.frontend.ui.components.MetricRowCard
import com.laboratoriofisico.app.frontend.ui.components.PhoneTiltIllustration
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.SensorCard
import com.laboratoriofisico.app.frontend.ui.components.SensorDisponibleBadge
import com.laboratoriofisico.app.frontend.ui.components.ValorGrandeCard
import com.laboratoriofisico.app.frontend.ui.components.mapSensorMedicionEstado
import com.laboratoriofisico.app.frontend.viewmodel.MedicionHistorialUi
import com.laboratoriofisico.app.frontend.viewmodel.LaboratorioMagneticoViewModel
import com.laboratoriofisico.app.frontend.viewmodel.MedirHubViewModel
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.permissions.SensorPermissions

@Composable
fun MedirHubScreen(
    viewModel: MedirHubViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var pendingLocation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionResult(SensorType.LOCATION, results.values.all { it })
        if (results.values.all { it }) viewModel.actualizar()
        pendingLocation = false
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume()
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onNavigateBack != null) LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.medir_hub_title),
            subtitle = stringResource(R.string.medir_hub_subtitle_real)
        )
        IntroBloque(
            titulo = stringResource(R.string.guia_medir_titulo),
            cuerpo = stringResource(R.string.guia_medir_cuerpo)
        )

        MedirHubCard(
            label = stringResource(R.string.medir_card_ubicacion),
            ayuda = stringResource(R.string.medir_ayuda_ubicacion),
            icon = Icons.Default.LocationOn,
            disponible = uiState.ubicacionDisponible,
            rows = listOf(
                "Latitud" to uiState.latitud,
                "Longitud" to uiState.longitud,
                "Altitud" to uiState.altitud,
                "Precisión" to uiState.precisionGps
            ),
            onActualizar = {
                val perms = SensorPermissions.requiredPermissions(SensorType.LOCATION)
                val needs = perms.any {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (needs) {
                    pendingLocation = true
                    permissionLauncher.launch(perms)
                } else {
                    viewModel.actualizar()
                }
            }
        )

        MedirHubCard(
            label = stringResource(R.string.medir_card_movimiento),
            ayuda = stringResource(R.string.medir_ayuda_movimiento),
            icon = Icons.Default.Speed,
            disponible = uiState.movimientoDisponible,
            rows = listOf(
                "Velocidad" to uiState.velocidad,
                "Distancia" to uiState.distancia,
                "Tiempo" to uiState.tiempo
            ),
            onActualizar = { viewModel.actualizar() }
        )

        MedirHubCard(
            label = stringResource(R.string.medir_card_acelerometro),
            icon = Icons.Default.Sensors,
            disponible = uiState.accelDisponible,
            ayuda = stringResource(R.string.medir_ayuda_acel),
            rows = listOf(
                stringResource(R.string.medir_eje_x) to uiState.accelX,
                stringResource(R.string.medir_eje_y) to uiState.accelY,
                stringResource(R.string.medir_eje_z) to uiState.accelZ,
                stringResource(R.string.medir_magnitud) to uiState.accelMagnitud
            ),
            onActualizar = { viewModel.actualizar() }
        )

        MedirHubCard(
            label = stringResource(R.string.medir_card_giroscopio),
            icon = Icons.Default.Refresh,
            disponible = uiState.gyroDisponible,
            ayuda = stringResource(R.string.medir_ayuda_giro),
            rows = listOf(
                stringResource(R.string.medir_giro_x) to uiState.gyroXDeg,
                stringResource(R.string.medir_giro_y) to uiState.gyroYDeg,
                stringResource(R.string.medir_giro_z) to uiState.gyroZDeg,
                stringResource(R.string.medir_giro_x_rad) to uiState.gyroXRad,
                stringResource(R.string.medir_giro_y_rad) to uiState.gyroYRad,
                stringResource(R.string.medir_giro_z_rad) to uiState.gyroZRad,
                stringResource(R.string.medir_magnitud) to uiState.gyroMagnitudRad
            ),
            onActualizar = { viewModel.actualizar() }
        )

        MedirHubCard(
            label = stringResource(R.string.medir_card_inclinacion),
            ayuda = stringResource(R.string.medir_ayuda_inclinacion),
            icon = Icons.Default.Sensors,
            disponible = uiState.inclinacionDisponible,
            rows = listOf(
                stringResource(R.string.inclinacion_pitch) to uiState.inclinacionPitch,
                stringResource(R.string.inclinacion_roll) to uiState.inclinacionRoll
            ),
            onActualizar = { viewModel.actualizar() },
            extraContent = { PhoneTiltIllustration(tiltDegrees = uiState.inclinacionGrados) }
        )

        if (uiState.esInvitado) {
            Text(
                text = stringResource(R.string.rol_invitado_sin_historial),
                style = MaterialTheme.typography.bodyMedium,
                color = GraphiteMedium
            )
        }

        if (uiState.puedeVerHistorial) {
            MedirHistorialSection(
                historialCompleto = uiState.historial,
                historialFiltrado = uiState.historial.filter {
                    uiState.filtroSeleccionado == null || it.tipo == uiState.filtroSeleccionado
                },
                filtros = uiState.filtros,
                filtroSeleccionado = uiState.filtroSeleccionado,
                seleccionada = uiState.medicionSeleccionada,
                onFiltro = viewModel::seleccionarFiltro,
                onSeleccionar = viewModel::seleccionarMedicion,
                onEliminar = viewModel::pedirEliminar
            )
        }
    }

    uiState.medicionPendienteEliminar?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelarEliminar,
            title = { Text(stringResource(R.string.medir_historial_eliminar_titulo)) },
            text = { Text(stringResource(R.string.medir_historial_eliminar_mensaje)) },
            confirmButton = {
                TextButton(onClick = viewModel::eliminarPendiente) {
                    Text(stringResource(R.string.eliminar))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelarEliminar) {
                    Text(stringResource(R.string.cancelar))
                }
            }
        )
    }
}

@Composable
private fun MedirHistorialSection(
    historialCompleto: List<MedicionHistorialUi>,
    historialFiltrado: List<MedicionHistorialUi>,
    filtros: List<String>,
    filtroSeleccionado: String?,
    seleccionada: MedicionHistorialUi?,
    onFiltro: (String?) -> Unit,
    onSeleccionar: (MedicionHistorialUi?) -> Unit,
    onEliminar: (MedicionHistorialUi) -> Unit
) {
    LabSectionLabel(text = stringResource(R.string.medir_historial_title))

    if (filtros.isNotEmpty()) {
        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabSectionLabel(text = stringResource(R.string.medir_historial_filtrar_titulo))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MedirFiltroChip(
                        label = stringResource(R.string.medir_historial_filtro_todos),
                        count = historialCompleto.size,
                        selected = filtroSeleccionado == null,
                        onClick = { onFiltro(null) }
                    )
                    filtros.forEach { filtro ->
                        MedirFiltroChip(
                            label = shortFilterLabel(filtro),
                            count = historialCompleto.count { it.tipo == filtro },
                            selected = filtroSeleccionado == filtro,
                            onClick = { onFiltro(filtro) }
                        )
                    }
                }
            }
        }
    }

    if (historialFiltrado.isEmpty()) {
        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabSectionLabel(text = stringResource(R.string.medir_historial_title))
                Text(
                    text = stringResource(R.string.medir_historial_vacio),
                    style = MaterialTheme.typography.bodyMedium,
                    color = GraphiteMedium
                )
            }
        }
    } else {
        historialFiltrado.forEach { item ->
            HistorialMedicionCard(
                item = item,
                expanded = seleccionada?.id == item.id,
                onToggleDetalle = {
                    onSeleccionar(if (seleccionada?.id == item.id) null else item)
                },
                onEliminar = { onEliminar(item) }
            )
        }
    }
}

@Composable
private fun MedirFiltroChip(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (selected) PetrolDark else CelesteLight
    val textColor = if (selected) Color.White else PetrolDark
    val borderColor = if (selected) PetrolDark else GraphiteMedium.copy(alpha = 0.25f)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = count.toString(),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) Color.White.copy(alpha = 0.2f) else EstadoChipBg)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.3.sp),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HistorialFechaBadge(fechaTexto: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(SensorAvailableBg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fechaTexto,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            color = SensorAvailableText,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HistorialMedicionCard(
    item: MedicionHistorialUi,
    expanded: Boolean,
    onToggleDetalle: () -> Unit,
    onEliminar: () -> Unit
) {
    val filas = if (expanded) item.datos else item.resumen
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabSectionLabel(
                    text = item.tipo,
                    modifier = Modifier.weight(1f, fill = false)
                )
                HistorialFechaBadge(fechaTexto = item.fechaTexto)
            }
            filas.forEach { dato ->
                MetricRowCard(label = dato.nombre, value = formatDato(dato))
            }
            SecondaryButton(
                text = if (expanded) {
                    stringResource(R.string.medir_historial_ocultar_detalle)
                } else {
                    stringResource(R.string.medir_historial_ver_detalle)
                },
                onClick = onToggleDetalle
            )
            if (expanded) {
                SecondaryButton(
                    text = stringResource(R.string.eliminar),
                    onClick = onEliminar
                )
            }
        }
    }
}

@Composable
private fun shortFilterLabel(tipo: String): String {
    val lower = tipo.lowercase()
    return when {
        "aceler" in lower -> stringResource(R.string.medir_historial_filtro_corto_aceleracion)
        "caíd" in lower || "caid" in lower -> stringResource(R.string.medir_historial_filtro_corto_caida)
        "péndul" in lower || "pendul" in lower -> stringResource(R.string.medir_historial_filtro_corto_pendulo)
        "reflex" in lower -> stringResource(R.string.medir_historial_filtro_corto_reflexion)
        "sonido" in lower -> stringResource(R.string.medir_historial_filtro_corto_sonido)
        "frecuen" in lower -> stringResource(R.string.medir_historial_filtro_corto_frecuencia)
        "onda" in lower -> stringResource(R.string.medir_historial_filtro_corto_ondas)
        "objeto" in lower -> stringResource(R.string.medir_historial_filtro_corto_objetos)
        "distanc" in lower -> stringResource(R.string.medir_historial_filtro_corto_distancia)
        "movim" in lower -> stringResource(R.string.medir_historial_filtro_corto_movimiento)
        "luz" in lower -> stringResource(R.string.medir_historial_filtro_corto_luz)
        else -> tipo
    }
}

private fun formatDato(dato: com.laboratoriofisico.app.frontend.viewmodel.MedicionDatoUi): String =
    if (dato.unidad.isBlank()) dato.valor else "${dato.valor} ${dato.unidad}"

@Composable
private fun MedirHubCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    disponible: Boolean,
    rows: List<Pair<String, String>>,
    onActualizar: () -> Unit,
    ayuda: String? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LabSectionLabel(text = label)
                SensorDisponibleBadge(disponible = disponible)
            }
            if (ayuda != null) {
                AyudaControl(texto = ayuda)
            }
            extraContent?.invoke()
            rows.forEach { (l, v) -> MetricRowCard(label = l, value = v) }
            SecondaryButton(
                text = stringResource(R.string.accion_actualizar),
                onClick = onActualizar
            )
        }
    }
}

@Composable
fun LaboratorioHubScreen(
    onNavigateBack: (() -> Unit)? = null,
    onExperimentoClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onNavigateBack != null) LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.laboratorio_hub_title),
            subtitle = stringResource(R.string.laboratorio_hub_subtitle)
        )
        IntroBloque(
            titulo = stringResource(R.string.guia_lab_titulo),
            cuerpo = stringResource(R.string.guia_lab_cuerpo)
        )
        HubNavigationCard(
            title = stringResource(R.string.guia_micro_titulo_yogur),
            description = stringResource(R.string.guia_micro_yogur_desc),
            icon = Icons.Default.PhotoCamera,
            onClick = { onExperimentoClick("microscopio-yogur") }
        )
        HubNavigationCard(
            title = stringResource(R.string.guia_micro_titulo_piel),
            description = stringResource(R.string.guia_micro_piel_desc),
            icon = Icons.Default.PhotoCamera,
            onClick = { onExperimentoClick("microscopio-piel") }
        )
        HubNavigationCard(
            title = stringResource(R.string.guia_micro_titulo_hoja),
            description = stringResource(R.string.guia_micro_hoja_desc),
            icon = Icons.Default.PhotoCamera,
            onClick = { onExperimentoClick("microscopio-hoja") }
        )
        HubNavigationCard(
            title = stringResource(R.string.guia_micro_titulo_sangre),
            description = stringResource(R.string.guia_micro_sangre_desc),
            icon = Icons.Default.PhotoCamera,
            onClick = { onExperimentoClick("microscopio-sangre") }
        )
    }
}

@Composable
fun LaboratorioMagneticoScreen(
    viewModel: LaboratorioMagneticoViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.onResume()
                    viewModel.iniciar()
                }
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (onNavigateBack != null) LabBackLink(onClick = onNavigateBack)
        val guia = guiaMagneticoLab()
        LabScreenTitle(title = stringResource(R.string.laboratorio_magnetico_title))
        ExperimentoGuiaCard(guia = guia)

        SensorCard(
            sensorName = stringResource(R.string.sensor_magnetometro_nombre),
            disponible = uiState.disponible && !uiState.permisoRequerido
        )

        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            EstadoChip(
                estado = mapSensorMedicionEstado(
                    disponible = uiState.disponible,
                    permisoRequerido = uiState.permisoRequerido,
                    sensorState = uiState.sensorState,
                    midiendo = uiState.midiendo,
                    completada = !uiState.midiendo && uiState.historial.isNotEmpty()
                )
            )
        }

        ValorGrandeCard(
            valor = uiState.intensidadUt?.let { "%.1f".format(it) } ?: "▬▬▬",
            unidad = "µT",
            descripcion = stringResource(R.string.magnetismo_valor_principal_desc)
        )

        val calibrando = uiState.anomaliaEstado.startsWith("Calibrando")
        MagnetismStatusCard(
            intensityUt = uiState.intensidadUt,
            compassStable = uiState.brujulaValida,
            calibrating = calibrando
        )
        MagnetismGaugeCard(intensityUt = if (calibrando) null else uiState.intensidadUt)
        MagnetismDirectionCard(
            azimuthDegrees = uiState.azimuth,
            direction = uiState.direccion,
            magnetDirection = uiState.imanDireccion
        )
        MagnetismHelpCard()
        MagnetismTechnicalDataCard(
            items = laboratorioMagneticoTechnicalItems(uiState)
        )
        SecondaryButton(
            text = stringResource(R.string.recalibrar_campo_base),
            onClick = { viewModel.recalibrarCampoBase() }
        )
        AyudaControl(texto = guia.ayudaGuardar)

        LabSectionLabel(text = stringResource(R.string.seccion_variacion_campo))
        if (uiState.historial.size >= 2) {
            GraficaLineaCard(
                datos = uiState.historial,
                ejeInicio = "0 s",
                ejeUnidad = "µT",
                ejeFin = "45 s"
            )
        } else {
            GraficaPlaceholder(
                placeholderText = stringResource(R.string.grafica_iniciar_medicion),
                ejeInicio = "0 s",
                ejeUnidad = "µT",
                ejeFin = "45 s"
            )
        }

        BotonAccionPrincipal(
            text = if (uiState.midiendo) stringResource(R.string.accion_detener_medicion) else guia.botonProbar,
            icon = Icons.Outlined.Bolt,
            onClick = { if (uiState.midiendo) viewModel.detener() else viewModel.iniciar() },
            enabled = uiState.disponible
        )
        AyudaControl(texto = if (uiState.midiendo) stringResource(R.string.guia_ayuda_detener) else guia.ayudaProbar)
    }
}

@Composable
private fun laboratorioMagneticoTechnicalItems(
    uiState: com.laboratoriofisico.app.frontend.viewmodel.LaboratorioMagneticoUiState
): List<MagnetismTechnicalItem> = listOf(
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_bx),
        value = uiState.bx,
        description = stringResource(R.string.magnetismo_tecnico_eje_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_by),
        value = uiState.by,
        description = stringResource(R.string.magnetismo_tecnico_eje_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_bz),
        value = uiState.bz,
        description = stringResource(R.string.magnetismo_tecnico_eje_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.magnetismo_tecnico_azimuth),
        value = uiState.azimuth?.let { "%.0f°".format(it) } ?: "N/A",
        description = stringResource(R.string.magnetismo_tecnico_azimuth_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.campo_base),
        value = uiState.campoBase,
        description = stringResource(R.string.magnetismo_tecnico_campo_base_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.campo_actual),
        value = uiState.campoActual,
        description = stringResource(R.string.magnetismo_tecnico_campo_actual_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.delta_campo),
        value = uiState.deltaCampo,
        description = stringResource(R.string.magnetismo_tecnico_delta_desc)
    ),
    MagnetismTechnicalItem(
        label = stringResource(R.string.anomalia_estado),
        value = uiState.anomaliaEstado,
        description = stringResource(R.string.magnetismo_tecnico_estado_desc)
    )
)
