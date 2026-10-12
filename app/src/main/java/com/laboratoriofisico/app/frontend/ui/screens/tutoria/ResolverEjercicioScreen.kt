package com.laboratoriofisico.app.frontend.ui.screens.tutoria

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.backend.model.EjercicioResuelto
import com.laboratoriofisico.app.backend.model.VerificacionEjercicio
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark
import com.laboratoriofisico.app.frontend.ui.components.LabBackLink
import com.laboratoriofisico.app.frontend.ui.components.LabScreenTitle
import com.laboratoriofisico.app.frontend.ui.components.LabSectionLabel
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import com.laboratoriofisico.app.frontend.ui.components.PrimaryButton
import com.laboratoriofisico.app.frontend.ui.components.SecondaryButton
import com.laboratoriofisico.app.frontend.ui.components.ValorGrandeCard
import com.laboratoriofisico.app.frontend.viewmodel.ResolverEjercicioViewModel
import com.laboratoriofisico.app.frontend.viewmodel.ResolverFase
import com.laboratoriofisico.app.tutor.ejercicio.TutorNumberFormat
import com.laboratoriofisico.app.tutor.ejercicio.paraMostrar
import kotlin.math.max
import kotlin.math.min

@Composable
fun ResolverEjercicioScreen(
    viewModel: ResolverEjercicioViewModel,
    onNavigateBack: () -> Unit,
    onAprenderJugando: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var pendingCamera by remember { mutableStateOf(false) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val takePicture = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            cropBitmap = bitmap
        }
    }

    val pickGallery = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let(viewModel::onFotoDesdeUri)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && pendingCamera) {
            pendingCamera = false
            takePicture.launch()
        } else {
            pendingCamera = false
        }
    }

    fun launchCamera() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            takePicture.launch()
        } else {
            pendingCamera = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(
            title = stringResource(R.string.tutor_resolver_foto_title),
            subtitle = stringResource(R.string.tutor_resolver_foto_subtitle)
        )

        when (uiState.fase) {
            ResolverFase.CAPTURA -> {
                CapturaSection(
                    fotoPath = uiState.fotoPath,
                    analizando = uiState.analizando,
                    onCamera = { launchCamera() },
                    onGallery = { pickGallery.launch("image/*") },
                    onAnalizar = viewModel::analizarFoto
                )
            }
            ResolverFase.ENUNCIADO -> {
                EnunciadoSection(
                    fotoPath = uiState.fotoPath,
                    enunciado = uiState.enunciadoEditable,
                    analizando = uiState.analizando,
                    onEnunciadoChange = viewModel::onEnunciadoChange,
                    onContinuar = viewModel::aceptarEnunciadoYVerSolucion,
                    onResolverCorregido = viewModel::resolverConEnunciadoCorregido
                )
            }
            ResolverFase.SOLUCION -> {
                uiState.ejercicio?.let { ejercicio ->
                    SolucionSection(
                        ejercicio = ejercicio.paraMostrar(),
                        pasosVisibles = uiState.pasosVisibles,
                        avisoVerificacion = uiState.avisoVerificacion,
                        onSiguientePaso = viewModel::siguientePaso,
                        onVerTodo = viewModel::verTodo,
                        onAprenderJugando = {
                            if (ejercicio.id > 0L) onAprenderJugando(ejercicio.id)
                        }
                    )
                }
            }
        }

        if (uiState.analizando) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text(stringResource(R.string.tutor_ejercicio_cargando))
            }
        }

        uiState.error?.let { error ->
            LabSurfaceCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    if (uiState.canRetry) {
                        PrimaryButton(
                            text = stringResource(R.string.tutor_ejercicio_reintentar),
                            onClick = viewModel::reintentarManual,
                            enabled = !uiState.analizando
                        )
                    }
                }
            }
        }
    }

    cropBitmap?.let { bitmap ->
        SimpleCropDialog(
            bitmap = bitmap,
            onDismiss = { cropBitmap = null },
            onConfirm = { cropped ->
                cropBitmap = null
                viewModel.guardarRecorte(cropped)
                // También fijar path desde bitmap vía ViewModel (guardarRecorte)
            }
        )
    }
}

@Composable
private fun CapturaSection(
    fotoPath: String?,
    analizando: Boolean,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
    onAnalizar: () -> Unit
) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabSectionLabel(text = stringResource(R.string.tutor_ejercicio_foto))
            fotoPath?.let { path ->
                val bmp = remember(path) { ResolverEjercicioViewModel.decodeFoto(path) }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        contentScale = ContentScale.Crop
                    )
                }
            } ?: Text(
                stringResource(R.string.tutor_ejercicio_sin_foto),
                color = GraphiteMedium
            )
            PrimaryButton(
                text = stringResource(R.string.tutor_ejercicio_camara),
                onClick = onCamera,
                enabled = !analizando
            )
            SecondaryButton(
                text = stringResource(R.string.tutor_ejercicio_galeria),
                onClick = onGallery,
                enabled = !analizando
            )
            PrimaryButton(
                text = stringResource(R.string.tutor_ejercicio_analizar),
                onClick = onAnalizar,
                enabled = !analizando && !fotoPath.isNullOrBlank()
            )
        }
    }
}

@Composable
private fun EnunciadoSection(
    fotoPath: String?,
    enunciado: String,
    analizando: Boolean,
    onEnunciadoChange: (String) -> Unit,
    onContinuar: () -> Unit,
    onResolverCorregido: () -> Unit
) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LabSectionLabel(text = stringResource(R.string.tutor_ejercicio_enunciado))
            fotoPath?.let { path ->
                val bmp = remember(path) { ResolverEjercicioViewModel.decodeFoto(path) }
                bmp?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(72.dp)
                            .border(1.dp, GraphiteMedium),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            OutlinedTextField(
                value = enunciado,
                onValueChange = onEnunciadoChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                label = { Text(stringResource(R.string.tutor_ejercicio_enunciado_editable)) }
            )
            Text(
                stringResource(R.string.tutor_ejercicio_enunciado_ayuda),
                style = MaterialTheme.typography.bodySmall,
                color = GraphiteMedium
            )
            PrimaryButton(
                text = stringResource(R.string.tutor_ejercicio_ver_solucion),
                onClick = onContinuar,
                enabled = !analizando
            )
            SecondaryButton(
                text = stringResource(R.string.tutor_ejercicio_resolver_texto),
                onClick = onResolverCorregido,
                enabled = !analizando && enunciado.isNotBlank()
            )
        }
    }
}

@Composable
private fun SolucionSection(
    ejercicio: EjercicioResuelto,
    pasosVisibles: Int,
    avisoVerificacion: String?,
    onSiguientePaso: () -> Unit,
    onVerTodo: () -> Unit,
    onAprenderJugando: () -> Unit
) {
    Text(
        stringResource(R.string.tutor_ejercicio_disclaimer),
        style = MaterialTheme.typography.bodySmall,
        color = GraphiteMedium
    )
    avisoVerificacion?.let {
        LabSurfaceCard {
            Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
        }
    }

    SolucionCard(
        icon = Icons.Outlined.Info,
        titulo = stringResource(R.string.tutor_ejercicio_card_pide)
    ) {
        Text(ejercicio.incognita, color = Graphite, fontWeight = FontWeight.SemiBold)
        Text(
            ejercicio.enunciado,
            color = GraphiteMedium,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "${ejercicio.materia.key} · ${ejercicio.tema}",
            style = MaterialTheme.typography.labelMedium,
            color = GraphiteMedium
        )
    }

    FormulaCard(ejercicio)

    val etiqueta = when (ejercicio.verificacion) {
        VerificacionEjercicio.COINCIDE -> stringResource(R.string.tutor_ejercicio_verificado)
        VerificacionEjercicio.NO_COINCIDE -> stringResource(R.string.tutor_ejercicio_no_coincide)
        VerificacionEjercicio.SIN_VERIFICACION -> stringResource(R.string.tutor_ejercicio_sin_verificar)
    }
    val interpretacion = stringResource(
        R.string.tutor_ejercicio_resultado_interpreta,
        TutorNumberFormat.withUnit(ejercicio.resultado.valor, ejercicio.resultado.unidad)
    )
    ValorGrandeCard(
        valor = TutorNumberFormat.format(ejercicio.resultado.valor),
        unidad = ejercicio.resultado.unidad,
        descripcion = "$etiqueta\n$interpretacion"
    )

    SolucionCard(
        icon = Icons.Outlined.TableChart,
        titulo = stringResource(R.string.tutor_ejercicio_card_datos)
    ) {
        if (ejercicio.datos.isEmpty()) {
            Text(stringResource(R.string.tutor_ejercicio_sin_datos), color = GraphiteMedium)
        } else {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.tutor_ejercicio_col_nombre),
                    Modifier.weight(1.2f),
                    color = GraphiteMedium,
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    stringResource(R.string.tutor_ejercicio_col_valor),
                    Modifier.weight(1f),
                    color = GraphiteMedium,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End
                )
                Text(
                    stringResource(R.string.tutor_ejercicio_col_unidad),
                    Modifier.weight(0.8f),
                    color = GraphiteMedium,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.End
                )
            }
            ejercicio.datos.forEach { d ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(d.nombre, Modifier.weight(1.2f), color = Graphite)
                    Text(
                        TutorNumberFormat.format(d.valor),
                        Modifier.weight(1f),
                        color = Graphite,
                        textAlign = TextAlign.End,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        d.unidad,
                        Modifier.weight(0.8f),
                        color = GraphiteMedium,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }

    ejercicio.pasos.take(pasosVisibles).forEach { paso ->
        SolucionCard(
            icon = Icons.AutoMirrored.Outlined.List,
            titulo = stringResource(R.string.tutor_ejercicio_paso_n, paso.numero, paso.titulo)
        ) {
            Text(paso.explicacion, color = Graphite)
            if (paso.operacion.isNotBlank()) {
                Text(
                    paso.operacion,
                    color = PetrolDark,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        if (pasosVisibles < ejercicio.pasos.size) {
            PrimaryButton(
                text = stringResource(R.string.tutor_ejercicio_siguiente_paso),
                onClick = onSiguientePaso,
                modifier = Modifier.weight(1f)
            )
        }
        SecondaryButton(
            text = stringResource(R.string.tutor_ejercicio_ver_todo),
            onClick = onVerTodo,
            modifier = Modifier.weight(1f),
            enabled = pasosVisibles < ejercicio.pasos.size
        )
    }

    Spacer(Modifier.height(8.dp))
    PrimaryButton(
        text = stringResource(R.string.tutor_ejercicio_aprender_jugando),
        onClick = onAprenderJugando,
        enabled = ejercicio.id > 0L
    )
}

@Composable
private fun FormulaCard(ejercicio: EjercicioResuelto) {
    SolucionCard(
        icon = Icons.Outlined.Functions,
        titulo = stringResource(R.string.tutor_ejercicio_card_formula)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = ejercicio.formulaLegible.ifBlank { "—" },
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 34.sp),
                fontWeight = FontWeight.Bold,
                color = PetrolDark,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        if (ejercicio.explicacionFormula.isNotBlank()) {
            Text(
                ejercicio.explicacionFormula,
                color = Graphite,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (ejercicio.variables.isNotEmpty()) {
            Text(
                stringResource(R.string.tutor_ejercicio_leyenda_variables),
                fontWeight = FontWeight.SemiBold,
                color = PetrolDark,
                style = MaterialTheme.typography.labelLarge
            )
            ejercicio.variables.forEach { v ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        v.simbolo,
                        Modifier.weight(0.35f),
                        fontWeight = FontWeight.Bold,
                        color = PetrolDark
                    )
                    Text(v.nombre, Modifier.weight(1f), color = Graphite)
                    Text(v.unidad, Modifier.weight(0.5f), color = GraphiteMedium, textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
private fun SolucionCard(
    icon: ImageVector,
    titulo: String,
    content: @Composable () -> Unit
) {
    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = PetrolDark, modifier = Modifier.size(20.dp))
                LabSectionLabel(text = titulo)
            }
            content()
        }
    }
}

@Composable
private fun SimpleCropDialog(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (Bitmap) -> Unit
) {
    var leftFrac by remember { mutableFloatStateOf(0.1f) }
    var topFrac by remember { mutableFloatStateOf(0.1f) }
    var rightFrac by remember { mutableFloatStateOf(0.9f) }
    var bottomFrac by remember { mutableFloatStateOf(0.9f) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.tutor_ejercicio_recorte_title)) },
        text = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                leftFrac = (leftFrac + drag.x / w).coerceIn(0f, rightFrac - 0.05f)
                                topFrac = (topFrac + drag.y / h).coerceIn(0f, bottomFrac - 0.05f)
                            }
                        }
                ) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(leftFrac * size.width, topFrac * size.height),
                        size = androidx.compose.ui.geometry.Size(
                            (rightFrac - leftFrac) * size.width,
                            (bottomFrac - topFrac) * size.height
                        ),
                        style = Stroke(width = 3f)
                    )
                }
            }
            Text(
                stringResource(R.string.tutor_ejercicio_recorte_ayuda),
                style = MaterialTheme.typography.bodySmall
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    val l = (leftFrac * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                    val t = (topFrac * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                    val r = (rightFrac * bitmap.width).toInt().coerceIn(l + 1, bitmap.width)
                    val b = (bottomFrac * bitmap.height).toInt().coerceIn(t + 1, bitmap.height)
                    val cropped = Bitmap.createBitmap(bitmap, l, t, r - l, b - t)
                    onConfirm(cropped)
                }
            ) { Text(stringResource(R.string.tutor_ejercicio_usar_recorte)) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancelar))
            }
        }
    )
}
