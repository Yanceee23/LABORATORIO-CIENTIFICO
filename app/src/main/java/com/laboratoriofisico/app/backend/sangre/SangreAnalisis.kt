package com.laboratoriofisico.app.backend.sangre

import org.json.JSONObject

object SangreAnalisis {
    private val prohibidas = listOf(
        "diabetes", "anemia", "infección", "infeccion", "leucemia",
        "normal", "anormal", "sano", "enfermo"
    )

    fun presentar(respuesta: String, datosRegistrados: String): String {
        val json = jsonDe(respuesta)
        if (json == null) {
            val texto = respuesta.trim()
            return if (texto.startsWith("No soy doctor.")) texto else {
                "No soy doctor. Esta es una observación educativa y no un diagnóstico.\n\n$texto"
            }
        }
        val calidad = campo(json, "calidad")
        val color = campo(json, "color")
        val rojos = conteo(json, "rojos_estimado", "rojos_min", "rojos_max")
        val blancos = conteo(json, "blancos_estimado", "blancos_min", "blancos_max")
        val confianza = campo(json, "confianza").ifBlank { "no se puede determinar con esta imagen" }
        val forma = campo(json, "rojos_forma")
        val aspectoBlancos = campo(json, "blancos_aspecto")
        val plaquetas = campo(json, "plaquetas")
        return """
            No soy doctor. Esta es una observación educativa y no un diagnóstico.

            ## Calidad de la imagen
            $calidad

            ## Color y aspecto
            $color

            ## Glóbulos rojos
            Forma y tamaño: $forma
            Conteo estimado: ${rojos.frase}. Confianza: $confianza.
            Estimado rojos: ${rojos.estimado ?: "no se puede determinar con esta imagen"}

            ## Glóbulos blancos
            ${if (blancos.estimado == null) "no se distinguen con claridad" else "Conteo estimado: ${blancos.frase}."}
            Aspecto: $aspectoBlancos
            Estimado blancos: ${blancos.estimado ?: "no se puede determinar con esta imagen"}

            ## Plaquetas, grumos, burbujas e impurezas
            $plaquetas

            ## Datos registrados
            $datosRegistrados

            ## Nota educativa
            Con una foto de microscopio y estos datos no se puede concluir nada sobre tu salud. Para interpretar tus resultados consulta a un médico o a un laboratorio clínico.

            ## Tipo de sangre
            No se puede saber el tipo de sangre con una imagen.
        """.trimIndent()
    }

    fun conteos(texto: String): Pair<Int?, Int?> {
        val rojos = Regex("Estimado rojos:\\s*(\\d+)").find(texto)?.groupValues?.get(1)?.toIntOrNull()
        val blancos = Regex("Estimado blancos:\\s*(\\d+)").find(texto)?.groupValues?.get(1)?.toIntOrNull()
        return rojos?.takeIf { it > 0 } to blancos?.takeIf { it > 0 }
    }

    private fun jsonDe(respuesta: String): JSONObject? {
        val limpio = respuesta.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val inicio = limpio.indexOf('{')
        val fin = limpio.lastIndexOf('}')
        if (inicio < 0 || fin <= inicio) return null
        return runCatching { JSONObject(limpio.substring(inicio, fin + 1)) }.getOrNull()
    }

    private fun campo(json: JSONObject, clave: String): String {
        val valor = json.optString(clave).trim()
        if (valor.isBlank() || valor.equals("null", ignoreCase = true)) {
            return "no se puede determinar con esta imagen"
        }
        val minuscula = valor.lowercase()
        if (prohibidas.any { minuscula.contains(it) }) {
            return "no se puede determinar con esta imagen"
        }
        return valor
    }

    private fun conteo(json: JSONObject, estimado: String, minimo: String, maximo: String): Conteo {
        val centro = entero(json, estimado)
        val min = entero(json, minimo)
        val max = entero(json, maximo)
        val frase = if (centro == null) {
            "no se puede determinar con esta imagen"
        } else if (min != null && max != null) {
            "unos $centro, entre $min y $max"
        } else {
            "unos $centro"
        }
        return Conteo(centro, frase)
    }

    private fun entero(json: JSONObject, clave: String): Int? {
        if (!json.has(clave) || json.isNull(clave)) return null
        val texto = json.opt(clave)?.toString()?.trim().orEmpty()
        if (prohibidas.any { texto.lowercase().contains(it) }) return null
        return Regex("\\d+").find(texto)?.value?.toIntOrNull()?.takeIf { it > 0 }
    }

    private data class Conteo(val estimado: Int?, val frase: String)
}
