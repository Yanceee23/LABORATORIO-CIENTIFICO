package com.laboratoriofisico.app.backend.sangre

import java.util.Locale
import kotlin.math.round

enum class TipoConteoNeubauer(val titulo: String) {
    ROJOS("Glóbulos rojos"),
    BLANCOS("Glóbulos blancos"),
    OTRA("Otra dilución")
}

object NeubauerConteo {
    const val UNIDAD = "células/µL"
    const val UNIDAD_GLUCOSA = "mg/dL"

    fun calcular(
        tipo: TipoConteoNeubauer,
        celulasContadas: Int,
        dilucion: Double? = null,
        volumenMicrolitros: Double? = null
    ): Double? {
        if (celulasContadas <= 0) return null
        val n = celulasContadas.toDouble()
        return when (tipo) {
            TipoConteoNeubauer.ROJOS -> n * 10_000.0
            TipoConteoNeubauer.BLANCOS -> n * 50.0
            TipoConteoNeubauer.OTRA -> {
                if (dilucion == null || dilucion <= 0.0 || volumenMicrolitros == null || volumenMicrolitros <= 0.0) {
                    null
                } else {
                    n * dilucion / volumenMicrolitros
                }
            }
        }
    }

    fun texto(valor: Double): String {
        val redondeado = round(valor * 100.0) / 100.0
        return if (redondeado == redondeado.toLong().toDouble()) {
            redondeado.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", redondeado)
        }
    }
}
