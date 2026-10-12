package com.laboratoriofisico.app.reportes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportFileHelper {
    private const val TAG = "LaboratorioExport"
    const val AUTHORITY_SUFFIX = ".fileprovider"

    fun authority(context: Context): String = "${context.packageName}$AUTHORITY_SUFFIX"

    fun exportsDir(context: Context): File =
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "exportaciones"
        ).apply { mkdirs() }

    fun createExportFile(context: Context, baseName: String, extension: String): File {
        val safeBase = baseName
            .lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9_-]+"), "_")
            .trim('_')
            .ifBlank { "reporte" }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(exportsDir(context), "${safeBase}_$timestamp.$extension")
    }

    fun contentUri(context: Context, file: File): Uri {
        require(file.exists()) { "El archivo exportado no existe: ${file.absolutePath}" }
        Log.i(TAG, "FileProvider uri for ${file.absolutePath} authority=${authority(context)}")
        return FileProvider.getUriForFile(context, authority(context), file)
    }

    fun shareIntent(context: Context, uri: Uri, mimeType: String, title: String): Intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newRawUri(title, uri)
        }
}
