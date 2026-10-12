package com.laboratoriofisico.app.backend.auth

import com.laboratoriofisico.app.backend.session.AppSession
import com.laboratoriofisico.app.backend.session.SessionRepository

/**
 * Verificación de rol en repositorios (además de la navegación).
 */
class RegisteredUserGuard(
    private val sessionRepository: SessionRepository
) {
    suspend fun requireRegisteredUserId(): Long {
        val session = sessionRepository.getSession()
        if (session is AppSession.Guest || session is AppSession.None) {
            error(AuthMessages.INVITADO_BLOQUEADO)
        }
        return session.userIdOrNull
            ?: error(AuthMessages.INVITADO_BLOQUEADO)
    }

    suspend fun canPersist(): Boolean =
        sessionRepository.getSession() is AppSession.User

    companion object {
        fun canAccessLaboratorio(session: AppSession): Boolean =
            RolePermissions.canAccessLaboratorio(session.role ?: AppRole.INVITADO)

        fun canAccessTutor(session: AppSession): Boolean =
            RolePermissions.canAccessTutor(session.role ?: AppRole.INVITADO)

        fun canAccessDocumentos(session: AppSession): Boolean =
            RolePermissions.canAccessDocumentos(session.role ?: AppRole.INVITADO)

        fun canAccessEstudio(session: AppSession): Boolean =
            RolePermissions.canAccessEstudio(session.role ?: AppRole.INVITADO)

        fun canSaveOrViewHistory(session: AppSession): Boolean {
            val role = session.role ?: AppRole.INVITADO
            return RolePermissions.canSaveMeasurements(role)
        }
    }
}
