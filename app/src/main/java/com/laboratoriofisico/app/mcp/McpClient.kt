package com.laboratoriofisico.app.mcp

/**
 * Cliente MCP para herramientas locales e integración con flujos de tutoría/estudio.
 */
interface McpClient {
    suspend fun conectar(endpoint: String): Boolean
    suspend fun enviarConsulta(consulta: String): String?
    suspend fun listarHerramientas(): List<String>
    suspend fun invocarHerramienta(nombre: String, argumentos: Map<String, Any?>): String
    fun estaConectado(): Boolean
}
