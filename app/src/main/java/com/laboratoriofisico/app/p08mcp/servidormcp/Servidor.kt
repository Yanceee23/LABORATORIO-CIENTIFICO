package com.laboratoriofisico.app.p08mcp.servidormcp

import com.laboratoriofisico.app.mcp.McpInvocationContext
import com.laboratoriofisico.app.mcp.McpServerConfig
import com.laboratoriofisico.app.mcp.McpToolInfo
import com.laboratoriofisico.app.mcp.McpToolRegistry
import com.laboratoriofisico.app.mcp.McpToolResult

interface Servidor {
    suspend fun iniciar(config: McpServerConfig)
    suspend fun detener()
    fun estaActivo(): Boolean
    fun listarHerramientas(): List<McpToolInfo>
    suspend fun invocar(
        nombre: String,
        argumentos: Map<String, Any?>,
        contexto: McpInvocationContext
    ): McpToolResult
}

class LocalServidor(
    private val registry: McpToolRegistry,
    private val seguridad: McpSeguridad,
    private val conexion: McpConexion
) : Servidor {
    private var activo = false
    private var config: McpServerConfig = McpServerConfig()

    override suspend fun iniciar(config: McpServerConfig) {
        seguridad.validarEndpoint(config.endpoint)
        this.config = config
        conexion.abrir(config.endpoint)
        activo = config.habilitado
    }

    override suspend fun detener() {
        conexion.cerrar()
        activo = false
    }

    override fun estaActivo(): Boolean = activo

    override fun listarHerramientas(): List<McpToolInfo> = registry.listTools()

    override suspend fun invocar(
        nombre: String,
        argumentos: Map<String, Any?>,
        contexto: McpInvocationContext
    ): McpToolResult {
        if (!activo) {
            return McpToolResult("Servidor MCP no iniciado.", isError = true)
        }
        seguridad.validarInvocacion(contexto)
        return registry.invoke(nombre, argumentos, contexto)
    }
}
