package com.laboratoriofisico.app.frontend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laboratoriofisico.app.R
import com.laboratoriofisico.app.core.theme.AmberWarning
import com.laboratoriofisico.app.core.theme.DarkRedError
import com.laboratoriofisico.app.core.theme.Graphite
import com.laboratoriofisico.app.core.theme.GraphiteMedium
import com.laboratoriofisico.app.core.theme.TealSuccess
import com.laboratoriofisico.app.frontend.sensor.MedicionEstado
import com.laboratoriofisico.app.frontend.viewmodel.ExperimentoSensorKind
import com.laboratoriofisico.app.frontend.viewmodel.SensorMedicionUiState
import com.laboratoriofisico.app.sensores.util.LightLevel

data class ExperimentoGuia(
    val paraQue: String,
    val queVeras: String,
    val pasos: List<String>,
    val ejemplo: String,
    val botonProbar: String,
    val ayudaProbar: String,
    val ayudaGuardar: String
)

@Composable
fun IntroBloque(
    titulo: String,
    cuerpo: String,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Graphite
            )
            Text(
                text = cuerpo,
                style = MaterialTheme.typography.bodyMedium,
                color = GraphiteMedium
            )
        }
    }
}

@Composable
fun ExperimentoGuiaCard(
    guia: ExperimentoGuia,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.guia_que_hace),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Graphite
            )
            Text(text = guia.paraQue, style = MaterialTheme.typography.bodyMedium, color = Graphite)
            Text(text = guia.queVeras, style = MaterialTheme.typography.bodyMedium, color = GraphiteMedium)

            Text(
                text = stringResource(R.string.guia_como_usarlo),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Graphite
            )
            guia.pasos.forEachIndexed { index, paso ->
                Text(
                    text = "${index + 1}. $paso",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Graphite
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TealSuccess.copy(alpha = 0.10f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.guia_ejemplo),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Graphite
                )
                Text(
                    text = guia.ejemplo,
                    style = MaterialTheme.typography.bodySmall,
                    color = GraphiteMedium
                )
            }
        }
    }
}

@Composable
fun ResultadoExplicadoCard(
    texto: String,
    modifier: Modifier = Modifier
) {
    LabSurfaceCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.guia_que_significa),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Graphite
            )
            Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = Graphite)
        }
    }
}

@Composable
fun AyudaControl(
    texto: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = texto,
        modifier = modifier.semantics { contentDescription = texto },
        style = MaterialTheme.typography.bodySmall,
        color = GraphiteMedium
    )
}

enum class EstadoAmigableTipo { Cargando, Exito, Error, Info }

@Composable
fun EstadoAmigable(
    tipo: EstadoAmigableTipo,
    texto: String,
    modifier: Modifier = Modifier
) {
    val (fondo, tinta) = when (tipo) {
        EstadoAmigableTipo.Cargando -> AmberWarning.copy(alpha = 0.14f) to Graphite
        EstadoAmigableTipo.Exito -> TealSuccess.copy(alpha = 0.14f) to Graphite
        EstadoAmigableTipo.Error -> DarkRedError.copy(alpha = 0.12f) to DarkRedError
        EstadoAmigableTipo.Info -> AmberWarning.copy(alpha = 0.10f) to Graphite
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = texto },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tipo == EstadoAmigableTipo.Cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = tinta
            )
        }
        Text(text = texto, style = MaterialTheme.typography.bodyMedium, color = tinta)
    }
}

fun clasificarMensaje(raw: String): Pair<EstadoAmigableTipo, String> {
    val lower = raw.lowercase()
    return when {
        "no hay lecturas reales suficientes" in lower ->
            EstadoAmigableTipo.Info to
                "Hizo falta un poco más de tiempo. Prueba otra vez y mantén el movimiento unos segundos."
        "no hay lecturas" in lower ->
            EstadoAmigableTipo.Info to
                "Todavía no hay datos. Pulsa el botón principal, espera unos segundos y vuelve a guardar."
        lower.startsWith("resultado guardado") || "medición guardada" in lower || "medicion guardada" in lower ->
            EstadoAmigableTipo.Exito to "Listo. Tu resultado quedó guardado. Puedes verlo después en Resultados."
        "no se pudo guardar" in lower || "error al guardar" in lower ->
            EstadoAmigableTipo.Error to "No pudimos guardar. Inténtalo otra vez en un momento."
        "inicia sesión" in lower || "invitado no guarda" in lower ->
            EstadoAmigableTipo.Info to "Para guardar hace falta una cuenta. Como invitado puedes seguir probando."
        "permiso" in lower ->
            EstadoAmigableTipo.Error to "Falta un permiso. Acepta el aviso del teléfono para continuar."
        "no se pudo vincular" in lower || "cámara" in lower && "no se pudo" in lower ->
            EstadoAmigableTipo.Error to "No se pudo abrir la cámara. Sal de esta pantalla y vuelve a entrar."
        "exception" in lower || "java." in lower ->
            EstadoAmigableTipo.Error to "Ocurrió un problema inesperado. Vuelve a intentarlo."
        else -> EstadoAmigableTipo.Info to raw
    }
}

@Composable
fun textoNivelLuz(level: LightLevel): String = when (level) {
    LightLevel.LOW -> stringResource(R.string.luz_lectura_baja)
    LightLevel.MEDIUM -> stringResource(R.string.luz_lectura_media)
    LightLevel.HIGH -> stringResource(R.string.luz_lectura_alta)
}

@Composable
fun guiaPara(kind: ExperimentoSensorKind): ExperimentoGuia {
    val guardar = stringResource(R.string.guia_ayuda_guardar)
    return when (kind) {
        ExperimentoSensorKind.LUZ -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_luz_para_que),
            queVeras = stringResource(R.string.guia_luz_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_luz_paso_1),
                stringResource(R.string.guia_luz_paso_2),
                stringResource(R.string.guia_luz_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_luz_ejemplo),
            botonProbar = stringResource(R.string.guia_luz_boton),
            ayudaProbar = stringResource(R.string.guia_luz_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.SONIDO -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_sonido_para_que),
            queVeras = stringResource(R.string.guia_sonido_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_sonido_paso_1),
                stringResource(R.string.guia_sonido_paso_2),
                stringResource(R.string.guia_sonido_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_sonido_ejemplo),
            botonProbar = stringResource(R.string.guia_sonido_boton),
            ayudaProbar = stringResource(R.string.guia_sonido_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.MAGNETISMO -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_magnetismo_para_que),
            queVeras = stringResource(R.string.guia_magnetismo_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_magnetismo_paso_1),
                stringResource(R.string.guia_magnetismo_paso_2),
                stringResource(R.string.guia_magnetismo_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_magnetismo_ejemplo),
            botonProbar = stringResource(R.string.guia_magnetismo_boton),
            ayudaProbar = stringResource(R.string.guia_magnetismo_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.DISTANCIA -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_distancia_para_que),
            queVeras = stringResource(R.string.guia_distancia_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_distancia_paso_1),
                stringResource(R.string.guia_distancia_paso_2),
                stringResource(R.string.guia_distancia_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_distancia_ejemplo),
            botonProbar = stringResource(R.string.guia_distancia_boton),
            ayudaProbar = stringResource(R.string.guia_distancia_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.MOVIMIENTO -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_movimiento_para_que),
            queVeras = stringResource(R.string.guia_movimiento_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_movimiento_paso_1),
                stringResource(R.string.guia_movimiento_paso_2),
                stringResource(R.string.guia_movimiento_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_movimiento_ejemplo),
            botonProbar = stringResource(R.string.guia_movimiento_boton),
            ayudaProbar = stringResource(R.string.guia_movimiento_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.FRECUENCIA -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_frecuencia_para_que),
            queVeras = stringResource(R.string.guia_frecuencia_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_frecuencia_paso_1),
                stringResource(R.string.guia_frecuencia_paso_2),
                stringResource(R.string.guia_frecuencia_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_frecuencia_ejemplo),
            botonProbar = stringResource(R.string.guia_frecuencia_boton),
            ayudaProbar = stringResource(R.string.guia_frecuencia_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.ONDAS -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_ondas_para_que),
            queVeras = stringResource(R.string.guia_ondas_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_ondas_paso_1),
                stringResource(R.string.guia_ondas_paso_2),
                stringResource(R.string.guia_ondas_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_ondas_ejemplo),
            botonProbar = stringResource(R.string.guia_ondas_boton),
            ayudaProbar = stringResource(R.string.guia_ondas_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.PENDULO -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_pendulo_para_que),
            queVeras = stringResource(R.string.guia_pendulo_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_pendulo_paso_1),
                stringResource(R.string.guia_pendulo_paso_2),
                stringResource(R.string.guia_pendulo_paso_3),
                stringResource(R.string.guia_pendulo_paso_4)
            ),
            ejemplo = stringResource(R.string.guia_pendulo_ejemplo),
            botonProbar = stringResource(R.string.guia_pendulo_boton),
            ayudaProbar = stringResource(R.string.guia_pendulo_ayuda_boton),
            ayudaGuardar = guardar
        )
        ExperimentoSensorKind.REFLEXION -> ExperimentoGuia(
            paraQue = stringResource(R.string.guia_reflexion_para_que),
            queVeras = stringResource(R.string.guia_reflexion_que_veras),
            pasos = listOf(
                stringResource(R.string.guia_reflexion_paso_1),
                stringResource(R.string.guia_reflexion_paso_2),
                stringResource(R.string.guia_reflexion_paso_3)
            ),
            ejemplo = stringResource(R.string.guia_reflexion_ejemplo),
            botonProbar = stringResource(R.string.guia_reflexion_boton),
            ayudaProbar = stringResource(R.string.guia_reflexion_ayuda_boton),
            ayudaGuardar = guardar
        )
    }
}

@Composable
fun explicacionSensor(uiState: SensorMedicionUiState): String? {
    if (uiState.permisoRequerido) return stringResource(R.string.explica_permiso)
    if (!uiState.disponible) return stringResource(R.string.explica_sin_sensor)
    val midiendo = uiState.medicionEstado == MedicionEstado.ACTIVO
    val valor = uiState.valorPrincipal ?: uiState.historial.lastOrNull()
    if (valor == null) {
        return if (midiendo) stringResource(R.string.explica_esperando) else null
    }
    return when (uiState.kind) {
        ExperimentoSensorKind.LUZ -> {
            val nivel = uiState.lightLevel?.let { textoNivelLuz(it) }
            if (uiState.luzExperimental) {
                stringResource(R.string.explica_luz_relativa, valor)
            } else if (nivel != null) {
                stringResource(R.string.explica_luz, valor, nivel)
            } else {
                stringResource(R.string.explica_luz_simple, valor)
            }
        }
        ExperimentoSensorKind.SONIDO -> {
            val clase = uiState.extras["clasificacion"]
            if (clase != null) {
                stringResource(R.string.explica_sonido_clase, valor, clase)
            } else {
                stringResource(R.string.explica_sonido, valor)
            }
        }
        ExperimentoSensorKind.MAGNETISMO -> stringResource(R.string.explica_magnetismo, valor)
        ExperimentoSensorKind.DISTANCIA -> stringResource(R.string.explica_distancia, valor)
        ExperimentoSensorKind.MOVIMIENTO -> {
            val estado = uiState.extras["estado_movimiento"]
            if (estado != null) {
                stringResource(R.string.explica_movimiento_estado, valor, estado)
            } else {
                stringResource(R.string.explica_movimiento, valor)
            }
        }
        ExperimentoSensorKind.FRECUENCIA -> stringResource(R.string.explica_frecuencia, valor)
        ExperimentoSensorKind.ONDAS -> {
            val hz = uiState.extras["frecuencia_hz"]
            if (hz != null) {
                stringResource(R.string.explica_ondas_hz, valor, hz)
            } else {
                stringResource(R.string.explica_ondas, valor)
            }
        }
        ExperimentoSensorKind.PENDULO -> {
            val periodo = uiState.pendulum.periodSeconds
            if (periodo != null && periodo > 0.0) {
                stringResource(R.string.explica_pendulo, periodo)
            } else if (midiendo) {
                stringResource(R.string.explica_pendulo_esperando)
            } else {
                stringResource(R.string.explica_pendulo_insuficiente)
            }
        }
        ExperimentoSensorKind.REFLEXION -> stringResource(R.string.explica_reflexion, valor)
    }
}

@Composable
fun guiaAceleracion(): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_aceleracion_para_que),
    queVeras = stringResource(R.string.guia_aceleracion_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_aceleracion_paso_1),
        stringResource(R.string.guia_aceleracion_paso_2),
        stringResource(R.string.guia_aceleracion_paso_3)
    ),
    ejemplo = stringResource(R.string.guia_aceleracion_ejemplo),
    botonProbar = stringResource(R.string.guia_aceleracion_boton),
    ayudaProbar = stringResource(R.string.guia_aceleracion_ayuda_boton),
    ayudaGuardar = stringResource(R.string.guia_ayuda_guardar)
)

@Composable
fun guiaCaida(): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_caida_para_que),
    queVeras = stringResource(R.string.guia_caida_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_caida_paso_1),
        stringResource(R.string.guia_caida_paso_2),
        stringResource(R.string.guia_caida_paso_3)
    ),
    ejemplo = stringResource(R.string.guia_caida_ejemplo),
    botonProbar = stringResource(R.string.guia_caida_boton),
    ayudaProbar = stringResource(R.string.guia_caida_ayuda_boton),
    ayudaGuardar = stringResource(R.string.guia_ayuda_guardar)
)

@Composable
fun guiaCamara(): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_camara_para_que),
    queVeras = stringResource(R.string.guia_camara_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_camara_paso_1),
        stringResource(R.string.guia_camara_paso_2),
        stringResource(R.string.guia_camara_paso_3)
    ),
    ejemplo = stringResource(R.string.guia_camara_ejemplo),
    botonProbar = stringResource(R.string.guia_camara_boton),
    ayudaProbar = stringResource(R.string.guia_camara_ayuda_boton),
    ayudaGuardar = stringResource(R.string.guia_camara_ayuda_foto)
)

@Composable
fun guiaObjeto(): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_objeto_para_que),
    queVeras = stringResource(R.string.guia_objeto_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_objeto_paso_1),
        stringResource(R.string.guia_objeto_paso_2),
        stringResource(R.string.guia_objeto_paso_3),
        stringResource(R.string.guia_objeto_paso_4)
    ),
    ejemplo = stringResource(R.string.guia_objeto_ejemplo),
    botonProbar = stringResource(R.string.guia_objeto_boton),
    ayudaProbar = stringResource(R.string.guia_objeto_ayuda_boton),
    ayudaGuardar = stringResource(R.string.guia_ayuda_guardar)
)

@Composable
fun guiaMicroscopio(tipoNombre: String): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_micro_para_que, tipoNombre),
    queVeras = stringResource(R.string.guia_micro_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_micro_paso_1),
        stringResource(R.string.guia_micro_paso_2),
        stringResource(R.string.guia_micro_paso_3)
    ),
    ejemplo = stringResource(R.string.guia_micro_ejemplo),
    botonProbar = stringResource(R.string.guia_micro_boton_foto),
    ayudaProbar = stringResource(R.string.guia_micro_ayuda_foto),
    ayudaGuardar = stringResource(R.string.guia_micro_ayuda_analizar)
)

@Composable
fun guiaCapturaMagnetica(): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_captura_para_que),
    queVeras = stringResource(R.string.guia_captura_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_captura_paso_1),
        stringResource(R.string.guia_captura_paso_2),
        stringResource(R.string.guia_captura_paso_3)
    ),
    ejemplo = stringResource(R.string.guia_captura_ejemplo),
    botonProbar = stringResource(R.string.guia_captura_boton_iniciar),
    ayudaProbar = stringResource(R.string.guia_captura_ayuda_iniciar),
    ayudaGuardar = stringResource(R.string.guia_ayuda_guardar)
)

@Composable
fun guiaMagneticoLab(): ExperimentoGuia = ExperimentoGuia(
    paraQue = stringResource(R.string.guia_maglab_para_que),
    queVeras = stringResource(R.string.guia_maglab_que_veras),
    pasos = listOf(
        stringResource(R.string.guia_maglab_paso_1),
        stringResource(R.string.guia_maglab_paso_2),
        stringResource(R.string.guia_maglab_paso_3)
    ),
    ejemplo = stringResource(R.string.guia_maglab_ejemplo),
    botonProbar = stringResource(R.string.guia_maglab_boton),
    ayudaProbar = stringResource(R.string.guia_maglab_ayuda_boton),
    ayudaGuardar = stringResource(R.string.guia_maglab_ayuda_calibrar)
)
