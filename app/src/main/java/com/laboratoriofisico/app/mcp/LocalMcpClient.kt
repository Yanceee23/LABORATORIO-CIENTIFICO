package com.laboratoriofisico.app.mcp

import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.p08mcp.servidormcp.LocalServidor
import com.laboratoriofisico.app.p08mcp.servidormcp.ConfiguracionMcp

/**
 * Cliente MCP in-process. Conecta al [LocalServidor] embebido en la app.
 */
class LocalMcpClient(
    private val servidor: LocalServidor,
    private val router: McpNaturalLanguageRouter,
    private val sessionRepository: SessionRepository
) : McpClient {

    private var conectado = false

    override suspend fun conectar(endpoint: String): Boolean {
        return runCatching {
            servidor.iniciar(ConfiguracionMcp(endpoint = endpoint).toServerConfig())
            conectado = servidor.estaActivo()
            conectado
        }.getOrDefault(false)
    }

    override suspend fun enviarConsulta(consulta: String): String? {
        if (!conectado) return "Servidor MCP no conectado. Llamá conectar() primero."
        val context = McpInvocationContext(usuarioId = sessionRepository.getLoggedInUserId())
        return when (val parsed = McpQueryParser.parse(consulta)) {
            is ParsedQuery.ToolCall -> {
                val result = servidor.invocar(parsed.name, parsed.args, context)
                if (result.isError) null else result.text
            }
            is ParsedQuery.NaturalLanguage -> {
                val routed = router.route(parsed.text, context)
                if (routed != null) {
                    if (routed.isError) null else routed.text
                } else {
                    router.helpText()
                }
            }
        }
    }

    override suspend fun listarHerramientas(): List<String> =
        servidor.listarHerramientas().map { "${it.name} — ${it.description}" }

    override suspend fun invocarHerramienta(nombre: String, argumentos: Map<String, Any?>): String {
        val context = McpInvocationContext(usuarioId = sessionRepository.getLoggedInUserId())
        val result = servidor.invocar(nombre, argumentos, context)
        if (result.isError) error(result.text)
        return result.text
    }

    override fun estaConectado(): Boolean = conectado && servidor.estaActivo()
}
