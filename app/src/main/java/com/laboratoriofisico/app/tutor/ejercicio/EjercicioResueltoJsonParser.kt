package com.laboratoriofisico.app.tutor.ejercicio

import com.laboratoriofisico.app.backend.model.ConfianzaEjercicio
import com.laboratoriofisico.app.backend.model.EjercicioDato
import com.laboratoriofisico.app.backend.model.EjercicioPaso
import com.laboratoriofisico.app.backend.model.EjercicioResultadoValor
import com.laboratoriofisico.app.backend.model.EjercicioVariable
import com.laboratoriofisico.app.backend.model.MateriaEjercicio
import com.laboratoriofisico.app.ia.gemini.GeminiResponseParser

/**
 * Resultado parseado de Gemini (antes de persistir / verificar).
 */
data class EjercicioGeminiParsed(
    val materia: MateriaEjercicio,
    val tema: String,
    val enunciado: String,
    val datos: List<EjercicioDato>,
    val incognita: String,
    val formula: String,
    val formulaLegible: String,
    val variables: List<EjercicioVariable>,
    val explicacionFormula: String,
    val pasos: List<EjercicioPaso>,
    val resultado: EjercicioResultadoValor,
    val confianza: ConfianzaEjercicio
)

/**
 * Parser JVM-friendly (sin org.json) del JSON estricto de Gemini.
 * Una sola reparación local si el JSON viene envuelto o con comas finales.
 */
object EjercicioResueltoJsonParser {

    fun parse(raw: String): Result<EjercicioGeminiParsed> = runCatching {
        val cleaned = GeminiResponseParser.extraerJson(raw)
        parseStrict(cleaned).getOrElse {
            parseStrict(repararUnaVez(cleaned)).getOrThrow()
        }
    }

    fun repararUnaVez(raw: String): String {
        var s = GeminiResponseParser.extraerJson(raw)
        s = s.trim()
        // Quitar comas finales antes de } o ]
        s = s.replace(Regex(",\\s*([}\\]])"), "$1")
        // Si viene texto alrededor, intentar recortar al primer { … último }
        val start = s.indexOf('{')
        val end = s.lastIndexOf('}')
        if (start >= 0 && end > start) {
            s = s.substring(start, end + 1)
        }
        return s
    }

    private fun parseStrict(json: String): Result<EjercicioGeminiParsed> = runCatching {
        val root = SimpleJson.parseObject(json)
            ?: error("JSON inválido")
        val materia = MateriaEjercicio.from(root.string("materia"))
        val tema = root.string("tema").orEmpty().trim()
        val enunciado = root.string("enunciado").orEmpty().trim()
        require(enunciado.isNotBlank()) { "Falta el enunciado" }

        val datos = root.array("datos").mapNotNull { item ->
            val obj = item as? JsonObj ?: return@mapNotNull null
            val nombre = obj.string("nombre").orEmpty().trim()
            val valor = obj.number("valor") ?: return@mapNotNull null
            val unidad = obj.string("unidad").orEmpty().trim()
            if (nombre.isBlank()) null else EjercicioDato(nombre, valor, unidad)
        }

        val incognita = root.string("incognita").orEmpty().trim()
        val formula = root.string("formula").orEmpty().trim()
        val formulaLegible = root.string("formula_legible").orEmpty().trim()
            .ifBlank { TutorTextCleaner.formulaLegibleDesdeCruda(formula) }
        val variables = root.array("variables").mapNotNull { item ->
            val obj = item as? JsonObj ?: return@mapNotNull null
            val simbolo = obj.string("simbolo").orEmpty().trim()
            if (simbolo.isBlank()) null
            else EjercicioVariable(
                simbolo = simbolo,
                nombre = obj.string("nombre").orEmpty().trim(),
                unidad = obj.string("unidad").orEmpty().trim()
            )
        }
        val explicacionFormula = root.string("explicacion_formula").orEmpty().trim()

        val pasos = root.array("pasos").mapIndexedNotNull { index, item ->
            val obj = item as? JsonObj ?: return@mapIndexedNotNull null
            EjercicioPaso(
                numero = obj.number("numero")?.toInt() ?: (index + 1),
                titulo = obj.string("titulo").orEmpty().trim(),
                explicacion = obj.string("explicacion").orEmpty().trim(),
                operacion = obj.string("operacion").orEmpty().trim()
            ).takeIf { it.titulo.isNotBlank() || it.explicacion.isNotBlank() }
        }
        require(pasos.isNotEmpty()) { "Faltan los pasos" }

        val resultadoObj = root.obj("resultado") ?: error("Falta el resultado")
        val valor = resultadoObj.number("valor") ?: error("Falta el valor del resultado")
        val unidad = resultadoObj.string("unidad").orEmpty().trim()
        val confianza = ConfianzaEjercicio.from(root.string("confianza"))

        EjercicioGeminiParsed(
            materia = materia,
            tema = tema.ifBlank { "General" },
            enunciado = enunciado,
            datos = datos,
            incognita = incognita.ifBlank { "resultado" },
            formula = formula,
            formulaLegible = formulaLegible,
            variables = variables,
            explicacionFormula = explicacionFormula,
            pasos = pasos,
            resultado = EjercicioResultadoValor(valor = valor, unidad = unidad),
            confianza = confianza
        )
    }
}

object EjercicioResueltoJsonCodec {
    fun encodeDatos(datos: List<EjercicioDato>): String =
        datos.joinToString(prefix = "[", postfix = "]") { d ->
            """{"nombre":${jsonString(d.nombre)},"valor":${d.valor},"unidad":${jsonString(d.unidad)}}"""
        }

    fun encodePasos(pasos: List<EjercicioPaso>): String =
        pasos.joinToString(prefix = "[", postfix = "]") { p ->
            """{"numero":${p.numero},"titulo":${jsonString(p.titulo)},"explicacion":${jsonString(p.explicacion)},"operacion":${jsonString(p.operacion)}}"""
        }

    fun encodeVariables(variables: List<EjercicioVariable>): String =
        variables.joinToString(prefix = "[", postfix = "]") { v ->
            """{"simbolo":${jsonString(v.simbolo)},"nombre":${jsonString(v.nombre)},"unidad":${jsonString(v.unidad)}}"""
        }

    fun decodeDatos(json: String): List<EjercicioDato> {
        if (json.isBlank()) return emptyList()
        val arr = SimpleJson.parseArray(json) ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item as? JsonObj ?: return@mapNotNull null
            val nombre = obj.string("nombre").orEmpty().trim()
            val valor = obj.number("valor") ?: return@mapNotNull null
            val unidad = obj.string("unidad").orEmpty().trim()
            if (nombre.isBlank()) null else EjercicioDato(nombre, valor, unidad)
        }
    }

    fun decodePasos(json: String): List<EjercicioPaso> {
        if (json.isBlank()) return emptyList()
        val arr = SimpleJson.parseArray(json) ?: return emptyList()
        return arr.mapIndexedNotNull { index, item ->
            val obj = item as? JsonObj ?: return@mapIndexedNotNull null
            EjercicioPaso(
                numero = obj.number("numero")?.toInt() ?: (index + 1),
                titulo = obj.string("titulo").orEmpty().trim(),
                explicacion = obj.string("explicacion").orEmpty().trim(),
                operacion = obj.string("operacion").orEmpty().trim()
            ).takeIf { it.titulo.isNotBlank() || it.explicacion.isNotBlank() }
        }
    }

    fun decodeVariables(json: String): List<EjercicioVariable> {
        if (json.isBlank()) return emptyList()
        val arr = SimpleJson.parseArray(json) ?: return emptyList()
        return arr.mapNotNull { item ->
            val obj = item as? JsonObj ?: return@mapNotNull null
            val simbolo = obj.string("simbolo").orEmpty().trim()
            if (simbolo.isBlank()) null
            else EjercicioVariable(
                simbolo = simbolo,
                nombre = obj.string("nombre").orEmpty().trim(),
                unidad = obj.string("unidad").orEmpty().trim()
            )
        }
    }

    private fun jsonString(value: String): String =
        "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}
/** Mini JSON para tests JVM y parsing estructurado. */
internal class JsonObj(private val map: Map<String, Any?>) {
    fun string(key: String): String? = map[key]?.toString()
    fun number(key: String): Double? = when (val v = map[key]) {
        is Number -> v.toDouble()
        is String -> v.toDoubleOrNull()
        else -> null
    }
    fun obj(key: String): JsonObj? = map[key] as? JsonObj
    fun array(key: String): List<Any?> = (map[key] as? List<*>)?.map { it } ?: emptyList()
}

internal object SimpleJson {
    fun parseObject(text: String): JsonObj? {
        val (value, _) = Parser(text).parseValue() ?: return null
        return value as? JsonObj
    }

    fun parseArray(text: String): List<Any?>? {
        val (value, _) = Parser(text).parseValue() ?: return null
        @Suppress("UNCHECKED_CAST")
        return value as? List<Any?>
    }

    private class Parser(private val s: String) {
        private var i = 0

        fun parseValue(): Pair<Any?, Unit>? {
            skipWs()
            if (i >= s.length) return null
            return when (val c = s[i]) {
                '{' -> parseObject() to Unit
                '[' -> parseArray() to Unit
                '"' -> parseString() to Unit
                't' -> { expect("true"); true to Unit }
                'f' -> { expect("false"); false to Unit }
                'n' -> { expect("null"); null to Unit }
                else -> if (c == '-' || c.isDigit()) parseNumber() to Unit else null
            }
        }

        private fun parseObject(): JsonObj {
            expect('{')
            val map = linkedMapOf<String, Any?>()
            skipWs()
            if (peek('}')) {
                expect('}')
                return JsonObj(map)
            }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                val (value, _) = parseValue() ?: error("valor")
                map[key] = value
                skipWs()
                if (peek('}')) {
                    expect('}')
                    break
                }
                expect(',')
            }
            return JsonObj(map)
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWs()
            if (peek(']')) {
                expect(']')
                return list
            }
            while (true) {
                skipWs()
                val (value, _) = parseValue() ?: error("valor")
                list.add(value)
                skipWs()
                if (peek(']')) {
                    expect(']')
                    break
                }
                expect(',')
            }
            return list
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (i < s.length) {
                when (val c = s[i++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (i >= s.length) break
                        when (val e = s[i++]) {
                            '"', '\\', '/' -> sb.append(e)
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000c')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                val hex = s.substring(i, (i + 4).coerceAtMost(s.length))
                                i += hex.length
                                sb.append(hex.toIntOrNull(16)?.toChar() ?: '?')
                            }
                            else -> sb.append(e)
                        }
                    }
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseNumber(): Number {
            val start = i
            if (peek('-')) i++
            while (i < s.length && s[i].isDigit()) i++
            if (peek('.')) {
                i++
                while (i < s.length && s[i].isDigit()) i++
            }
            if (peek('e') || peek('E')) {
                i++
                if (peek('+') || peek('-')) i++
                while (i < s.length && s[i].isDigit()) i++
            }
            val raw = s.substring(start, i)
            return raw.toLongOrNull() ?: raw.toDouble()
        }

        private fun skipWs() {
            while (i < s.length && s[i].isWhitespace()) i++
        }

        private fun peek(c: Char): Boolean = i < s.length && s[i] == c

        private fun expect(c: Char) {
            skipWs()
            require(i < s.length && s[i] == c) { "Se esperaba '$c'" }
            i++
        }

        private fun expect(text: String) {
            skipWs()
            require(s.startsWith(text, i)) { "Se esperaba $text" }
            i += text.length
        }
    }
}
