package com.laboratoriofisico.app.usb

data class UvcMicroscopeState(
    val usbHostSupported: Boolean = false,
    val deviceConnected: Boolean = false,
    val permissionGranted: Boolean = false,
    val permissionDenied: Boolean = false,
    val previewBlocked: Boolean = false,
    val previewActive: Boolean = false,
    val capturing: Boolean = false,
    val statusMessage: String = "Microscopio desconectado",
    val previewPhase: String = "",
    val errorMessage: String? = null,
    val fallbackGalleryAvailable: Boolean = false,
    val debug: UvcUsbDebugInfo = UvcUsbDebugInfo()
)

data class UvcUsbDebugInfo(
    val lastGranted: Boolean? = null,
    val lastHasPermissionNow: Boolean? = null,
    val liveHasPermission: Boolean = false,
    val permissionStatus: String = "PENDIENTE",
    val dialogStatus: String = "Sin solicitud aún",
    val settingsHint: String? = null,
    val events: List<String> = emptyList()
)
