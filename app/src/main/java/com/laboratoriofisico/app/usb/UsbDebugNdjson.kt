package com.laboratoriofisico.app.usb

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object UsbDebugNdjson {
    private const val TAG = "UvcMicroscope"
    private const val SESSION_ID = "c0a240"
    private const val ENDPOINT = "http://127.0.0.1:7650/ingest/e867b72b-b027-4ddc-932e-a0e05ec73b1a"
    private var logFile: File? = null
    private var workspaceFile: File? = null

    fun init(context: Context) {
        logFile = File(context.applicationContext.filesDir, "debug-c0a240.log")
        workspaceFile = File("c:/LABORATORIO FISICO/debug-c0a240.log")
    }

    // #region agent log
    fun log(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?> = emptyMap(),
        runId: String = "pre-fix"
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
        Log.i(TAG, line)
        logFile?.appendText("$line\n")
        runCatching { workspaceFile?.takeIf { it.parentFile?.exists() == true }?.appendText("$line\n") }
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
