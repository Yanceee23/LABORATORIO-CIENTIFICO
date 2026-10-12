package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.AmberWarning
import com.laboratoriofisico.app.core.theme.DarkRedError
import com.laboratoriofisico.app.core.theme.EstadoChipBg
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark
import com.laboratoriofisico.app.core.theme.PlaceholderGray
import com.laboratoriofisico.app.core.theme.SensorAvailableBg
import com.laboratoriofisico.app.core.theme.SensorAvailableDot
import com.laboratoriofisico.app.core.theme.SensorAvailableText
import com.laboratoriofisico.app.core.theme.TealSuccess
import com.laboratoriofisico.app.frontend.sensor.MedicionEstado
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

enum class SensorUiEstado {
    PREPARANDO,
    LISTO,
    MIDIENDO,
    COMPLETADA,
    NO_DISPONIBLE,
    PERMISO_REQUERIDO,
    ERROR
}

fun mapMedicionEstado(
    estado: MedicionEstado,
    sensorDisponible: Boolean,
    permisoRequerido: Boolean = false,
    error: Boolean = false,
    medicionCompletada: Boolean = false
): SensorUiEstado = when {
    error -> SensorUiEstado.ERROR
    permisoRequerido -> SensorUiEstado.PERMISO_REQUERIDO
    !sensorDisponible -> SensorUiEstado.NO_DISPONIBLE
    estado == MedicionEstado.ACTIVO -> SensorUiEstado.MIDIENDO
    estado == MedicionEstado.PAUSADO -> SensorUiEstado.PREPARANDO
    medicionCompletada -> SensorUiEstado.COMPLETADA
    else -> SensorUiEstado.LISTO
}

@Composable
fun SensorCard(
    sensorName: String,
    disponible: Boolean,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                LabSectionLabel(text = stringResource(R.string.sensor_label))
                Text(
                    text = sensorName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Graphite,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            SensorDisponibleBadge(disponible = disponible)
        }
    }
}

@Composable
fun SensorDisponibleBadge(
    disponible: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (disponible) SensorAvailableBg else EstadoChipBg
    val textColor = if (disponible) SensorAvailableText else GraphiteMedium
    val label = if (disponible) {
        stringResource(R.string.sensor_disponible)
    } else {
        stringResource(R.string.sensor_no_disponible)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (disponible) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SensorAvailableDot)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun EstadoChip(
    estado: SensorUiEstado,
    modifier: Modifier = Modifier
) {
    val (bg, fg, labelRes) = when (estado) {
        SensorUiEstado.PREPARANDO -> Triple(EstadoChipBg, GraphiteMedium, R.string.estado_preparando)
        SensorUiEstado.LISTO -> Triple(EstadoChipBg, PetrolDark, R.string.estado_listo)
        SensorUiEstado.MIDIENDO -> Triple(TealSuccess.copy(alpha = 0.15f), TealSuccess, R.string.estado_midiendo)
        SensorUiEstado.COMPLETADA -> Triple(SensorAvailableBg, SensorAvailableText, R.string.estado_completada)
        SensorUiEstado.NO_DISPONIBLE -> Triple(EstadoChipBg, GraphiteMedium, R.string.estado_no_disponible)
        SensorUiEstado.PERMISO_REQUERIDO -> Triple(AmberWarning.copy(alpha = 0.15f), AmberWarning, R.string.estado_permiso)
        SensorUiEstado.ERROR -> Triple(DarkRedError.copy(alpha = 0.12f), DarkRedError, R.string.estado_error)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (estado == SensorUiEstado.LISTO || estado == SensorUiEstado.COMPLETADA) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
            color = fg,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Mayúsculas solo de a–z. µ no se convierte en la letra griega que parece una M. */
fun unidadVisible(unidad: String): String = buildString(unidad.length) {
    unidad.forEach { ch ->
        append(if (ch in 'a'..'z') ch.uppercaseChar() else ch)
    }
}

@Composable
fun ValorGrandeCard(
    valor: String,
    unidad: String,
    descripcion: String,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = valor,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp),
                fontWeight = FontWeight.Bold,
                color = if (valor == "▬▬▬") PlaceholderGray else Graphite
            )
            Text(
                text = unidadVisible(unidad),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = GraphiteMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = GraphiteMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun ClasificacionChips(
    opciones: List<String>,
    seleccionado: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
            val selected = opcion == seleccionado
            Text(
                text = opcion,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) PetrolDark else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (selected) PetrolDark else GraphiteMedium.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(vertical = 10.dp),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) Color.White else GraphiteMedium,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
fun HoraCard(
    modifier: Modifier = Modifier
) {
    val hora = rememberLiveDeviceTime()
    LabSurfaceCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = GraphiteMedium,
                    modifier = Modifier.size(20.dp)
                )
                LabSectionLabel(text = stringResource(R.string.hora_label))
            }
            Text(
                text = hora,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Graphite
            )
        }
    }
}

@Composable
private fun rememberLiveDeviceTime(): String {
    val formatter = remember {
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    }
    val zone = remember { ZoneId.systemDefault() }
    var timeText by remember { mutableStateOf(formatter.format(ZonedDateTime.now(zone))) }
    LaunchedEffect(formatter, zone) {
        while (isActive) {
            timeText = formatter.format(ZonedDateTime.now(zone))
            delay(1_000L)
        }
    }
    return timeText
}

@Composable
fun GraficaPlaceholder(
    placeholderText: String,
    ejeInicio: String,
    ejeUnidad: String,
    ejeFin: String,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .border(
                    width = 1.dp,
                    color = PlaceholderGray.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = placeholderText,
                style = MaterialTheme.typography.bodySmall,
                color = GraphiteMedium,
                textAlign = TextAlign.Center
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = ejeInicio, style = MaterialTheme.typography.labelSmall, color = GraphiteMedium)
            Text(text = ejeUnidad, style = MaterialTheme.typography.labelSmall, color = GraphiteMedium)
            Text(text = ejeFin, style = MaterialTheme.typography.labelSmall, color = GraphiteMedium)
        }
    }
}

@Composable
fun GraficaLineaCard(
    datos: List<Double>,
    ejeInicio: String,
    ejeUnidad: String,
    ejeFin: String,
    lineColor: Color = PetrolDark,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (datos.size < 2) return@Canvas
            val max = datos.maxOrNull() ?: 1.0
            val min = datos.minOrNull() ?: 0.0
            val range = (max - min).coerceAtLeast(1.0)
            val path = Path()
            datos.forEachIndexed { i, v ->
                val x = size.width * i / (datos.size - 1)
                val y = size.height - ((v - min) / range * size.height).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = lineColor, style = Stroke(width = 3f))
            drawLine(
                color = GraphiteMedium.copy(alpha = 0.3f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1f
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = ejeInicio, style = MaterialTheme.typography.labelSmall, color = GraphiteMedium)
            Text(text = ejeUnidad, style = MaterialTheme.typography.labelSmall, color = GraphiteMedium)
            Text(text = ejeFin, style = MaterialTheme.typography.labelSmall, color = GraphiteMedium)
        }
    }
}

@Composable
fun BotonAccionPrincipal(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Default.Lightbulb
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = PetrolDark,
            contentColor = Color.White,
            disabledContainerColor = PetrolDark.copy(alpha = 0.4f)
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            text = text.uppercase(),
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 1.sp),
            fontWeight = FontWeight.SemiBold
        )
    }
}
