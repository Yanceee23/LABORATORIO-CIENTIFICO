package com.laboratoriofisico.app.backend.model

/**
 * Ejercicio resuelto con foto (Fase 1) + campos para presentación y juego (Fase 2).
 */
data class EjercicioDato(
    val nombre: String,
    val valor: Double,
    val unidad: String
)

data class EjercicioVariable(
    val simbolo: String,
    val nombre: String,
    val unidad: String
)

data class EjercicioPaso(
    val numero: Int,
    val titulo: String,
    val explicacion: String,
    val operacion: String
)

data class EjercicioResultadoValor(
    val valor: Double,
    val unidad: String
)

enum class MateriaEjercicio(val key: String) {
    FISICA("fisica"),
    MATEMATICA("matematica"),
    QUIMICA("quimica"),
    BIOLOGIA("biologia");

    companion object {
        fun from(raw: String?): MateriaEjercicio =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) }
                ?: FISICA
    }
}

enum class ConfianzaEjercicio(val key: String) {
    BAJA("baja"),
    MEDIA("media"),
    ALTA("alta");

    companion object {
        fun from(raw: String?): ConfianzaEjercicio =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) }
                ?: MEDIA
    }
}

enum class VerificacionEjercicio(val key: String) {
    COINCIDE("coincide"),
    NO_COINCIDE("no_coincide"),
    SIN_VERIFICACION("sin_verificacion");

    companion object {
        fun from(raw: String?): VerificacionEjercicio =
            entries.firstOrNull { it.key.equals(raw?.trim(), ignoreCase = true) }
                ?: SIN_VERIFICACION
    }
}

data class EjercicioResuelto(
    val id: Long = 0,
    val usuarioId: Long,
    val fotoPath: String,
    val materia: MateriaEjercicio,
    val tema: String,
    val enunciado: String,
    val datos: List<EjercicioDato>,
    val incognita: String,
    val formula: String,
    val formulaLegible: String = "",
    val variables: List<EjercicioVariable> = emptyList(),
    val explicacionFormula: String = "",
    val pasos: List<EjercicioPaso>,
    val resultado: EjercicioResultadoValor,
    val verificacion: VerificacionEjercicio,
    val confianza: ConfianzaEjercicio,
    val fechaCreacion: Long
) {
    fun puedeRetoGuiado(): Boolean =
        datos.size >= 2 && formula.isNotBlank()
}

data class JuegoEjercicioProgreso(
    val id: Long = 0,
    val usuarioId: Long,
    val ejercicioId: Long,
    val intentos: Int = 0,
    val estrellas: Int = 0,
    val pistasUsadas: Int = 0,
    val completo: Boolean = false,
    val fechaActualizacion: Long = System.currentTimeMillis()
)
