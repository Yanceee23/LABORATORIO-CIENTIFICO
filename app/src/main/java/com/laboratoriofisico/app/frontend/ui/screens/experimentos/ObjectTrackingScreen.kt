package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.DarkRedError
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.TealSuccess
import com.laboratoriofisico.app.frontend.ui.components.AyudaControl
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigable
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigableTipo
import com.laboratoriofisico.app.frontend.ui.components.ExperimentoGuiaCard
import com.laboratoriofisico.app.frontend.ui.components.ResultadoExplicadoCard
import com.laboratoriofisico.app.frontend.ui.components.clasificarMensaje
import com.laboratoriofisico.app.frontend.ui.components.guiaObjeto
import com.laboratoriofisico.app.frontend.ui.components.GraficaLineaCard
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.MetricRowCard
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.SensorCard
import com.laboratoriofisico.app.frontend.viewmodel.ObjectTrackingUiState
import com.laboratoriofisico.app.frontend.viewmodel.ObjectTrackingViewModel
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.permissions.SensorPermissions
import com.laboratoriofisico.app.sensores.util.TrackingOutcome

@Composable
fun ObjectTrackingScreen(
    viewModel: ObjectTrackingViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var permissionRequested by remember { mutableStateOf(false) }
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
        permissionRequested = false
        if (granted) viewModel.iniciar(lifecycleOwner, previewView)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume(lifecycleOwner, previewView)
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.detener()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
            // Evitar que INICIAR quede bajo la barra de navegación inferior.
            .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        val guia = guiaObjeto()
        LabScreenTitle(title = stringResource(R.string.objeto_title))
        ExperimentoGuiaCard(guia = guia)

        SensorCard(
            sensorName = stringResource(R.string.sensor_camara_nombre),
            disponible = uiState.disponible && !uiState.permisoRequerido
        )

        CameraPreviewPanel(
            uiState = uiState,
            previewView = previewView,
            onTap = viewModel::onSeleccionarObjeto
        )

        MetricRowCard(
            label = stringResource(R.string.objeto_estado),
            value = estadoTexto(uiState)
        )

        val pixeles = uiState.desplazamientoPx
        val centimetros = uiState.distanciaCm
        if (pixeles != null && uiState.objetivoSeleccionado) {
            val texto = if (centimetros != null) {
                stringResource(R.string.explica_objeto_cm, pixeles, centimetros)
            } else {
                stringResource(R.string.explica_objeto_px, pixeles)
            }
            ResultadoExplicadoCard(texto = texto)
        }
        if (uiState.guardando) {
            EstadoAmigable(
                tipo = EstadoAmigableTipo.Cargando,
                texto = stringResource(R.string.estado_guardando)
            )
        }
        uiState.mensaje?.let { msg ->
            val (tipo, texto) = clasificarMensaje(msg)
            EstadoAmigable(tipo = tipo, texto = texto)
        }

        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LabSectionLabel(text = stringResource(R.string.objeto_posiciones))
                MetricRowCard(
                    label = stringResource(R.string.objeto_posicion_inicial),
                    value = formatPosition(uiState.initialX, uiState.initialY)
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_posicion_actual),
                    value = formatPosition(uiState.currentX, uiState.currentY)
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_desplazamiento),
                    value = formatLiveDistance(uiState.desplazamientoPx ?: uiState.distanciaPx, uiState.distanciaCm)
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_distancia_total),
                    value = formatLiveDistance(uiState.distanciaTotalPx, uiState.distanciaTotalCm)
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_tiempo),
                    value = if (uiState.objetivoSeleccionado) {
                        "%.2f s".format(uiState.tiempoSegundos)
                    } else {
                        PLACEHOLDER
                    }
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_velocidad_actual),
                    value = formatLiveSpeed(uiState.velocidadPxPorSegundo, uiState) ?: PLACEHOLDER
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_velocidad_media),
                    value = formatLiveSpeed(uiState.velocidadMediaPxPorSegundo, uiState) ?: PLACEHOLDER
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_velocidad_maxima),
                    value = formatLiveSpeed(uiState.velocidadMaximaPxPorSegundo, uiState) ?: PLACEHOLDER
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_veces_movimiento),
                    value = uiState.vecesMovimiento.toString()
                )
                MetricRowCard(
                    label = stringResource(R.string.objeto_distancia_real),
                    value = if (uiState.calibracion != null) {
                        uiState.distanciaCm?.let { "%.2f cm".format(it) } ?: PLACEHOLDER
                    } else {
                        stringResource(R.string.objeto_calibra_para_cm)
                    }
                )
            }
        }

        if (uiState.trayectoria.size >= 2) {
            LabSectionLabel(text = stringResource(R.string.objeto_grafica_x))
            GraficaLineaCard(
                datos = uiState.trayectoria.map { it.x },
                ejeInicio = "0 s",
                ejeUnidad = "px",
                ejeFin = "%.1f s".format(uiState.tiempoSegundos)
            )
            LabSectionLabel(text = stringResource(R.string.objeto_grafica_y))
            GraficaLineaCard(
                datos = uiState.trayectoria.map { it.y },
                ejeInicio = "0 s",
                ejeUnidad = "px",
                ejeFin = "%.1f s".format(uiState.tiempoSegundos)
            )
        }

        uiState.resultado?.let { resultado ->
            LabSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabSectionLabel(text = stringResource(R.string.objeto_resultado))
                    MetricRowCard(
                        label = stringResource(R.string.objeto_estado),
                        value = if (resultado.detectado) {
                            stringResource(R.string.objeto_detectado)
                        } else {
                            stringResource(R.string.objeto_perdido)
                        }
                    )
                    MetricRowCard(
                        label = stringResource(R.string.objeto_tiempo),
                        value = "%.2f s".format(resultado.tiempoSegundos)
                    )
                    MetricRowCard(
                        label = stringResource(R.string.objeto_posicion_inicial),
                        value = formatPosition(resultado.initialX, resultado.initialY)
                    )
                    MetricRowCard(
                        label = stringResource(R.string.objeto_posicion_final),
                        value = formatPosition(resultado.finalX, resultado.finalY)
                    )
                    MetricRowCard(
                        label = stringResource(R.string.objeto_desplazamiento),
                        value = formatDelta(resultado.deltaX, resultado.deltaY)
                    )
                    MetricRowCard(
                        label = stringResource(R.string.objeto_distancia),
                        value = "%.1f px".format(resultado.distanciaPx)
                    )
                    MetricRowCard(
                        label = stringResource(R.string.objeto_velocidad_media),
                        value = resultado.velocidadMediaPxPorSegundo?.let { "%.1f px/s".format(it) }
                            ?: PLACEHOLDER
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.objeto_limitacion),
            style = MaterialTheme.typography.bodySmall,
            color = GraphiteMedium
        )

        BotonAccionPrincipal(
            text = if (uiState.camaraActiva) {
                stringResource(R.string.accion_detener_medicion)
            } else {
                guia.botonProbar
            },
            icon = Icons.Default.PhotoCamera,
            onClick = {
                val granted = SensorPermissions.requiredPermissions(SensorType.CAMERA).all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                when {
                    !uiState.disponible -> Unit
                    !granted -> {
                        permissionRequested = true
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                    uiState.camaraActiva -> viewModel.detener()
                    else -> {
                        // Sincronizar permiso ya concedido antes del bind (mismo flujo preview+medición).
                        viewModel.onPermissionResult(true)
                        viewModel.iniciar(lifecycleOwner, previewView)
                    }
                }
            },
            enabled = uiState.disponible
        )
        AyudaControl(
            texto = if (uiState.camaraActiva) {
                stringResource(R.string.guia_ayuda_detener)
            } else {
                guia.ayudaProbar
            }
        )

        SecondaryButton(
            text = stringResource(R.string.medicion_guardar),
            onClick = { viewModel.guardarResultado() },
            enabled = uiState.puedeGuardar && !uiState.guardando
        )
        AyudaControl(texto = guia.ayudaGuardar)

        SecondaryButton(
            text = stringResource(R.string.descartar),
            onClick = { viewModel.descartarResultado() },
            enabled = uiState.puedeGuardar || uiState.resultado != null || uiState.guardado != null
        )
        AyudaControl(texto = stringResource(R.string.guia_objeto_ayuda_descartar))
    }
}

@Composable
private fun CameraPreviewPanel(
    uiState: ObjectTrackingUiState,
    previewView: PreviewView,
    onTap: (Float, Float) -> Unit
) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LabSectionLabel(text = stringResource(R.string.seccion_preview_camara))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(PREVIEW_ASPECT_RATIO)
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(uiState.camaraActiva) {
                            if (!uiState.camaraActiva) return@pointerInput
                            detectTapGestures { offset ->
                                // Coordenadas normalizadas del preview en pantalla (0..1),
                                // alineadas con LumaFrame ya rotado. No usar meteringPointFactory:
                                // esas coords son del sensor/3A y no coinciden con el frame del tracker.
                                val nx = (offset.x / size.width).coerceIn(0f, 1f)
                                val ny = (offset.y / size.height).coerceIn(0f, 1f)
                                onTap(nx, ny)
                            }
                        }
                )

                LiveMotionOverlay(uiState = uiState)

                // Solo tapar el preview si la cámara aún no está en marcha.
                // El botón real es INICIAR MEDICIÓN (abre preview + tracking); no existe "Iniciar preview".
                if (!uiState.camaraActiva || uiState.hardwareState != CameraHardwareState.RUNNING) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                !uiState.disponible -> stringResource(R.string.sensor_no_disponible)
                                uiState.permisoRequerido ->
                                    stringResource(R.string.camara_permiso_requerido)
                                uiState.camaraActiva &&
                                    (uiState.hardwareState == CameraHardwareState.INITIALIZING ||
                                        uiState.hardwareState == CameraHardwareState.READY) ->
                                    stringResource(R.string.objeto_preview_abriendo)
                                uiState.camaraActiva ->
                                    stringResource(R.string.objeto_preview_abriendo)
                                else -> stringResource(R.string.objeto_preview_inactivo)
                            },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            Text(
                text = when {
                    uiState.identificando -> stringResource(R.string.objeto_identificando)
                    !uiState.descripcionObjeto.isNullOrBlank() -> uiState.descripcionObjeto
                    !uiState.camaraActiva -> stringResource(R.string.objeto_ayuda_iniciar)
                    !uiState.objetivoSeleccionado -> stringResource(R.string.objeto_ayuda_tocar)
                    uiState.estadoSeguimiento == TrackingOutcome.LOST ->
                        stringResource(R.string.objeto_ayuda_perdido)
                    else -> stringResource(R.string.objeto_ayuda_mover)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (uiState.descripcionObjeto.isNullOrBlank()) FontWeight.Normal else FontWeight.SemiBold,
                color = if (uiState.estadoSeguimiento == TrackingOutcome.LOST) {
                    DarkRedError
                } else {
                    Graphite
                }
            )
        }
    }
}

@Composable
private fun LiveMotionOverlay(uiState: ObjectTrackingUiState) {
    if (!uiState.objetivoSeleccionado) return
    Box(modifier = Modifier.fillMaxSize()) {
        ObjectBoxOverlay(uiState = uiState)
        LiveMetricsPanel(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
        )
        DisplacementLabel(
            uiState = uiState,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
        )
    }
}

@Composable
private fun ObjectBoxOverlay(uiState: ObjectTrackingUiState) {
    val markerX = uiState.marcadorX ?: return
    val markerY = uiState.marcadorY ?: return
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(
            x = markerX.coerceIn(0f, 1f) * size.width,
            y = markerY.coerceIn(0f, 1f) * size.height
        )
        val side = minOf(size.width, size.height) * 0.46f
        val half = side / 2f
        val left = (center.x - half).coerceIn(0f, size.width - side)
        val top = (center.y - half).coerceIn(0f, size.height - side)
        val box = androidx.compose.ui.geometry.Size(side, side)
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = box,
            style = Stroke(width = 8f)
        )
        drawRect(
            color = TealSuccess,
            topLeft = Offset(left + 4f, top + 4f),
            size = androidx.compose.ui.geometry.Size(side - 8f, side - 8f),
            style = Stroke(width = 4f)
        )
    }
}

@Composable
private fun LiveMetricsPanel(
    uiState: ObjectTrackingUiState,
    modifier: Modifier = Modifier
) {
    val moving = uiState.enMovimiento
    val stateColor = if (moving) TealSuccess else GraphiteMedium
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (moving) {
                stringResource(R.string.objeto_estado_en_movimiento)
            } else {
                stringResource(R.string.objeto_estado_quieto)
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = stateColor
        )
        OverlayMetric(
            label = stringResource(R.string.objeto_desplazamiento),
            value = formatLiveDistance(uiState.desplazamientoPx ?: uiState.distanciaPx, uiState.distanciaCm)
        )
        OverlayMetric(
            label = stringResource(R.string.objeto_distancia_total),
            value = formatLiveDistance(uiState.distanciaTotalPx, uiState.distanciaTotalCm)
        )
        OverlayMetric(
            label = stringResource(R.string.objeto_velocidad_actual),
            value = formatLiveSpeed(uiState.velocidadPxPorSegundo, uiState) ?: PLACEHOLDER
        )
        OverlayMetric(
            label = stringResource(R.string.objeto_tiempo),
            value = "%.1f s".format(uiState.tiempoSegundos)
        )
        OverlayMetric(
            label = stringResource(R.string.objeto_veces_movimiento),
            value = uiState.vecesMovimiento.toString()
        )
        if (uiState.calibracion == null) {
            Text(
                text = stringResource(R.string.objeto_calibra_para_cm),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }
}

@Composable
private fun OverlayMetric(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.78f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun DisplacementLabel(
    uiState: ObjectTrackingUiState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(TealSuccess.copy(alpha = 0.9f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = formatLiveDistance(uiState.desplazamientoPx ?: uiState.distanciaPx, uiState.distanciaCm),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
private fun estadoTexto(uiState: ObjectTrackingUiState): String = when {
    !uiState.camaraActiva && uiState.resultado == null -> stringResource(R.string.objeto_estado_inactivo)
    uiState.camaraActiva && !uiState.objetivoSeleccionado &&
        uiState.estadoSeguimiento == TrackingOutcome.INSUFFICIENT_CONTRAST ->
        stringResource(R.string.objeto_senal_insuficiente)
    uiState.camaraActiva && !uiState.objetivoSeleccionado ->
        stringResource(R.string.objeto_estado_sin_objetivo)
    uiState.estadoSeguimiento == TrackingOutcome.TRACKED && uiState.enMovimiento ->
        stringResource(R.string.objeto_estado_en_movimiento)
    uiState.estadoSeguimiento == TrackingOutcome.TRACKED -> stringResource(R.string.objeto_estado_quieto)
    uiState.estadoSeguimiento == TrackingOutcome.LOST -> stringResource(R.string.objeto_perdido)
    uiState.estadoSeguimiento == TrackingOutcome.INSUFFICIENT_CONTRAST ->
        stringResource(R.string.objeto_senal_insuficiente)
    else -> stringResource(R.string.objeto_estado_sin_objetivo)
}

private fun formatPosition(x: Int?, y: Int?): String =
    if (x == null || y == null) PLACEHOLDER else "X: $x px · Y: $y px"

private fun formatDelta(x: Int?, y: Int?): String =
    if (x == null || y == null) PLACEHOLDER else "X: %+d px · Y: %+d px".format(x, y)

private fun formatLiveDistance(pixels: Double?, centimeters: Double?): String = when {
    centimeters != null -> "%.1f cm".format(centimeters)
    pixels != null -> "%.1f px".format(pixels)
    else -> PLACEHOLDER
}

private fun formatLiveSpeed(pixelsPerSecond: Double?, uiState: ObjectTrackingUiState): String? {
    val speed = pixelsPerSecond ?: return null
    val calibration = uiState.calibracion
    return if (calibration != null && calibration.isValid) {
        "%.1f cm/s".format(speed / calibration.pixelsPerCm)
    } else {
        "%.1f px/s".format(speed)
    }
}

private const val PLACEHOLDER = "▬▬▬"
private const val PREVIEW_ASPECT_RATIO = 3f / 4f
