package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.frontend.ui.components.AyudaControl
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigable
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigableTipo
import com.laboratoriofisico.app.frontend.ui.components.ExperimentoGuiaCard
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.ResultadoExplicadoCard
import com.laboratoriofisico.app.frontend.ui.components.clasificarMensaje
import com.laboratoriofisico.app.frontend.ui.components.guiaCamara
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.MetricRowCard
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.SensorCard
import com.laboratoriofisico.app.frontend.viewmodel.CameraExperimentViewModel
import com.laboratoriofisico.app.sensores.model.CameraHardwareState
import com.laboratoriofisico.app.sensores.permissions.SensorPermissions
import com.laboratoriofisico.app.sensores.model.SensorType

@Composable
fun CameraExperimentScreen(
    viewModel: CameraExperimentViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    var permissionRequested by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onPermissionResult(granted)
        permissionRequested = false
        if (granted && !uiState.activa) {
            viewModel.iniciar(lifecycleOwner, previewView)
        }
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

    DisposableEffect(uiState.disponible, uiState.permisoRequerido, permissionRequested) {
        if (uiState.disponible && uiState.permisoRequerido && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
        onDispose { }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(title = stringResource(R.string.experimento_camara_title))
        ExperimentoGuiaCard(guia = guiaCamara())

        SensorCard(
            sensorName = stringResource(R.string.sensor_camara_nombre),
            disponible = uiState.disponible && !uiState.permisoRequerido
        )

        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabSectionLabel(text = stringResource(R.string.seccion_preview_camara))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                ) {
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )
                    if (!uiState.activa || uiState.hardwareState != CameraHardwareState.RUNNING) {
                        Text(
                            text = when {
                                !uiState.disponible -> stringResource(R.string.sensor_no_disponible)
                                uiState.permisoRequerido ->
                                    stringResource(R.string.camara_permiso_requerido)
                                uiState.hardwareState == CameraHardwareState.INITIALIZING ->
                                    stringResource(R.string.camara_inicializando)
                                else -> stringResource(R.string.camara_preview_inactivo)
                            },
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        MetricRowCard(
            label = stringResource(R.string.camara_estado),
            value = textoEstadoCamara(uiState.hardwareState)
        )
        if (uiState.hardwareState == CameraHardwareState.RUNNING) {
            ResultadoExplicadoCard(texto = stringResource(R.string.explica_camara))
        }
        MetricRowCard(
            label = stringResource(R.string.camara_lente),
            value = uiState.lensFacing.label
        )
        MetricRowCard(label = stringResource(R.string.camara_resolucion), value = uiState.resolucion)
        MetricRowCard(label = stringResource(R.string.camara_brillo), value = uiState.brillo)
        MetricRowCard(label = stringResource(R.string.camara_fps), value = uiState.fps)
        MetricRowCard(label = stringResource(R.string.camara_formato), value = uiState.formato)
        MetricRowCard(label = stringResource(R.string.camara_ultima_captura), value = uiState.ultimaCaptura)

        if (uiState.capturando) {
            EstadoAmigable(
                tipo = EstadoAmigableTipo.Cargando,
                texto = stringResource(R.string.cargando)
            )
        }
        uiState.mensaje?.let { msg ->
            val (tipo, texto) = clasificarMensaje(msg)
            EstadoAmigable(tipo = tipo, texto = texto)
        }

        Text(
            text = stringResource(R.string.camara_nota_analisis),
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall
        )

        if (uiState.canSwitchToFront && uiState.canSwitchToBack) {
            SecondaryButton(
                text = stringResource(R.string.camara_cambiar_lente),
                onClick = {
                    val granted = SensorPermissions.requiredPermissions(SensorType.CAMERA).all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (granted) {
                        if (!uiState.activa) viewModel.iniciar(lifecycleOwner, previewView)
                        viewModel.cambiarCamara(lifecycleOwner, previewView)
                    }
                }
            )
            AyudaControl(texto = stringResource(R.string.guia_camara_ayuda_lente))
        }

        BotonAccionPrincipal(
            text = if (uiState.activa) {
                stringResource(R.string.accion_detener_medicion)
            } else {
                stringResource(R.string.camara_iniciar_preview)
            },
            icon = Icons.Default.PhotoCamera,
            onClick = {
                val granted = SensorPermissions.requiredPermissions(SensorType.CAMERA).all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                when {
                    !uiState.disponible -> Unit
                    !granted -> permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    uiState.activa -> viewModel.detener()
                    else -> viewModel.iniciar(lifecycleOwner, previewView)
                }
            },
            enabled = uiState.disponible
        )
        AyudaControl(
            texto = if (uiState.activa) {
                stringResource(R.string.guia_ayuda_detener)
            } else {
                stringResource(R.string.guia_camara_ayuda_boton)
            }
        )

        BotonAccionPrincipal(
            text = stringResource(R.string.camara_capturar),
            icon = Icons.Default.PhotoCamera,
            onClick = { viewModel.capturar() },
            enabled = uiState.activa && uiState.hardwareState == CameraHardwareState.RUNNING &&
                !uiState.capturando
        )
        AyudaControl(texto = stringResource(R.string.guia_camara_ayuda_foto))
    }
}

@Composable
private fun textoEstadoCamara(state: CameraHardwareState): String = when (state) {
    CameraHardwareState.UNAVAILABLE,
    CameraHardwareState.NO_CAMERA -> stringResource(R.string.camara_estado_sin_camara)
    CameraHardwareState.PERMISSION_REQUIRED -> stringResource(R.string.camara_estado_permiso)
    CameraHardwareState.INITIALIZING -> stringResource(R.string.camara_estado_abriendo)
    CameraHardwareState.READY -> stringResource(R.string.camara_estado_lista)
    CameraHardwareState.RUNNING -> stringResource(R.string.camara_estado_en_vivo)
    CameraHardwareState.BUSY -> stringResource(R.string.camara_estado_ocupada)
    CameraHardwareState.ERROR -> stringResource(R.string.camara_estado_error)
    CameraHardwareState.CLOSED -> stringResource(R.string.camara_estado_cerrada)
}
