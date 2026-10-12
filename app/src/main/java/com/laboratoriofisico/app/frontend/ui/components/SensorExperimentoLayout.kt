package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.AmberWarning
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.PlaceholderGray
import com.laboratoriofisico.app.frontend.sensor.MedicionEstado
import com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState
data class SensorExperimentoLayoutConfig(
    val titulo: String,
    val sensorName: String,
    val unidad: String,
    val valorDescripcion: String,
    val botonIcon: ImageVector,
    val botonIniciarText: String,
    val botonDetenerText: String,
    val seccionClasificacion: String? = null,
    val opcionesClasificacion: List<String> = emptyList(),
    val clasificacionSeleccionada: String? = null,
    val seccionGrafica: String,
    val graficaUnidad: String,
    /** Serie alternativa para la gráfica; si es null se usa el historial del valor principal. */
    val graficaDatos: List<Double>? = null,
    val contenidoExtra: (@Composable () -> Unit)? = null,
    val avisoLimitacion: String? = null,
    /** Si true, muestra la barra de intensidad de luz en lugar del valor grande gris. */
    val usarBarraIntensidadLuz: Boolean = false
)

@Composable
fun SensorExperimentoLayout(
    uiState: SensorMedicionUiState,
    config: SensorExperimentoLayoutConfig,
    onNavigateBack: () -> Unit,
    onToggleMedicion: () -> Unit,
    onGuardarResultado: () -> Unit,
    modifier: Modifier = Modifier
) {
    val midiendo = uiState.medicionEstado == MedicionEstado.ACTIVO
    val completada = uiState.medicionEstado == MedicionEstado.DETENIDO && uiState.historial.isNotEmpty()
    val uiEstado = mapSensorMedicionEstado(
        disponible = uiState.disponible,
        permisoRequerido = uiState.permisoRequerido,
        sensorState = uiState.sensorState,
        midiendo = midiendo,
        completada = completada
    )
    val valorTexto = when {
        !uiState.disponible || uiState.permisoRequerido -> "▬▬▬"
        uiState.valorPrincipal != null -> formatValor(uiState.valorPrincipal!!, config.unidad)
        midiendo || uiState.historial.isNotEmpty() -> "▬▬▬"
        else -> "▬▬▬"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val guia = guiaPara(uiState.kind)
        val explicacion = explicacionSensor(uiState)

        LabBackLink(onClick = onNavigateBack)
        LabScreenTitle(title = config.titulo)
        ExperimentoGuiaCard(guia = guia)

        SensorCard(sensorName = config.sensorName, disponible = uiState.disponible && !uiState.permisoRequerido)

        if (uiState.luzExperimental) {
            ExperimentalLightBanner()
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            EstadoChip(estado = uiEstado)
        }

        BotonAccionPrincipal(
            text = if (midiendo) config.botonDetenerText else guia.botonProbar,
            icon = config.botonIcon,
            enabled = uiEstado == SensorUiEstado.LISTO ||
                uiEstado == SensorUiEstado.COMPLETADA ||
                uiEstado == SensorUiEstado.MIDIENDO,
            onClick = onToggleMedicion
        )
        AyudaControl(texto = if (midiendo) stringResource(R.string.guia_ayuda_detener) else guia.ayudaProbar)

        if (config.usarBarraIntensidadLuz) {
            LabSurfaceCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = valorTexto,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (valorTexto == "▬▬▬") PlaceholderGray else Graphite
                    )
                    Text(
                        text = unidadVisible(config.unidad),
                        style = MaterialTheme.typography.labelMedium,
                        color = GraphiteMedium
                    )
                    Text(
                        text = config.valorDescripcion,
                        style = MaterialTheme.typography.bodySmall,
                        color = GraphiteMedium
                    )
                    val level = uiState.lightLevel
                    Text(
                        text = if (level != null) {
                            textoNivelLuz(level)
                        } else {
                            stringResource(R.string.luz_esperando_lectura)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (level != null) Graphite else GraphiteMedium
                    )
                    LightIntensityBar(progress = uiState.lightProgress)
                }
            }
        } else {
            ValorGrandeCard(
                valor = valorTexto,
                unidad = config.unidad,
                descripcion = config.valorDescripcion
            )
        }

        if (explicacion != null) {
            ResultadoExplicadoCard(texto = explicacion)
        }

        config.contenidoExtra?.invoke()

        config.avisoLimitacion?.let { aviso ->
            Text(
                text = aviso,
                style = MaterialTheme.typography.bodySmall,
                color = GraphiteMedium
            )
        }

        if (
            config.seccionClasificacion != null &&
            config.opcionesClasificacion.isNotEmpty() &&
            !uiState.luzExperimental
        ) {
            LabSectionLabel(text = config.seccionClasificacion)
            ClasificacionChips(
                opciones = config.opcionesClasificacion,
                seleccionado = config.clasificacionSeleccionada
            )
        }

        HoraCard()

        LabSectionLabel(text = config.seccionGrafica)
        val datosGrafica = config.graficaDatos ?: uiState.historial
        if (datosGrafica.size >= 2) {
            GraficaLineaCard(
                datos = datosGrafica,
                ejeInicio = "0 s",
                ejeUnidad = config.graficaUnidad,
                ejeFin = "45 s"
            )
        } else {
            GraficaPlaceholder(
                placeholderText = stringResource(R.string.grafica_iniciar_medicion),
                ejeInicio = "0 s",
                ejeUnidad = config.graficaUnidad,
                ejeFin = "45 s"
            )
        }

        SecondaryButton(
            text = stringResource(R.string.medicion_guardar),
            onClick = onGuardarResultado,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving && uiState.historial.isNotEmpty() &&
                (uiEstado == SensorUiEstado.COMPLETADA || uiEstado == SensorUiEstado.LISTO || uiEstado == SensorUiEstado.MIDIENDO)
        )
        AyudaControl(texto = guia.ayudaGuardar)

        if (uiState.isSaving) {
            EstadoAmigable(
                tipo = EstadoAmigableTipo.Cargando,
                texto = stringResource(R.string.estado_guardando)
            )
        }

        uiState.saveMessage?.let { raw ->
            val (tipo, texto) = clasificarMensaje(raw)
            EstadoAmigable(tipo = tipo, texto = texto)
        }

        uiState.mensaje?.let { raw ->
            val (tipo, texto) = clasificarMensaje(raw)
            EstadoAmigable(tipo = tipo, texto = texto)
        }
    }
}

@Composable
fun MiniMetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null
) {
    LabSurfaceCard(
        modifier = modifier.semantics {
            if (ayuda != null) contentDescription = "$label. $value. $ayuda"
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LabSectionLabel(text = label)
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (value == "—" || value == "▬▬▬") PlaceholderGray else Graphite
            )
            if (ayuda != null) {
                AyudaControl(texto = ayuda)
            }
        }
    }
}

@Composable
fun MetricRowCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    ayuda: String? = null
) {
    LabSurfaceCard(
        modifier = modifier.semantics {
            if (ayuda != null) contentDescription = "$label. $value. $ayuda"
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = GraphiteMedium)
                Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Graphite)
            }
            if (ayuda != null) {
                AyudaControl(texto = ayuda)
            }
        }
    }
}

private fun formatValor(valor: Double, unidad: String): String = when (unidad.lowercase()) {
    "lux", "db", "hz", "µt", "ut", "%" -> "%.0f".format(valor)
    "raw", "crudo" -> "%.2f".format(valor)
    "m", "s" -> "%.2f".format(valor)
    "m/s²", "m/s2" -> "%.2f".format(valor)
    else -> "%.1f".format(valor)
}

@Composable
fun ExperimentalLightBanner(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmberWarning.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.luz_modo_experimental),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = AmberWarning
        )
    }
}
