package com.laboratoriofisico.app.usb

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object UsbDebugNdjson {
    private const val TAG = "CameraDebug"
    private const val SESSION_ID = "3c0f43"
    private const val ENDPOINT = "http://127.0.0.1:7650/ingest/e867b72b-b027-4ddc-932e-a0e05ec73b1a"
    private var logFile: File? = null
    private var workspaceFile: File? = null

    fun init(context: Context) {
        val app = context.applicationContext
        logFile = File(app.filesDir, "debug-3c0f43.log")
        // Copia también a almacenamiento de la app para poder hacer adb pull.
        runCatching {
            val external = app.getExternalFilesDir(null)
            if (external != null) {
                workspaceFile = File(external, "debug-3c0f43.log")
            }
        }
        if (workspaceFile == null) {
            workspaceFile = File("c:/LABORATORIO FISICO/debug-3c0f43.log")
        }
        Log.i(TAG, "init logFile=${logFile?.absolutePath} workspaceFile=${workspaceFile?.absolutePath}")
        runCatching { logFile?.appendText("") }
        runCatching { workspaceFile?.parentFile?.mkdirs(); workspaceFile?.appendText("") }
    }

    // #region agent log
    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "post-fix"
    ) {
        val payload = JSONObject().apply {
            put("sessionId", SESSION_ID)
            put("hypothesisId", hypothesisId)
            put("location", location)
            put("message", message)
            put("timestamp", System.currentTimeMillis())
            put("runId", runId)
            put("data", JSONObject(data))
        }
        val line = payload.toString()
        if (hypothesisId == "H4" || message.contains("error", ignoreCase = true) || message.contains("exception", ignoreCase = true)) {
            Log.e(TAG, "$location $message $data")
        } else {
            Log.i(TAG, "$location $message $data")
        }
        runCatching { logFile?.appendText("$line\n") }
        runCatching {
            workspaceFile?.parentFile?.mkdirs()
            workspaceFile?.appendText("$line\n")
        }
        thread(name = "usb-debug-ndjson") {
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
    // #endregion
}
