package com.laboratoriofisico.app.frontend.ui.components

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import com.laboratoriofisico.app.usb.UvcMicroscopeFragment
import com.laboratoriofisico.app.usb.UvcMicroscopeManager

@Composable
fun UvcMicroscopePreview(
    manager: UvcMicroscopeManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val containerId = remember { View.generateViewId() }
    val tag = remember { "uvc_microscope_$containerId" }
    val fragmentAgregado = remember { booleanArrayOf(false) }

    AndroidView(
        factory = { ctx ->
            FragmentContainerView(ctx).apply {
                id = containerId
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        modifier = modifier,
        update = { view ->
            if (fragmentAgregado[0]) return@AndroidView
            val fragmentManager = activity?.supportFragmentManager ?: return@AndroidView
            if (fragmentManager.findFragmentByTag(tag) != null ||
                fragmentManager.fragments.any { it is UvcMicroscopeFragment }
            ) {
                fragmentAgregado[0] = true
                return@AndroidView
            }
            fragmentAgregado[0] = true
            view.post {
                if (!view.isAttachedToWindow) {
                    fragmentAgregado[0] = false
                    return@post
                }
                if (fragmentManager.findFragmentByTag(tag) != null ||
                    fragmentManager.fragments.any { it is UvcMicroscopeFragment }
                ) {
                    return@post
                }
                fragmentManager.beginTransaction()
                    .replace(view.id, UvcMicroscopeFragment(manager), tag)
                    .commitAllowingStateLoss()
            }
        }
    )

    DisposableEffect(activity, tag) {
        onDispose {
            val fragmentManager = activity?.supportFragmentManager ?: return@onDispose
            val fragment = fragmentManager.findFragmentByTag(tag) ?: return@onDispose
            fragmentManager.beginTransaction()
                .remove(fragment)
                .commitAllowingStateLoss()
        }
    }
}
