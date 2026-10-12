package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.sensores.util.NearbyMagnetism
import com.laboratoriofisico.app.sensores.util.NearbyMagnetismLevel
import com.laboratoriofisico.app.core.theme.AmberWarning
import com.laboratoriofisico.app.core.theme.DarkRedError
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.TealSuccess

data class MagnetismTechnicalItem(
    val label: String,
    val value: String,
    val description: String
)

@Composable
fun MagnetismStatusCard(
    intensityUt: Double?,
    compassStable: Boolean,
    calibrating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val status = magnetismStatus(intensityUt, calibrating)
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabSectionLabel(text = stringResource(R.string.anomalia_estado))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = status.color
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(status.color.copy(alpha = 0.14f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = intensityUt?.let { "%.1f µT".format(it) } ?: "—",
                        style = MaterialTheme.typography.labelLarge,
                        color = status.color,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = status.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Graphite
            )
            if (!compassStable) {
                Text(
                    text = stringResource(R.string.magnetismo_calibrar_ocho),
                    style = MaterialTheme.typography.bodySmall,
                    color = AmberWarning,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun MagnetismGaugeCard(
    intensityUt: Double?,
    modifier: Modifier = Modifier
) {
    val progress = NearbyMagnetism.gaugeProgress(intensityUt)
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabSectionLabel(text = stringResource(R.string.magnetismo_medidor_titulo))
            Text(
                text = stringResource(R.string.magnetismo_medidor_referencia_tierra),
                style = MaterialTheme.typography.bodyMedium,
                color = Graphite
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(50)),
                color = magnetismStatus(intensityUt).color,
                trackColor = GraphiteMedium.copy(alpha = 0.18f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.magnetismo_estado_normal),
                    style = MaterialTheme.typography.labelSmall,
                    color = TealSuccess
                )
                Text(
                    text = stringResource(R.string.magnetismo_estado_elevado),
                    style = MaterialTheme.typography.labelSmall,
                    color = AmberWarning,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.magnetismo_estado_muy_alto),
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkRedError
                )
            }
        }
    }
}

@Composable
fun MagnetismDirectionCard(
    azimuthDegrees: Double?,
    direction: String,
    magnetDirection: String,
    modifier: Modifier = Modifier
) {
    val visibleDirection = magnetDirection.takeIf { it != "N/A" }
    val fallbackDirection = direction.takeIf { it != "N/A" }
    LabSurfaceCard(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LabSectionLabel(
                text = stringResource(R.string.magnetismo_direccion_titulo),
                modifier = Modifier.fillMaxWidth()
            )
            BrujulaWidget(azimuthDegrees = azimuthDegrees, sizeDp = 150)
            Text(
                text = when {
                    visibleDirection != null -> stringResource(R.string.magnetismo_direccion_imagen, visibleDirection)
                    fallbackDirection != null -> stringResource(R.string.magnetismo_direccion_campo, fallbackDirection)
                    else -> stringResource(R.string.magnetismo_direccion_sin_datos)
                },
                style = MaterialTheme.typography.titleMedium,
                color = Graphite,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun MagnetismHelpCard(modifier: Modifier = Modifier) {
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.magnetismo_ayuda_titulo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.magnetismo_ayuda_texto),
                style = MaterialTheme.typography.bodyMedium,
                color = Graphite
            )
        }
    }
}

@Composable
fun MagnetismTechnicalDataCard(
    items: List<MagnetismTechnicalItem>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        if (expanded) {
                            R.string.magnetismo_ocultar_datos_tecnicos
                        } else {
                            R.string.magnetismo_ver_datos_tecnicos
                        }
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleSmall,
                    color = GraphiteMedium
                )
            }
            if (expanded) {
                items.forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Graphite
                            )
                            Text(
                                text = item.value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Graphite
                            )
                        }
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = GraphiteMedium
                        )
                    }
                }
            }
        }
    }
}

private data class MagnetismStatus(
    val title: String,
    val description: String,
    val color: Color
)

@Composable
private fun magnetismStatus(intensityUt: Double?, calibrating: Boolean = false): MagnetismStatus {
    if (calibrating) {
        return MagnetismStatus(
            title = stringResource(R.string.magnetismo_estado_calibrando),
            description = stringResource(R.string.magnetismo_estado_calibrando_desc),
            color = GraphiteMedium
        )
    }
    if (intensityUt == null) {
        return MagnetismStatus(
            title = stringResource(R.string.magnetismo_estado_sin_lectura),
            description = stringResource(R.string.magnetismo_estado_sin_lectura_desc),
            color = GraphiteMedium
        )
    }
    return when (NearbyMagnetism.level(intensityUt)) {
        NearbyMagnetismLevel.MUY_BAJO -> MagnetismStatus(
            title = stringResource(R.string.magnetismo_estado_normal),
            description = stringResource(R.string.magnetismo_estado_normal_desc),
            color = TealSuccess
        )
        NearbyMagnetismLevel.MEDIO -> MagnetismStatus(
            title = stringResource(R.string.magnetismo_estado_elevado),
            description = stringResource(R.string.magnetismo_estado_elevado_desc),
            color = AmberWarning
        )
        NearbyMagnetismLevel.ALTO -> MagnetismStatus(
            title = stringResource(R.string.magnetismo_estado_muy_alto),
            description = stringResource(R.string.magnetismo_estado_muy_alto_desc),
            color = DarkRedError
        )
    }
}
