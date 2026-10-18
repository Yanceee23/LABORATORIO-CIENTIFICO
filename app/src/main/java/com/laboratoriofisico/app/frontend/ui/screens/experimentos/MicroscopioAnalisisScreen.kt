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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.laboratoriofisico.app.backend.model.Medicion
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.sangre.NeubauerConteo
import com.laboratoriofisico.app.backend.sangre.TipoConteoNeubauer
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
import com.laboratoriofisico.app.usb.UvcMicroscopeManager
import com.laboratoriofisico.app.frontend.viewmodel.MicroscopioAnalisisUiState
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
    val mostrarPreview =
        uvcState.usbHostSupported && !uvcState.previewBlocked && cameraPermissionGranted
    // #region agent log
    LaunchedEffect(
        mostrarPreview,
        cameraPermissionGranted,
        uvcState.deviceConnected,
        uvcState.permissionGranted,
        uvcState.previewActive,
        uvcState.previewBlocked,
        uvcState.errorMessage
    ) {
        com.laboratoriofisico.app.usb.UsbDebugNdjson.log(
            hypothesisId = "H1",
            location = "MicroscopioAnalisisScreen.previewGate",
            message = "Decisión de montar el preview UVC",
            data = mapOf(
                "mostrarPreview" to mostrarPreview,
                "cameraGranted" to cameraPermissionGranted,
                "usbHost" to uvcState.usbHostSupported,
                "deviceConnected" to uvcState.deviceConnected,
                "usbPermission" to uvcState.permissionGranted,
                "previewActive" to uvcState.previewActive,
                "previewBlocked" to uvcState.previewBlocked,
                "error" to uvcState.errorMessage
            )
        )
    }
    // #endregion
    // Sin permiso CAMERA el fragmento UVC no se monta (preview blanco). Pedirlo al entrar.
    LaunchedEffect(Unit) {
        if (!cameraPermissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val tituloClaro = uiState.tipo.tituloClaro()
    // El preview UVC va fuera del scroll. TextureView queda blanco en Compose;
    // el fragmento usa SurfaceView para pintar el video en su propia superficie.
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LabBackLink(onClick = onNavigateBack)
            LabScreenTitle(title = tituloClaro)
            ExperimentoGuiaCard(guia = guiaMicroscopio(tituloClaro))
            if (uiState.tipo == TipoAnalisisMicroscopio.SANGRE) {
                DisclaimerSangre()
            }
        }

        PreviewMicroscopio(
            manager = viewModel.uvcMicroscopeManager,
            mostrarPreview = mostrarPreview,
            previewActivo = uvcState.previewActive,
            aviso = uvcState.previewHint(),
            error = uvcState.errorMessage
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                DatosSangreForm(uiState = uiState, viewModel = viewModel)
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
}

@Composable
private fun PreviewMicroscopio(
    manager: UvcMicroscopeManager,
    mostrarPreview: Boolean,
    previewActivo: Boolean,
    aviso: String,
    error: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LabSectionLabel(text = stringResource(R.string.seccion_preview_camara))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            if (mostrarPreview) {
                UvcMicroscopePreview(
                    manager = manager,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (!previewActivo) {
                Text(
                    text = aviso,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        error?.let { raw ->
            val (tipo, texto) = clasificarMensaje(raw)
            EstadoAmigable(tipo = tipo, texto = texto)
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
    previewPhase.isNotBlank() -> previewPhase
    !usbHostSupported -> stringResource(R.string.micro_hint_sin_usb)
    previewBlocked -> stringResource(R.string.micro_hint_bloqueado)
    !deviceConnected -> stringResource(R.string.micro_hint_desconectado)
    !permissionGranted -> "Pidiendo permiso…"
    else -> "Abriendo cámara…"
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
private fun DatosSangreForm(
    uiState: MicroscopioAnalisisUiState,
    viewModel: MicroscopioAnalisisViewModel
) {
    val conteo = viewModel.conteoNeubauer(uiState)
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Datos de la muestra",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            CampoSangre("Edad", uiState.edad, viewModel::onEdadSangre, KeyboardType.Number)
            CampoSangre("Peso", uiState.peso, viewModel::onPesoSangre, KeyboardType.Decimal)
            CampoSangre("Qué comió", uiState.comio, viewModel::onComioSangre, KeyboardType.Text)
            CampoSangre("A qué hora", uiState.horaComida, viewModel::onHoraComidaSangre, KeyboardType.Text)
            CampoSangre("Glucosa (mg/dL)", uiState.glucosa, viewModel::onGlucosaSangre, KeyboardType.Decimal)
            Text(
                text = "Células contadas en la cámara de Neubauer",
                style = MaterialTheme.typography.bodySmall
            )
            OpcionConteo(
                titulo = "Glóbulos rojos 1:200",
                seleccionado = uiState.tipoConteo == TipoConteoNeubauer.ROJOS,
                onClick = { viewModel.onTipoConteoSangre(TipoConteoNeubauer.ROJOS) }
            )
            OpcionConteo(
                titulo = "Glóbulos blancos 1:20",
                seleccionado = uiState.tipoConteo == TipoConteoNeubauer.BLANCOS,
                onClick = { viewModel.onTipoConteoSangre(TipoConteoNeubauer.BLANCOS) }
            )
            OpcionConteo(
                titulo = "Otra dilución",
                seleccionado = uiState.tipoConteo == TipoConteoNeubauer.OTRA,
                onClick = { viewModel.onTipoConteoSangre(TipoConteoNeubauer.OTRA) }
            )
            CampoSangre("N (células contadas)", uiState.celulasContadas, viewModel::onCelulasSangre, KeyboardType.Number)
            if (uiState.tipoConteo == TipoConteoNeubauer.OTRA) {
                CampoSangre("Dilución", uiState.dilucion, viewModel::onDilucionSangre, KeyboardType.Decimal)
                CampoSangre("Volumen contado (µL)", uiState.volumenMicrolitros, viewModel::onVolumenSangre, KeyboardType.Decimal)
            }
            Text(
                text = when {
                    conteo != null -> "Células por µL: ${NeubauerConteo.texto(conteo)}"
                    uiState.celulasContadas.isBlank() -> "Escribe N para ver el conteo."
                    uiState.tipoConteo == TipoConteoNeubauer.OTRA -> "Falta la dilución o el volumen."
                    else -> "N tiene que ser mayor que 0."
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            SecondaryButton(
                text = "Guardar lectura",
                onClick = viewModel::guardarLecturaSangre,
                enabled = !uiState.guardandoLectura
            )
            if (uiState.lecturasSangre.isNotEmpty()) {
                Text(
                    text = "Lecturas guardadas",
                    style = MaterialTheme.typography.titleSmall
                )
                uiState.lecturasSangre.forEach { lectura ->
                    LecturaSangreLinea(lectura)
                }
            }
        }
    }
}

@Composable
private fun CampoSangre(
    etiqueta: String,
    valor: String,
    onChange: (String) -> Unit,
    teclado: KeyboardType
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun OpcionConteo(
    titulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        RadioButton(selected = seleccionado, onClick = onClick)
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun LecturaSangreLinea(lectura: Medicion) {
    Text(
        text = "${DateTimeUtils.format(lectura.timestamp)} · ${lectura.nombre}: ${NeubauerConteo.texto(lectura.valor)} ${lectura.unidad}",
        style = MaterialTheme.typography.bodySmall
    )
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
