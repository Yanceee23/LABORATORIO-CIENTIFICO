package com.laboratoriofisico.app.backend.auth

/**
 * Roles de acceso de la app.
 * Invitado no tiene fila en Room; solo vive en la sesión.
 */
enum class AppRole(val storageValue: String) {
    INVITADO("invitado"),
    REGISTRADO("registrado");

    companion object {
        fun fromStorage(value: String?): AppRole = when (value?.trim()?.lowercase()) {
            INVITADO.storageValue -> INVITADO
            "estudiante", REGISTRADO.storageValue, "usuario", "admin" -> REGISTRADO
            else -> REGISTRADO
        }
    }
}

object RolePermissions {
    fun canAccessLaboratorio(role: AppRole): Boolean = role == AppRole.REGISTRADO
    fun canAccessTutor(role: AppRole): Boolean = role == AppRole.REGISTRADO
    fun canAccessDocumentos(role: AppRole): Boolean = role == AppRole.REGISTRADO
    fun canAccessEstudio(role: AppRole): Boolean = role == AppRole.REGISTRADO
    fun canSaveMeasurements(role: AppRole): Boolean = role == AppRole.REGISTRADO
    fun canViewMeasurementHistory(role: AppRole): Boolean = role == AppRole.REGISTRADO
    fun canExport(role: AppRole): Boolean = role == AppRole.REGISTRADO
}
