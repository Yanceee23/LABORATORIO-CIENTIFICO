package com.laboratoriofisico.app.frontend.ui.components

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionViewModel
import com.laboratoriofisico.app.sensores.model.SensorState

@Composable
fun SensorMedicionLifecycleEffect(viewModel: SensorMedicionViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onResume(lifecycleOwner)
                Lifecycle.Event.ON_PAUSE -> viewModel.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun SensorMedicionPermissionEffect(viewModel: SensorMedicionViewModel) {
    val context = LocalContext.current
    var pendingRequest by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.onPermissionResult(results.values.all { it })
        pendingRequest = false
    }

    DisposableEffect(uiState.permisoRequerido, pendingRequest) {
        if (uiState.permisoRequerido && !pendingRequest) {
            val permissions = viewModel.permisosRequeridos()
            val needsRequest = permissions.any {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needsRequest) {
                pendingRequest = true
                launcher.launch(permissions)
            } else {
                viewModel.onPermissionResult(true)
            }
        }
        onDispose { }
    }
}

fun mapSensorMedicionEstado(
    disponible: Boolean,
    permisoRequerido: Boolean,
    sensorState: SensorState,
    midiendo: Boolean,
    completada: Boolean
): SensorUiEstado = when {
    sensorState == SensorState.ERROR -> SensorUiEstado.ERROR
    permisoRequerido || sensorState == SensorState.PERMISSION_DENIED -> SensorUiEstado.PERMISO_REQUERIDO
    !disponible || sensorState == SensorState.DISABLED -> SensorUiEstado.NO_DISPONIBLE
    midiendo -> SensorUiEstado.MIDIENDO
    completada -> SensorUiEstado.COMPLETADA
    else -> SensorUiEstado.LISTO
}
