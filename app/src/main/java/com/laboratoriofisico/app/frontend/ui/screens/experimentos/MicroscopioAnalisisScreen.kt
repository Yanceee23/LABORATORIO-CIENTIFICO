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
import com.laboratoriofisico.app.backend.hoja.HojaCalculoNotas
import com.laboratoriofisico.app.backend.model.AnalisisMicroscopio
import com.laboratoriofisico.app.backend.model.TipoAnalisisMicroscopio
import com.laboratoriofisico.app.backend.piel.PielCalculoNotas
import com.laboratoriofisico.app.backend.sangre.NeubauerConteo
import com.laboratoriofisico.app.backend.sangre.SangreFormulario
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
            if (uiState.tipo == TipoAnalisisMicroscopio.SANGRE) {
                FormularioSangre(uiState = uiState, viewModel = viewModel)
            } else {
                ExperimentoGuiaCard(guia = guiaMicroscopio(tituloClaro))
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

            if (uiState.tipo == TipoAnalisisMicroscopio.SANGRE) {
                uiState.historial.firstOrNull()?.let { item ->
                    AnalisisIACard(tipo = item.tipoExperimento, analisis = item.analisis)
                }
                CalculadoraNeubauer(uiState = uiState, viewModel = viewModel)
            }

            LabSectionLabel(text = stringResource(R.string.micro_historial))
            if (uiState.historial.isEmpty()) {
                LabSurfaceCard {
                    Text(stringResource(R.string.micro_vacio))
                }
            } else {
                uiState.historial.forEachIndexed { index, item ->
                    val notasLocales = when {
                        uiState.tipo == TipoAnalisisMicroscopio.PIEL && index != 0 ->
                            PielCalculoNotas.bloque(item.analisis)
                        uiState.tipo == TipoAnalisisMicroscopio.HOJA && index != 0 ->
                            HojaCalculoNotas.bloque(item.analisis)
                        else -> null
                    }
                    AnalisisTimelineItem(item = item, notasLocales = notasLocales)
                    if (uiState.tipo == TipoAnalisisMicroscopio.PIEL && index == 0) {
                        CalculadoraPielEscamas(
                            imagenPath = item.imagenPath,
                            analisisGuardado = item.analisis,
                            onGuardar = { bloque -> viewModel.guardarMedidasPiel(item.imagenPath, bloque) }
                        )
                    }
                    if (uiState.tipo == TipoAnalisisMicroscopio.HOJA && index == 0) {
                        CalculadoraHoja(
                            imagenPath = item.imagenPath,
                            analisisGuardado = item.analisis,
                            onGuardar = { bloque -> viewModel.guardarMedidasHoja(item.imagenPath, bloque) }
                        )
                    }
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
private fun AnalisisTimelineItem(
    item: AnalisisMicroscopio,
    notasLocales: String? = null
) {
    val analisisVisible = when (item.tipoExperimento) {
        TipoAnalisisMicroscopio.PIEL -> PielCalculoNotas.descripcionIa(item.analisis)
        TipoAnalisisMicroscopio.HOJA -> HojaCalculoNotas.descripcionIa(item.analisis)
        else -> item.analisis
    }
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = DateTimeUtils.format(item.timestamp),
                style = MaterialTheme.typography.labelMedium
            )
            CapturedImage(path = item.imagenPath)
            AnalisisIACard(tipo = item.tipoExperimento, analisis = analisisVisible)
            notasLocales?.let { notas ->
                Text(text = notas, style = MaterialTheme.typography.bodySmall)
            }
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
private fun FormularioSangre(
    uiState: MicroscopioAnalisisUiState,
    viewModel: MicroscopioAnalisisViewModel
) {
    val glucosaNumero = uiState.glucosa.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 }
    val equivalente = glucosaNumero?.let { SangreFormulario.equivalente(it, uiState.unidadGlucosa) }
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CampoSangre("¿Qué comiste?", uiState.comio, viewModel::onComioSangre, KeyboardType.Text)
            Text("¿A qué hora comiste?", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CampoSangre("Hora", uiState.horaComida, viewModel::onHoraComidaSangre, KeyboardType.Number, Modifier.weight(1f))
                CampoSangre("Min", uiState.minutoComida, viewModel::onMinutoComidaSangre, KeyboardType.Number, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                OpcionTexto("AM", uiState.periodoComida == "AM") { viewModel.onPeriodoComidaSangre("AM") }
                OpcionTexto("PM", uiState.periodoComida == "PM") { viewModel.onPeriodoComidaSangre("PM") }
            }
            Text(
                text = SangreFormulario.hora(uiState.horaComida, uiState.minutoComida, uiState.periodoComida),
                style = MaterialTheme.typography.bodySmall
            )
            CampoSangre("Glucosa en sangre", uiState.glucosa, viewModel::onGlucosaSangre, KeyboardType.Decimal)
            Row(modifier = Modifier.fillMaxWidth()) {
                OpcionTexto("mg/dL", uiState.unidadGlucosa == SangreFormulario.MG_DL) {
                    viewModel.onUnidadGlucosaSangre(SangreFormulario.MG_DL)
                }
                OpcionTexto("mmol/L", uiState.unidadGlucosa == SangreFormulario.MMOL_L) {
                    viewModel.onUnidadGlucosaSangre(SangreFormulario.MMOL_L)
                }
            }
            Text(
                text = if (equivalente == null) {
                    "mg/dL = mmol/L × 18. mmol/L = mg/dL ÷ 18."
                } else {
                    "Equivalente: $equivalente. mg/dL = mmol/L × 18. mmol/L = mg/dL ÷ 18."
                },
                style = MaterialTheme.typography.bodySmall
            )
            Text("¿Haces actividad física?", style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth()) {
                OpcionTexto("Sí", uiState.haceActividad) { viewModel.onHaceActividadSangre(true) }
                OpcionTexto("No", !uiState.haceActividad) { viewModel.onHaceActividadSangre(false) }
            }
            if (uiState.haceActividad) {
                CampoSangre("Minutos por día", uiState.minutosActividad, viewModel::onMinutosActividadSangre, KeyboardType.Number)
                CampoSangre("Tipo de actividad (opcional)", uiState.tipoActividad, viewModel::onTipoActividadSangre, KeyboardType.Text)
            }
        }
    }
}

@Composable
private fun CalculadoraNeubauer(
    uiState: MicroscopioAnalisisUiState,
    viewModel: MicroscopioAnalisisViewModel
) {
    val rojos = uiState.nRojos.toIntOrNull()?.let(NeubauerConteo::rojos)
    val blancos = uiState.nBlancos.toIntOrNull()?.let(NeubauerConteo::blancos)
    val general = NeubauerConteo.general(
        uiState.nGeneral.toIntOrNull() ?: 0,
        uiState.dilucionGeneral.trim().replace(',', '.').toDoubleOrNull() ?: 0.0,
        uiState.areaGeneral.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
    )
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Cálculo educativo, no es un diagnóstico",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Solo vale con cámara de Neubauer y estas diluciones. N sale del conteo de la foto y puedes corregirlo.",
                style = MaterialTheme.typography.bodySmall
            )
            CampoSangre("N glóbulos rojos", uiState.nRojos, viewModel::onNRojosSangre, KeyboardType.Number)
            Text(
                text = "Glóbulos rojos por µL = N × 10.000 (dilución 1:200, 5 cuadros pequeños). ${rojos?.let { NeubauerConteo.texto(it) } ?: "—"}",
                style = MaterialTheme.typography.bodySmall
            )
            CampoSangre("N glóbulos blancos", uiState.nBlancos, viewModel::onNBlancosSangre, KeyboardType.Number)
            Text(
                text = "Glóbulos blancos por µL = N × 50 (dilución 1:20, 4 cuadros grandes). ${blancos?.let { NeubauerConteo.texto(it) } ?: "—"}",
                style = MaterialTheme.typography.bodySmall
            )
            CampoSangre("N general", uiState.nGeneral, viewModel::onNGeneralSangre, KeyboardType.Number)
            CampoSangre("Dilución", uiState.dilucionGeneral, viewModel::onDilucionGeneralSangre, KeyboardType.Decimal)
            CampoSangre("Área contada (mm²)", uiState.areaGeneral, viewModel::onAreaGeneralSangre, KeyboardType.Decimal)
            Text(
                text = "Células/µL = (N × dilución) / (área contada en mm² × 0,1). ${general?.let { NeubauerConteo.texto(it) } ?: "—"}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CampoSangre(
    etiqueta: String,
    valor: String,
    onChange: (String) -> Unit,
    teclado: KeyboardType,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = valor,
        onValueChange = onChange,
        label = { Text(etiqueta) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        modifier = modifier
    )
}

@Composable
private fun OpcionTexto(
    titulo: String,
    seleccionado: Boolean,
    onClick: () -> Unit
) {
    Row {
        RadioButton(selected = seleccionado, onClick = onClick)
        Text(
            text = titulo,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 12.dp, end = 12.dp)
        )
    }
}
