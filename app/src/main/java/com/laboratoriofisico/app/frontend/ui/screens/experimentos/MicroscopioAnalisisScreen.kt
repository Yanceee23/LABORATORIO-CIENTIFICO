package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.model.AnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.core.util.DateTimeUtils
import com.laboratoriofisico.app.frontend.ui.components.AyudaControl
import com.laboratoriofisico.app.frontend.ui.components.BotonAccionPrincipal
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigable
import com.laboratoriofisico.app.frontend.ui.components.EstadoAmigableTipo
import com.laboratoriofisico.app.frontend.ui.components.ExperimentoGuiaCard
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.ResultadoExplicadoCard
import com.laboratoriofisico.app.frontend.ui.components.clasificarMensaje
import com.laboratoriofisico.app.frontend.ui.components.guiaMicroscopio
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.MarkdownAnalysisText
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.UvcMicroscopePreview
import com.laboratoriofisico.app.frontend.viewmodel.MicroscopioAnalisisViewModel

@Composable
fun MicroscopioAnalisisScreen(
    viewModel: MicroscopioAnalisisViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.seleccionarImagenFallback(uri)
    }
    val uvcState = uiState.uvcState

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val tituloClaro = uiState.tipo.tituloClaro()
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(title = tituloClaro)
        ExperimentoGuiaCard(guia = guiaMicroscopio(tituloClaro))

        if (uiState.tipo == TipoAnalisisMicroscopio.SANGRE) {
            DisclaimerSangre()
        }

        LabSurfaceCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LabSectionLabel(text = stringResource(R.string.seccion_preview_camara))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    if (uvcState.usbHostSupported && !uvcState.previewBlocked && cameraPermissionGranted) {
                        UvcMicroscopePreview(
                            manager = viewModel.uvcMicroscopeManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (!uvcState.previewActive) {
                        Text(
                            text = uvcState.previewHint(),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                uvcState.errorMessage?.let { error ->
                    val (tipo, texto) = clasificarMensaje(error)
                    EstadoAmigable(tipo = tipo, texto = texto)
                }
            }
        }

        CameraActions(
            canRequestPermission = uvcState.deviceConnected && !uvcState.permissionGranted,
            canRequestCameraPermission = uvcState.deviceConnected && !cameraPermissionGranted,
            canCapture = uvcState.previewActive && !uvcState.previewBlocked && !uiState.capturando && !uiState.analizando,
            canAnalyze = uiState.ultimaCapturaPath != null && !uiState.capturando && !uiState.analizando,
            fallbackGalleryAvailable = uvcState.fallbackGalleryAvailable,
            onRequestPermission = viewModel::solicitarPermisoUsb,
            onRequestCameraPermission = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
            onCapture = viewModel::capturar,
            onAnalyze = viewModel::analizarCaptura,
            onPickFallback = { galleryLauncher.launch("image/*") }
        )

        if (uiState.tipo == TipoAnalisisMicroscopio.SANGRE) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Checkbox(
                    checked = uiState.incluirHipotesisEducativaSangre,
                    onCheckedChange = viewModel::setIncluirHipotesisEducativaSangre
                )
                Text(
                    text = "Pedir una hipótesis educativa especulativa. No es diagnóstico y no determina ABO/Rh.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        uiState.ultimaCapturaPath?.let { path ->
            LabSectionLabel(text = stringResource(R.string.micro_ultima_captura))
            CapturedImage(path = path)
            Text(
                text = stringResource(R.string.micro_nota_analizar),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (uiState.analizando) {
            EstadoAmigable(
                tipo = EstadoAmigableTipo.Cargando,
                texto = stringResource(R.string.guia_micro_analizando)
            )
        }
        if (uiState.ultimoAnalisisId != null) {
            ResultadoExplicadoCard(texto = stringResource(R.string.guia_micro_significado))
            Text(
                text = stringResource(R.string.micro_consultas_ia, uiState.geminiCallsEnAnalisisActual),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        uiState.mensaje?.let { raw ->
            val (tipo, texto) = clasificarMensaje(raw)
            EstadoAmigable(tipo = tipo, texto = texto)
        }
        uiState.error?.let { raw ->
            val (tipo, texto) = clasificarMensaje(raw)
            EstadoAmigable(
                tipo = if (tipo == EstadoAmigableTipo.Info) EstadoAmigableTipo.Error else tipo,
                texto = texto
            )
        }

        LabSectionLabel(text = stringResource(R.string.micro_historial))
        if (uiState.historial.isEmpty()) {
            LabSurfaceCard {
                Text(stringResource(R.string.micro_vacio))
            }
        } else {
            uiState.historial.forEach { item ->
                AnalisisTimelineItem(item = item)
            }
        }
    }
}

@Composable
private fun CameraActions(
    canRequestPermission: Boolean,
    canRequestCameraPermission: Boolean,
    canCapture: Boolean,
    canAnalyze: Boolean,
    fallbackGalleryAvailable: Boolean,
    onRequestPermission: () -> Unit,
    onRequestCameraPermission: () -> Unit,
    onCapture: () -> Unit,
    onAnalyze: () -> Unit,
    onPickFallback: () -> Unit
) {
    if (canRequestCameraPermission) {
        BotonAccionPrincipal(
            text = stringResource(R.string.micro_permiso_camara),
            icon = Icons.Default.PhotoCamera,
            onClick = onRequestCameraPermission
        )
    }
    if (canRequestPermission) {
        BotonAccionPrincipal(
            text = stringResource(R.string.micro_permiso_usb),
            icon = Icons.Default.PhotoCamera,
            onClick = onRequestPermission
        )
    }
    BotonAccionPrincipal(
        text = stringResource(R.string.guia_micro_boton_foto),
        icon = Icons.Default.PhotoCamera,
        onClick = onCapture,
        enabled = canCapture
    )
    AyudaControl(texto = stringResource(R.string.guia_micro_ayuda_foto))
    if (canAnalyze) {
        SecondaryButton(
            text = stringResource(R.string.guia_micro_boton_analizar),
            onClick = onAnalyze
        )
        AyudaControl(texto = stringResource(R.string.guia_micro_ayuda_analizar))
    }
    if (fallbackGalleryAvailable) {
        SecondaryButton(
            text = stringResource(R.string.guia_micro_boton_galeria),
            onClick = onPickFallback
        )
    }
}

@Composable
private fun AnalisisTimelineItem(item: AnalisisMicroscopio) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = DateTimeUtils.format(item.timestamp),
                style = MaterialTheme.typography.labelMedium
            )
            CapturedImage(path = item.imagenPath)
            AnalisisIACard(tipo = item.tipoExperimento, analisis = item.analisis)
        }
    }
}

@Composable
private fun AnalisisIACard(
    tipo: TipoAnalisisMicroscopio,
    analisis: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = tipo.tituloClaro(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        MarkdownAnalysisText(markdown = analisis)
    }
}

@Composable
private fun TipoAnalisisMicroscopio.tituloClaro(): String = when (this) {
    TipoAnalisisMicroscopio.YOGUR -> stringResource(R.string.guia_micro_titulo_yogur)
    TipoAnalisisMicroscopio.PIEL -> stringResource(R.string.guia_micro_titulo_piel)
    TipoAnalisisMicroscopio.HOJA -> stringResource(R.string.guia_micro_titulo_hoja)
    TipoAnalisisMicroscopio.SANGRE -> stringResource(R.string.guia_micro_titulo_sangre)
}

@Composable
private fun com.laboratoriofisico.app.usb.UvcMicroscopeState.previewHint(): String = when {
    !usbHostSupported -> stringResource(R.string.micro_hint_sin_usb)
    previewBlocked -> stringResource(R.string.micro_hint_bloqueado)
    !deviceConnected -> stringResource(R.string.micro_hint_desconectado)
    !permissionGranted -> stringResource(R.string.micro_hint_permiso)
    else -> stringResource(R.string.micro_hint_preparando)
}

@Composable
private fun CapturedImage(path: String) {
    val bitmap = remember(path) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val sample = maxOf(1, minOf(bounds.outWidth, bounds.outHeight) / 720)
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample }
            )
        }.getOrNull()
    }
    if (bitmap == null) {
        Text(stringResource(R.string.micro_imagen_error))
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Imagen capturada con microscopio",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        )
    }
}

@Composable
private fun DisclaimerSangre() {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null
                )
                Text(
                    text = "Aclaración educativa obligatoria",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Text(
                text = "Este análisis NO es un diagnóstico médico. El tipo de sangre ABO/Rh requiere reactivos químicos y no puede determinarse de forma fiable solo por imagen.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
