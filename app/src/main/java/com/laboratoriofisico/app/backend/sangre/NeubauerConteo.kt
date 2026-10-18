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
    ): Double? = when (tipo) {
        TipoConteoNeubauer.ROJOS -> rojos(celulasContadas)
        TipoConteoNeubauer.BLANCOS -> blancos(celulasContadas)
        TipoConteoNeubauer.OTRA -> general(celulasContadas, dilucion ?: 0.0, volumenMicrolitros ?: 0.0)
    }

    fun rojos(celulasContadas: Int): Double? {
        if (celulasContadas <= 0) return null
        return celulasContadas * 10_000.0
    }

    fun blancos(celulasContadas: Int): Double? {
        if (celulasContadas <= 0) return null
        return celulasContadas * 50.0
    }

    fun general(celulasContadas: Int, dilucion: Double, areaMm2: Double): Double? {
        if (celulasContadas <= 0 || dilucion <= 0.0 || areaMm2 <= 0.0) return null
        return (celulasContadas * dilucion) / (areaMm2 * 0.1)
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
