package com.laboratoriofisico.app.backend.session

/**
 * Contrato de sesión de autenticación.
 * La implementación real ([SessionRepository]) persiste el usuario activo en DataStore.
 */
interface AuthSessionManager {
    suspend fun setLoggedInUser(userId: Long)

    suspend fun logout()

    /** Id del usuario con sesión activa, o null si no hay sesión registrada. */
    suspend fun getActiveUserId(): Long?

    /** True si [userId] ya tiene una sesión activa. */
    suspend fun hasActiveSessionFor(userId: Long): Boolean =
        getActiveUserId() == userId

    /** Entra como invitado (sin fila en Room). */
    suspend fun enterAsGuest()
}
