package com.laboratoriofisico.app.backend.sangre

object SangreFormulario {
    const val MG_DL = "mg/dL"
    const val MMOL_L = "mmol/L"

    fun hora(hora: String, minuto: String, periodo: String): String {
        val h = hora.trim().toIntOrNull() ?: return "no indicada"
        val m = minuto.trim().toIntOrNull() ?: return "no indicada"
        if (h !in 1..12 || m !in 0..59) return "no indicada"
        val amPm = if (periodo.equals("PM", ignoreCase = true)) "PM" else "AM"
        return "%02d:%02d %s".format(h, m, amPm)
    }

    fun glucosa(valor: String, unidad: String): String {
        val numero = valor.numeroPositivo() ?: return "no indicada"
        val unidadElegida = if (unidad == MMOL_L) MMOL_L else MG_DL
        val equivalente = equivalente(numero, unidadElegida) ?: return "no indicada"
        return "$numero $unidadElegida ($equivalente)"
    }

    fun equivalente(numero: Double, unidad: String): String? {
        if (numero <= 0.0) return null
        return if (unidad == MMOL_L) {
            "${NeubauerConteo.texto(numero * 18.0)} mg/dL"
        } else {
            "${NeubauerConteo.texto(numero / 18.0)} mmol/L"
        }
    }

    fun actividad(hace: Boolean, minutos: String, tipo: String): String {
        if (!hace) return "No"
        val detalle = minutos.numeroPositivo()?.let { "${NeubauerConteo.texto(it)} minutos por día" }
        val nombre = tipo.trim().takeIf { it.isNotEmpty() }
        return listOfNotNull("Sí", detalle, nombre).joinToString(". ")
    }

    fun contexto(
        comio: String,
        hora: String,
        minuto: String,
        periodo: String,
        glucosa: String,
        unidadGlucosa: String,
        haceActividad: Boolean,
        minutosActividad: String,
        tipoActividad: String
    ): String = """
        Qué comió: ${comio.trim().ifBlank { "no indicado" }}
        Hora de la comida: ${hora(hora, minuto, periodo)}
        Glucosa: ${glucosa(glucosa, unidadGlucosa)}
        Actividad física: ${actividad(haceActividad, minutosActividad, tipoActividad)}
    """.trimIndent()

    private fun String.numeroPositivo(): Double? {
        val valor = trim().replace(',', '.').toDoubleOrNull() ?: return null
        return valor.takeIf { it > 0.0 }
    }
}
