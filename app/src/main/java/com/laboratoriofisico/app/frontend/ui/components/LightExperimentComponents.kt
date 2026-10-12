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

import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material3.MaterialTheme

import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment

import androidx.compose.ui.Modifier

import androidx.compose.ui.draw.clip

import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.geometry.Size

import androidx.compose.ui.graphics.Brush

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.graphics.Path

import androidx.compose.ui.graphics.drawscope.Stroke

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.ui.platform.LocalDensity

import androidx.compose.ui.res.stringResource

import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.unit.dp

import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.remember

import androidx.compose.runtime.setValue

import com.laboratoriofisico.app.R

import com.laboratoriofisico.app.core.theme.Graphite

import com.laboratoriofisico.app.core.theme.GraphiteMedium

import com.laboratoriofisico.app.core.theme.PlaceholderGray

import com.laboratoriofisico.app.frontend.viewmodel.LightHistoryEntry

import com.laboratoriofisico.app.sensores.util.LightClassifier

import com.laboratoriofisico.app.sensores.util.LightLevel



@Composable

fun LightClassificationPanel(

    valor: Double?,

    unidad: String,

    level: LightLevel?,

    progress: Float?,

    disponible: Boolean,

    modifier: Modifier = Modifier

) {

    LabSurfaceCard(modifier = modifier) {

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            LabSectionLabel(text = stringResource(R.string.luz_seccion_intensidad))



            if (!disponible) {

                Text(

                    text = stringResource(R.string.sensor_no_disponible),

                    style = MaterialTheme.typography.titleMedium,

                    fontWeight = FontWeight.Bold,

                    color = GraphiteMedium

                )

                return@Column

            }



            LightMetricBlock(

                label = stringResource(R.string.luz_valor_actual),

                value = valor?.let { formatLightValue(it, unidad) } ?: PLACEHOLDER

            )

            LightMetricBlock(

                label = stringResource(R.string.luz_nivel),

                value = level?.let { LightClassifier.levelDisplayName(it) } ?: PLACEHOLDER,

                emphasized = true

            )



            LightIntensityBar(progress = progress)



            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {

                LabSectionLabel(text = stringResource(R.string.luz_descripcion))

                Text(

                    text = level?.let { textoNivelLuz(it) }

                        ?: stringResource(R.string.luz_esperando_lectura),

                    style = MaterialTheme.typography.bodyMedium,

                    color = if (level != null) Graphite else GraphiteMedium

                )

            }



            Text(

                text = LightClassifier.safetyDisclaimer(),

                style = MaterialTheme.typography.bodySmall,

                color = GraphiteMedium

            )

        }

    }

}



/**

 * Barra verde→rojo con pin azul. Todo se dibuja en un Canvas de altura fija

 * para que la flecha no se pierda por problemas de layout/scroll.

 */

@Composable

fun LightIntensityBar(

    progress: Float?,

    modifier: Modifier = Modifier

) {

    val density = LocalDensity.current

    var widthPx by remember { mutableIntStateOf(0) }

    val safeProgress = progress?.coerceIn(0f, 1f)



    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Canvas(

            modifier = Modifier

                .fillMaxWidth()

                .height(56.dp)

                .onSizeChanged { widthPx = it.width }

        ) {

            val w = size.width

            val trackTop = 4.dp.toPx()

            val trackHeight = 18.dp.toPx()

            val trackRadius = trackHeight / 2f

            val brush = Brush.horizontalGradient(

                colorStops = arrayOf(

                    0.0f to LightGreen,

                    0.35f to LightYellow,

                    0.65f to LightOrange,

                    1.0f to LightRed

                )

            )

            drawRoundRect(

                brush = brush,

                topLeft = Offset(0f, trackTop),

                size = Size(w, trackHeight),

                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackRadius, trackRadius)

            )

            drawRoundRect(

                color = TrackBorder,

                topLeft = Offset(0f, trackTop),

                size = Size(w, trackHeight),

                cornerRadius = androidx.compose.ui.geometry.CornerRadius(trackRadius, trackRadius),

                style = Stroke(width = 2.dp.toPx())

            )



            if (safeProgress != null && w > 0f) {

                val pinX = (w * safeProgress).coerceIn(14.dp.toPx(), w - 14.dp.toPx())

                val tipY = trackTop + trackHeight + 2.dp.toPx()

                val pinTop = tipY

                val pinBottom = pinTop + 22.dp.toPx()

                val half = 11.dp.toPx()

                val path = Path().apply {

                    moveTo(pinX, pinTop)

                    lineTo(pinX - half, pinBottom)

                    lineTo(pinX + half, pinBottom)

                    close()

                }

                drawPath(path, PinBlue)

                drawCircle(

                    color = PinBlue,

                    radius = 12.dp.toPx(),

                    center = Offset(pinX, pinBottom + 2.dp.toPx())

                )

            }

        }



        if (safeProgress != null && widthPx > 0) {

            val labelOffset = with(density) {

                ((widthPx * safeProgress) - 18.dp.toPx()).coerceIn(0f, widthPx - 36.dp.toPx()).toDp()

            }

            Box(modifier = Modifier.fillMaxWidth()) {

                Text(

                    text = "${(safeProgress * 100f).toInt()}%",

                    modifier = Modifier.padding(start = labelOffset),

                    style = MaterialTheme.typography.labelLarge,

                    fontWeight = FontWeight.Bold,

                    color = PinBlue

                )

            }

        }



        Row(

            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement = Arrangement.SpaceBetween

        ) {

            Text(

                text = stringResource(R.string.iluminacion_baja),

                style = MaterialTheme.typography.labelSmall,

                fontWeight = FontWeight.Bold,

                color = LightGreenDark

            )

            Text(

                text = stringResource(R.string.iluminacion_media),

                style = MaterialTheme.typography.labelSmall,

                fontWeight = FontWeight.Bold,

                color = LightYellowDark

            )

            Text(

                text = stringResource(R.string.iluminacion_alta),

                style = MaterialTheme.typography.labelSmall,

                fontWeight = FontWeight.Bold,

                color = LightRedDark

            )

        }

    }

}



@Composable

fun LightHistoryPanel(

    entries: List<LightHistoryEntry>,

    modifier: Modifier = Modifier

) {

    if (entries.isEmpty()) return

    LabSurfaceCard(modifier = modifier) {

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

            LabSectionLabel(text = stringResource(R.string.luz_historial))

            entries.takeLast(10).reversed().forEach { entry ->

                Text(

                    text = entry.formatLine(),

                    style = MaterialTheme.typography.bodySmall,

                    color = Graphite,

                    modifier = Modifier.fillMaxWidth()

                )

            }

        }

    }

}



@Composable

private fun LightMetricBlock(

    label: String,

    value: String,

    modifier: Modifier = Modifier,

    emphasized: Boolean = false

) {

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {

        LabSectionLabel(text = label)

        Text(

            text = value,

            style = if (emphasized) {

                MaterialTheme.typography.headlineSmall

            } else {

                MaterialTheme.typography.headlineMedium

            },

            fontWeight = FontWeight.Bold,

            color = if (value == PLACEHOLDER) PlaceholderGray else Graphite

        )

    }

}



private fun formatLightValue(value: Double, unidad: String): String =

    "%.0f %s".format(value, unidad)



private const val PLACEHOLDER = "▬▬▬"



private val LightGreen = Color(0xFF22C55E)

private val LightYellow = Color(0xFFFACC15)

private val LightOrange = Color(0xFFF97316)

private val LightRed = Color(0xFFEF4444)

private val LightGreenDark = Color(0xFF15803D)

private val LightYellowDark = Color(0xFFA16207)

private val LightRedDark = Color(0xFFB91C1C)

private val TrackBorder = Color(0xFFD1D5DB)

private val PinBlue = Color(0xFF2563EB)


