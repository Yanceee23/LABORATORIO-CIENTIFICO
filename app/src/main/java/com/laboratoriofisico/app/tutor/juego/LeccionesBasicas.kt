package com.laboratoriofisico.app.tutor.juego

enum class MateriaAprender {
    FISICA,
    QUIMICA,
    BIOLOGIA
}

data class LeccionBasica(
    val id: String,
    val materia: MateriaAprender,
    val titulo: String,
    val queEs: String,
    val enunciado: String,
    val pista: String,
    val solucion: String,
    val unidad: String = "",
    val valor: Double? = null,
    val textos: List<String> = emptyList()
)

object LeccionesBasicas {
    val todas: List<LeccionBasica> = listOf(
        LeccionBasica(
            id = "fisica-velocidad",
            materia = MateriaAprender.FISICA,
            titulo = "Velocidad",
            queEs = "La velocidad dice qué tan rápido se mueve algo. Se calcula con la distancia dividida entre el tiempo: v = d / t.",
            enunciado = "Un auto recorre 100 m en 20 s. ¿Cuál es su velocidad?",
            pista = "Divide la distancia entre el tiempo.",
            solucion = "v = 100 m / 20 s = 5 m/s",
            unidad = "m/s",
            valor = 5.0
        ),
        LeccionBasica(
            id = "fisica-fuerza",
            materia = MateriaAprender.FISICA,
            titulo = "Fuerza",
            queEs = "La fuerza es un empujón o un jalón. La segunda ley de Newton dice que F = m · a.",
            enunciado = "Una caja de 2 kg acelera a 3 m/s². ¿Qué fuerza se le aplica?",
            pista = "Multiplica la masa por la aceleración.",
            solucion = "F = 2 kg · 3 m/s² = 6 N",
            unidad = "N",
            valor = 6.0
        ),
        LeccionBasica(
            id = "fisica-densidad",
            materia = MateriaAprender.FISICA,
            titulo = "Densidad",
            queEs = "La densidad dice cuánta masa hay en un volumen. Se calcula con ρ = m / V.",
            enunciado = "Un objeto tiene 40 g y ocupa 20 cm³. ¿Cuál es su densidad?",
            pista = "Divide la masa entre el volumen.",
            solucion = "ρ = 40 g / 20 cm³ = 2 g/cm³",
            unidad = "g/cm³",
            valor = 2.0
        ),
        LeccionBasica(
            id = "quimica-atomo",
            materia = MateriaAprender.QUIMICA,
            titulo = "Átomo",
            queEs = "El átomo es la parte más pequeña de un elemento que conserva sus propiedades. Tiene protones, neutrones y electrones.",
            enunciado = "¿Qué partícula del átomo tiene carga positiva?",
            pista = "Está en el núcleo y su carga es +.",
            solucion = "El protón.",
            textos = listOf("proton", "protones")
        ),
        LeccionBasica(
            id = "quimica-mol",
            materia = MateriaAprender.QUIMICA,
            titulo = "Mol",
            queEs = "El mol mide la cantidad de sustancia. Un mol contiene 6,022 × 10²³ partículas. La masa de un mol es la masa molar.",
            enunciado = "El carbono tiene masa molar 12 g/mol. ¿Cuántos moles hay en 24 g de carbono?",
            pista = "Divide los gramos entre la masa molar.",
            solucion = "n = 24 g / 12 g/mol = 2 mol",
            unidad = "mol",
            valor = 2.0
        ),
        LeccionBasica(
            id = "quimica-ph",
            materia = MateriaAprender.QUIMICA,
            titulo = "pH",
            queEs = "El pH indica si una disolución es ácida, neutra o básica. Menor que 7 es ácida, 7 es neutra y mayor que 7 es básica.",
            enunciado = "Una disolución tiene pH 3. ¿Es ácida, neutra o básica?",
            pista = "Compara el 3 con el 7.",
            solucion = "Es ácida, porque 3 es menor que 7.",
            textos = listOf("acida", "acido")
        ),
        LeccionBasica(
            id = "biologia-celula",
            materia = MateriaAprender.BIOLOGIA,
            titulo = "Célula",
            queEs = "La célula es la unidad básica de los seres vivos. Las funciones de la vida ocurren dentro de las células.",
            enunciado = "¿Cómo se llama el organelo que obtiene energía de los alimentos?",
            pista = "Se le llama la central de energía de la célula.",
            solucion = "La mitocondria.",
            textos = listOf("mitocondria", "mitocondrias")
        ),
        LeccionBasica(
            id = "biologia-fotosintesis",
            materia = MateriaAprender.BIOLOGIA,
            titulo = "Fotosíntesis",
            queEs = "La fotosíntesis es el proceso con el que las plantas fabrican su alimento usando luz, agua y dióxido de carbono. Liberan oxígeno.",
            enunciado = "¿Qué gas liberan las plantas durante la fotosíntesis?",
            pista = "Es el gas que respiramos.",
            solucion = "Oxígeno.",
            textos = listOf("oxigeno")
        ),
        LeccionBasica(
            id = "biologia-adn",
            materia = MateriaAprender.BIOLOGIA,
            titulo = "ADN",
            queEs = "El ADN guarda la información genética. Sus bases son adenina (A), timina (T), citosina (C) y guanina (G). A se une con T, y C con G.",
            enunciado = "Si una base es adenina, ¿con cuál base se empareja?",
            pista = "A no se une con C ni con G.",
            solucion = "Con timina.",
            textos = listOf("timina")
        )
    )

    fun de(materia: MateriaAprender): List<LeccionBasica> = todas.filter { it.materia == materia }

    fun acierto(leccion: LeccionBasica, respuesta: String): Boolean {
        val valor = leccion.valor
        if (valor != null) {
            val numero = JuegoEjercicioEngine.parseNumero(respuesta) ?: return false
            val diff = kotlin.math.abs(numero - valor)
            val scale = maxOf(kotlin.math.abs(valor), 1.0)
            return diff <= 1e-3 || diff / scale <= 0.01
        }
        val escrita = normalizar(respuesta)
        if (escrita.isBlank()) return false
        return leccion.textos.any { aceptada ->
            val esperada = normalizar(aceptada)
            escrita == esperada || escrita.contains(esperada)
        }
    }

    private fun normalizar(texto: String): String =
        java.text.Normalizer.normalize(texto.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9 ]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
