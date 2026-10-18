package com.laboratoriofisico.app.frontend.ui.screens.experimentos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.backend.hoja.HojaCalculo
import com.laboratoriofisico.app.backend.hoja.HojaCalculoNotas
import com.laboratoriofisico.app.backend.hoja.MedidasHoja
import com.laboratoriofisico.app.backend.piel.PielEscamasCalculo
import com.laboratoriofisico.app.frontend.ui.components.LabSurfaceCard
import kotlinx.coroutines.delay

private enum class ModoMedidaHoja(val titulo: String) {
    CALIBRAR("Calibrar con regla"),
    ESTOMA("Estoma"),
    CELULA("Célula"),
    LARGO("Largo"),
    ANCHO("Ancho")
}

@Composable
fun CalculadoraHoja(
    imagenPath: String,
    analisisGuardado: String,
    onGuardar: (String) -> Unit
) {
    val guardado = remember(imagenPath) { HojaCalculoNotas.leer(analisisGuardado) }
    val foto = remember(imagenPath) { cargarFoto(imagenPath) }
    var abierta by remember(imagenPath) { mutableStateOf(false) }
    var modo by remember(imagenPath) { mutableStateOf(ModoMedidaHoja.CALIBRAR) }
    var distanciaReal by remember(imagenPath) {
        mutableStateOf(guardado.distanciaRealUm?.let(PielEscamasCalculo::texto).orEmpty())
    }
    var pixelesCalibracion by remember(imagenPath) { mutableStateOf(guardado.pixelesCalibracion) }
    var estomas by remember(imagenPath) { mutableStateOf(guardado.estomas?.toString().orEmpty()) }
    var celulas by remember(imagenPath) { mutableStateOf(guardado.celulasEpidermicas?.toString().orEmpty()) }
    var pixelesEstoma by remember(imagenPath) { mutableStateOf(guardado.pixelesEstoma) }
    var pixelesCelula by remember(imagenPath) { mutableStateOf(guardado.pixelesCelula) }
    var pixelesLargo by remember(imagenPath) { mutableStateOf(guardado.pixelesLargo) }
    var pixelesAncho by remember(imagenPath) { mutableStateOf(guardado.pixelesAncho) }
    var toques by remember(imagenPath) { mutableStateOf<List<Offset>>(emptyList()) }

    val umPorPixel = pixelesCalibracion?.let { pixeles ->
        PielEscamasCalculo.numero(distanciaReal)?.let { real ->
            PielEscamasCalculo.umPorPixel(real, pixeles)
        }
    }
    val medidas = MedidasHoja(
        distanciaRealUm = PielEscamasCalculo.numero(distanciaReal),
        pixelesCalibracion = pixelesCalibracion,
        estomas = HojaCalculo.enteroNoNegativo(estomas),
        celulasEpidermicas = HojaCalculo.enteroNoNegativo(celulas),
        pixelesEstoma = pixelesEstoma,
        pixelesCelula = pixelesCelula,
        pixelesLargo = pixelesLargo,
        pixelesAncho = pixelesAncho
    )
    val informe = HojaCalculoNotas.informe(medidas, umPorPixel)
    LaunchedEffect(informe) {
        val texto = informe ?: return@LaunchedEffect
        delay(300)
        onGuardar(texto)
    }
    val indice = if (medidas.estomas != null && medidas.celulasEpidermicas != null) {
        HojaCalculo.indiceEstomatico(medidas.estomas, medidas.celulasEpidermicas)
    } else {
        null
    }
    val relacion = if (medidas.pixelesLargo != null && medidas.pixelesAncho != null) {
        PielEscamasCalculo.relacionLargoAncho(medidas.pixelesLargo, medidas.pixelesAncho)
    } else {
        null
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
                        text = if (abierta) "Ocultar calculadora" else "Calculadora de la hoja",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Cálculo educativo, no es un dato de la salud de la planta",
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
                                    ModoMedidaHoja.CALIBRAR -> pixelesCalibracion = pixeles
                                    ModoMedidaHoja.ESTOMA -> pixelesEstoma = pixeles
                                    ModoMedidaHoja.CELULA -> pixelesCelula = pixeles
                                    ModoMedidaHoja.LARGO -> pixelesLargo = pixeles
                                    ModoMedidaHoja.ANCHO -> pixelesAncho = pixeles
                                }
                            }
                        }
                    )
                }
                ModoMedidaHoja.entries.forEach { opcion ->
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
                if (modo == ModoMedidaHoja.CALIBRAR) {
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
                    value = estomas,
                    onValueChange = { valor -> estomas = valor.filter { it.isDigit() } },
                    label = { Text("Estomas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = celulas,
                    onValueChange = { valor -> celulas = valor.filter { it.isDigit() } },
                    label = { Text("Células epidérmicas") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (indice != null) {
                    Text(
                        text = "Índice estomático (%) = ${PielEscamasCalculo.texto(indice)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = "Con pocas células en el campo el resultado es poco fiable; conviene contar en varias zonas y promediar.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (umPorPixel == null) {
                    Text(
                        text = "Calibra con una regla para calcular",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    val estomaUm = medidas.pixelesEstoma?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
                    val celulaUm = medidas.pixelesCelula?.let { PielEscamasCalculo.tamanoUm(it, umPorPixel) }
                    Text(
                        text = "µm por píxel = ${PielEscamasCalculo.texto(umPorPixel)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    estomaUm?.let {
                        Text(
                            text = "Tamaño real del estoma = ${PielEscamasCalculo.texto(it)} µm",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    celulaUm?.let {
                        Text(
                            text = "Tamaño real de la célula = ${PielEscamasCalculo.texto(it)} µm",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Text(
                    text = "Forma de la célula = ${relacion?.let(PielEscamasCalculo::texto) ?: "—"}. Cerca de 1 = más redonda; mayor a 1 = más alargada.",
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
