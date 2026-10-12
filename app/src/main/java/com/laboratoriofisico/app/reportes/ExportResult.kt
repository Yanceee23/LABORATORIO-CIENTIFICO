package com.laboratoriofisico.app.reportes

import android.net.Uri

sealed class ExportResult {
    data class Success(val uri: Uri, val fileName: String) : ExportResult()
    data class Error(val message: String) : ExportResult()
}
