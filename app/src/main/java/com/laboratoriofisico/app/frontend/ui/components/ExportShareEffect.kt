package com.laboratoriofisico.app.frontend.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.laboratoriofisico.app.reportes.ExportShareHelper

@Composable
fun ExportShareLaunchedEffect(
    shareUri: Uri?,
    shareFileName: String?,
    mimeType: String,
    chooserTitleRes: Int,
    onShareHandled: () -> Unit,
    onShareError: (String) -> Unit
) {
    val context = LocalContext.current
    LaunchedEffect(shareUri) {
        val uri = shareUri ?: return@LaunchedEffect
        val chooserTitle = context.getString(chooserTitleRes)
        val error = ExportShareHelper.launchShare(
            context = context,
            uri = uri,
            mimeType = mimeType,
            chooserTitle = chooserTitle
        )
        if (error != null) {
            onShareError(error)
        }
        onShareHandled()
    }
}
