package com.laboratoriofisico.app.configuracion

import com.laboratoriofisico.app.backend.model.Configuracion

object ConfiguracionDefaults {
    /** Valores por defecto en memoria para invitado (no se guardan en Room). */
    fun crear(usuarioId: Long = 0L): Configuracion = Configuracion(
        id = 0,
        usuarioId = usuarioId,
        temaOscuro = false,
        idioma = "es",
        unidadesMetricas = true,
        ultimaSincronizacion = null
    )
}
