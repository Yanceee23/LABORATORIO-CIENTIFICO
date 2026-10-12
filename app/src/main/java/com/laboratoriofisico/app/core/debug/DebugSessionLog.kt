package com.laboratoriofisico.app.core.debug

import android.content.Context
import android.util.Log
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Instrumentación de sesión debug — Logcat + archivo en dispositivo + ingest HTTP. */
object DebugSessionLog {
    private const val TAG = "DEBUG_ffc1ab"
    private const val SESSION_ID = "ffc1ab"
    private const val ENDPOINT = "http://127.0.0.1:7650/ingest/e867b72b-b027-4ddc-932e-a0e05ec73b1a"
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.applicationContext.filesDir, "debug-ffc1ab.log")
    }

    // #region agent log
    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix"
    ) {
        runCatching {
            val dataJson = data.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
                "\"$k\":${jsonValue(v)}"
            }
            val line = buildString {
                append("{")
                append("\"sessionId\":\"$SESSION_ID\",")
                append("\"hypothesisId\":\"$hypothesisId\",")
                append("\"location\":${jsonValue(location)},")
                append("\"message\":${jsonValue(message)},")
                append("\"timestamp\":${System.currentTimeMillis()},")
                append("\"runId\":${jsonValue(runId)},")
                append("\"data\":$dataJson")
                append("}")
            }

            runCatching { Log.i(TAG, line) }
            runCatching { logFile?.appendText("$line\n") }

            thread(name = "debug-session-log") {
                runCatching {
                    val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("X-Debug-Session-Id", SESSION_ID)
                        doOutput = true
                        connectTimeout = 2_000
                        readTimeout = 2_000
                    }
                    conn.outputStream.use { it.write(line.toByteArray()) }
                    conn.responseCode
                    conn.disconnect()
                }
            }
        }
    }

    private fun jsonValue(value: Any?): String = when (value) {
        null -> "null"
        is Number, is Boolean -> value.toString()
        else -> "\"${value.toString().replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }
    // #endregion
}
