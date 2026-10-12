package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PetrolDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun BrujulaWidget(
    azimuthDegrees: Double?,
    modifier: Modifier = Modifier,
    sizeDp: Int = 220
) {
    LabSurfaceCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier.size(sizeDp.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f * 0.85f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(
                        color = GraphiteMedium.copy(alpha = 0.2f),
                        radius = radius,
                        center = center,
                        style = Stroke(2f)
                    )
                    val labels = listOf("N", "NE", "E", "SE", "S", "SO", "O", "NO")
                    labels.forEachIndexed { index, _ ->
                        val angle = Math.toRadians((index * 45 - 90).toDouble())
                        val lx = center.x + radius * cos(angle).toFloat()
                        val ly = center.y + radius * sin(angle).toFloat()
                        drawCircle(color = GraphiteMedium, radius = 3f, center = Offset(lx, ly))
                    }
                    if (azimuthDegrees != null) {
                        rotate(degrees = azimuthDegrees.toFloat(), pivot = center) {
                            drawLine(
                                color = PetrolDark,
                                start = center,
                                end = Offset(center.x, center.y - radius * 0.75f),
                                strokeWidth = 4f
                            )
                        }
                    }
                    drawCircle(color = Graphite, radius = 6f, center = center)
                }
            }
            Text(
                text = "Dirección actual: ${azimuthDegrees?.let { "%.0f°".format(it) } ?: "—"}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = GraphiteMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun PhoneTiltIllustration(
    tiltDegrees: String,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val tilt = tiltDegrees.replace("°", "").toFloatOrNull() ?: 0f
                rotate(degrees = tilt.coerceIn(-45f, 45f), pivot = center) {
                    drawRoundRect(
                        color = PetrolDark.copy(alpha = 0.15f),
                        topLeft = Offset(size.width * 0.25f, size.height * 0.15f),
                        size = Size(size.width * 0.5f, size.height * 0.7f),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(3f)
                    )
                }
            }
        }
    }
}
