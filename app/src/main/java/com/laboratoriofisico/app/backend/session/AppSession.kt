package com.laboratoriofisico.app.backend.session

import com.laboratoriofisico.app.backend.auth.AppRole

/**
 * Estado de sesión de la app.
 * [Guest] no crea fila en Room ni guarda datos persistentes.
 */
sealed class AppSession {
    data object None : AppSession()
    data object Guest : AppSession()
    data class User(val userId: Long) : AppSession()

    val isAuthenticated: Boolean get() = this !is None
    val role: AppRole?
        get() = when (this) {
            None -> null
            Guest -> AppRole.INVITADO
            is User -> AppRole.REGISTRADO
        }
    val userIdOrNull: Long?
        get() = (this as? User)?.userId
}
