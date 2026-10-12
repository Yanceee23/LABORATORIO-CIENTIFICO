package com.laboratoriofisico.app.reportes

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.util.Log

object ExportShareHelper {
    private const val TAG = "LaboratorioExport"

    /**
     * Abre el selector de Android para compartir [uri].
     * @return null si se lanzó bien; mensaje de error si falló.
     */
    fun launchShare(
        context: Context,
        uri: Uri,
        mimeType: String,
        chooserTitle: String
    ): String? {
        val activity = context.findActivity()
            ?: return "No se pudo abrir el selector para compartir (contexto inválido)."

        return try {
            val sendIntent = ExportFileHelper.shareIntent(context, uri, mimeType, chooserTitle)
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Log.i(
                TAG,
                "Launching share chooser mime=$mimeType uri=$uri authority=${uri.authority}"
            )
            activity.startActivity(chooser)
            null
        } catch (_: ActivityNotFoundException) {
            Log.e(TAG, "No activity found to handle share intent uri=$uri")
            "No hay apps instaladas para compartir este archivo."
        } catch (error: Exception) {
            Log.e(TAG, "Share failed uri=$uri", error)
            "No se pudo compartir: ${error.message ?: "error desconocido"}"
        }
    }

    private tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
