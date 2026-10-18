package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.backend.piel.MedidasGuardadas
import com.laboratoriofisico.app.backend.piel.PielCalculoNotas
import com.laboratoriofisico.app.backend.piel.PielEscamasCalculo
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import kotlinx.coroutines.delay

private enum class ModoMedidaPiel(val titulo: String) {
    CALIBRAR("Calibrar con regla"),
    ESCAMA("Tamaño de una escama"),
    LARGO("Largo"),
    ANCHO("Ancho")
}

internal data class FotoParaMedir(
    val imagen: androidx.compose.ui.graphics.ImageBitmap,
    val ancho: Int,
    val alto: Int
)

@Composable
fun CalculadoraPielEscamas(
    imagenPath: String,
    analisisGuardado: String,
    onGuardar: (String) -> Unit
) {
    val guardado = remember(imagenPath) { PielCalculoNotas.leer(analisisGuardado) }
    val foto = remember(imagenPath) { cargarFoto(imagenPath) }
    var abierta by remember(imagenPath) { mutableStateOf(false) }
    var modo by remember(imagenPath) { mutableStateOf(ModoMedidaPiel.CALIBRAR) }
    var distanciaReal by remember(imagenPath) {
        mutableStateOf(guardado.distanciaRealUm?.let(PielEscamasCalculo::texto).orEmpty())
    }
    var pixelesCalibracion by remember(imagenPath) { mutableStateOf(guardado.pixelesCalibracion) }
    var nEscamas by remember(imagenPath) {
        mutableStateOf(if (guardado.n > 0) guardado.n.toString() else "")
    }
    var pixelesEscama by remember(imagenPath) { mutableStateOf(guardado.pixelesEscama) }
    var pixelesLargo by remember(imagenPath) { mutableStateOf(guardado.pixelesLargo) }
    var pixelesAncho by remember(imagenPath) { mutableStateOf(guardado.pixelesAncho) }
    var toques by remember(imagenPath) { mutableStateOf<List<Offset>>(emptyList()) }

    val anchoImagen = foto?.ancho?.takeIf { it > 0 } ?: guardado.anchoImagenPx
    val umPorPixel = pixelesCalibracion?.let { pixeles ->
        PielEscamasCalculo.numero(distanciaReal)?.let { real ->
            PielEscamasCalculo.umPorPixel(real, pixeles)
        }
    }
    val medidas = MedidasGuardadas(
        distanciaRealUm = PielEscamasCalculo.numero(distanciaReal),
        pixelesCalibracion = pixelesCalibracion,
        anchoImagenPx = anchoImagen,
        n = PielEscamasCalculo.entero(nEscamas) ?: 0,
        pixelesEscama = pixelesEscama,
        pixelesLargo = pixelesLargo,
        pixelesAncho = pixelesAncho
    )
    val informe = umPorPixel?.let { PielCalculoNotas.informe(medidas, it) }
    LaunchedEffect(informe) {
        val texto = informe ?: return@LaunchedEffect
        delay(300)
        onGuardar(texto)
    }

    LabSurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { abierta = !abierta },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = if (abierta) "Ocultar calculadora" else "Calculadora de piel y escamas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cálculo educativo, no es un dato de salud de la piel",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (abierta) "▲" else "▼",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            if (abierta) {
                Text(
                    text = "Toca dos puntos sobre la foto. 1 mm de la regla = 1000 µm.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (foto == null) {
                    Text("No se pudo abrir la foto para medir.", style = MaterialTheme.typography.bodySmall)
                } else {
                    FotoConToques(
                        foto = foto,
                        puntos = toques,
                        onToque = { punto ->
                            val siguientes = if (toques.size >= 2) listOf(punto) else toques + punto
                            toques = siguientes
                            if (siguientes.size == 2) {
                                val pixeles = PielEscamasCalculo.distanciaPixeles(
                                    siguientes[0].x,
                                    siguientes[0].y,
                                    siguientes[1].x,
                                    siguientes[1].y
                                )
                                when (modo) {
                                    ModoMedidaPiel.CALIBRAR -> pixelesCalibracion = pixeles
                                    ModoMedidaPiel.ESCAMA -> pixelesEscama = pixeles
                                    ModoMedidaPiel.LARGO -> pixelesLargo = pixeles
                                    ModoMedidaPiel.ANCHO -> pixelesAncho = pixeles
                                }
                            }
                        }
                    )
                }
                ModoMedidaPiel.entries.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                modo = opcion
                                toques = emptyList()
                            }
                    ) {
                        RadioButton(
                            selected = modo == opcion,
                            onClick = {
                                modo = opcion
                                toques = emptyList()
                            }
                        )
                        Text(
                            text = opcion.titulo,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (modo == opcion) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
                Text(
                    text = when (toques.size) {
                        0 -> "Toca el primer punto."
                        1 -> "Toca el segundo punto."
                        else -> "Píxeles entre los puntos: ${
                            PielEscamasCalculo.distanciaPixeles(
                                toques[0].x, toques[0].y, toques[1].x, toques[1].y
                            )?.let(PielEscamasCalculo::texto) ?: "—"
                        }"
                    },
                    style = MaterialTheme.typography.bodySmall
                )
                if (modo == ModoMedidaPiel.CALIBRAR) {
                    OutlinedTextField(
                        value = distanciaReal,
                        onValueChange = { distanciaReal = it },
                        label = { Text("Distancia real (µm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = nEscamas,
                    onValueChange = { valor -> nEscamas = valor.filter { it.isDigit() } },
                    label = { Text("N escamas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (umPorPixel == null) {
                    Text(
                        text = "Calibra con una regla para calcular",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    ResultadosPiel(umPorPixel = umPorPixel, medidas = medidas)
                }
                Text(
                    text = "Con pocas escamas en el campo el resultado es poco fiable; conviene contar en varias zonas y promediar.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "El número de aumento de la caja no es el aumento real. Calibra con una regla para medir bien.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ResultadosPiel(
    umPorPixel: Double,
    medidas: MedidasGuardadas
) {
    val diametro = PielEscamasCalculo.diametroCampoMm(medidas.anchoImagenPx, umPorPixel)
    val area = diametro?.let(PielEscamasCalculo::areaCampoMm2)
    val densidad = area?.let { PielEscamasCalculo.escamasPorMm2(medidas.n, it) }
    val escama = medidas.pixelesEscama?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
    val largo = medidas.pixelesLargo?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
    val ancho = medidas.pixelesAncho?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
    val relacion = if (largo != null && ancho != null) {
        PielEscamasCalculo.relacionLargoAncho(largo, ancho)
    } else {
        null
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "µm por píxel = ${PielEscamasCalculo.texto(umPorPixel)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Diámetro del campo (mm) = ${diametro?.let(PielEscamasCalculo::texto) ?: "—"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Área del campo (mm²) = ${area?.let(PielEscamasCalculo::texto) ?: "—"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Escamas por mm² = ${densidad?.let(PielEscamasCalculo::texto) ?: "—"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Tamaño real de una escama = ${escama?.let { "${PielEscamasCalculo.texto(it)} µm" } ?: "—"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Relación largo/ancho = ${relacion?.let(PielEscamasCalculo::texto) ?: "—"}. Cerca de 1 = más redonda; mayor = más alargada.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
internal fun FotoConToques(
    foto: FotoParaMedir,
    puntos: List<Offset>,
    onToque: (Offset) -> Unit
) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(foto.ancho, foto.alto) {
                detectTapGestures { toque ->
                    aPixeles(toque, size, foto.ancho, foto.alto)?.let(onToque)
                }
            }
    ) {
        Image(
            bitmap = foto.imagen,
            contentDescription = "Foto para calibrar y medir",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dibujados = puntos.mapNotNull { aPantalla(it, size, foto.ancho, foto.alto) }
            if (dibujados.size == 2) {
                drawLine(color = color, start = dibujados[0], end = dibujados[1], strokeWidth = 4f)
            }
            dibujados.forEach { centro ->
                drawCircle(color = Color.White, radius = 14f, center = centro)
                drawCircle(color = color, radius = 8f, center = centro)
            }
        }
    }
}

internal fun cargarFoto(path: String): FotoParaMedir? = runCatching {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, limites)
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
    val muestra = maxOf(1, maxOf(limites.outWidth, limites.outHeight) / 1080)
    val bitmap = BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = muestra }
    ) ?: return null
    FotoParaMedir(bitmap.asImageBitmap(), limites.outWidth, limites.outHeight)
}.getOrNull()

private fun aPixeles(toque: Offset, caja: IntSize, ancho: Int, alto: Int): Offset? {
    val marco = marcoDeFoto(caja.width.toFloat(), caja.height.toFloat(), ancho, alto) ?: return null
    val x = toque.x - marco.izquierda
    val y = toque.y - marco.arriba
    if (x < 0f || y < 0f || x > marco.ancho || y > marco.alto) return null
    return Offset(x / marco.escala, y / marco.escala)
}

private fun aPantalla(punto: Offset, caja: Size, ancho: Int, alto: Int): Offset? {
    val marco = marcoDeFoto(caja.width, caja.height, ancho, alto) ?: return null
    return Offset(marco.izquierda + punto.x * marco.escala, marco.arriba + punto.y * marco.escala)
}

private data class MarcoFoto(
    val izquierda: Float,
    val arriba: Float,
    val ancho: Float,
    val alto: Float,
    val escala: Float
)

private fun marcoDeFoto(cajaAncho: Float, cajaAlto: Float, ancho: Int, alto: Int): MarcoFoto? {
    if (ancho <= 0 || alto <= 0 || cajaAncho <= 0f || cajaAlto <= 0f) return null
    val escala = minOf(cajaAncho / ancho, cajaAlto / alto)
    val dibujoAncho = ancho * escala
    val dibujoAlto = alto * escala
    return MarcoFoto(
        izquierda = (cajaAncho - dibujoAncho) / 2f,
        arriba = (cajaAlto - dibujoAlto) / 2f,
        ancho = dibujoAncho,
        alto = dibujoAlto,
        escala = escala
    )
}
