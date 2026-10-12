package com.laboratoriofisico.app.ia.gemini

object GeminiPrompts {
    const val MICROSCOPIO_PROMPT_VERSION = 2

    val TUTOR_SYSTEM = """
        Sos un tutor de física paciente y claro para estudiantes de bachillerato y primeros años universitarios.
        Respondé en español, con pasos ordenados cuando sea útil.
        Usá unidades del SI. Si falta información, decilo en vez de inventar datos.
        Hacé la respuesta fácil de ver: títulos con ##, viñetas con - y el resultado final en una línea aparte.
        No menciones que sos una IA ni hables de APIs o modelos.
    """.trimIndent()

    fun identificarObjeto(): String = """
        Mira esta foto de la cámara del teléfono. El objeto señalado está cerca del centro.
        Responde en español, en una sola frase, con este formato:
        El objeto es [qué es]. Marca: [marca que se lea en la foto, o "no se ve"]. Color: [color principal].
        No inventes la marca ni el modelo. Si no se lee, escribe "no se ve".
        No uses títulos, viñetas ni párrafos extra.
    """.trimIndent()

    fun explicarFormula(formula: String) = """
        Explicá la fórmula de física: $formula
        Incluí: qué significa cada variable, unidades SI, cuándo se aplica y un ejemplo numérico breve.
    """.trimIndent()

    fun explicarConcepto(concepto: String) = """
        Explicá el concepto de física: $concepto
        Usá lenguaje claro, analogías simples y un ejemplo del laboratorio escolar si aplica.
    """.trimIndent()

    fun resolverEjercicio(enunciado: String, datos: String) = """
        Resolvé este ejercicio de física paso a paso.
        Enunciado: $enunciado
        Datos: $datos
        Mostrá datos, fórmula, sustitución y resultado con unidades.
    """.trimIndent()

    fun resolverEjercicioDesdeFoto(): String = """
        Lee el ejercicio de la imagen y resuélvelo.
        Responde SOLO con JSON válido (sin markdown ni texto fuera del JSON), con esta estructura exacta:
        {
          "materia":"fisica|matematica|quimica|biologia",
          "tema":"texto corto",
          "enunciado":"texto detectado del ejercicio",
          "datos":[{"nombre":"...","valor":0,"unidad":"..."}],
          "incognita":"qué se pide",
          "formula":"fórmula compacta (ej. v=d/t)",
          "formula_legible":"misma fórmula con símbolos Unicode legibles (× ÷ ² √ π Δ), sin LaTeX",
          "variables":[{"simbolo":"v","nombre":"velocidad","unidad":"m/s"}],
          "explicacion_formula":"una frase simple que diga qué significa la fórmula",
          "pasos":[{"numero":1,"titulo":"...","explicacion":"máximo 2 oraciones cortas en español simple, con un porqué","operacion":"..."}],
          "resultado":{"valor":0,"unidad":"..."},
          "confianza":"baja|media|alta"
        }
        Reglas:
        - Explicaciones en español simple, sin jerga ni palabras rebuscadas.
        - Cada paso: máximo 2 oraciones y un porqué (ej. "Convertimos a m/s porque la fórmula pide esa unidad").
        - No uses markdown (** ##), ni LaTeX (\frac \text $), ni backticks.
        - No inventes datos que no se vean.
        - Si la foto está borrosa o ilegible, responde JSON con enunciado "Foto borrosa o ilegible" y confianza "baja".
        - En biología y química: nada de consejos médicos ni de seguridad química peligrosa.
        - No menciones que eres una IA.
    """.trimIndent()

    fun resolverEjercicioDesdeTexto(enunciado: String): String = """
        Resuelve este ejercicio. El enunciado ya fue corregido por el estudiante.
        Enunciado: $enunciado
        Responde SOLO con JSON válido (sin markdown), misma estructura:
        {
          "materia":"fisica|matematica|quimica|biologia",
          "tema":"texto corto",
          "enunciado":"copia o mejora leve del enunciado",
          "datos":[{"nombre":"...","valor":0,"unidad":"..."}],
          "incognita":"qué se pide",
          "formula":"fórmula compacta",
          "formula_legible":"fórmula con símbolos Unicode (× ÷ ² √ π Δ), sin LaTeX",
          "variables":[{"simbolo":"v","nombre":"velocidad","unidad":"m/s"}],
          "explicacion_formula":"una frase simple",
          "pasos":[{"numero":1,"titulo":"...","explicacion":"máximo 2 oraciones cortas con un porqué","operacion":"..."}],
          "resultado":{"valor":0,"unidad":"..."},
          "confianza":"baja|media|alta"
        }
        Reglas: español simple; sin markdown/LaTeX; no inventes datos; en biología/química sin consejos médicos ni peligrosos.
    """.trimIndent()

    fun analizarResultado(
        resultadoId: Long,
        valor: Double,
        unidad: String,
        notas: String,
        medicionNombre: String?,
        medicionValor: Double?,
        medicionUnidad: String?
    ): String {
        val medicion = if (medicionNombre != null && medicionValor != null) {
            "Medición base: $medicionNombre = $medicionValor $medicionUnidad"
        } else {
            "Medición base: no disponible"
        }
        return """
            Analizá este resultado real de un experimento del laboratorio (id=$resultadoId).
            Resultado calculado: $valor $unidad
            $medicion
            Notas/metadatos: ${notas.ifBlank { "sin notas" }}
            Evaluá si el valor es razonable, posibles errores de medición y cómo mejorar el experimento.
            Usá solo estos números. No inventes otras lecturas.
            Formato:
            ## Qué significa este número
            una frase
            ## Si es razonable
            sí o no, y por qué
            ## Cómo mejorarlo
            - un paso concreto
        """.trimIndent()
    }

    fun preguntarSobrePdf(contexto: String, pregunta: String, referencias: String): String = """
        Respondé la pregunta del estudiante usando SOLO el contenido del documento provisto.
        Si la respuesta no está en el documento, decilo claramente.
        Citá la página o sección cuando sea posible (ej. "Según la sección X, pág. N…").
        Secciones disponibles: $referencias

        --- CONTENIDO DEL DOCUMENTO ---
        $contexto
        --- FIN DEL DOCUMENTO ---

        Pregunta del estudiante: $pregunta
    """.trimIndent()

    fun resumen(contexto: String, tipo: String): String = """
        Generá un resumen $tipo del siguiente material.
        Usá solo información presente en el texto. No inventes temas, números ni fórmulas que no aparezcan.
        Respondé en español con este formato, para que se lea de un vistazo:
        ## Idea principal
        un párrafo corto
        ## Puntos clave
        - viñetas concretas
        ## Datos que sí están en el texto
        - números, unidades o fórmulas copiados del material; si no hay, escribí "El texto no trae datos numéricos."

        --- CONTENIDO ---
        $contexto
    """.trimIndent()

    private val REGLA_MICROSCOPIO_NO_INVENTAR = """
        Basa tu respuesta únicamente en lo que es visualmente identificable en esta imagen.
        Si no puedes determinar algo con la información disponible, dilo claramente en vez de asumir o inventar.
        No inventes especies, tipos celulares, bacterias, diagnósticos, porcentajes ni resultados de laboratorio.
        Responde en español, con observaciones concretas y lenguaje educativo.
        Tono: científico y didáctico, como un profesor de biología explicando la muestra a un estudiante.
        Prefiere una explicación completa a una respuesta corta o genérica.
    """.trimIndent()

    fun analizarYogurMicroscopio(resumenAnterior: String?): String = """
        Analiza una fotografía real de una muestra de yogur observada con microscopio USB acoplado a la cámara del teléfono.

        $REGLA_MICROSCOPIO_NO_INVENTAR

        Contexto de la muestra: yogur recién preparado con azul de metileno. Puede haber una captura previa de la misma línea de tiempo.
        Captura previa: ${resumenAnterior?.takeIf { it.isNotBlank() } ?: "No hay captura previa registrada."}

        Tareas:
        - Identifica TODO lo visualmente reconocible: partículas, agregados, puntos, filamentos, manchas, burbujas, zonas teñidas, patrones, textura y distribución.
        - Explica biológicamente qué podría representar cada elemento visible y por qué puede verse así al microscopio.
        - Contexto educativo: explica qué son bacterias lácticas como Lactobacillus, cuál es su papel en el yogur y por qué un colorante como azul de metileno puede resaltar material biológico o zonas con diferente afinidad por el tinte.
        - Indica con claridad si la resolución permite o no distinguir bacterias individuales; si solo se ven agregados o manchas, dilo.
        - Si hay captura previa, compara solo cambios visuales identificables entre esta imagen y el resumen anterior.
        - No afirmes especie exacta, viabilidad, contaminación o concentración si no es visible con claridad.
        - Organiza la respuesta con estos títulos markdown, en este orden: ## Observaciones visuales, ## Interpretación biológica, ## Límites de la imagen, ## Conclusión educativa.
    """.trimIndent()

    fun analizarPielMicroscopio(): String = """
        Analiza una fotografía real de una muestra de piel o escamas observada con microscopio USB acoplado a la cámara del teléfono.

        $REGLA_MICROSCOPIO_NO_INVENTAR

        Tareas:
        - Identifica TODO lo visualmente reconocible: textura, tono, bordes, láminas, escamas, fibras, pliegues, vello/pelo, partículas adheridas, grietas, patrones y zonas translúcidas u opacas.
        - Explica biológicamente qué podría representar cada estructura visible: células superficiales queratinizadas, fragmentos de estrato córneo, fibras externas, grasa/sebo o artefactos, solo si la imagen lo permite.
        - Da contexto educativo sobre la piel como tejido epitelial de protección y sobre por qué las escamas suelen verse como láminas o fragmentos irregulares.
        - Aclara si no se puede determinar con certeza si la muestra es piel humana, animal u otro material.
        - No diagnostiques enfermedades ni hagas inferencias médicas.
        - Organiza la respuesta con estos títulos markdown: ## Observaciones visuales, ## Interpretación biológica, ## Límites de la imagen, ## Conclusión educativa.
    """.trimIndent()

    fun analizarHojaMicroscopio(): String = """
        Analiza una fotografía real de una hoja observada con microscopio USB acoplado a la cámara del teléfono.

        $REGLA_MICROSCOPIO_NO_INVENTAR

        Tareas:
        - Identifica TODO lo visualmente reconocible: patrón celular, paredes celulares, cloroplastos, estomas, nervaduras, tricomas, zonas verdes, espacios, texturas y variaciones de color.
        - Explica biológicamente cada estructura visible: pared celular como límite rígido, cloroplastos como orgánulos fotosintéticos, estomas como poros de intercambio gaseoso, nervaduras como tejido conductor, si realmente se aprecian.
        - Da contexto educativo sobre cómo las células vegetales se organizan en la hoja y por qué la clorofila genera tonos verdes.
        - Estima la humedad solo como apariencia visual educativa: seca, intermedia o húmeda, explicando qué rasgos visibles lo sugieren.
        - Aclara si no se puede identificar la especie de la planta con certeza.
        - No inventes especie, estado fisiológico ni condiciones de cultivo.
        - Organiza la respuesta con estos títulos markdown: ## Observaciones visuales, ## Interpretación biológica, ## Límites de la imagen, ## Conclusión educativa.
    """.trimIndent()

    fun analizarSangreMicroscopio(solicitaHipotesisEducativa: Boolean): String = """
        Analiza una fotografía real de un frotis de sangre observado con microscopio USB acoplado a la cámara del teléfono.

        $REGLA_MICROSCOPIO_NO_INVENTAR

        Aclaración obligatoria: esto NO es un diagnóstico médico. El tipo de sangre ABO/Rh requiere reactivos químicos y no puede determinarse de forma fiable solo por imagen.

        Tareas:
        - Identifica TODO lo visualmente reconocible: color, distribución, forma general de células o partículas, agrupaciones, zonas superpuestas, artefactos, burbujas, fibras, grumos o variaciones de enfoque.
        - Explica de forma educativa qué podrían ser eritrocitos, leucocitos, plaquetas o artefactos de preparación si son visualmente distinguibles; si no lo son, dilo explícitamente.
        - Da contexto biológico sobre la función general de los glóbulos rojos y por qué en un frotis pueden verse agrupados, superpuestos o deformados por la preparación.
        - No diagnostiques enfermedades, anemia, infecciones, coagulación anormal ni confirmes tipo de sangre.
        ${if (solicitaHipotesisEducativa) "- Si decides incluir una hipótesis educativa especulativa, debe estar marcada claramente como especulativa, no diagnóstica, y explicar que no sustituye pruebas ABO/Rh con reactivos." else "- No incluyas hipótesis de tipo de sangre porque el usuario no la solicitó."}
        - Organiza la respuesta con estos títulos markdown: ## Aclaración médica obligatoria, ## Observaciones visuales, ## Interpretación biológica, ## Límites de la imagen, ## Conclusión educativa.
    """.trimIndent()

    fun notas(contexto: String): String = """
        Generá notas de estudio concisas a partir del material de física provisto.
        Incluí fórmulas, definiciones y puntos clave que aparezcan en el texto.
        Formato visible:
        ## Definiciones
        - una idea por viñeta
        ## Fórmulas que aparecen en el texto
        - solo las que están escritas en el material
        No inventes datos. Si falta un número, no lo completes.

        --- CONTENIDO ---
        $contexto
    """.trimIndent()

    fun preguntasEstudio(contexto: String, dificultad: String, cantidad: Int): String = """
        Generá exactamente $cantidad preguntas de estudio de dificultad $dificultad sobre el material provisto.
        Respondé SOLO con JSON válido (sin markdown), con esta estructura:
        {"preguntas":[{"pregunta":"...","respuesta":"...","explicacion":"..."}]}
        Las preguntas deben basarse únicamente en el contenido del documento.

        --- CONTENIDO ---
        $contexto
    """.trimIndent()

    fun ejercicioEstudio(contexto: String): String = """
        Generá UN ejercicio de física basado en el material del documento.
        Respondé SOLO con JSON válido (sin markdown):
        {"enunciado":"...","datos":"...","formula":"...","solucion":"valor numérico o expresión corta","explicacion":"...","dificultad":"facil|media|dificil"}
        La solución debe ser verificable (número o texto corto).

        --- CONTENIDO ---
        $contexto
    """.trimIndent()

    fun tarjetasEstudio(contexto: String, cantidad: Int): String = """
        Generá exactamente $cantidad tarjetas de estudio (frente/reverso) del material provisto.
        Respondé SOLO con JSON válido (sin markdown):
        {"tarjetas":[{"frente":"...","reverso":"...","dificultad":"facil|media|dificil"}]}

        --- CONTENIDO ---
        $contexto
    """.trimIndent()

    fun examenEstudio(contexto: String, cantidad: Int): String = """
        Generá un examen de opción múltiple con exactamente $cantidad preguntas sobre el material provisto.
        Cada pregunta debe tener 4 opciones. respuestaCorrecta es el índice 0-3 de la opción correcta.
        Respondé SOLO con JSON válido (sin markdown):
        {"preguntas":[{"pregunta":"...","opciones":["A","B","C","D"],"respuestaCorrecta":0}]}

        --- CONTENIDO ---
        $contexto
    """.trimIndent()
}
