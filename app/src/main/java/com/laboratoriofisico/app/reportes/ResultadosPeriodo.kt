package com.laboratoriofisico.app.reportes

import com.laboratoriofisico.app.reportes.model.ResultadoExportData
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class ResultadoPeriodoItem(
    val timestamp: Long,
    val titulo: String,
    val valor: String,
    val unidad: String,
    val detalle: String
)

data class HoraGrupo<T>(
    val hora: Int,
    val etiqueta: String,
    val items: List<T>
)

data class DiaGrupo<T>(
    val clave: String,
    val etiqueta: String,
    val horas: List<HoraGrupo<T>>
)

object ResultadosPeriodo {
    private val locale = Locale("es", "MX")

    fun desde(data: ResultadoExportData): ResultadoPeriodoItem {
        val titulo = tituloAmigable(data.experimento.titulo)
        return ResultadoPeriodoItem(
            timestamp = data.resultado.timestamp,
            titulo = titulo,
            valor = formatValor(data.resultado.valorCalculado),
            unidad = data.resultado.unidad,
            detalle = detalleAmigable(data, titulo)
        )
    }

    fun <T> agrupar(
        items: List<T>,
        timeZone: TimeZone = TimeZone.getDefault(),
        timestampOf: (T) -> Long
    ): List<DiaGrupo<T>> {
        if (items.isEmpty()) return emptyList()
        val calendar = Calendar.getInstance(timeZone, locale)
        val porDia = items.groupBy { item ->
            calendar.timeInMillis = timestampOf(item)
            "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
        }
        return porDia.entries
            .sortedByDescending { it.key }
            .map { (clave, delDia) ->
                val horas = delDia.groupBy { item ->
                    calendar.timeInMillis = timestampOf(item)
                    calendar.get(Calendar.HOUR_OF_DAY)
                }.entries
                    .sortedBy { it.key }
                    .map { (hora, deLaHora) ->
                        HoraGrupo(
                            hora = hora,
                            etiqueta = etiquetaHora(hora),
                            items = deLaHora.sortedBy { timestampOf(it) }
                        )
                    }
                calendar.timeInMillis = timestampOf(delDia.first())
                DiaGrupo(
                    clave = clave,
                    etiqueta = etiquetaDia(calendar),
                    horas = horas
                )
            }
    }

    fun tituloAmigable(titulo: String): String = when (titulo.trim()) {
        "Análisis de luz" -> "Qué tan iluminado está"
        "Medir sonido" -> "Qué tan fuerte es el sonido"
        "Campo magnético" -> "Campo magnético"
        "Medir distancia" -> "Cuánto caminaste"
        "Movimiento" -> "Cómo se mueve el teléfono"
        "Medir frecuencia" -> "Qué tono tiene el sonido"
        "Analizar ondas" -> "La forma del sonido"
        "Péndulo", "Analizar un péndulo" -> "El vaivén de un péndulo"
        "Reflexión de luz" -> "Compara la luz que llega"
        "Medir la aceleración de un objeto" -> "Qué tan brusco se acelera"
        "Estudiar una caída" -> "Observa una caída"
        "Análisis de movimiento de objetos" -> "Sigue un objeto con la cámara"
        else -> titulo.ifBlank { "Experimento" }
    }

    fun formatValor(value: Double): String {
        val abs = kotlin.math.abs(value)
        val pattern = when {
            abs >= 100.0 -> "%.0f"
            abs >= 10.0 -> "%.1f"
            else -> "%.2f"
        }
        return pattern.format(Locale.US, value).replace('.', ',')
    }

    private fun detalleAmigable(data: ResultadoExportData, titulo: String): String {
        val lectura = data.valoresTexto["lectura_usuario"]
            ?.takeIf { it.isNotBlank() && "event.values" !in it }
        if (lectura != null) return lectura.replace('.', ',')
        val clasificacion = data.valoresTexto["clasificacion"]?.takeIf { it.isNotBlank() }
        val sensor = sensorAmigable(data.sensor)
        val base = "Se guardó en «$titulo»."
        return when {
            clasificacion != null && sensor != null -> "$base Nivel: $clasificacion. Lo midió el $sensor."
            clasificacion != null -> "$base Nivel: $clasificacion."
            sensor != null -> "$base Lo midió el $sensor."
            data.experimento.descripcion.isNotBlank() -> data.experimento.descripcion
            else -> base
        }
    }

    private fun sensorAmigable(sensor: String?): String? = when (sensor?.uppercase()) {
        "ACCELEROMETER" -> "acelerómetro"
        "GYROSCOPE" -> "giroscopio"
        "MAGNETOMETER" -> "magnetómetro"
        "LIGHT" -> "sensor de luz"
        "MICROPHONE" -> "micrófono"
        "LOCATION" -> "GPS"
        "CAMERA" -> "cámara"
        null, "" -> null
        else -> sensor.lowercase().replace('_', ' ')
    }

    private fun etiquetaDia(calendar: Calendar): String {
        val nombre = DIA[calendar.get(Calendar.DAY_OF_WEEK)] ?: ""
        val mes = MES[calendar.get(Calendar.MONTH)] ?: ""
        return "$nombre ${calendar.get(Calendar.DAY_OF_MONTH)} de $mes de ${calendar.get(Calendar.YEAR)}"
    }

    private fun etiquetaHora(hora: Int): String {
        val inicio = "%02d:00".format(hora)
        val fin = "%02d:00".format((hora + 1) % 24)
        return "De las $inicio a las $fin"
    }

    private val DIA = mapOf(
        Calendar.SUNDAY to "Domingo",
        Calendar.MONDAY to "Lunes",
        Calendar.TUESDAY to "Martes",
        Calendar.WEDNESDAY to "Miércoles",
        Calendar.THURSDAY to "Jueves",
        Calendar.FRIDAY to "Viernes",
        Calendar.SATURDAY to "Sábado"
    )

    private val MES = mapOf(
        Calendar.JANUARY to "enero",
        Calendar.FEBRUARY to "febrero",
        Calendar.MARCH to "marzo",
        Calendar.APRIL to "abril",
        Calendar.MAY to "mayo",
        Calendar.JUNE to "junio",
        Calendar.JULY to "julio",
        Calendar.AUGUST to "agosto",
        Calendar.SEPTEMBER to "septiembre",
        Calendar.OCTOBER to "octubre",
        Calendar.NOVEMBER to "noviembre",
        Calendar.DECEMBER to "diciembre"
    )
}
