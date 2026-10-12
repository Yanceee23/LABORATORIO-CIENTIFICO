package com.laboratoriofisico.app.frontend.ui.screens

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.frontend.ui.components.LaboratorioScaffold
import com.laboratoriofisico.app.frontend.viewmodel.SensorDiagnosticItem
import com.laboratoriofisico.app.frontend.viewmodel.SensorDiagnosticViewModel
import com.laboratoriofisico.app.sensores.model.SamplingRate
import com.laboratoriofisico.app.sensores.model.SensorReading
import com.laboratoriofisico.app.sensores.model.SensorState
import com.laboratoriofisico.app.sensores.model.SensorType
import com.laboratoriofisico.app.sensores.model.displayLabel
import com.laboratoriofisico.app.sensores.permissions.SensorPermissions

@Composable
fun SensorDiagnosticScreen(
    viewModel: SensorDiagnosticViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    var pendingPermissionType by remember { mutableStateOf<SensorType?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val type = pendingPermissionType
        if (type != null) {
            val granted = results.values.all { it }
            viewModel.onPermissionResult(type, granted)
            if (granted) {
                viewModel.startSensor(type)
            }
        }
        pendingPermissionType = null
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onPause()
        }
    }

    LaboratorioScaffold(
        title = stringResource(R.string.sensores_diagnostico_title),
        modifier = modifier,
        onNavigateBack = onNavigateBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.sensores_diagnostico_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SamplingRate.entries.forEach { rate ->
                        FilterChip(
                            selected = uiState.samplingRate == rate,
                            onClick = { viewModel.setSamplingRate(rate) },
                            label = { Text(rate.name) }
                        )
                    }
                }
            }
            items(uiState.sensors, key = { it.type.name }) { item ->
                SensorDiagnosticCard(
                    item = item,
                    onStart = {
                        if (item.requiresPermission) {
                            val permissions = SensorPermissions.requiredPermissions(item.type)
                            val allGranted = permissions.all {
                                ContextCompat.checkSelfPermission(context, it) ==
                                    PackageManager.PERMISSION_GRANTED
                            }
                            if (allGranted) {
                                viewModel.onPermissionResult(item.type, true)
                                viewModel.startSensor(item.type)
                            } else {
                                pendingPermissionType = item.type
                                permissionLauncher.launch(permissions)
                            }
                        } else {
                            viewModel.startSensor(item.type)
                        }
                    },
                    onStop = { viewModel.stopSensor(item.type) },
                    onClear = { viewModel.onClearStored(item.type) }
                )
            }
        }
    }
}

@Composable
private fun SensorDiagnosticCard(
    item: SensorDiagnosticItem,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = item.type.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = if (item.available) {
                    stringResource(R.string.sensor_disponible)
                } else {
                    stringResource(R.string.sensor_no_disponible)
                }
            )
            Text(text = "Estado: ${item.state.displayLabel()}")
            Text(text = "Unidad: ${item.unit}")

            item.latestReading?.let { reading ->
                SensorReadingText(item.type, reading)
            } ?: Text(text = stringResource(R.string.sin_lecturas_recientes))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStart,
                    enabled = item.available && item.state != SensorState.ACTIVE &&
                        item.type != SensorType.CAMERA
                ) {
                    Text("Iniciar")
                }
                Button(
                    onClick = onStop,
                    enabled = item.state == SensorState.ACTIVE
                ) {
                    Text("Detener")
                }
                Button(onClick = onClear) {
                    Text("Limpiar")
                }
            }
            if (item.type == SensorType.CAMERA) {
                Text(
                    text = stringResource(R.string.camara_diagnostico_nota),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun SensorReadingText(type: SensorType, reading: SensorReading) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        when (type) {
            SensorType.ACCELEROMETER -> {
                reading.valueX?.let { Text("X: $it m/s²") }
                reading.valueY?.let { Text("Y: $it m/s²") }
                reading.valueZ?.let { Text("Z: $it m/s²") }
            }
            SensorType.GYROSCOPE -> {
                reading.valueX?.let { Text("X: $it rad/s") }
                reading.valueY?.let { Text("Y: $it rad/s") }
                reading.valueZ?.let { Text("Z: $it rad/s") }
            }
            SensorType.MAGNETOMETER -> {
                reading.valueX?.let { Text("Bx: $it µT") }
                reading.valueY?.let { Text("By: $it µT") }
                reading.valueZ?.let { Text("Bz: $it µT") }
            }
            SensorType.LIGHT -> {
                reading.valueScalar?.let {
                    Text("Valor: $it ${reading.unit}")
                }
            }
            SensorType.LOCATION -> {
                reading.valueX?.let { Text("Lat: $it°") }
                reading.valueY?.let { Text("Lon: $it°") }
                reading.valueZ?.let { Text("Alt: $it m") }
                reading.accuracy?.let { Text("Precisión: ±$it m") }
                reading.speed?.let { Text("Velocidad: $it m/s") }
            }
            SensorType.MICROPHONE -> {
                reading.valueScalar?.let { Text("RMS: $it") }
                reading.valueY?.let { Text("Frecuencia est.: $it Hz") }
            }
            SensorType.CAMERA -> {
                reading.valueX?.let { w ->
                    reading.valueY?.let { h -> Text("Resolución: ${w.toInt()}×${h.toInt()}") }
                }
                reading.valueScalar?.let { Text("Brillo (luma): $it") }
                reading.valueZ?.let { Text("FPS aprox.: $it") }
                reading.metadata?.let { Text("Meta: $it") }
            }
        }
        Text(text = "Timestamp: ${DateTimeUtils.format(reading.timestamp)}")
    }
}
