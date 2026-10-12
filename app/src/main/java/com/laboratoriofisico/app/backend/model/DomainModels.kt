package com.laboratoriofisico.app.backend.model

data class Usuario(
    val id: Long = 0,
    val nombre: String,
    val email: String,
    val rol: String,
    val fechaCreacion: Long
)

data class Ubicacion(
    val id: Long = 0,
    val usuarioId: Long,
    val nombre: String,
    val latitud: Double?,
    val longitud: Double?,
    val descripcion: String
)

enum class EstadoExperimento {
    BORRADOR,
    EN_PROGRESO,
    COMPLETADO,
    ARCHIVADO
}

data class Experimento(
    val id: Long = 0,
    val titulo: String,
    val descripcion: String,
    val usuarioId: Long,
    val ubicacionId: Long?,
    val estado: EstadoExperimento,
    val fechaCreacion: Long
)

data class Medicion(
    val id: Long = 0,
    val experimentoId: Long,
    val usuarioId: Long,
    val nombre: String,
    val valor: Double,
    val unidad: String,
    val timestamp: Long
)

data class Resultado(
    val id: Long = 0,
    val medicionId: Long,
    val usuarioId: Long,
    val valorCalculado: Double,
    val unidad: String,
    val notas: String,
    val timestamp: Long
)

enum class TipoAnalisisMicroscopio(val key: String, val titulo: String) {
    YOGUR("yogur", "Bacterias del yogur"),
    PIEL("piel", "Piel y escamas"),
    HOJA("hoja", "Células de la hoja"),
    SANGRE("sangre", "Sangre")
}

data class AnalisisMicroscopio(
    val id: Long = 0,
    val experimentoId: Long,
    val tipoExperimento: TipoAnalisisMicroscopio,
    val subtipo: String?,
    val imagenPath: String,
    val analisis: String,
    val resumenParaComparacion: String,
    val promptVersion: Int,
    val timestamp: Long
)

data class Configuracion(
    val id: Long = 1,
    val usuarioId: Long = 0,
    val temaOscuro: Boolean,
    val idioma: String,
    val unidadesMetricas: Boolean,
    val ultimaSincronizacion: Long?
)

data class ResumenDashboard(
    val totalExperimentos: Int,
    val totalMediciones: Int,
    val experimentosActivos: Int
)

data class Documento(
    val id: Long = 0,
    val usuarioId: Long,
    val nombre: String,
    val nombreArchivo: String,
    val rutaArchivo: String,
    val extension: String,
    val tamano: Long,
    val numeroPaginas: Int,
    val fechaImportacion: Long,
    val estadoProcesamiento: String
)

data class SeccionDocumento(
    val id: Long = 0,
    val documentoId: Long,
    val numeroPagina: Int,
    val titulo: String,
    val contenido: String,
    val orden: Int
)

data class ResultadoBusqueda(
    val seccionId: Long,
    val titulo: String,
    val fragmento: String,
    val numeroPagina: Int
)

enum class TipoResumen { AUTOMATICO, CORTO, DETALLADO }

enum class Dificultad(val key: String, val label: String) {
    FACIL("facil", "Fácil"),
    MEDIA("media", "Media"),
    DIFICIL("dificil", "Difícil")
}

data class PreguntaEstudio(
    val id: Long = 0,
    val documentoId: Long,
    val pregunta: String,
    val respuesta: String,
    val explicacion: String,
    val dificultad: String,
    val fechaCreacion: Long
)

data class EjercicioEstudio(
    val id: Long = 0,
    val documentoId: Long,
    val enunciado: String,
    val datos: String,
    val formula: String,
    val solucion: String,
    val explicacion: String,
    val dificultad: String,
    val fechaCreacion: Long
)

data class TarjetaEstudio(
    val id: Long = 0,
    val documentoId: Long,
    val frente: String,
    val reverso: String,
    val dificultad: String,
    val estado: String,
    val fechaCreacion: Long
)

data class HistorialEstudio(
    val id: Long = 0,
    val documentoId: Long,
    val tipoActividad: String,
    val resultado: String,
    val fecha: Long,
    val tiempoEstudio: Long
)

data class ExamenConfig(
    val numeroPreguntas: Int = 5,
    val tiempoMinutos: Int = 10
)

data class ExamenPregunta(
    val id: Int,
    val pregunta: String,
    val opciones: List<String>,
    val respuestaCorrecta: Int
)

data class Examen(
    val preguntas: List<ExamenPregunta>,
    val config: ExamenConfig
)

data class MensajeChat(
    val id: Long = 0,
    val texto: String,
    val esUsuario: Boolean,
    val fechaHora: Long
)

data class IaContexto(
    val documentoId: Long? = null,
    val resultadoId: Long? = null,
    val formula: String? = null,
    val concepto: String? = null,
    val enunciado: String? = null,
    val datos: String? = null
)

data class IaRespuesta(
    val texto: String,
    val esStub: Boolean = true
)

data class MedicionConResultados(
    val medicion: Medicion,
    val resultados: List<Resultado>
)
