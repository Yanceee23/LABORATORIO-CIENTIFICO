package com.laboratoriofisico.app.core.navigation

object NavRoutes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTRO = "registro"
    const val ESTABLECER_PASSWORD = "establecer-password"
    const val HOME = "home"
    const val EXPERIMENTOS = "experimentos"
    const val DOCUMENTOS = "documentos"
    const val ESTUDIO = "estudio"
    const val TUTOR = "tutor"
    const val EXPERIMENTO_DETALLE = "experimento/{experimentoId}"
    const val MEDICIONES = "mediciones/{experimentoId}"
    const val USUARIO = "usuario"
    const val AJUSTES = "ajustes"
    const val ACERCA_DE = "acerca-de"
    const val SENSORES_DIAGNOSTICO = "sensores/diagnostico"
    const val CAPTURA_MAGNETOMETRO = "captura-magnetometro/{experimentoId}"

    // Experimentos hub
    const val EXPERIMENTOS_HUB = "experimentos-hub"
    const val MEDIR_HUB = "medir"
    const val LABORATORIO_HUB = "laboratorio"
    const val LABORATORIO_MAGNETICO = "laboratorio-magnetico"
    const val MEDIR_SONIDO = "experimento/sonido"
    const val MEDIR_FRECUENCIA = "experimento/frecuencia"
    const val ANALIZAR_ONDAS = "experimento/ondas"
    const val MEDIR_MOVIMIENTO = "experimento/movimiento"
    const val MEDIR_DISTANCIA = "experimento/distancia"
    const val MEDIR_ACELERACION_OBJETO = "experimento/aceleracion-objeto"
    const val ESTUDIAR_CAIDA = "experimento/caida"
    const val PENDULO = "experimento/pendulo"
    const val ANALISIS_LUZ = "experimento/luz"
    const val REFLEXION_LUZ = "experimento/reflexion-luz"
    const val EXPERIMENTO_CAMARA = "experimento/camara"
    const val ANALISIS_OBJETOS = "experimento/objetos"
    const val MICROSCOPIO_ANALISIS = "experimento/microscopio/{tipo}"
    const val MEDICION_TEMPLATE = "medicion/{tipo}"

    // Resultados
    const val RESULTADOS = "resultados"
    const val RESULTADO_DETALLE = "resultado/{resultadoId}"

    // Documentos
    const val IMPORTAR_PDF = "documentos/importar"
    const val PDF_VISOR = "documentos/visor/{documentoId}"
    const val PDF_BUSCAR = "documentos/buscar/{documentoId}"

    fun documentoSubRoute(route: String?): Boolean =
        route?.startsWith("documentos/visor/") == true ||
            route?.startsWith("documentos/buscar/") == true

    // Estudio sub-routes (handled in hub with tabs, but deep links possible)
    const val ESTUDIO_RESUMEN = "estudio/resumen"
    const val ESTUDIO_NOTAS = "estudio/notas"
    const val ESTUDIO_PREGUNTAS = "estudio/preguntas"
    const val ESTUDIO_EJERCICIOS = "estudio/ejercicios"
    const val ESTUDIO_EXAMEN = "estudio/examen"
    const val ESTUDIO_TARJETAS = "estudio/tarjetas"
    const val ESTUDIO_HISTORIAL = "estudio/historial"

    // Tutor
    const val TUTOR_APRENDER = "tutor/aprender"
    const val TUTOR_RESOLVER_FOTO = "tutor/resolver-foto"
    const val TUTOR_MIS_EJERCICIOS = "tutor/mis-ejercicios"
    const val TUTOR_EJERCICIO_DETALLE = "tutor/ejercicio/{ejercicioId}"
    const val TUTOR_JUEGO = "tutor/juego/{ejercicioId}"
    const val TUTOR_PDF = "tutor/pdf"
    const val TUTOR_CHAT = "tutor/chat"

    fun tutorEjercicioDetalle(ejercicioId: Long) = "tutor/ejercicio/$ejercicioId"
    fun tutorJuego(ejercicioId: Long) = "tutor/juego/$ejercicioId"
    fun experimentoDetalle(experimentoId: Long) = "experimento/$experimentoId"
    fun mediciones(experimentoId: Long) = "mediciones/$experimentoId"
    fun capturaMagnetometro(experimentoId: Long) = "captura-magnetometro/$experimentoId"
    fun resultadoDetalle(resultadoId: Long) = "resultado/$resultadoId"
    fun pdfVisor(documentoId: Long) = "documentos/visor/$documentoId"
    fun pdfBuscar(documentoId: Long) = "documentos/buscar/$documentoId"
    fun medicionTemplate(tipo: String) = "medicion/$tipo"
    fun microscopioAnalisis(tipo: String) = "experimento/microscopio/$tipo"
}
