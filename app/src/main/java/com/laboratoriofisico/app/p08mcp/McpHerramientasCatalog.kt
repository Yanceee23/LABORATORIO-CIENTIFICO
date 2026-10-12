package com.laboratoriofisico.app.p08mcp

/**
 * Catálogo de herramientas MCP implementadas en [com.laboratoriofisico.app.mcp.McpToolRegistry].
 * Reemplaza los stubs vacíos de herramientaspdf, herramientasfisica, herramientasestudio y herramientasresultados.
 */
object McpHerramientasCatalog {
    val pdf = listOf(
        "listar_documentos",
        "buscar_documento",
        "buscar_texto",
        "obtener_pagina",
        "obtener_seccion",
        "obtener_contexto"
    )
    val fisica = listOf(
        "calcular_velocidad",
        "calcular_aceleracion",
        "calcular_distancia",
        "calcular_frecuencia",
        "calcular_periodo",
        "calcular_onda",
        "calcular_gravedad",
        "calcular_magnetismo"
    )
    val estudio = listOf(
        "crear_resumen",
        "crear_notas",
        "crear_preguntas",
        "crear_ejercicios",
        "crear_examen",
        "crear_tarjetas"
    )
    val resultados = listOf(
        "consultar_resultado",
        "comparar_resultados",
        "analizar_resultado"
    )

    val todas: List<String> = pdf + fisica + estudio + resultados
}
