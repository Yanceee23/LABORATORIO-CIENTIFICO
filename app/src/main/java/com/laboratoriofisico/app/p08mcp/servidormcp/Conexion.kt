package com.laboratoriofisico.app.p08mcp.servidormcp

class McpConexion {
    private var endpoint: String? = null

    fun abrir(endpoint: String) {
        this.endpoint = endpoint
    }

    fun cerrar() {
        endpoint = null
    }

    fun endpointActual(): String? = endpoint
}
