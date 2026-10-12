package com.laboratoriofisico.app.ia.gemini

import com.laboratoriofisico.app.backend.model.EjercicioEstudio
import com.laboratoriofisico.app.backend.model.Examen
import com.laboratoriofisico.app.backend.model.ExamenConfig
import com.laboratoriofisico.app.backend.model.ExamenPregunta
import com.laboratoriofisico.app.backend.model.PreguntaEstudio
import com.laboratoriofisico.app.backend.model.TarjetaEstudio
import org.json.JSONArray
import org.json.JSONObject

object GeminiResponseParser {
    fun extraerJson(texto: String): String {
        val trimmed = texto.trim()
        val fence = Regex("```(?:json)?\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
        val match = fence.find(trimmed)
        return match?.groupValues?.get(1)?.trim() ?: trimmed
    }

    fun parsePreguntas(
        texto: String,
        documentoId: Long,
        dificultad: String
    ): List<PreguntaEstudio> {
        val json = JSONObject(extraerJson(texto))
        val array = json.optJSONArray("preguntas") ?: JSONArray()
        val now = System.currentTimeMillis()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val pregunta = item.optString("pregunta").trim()
                val respuesta = item.optString("respuesta").trim()
                if (pregunta.isBlank() || respuesta.isBlank()) continue
                add(
                    PreguntaEstudio(
                        documentoId = documentoId,
                        pregunta = pregunta,
                        respuesta = respuesta,
                        explicacion = item.optString("explicacion").trim(),
                        dificultad = dificultad,
                        fechaCreacion = now
                    )
                )
            }
        }
    }

    fun parseEjercicio(texto: String, documentoId: Long): EjercicioEstudio {
        val item = JSONObject(extraerJson(texto))
        val enunciado = item.optString("enunciado").trim()
        val solucion = item.optString("solucion").trim()
        require(enunciado.isNotBlank() && solucion.isNotBlank()) {
            "La IA no generó un ejercicio válido."
        }
        return EjercicioEstudio(
            documentoId = documentoId,
            enunciado = enunciado,
            datos = item.optString("datos").trim(),
            formula = item.optString("formula").trim(),
            solucion = solucion,
            explicacion = item.optString("explicacion").trim(),
            dificultad = item.optString("dificultad", "media").trim().ifBlank { "media" },
            fechaCreacion = System.currentTimeMillis()
        )
    }

    fun parseTarjetas(texto: String, documentoId: Long): List<TarjetaEstudio> {
        val json = JSONObject(extraerJson(texto))
        val array = json.optJSONArray("tarjetas") ?: JSONArray()
        val now = System.currentTimeMillis()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val frente = item.optString("frente").trim()
                val reverso = item.optString("reverso").trim()
                if (frente.isBlank() || reverso.isBlank()) continue
                add(
                    TarjetaEstudio(
                        documentoId = documentoId,
                        frente = frente,
                        reverso = reverso,
                        dificultad = item.optString("dificultad", "media").trim().ifBlank { "media" },
                        estado = "nueva",
                        fechaCreacion = now
                    )
                )
            }
        }
    }

    fun parseExamen(texto: String, config: ExamenConfig): Examen {
        val json = JSONObject(extraerJson(texto))
        val array = json.optJSONArray("preguntas") ?: JSONArray()
        val preguntas = buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val pregunta = item.optString("pregunta").trim()
                if (pregunta.isBlank()) continue
                val opcionesJson = item.optJSONArray("opciones") ?: JSONArray()
                val opciones = buildList {
                    for (j in 0 until opcionesJson.length()) {
                        add(opcionesJson.optString(j).trim())
                    }
                }.filter { it.isNotBlank() }
                if (opciones.size < 2) continue
                val respuestaCorrecta = item.optInt("respuestaCorrecta", 0)
                    .coerceIn(0, opciones.lastIndex)
                add(
                    ExamenPregunta(
                        id = size + 1,
                        pregunta = pregunta,
                        opciones = opciones,
                        respuestaCorrecta = respuestaCorrecta
                    )
                )
            }
        }
        require(preguntas.isNotEmpty()) { "La IA no generó preguntas de examen válidas." }
        return Examen(preguntas = preguntas, config = config)
    }
}
