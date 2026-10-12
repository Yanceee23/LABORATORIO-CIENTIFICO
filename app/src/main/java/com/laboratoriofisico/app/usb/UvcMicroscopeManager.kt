package com.laboratoriofisico.app.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UvcMicroscopeManager(
    context: Context
) {
    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager
    private val usbHostSupported = appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _state = MutableStateFlow(
        UvcMicroscopeState(
            usbHostSupported = usbHostSupported,
            fallbackGalleryAvailable = !usbHostSupported,
            statusMessage = if (usbHostSupported) "Microscopio desconectado" else "Este dispositivo no soporta USB Host."
        )
    )
    val state: StateFlow<UvcMicroscopeState> = _state.asStateFlow()

    private var receiverRegistered = false
    private var fragment: UvcMicroscopeFragment? = null
    private var explicitPermissionDenied = false
    private var lastConnectedDeviceId: Int? = null
    private var lastGranted: Boolean? = null
    private var lastHasPermissionNow: Boolean? = null
    private var lastRequestAtMs: Long? = null
    private var lastCallbackAtMs: Long? = null
    private var permissionRequestCount = 0
    private var permissionCallbackCount = 0
    private var awaitingCallback = false
    private var requestGeneration = 0
    private var dialogLikelyShown: Boolean? = null
    private var dialogLikelySuppressed = false
    private val eventLog = ArrayDeque<String>()

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    Log.d(TAG, "USB device attached")
                    explicitPermissionDenied = false
                    appendEvent("USB conectado")
                    resetPreviewBlock()
                    debugLog(
                        hypothesisId = "H5",
                        location = "UvcMicroscopeManager.usbReceiver.ATTACHED",
                        message = "USB device attached",
                        data = mapOf("deviceId" to intent.getUsbDeviceExtra()?.deviceId)
                    )
                    refreshUsbDeviceState()
                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    Log.d(TAG, "USB device detached")
                    explicitPermissionDenied = false
                    awaitingCallback = false
                    appendEvent("Microscopio desconectado")
                    resetPreviewBlock()
                    debugLog(
                        hypothesisId = "H5",
                        location = "UvcMicroscopeManager.usbReceiver.DETACHED",
                        message = "USB device detached",
                        data = mapOf("previousDeviceId" to lastConnectedDeviceId)
                    )
                    refreshUsbDeviceState()
                }
                ACTION_USB_PERMISSION -> {
                    val device = intent.getUsbDeviceExtra()
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    val hasPermissionNow = device?.let { usbManager.hasPermission(it) } == true
                    val effectiveGranted = granted || hasPermissionNow
                    val elapsedMs = lastRequestAtMs?.let { System.currentTimeMillis() - it }
                    lastGranted = granted
                    lastHasPermissionNow = hasPermissionNow
                    lastCallbackAtMs = System.currentTimeMillis()
                    permissionCallbackCount += 1
                    awaitingCallback = false
                    dialogLikelyShown = elapsedMs != null && elapsedMs >= DIALOG_SHOWN_THRESHOLD_MS
                    dialogLikelySuppressed = elapsedMs != null && elapsedMs < DIALOG_SUPPRESSED_THRESHOLD_MS && !granted
                    Log.d(
                        TAG,
                        "USB permission result: granted=$granted hasPermissionNow=$hasPermissionNow " +
                            "deviceId=${device?.deviceId} effectiveGranted=$effectiveGranted elapsedMs=$elapsedMs"
                    )
                    appendEvent("Callback recibido: granted=$granted hasPermissionNow=$hasPermissionNow (${elapsedMs ?: -1}ms)")
                    debugLog(
                        hypothesisId = if (dialogLikelySuppressed) "H1" else "H2",
                        location = "UvcMicroscopeManager.usbReceiver.PERMISSION",
                        message = "USB permission callback",
                        data = mapOf(
                            "granted" to granted,
                            "hasPermissionNow" to hasPermissionNow,
                            "effectiveGranted" to effectiveGranted,
                            "deviceId" to device?.deviceId,
                            "elapsedMs" to elapsedMs,
                            "dialogLikelyShown" to dialogLikelyShown,
                            "dialogLikelySuppressed" to dialogLikelySuppressed,
                            "requestCount" to permissionRequestCount,
                            "callbackCount" to permissionCallbackCount
                        )
                    )
                    explicitPermissionDenied = !effectiveGranted
                    applyPermissionResult(effectiveGranted)
                }
            }
        }
    }

    init {
        UsbDebugNdjson.init(appContext)
    }

    fun startMonitoring() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                addAction(ACTION_USB_PERMISSION)
            }
            ContextCompat.registerReceiver(
                appContext,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
            appendEvent("Receiver USB registrado")
        }
        refreshUsbDeviceState()
    }

    fun attachFragment(fragment: UvcMicroscopeFragment) {
        this.fragment = fragment
        refreshUsbDeviceState()
    }

    fun detachFragment(fragment: UvcMicroscopeFragment) {
        if (this.fragment === fragment) {
            this.fragment = null
            onPreviewClosed()
        }
    }

    fun requestUsbPermission() {
        val device = findUvcDevice()
        if (device == null) {
            appendEvent("Permiso solicitado: sin dispositivo")
            _state.update {
                it.copy(
                    deviceConnected = false,
                    statusMessage = "Microscopio desconectado",
                    errorMessage = "Conecta el microscopio USB-C para solicitar permiso.",
                    fallbackGalleryAvailable = true,
                    debug = currentDebug(liveHasPermission = false)
                )
            }
            return
        }
        if (usbManager.hasPermission(device)) {
            Log.d(TAG, "USB permission already granted for deviceId=${device.deviceId}")
            lastGranted = true
            lastHasPermissionNow = true
            explicitPermissionDenied = false
            awaitingCallback = false
            dialogLikelySuppressed = false
            appendEvent("Permiso ya concedido por el sistema (hasPermission=true)")
            debugLog(
                hypothesisId = "H3",
                location = "UvcMicroscopeManager.requestUsbPermission.alreadyGranted",
                message = "System already has USB permission",
                data = mapOf("deviceId" to device.deviceId)
            )
            applyPermissionResult(granted = true)
            return
        }
        permissionRequestCount += 1
        requestGeneration += 1
        val generation = requestGeneration
        lastRequestAtMs = System.currentTimeMillis()
        lastGranted = null
        awaitingCallback = true
        dialogLikelyShown = null
        dialogLikelySuppressed = false
        lastHasPermissionNow = usbManager.hasPermission(device)
        explicitPermissionDenied = false
        appendEvent("Permiso solicitado (intento #$permissionRequestCount, deviceId=${device.deviceId})")
        debugLog(
            hypothesisId = "H1",
            location = "UvcMicroscopeManager.requestUsbPermission",
            message = "Requesting USB permission",
            data = mapOf(
                "deviceId" to device.deviceId,
                "requestCount" to permissionRequestCount,
                "callbackCount" to permissionCallbackCount,
                "liveHasPermission" to lastHasPermissionNow
            )
        )
        _state.update {
            it.copy(
                permissionDenied = false,
                errorMessage = null,
                statusMessage = "Esperando respuesta de permiso USB…",
                debug = currentDebug(liveHasPermission = false)
            )
        }
        Log.i(TAG, "UsbManager.requestPermission called for ${device.usbDebugLabel()}")
        usbManager.requestPermission(device, usbPermissionIntent(device))
        mainHandler.postDelayed({
            if (awaitingCallback && requestGeneration == generation) {
                dialogLikelyShown = false
                dialogLikelySuppressed = true
                appendEvent("Sin callback: Android probablemente no mostró el diálogo")
                debugLog(
                    hypothesisId = "H1",
                    location = "UvcMicroscopeManager.requestUsbPermission.timeout",
                    message = "No USB permission callback after timeout",
                    data = mapOf(
                        "timeoutMs" to CALLBACK_TIMEOUT_MS,
                        "requestCount" to permissionRequestCount,
                        "callbackCount" to permissionCallbackCount
                    )
                )
                publishDebugOnly()
            }
        }, CALLBACK_TIMEOUT_MS)
    }

    fun capturePhoto(outputFile: File, onResult: (Result<String>) -> Unit) {
        try {
            val activeFragment = fragment
            appendEvent("Captura solicitada")
            debugLog(
                hypothesisId = "CAP",
                location = "UvcMicroscopeManager.capturePhoto",
                message = "Inicio captura UVC",
                data = mapOf(
                    "connected" to _state.value.deviceConnected,
                    "permissionGranted" to _state.value.permissionGranted,
                    "previewActive" to _state.value.previewActive,
                    "hasFragment" to (activeFragment != null),
                    "output" to outputFile.absolutePath
                )
            )
            if (_state.value.deviceConnected.not() || _state.value.permissionGranted.not()) {
                onResult(Result.failure(IllegalStateException("Microscopio desconectado o sin permiso USB.")))
                return
            }
            if (activeFragment == null || _state.value.previewActive.not()) {
                onResult(Result.failure(IllegalStateException("El preview UVC aún no está listo.")))
                return
            }
            _state.update { it.copy(capturing = true, errorMessage = null) }
            activeFragment.capturePhoto(outputFile) { result ->
                _state.update { it.copy(capturing = false) }
                result.fold(
                    onSuccess = { path -> appendEvent("Captura OK: ${File(path).name}") },
                    onFailure = { error -> appendEvent("Captura falló: ${error.message}") }
                )
                onResult(result)
            }
        } catch (error: Throwable) {
            appendEvent("Captura crash evitado: ${error.message}")
            debugLog(
                hypothesisId = "CAP",
                location = "UvcMicroscopeManager.capturePhoto.catch",
                message = "Excepción no controlada en captura",
                data = mapOf("error" to (error.message ?: error.javaClass.simpleName))
            )
            _state.update { it.copy(capturing = false, errorMessage = "Error al capturar la fotografía.") }
            onResult(Result.failure(IllegalStateException("Error al capturar la fotografía.", error)))
        }
    }

    fun onPreviewOpened() {
        appendEvent("Preview UVC abierto")
        debugLog(
            hypothesisId = "H3",
            location = "UvcMicroscopeManager.onPreviewOpened",
            message = "UVC preview opened",
            data = mapOf("lastGranted" to lastGranted, "lastHasPermissionNow" to lastHasPermissionNow)
        )
        _state.update {
            it.copy(
                previewActive = true,
                previewBlocked = false,
                deviceConnected = true,
                permissionGranted = true,
                permissionDenied = false,
                statusMessage = "Microscopio conectado",
                errorMessage = null,
                fallbackGalleryAvailable = false,
                debug = currentDebug(liveHasPermission = true)
            )
        }
    }

    fun onPreviewClosed() {
        _state.update {
            it.copy(
                previewActive = false,
                statusMessage = if (it.deviceConnected) "Microscopio conectado" else "Microscopio desconectado",
                debug = currentDebug(liveHasPermission = it.permissionGranted)
            )
        }
    }

    fun onPreviewError(message: String?) {
        val previewError = message ?: "No se pudo inicializar el microscopio USB."
        val fatalPreviewError = previewError.isFatalPreviewError()
        appendEvent("Error preview: $previewError")
        if (fatalPreviewError) {
            appendEvent("Preview detenido: verifica cable/alimentación")
        }
        debugLog(
            hypothesisId = "H4",
            location = "UvcMicroscopeManager.onPreviewError",
            message = "UVC preview error",
            data = mapOf(
                "message" to message,
                "fatalPreviewError" to fatalPreviewError,
                "uiError" to _state.value.errorMessage
            )
        )
        _state.update {
            it.copy(
                previewActive = false,
                previewBlocked = fatalPreviewError,
                statusMessage = if (fatalPreviewError) {
                    "Preview UVC detenido para evitar reconexiones en bucle."
                } else {
                    it.statusMessage
                },
                errorMessage = if (fatalPreviewError) {
                    "No se pudo abrir el stream UVC estable. Verifica el cable OTG/USB-C, la alimentación del microscopio y vuelve a conectarlo."
                } else {
                    previewError
                },
                fallbackGalleryAvailable = true,
                debug = currentDebug(liveHasPermission = it.permissionGranted)
            )
        }
    }

    fun refreshUsbDeviceState() {
        if (!usbHostSupported) {
            _state.update {
                it.copy(
                    usbHostSupported = false,
                    deviceConnected = false,
                    permissionGranted = false,
                    previewActive = false,
                    statusMessage = "Este dispositivo no soporta USB Host.",
                    fallbackGalleryAvailable = true,
                    debug = currentDebug(liveHasPermission = false)
                )
            }
            return
        }

        val device = findUvcDevice()
        val connected = device != null
        val hasPermission = device?.let { usbManager.hasPermission(it) } == true
        val deviceId = device?.deviceId
        if (device != null) {
            Log.i(TAG, "USB device detected: ${device.usbDebugLabel()} hasPermission=$hasPermission")
            appendEvent("Dispositivo detectado: ${device.usbDebugLabel()} hasPermission=$hasPermission")
        }
        val reconnected = connected && lastConnectedDeviceId != null && deviceId != lastConnectedDeviceId
        if (!connected) {
            lastConnectedDeviceId = null
            explicitPermissionDenied = false
        } else if (reconnected || lastConnectedDeviceId == null) {
            if (reconnected) {
                Log.d(TAG, "USB device reconnected (deviceId=$deviceId), resetting permission denial state")
                explicitPermissionDenied = false
                appendEvent("USB reconectado (deviceId=$deviceId)")
            }
            lastConnectedDeviceId = deviceId
        }

        if (hasPermission) {
            explicitPermissionDenied = false
            dialogLikelySuppressed = false
        }

        val previousError = _state.value.errorMessage
        _state.update {
            it.copy(
                usbHostSupported = true,
                deviceConnected = connected,
                permissionGranted = hasPermission,
                permissionDenied = connected && !hasPermission && explicitPermissionDenied,
                previewBlocked = if (connected) it.previewBlocked else false,
                previewActive = if (connected && hasPermission) it.previewActive else false,
                statusMessage = when {
                    !connected -> "Microscopio desconectado"
                    it.previewBlocked -> "Preview UVC detenido para evitar reconexiones en bucle."
                    hasPermission -> "Microscopio conectado"
                    explicitPermissionDenied -> "Permiso USB rechazado. No se puede abrir el microscopio."
                    else -> "Microscopio conectado. Falta permiso USB."
                },
                errorMessage = when {
                    !connected -> "Microscopio desconectado, verifica el cable."
                    hasPermission -> null
                    explicitPermissionDenied && dialogLikelySuppressed -> SETTINGS_HINT
                    explicitPermissionDenied -> "Permiso USB rechazado por el usuario."
                    else -> null
                },
                fallbackGalleryAvailable = !connected || !hasPermission,
                debug = currentDebug(liveHasPermission = hasPermission)
            )
        }
        debugLog(
            hypothesisId = "H3",
            location = "UvcMicroscopeManager.refreshUsbDeviceState",
            message = "USB state refreshed",
            data = mapOf(
                "connected" to connected,
                "hasPermission" to hasPermission,
                "explicitDenied" to explicitPermissionDenied,
                "previousError" to previousError,
                "newError" to _state.value.errorMessage,
                "permissionGranted" to _state.value.permissionGranted,
                "permissionDenied" to _state.value.permissionDenied
            )
        )
        Log.d(
            TAG,
            "refreshUsbDeviceState: connected=$connected hasPermission=$hasPermission " +
                "explicitDenied=$explicitPermissionDenied previewActive=${_state.value.previewActive}"
        )
        if (connected && !hasPermission && !awaitingCallback && !explicitPermissionDenied) {
            appendEvent("hasPermission=false → solicitando permiso USB automáticamente")
            Log.i(TAG, "hasPermission=false; calling UsbManager.requestPermission()")
            requestUsbPermission()
        }
    }

    private fun applyPermissionResult(granted: Boolean) {
        _state.update {
            it.copy(
                permissionGranted = granted,
                permissionDenied = !granted,
                statusMessage = if (granted) {
                    "Permiso USB concedido. Abriendo preview del microscopio."
                } else {
                    "Permiso USB rechazado. No se puede abrir el microscopio."
                },
                errorMessage = when {
                    granted -> null
                    dialogLikelySuppressed -> SETTINGS_HINT
                    else -> "Permiso USB rechazado por el usuario."
                },
                fallbackGalleryAvailable = !granted,
                debug = currentDebug(liveHasPermission = granted)
            )
        }
        debugLog(
            hypothesisId = "H3",
            location = "UvcMicroscopeManager.applyPermissionResult",
            message = "Applied permission result before refresh",
            data = mapOf(
                "granted" to granted,
                "errorAfterApply" to _state.value.errorMessage,
                "dialogLikelySuppressed" to dialogLikelySuppressed
            )
        )
        refreshUsbDeviceState()
    }

    private fun publishDebugOnly() {
        _state.update {
            it.copy(
                errorMessage = if (dialogLikelySuppressed && !it.permissionGranted) {
                    SETTINGS_HINT
                } else {
                    it.errorMessage
                },
                debug = currentDebug(liveHasPermission = it.permissionGranted)
            )
        }
    }

    private fun currentDebug(liveHasPermission: Boolean): UvcUsbDebugInfo {
        val status = when {
            liveHasPermission || lastGranted == true -> "CONCEDIDO"
            lastGranted == false || explicitPermissionDenied -> "RECHAZADO"
            lastGranted == null && awaitingCallback -> "PENDIENTE"
            lastGranted == null -> "PENDIENTE"
            else -> "PENDIENTE"
        }
        val dialogStatus = when {
            awaitingCallback -> "Esperando diálogo/callback nativo…"
            dialogLikelySuppressed -> "Diálogo probablemente NO se mostró (Android auto-respondió o lo suprimió)"
            dialogLikelyShown == true -> "Diálogo nativo probablemente SÍ se mostró (callback tardó lo suficiente)"
            dialogLikelyShown == false -> "Respuesta demasiado rápida; el diálogo pudo no verse"
            permissionRequestCount == 0 -> "Sin solicitud aún"
            else -> "Solicitud hecha; sin evidencia clara del diálogo"
        }
        return UvcUsbDebugInfo(
            lastGranted = lastGranted,
            lastHasPermissionNow = lastHasPermissionNow,
            liveHasPermission = liveHasPermission,
            permissionStatus = status,
            dialogStatus = dialogStatus,
            settingsHint = if (dialogLikelySuppressed && !liveHasPermission) SETTINGS_HINT else null,
            events = eventLog.toList()
        )
    }

    private fun appendEvent(message: String) {
        val line = "${timeFormat.format(Date())}  $message"
        synchronized(eventLog) {
            eventLog.addLast(line)
            while (eventLog.size > MAX_EVENTS) {
                eventLog.removeFirst()
            }
        }
    }

    private fun resetPreviewBlock() {
        _state.update {
            it.copy(
                previewBlocked = false,
                previewActive = false,
                debug = currentDebug(liveHasPermission = it.permissionGranted)
            )
        }
    }

    private fun String.isFatalPreviewError(): Boolean {
        val normalized = lowercase()
        return normalized.contains("failed to set preview size") ||
            normalized.contains("unsupported preview size") ||
            normalized.contains("opendevice failed") ||
            normalized.contains("open camera failed") ||
            normalized.contains("already be removed") ||
            normalized.contains("no permission")
    }

    // #region agent log
    private fun debugLog(
        hypothesisId: String,
        location: String,
        message: String,
        data: Map<String, Any?>
    ) {
        UsbDebugNdjson.log(
            hypothesisId = hypothesisId,
            location = location,
            message = message,
            data = data
        )
    }
    // #endregion

    private fun Intent.getUsbDeviceExtra(): UsbDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

    private fun findUvcDevice(): UsbDevice? =
        usbManager.deviceList.values.firstOrNull { it.isLikelyUvcCamera() }

    private fun UsbDevice.isLikelyUvcCamera(): Boolean {
        if (deviceClass == UsbConstants.USB_CLASS_VIDEO) return true
        for (index in 0 until interfaceCount) {
            val usbInterface = getInterface(index)
            if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_VIDEO) return true
            if (
                usbInterface.interfaceClass == USB_CLASS_MISC &&
                usbInterface.interfaceSubclass == USB_SUBCLASS_COMMON &&
                usbInterface.interfaceProtocol == USB_PROTOCOL_INTERFACE_ASSOCIATION
            ) {
                return true
            }
        }
        return false
    }

    private fun UsbDevice.usbDebugLabel(): String =
        "deviceId=$deviceId vendorId=$vendorId productId=$productId class=$deviceClass " +
            "interfaces=$interfaceCount name=$deviceName"

    private fun usbPermissionIntent(device: UsbDevice): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val intent = Intent(ACTION_USB_PERMISSION).apply {
            setPackage(appContext.packageName)
            putExtra(UsbManager.EXTRA_DEVICE, device)
        }
        return PendingIntent.getBroadcast(
            appContext,
            device.deviceId,
            intent,
            flags
        )
    }

    private companion object {
        const val TAG = "UvcMicroscope"
        const val ACTION_USB_PERMISSION = "com.laboratoriofisico.app.USB_PERMISSION"
        const val USB_CLASS_MISC = 239
        const val USB_SUBCLASS_COMMON = 2
        const val USB_PROTOCOL_INTERFACE_ASSOCIATION = 1
        const val MAX_EVENTS = 10
        const val CALLBACK_TIMEOUT_MS = 2500L
        const val DIALOG_SHOWN_THRESHOLD_MS = 400L
        const val DIALOG_SUPPRESSED_THRESHOLD_MS = 300L
        const val SETTINGS_HINT =
            "Android no volvió a pedir el permiso USB para este dispositivo. " +
                "Desconecta el microscopio, ve a Ajustes → Apps → Laboratorio Científico → Permisos " +
                "(o Permisos adicionales / dispositivos USB) y vuelve a conectarlo para autorizarlo."
    }
}
