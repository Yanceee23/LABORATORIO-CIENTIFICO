package com.laboratoriofisico.app.mcp

import com.laboratoriofisico.app.backend.model.ExamenConfig
import com.laboratoriofisico.app.backend.model.TipoResumen
import com.laboratoriofisico.app.backend.repository.DocumentoRepository
import com.laboratoriofisico.app.backend.repository.MedicionRepository
import com.laboratoriofisico.app.backend.repository.ResultadoRepository
import com.laboratoriofisico.app.backend.repository.SeccionDocumentoRepository
import com.laboratoriofisico.app.backend.session.SessionRepository
import com.laboratoriofisico.app.estudio.EstudioContentService
import com.laboratoriofisico.app.fisica.FormulaId
import com.laboratoriofisico.app.fisica.FisicaEngine
import com.laboratoriofisico.app.ia.IaService
import com.laboratoriofisico.app.ia.gemini.DocumentoContextoHelper
import com.laboratoriofisico.app.pdf.PdfProcessingService
import com.laboratoriofisico.app.p08mcp.flujos.AnalizarExperimentoFlow
import com.laboratoriofisico.app.p08mcp.flujos.EstudiarPdfFlow
import com.laboratoriofisico.app.p08mcp.flujos.PreguntaSobrePdfFlow
import com.laboratoriofisico.app.p08mcp.flujos.ResolverEjercicioFlow
import com.laboratoriofisico.app.p08mcp.servidormcp.LocalServidor
import com.laboratoriofisico.app.p08mcp.servidormcp.McpConexion
import com.laboratoriofisico.app.p08mcp.servidormcp.McpSeguridad
import kotlinx.coroutines.flow.first

data class McpComponents(
    val registry: McpToolRegistry,
    val servidor: LocalServidor,
    val client: LocalMcpClient,
    val preguntaSobrePdf: PreguntaSobrePdfFlow,
    val estudiarPdf: EstudiarPdfFlow,
    val resolverEjercicio: ResolverEjercicioFlow,
    val analizarExperimento: AnalizarExperimentoFlow
)

object McpToolsFactory {
    fun build(
        sessionRepository: SessionRepository,
        documentoRepository: DocumentoRepository,
        seccionDocumentoRepository: SeccionDocumentoRepository,
        pdfProcessingService: PdfProcessingService,
        documentoContextoHelper: DocumentoContextoHelper,
        fisicaEngine: FisicaEngine,
        estudioContentService: EstudioContentService,
        resultadoRepository: ResultadoRepository,
        medicionRepository: MedicionRepository,
        iaService: IaService
    ): McpComponents {
        val handlers = buildList {
            addAll(pdfTools(documentoRepository, seccionDocumentoRepository, pdfProcessingService, documentoContextoHelper))
            addAll(fisicaTools(fisicaEngine))
            addAll(estudioTools(estudioContentService))
            addAll(resultadosTools(resultadoRepository, medicionRepository, iaService))
        }
        val registry = McpToolRegistry(handlers)
        val seguridad = McpSeguridad(sessionRepository)
        val conexion = McpConexion()
        val servidor = LocalServidor(registry, seguridad, conexion)
        val client = LocalMcpClient(servidor, McpNaturalLanguageRouter(registry), sessionRepository)

        val preguntaSobrePdf = PreguntaSobrePdfFlow(iaService)
        val estudiarPdf = EstudiarPdfFlow(estudioContentService)
        val resolverEjercicio = ResolverEjercicioFlow(iaService, fisicaEngine)
        val analizarExperimento = AnalizarExperimentoFlow(iaService, resultadoRepository)

        return McpComponents(
            registry = registry,
            servidor = servidor,
            client = client,
            preguntaSobrePdf = preguntaSobrePdf,
            estudiarPdf = estudiarPdf,
            resolverEjercicio = resolverEjercicio,
            analizarExperimento = analizarExperimento
        )
    }

    private fun pdfTools(
        documentoRepository: DocumentoRepository,
        seccionDocumentoRepository: SeccionDocumentoRepository,
        pdfProcessingService: PdfProcessingService,
        documentoContextoHelper: DocumentoContextoHelper
    ): List<McpToolHandler> = listOf(
        tool("listar_documentos", "Lista PDFs del usuario actual") { _, ctx ->
            val userId = ctx.usuarioId ?: return@tool error("Sesión requerida")
            val docs = documentoRepository.observeByUsuario(userId).first()
            ok(
                docs.joinToString("\n") { "- #${it.id} ${it.nombre} (${it.estadoProcesamiento})" }
                    .ifBlank { "Sin documentos importados." }
            )
        },
        tool("buscar_documento", "Busca documentos por nombre") { args, ctx ->
            val userId = ctx.usuarioId ?: return@tool error("Sesión requerida")
            val query = args.string("query") ?: return@tool error("Falta query")
            val docs = documentoRepository.observeByUsuario(userId).first()
                .filter { it.nombre.contains(query, ignoreCase = true) }
            ok(
                docs.joinToString("\n") { "- #${it.id} ${it.nombre}" }
                    .ifBlank { "Sin coincidencias." }
            )
        },
        tool("buscar_texto", "Busca texto dentro de un PDF") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val query = args.string("query") ?: return@tool error("Falta query")
            val hits = pdfProcessingService.buscarTexto(docId, query)
            ok(
                hits.joinToString("\n") { "p.${it.numeroPagina}: ${it.fragmento}" }
                    .ifBlank { "Sin coincidencias en el documento." }
            )
        },
        tool("obtener_pagina", "Texto de una página del PDF") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val pagina = args.int("pagina") ?: return@tool error("Falta pagina")
            val secciones = seccionDocumentoRepository.getByDocumento(docId)
                .filter { it.numeroPagina == pagina }
            ok(secciones.joinToString("\n") { it.contenido }.ifBlank { "Página sin texto." })
        },
        tool("obtener_seccion", "Contenido de una sección") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val seccionId = args.long("seccionId") ?: return@tool error("Falta seccionId")
            val seccion = seccionDocumentoRepository.getByDocumento(docId)
                .find { it.id == seccionId }
                ?: return@tool error("Sección no encontrada")
            ok("${seccion.titulo}\n${seccion.contenido}")
        },
        tool("obtener_contexto", "Contexto del PDF para una consulta") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val consulta = args.string("consulta")
            val ctx = documentoContextoHelper.cargarContexto(docId, consulta)
            ok(ctx.texto.take(4000))
        }
    )

    private fun fisicaTools(engine: FisicaEngine): List<McpToolHandler> = listOf(
        calcTool(engine, "calcular_velocidad", FormulaId.V_IGUAL_D_T, mapOf("d" to "d", "t" to "t")),
        calcTool(engine, "calcular_aceleracion", FormulaId.A_IGUAL_DELTA_V_DELTA_T, mapOf("dv" to "dv", "dt" to "dt")),
        calcTool(engine, "calcular_distancia", FormulaId.D_IGUAL_V_T, mapOf("v" to "v", "t" to "t")),
        calcTool(engine, "calcular_frecuencia", FormulaId.F_IGUAL_1_SOBRE_T, mapOf("T" to "T")),
        calcTool(engine, "calcular_periodo", FormulaId.T_IGUAL_1_SOBRE_F, mapOf("f" to "f")),
        calcTool(engine, "calcular_onda", FormulaId.V_IGUAL_LAMBDA_F, mapOf("lambda" to "lambda", "f" to "f")),
        calcTool(engine, "calcular_gravedad", FormulaId.G_PENDULO, mapOf("L" to "L", "T" to "T")),
        calcTool(engine, "calcular_magnetismo", FormulaId.B_INTENSIDAD, mapOf("Bx" to "Bx", "By" to "By", "Bz" to "Bz"))
    )

    private fun calcTool(
        engine: FisicaEngine,
        name: String,
        formulaId: FormulaId,
        argKeys: Map<String, String>
    ): McpToolHandler = tool(name, "Calcula ${formulaId.id}") { args, _ ->
        val entradas = argKeys.mapNotNull { (argKey, formulaKey) ->
            args.double(argKey)?.let { formulaKey to it }
        }.toMap()
        if (entradas.size != argKeys.size) return@tool error("Argumentos requeridos: ${argKeys.keys}")
        val result = engine.calcular(formulaId.id, entradas)
            ?: return@tool error("No se pudo calcular (datos inválidos)")
        ok("$result")
    }

    private fun estudioTools(service: EstudioContentService): List<McpToolHandler> = listOf(
        tool("crear_resumen", "Genera resumen de un PDF") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val tipo = args.string("tipo")?.let { runCatching { TipoResumen.valueOf(it) }.getOrNull() }
                ?: TipoResumen.CORTO
            ok(service.generarResumen(docId, tipo))
        },
        tool("crear_notas", "Genera notas de estudio") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            ok(service.generarNotas(docId))
        },
        tool("crear_preguntas", "Genera preguntas de repaso") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val dif = args.string("dificultad") ?: "media"
            val preguntas = service.generarPreguntas(docId, dif)
            ok(preguntas.joinToString("\n\n") { "P: ${it.pregunta}\nR: ${it.respuesta}" })
        },
        tool("crear_ejercicios", "Genera un ejercicio") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val ej = service.generarEjercicio(docId)
            ok("${ej.enunciado}\nDatos: ${ej.datos}\nSolución: ${ej.solucion}")
        },
        tool("crear_examen", "Genera examen de práctica") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val examen = service.crearExamen(docId, ExamenConfig())
            ok(examen.preguntas.joinToString("\n\n") { it.pregunta })
        },
        tool("crear_tarjetas", "Genera tarjetas de estudio") { args, _ ->
            val docId = args.long("documentoId") ?: return@tool error("Falta documentoId")
            val tarjetas = service.generarTarjetas(docId)
            ok(tarjetas.joinToString("\n\n") { "${it.frente} → ${it.reverso}" })
        }
    )

    private fun resultadosTools(
        resultadoRepository: ResultadoRepository,
        medicionRepository: MedicionRepository,
        iaService: IaService
    ): List<McpToolHandler> = listOf(
        tool("consultar_resultado", "Detalle de un resultado guardado") { args, _ ->
            val id = args.long("resultadoId") ?: return@tool error("Falta resultadoId")
            val r = resultadoRepository.getById(id) ?: return@tool error("Resultado no encontrado")
            val m = medicionRepository.getById(r.medicionId)
            ok(buildString {
                appendLine("Resultado #$id: ${r.valorCalculado} ${r.unidad}")
                appendLine("Notas: ${r.notas}")
                m?.let { appendLine("Medición: ${it.nombre} = ${it.valor} ${it.unidad}") }
            })
        },
        tool("comparar_resultados", "Compara dos resultados") { args, _ ->
            val a = args.long("resultadoIdA") ?: return@tool error("Falta resultadoIdA")
            val b = args.long("resultadoIdB") ?: return@tool error("Falta resultadoIdB")
            val ra = resultadoRepository.getById(a) ?: return@tool error("Resultado A no encontrado")
            val rb = resultadoRepository.getById(b) ?: return@tool error("Resultado B no encontrado")
            val diff = ra.valorCalculado - rb.valorCalculado
            ok(
                "A #$a: ${ra.valorCalculado} ${ra.unidad}\n" +
                    "B #$b: ${rb.valorCalculado} ${rb.unidad}\n" +
                    "Diferencia: $diff"
            )
        },
        tool("analizar_resultado", "Análisis IA de un resultado") { args, _ ->
            val id = args.long("resultadoId") ?: return@tool error("Falta resultadoId")
            ok(iaService.analizarResultado(id).texto)
        }
    )

    private fun tool(
        name: String,
        description: String,
        block: suspend (Map<String, Any?>, McpInvocationContext) -> McpToolResult
    ): McpToolHandler = object : McpToolHandler {
        override val info = McpToolInfo(name, description)
        override suspend fun invoke(args: Map<String, Any?>, context: McpInvocationContext) = block(args, context)
    }

    private fun ok(text: String) = McpToolResult(text)
    private fun error(msg: String) = McpToolResult(msg, isError = true)

    private fun Map<String, Any?>.string(key: String): String? = this[key]?.toString()
    private fun Map<String, Any?>.long(key: String): Long? = when (val v = this[key]) {
        is Number -> v.toLong()
        is String -> v.toLongOrNull()
        else -> null
    }
    private fun Map<String, Any?>.int(key: String): Int? = long(key)?.toInt()
    private fun Map<String, Any?>.double(key: String): Double? = when (val v = this[key]) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }
}
